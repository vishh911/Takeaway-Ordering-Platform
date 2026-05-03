package com.campuseateries.dao;

import com.campuseateries.database.DbConnectionManager;
import com.campuseateries.model.CartLine;
import com.campuseateries.model.CustomerOrderSummary;
import com.campuseateries.model.DeliveryDraft;
import com.campuseateries.model.OrderLineDraft;
import com.campuseateries.model.OrderStatus;
import com.campuseateries.model.PaymentMethod;
import com.campuseateries.model.PaymentStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Aggregates persistence for orders/lines plus lightweight dashboard projections. */
public class OrderDao {

    private final DbConnectionManager db = DbConnectionManager.getInstance();

    public List<CustomerOrderSummary> fetchHistory(long userId) throws Exception {
        String sql = """
                SELECT o.order_id,
                       o.status,
                       o.total_amount,
                       o.placed_at,
                       r.name AS restaurant_name,
                       py.status AS pay_status,
                       py.payment_method
                FROM orders o
                JOIN restaurants r ON r.restaurant_id = o.restaurant_id
                LEFT JOIN payments py ON py.order_id = o.order_id
                WHERE o.user_id = ?
                ORDER BY o.order_id DESC
                LIMIT 200
                """;

        try (Connection cn = db.openConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                ArrayList<CustomerOrderSummary> rows = new ArrayList<>();
                while (rs.next()) {
                    rows.add(mapSummary(rs));
                }
                return rows;
            }
        }
    }

    public List<CustomerOrderSummary> fetchLatestGlobal(int limit) throws Exception {
        String sql = """
                SELECT o.order_id,
                       o.status,
                       o.total_amount,
                       o.placed_at,
                       r.name AS restaurant_name,
                       py.status AS pay_status,
                       py.payment_method,
                       u.email AS diner_email,
                       u.full_name AS diner_name
                FROM orders o
                JOIN restaurants r ON r.restaurant_id = o.restaurant_id
                JOIN users u ON u.user_id = o.user_id
                LEFT JOIN payments py ON py.order_id = o.order_id
                ORDER BY o.order_id DESC
                LIMIT ?
                """;

        try (Connection cn = db.openConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                ArrayList<CustomerOrderSummary> rows = new ArrayList<>();
                while (rs.next()) {
                    CustomerOrderSummary summary = mapSummary(rs);
                    String diner = rs.getString("diner_name") + " <" + rs.getString("diner_email") + ">";
                    summary.setRestaurantName(summary.getRestaurantName() + " | " + diner);
                    rows.add(summary);
                }
                return rows;
            }
        }
    }

    public Optional<OrderDetailBundle> locateOrderForStudent(long studentId, long orderId) throws Exception {
        String sql = """
                SELECT o.order_id,
                       o.status,
                       o.total_amount,
                       o.placed_at,
                       r.name AS restaurant_name,
                       py.status AS pay_status,
                       py.payment_method
                FROM orders o
                JOIN restaurants r ON r.restaurant_id=o.restaurant_id
                LEFT JOIN payments py ON py.order_id=o.order_id
                WHERE o.order_id=? AND o.user_id=?
                """;

        try (Connection cn = db.openConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            ps.setLong(2, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                CustomerOrderSummary header = mapSummary(rs);
                return Optional.of(new OrderDetailBundle(header, fetchLines(orderId)));
            }
        }
    }

    public long insertPendingOrder(Connection cn, long userId, long restaurantId, BigDecimal subtotal,
                                   BigDecimal tax, BigDecimal total, OrderStatus status) throws Exception {
        String sql = """
                INSERT INTO orders (user_id, restaurant_id, status, subtotal, tax_amount, total_amount)
                VALUES (?,?,?,?,?,?)
                """;
        try (PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, userId);
            ps.setLong(2, restaurantId);
            ps.setString(3, status.name());
            ps.setBigDecimal(4, subtotal.setScale(2, RoundingMode.HALF_UP));
            ps.setBigDecimal(5, tax.setScale(2, RoundingMode.HALF_UP));
            ps.setBigDecimal(6, total.setScale(2, RoundingMode.HALF_UP));
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getLong(1);
            }
        }
    }

    public void insertLines(Connection cn, long orderId, List<CartLine> cartSnapshot) throws Exception {
        String sql = """
                INSERT INTO order_items (order_id, menu_item_id, quantity, snapshot_unit_price, line_total)
                VALUES (?,?,?,?,?)
                """;
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            for (CartLine cl : cartSnapshot) {
                BigDecimal lineTotal = cl.getUnitPrice().multiply(BigDecimal.valueOf(cl.getQuantity()))
                        .setScale(2, RoundingMode.HALF_UP);
                ps.setLong(1, orderId);
                ps.setLong(2, cl.getMenuItemId());
                ps.setInt(3, cl.getQuantity());
                ps.setBigDecimal(4, cl.getUnitPrice().setScale(2, RoundingMode.HALF_UP));
                ps.setBigDecimal(5, lineTotal);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public void insertDelivery(Connection cn, long orderId, DeliveryDraft draft) throws Exception {
        String sql = """
                INSERT INTO delivery_details (order_id, building_code, room_number,
                                              special_instructions, eta_minutes)
                VALUES (?,?,?,?,?)
                """;
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            ps.setString(2, draft.getBuildingCode());
            ps.setString(3, draft.getRoomNumber());
            ps.setString(4, draft.getInstructions());
            if (draft.getEtaMinutes() == null) {
                ps.setNull(5, Types.INTEGER);
            } else {
                ps.setInt(5, draft.getEtaMinutes());
            }
            ps.executeUpdate();
        }
    }

    public void insertPlaceholderPayment(Connection cn, long orderId, BigDecimal amount,
                                         PaymentMethod preferredMethodPlaceholder) throws Exception {
        String sql = """
                INSERT INTO payments (order_id, payment_method, amount, transaction_ref, status)
                VALUES (?,?,?,?,?)
                """;
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, orderId);
            ps.setString(2, preferredMethodPlaceholder.name());
            ps.setBigDecimal(3, amount.setScale(2, RoundingMode.HALF_UP));
            ps.setNull(4, Types.VARCHAR);
            ps.setString(5, PaymentStatus.INITIATED.name());
            ps.executeUpdate();
        }
    }

    /** Immutable MVC bundle projecting header + constituent order lines via JOIN-heavy SQL reuse. */
    public static final class OrderDetailBundle {
        private final CustomerOrderSummary summary;
        private final List<OrderLineDraft> lines;

        public OrderDetailBundle(CustomerOrderSummary summary, List<OrderLineDraft> lines) {
            this.summary = summary;
            this.lines = lines;
        }

        public CustomerOrderSummary summary() {
            return summary;
        }

        public List<OrderLineDraft> lines() {
            return lines;
        }
    }

    private List<OrderLineDraft> fetchLines(long orderId) throws Exception {
        String sqlLines = """
                SELECT oi.order_item_id,
                       mi.item_name,
                       oi.quantity,
                       oi.snapshot_unit_price
                FROM order_items oi
                JOIN menu_items mi ON mi.menu_item_id = oi.menu_item_id
                WHERE oi.order_id=?
                ORDER BY oi.order_item_id ASC
                """;
        ArrayList<OrderLineDraft> drafts = new ArrayList<>();
        try (Connection cn = db.openConnection(); PreparedStatement ps = cn.prepareStatement(sqlLines)) {
            ps.setLong(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    OrderLineDraft line = new OrderLineDraft();
                    line.setOrderItemId(rs.getLong("order_item_id"));
                    line.setMenuItemName(rs.getString("item_name"));
                    line.setQuantity(rs.getInt("quantity"));
                    line.setSnapshotUnitPrice(
                            rs.getBigDecimal("snapshot_unit_price").setScale(2, RoundingMode.HALF_UP));
                    drafts.add(line);
                }
            }
        }
        return drafts;
    }

    private static CustomerOrderSummary mapSummary(ResultSet rs) throws Exception {
        CustomerOrderSummary summary = new CustomerOrderSummary();
        summary.setOrderId(rs.getLong("order_id"));
        summary.setStatus(OrderStatus.parse(rs.getString("status")));
        summary.setTotal(rs.getBigDecimal("total_amount").setScale(2, RoundingMode.HALF_UP));
        Timestamp placed = rs.getTimestamp("placed_at");
        summary.setPlacedAt(placed == null ? null : placed.toLocalDateTime());
        summary.setRestaurantName(rs.getString("restaurant_name"));
        String payStatus = rs.getString("pay_status");
        summary.setPaymentStatus(payStatus == null ? null : PaymentStatus.parse(payStatus));
        String pm = rs.getString("payment_method");
        summary.setPaymentMethod(pm == null ? null : PaymentMethod.parse(pm));
        return summary;
    }
}
