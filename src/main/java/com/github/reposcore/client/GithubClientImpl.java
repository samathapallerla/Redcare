package com.github.reposcore.client;

import com.github.reposcore.exception.GithubApiException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class GithubClientImpl implements GithubClient {

    private static final Logger log = LoggerFactory.getLogger(GithubClientImpl.class);

    private final RestClient restClient;

    public GithubClientImpl(@Value("${github.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.ACCEPT, "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2026-03-10")
                .build();
        log.info("GithubClient initialised with base URL: {}", baseUrl);
    }

    @Override
    @CircuitBreaker(name = "githubClient", fallbackMethod = "fallback")
    public GithubClient.SearchResponse searchRepositories(String query) {
        log.info("Calling GitHub API with query: {}", query);
        var response = restClient.get()
                .uri(uri -> uri.path("/search/repositories")
                        .queryParam("q", query)
                        .build())
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    log.error("GitHub client error: {}", res.getStatusCode());
                    throw new GithubApiException("GitHub client error: " + res.getStatusCode(), res.getStatusCode().value());
                })
                .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                    log.error("GitHub server error: {}", res.getStatusCode());
                    throw new GithubApiException("GitHub server error: " + res.getStatusCode(), res.getStatusCode().value());
                })
                .body(GithubClient.SearchResponse.class);

        log.info("GitHub API returned {} items", response != null && response.items() != null ? response.items().size() : 0);
        return response;
    }

    private GithubClient.SearchResponse fallback(String query, Exception ex) {
        log.error("Circuit breaker open for query '{}': {}", query, ex.getMessage());
        throw new GithubApiException("GitHub API unavailable: circuit breaker open", 503);
    }
}