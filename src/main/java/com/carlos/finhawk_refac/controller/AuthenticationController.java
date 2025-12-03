package com.carlos.finhawk_refac.controller;


import com.carlos.finhawk_refac.config.security.TokenService;
import com.carlos.finhawk_refac.dto.AuthenticationDTO;
import com.carlos.finhawk_refac.dto.RegisterDTO;
import com.carlos.finhawk_refac.dto.response.LoginResponseDTO;
import com.carlos.finhawk_refac.entity.UserAccount;
import com.carlos.finhawk_refac.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private TokenService tokenService;

    @PostMapping("/login")
    public ResponseEntity login (@RequestBody @Valid AuthenticationDTO data){

        var usernamePassword = new UsernamePasswordAuthenticationToken(data.email(), data.password());
        var auth = this.authenticationManager.authenticate(usernamePassword);

        var token = tokenService.generateToken((UserAccount) auth.getPrincipal());
        return ResponseEntity.ok(new LoginResponseDTO(token));

    }

    @PostMapping("/register")
    public ResponseEntity register(@RequestBody @Valid RegisterDTO data){
        if (this.userAccountRepository.findByLogin(data.email()) != null)
            return ResponseEntity.badRequest().build();
        String encryptedPassword = new BCryptPasswordEncoder().encode(data.password());
        UserAccount newUser = new UserAccount(data.email(), encryptedPassword, data.role());

        this.userAccountRepository.save(newUser);
        return ResponseEntity.ok().build();
    }


}
