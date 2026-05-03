package com.campuseateries.controller;

import com.campuseateries.dao.OrderDao;
import com.campuseateries.model.DeliveryDraft;
import com.campuseateries.model.PaymentMethod;
import com.campuseateries.model.Restaurant;
import com.campuseateries.model.user.AbstractUser;
import com.campuseateries.model.user.AdminAccount;
import com.campuseateries.model.user.StudentUser;
import com.campuseateries.model.UserRole;
import com.campuseateries.service.AnalyticsDashboardService;
import com.campuseateries.service.AuthService;
import com.campuseateries.service.BusinessRuleException;
import com.campuseateries.service.CartOrchestrationService;
import com.campuseateries.service.IAuthService;
import com.campuseateries.service.OrderFulfillmentService;
import com.campuseateries.service.OrderReadService;
import com.campuseateries.service.PaymentOrchestrationService;
import com.campuseateries.service.PlacedOrderSnapshot;
import com.campuseateries.service.RestaurantCatalogService;
import com.campuseateries.model.payment.AbstractPayment;
import com.campuseateries.view.ConsoleView;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;

/**
 * Controller hub binding {@link Scanner} input to service collaborators while delegating rendering
 * to {@link ConsoleView}.
 */
public class MvcShellController {

    private final Scanner scanner;
    private final IAuthService auth = new AuthService();
    private final RestaurantCatalogService catalog = new RestaurantCatalogService();
    private final CartOrchestrationService cart = new CartOrchestrationService();
    private final OrderFulfillmentService orders = new OrderFulfillmentService();
    private final OrderReadService readings = new OrderReadService();
    private final PaymentOrchestrationService payments = new PaymentOrchestrationService();
    private final AnalyticsDashboardService dashboards = new AnalyticsDashboardService();

    private AbstractUser session;

    public MvcShellController(Scanner scanner) {
        this.scanner = scanner;
    }

    public void run() throws Exception {
        ConsoleView.splash();
        boolean running = true;
        while (running) {
            if (session == null) {
                running = anonymousMenu();
            } else if (session.getRole() == UserRole.ADMIN) {
                running = adminLoop((AdminAccount) session);
            } else {
                running = studentLoop((StudentUser) session);
            }
        }
        System.out.println("Shutdown complete.");
    }

    private boolean anonymousMenu() {
        printAnonymousMenu();
        int choice = readInt("Pick> ");
        try {
            switch (choice) {
                case 1 -> registerFlow();
                case 2 -> loginFlow();
                case 3 -> browsePublicCatalog();
                case 4 -> demoTip();
                case 9 -> {
                    return false;
                }
                default -> System.out.println("Unknown selection.");
            }
        } catch (BusinessRuleException ex) {
            System.out.println("[rule] " + ex.getMessage());
        } catch (Exception ex) {
            System.err.println("[error] " + ex.getMessage());
        }
        return true;
    }

    private void registerFlow() throws Exception {
        String email = prompt("student email>");
        String full = prompt("full name>");
        String phone = prompt("phone>");
        String pass = promptSecret("password>");
        AbstractUser newbie = auth.registerStudent(email, pass, full, phone);
        System.out.printf("Registered %s%n", newbie.getEmail());
    }

    private void loginFlow() {
        String email = prompt("email>");
        String pass = promptSecret("password>");
        Optional<AbstractUser> authed = auth.login(email, pass);
        if (authed.isEmpty()) {
            System.out.println("Invalid credentials.");
            return;
        }
        session = authed.get();
        System.out.println("Welcome, " + session.getFullName() + "!");
    }

    private void browsePublicCatalog() throws Exception {
        List<Restaurant> rests = catalog.activeRestaurants();
        ConsoleView.printRestaurantList(rests);
    }

    private void demoTip() {
        System.out.println("""
                
                DEMO credentials (after seed SQL):
                  Student: alice.student@campus.edu / password123
                  Admin:   admin.ops@campus.edu      / password123
                
                Stored routines:
                  CALL sp_simulate_pay_order(...)
                  CALL sp_report_restaurant_daily_revenue(...)
                  VIEW   v_restaurant_performance
                
                Ensure src/main/resources/db.properties mirrors your MySQL host.
                
                """);
    }

    private boolean studentLoop(StudentUser student) {
        ConsoleView.printRestaurantList(emptyIfError(() -> catalog.activeRestaurants()));
        studentMenuBanner(student);
        int choice = readInt("student> ");
        try {
            switch (choice) {
                case 1 -> browseMenuFlow();
                case 2 -> cartFlow(student.getUserId());
                case 3 -> checkoutFlow(student);
                case 4 -> payOutstandingFlow(student.getUserId());
                case 5 -> historyFlow(student.getUserId());
                case 6 -> trackFlow(student.getUserId());
                case 9 -> logout();
                default -> System.out.println("Unknown selection.");
            }
        } catch (BusinessRuleException ex) {
            System.out.println("[rule] " + ex.getMessage());
        } catch (Exception ex) {
            System.err.println("[error] " + ex.getMessage());
        }
        return true;
    }

    private void browseMenuFlow() throws Exception {
        long rid = readLong("restaurant id> ");
        var menu = catalog.menuForRestaurant(rid, false);
        ConsoleView.printMenu(menu);
    }

    private void cartFlow(long userId) throws Exception {
        boolean stay = true;
        while (stay) {
            var lines = cart.summarize(userId);
            ConsoleView.printCart(lines);
            CartOrchestrationService.CartTotals totals = cart.aggregate(lines);
            System.out.printf("(Subtotal $%.2f | Est. tax $%.2f | Grand $%.2f)%n%n",
                    totals.subtotal(), totals.tax(), totals.grandTotal());
            System.out.println("1 add | 2 set qty | 3 remove | 9 back");
            int c = readInt("cart> ");
            switch (c) {
                case 1 -> {
                    long mid = readLong("menu_item_id> ");
                    int qty = readInt("qty> ");
                    cart.add(userId, mid, qty);
                }
                case 2 -> {
                    long mid = readLong("menu_item_id> ");
                    int qty = readInt("new absolute qty> ");
                    cart.updateQuantityAbsolute(userId, mid, qty);
                }
                case 3 -> {
                    long mid = readLong("menu_item_id> ");
                    cart.remove(userId, mid);
                }
                case 9 -> stay = false;
                default -> System.out.println("Unknown.");
            }
        }
    }

    private void checkoutFlow(StudentUser student) throws Exception {
        var lines = cart.summarize(student.getUserId());
        ConsoleView.printCart(lines);
        if (lines.isEmpty()) {
            System.out.println("Nothing to checkout.");
            return;
        }
        DeliveryDraft draft = new DeliveryDraft();
        draft.setBuildingCode(prompt("building code e.g. LIB> "));
        draft.setRoomNumber(prompt("room (optional)> "));
        draft.setInstructions(prompt("runner notes> "));
        PaymentMethod hint = pickPaymentMethod();
        PlacedOrderSnapshot snap = orders.commitCartToOrder(student.getUserId(), draft, hint);
        System.out.printf("Order #%d placed. Total $%.2f (awaiting settlement).%n",
                snap.orderId(), snap.total().doubleValue());
        autoSettlePrompt(snap.orderId(), snap.total());
    }

    private void payOutstandingFlow(long userId) throws Exception {
        long oid = readLong("order id to settle> ");
        var detail = readings.trackStudentOrder(userId, oid);
        if (detail.isEmpty()) {
            System.out.println("Order not found for this account.");
            return;
        }
        var header = detail.get().summary();
        if (header.getPaymentStatus() != null && header.getPaymentStatus().name().equals("COMPLETED")) {
            System.out.println("Already paid.");
            return;
        }
        autoSettlePrompt(oid, header.getTotal());
    }

    private void autoSettlePrompt(long orderId, BigDecimal total) throws Exception {
        PaymentMethod method = pickPaymentMethod();
        AbstractPayment channel = payments.buildSimulator(method, total, "4242");
        boolean ok = payments.settle(orderId, channel);
        System.out.println(ok ? "Payment simulated successfully (triggers advanced order)." : "Payment failed.");
    }

    private void historyFlow(long userId) throws Exception {
        ConsoleView.printOrders(readings.studentHistory(userId));
    }

    private void trackFlow(long userId) throws Exception {
        long oid = readLong("order id> ");
        Optional<OrderDao.OrderDetailBundle> bundle = readings.trackStudentOrder(userId, oid);
        if (bundle.isEmpty()) {
            System.out.println("Not found.");
        } else {
            ConsoleView.printOrderDetail(bundle.get());
        }
    }

    private boolean adminLoop(AdminAccount admin) throws Exception {
        ConsoleView.adminHeader(admin);
        System.out.println("1 KPI rollup | 2 top sellers | 3 heavy users | 4 live statuses | "
                + "5 daily revenue date | 6 live order tape | 9 logout");
        int choice = readInt("admin> ");
        switch (choice) {
            case 1 -> ConsoleView.restaurantRollup(dashboards.restaurantPerformanceRollup());
            case 2 -> ConsoleView.renderTopSelling(dashboards.topSelling(5, 30));
            case 3 -> ConsoleView.renderActiveUsers(dashboards.mostActiveUsers(5));
            case 4 -> {
                Map<String, Long> statuses = dashboards.recentStatusCounts(72);
                System.out.println("-- Live status funnel (stored proc) ----------------------------");
                statuses.forEach((k, v) -> System.out.println(k + " => " + v));
                System.out.println();
            }
            case 5 -> {
                LocalDate day = LocalDate.parse(prompt("yyyy-MM-dd> "));
                BigDecimal amt = dashboards.dailyPaidTotals(day);
                System.out.printf("Paid revenue snapshot: $%.2f on %s%n%n", amt.doubleValue(), day);
            }
            case 6 -> ConsoleView.printOrders(readings.adminRecentTape(10));
            case 9 -> logout();
            default -> System.out.println("Unknown.");
        }
        return true;
    }

    private void logout() {
        session = null;
        System.out.println("Logged out.");
    }

    private void studentMenuBanner(StudentUser s) {
        System.out.printf("%n<<< Student cockpit | %s >>>%n", s.getFullName());
        System.out.println("1 browse menu | 2 cart ops | 3 checkout | "
                + "4 pay pending | 5 history | 6 track detail | 9 logout");
    }

    private void printAnonymousMenu() {
        System.out.println("""
                
                Anonymous menu -------------------------------------------------
                [1] register student
                [2] login
                [3] browse eateries (catalog)
                [4] readme tips + credentials reminder
                [9] quit
                """);
    }

    private PaymentMethod pickPaymentMethod() {
        System.out.println("Payment channel: [1 CARD_SIM] [2 DIGITAL_SIM] [3 COD_SIM]");
        int c = readInt("choice> ");
        return switch (c) {
            case 2 -> PaymentMethod.DIGITAL_SIM;
            case 3 -> PaymentMethod.COD_SIM;
            default -> PaymentMethod.CARD_SIM;
        };
    }

    private int readInt(String label) {
        System.out.print(label);
        if (!scanner.hasNextInt()) {
            scanner.nextLine();
            return -1;
        }
        int v = scanner.nextInt();
        scanner.nextLine();
        return v;
    }

    private long readLong(String label) {
        System.out.print(label);
        if (!scanner.hasNextLong()) {
            scanner.nextLine();
            return -1L;
        }
        long v = scanner.nextLong();
        scanner.nextLine();
        return v;
    }

    private String prompt(String label) {
        System.out.print(label);
        return scanner.nextLine().strip();
    }

    private String promptSecret(String label) {
        System.out.print(label);
        return scanner.nextLine();
    }

    private List<Restaurant> emptyIfError(java.util.concurrent.Callable<List<Restaurant>> op) {
        try {
            return op.call();
        } catch (Exception e) {
            System.err.println("Unable to list restaurants yet: configure JDBC / run seed SQL?");
            return List.of();
        }
    }
}
