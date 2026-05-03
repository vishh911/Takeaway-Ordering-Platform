package com.campuseateries.model;

public class Restaurant {

    private long restaurantId;
    private String name;
    private String description;
    private String campusZone;
    private boolean active;

    public Restaurant() {
    }

    public Restaurant(long restaurantId, String name, String description, String campusZone, boolean active) {
        this.restaurantId = restaurantId;
        this.name = name;
        this.description = description;
        this.campusZone = campusZone;
        this.active = active;
    }

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCampusZone() {
        return campusZone;
    }

    public void setCampusZone(String campusZone) {
        this.campusZone = campusZone;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
