package com.campuseateries.service;

import com.campuseateries.dao.OrderDao;
import com.campuseateries.model.CustomerOrderSummary;

import java.util.List;
import java.util.Optional;

/** Lightweight read façade keeping controllers ignorant of JDBC detail while honoring MVC layering. */
public class OrderReadService {

    private final OrderDao orders = new OrderDao();

    public List<CustomerOrderSummary> studentHistory(long userId) throws Exception {
        return orders.fetchHistory(userId);
    }

    public Optional<OrderDao.OrderDetailBundle> trackStudentOrder(long studentId, long orderId)
            throws Exception {
        return orders.locateOrderForStudent(studentId, orderId);
    }

    public List<CustomerOrderSummary> adminRecentTape(int rows) throws Exception {
        return orders.fetchLatestGlobal(rows);
    }
}
