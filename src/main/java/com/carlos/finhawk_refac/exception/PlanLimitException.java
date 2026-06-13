package com.carlos.finhawk_refac.exception;

public class PlanLimitException extends RuntimeException {

    private final String limitType;

    public PlanLimitException(String limitType, String message) {
        super(message);
        this.limitType = limitType;
    }

    public String getLimitType() {
        return limitType;
    }
}
