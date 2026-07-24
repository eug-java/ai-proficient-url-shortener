package com.example.shortener.api;

import com.example.shortener.domain.DuplicateAliasException;
import com.example.shortener.domain.ExpiredException;
import com.example.shortener.domain.InvalidRequestException;
import com.example.shortener.domain.NotFoundException;
import com.example.shortener.observability.ShortenerMetrics;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ShortenerMetrics metrics;

    public GlobalExceptionHandler(ShortenerMetrics metrics) {
        this.metrics = metrics;
    }

    @ExceptionHandler(InvalidRequestException.class)
    ProblemDetail invalidRequest(InvalidRequestException exception, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", exception.getMessage(), request);
    }

    @ExceptionHandler(DuplicateAliasException.class)
    ProblemDetail duplicateAlias(HttpServletRequest request) {
        return problem(
                HttpStatus.CONFLICT,
                "DUPLICATE_ALIAS",
                "Custom alias is already in use",
                request
        );
    }

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail notFound(HttpServletRequest request) {
        return problem(
                HttpStatus.NOT_FOUND,
                "SHORT_URL_NOT_FOUND",
                "Short URL was not found",
                request
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ProblemDetail missingResource(HttpServletRequest request) {
        // Not a business error — avoid inflating shortener.errors.total with probe noise.
        return problemDetail(
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                "Resource was not found",
                request
        );
    }

    @ExceptionHandler(ExpiredException.class)
    ProblemDetail expired(HttpServletRequest request) {
        return problem(
                HttpStatus.GONE,
                "SHORT_URL_EXPIRED",
                "Short URL has expired",
                request
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        ProblemDetail detail = problem(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                "Request validation failed",
                request
        );
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage())
        );
        detail.setProperty("fieldErrors", fieldErrors);
        return detail;
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail unexpected(Exception exception, HttpServletRequest request) {
        log.error("Unhandled exception", exception);
        return problem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "An unexpected error occurred",
                request
        );
    }

    private ProblemDetail problem(
            HttpStatus status,
            String code,
            String detailMessage,
            HttpServletRequest request
    ) {
        metrics.error(code);
        return problemDetail(status, code, detailMessage, request);
    }

    private ProblemDetail problemDetail(
            HttpStatus status,
            String code,
            String detailMessage,
            HttpServletRequest request
    ) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, detailMessage);
        detail.setTitle(status.getReasonPhrase());
        detail.setType(URI.create("urn:problem-type:" + code.toLowerCase().replace('_', '-')));
        detail.setInstance(URI.create(request.getRequestURI()));
        detail.setProperty("code", code);
        return detail;
    }
}
