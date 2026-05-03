package com.campuseateries.model;

import java.math.BigDecimal;

/** Read-model for inspecting placed order lines pulled via JOIN-heavy analytics queries. */
public class OrderLineDraft {

    private long orderItemId;
    private String menuItemName;
    private int quantity;
    private BigDecimal snapshotUnitPrice;

    public long getOrderItemId() {
        return orderItemId;
    }

    public void setOrderItemId(long orderItemId) {
        this.orderItemId = orderItemId;
    }

    public String getMenuItemName() {
        return menuItemName;
    }

    public void setMenuItemName(String menuItemName) {
        this.menuItemName = menuItemName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getSnapshotUnitPrice() {
        return snapshotUnitPrice;
    }

    public void setSnapshotUnitPrice(BigDecimal snapshotUnitPrice) {
        this.snapshotUnitPrice = snapshotUnitPrice;
    }
}
