package com.example.springbootjavarefresh.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

import java.util.Arrays;

@Configuration
public class OAuth2ClientConfig {

    @Bean
    @Conditional(GoogleOAuthConfiguredCondition.class)
    public ClientRegistrationRepository clientRegistrationRepository(
            @Value("${app.auth.google.client-id}") String clientId,
            @Value("${app.auth.google.client-secret}") String clientSecret,
            @Value("${app.auth.google.scopes:openid,profile,email}") String scopes) {
        ClientRegistration google = ClientRegistration.withRegistrationId("google")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .scope(Arrays.stream(scopes.split(","))
                        .map(String::trim)
                        .filter(value -> !value.isBlank())
                        .toList())
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .userInfoUri("https://openidconnect.googleapis.com/v1/userinfo")
                .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .issuerUri("https://accounts.google.com")
                .userNameAttributeName("sub")
                .clientName("Google")
                .build();

        return new InMemoryClientRegistrationRepository(google);
    }
}
