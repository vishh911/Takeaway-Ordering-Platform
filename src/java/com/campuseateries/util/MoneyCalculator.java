package com.campuseateries.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Tax + rounding helpers reused by transactional order placement. */
public final class MoneyCalculator {

    /** Flat campus meals tax simulated for coursework clarity. */
    public static final BigDecimal CAMPUS_TAX_RATE = new BigDecimal("0.08");

    private MoneyCalculator() {
    }

    public static BigDecimal taxOn(BigDecimal subtotal) {
        return subtotal.multiply(CAMPUS_TAX_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal totalOf(BigDecimal subtotal, BigDecimal tax) {
        return subtotal.add(tax).setScale(2, RoundingMode.HALF_UP);
    }
}
