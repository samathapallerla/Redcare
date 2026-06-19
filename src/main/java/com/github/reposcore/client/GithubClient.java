package com.github.reposcore.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.List;

public interface GithubClient {

    SearchResponse searchRepositories(String query);

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SearchResponse(
            @JsonProperty("total_count")        int totalCount,
            @JsonProperty("incomplete_results") boolean incompleteResults,
            List<GitRepository> items) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GitRepository(
            long id,
            String name,
            @JsonProperty("full_name")        String fullName,
            String description,
            @JsonProperty("html_url")         String htmlUrl,
            @JsonProperty("stargazers_count") int stargazersCount,
            @JsonProperty("forks_count")      int forksCount,
            @JsonProperty("updated_at")       OffsetDateTime updatedAt,
            @JsonProperty("created_at")       OffsetDateTime createdAt,
            String language) {}
}