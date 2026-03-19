package com.example.springbootjavarefresh.repository;

import com.example.springbootjavarefresh.entity.EmailVerificationToken;
import com.example.springbootjavarefresh.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {
    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);
    List<EmailVerificationToken> findAllByUserAndUsedAtIsNull(User user);
}
