package com.campuseateries.view;

import com.campuseateries.dao.OrderDao;
import com.campuseateries.model.AnalyticsRestaurantRow;
import com.campuseateries.model.CartLine;
import com.campuseateries.model.CustomerOrderSummary;
import com.campuseateries.model.MenuItem;
import com.campuseateries.model.MostActiveUserRow;
import com.campuseateries.model.Restaurant;
import com.campuseateries.model.TopMenuItemRow;
import com.campuseateries.model.user.AdminAccount;

import java.util.List;

/** Presentation helpers that keep scanners out of controllers (View half of MVC). */
public final class ConsoleView {

    private ConsoleView() {
    }

    public static void splash() {
        System.out.println();
        System.out.println("=== Campus Eateries | Takeaway & Admin Console =================");
        System.out.println("MVC layering: Model <-- Service/DAO <-- Controller <-- View prompts");
        System.out.println();
    }

    public static void printRestaurantList(List<Restaurant> rows) {
        System.out.println("-- Active restaurants ------------------------------------------");
        for (Restaurant r : rows) {
            System.out.printf(" %d | %s | Zone: %s | %s%n",
                    r.getRestaurantId(), r.getName(), r.getCampusZone(),
                    truncated(r.getDescription(), 72));
        }
        System.out.println();
    }

    public static void printMenu(List<MenuItem> menus) {
        System.out.println("-- Menu --------------------------------------------------------");
        for (MenuItem m : menus) {
            System.out.printf(" [%d] %s | %.2f USD | #%d stock | Cat: %s | Vendor: %s%n",
                    m.getMenuItemId(),
                    m.getItemName(),
                    m.getUnitPrice().doubleValue(),
                    m.getStockQuantity(),
                    m.getCategoryName(),
                    m.getRestaurantNameSnapshot());
        }
        System.out.println();
    }

    public static void printCart(List<CartLine> lines) {
        if (lines.isEmpty()) {
            System.out.println("(Cart empty)");
            System.out.println();
            return;
        }
        System.out.println("-- Cart -------------------------------------------------------");
        for (CartLine cl : lines) {
            System.out.printf(" entry %d | %s × %d | line %.2f | vendor %s%n",
                    cl.getCartEntryId(),
                    cl.getItemName(),
                    cl.getQuantity(),
                    cl.lineSubtotal().doubleValue(),
                    cl.getRestaurantName());
        }
        System.out.println();
    }

    public static void printOrders(List<CustomerOrderSummary> history) {
        System.out.println("-- Order history ----------------------------------------------");
        for (CustomerOrderSummary h : history) {
            System.out.printf(" #%d | %s | %-18s | $%.2f | pay=%s|%s%n",
                    h.getOrderId(),
                    h.getPlacedAt(),
                    h.getStatus(),
                    h.getTotal().doubleValue(),
                    h.getPaymentStatus(),
                    h.getPaymentMethod());
            System.out.println("        " + h.getRestaurantName());
        }
        System.out.println();
    }

    public static void printOrderDetail(OrderDao.OrderDetailBundle bundle) {
        CustomerOrderSummary s = bundle.summary();
        System.out.println("-- Detail -----------------------------------------------------");
        System.out.printf("#%d restaurant=%s placed=%s%n",
                s.getOrderId(),
                s.getRestaurantName(),
                s.getPlacedAt());
        System.out.printf("Status=%s | Total=$%.2f | Pay=%s via %s%n",
                s.getStatus(),
                s.getTotal().doubleValue(),
                s.getPaymentStatus(),
                s.getPaymentMethod());
        System.out.println("Lines:");
        bundle.lines().forEach(line ->
                System.out.printf(" • %s × %d @ $%.2f%n",
                        line.getMenuItemName(),
                        line.getQuantity(),
                        line.getSnapshotUnitPrice()));
        System.out.println();
    }

    public static void adminHeader(AdminAccount admin) {
        System.out.printf("%n<<< Admin cockpit | %s | dept=%s lvl=%d >>>%n%n",
                admin.getFullName(),
                admin.getDepartment(),
                admin.getClearanceLevel());
    }

    public static void renderTopSelling(List<TopMenuItemRow> rows) {
        System.out.println("-- Top sellers (JOIN + aggregates) -------------------------------");
        for (TopMenuItemRow r : rows) {
            System.out.printf("%s (%s) | units=%d | revenue $%.2f%n",
                    r.getItemName(), r.getRestaurantName(), r.getUnitsSold(),
                    r.getRevenue().doubleValue());
        }
        System.out.println();
    }

    public static void renderActiveUsers(List<MostActiveUserRow> rows) {
        System.out.println("-- Most loyal diners ---------------------------------------------");
        for (MostActiveUserRow r : rows) {
            System.out.printf("%s <%s> | completed orders=%d%n",
                    r.getFullName(), r.getEmail(), r.getCompletedOrders());
        }
        System.out.println();
    }

    public static void restaurantRollup(List<AnalyticsRestaurantRow> rows) {
        System.out.println("-- Restaurant performance VIEW -----------------------------------");
        for (AnalyticsRestaurantRow r : rows) {
            System.out.printf("%s | orders=%d | units≈%.0f | paid $%.2f%n",
                    r.getName(), r.getLifetimeOrders(),
                    r.getLifetimeUnitsSold().doubleValue(),
                    r.getLifetimePaidAmount().doubleValue());
        }
        System.out.println();
    }

    private static String truncated(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max - 3) + "...";
    }
}
