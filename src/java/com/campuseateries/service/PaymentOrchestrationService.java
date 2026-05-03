package com.campuseateries.service;

import com.campuseateries.model.PaymentMethod;
import com.campuseateries.model.payment.AbstractPayment;
import com.campuseateries.model.payment.CashOnDeliverySimPayment;
import com.campuseateries.model.payment.SimCardPayment;
import com.campuseateries.model.payment.SimDigitalWalletPayment;

import java.math.BigDecimal;

/**
 * Applies polymorphism: OO payment strategies authorize locally, enumerated channels satisfy stored
 * procedure contracts.
 */
public class PaymentOrchestrationService {

    private final OrderFulfillmentService fulfillment = new OrderFulfillmentService();

    public boolean settle(long orderId, AbstractPayment simulatedChannel) throws Exception {
        PaymentMethod jdbcEnum = simulatedChannel.jdbcChannel();
        boolean authorized =
                simulatedChannel.authorize(simulatedChannel.getGrossAmountUsd().doubleValue());
        if (!authorized) {
            return false;
        }
        return fulfillment.completeSimulatedSettlement(orderId, jdbcEnum);
    }

    /** Factory shorthand for MVC menu numeric mapping. */
    public AbstractPayment buildSimulator(PaymentMethod method, BigDecimal amount, String metaLast4) {
        return switch (method) {
            case CARD_SIM -> new SimCardPayment(amount, metaLast4 == null ? "4242" : metaLast4);
            case DIGITAL_SIM -> new SimDigitalWalletPayment(amount);
            case COD_SIM -> new CashOnDeliverySimPayment(amount);
        };
    }
}
