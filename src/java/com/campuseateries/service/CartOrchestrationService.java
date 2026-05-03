package com.campuseateries.service;

import com.campuseateries.dao.CartDao;
import com.campuseateries.model.CartLine;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Mutation operations on shopping carts with derived analytics helpers reused by fulfillment. */
public class CartOrchestrationService {

    private final CartDao cartDao = new CartDao();

    public List<CartLine> summarize(long userId) throws Exception {
        return cartDao.summarize(userId);
    }

    public void add(long userId, long menuItemId, int qty) throws Exception {
        if (qty <= 0) {
            throw new BusinessRuleException("Quantity must be positive.");
        }
        cartDao.upsertLine(null, userId, menuItemId, qty);
    }

    public void updateQuantityAbsolute(long userId, long menuItemId, int absoluteQty)
            throws Exception {
        cartDao.absoluteQuantity(null, userId, menuItemId, absoluteQty);
    }

    public void remove(long userId, long menuItemId) throws Exception {
        cartDao.deleteLine(null, userId, menuItemId);
    }

    public CartTotals aggregate(List<CartLine> lines) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartLine cl : lines) {
            subtotal = subtotal.add(cl.lineSubtotal());
        }
        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
        return new CartTotals(subtotal);
    }

    public boolean assertSingleRestaurant(List<CartLine> lines, StringBuilder offendingNameBuffer)
            throws BusinessRuleException {
        if (lines.isEmpty()) {
            throw new BusinessRuleException("Your cart is empty.");
        }
        Set<Long> restaurantIds = new HashSet<>();
        for (CartLine cl : lines) {
            restaurantIds.add(cl.getRestaurantId());
        }
        if (restaurantIds.size() > 1) {
            offendingNameBuffer.append("(mixed restaurants detected)");
            return false;
        }
        return true;
    }

    public record CartTotals(BigDecimal subtotal) {
        public BigDecimal tax() {
            return com.campuseateries.util.MoneyCalculator.taxOn(subtotal);
        }

        public BigDecimal grandTotal() {
            return com.campuseateries.util.MoneyCalculator.totalOf(subtotal, tax());
        }
    }
}
