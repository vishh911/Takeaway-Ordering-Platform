package com.campuseateries.dao;

import com.campuseateries.database.DbConnectionManager;
import com.campuseateries.model.AnalyticsRestaurantRow;
import com.campuseateries.model.MostActiveUserRow;
import com.campuseateries.model.TopMenuItemRow;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Executes reporting SQL (JOIN/aggregation + stored routines) powering admin KPIs. */
public class AnalyticsDao {

    private final DbConnectionManager db = DbConnectionManager.getInstance();

    public List<TopMenuItemRow> fetchTopSellingMenuItems(int topN, int trailingDays) throws Exception {
        String sql = """
                SELECT mi.menu_item_id,
                       mi.item_name,
                       r.name AS restaurant_name,
                       SUM(oi.quantity) AS units_sold,
                       ROUND(SUM(oi.line_total), 2) AS revenue
                FROM order_items oi
                JOIN orders o ON o.order_id = oi.order_id
                JOIN menu_items mi ON mi.menu_item_id = oi.menu_item_id
                JOIN restaurants r ON r.restaurant_id = mi.restaurant_id
                LEFT JOIN payments py ON py.order_id = o.order_id AND py.status='COMPLETED'
                WHERE py.payment_id IS NOT NULL
                  AND o.placed_at >= (CURRENT_TIMESTAMP - INTERVAL ? DAY)
                GROUP BY mi.menu_item_id, mi.item_name, r.name
                ORDER BY units_sold DESC, revenue DESC
                LIMIT ?
                """;
        try (Connection cn = db.openConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, trailingDays);
            ps.setInt(2, topN);
            try (ResultSet rs = ps.executeQuery()) {
                List<TopMenuItemRow> rows = new ArrayList<>();
                while (rs.next()) {
                    TopMenuItemRow row = new TopMenuItemRow();
                    row.setMenuItemId(rs.getLong("menu_item_id"));
                    row.setItemName(rs.getString("item_name"));
                    row.setRestaurantName(rs.getString("restaurant_name"));
                    row.setUnitsSold(rs.getLong("units_sold"));
                    row.setRevenue(rs.getBigDecimal("revenue").setScale(2, RoundingMode.HALF_UP));
                    rows.add(row);
                }
                return rows;
            }
        }
    }

    public List<MostActiveUserRow> fetchMostActiveUsers(int topN) throws Exception {
        String sql = """
                SELECT u.user_id,u.full_name,u.email,COUNT(DISTINCT o.order_id) AS completed_orders
                FROM users u
                JOIN orders o ON o.user_id=u.user_id
                JOIN payments py ON py.order_id=o.order_id AND py.status='COMPLETED'
                GROUP BY u.user_id,u.full_name,u.email
                ORDER BY completed_orders DESC
                LIMIT ?
                """;
        try (Connection cn = db.openConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, topN);
            try (ResultSet rs = ps.executeQuery()) {
                ArrayList<MostActiveUserRow> rows = new ArrayList<>();
                while (rs.next()) {
                    MostActiveUserRow row = new MostActiveUserRow();
                    row.setUserId(rs.getLong("user_id"));
                    row.setFullName(rs.getString("full_name"));
                    row.setEmail(rs.getString("email"));
                    row.setCompletedOrders(rs.getLong("completed_orders"));
                    rows.add(row);
                }
                return rows;
            }
        }
    }

    public List<AnalyticsRestaurantRow> restaurantPerformanceViaView() throws Exception {
        String sql = """
                SELECT restaurant_id,
                       restaurant_name,
                       lifetime_orders,
                       lifetime_units_sold,
                       lifetime_paid_amount
                FROM v_restaurant_performance
                ORDER BY lifetime_paid_amount DESC
                """;
        try (Connection cn = db.openConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            ArrayList<AnalyticsRestaurantRow> rows = new ArrayList<>();
            while (rs.next()) {
                AnalyticsRestaurantRow row = new AnalyticsRestaurantRow();
                row.setRestaurantId(rs.getLong("restaurant_id"));
                row.setName(rs.getString("restaurant_name"));
                row.setLifetimeOrders(rs.getLong("lifetime_orders"));
                row.setLifetimeUnitsSold(rs.getBigDecimal("lifetime_units_sold"));
                BigDecimal amt = rs.getBigDecimal("lifetime_paid_amount");
                row.setLifetimePaidAmount(
                        amt == null ? BigDecimal.ZERO : amt.setScale(2, RoundingMode.HALF_UP));
                rows.add(row);
            }
            return rows;
        }
    }

    public Map<String, Long> realTimeStatuses(int trailingHours) throws Exception {
        Map<String, Long> statusCounts = new LinkedHashMap<>();
        try (Connection cn = db.openConnection();
             CallableStatement cs = cn.prepareCall("{CALL sp_analytics_order_insights(?)}")) {
            cs.setInt(1, trailingHours);
            cs.execute();
            try (ResultSet rs = cs.getResultSet()) {
                if (rs != null) {
                    while (rs.next()) {
                        statusCounts.put(rs.getString("status"), rs.getLong("order_count"));
                    }
                }
            }
        }
        return statusCounts;
    }

    public BigDecimal summarizeDailyPaidRevenue(LocalDate calendarDay) throws Exception {
        String sql = """
                SELECT ROUND(IFNULL(SUM(py.amount),0),2)
                FROM orders o
                JOIN payments py ON py.order_id=o.order_id AND py.status='COMPLETED'
                WHERE DATE(o.placed_at)=?
                """;
        try (Connection cn = db.openConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(calendarDay));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBigDecimal(1).setScale(2, RoundingMode.HALF_UP);
            }
        }
    }
}
