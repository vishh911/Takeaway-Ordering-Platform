package com.campuseateries.service;

import com.campuseateries.model.DeliveryDraft;
import com.campuseateries.model.PaymentMethod;

/**
 * Stateful orchestration façade implemented by transactional {@linkplain OrderFulfillmentService}
 * (inherits encapsulation barrier between controllers and JDBC DAO collaborators).
 */
public interface IOrderFulfillment {

    PlacedOrderSnapshot commitCartToOrder(long userId, DeliveryDraft deliveryDraft,
                                           PaymentMethod paymentChannelHint)
            throws BusinessRuleException, Exception;

    boolean completeSimulatedSettlement(long orderId, PaymentMethod method) throws Exception;
}
