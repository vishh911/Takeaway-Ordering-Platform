package com.campuseateries.model.payment;

import com.campuseateries.model.PaymentMethod;

import java.math.BigDecimal;

public class SimCardPayment extends AbstractPayment {

    private final String lastFourDigits;

    public SimCardPayment(BigDecimal amount, String lastFourDigits) {
        super(amount);
        this.lastFourDigits = lastFourDigits;
    }

    @Override
    public String channelName() {
        return "CARD_SIM ****" + lastFourDigits;
    }

    @Override
    public PaymentMethod jdbcChannel() {
        return PaymentMethod.CARD_SIM;
    }
}
