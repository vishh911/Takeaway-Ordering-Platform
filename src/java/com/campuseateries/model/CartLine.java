package com.campuseateries.model;

import java.math.BigDecimal;

public class CartLine {

    private long cartEntryId;
    private long userId;
    private long menuItemId;
    private int quantity;
    private String itemName;
    private BigDecimal unitPrice;
    private long restaurantId;
    private String restaurantName;

    public long getCartEntryId() {
        return cartEntryId;
    }

    public void setCartEntryId(long cartEntryId) {
        this.cartEntryId = cartEntryId;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public long getMenuItemId() {
        return menuItemId;
    }

    public void setMenuItemId(long menuItemId) {
        this.menuItemId = menuItemId;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public long getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(long restaurantId) {
        this.restaurantId = restaurantId;
    }

    public String getRestaurantName() {
        return restaurantName;
    }

    public void setRestaurantName(String restaurantName) {
        this.restaurantName = restaurantName;
    }

    public BigDecimal lineSubtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
