package com.example.springbootjavarefresh.service;

import com.example.springbootjavarefresh.dto.OtdDeliveryResponse;
import com.example.springbootjavarefresh.entity.User;

public interface DataDeliveryEmailService {
    void sendDeliveryEmail(User user, OtdDeliveryResponse deliveryResponse);
}
