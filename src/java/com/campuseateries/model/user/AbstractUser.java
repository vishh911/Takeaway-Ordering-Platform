package com.campuseateries.model.user;

import com.campuseateries.model.UserRole;

/** Base abstraction for STUDENT vs ADMIN hierarchies enforcing encapsulation via getters/setters only. */
public abstract class AbstractUser implements IAuthenticatedUser {

    protected long userId;
    protected String email;
    protected String passwordHash;
    protected String fullName;
    protected String phone;
    protected UserRole role;
    protected boolean active;

    protected AbstractUser() {
    }

    @Override
    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    @Override
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    @Override
    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    @Override
    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public abstract boolean canManageCatalog();
}
