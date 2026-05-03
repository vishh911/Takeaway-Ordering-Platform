package com.campuseateries.model;

public enum PaymentMethod {
    CARD_SIM,
    DIGITAL_SIM,
    COD_SIM;

    public static PaymentMethod parse(String raw) {
        return PaymentMethod.valueOf(raw);
    }
}
