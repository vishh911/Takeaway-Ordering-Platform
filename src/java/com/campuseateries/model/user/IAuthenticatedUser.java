package com.campuseateries.model.user;

import com.campuseateries.model.UserRole;

/**
 * Encapsulates shared identity primitives (interface segregation principle for auth flows).
 */
public interface IAuthenticatedUser {

    long getUserId();

    String getEmail();

    UserRole getRole();

    String getFullName();

    boolean isActive();
}
