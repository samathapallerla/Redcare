package com.github.reposcore.controller;

import com.github.reposcore.TestFixtures;
import com.github.reposcore.dto.SearchCriteria;
import com.github.reposcore.dto.SearchResult;
import com.github.reposcore.exception.GithubApiException;
import com.github.reposcore.service.GithubSearchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RepositoryController.class)
class RepositoryControllerTest {

    private static final String SEARCH_URL = "/api/v1/repositories/search";

    @Autowired MockMvc mockMvc;
    @MockitoBean GithubSearchService githubSearchService;

    // --- 400: at least one param required ---

    @Test
    void search_noParams_returns400() throws Exception {
        mockMvc.perform(get(SEARCH_URL))
                .andExpect(status().isBadRequest());
    }

    // --- 200 cases ---

    @ParameterizedTest(name = "{0}")
    @MethodSource("validRequestScenarios")
    void search_validRequest_returns200(String label, MockHttpServletRequestBuilder request,
                                        List<SearchResult.ScoredRepository> mockResults,
                                        List<ResultMatcher> matchers) throws Exception {
        when(githubSearchService.search(any(SearchCriteria.class))).thenReturn(mockResults);
        var result = mockMvc.perform(request).andExpect(status().isOk());
        for (ResultMatcher m : matchers) result.andExpect(m);
    }

    static Stream<Arguments> validRequestScenarios() {
        return Stream.of(
            Arguments.of(
                "language only",
                get(SEARCH_URL).param("language", "Java"),
                List.of(TestFixtures.springBootRepo()),
                List.of(jsonPath("$[0].name").value("spring-boot"),
                        jsonPath("$[0].popularityScore").value(95.0))
            ),
            Arguments.of(
                "createdAfter only",
                get(SEARCH_URL).param("createdAfter", "2020-01-01"),
                List.of(TestFixtures.springBootRepo()),
                List.of(jsonPath("$[0].name").value("spring-boot"))
            ),
            Arguments.of(
                "language and createdAfter",
                get(SEARCH_URL).param("language", "Java").param("createdAfter", "2020-01-01"),
                List.of(TestFixtures.springBootRepo()),
                List.of(jsonPath("$[0].name").value("spring-boot"))
            ),
            Arguments.of(
                "no matching repos returns empty array",
                get(SEARCH_URL).param("language", "COBOL"),
                List.of(),
                List.of(jsonPath("$").isEmpty())
            )
        );
    }

    // --- error propagation ---

    @Test
    void search_githubApiError_returnsBadGateway() throws Exception {
        when(githubSearchService.search(any(SearchCriteria.class)))
                .thenThrow(new GithubApiException("GitHub server error", 502));

        mockMvc.perform(get(SEARCH_URL).param("language", "Java"))
                .andExpect(status().isBadGateway());
    }
}