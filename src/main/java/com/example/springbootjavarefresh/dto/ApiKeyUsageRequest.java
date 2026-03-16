package com.example.springbootjavarefresh.dto;

import com.example.springbootjavarefresh.entity.UsageType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class ApiKeyUsageRequest {

    @NotBlank
    private String apiKey;

    @NotNull
    private Long productId;

    @NotNull
    private UsageType usageType;

    @NotNull
    @Min(1)
    private Integer requestCount = 1;

    private BigDecimal megabytesUsed = BigDecimal.ZERO;

    private Long payloadKilobytesUsed = 0L;

    private Integer realtimeSubscriptionsUsed = 0;

    private String notes;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public UsageType getUsageType() {
        return usageType;
    }

    public void setUsageType(UsageType usageType) {
        this.usageType = usageType;
    }

    public Integer getRequestCount() {
        return requestCount;
    }

    public void setRequestCount(Integer requestCount) {
        this.requestCount = requestCount;
    }

    public BigDecimal getMegabytesUsed() {
        return megabytesUsed;
    }

    public void setMegabytesUsed(BigDecimal megabytesUsed) {
        this.megabytesUsed = megabytesUsed;
    }

    public Long getPayloadKilobytesUsed() {
        return payloadKilobytesUsed;
    }

    public void setPayloadKilobytesUsed(Long payloadKilobytesUsed) {
        this.payloadKilobytesUsed = payloadKilobytesUsed;
    }

    public Integer getRealtimeSubscriptionsUsed() {
        return realtimeSubscriptionsUsed;
    }

    public void setRealtimeSubscriptionsUsed(Integer realtimeSubscriptionsUsed) {
        this.realtimeSubscriptionsUsed = realtimeSubscriptionsUsed;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
