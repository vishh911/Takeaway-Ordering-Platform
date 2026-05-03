package com.campuseateries.model.payment;

import com.campuseateries.model.PaymentMethod;

import java.math.BigDecimal;

public class CashOnDeliverySimPayment extends AbstractPayment {

    public CashOnDeliverySimPayment(BigDecimal amount) {
        super(amount);
    }

    @Override
    public String channelName() {
        return "COD_SIM rider collects cash";
    }

    @Override
    public PaymentMethod jdbcChannel() {
        return PaymentMethod.COD_SIM;
    }
}
