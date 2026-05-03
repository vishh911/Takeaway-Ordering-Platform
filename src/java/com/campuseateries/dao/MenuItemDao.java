package com.campuseateries.dao;

import com.campuseateries.database.DbConnectionManager;
import com.campuseateries.model.MenuItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MenuItemDao {

    private final DbConnectionManager db = DbConnectionManager.getInstance();

    public List<MenuItem> findByRestaurant(long restaurantId, boolean includeUnavailable) throws Exception {
        List<MenuItem> list = new ArrayList<>();
        String sql = """
                SELECT mi.menu_item_id, mi.restaurant_id, mi.category_id,
                       c.name AS category_name,
                       mi.item_name, mi.description,
                       mi.unit_price, mi.stock_quantity, mi.is_available,
                       r.name AS restaurant_name
                FROM menu_items mi
                JOIN categories c ON c.category_id = mi.category_id
                JOIN restaurants r ON r.restaurant_id = mi.restaurant_id
                WHERE mi.restaurant_id=?
                  AND (? OR mi.is_available=1)
                ORDER BY category_name, item_name
                """;
        try (Connection cn = db.openConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, restaurantId);
            ps.setBoolean(2, includeUnavailable);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    public Optional<MenuItem> find(long menuItemId) throws Exception {
        String sql = """
                SELECT mi.menu_item_id, mi.restaurant_id, mi.category_id,
                       c.name AS category_name,
                       mi.item_name, mi.description,
                       mi.unit_price, mi.stock_quantity, mi.is_available,
                       r.name AS restaurant_name
                FROM menu_items mi
                JOIN categories c ON c.category_id = mi.category_id
                JOIN restaurants r ON r.restaurant_id = mi.restaurant_id
                WHERE mi.menu_item_id=?
                """;
        try (Connection cn = db.openConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, menuItemId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        }
    }

    private MenuItem map(ResultSet rs) throws Exception {
        MenuItem m = new MenuItem();
        m.setMenuItemId(rs.getLong("menu_item_id"));
        m.setRestaurantId(rs.getLong("restaurant_id"));
        m.setCategoryId(rs.getInt("category_id"));
        m.setCategoryName(rs.getString("category_name"));
        m.setItemName(rs.getString("item_name"));
        m.setDescription(rs.getString("description"));
        m.setUnitPrice(rs.getBigDecimal("unit_price").setScale(2, RoundingMode.HALF_UP));
        m.setStockQuantity(rs.getInt("stock_quantity"));
        m.setAvailable(rs.getBoolean("is_available"));
        m.setRestaurantNameSnapshot(rs.getString("restaurant_name"));
        return m;
    }
}
