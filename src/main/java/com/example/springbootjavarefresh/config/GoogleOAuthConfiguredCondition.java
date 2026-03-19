package com.example.springbootjavarefresh.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class GoogleOAuthConfiguredCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String clientId = context.getEnvironment().getProperty("app.auth.google.client-id");
        String clientSecret = context.getEnvironment().getProperty("app.auth.google.client-secret");
        return clientId != null && !clientId.isBlank() && clientSecret != null && !clientSecret.isBlank();
    }
}
