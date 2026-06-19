package com.github.reposcore.exception;

import com.github.reposcore.dto.SearchResult;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(GithubApiException.class)
    ResponseEntity<SearchResult.Failure> handleGithubApiException(GithubApiException ex) {
        log.error("GitHub API error [status={}]: {}", ex.getStatusCode(), ex.getMessage());
        var status = HttpStatus.resolve(ex.getStatusCode());
        var httpStatus = status != null ? status : HttpStatus.BAD_GATEWAY;
        return ResponseEntity.status(httpStatus)
                .body(new SearchResult.Failure(httpStatus.value(), "GitHub API Error", ex.getMessage()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<SearchResult.Failure> handleConstraintViolation(ConstraintViolationException ex) {
        log.warn("Validation error: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(new SearchResult.Failure(400, "Validation Error", ex.getMessage()));
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    ResponseEntity<SearchResult.Failure> handleBadRequest(Exception ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(new SearchResult.Failure(400, "Bad Request", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<SearchResult.Failure> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Invalid request: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(new SearchResult.Failure(400, "Bad Request", ex.getMessage()));
    }
}