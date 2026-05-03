package com.campuseateries.model.payment;

import com.campuseateries.model.PaymentMethod;

import java.math.BigDecimal;

/** Template method scaffolding for heterogeneous settlement paths. */
public abstract class AbstractPayment implements IPaymentSimulator {

    protected final BigDecimal grossAmountUsd;

    protected AbstractPayment(BigDecimal grossAmountUsd) {
        this.grossAmountUsd = grossAmountUsd;
    }

    public BigDecimal getGrossAmountUsd() {
        return grossAmountUsd;
    }

    /** Maps OO hierarchy nodes to enumerated JDBC payloads for DAO collaborators. */
    public abstract PaymentMethod jdbcChannel();

    @Override
    public boolean authorize(double amountUsd) {
        return Math.abs(amountUsd - grossAmountUsd.doubleValue()) < 0.0005d;
    }
}
