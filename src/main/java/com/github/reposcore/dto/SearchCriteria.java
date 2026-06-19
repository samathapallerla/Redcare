package com.github.reposcore.dto;

import java.time.LocalDate;

public record SearchCriteria(
        String language,
        LocalDate createdAfter) {}