package com.campuseateries.service;

import com.campuseateries.model.user.AbstractUser;

import java.util.Optional;

public interface IAuthService {

    Optional<AbstractUser> login(String email, String rawPassword);

    AbstractUser registerStudent(String email, String rawPassword, String fullName,
                                 String phone) throws BusinessRuleException, Exception;

    Optional<AbstractUser> refresh(long userId) throws Exception;
}
