package com.campuseateries.dao;

import com.campuseateries.database.DbConnectionManager;
import com.campuseateries.model.CartLine;

import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class CartDao {

    private final DbConnectionManager db = DbConnectionManager.getInstance();

    public List<CartLine> summarize(long userId) throws Exception {
        List<CartLine> lines = new ArrayList<>();
        String sql = """
                SELECT sci.cart_entry_id, sci.user_id, sci.menu_item_id, sci.quantity,
                       mi.unit_price,
                       mi.item_name,
                       r.restaurant_id,
                       r.name AS restaurant_name
                FROM shopping_cart_items sci
                JOIN menu_items mi ON mi.menu_item_id = sci.menu_item_id
                JOIN restaurants r ON r.restaurant_id = mi.restaurant_id
                WHERE sci.user_id=?
                ORDER BY r.restaurant_id, mi.item_name
                """;
        try (Connection cn = db.openConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CartLine cl = new CartLine();
                    cl.setCartEntryId(rs.getLong("cart_entry_id"));
                    cl.setUserId(rs.getLong("user_id"));
                    cl.setMenuItemId(rs.getLong("menu_item_id"));
                    cl.setQuantity(rs.getInt("quantity"));
                    cl.setUnitPrice(rs.getBigDecimal("unit_price").setScale(2, RoundingMode.HALF_UP));
                    cl.setItemName(rs.getString("item_name"));
                    cl.setRestaurantId(rs.getLong("restaurant_id"));
                    cl.setRestaurantName(rs.getString("restaurant_name"));
                    lines.add(cl);
                }
            }
        }
        return lines;
    }

    public void upsertLine(Connection external, long userId, long menuItemId, int quantity) throws Exception {
        boolean owns = external == null;
        Connection cn = owns ? db.openConnection() : external;
        try {
            String sqlMerge = """
                    INSERT INTO shopping_cart_items (user_id, menu_item_id, quantity)
                    VALUES (?,?,?)
                    ON DUPLICATE KEY UPDATE quantity = quantity + VALUES(quantity)
                    """;
            try (PreparedStatement ps = cn.prepareStatement(sqlMerge)) {
                ps.setLong(1, userId);
                ps.setLong(2, menuItemId);
                ps.setInt(3, quantity);
                ps.executeUpdate();
            }
        } finally {
            if (owns && cn != null) {
                cn.close();
            }
        }
    }

    public void absoluteQuantity(Connection external, long userId, long menuItemId, int absoluteQty) throws Exception {
        boolean owns = external == null;
        Connection cn = owns ? db.openConnection() : external;
        try {
            if (absoluteQty <= 0) {
                internalDelete(cn, userId, menuItemId);
                return;
            }
            String upsertAbsolute = """
                    INSERT INTO shopping_cart_items (user_id,menu_item_id,quantity)
                    VALUES (?,?,?)
                    ON DUPLICATE KEY UPDATE quantity = ?
                    """;
            try (PreparedStatement ps = cn.prepareStatement(upsertAbsolute)) {
                ps.setLong(1, userId);
                ps.setLong(2, menuItemId);
                ps.setInt(3, absoluteQty);
                ps.setInt(4, absoluteQty);
                ps.executeUpdate();
            }
        } finally {
            if (owns && cn != null) {
                cn.close();
            }
        }
    }

    public void deleteLine(Connection external, long userId, long menuItemId) throws Exception {
        boolean owns = external == null;
        Connection cn = owns ? db.openConnection() : external;
        try {
            internalDelete(cn, userId, menuItemId);
        } finally {
            if (owns && cn != null) {
                cn.close();
            }
        }
    }

    private static void internalDelete(Connection cn, long userId, long menuItemId) throws Exception {
        String sqlDel = """
                DELETE FROM shopping_cart_items
                WHERE user_id=? AND menu_item_id=?
                """;
        try (PreparedStatement ps = cn.prepareStatement(sqlDel)) {
            ps.setLong(1, userId);
            ps.setLong(2, menuItemId);
            ps.executeUpdate();
        }
    }

    /** Used after checkout succeeds inside transactional {@link Connection}. */
    public void purgeItemsForUser(Connection cn, long userId, List<Long> menuItemIds) throws Exception {
        if (menuItemIds.isEmpty()) {
            return;
        }
        StringBuilder sb = new StringBuilder("DELETE FROM shopping_cart_items WHERE user_id=? AND menu_item_id IN (");
        sb.append("?,".repeat(menuItemIds.size()));
        sb.setLength(sb.length() - 1);
        sb.append(')');
        try (PreparedStatement ps = cn.prepareStatement(sb.toString())) {
            ps.setLong(1, userId);
            int idx = 2;
            for (Long id : menuItemIds) {
                ps.setLong(idx++, id);
            }
            ps.executeUpdate();
        }
    }
}
