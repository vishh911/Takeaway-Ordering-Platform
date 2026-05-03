package com.campuseateries.model;

import java.math.BigDecimal;

public class AnalyticsRestaurantRow {

    private long restaurantId;
    private String name;
    private long lifetimeOrders;
    private BigDecimal lifetimeUnitsSold;
    private BigDecimal lifetimePaidAmount;

    public long getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(long restaurantId) {
        this.restaurantId = restaurantId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getLifetimeOrders() {
        return lifetimeOrders;
    }

    public void setLifetimeOrders(long lifetimeOrders) {
        this.lifetimeOrders = lifetimeOrders;
    }

    public BigDecimal getLifetimeUnitsSold() {
        return lifetimeUnitsSold;
    }

    public void setLifetimeUnitsSold(BigDecimal lifetimeUnitsSold) {
        this.lifetimeUnitsSold = lifetimeUnitsSold;
    }

    public BigDecimal getLifetimePaidAmount() {
        return lifetimePaidAmount;
    }

    public void setLifetimePaidAmount(BigDecimal lifetimePaidAmount) {
        this.lifetimePaidAmount = lifetimePaidAmount;
    }
}
