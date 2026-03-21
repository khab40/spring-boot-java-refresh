package com.example.springbootjavarefresh.repository;

import com.example.springbootjavarefresh.entity.DataDelivery;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DataDeliveryRepository extends JpaRepository<DataDelivery, Long> {
    List<DataDelivery> findAllByUserIdOrderByCreatedAtDesc(Long userId);
}
