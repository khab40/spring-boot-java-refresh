package com.example.springbootjavarefresh.repository;

import com.example.springbootjavarefresh.entity.ApiKey;
import com.example.springbootjavarefresh.entity.ApiKeyStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApiKeyRepository extends JpaRepository<ApiKey, Long> {
    Optional<ApiKey> findByKeyHash(String keyHash);
    List<ApiKey> findByUserIdAndStatus(Long userId, ApiKeyStatus status);
}
