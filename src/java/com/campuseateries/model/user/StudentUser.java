package com.campuseateries.model.user;

/** Domain-polymorphism branch for typical campus diner accounts. */
public class StudentUser extends AbstractUser {

    @Override
    public boolean canManageCatalog() {
        return false;
    }
}
