package com.campuseateries.service;

import com.campuseateries.dao.CartDao;
import com.campuseateries.dao.OrderDao;
import com.campuseateries.dao.PaymentDao;
import com.campuseateries.database.DbConnectionManager;
import com.campuseateries.model.CartLine;
import com.campuseateries.model.DeliveryDraft;
import com.campuseateries.model.OrderStatus;
import com.campuseateries.model.PaymentMethod;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

/**
 * Transactional collaborator: merges cart totals, persists normalized rows atomically, and invokes
 * payment simulation procedures under referential constraints + triggers.
 */
public class OrderFulfillmentService implements IOrderFulfillment {

    private final DbConnectionManager db = DbConnectionManager.getInstance();
    private final OrderDao orders = new OrderDao();
    private final CartDao cartDao = new CartDao();
    private final PaymentDao paymentDao = new PaymentDao();
    private final CartOrchestrationService cartMath = new CartOrchestrationService();

    @Override
    public PlacedOrderSnapshot commitCartToOrder(long userId,
                                                 DeliveryDraft deliveryDraft,
                                                 PaymentMethod paymentHint)
            throws BusinessRuleException, Exception {
        sanitizeDeliveryDraft(deliveryDraft);
        List<CartLine> lines = cartDao.summarize(userId);
        StringBuilder offending = new StringBuilder();
        if (!cartMath.assertSingleRestaurant(lines, offending)) {
            throw new BusinessRuleException(
                    "Campus eateries currently support checkout from one booth per ticket. Mixed carts must be trimmed.");
        }
        CartOrchestrationService.CartTotals totals = cartMath.aggregate(lines);
        List<Long> menuIds = extractMenuIds(lines);
        try (Connection cn = db.openConnection()) {
            cn.setAutoCommit(false);
            try {
                long restaurantKey = lines.get(0).getRestaurantId();
                long orderId = orders.insertPendingOrder(cn,
                        userId,
                        restaurantKey,
                        totals.subtotal(),
                        totals.tax(),
                        totals.grandTotal(),
                        OrderStatus.AWAITING_PAYMENT);
                orders.insertLines(cn, orderId, lines);
                orders.insertDelivery(cn, orderId, deliveryDraft);
                orders.insertPlaceholderPayment(cn, orderId, totals.grandTotal(), paymentHint);
                cartDao.purgeItemsForUser(cn, userId, menuIds);
                cn.commit();
                return new PlacedOrderSnapshot(orderId, totals.subtotal(), totals.tax(), totals.grandTotal());
            } catch (Exception ex) {
                cn.rollback();
                throw ex;
            }
        }
    }

    @Override
    public boolean completeSimulatedSettlement(long orderId, PaymentMethod method) throws Exception {
        return paymentDao.simulatePaymentGateway(orderId, method);
    }

    private static List<Long> extractMenuIds(List<CartLine> lines) {
        List<Long> ids = new ArrayList<>(lines.size());
        for (CartLine cl : lines) {
            ids.add(cl.getMenuItemId());
        }
        return ids;
    }

    private static void sanitizeDeliveryDraft(DeliveryDraft draft) throws BusinessRuleException {
        if (draft == null || draft.getBuildingCode() == null || draft.getBuildingCode().isBlank()) {
            throw new BusinessRuleException("Delivery building identifier is mandatory for runners.");
        }
        draft.setBuildingCode(draft.getBuildingCode().trim().toUpperCase());
        if (draft.getRoomNumber() != null) {
            draft.setRoomNumber(draft.getRoomNumber().trim());
        }
        if (draft.getInstructions() != null) {
            draft.setInstructions(draft.getInstructions().trim());
        }
    }
}
