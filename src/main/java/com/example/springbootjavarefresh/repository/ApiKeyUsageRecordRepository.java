package com.example.springbootjavarefresh.repository;

import com.example.springbootjavarefresh.entity.ApiKeyUsageRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApiKeyUsageRecordRepository extends JpaRepository<ApiKeyUsageRecord, Long> {
    List<ApiKeyUsageRecord> findByUserId(Long userId);
    List<ApiKeyUsageRecord> findByApiKeyId(Long apiKeyId);
}
