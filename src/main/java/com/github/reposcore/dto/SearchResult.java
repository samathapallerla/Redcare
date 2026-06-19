package com.github.reposcore.dto;

import com.github.reposcore.client.GithubClient;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

public sealed interface SearchResult permits SearchResult.ScoredRepository, SearchResult.Failure {

    record Failure(
            int status,
            String title,
            String detail) implements SearchResult {}

    record ScoredRepository(
            long id,
            String name,
            String fullName,
            String description,
            String htmlUrl,
            int stargazersCount,
            int forksCount,
            OffsetDateTime updatedAt,
            OffsetDateTime createdAt,
            String language,

            @Schema(description = "Composite popularity score (0–100)", example = "87.4")
            double popularityScore) implements SearchResult {

        public static ScoredRepository of(GithubClient.GitRepository r, double score) {
            return new ScoredRepository(r.id(), r.name(), r.fullName(), r.description(),
                    r.htmlUrl(), r.stargazersCount(), r.forksCount(),
                    r.updatedAt(), r.createdAt(), r.language(), score);
        }
    }
}