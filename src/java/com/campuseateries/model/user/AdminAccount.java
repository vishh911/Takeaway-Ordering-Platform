package com.campuseateries.model.user;

/** Mirrors optional admin_profiles table information for MVC reporting surfaces. */
public class AdminAccount extends AbstractUser {

    private String department;
    private int clearanceLevel;

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public int getClearanceLevel() {
        return clearanceLevel;
    }

    public void setClearanceLevel(int clearanceLevel) {
        this.clearanceLevel = clearanceLevel;
    }

    @Override
    public boolean canManageCatalog() {
        return clearanceLevel >= 1;
    }
}
