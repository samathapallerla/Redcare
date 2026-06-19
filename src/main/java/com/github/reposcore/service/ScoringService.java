package com.github.reposcore.service;

import com.github.reposcore.client.GithubClient;
import com.github.reposcore.dto.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

/** Assigns a popularity score (0–100) to each repository. */
@Service
public class ScoringService {

    private static final Logger log = LoggerFactory.getLogger(ScoringService.class);

    public SearchResult.ScoredRepository score(GithubClient.GitRepository repo) {
        long daysSinceUpdate = ChronoUnit.DAYS.between(repo.updatedAt().toLocalDate(), LocalDate.now());

        double starsScore   = Math.min(repo.stargazersCount() / 10000.0, 1.0);
        double forksScore   = Math.min(repo.forksCount()      /  2000.0, 1.0);
        double recencyScore = Math.max(0, 1 - daysSinceUpdate / 365.0);

        double score = (starsScore * 0.5 + forksScore * 0.3 + recencyScore * 0.2) * 100;
        log.debug("Scored '{}': stars={} forks={} recency={} -> {}", repo.name(),
                repo.stargazersCount(), repo.forksCount(), daysSinceUpdate, Math.round(score));
        return SearchResult.ScoredRepository.of(repo, score);
    }

    public List<SearchResult.ScoredRepository> scoreAndSort(List<GithubClient.GitRepository> repos) {
        log.info("Scoring and sorting {} repositories", repos.size());
        return repos.stream()
                .map(this::score)
                .sorted(Comparator.comparingDouble(SearchResult.ScoredRepository::popularityScore).reversed())
                .toList();
    }
}