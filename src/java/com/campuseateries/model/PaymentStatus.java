package com.campuseateries.model;

public enum PaymentStatus {
    INITIATED,
    COMPLETED,
    FAILED;

    public static PaymentStatus parse(String raw) {
        return PaymentStatus.valueOf(raw);
    }
}
