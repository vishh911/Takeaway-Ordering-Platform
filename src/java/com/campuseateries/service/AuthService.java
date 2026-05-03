package com.campuseateries.service;

import com.campuseateries.dao.UserDao;
import com.campuseateries.database.DbConnectionManager;
import com.campuseateries.model.user.AbstractUser;
import com.campuseateries.util.PasswordHasher;

import java.sql.Connection;
import java.util.Optional;

/** Authentication + lightweight registration façade (delegates hashing + uniqueness rules). */
public class AuthService implements IAuthService {

    private final UserDao userDao = new UserDao();
    private final DbConnectionManager db = DbConnectionManager.getInstance();

    @Override
    public Optional<AbstractUser> login(String email, String rawPassword) {
        try {
            return userDao.authenticate(email, PasswordHasher.sha256Hex(rawPassword));
        } catch (Exception e) {
            throw new RuntimeException("Login lookup failed", e);
        }
    }

    @Override
    public AbstractUser registerStudent(String email, String rawPassword, String fullName, String phone)
            throws BusinessRuleException, Exception {
        if (email == null || email.isBlank()) {
            throw new BusinessRuleException("Email is required.");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new BusinessRuleException("Full name is required.");
        }
        try (Connection cn = db.openConnection()) {
            cn.setAutoCommit(false);
            try {
                if (userDao.emailExists(cn, email)) {
                    throw new BusinessRuleException("Email already registered.");
                }
                AbstractUser newbie = userDao.insertStudent(cn,
                        email,
                        PasswordHasher.sha256Hex(rawPassword),
                        fullName.strip(),
                        phone == null ? null : phone.strip());
                cn.commit();
                return newbie;
            } catch (Exception ex) {
                cn.rollback();
                throw ex;
            }
        }
    }

    @Override
    public Optional<AbstractUser> refresh(long userId) throws Exception {
        return userDao.findById(userId);
    }
}
