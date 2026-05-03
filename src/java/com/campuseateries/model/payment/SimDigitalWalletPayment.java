package com.campuseateries.model.payment;

import com.campuseateries.model.PaymentMethod;

import java.math.BigDecimal;

public class SimDigitalWalletPayment extends AbstractPayment {

    public SimDigitalWalletPayment(BigDecimal amount) {
        super(amount);
    }

    @Override
    public String channelName() {
        return "DIGITAL_SIM campus wallet";
    }

    @Override
    public PaymentMethod jdbcChannel() {
        return PaymentMethod.DIGITAL_SIM;
    }
}
