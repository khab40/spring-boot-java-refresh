package com.example.springbootjavarefresh.observability;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.time.Duration;

@Component
public class ExternalServiceProbe {

    private final HttpClient httpClient;
    private final String frontendHealthUrl;
    private final String airflowHealthUrl;

    public ExternalServiceProbe(
            @Value("${app.monitoring.frontend-health-url:http://frontend:3000/api/health}") String frontendHealthUrl,
            @Value("${app.monitoring.airflow-health-url:http://airflow:8080/api/v2/monitor/health}") String airflowHealthUrl,
            @Value("${app.monitoring.external-timeout-ms:2000}") long timeoutMillis) {
        this.frontendHealthUrl = frontendHealthUrl;
        this.airflowHealthUrl = airflowHealthUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeoutMillis))
                .followRedirects(Redirect.NORMAL)
                .version(Version.HTTP_1_1)
                .build();
    }

    public ServiceStatus probeFrontendUi() {
        return probe("frontend-ui", frontendHealthUrl);
    }

    public ServiceStatus probeAirflow() {
        return probe("airflow", airflowHealthUrl);
    }

    private ServiceStatus probe(String service, String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .GET()
                    .timeout(httpClient.connectTimeout().orElse(Duration.ofSeconds(2)))
                    .header("Accept", "application/json")
                    .header("User-Agent", "market-data-lake-health-probe/1.0")
                    .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            boolean up = response.statusCode() >= 200 && response.statusCode() < 300;
            return new ServiceStatus(service, up, url, response.statusCode(), up ? "reachable" : "unexpected status");
        } catch (IOException | InterruptedException | IllegalArgumentException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return new ServiceStatus(service, false, url, 0, exception.getClass().getSimpleName() + ": " + exception.getMessage());
        }
    }
}
