package com.example.springbootjavarefresh.repository;

import com.example.springbootjavarefresh.entity.User;
import com.example.springbootjavarefresh.entity.AuthProvider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByAuthProviderAndProviderSubject(AuthProvider authProvider, String providerSubject);
}
