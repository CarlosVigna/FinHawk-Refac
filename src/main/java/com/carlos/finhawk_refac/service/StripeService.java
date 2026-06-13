package com.carlos.finhawk_refac.service;

import com.carlos.finhawk_refac.entity.UserAccount;
import com.carlos.finhawk_refac.enums.PlanType;
import com.carlos.finhawk_refac.repository.UserAccountRepository;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StripeService {

    private static final Logger log = LoggerFactory.getLogger(StripeService.class);

    @Value("${stripe.secret.key}")
    private String secretKey;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    @Value("${stripe.price.monthly}")
    private String priceMonthly;

    @Value("${stripe.price.annual}")
    private String priceAnnual;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    private final UserAccountRepository userAccountRepository;

    public StripeService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @PostConstruct
    public void init() {
        if (secretKey != null && !secretKey.isBlank()) {
            Stripe.apiKey = secretKey;
        }
    }

    public String createCheckoutSession(UserAccount user, String period) throws Exception {
        String priceId = "annual".equalsIgnoreCase(period) ? priceAnnual : priceMonthly;

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setCustomerEmail(user.getEmail())
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setPrice(priceId)
                                .setQuantity(1L)
                                .build()
                )
                .putMetadata("userId", user.getId().toString())
                .setSuccessUrl(frontendUrl + "/dashboard?upgrade=success")
                .setCancelUrl(frontendUrl + "/dashboard?upgrade=cancelled")
                .build();

        Session session = Session.create(params);
        return session.getUrl();
    }

    @Transactional
    public void handleWebhook(String payload, String sigHeader) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.warn("Stripe webhook secret not configured — skipping verification");
            return;
        }

        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            throw new RuntimeException("Invalid Stripe webhook signature");
        }

        if ("checkout.session.completed".equals(event.getType())) {
            Session session = (Session) event.getDataObjectDeserializer()
                    .getObject()
                    .orElseThrow(() -> new RuntimeException("Could not deserialize Stripe session"));

            String userId = session.getMetadata().get("userId");
            if (userId == null) return;

            userAccountRepository.findById(Long.parseLong(userId)).ifPresent(user -> {
                user.setPlan(PlanType.PRO);
                userAccountRepository.save(user);
                log.info("User {} upgraded to PRO via Stripe", userId);
            });
        }
    }
}
