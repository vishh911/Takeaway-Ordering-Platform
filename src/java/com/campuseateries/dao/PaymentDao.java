package com.campuseateries.dao;

import com.campuseateries.database.DbConnectionManager;
import com.campuseateries.model.PaymentMethod;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Types;

/** Bridges JDBC {@link CallableStatement} usage to transactional MySQL routines. */
public class PaymentDao {

    private final DbConnectionManager db = DbConnectionManager.getInstance();

    public boolean simulatePaymentGateway(long orderId, PaymentMethod method) throws Exception {
        try (Connection cn = db.openConnection();
             CallableStatement cs = cn.prepareCall("{call sp_simulate_pay_order(?,?,?)}")) {
            cs.setLong(1, orderId);
            cs.setString(2, method.name());
            cs.registerOutParameter(3, Types.TINYINT);
            cs.execute();
            return cs.getByte(3) != 0;
        }
    }
}
