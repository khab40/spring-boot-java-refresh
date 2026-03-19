package com.example.springbootjavarefresh.service;

import com.example.springbootjavarefresh.entity.User;

public interface VerificationEmailService {
    void sendVerificationEmail(User user, String verificationToken);
}
