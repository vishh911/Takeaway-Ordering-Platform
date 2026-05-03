package com.campuseateries.model;

public enum OrderStatus {
    DRAFT,
    PLACED,
    AWAITING_PAYMENT,
    PAID,
    PREPARING,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED;

    public static OrderStatus parse(String sqlValue) {
        return OrderStatus.valueOf(sqlValue);
    }
}
