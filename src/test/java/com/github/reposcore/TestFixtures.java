package com.github.reposcore;

import com.github.reposcore.client.GithubClient;
import com.github.reposcore.dto.SearchResult;

import java.time.OffsetDateTime;

public final class TestFixtures {

    private TestFixtures() {}

    /** Full control — id, stars, forks, and updatedAt all matter (ScoringServiceTest). */
    public static GithubClient.GitRepository gitRepo(long id, int stars, int forks, OffsetDateTime updatedAt) {
        return new GithubClient.GitRepository(
                id, "repo-" + id, "owner/repo-" + id, "desc",
                "https://github.com/owner/repo-" + id,
                stars, forks, updatedAt, updatedAt.minusYears(1), "Java");
    }

    /** Convenience — defaults id to 1 (ScoringServiceTest single-repo cases). */
    public static GithubClient.GitRepository gitRepo(int stars, int forks, OffsetDateTime updatedAt) {
        return gitRepo(1L, stars, forks, updatedAt);
    }

    /** Named repo with default stars/forks (GithubSearchServiceTest). */
    public static GithubClient.GitRepository gitRepo(long id, String name) {
        var now = OffsetDateTime.now();
        return new GithubClient.GitRepository(
                id, name, "owner/" + name, "desc",
                "https://github.com/owner/" + name,
                1_000, 200, now, now.minusYears(1), "Java");
    }

    /** ScoredRepository with an explicit score (GithubSearchServiceTest). */
    public static SearchResult.ScoredRepository scoredRepo(long id, String name, double score) {
        var now = OffsetDateTime.now();
        return new SearchResult.ScoredRepository(
                id, name, "owner/" + name, "desc",
                "https://github.com/owner/" + name,
                1_000, 200, now, now.minusYears(1), "Java", score);
    }

    /** Ready-made spring-boot repo (RepositoryControllerTest). */
    public static SearchResult.ScoredRepository springBootRepo() {
        var now = OffsetDateTime.now();
        return new SearchResult.ScoredRepository(
                1L, "spring-boot", "spring-projects/spring-boot", "Spring Boot",
                "https://github.com/spring-projects/spring-boot",
                75_000, 40_000, now, now.minusYears(5), "Java", 95.0);
    }
}