package com.github.reposcore.controller;

import com.github.reposcore.dto.SearchCriteria;
import com.github.reposcore.dto.SearchResult;
import com.github.reposcore.service.GithubSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/repositories")
public class RepositoryController {

    private static final Logger log = LoggerFactory.getLogger(RepositoryController.class);

    private final GithubSearchService githubSearchService;

    public RepositoryController(GithubSearchService githubSearchService) {
        this.githubSearchService = githubSearchService;
    }

    @GetMapping("/search")
    public ResponseEntity<List<SearchResult.ScoredRepository>> search(
            @RequestParam(required = false) String language,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdAfter) {

        log.info("Received search request [language={}, createdAfter={}]", language, createdAfter);

        if (language == null && createdAfter == null) {
            log.warn("Search rejected: at least one of 'language' or 'createdAfter' must be provided");
            throw new IllegalArgumentException("At least one of 'language' or 'createdAfter' must be provided");
        }

        var results = githubSearchService.search(new SearchCriteria(language, createdAfter));
        log.info("Search completed [language={}, createdAfter={}] - returned {} results",
                language, createdAfter, results.size());
        return ResponseEntity.ok(results);
    }
}