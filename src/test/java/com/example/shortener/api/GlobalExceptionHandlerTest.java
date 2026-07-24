package com.example.shortener.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.shortener.domain.InvalidRequestException;
import com.example.shortener.observability.ShortenerMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

class GlobalExceptionHandlerTest {

    private SimpleMeterRegistry registry;
    private GlobalExceptionHandler handler;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        handler = new GlobalExceptionHandler(new ShortenerMetrics(registry));
        request = new MockHttpServletRequest("GET", "/unknown-alias");
    }

    @Test
    void invalidRequestShouldReturnBadRequestProblemDetail() {
        ProblemDetail detail = handler.invalidRequest(
                new InvalidRequestException("Expiration must be in the future"),
                request
        );

        assertProblem(
                detail,
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST",
                "Expiration must be in the future"
        );
    }

    @Test
    void duplicateAliasShouldReturnConflictProblemDetail() {
        ProblemDetail detail = handler.duplicateAlias(request);

        assertProblem(
                detail,
                HttpStatus.CONFLICT,
                "DUPLICATE_ALIAS",
                "Custom alias is already in use"
        );
    }

    @Test
    void notFoundShouldReturnNotFoundProblemDetail() {
        ProblemDetail detail = handler.notFound(request);

        assertProblem(
                detail,
                HttpStatus.NOT_FOUND,
                "SHORT_URL_NOT_FOUND",
                "Short URL was not found"
        );
    }

    @Test
    void expiredShouldReturnGoneProblemDetail() {
        ProblemDetail detail = handler.expired(request);

        assertProblem(
                detail,
                HttpStatus.GONE,
                "SHORT_URL_EXPIRED",
                "Short URL has expired"
        );
    }

    @Test
    void validationShouldReturnFieldErrors() throws NoSuchMethodException {
        Object target = new Object();
        BeanPropertyBindingResult bindingResult =
                new BeanPropertyBindingResult(target, "request");

        bindingResult.addError(new FieldError(
                "request", "originalUrl", "must not be blank"
        ));
        bindingResult.addError(new FieldError(
                "request", "customAlias", "must match the required format"
        ));
        bindingResult.addError(new FieldError(
                "request", "originalUrl", "must be a valid URL"
        ));

        Method method = GlobalExceptionHandlerTest.class
                .getDeclaredMethod("validationTarget", Object.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(parameter, bindingResult);

        ProblemDetail detail = handler.validation(exception, request);

        assertProblem(
                detail,
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED",
                "Request validation failed"
        );
        assertThat(detail.getProperties())
                .containsEntry("fieldErrors", Map.of(
                        "originalUrl", "must not be blank",
                        "customAlias", "must match the required format"
                ));
    }

    @Test
    void unexpectedExceptionShouldReturnGenericInternalServerError() {
        ProblemDetail detail = handler.unexpected(
                new IllegalStateException("database password"),
                request
        );

        assertProblem(
                detail,
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "An unexpected error occurred"
        );
        assertThat(detail.getDetail()).doesNotContain("database password");
    }

    @SuppressWarnings("unused")
    private void validationTarget(Object requestBody) {
        // Used only to construct a real MethodParameter for the validation test.
    }

    private void assertProblem(
            ProblemDetail detail,
            HttpStatus expectedStatus,
            String expectedCode,
            String expectedMessage
    ) {
        assertThat(detail.getStatus()).isEqualTo(expectedStatus.value());
        assertThat(detail.getTitle()).isEqualTo(expectedStatus.getReasonPhrase());
        assertThat(detail.getDetail()).isEqualTo(expectedMessage);
        assertThat(detail.getType()).isEqualTo(URI.create(
                "urn:problem-type:" + expectedCode.toLowerCase().replace('_', '-')
        ));
        assertThat(detail.getInstance()).isEqualTo(URI.create("/unknown-alias"));
        assertThat(detail.getProperties()).containsEntry("code", expectedCode);
        assertThat(registry.get("shortener.errors.total")
                .tag("code", expectedCode)
                .counter()
                .count()).isEqualTo(1.0);
    }
}
