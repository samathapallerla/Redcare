package com.github.reposcore.service;

import com.github.reposcore.client.GithubClient;
import com.github.reposcore.dto.SearchCriteria;
import com.github.reposcore.dto.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class GithubSearchService {

    private static final Logger log = LoggerFactory.getLogger(GithubSearchService.class);

    private final GithubClient githubClient;
    private final ScoringService scoringService;

    public GithubSearchService(GithubClient githubClient, ScoringService scoringService) {
        this.githubClient   = githubClient;
        this.scoringService = scoringService;
    }

    public List<SearchResult.ScoredRepository> search(SearchCriteria criteria) {
        var query = buildQuery(criteria);
        log.info("Searching repositories with query: '{}'", query);

        var response = githubClient.searchRepositories(query);

        if (response == null || response.items() == null) {
            log.warn("No response or empty items returned from GitHub for query: '{}'", query);
            return List.of();
        }

        var results = scoringService.scoreAndSort(response.items());
        log.info("Returning {} scored repositories", results.size());
        return results;
    }

    private String buildQuery(SearchCriteria criteria) {
        var parts = new ArrayList<String>();
        if (criteria.language() != null && !criteria.language().isBlank()) {
            parts.add("language:" + criteria.language());
        }
        if (criteria.createdAfter() != null) {
            parts.add("created:>" + criteria.createdAfter());
        }
        return String.join(" ", parts);
    }
}