package com.campuseateries.service;

import com.campuseateries.dao.MenuItemDao;
import com.campuseateries.dao.RestaurantDao;
import com.campuseateries.model.MenuItem;
import com.campuseateries.model.Restaurant;

import java.util.List;

/** Read-heavy façade abstracting DAO wiring for MVC controllers browsing vendors. */
public class RestaurantCatalogService {

    private final RestaurantDao restaurants = new RestaurantDao();
    private final MenuItemDao menus = new MenuItemDao();

    public List<Restaurant> activeRestaurants() throws Exception {
        return restaurants.findActiveOrdered();
    }

    public List<MenuItem> menuForRestaurant(long restaurantId, boolean includeUnavailableItems)
            throws Exception {
        return menus.findByRestaurant(restaurantId, includeUnavailableItems);
    }
}
