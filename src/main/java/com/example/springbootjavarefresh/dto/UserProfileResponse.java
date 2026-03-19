package com.example.springbootjavarefresh.dto;

import com.example.springbootjavarefresh.entity.User;
import com.example.springbootjavarefresh.entity.AuthProvider;
import com.example.springbootjavarefresh.entity.UserRole;

import java.time.LocalDateTime;

public record UserProfileResponse(
        Long id,
        String email,
        String firstName,
        String lastName,
        String company,
        String country,
        String phoneNumber,
        UserRole role,
        AuthProvider authProvider,
        boolean emailVerified,
        LocalDateTime createdAt,
        LocalDateTime emailVerifiedAt) {

    public static UserProfileResponse fromUser(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getCompany(),
                user.getCountry(),
                user.getPhoneNumber(),
                user.getRole(),
                user.getAuthProvider(),
                user.isEmailVerified(),
                user.getCreatedAt(),
                user.getEmailVerifiedAt()
        );
    }
}
