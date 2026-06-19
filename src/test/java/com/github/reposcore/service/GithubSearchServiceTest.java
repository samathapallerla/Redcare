package com.github.reposcore.service;

import com.github.reposcore.TestFixtures;
import com.github.reposcore.client.GithubClient;
import com.github.reposcore.dto.SearchCriteria;
import com.github.reposcore.dto.SearchResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GithubSearchServiceTest {

    @Mock GithubClient githubClient;
    @Mock ScoringService scoringService;
    @InjectMocks GithubSearchService githubSearchService;

    @Test
    void search_languageOnly_buildsCorrectQuery() {
        when(githubClient.searchRepositories("language:Java"))
                .thenReturn(new GithubClient.SearchResponse(0, false, List.of()));
        when(scoringService.scoreAndSort(List.of())).thenReturn(List.of());

        githubSearchService.search(new SearchCriteria("Java", null));

        verify(githubClient).searchRepositories("language:Java");
    }

    @Test
    void search_createdAfterOnly_buildsCorrectQuery() {
        var date = LocalDate.of(2020, 1, 1);
        when(githubClient.searchRepositories("created:>2020-01-01"))
                .thenReturn(new GithubClient.SearchResponse(0, false, List.of()));
        when(scoringService.scoreAndSort(List.of())).thenReturn(List.of());

        githubSearchService.search(new SearchCriteria(null, date));

        verify(githubClient).searchRepositories("created:>2020-01-01");
    }

    @Test
    void search_languageAndCreatedAfter_buildsCombinedQuery() {
        var date = LocalDate.of(2020, 1, 1);
        when(githubClient.searchRepositories("language:Java created:>2020-01-01"))
                .thenReturn(new GithubClient.SearchResponse(0, false, List.of()));
        when(scoringService.scoreAndSort(List.of())).thenReturn(List.of());

        githubSearchService.search(new SearchCriteria("Java", date));

        verify(githubClient).searchRepositories("language:Java created:>2020-01-01");
    }

    @Test
    void search_validResponse_returnsScoredRepos() {
        var rawRepo = TestFixtures.gitRepo(1L, "spring-boot");
        var scored  = TestFixtures.scoredRepo(1L, "spring-boot", 88.0);

        when(githubClient.searchRepositories("language:Java"))
                .thenReturn(new GithubClient.SearchResponse(1, false, List.of(rawRepo)));
        when(scoringService.scoreAndSort(List.of(rawRepo))).thenReturn(List.of(scored));

        List<SearchResult.ScoredRepository> results =
                githubSearchService.search(new SearchCriteria("Java", null));

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().popularityScore()).isEqualTo(88.0);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("emptyResponseCases")
    void search_clientReturnsNothing_returnsEmptyList(String label, GithubClient.SearchResponse response) {
        when(githubClient.searchRepositories(anyString())).thenReturn(response);

        assertThat(githubSearchService.search(new SearchCriteria("Java", null))).isEmpty();
    }

    static Stream<Arguments> emptyResponseCases() {
        return Stream.of(
            Arguments.of("null response",          null),
            Arguments.of("null items in response", new GithubClient.SearchResponse(0, false, null))
        );
    }
}