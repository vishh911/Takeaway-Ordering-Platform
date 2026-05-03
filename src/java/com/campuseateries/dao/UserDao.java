package com.campuseateries.dao;

import com.campuseateries.database.DbConnectionManager;
import com.campuseateries.model.UserRole;
import com.campuseateries.model.user.AbstractUser;
import com.campuseateries.model.user.AdminAccount;
import com.campuseateries.model.user.StudentUser;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Optional;

public class UserDao {

    private final DbConnectionManager db = DbConnectionManager.getInstance();

    public Optional<AbstractUser> authenticate(String email, String passwordHash) throws Exception {
        String sql = """
                SELECT u.user_id,u.email,u.password_hash,u.full_name,u.phone,u.role,u.is_active,
                       ap.admin_user_id AS ap_id, ap.department AS ap_department, ap.clearance_level AS ap_clearance
                FROM users u
                LEFT JOIN admin_profiles ap ON ap.admin_user_id = u.user_id
                WHERE u.email = ?
                LIMIT 1
                """;

        try (Connection cn = db.openConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, email.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                if (!passwordHash.equalsIgnoreCase(rs.getString("password_hash"))) {
                    return Optional.empty();
                }
                AbstractUser hydrated = hydrate(rs);
                return Optional.of(hydrated);
            }
        }
    }

    public boolean emailExists(Connection external, String email) throws Exception {
        String sql = "SELECT 1 FROM users WHERE email=? LIMIT 1";
        Connection cn = external != null ? external : db.openConnection();
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, email.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } finally {
            if (external == null && cn != null) {
                cn.close();
            }
        }
    }

    public AbstractUser insertStudent(Connection cn, String email, String pwdHash,
                                      String fullName, String phone) throws Exception {
        String sql = """
                INSERT INTO users (email, password_hash, full_name, phone, role, is_active)
                VALUES (?,?,?,?,?,1)
                """;
        try (PreparedStatement ps = cn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, email.trim().toLowerCase());
            ps.setString(2, pwdHash);
            ps.setString(3, fullName);
            ps.setString(4, phone);
            ps.setString(5, UserRole.STUDENT.name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                long userId = keys.getLong(1);
                StudentUser s = new StudentUser();
                s.setUserId(userId);
                s.setEmail(email.trim().toLowerCase());
                s.setPasswordHash(pwdHash);
                s.setFullName(fullName);
                s.setPhone(phone);
                s.setRole(UserRole.STUDENT);
                s.setActive(true);
                return s;
            }
        }
    }

    /** Fetch user by pk for dashboards that only store session id primitives. */
    public Optional<AbstractUser> findById(long userId) throws Exception {
        String sql = """
                SELECT u.user_id,u.email,u.password_hash,u.full_name,u.phone,u.role,u.is_active,
                       ap.admin_user_id AS ap_id, ap.department AS ap_department, ap.clearance_level AS ap_clearance
                FROM users u
                LEFT JOIN admin_profiles ap ON ap.admin_user_id = u.user_id
                WHERE u.user_id=?
                """;
        try (Connection cn = db.openConnection(); PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(hydrate(rs)) : Optional.empty();
            }
        }
    }

    private static AbstractUser hydrate(ResultSet rs) throws Exception {
        UserRole role = UserRole.parse(rs.getString("role"));
        switch (role) {
            case ADMIN -> {
                AdminAccount admin = new AdminAccount();
                fillCommon(rs, admin, role);
                if (rs.getObject("ap_id") != null) {
                    admin.setDepartment(rs.getString("ap_department"));
                    admin.setClearanceLevel(rs.getInt("ap_clearance"));
                }
                return admin;
            }
            default -> {
                StudentUser student = new StudentUser();
                fillCommon(rs, student, UserRole.STUDENT);
                return student;
            }
        }
    }

    private static void fillCommon(ResultSet rs, AbstractUser target, UserRole resolved) throws Exception {
        target.setUserId(rs.getLong("user_id"));
        target.setEmail(rs.getString("email"));
        target.setPasswordHash(rs.getString("password_hash"));
        target.setFullName(rs.getString("full_name"));
        target.setPhone(rs.getString("phone"));
        target.setRole(resolved);
        target.setActive(rs.getBoolean("is_active"));
    }
}
