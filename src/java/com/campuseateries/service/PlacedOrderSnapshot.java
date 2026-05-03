package com.campuseateries.service;

import java.math.BigDecimal;

/** Immutable MVC DTO bridging controllers to persistence outcomes. */
public record PlacedOrderSnapshot(long orderId, BigDecimal subtotal,
                                  BigDecimal tax, BigDecimal total) {
}
