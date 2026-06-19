package com.github.reposcore.service;

import com.github.reposcore.TestFixtures;
import com.github.reposcore.client.GithubClient;
import com.github.reposcore.dto.SearchResult;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScoringServiceTest {

    private final ScoringService scoringService = new ScoringService();

    // score = (stars/10000 * 0.5 + forks/2000 * 0.3 + recency * 0.2) * 100

    @Test
    void score_maxStarsAndForks_updatedToday_returns100() {
        var repo = TestFixtures.gitRepo(10000, 2000, OffsetDateTime.now());

        var result = scoringService.score(repo);

        assertThat(result.popularityScore()).isEqualTo(100.0);
    }

    @Test
    void score_zeroStarsAndForks_updatedOver365DaysAgo_returns0() {
        var repo = TestFixtures.gitRepo(0, 0, OffsetDateTime.now().minusDays(400));

        var result = scoringService.score(repo);

        assertThat(result.popularityScore()).isEqualTo(0.0);
    }

    @Test
    void score_recentUpdate_contributesRecencyBonus() {
        var recentRepo = TestFixtures.gitRepo(0, 0, OffsetDateTime.now());
        var oldRepo    = TestFixtures.gitRepo(0, 0, OffsetDateTime.now().minusDays(400));

        double recent = scoringService.score(recentRepo).popularityScore();
        double old    = scoringService.score(oldRepo).popularityScore();

        assertThat(recent).isGreaterThan(old);
    }

    @Test
    void score_starsCapAt10000() {
        var repoAt10k  = TestFixtures.gitRepo(10000, 0, OffsetDateTime.now().minusDays(400));
        var repoAt100k = TestFixtures.gitRepo(100000, 0, OffsetDateTime.now().minusDays(400));

        double score10k  = scoringService.score(repoAt10k).popularityScore();
        double score100k = scoringService.score(repoAt100k).popularityScore();

        assertThat(score10k).isEqualTo(score100k);
    }

    @Test
    void score_resultMapsRepoFieldsCorrectly() {
        var repo = TestFixtures.gitRepo(100, 50, OffsetDateTime.now());

        SearchResult.ScoredRepository result = scoringService.score(repo);

        assertThat(result.id()).isEqualTo(repo.id());
        assertThat(result.name()).isEqualTo(repo.name());
        assertThat(result.stargazersCount()).isEqualTo(repo.stargazersCount());
    }

    @Test
    void scoreAndSort_returnsSortedByScoreDescending() {
        var low  = TestFixtures.gitRepo(1, 100, 0, OffsetDateTime.now().minusDays(400));
        var high = TestFixtures.gitRepo(2, 10000, 2000, OffsetDateTime.now());
        var mid  = TestFixtures.gitRepo(3, 5000, 1000, OffsetDateTime.now().minusDays(30));

        List<SearchResult.ScoredRepository> sorted = scoringService.scoreAndSort(List.of(low, high, mid));

        assertThat(sorted).extracting(SearchResult.ScoredRepository::id)
                .containsExactly(2L, 3L, 1L);
    }


}