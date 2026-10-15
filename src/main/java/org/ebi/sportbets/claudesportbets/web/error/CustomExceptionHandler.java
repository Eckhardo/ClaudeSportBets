package org.ebi.sportbets.claudesportbets.web.error;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.annotation.Nullable;
import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.hibernate.TransientObjectException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.validation.BindingResult;
import org.springframework.web.ErrorResponse;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@Hidden
@ControllerAdvice
public class CustomExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(CustomExceptionHandler.class);

    @ExceptionHandler({EntityNotFoundException.class, TransientObjectException.class})
    public ProblemDetail resolveEntityNotFoundException2(Exception ex, ServletRequest request, HttpServletResponse response) {
        log.error("EntityNotFoundException", ex);


        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid associated entity: " + ex.getMessage());
        problemDetail.setType(URI.create("https://example.com/errors/invalid-associated-entity"));
        problemDetail.setTitle("Invalid associated entity");
        return problemDetail;
    }

    @ExceptionHandler({EntityExistsException.class})
    public ProblemDetail resolveEntityExitsException2(Exception ex, ServletRequest request, HttpServletResponse response) {

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "duplicate entity: " + ex.getMessage());
        problemDetail.setType(URI.create("https://example.com/errors/invalid-associated-entity"));
        problemDetail.setTitle("duplicate entity");
        return problemDetail;
    }


    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        // Extrahiere Fehler (Feldname -> Fehlernachricht)
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class})
    public ProblemDetail resolveMethodArgumentTypeMismatchException(Exception ex, ServletRequest request, HttpServletResponse response) {

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "wrong method argument type: " + ex.getMessage());
        problemDetail.setType(URI.create("https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/method/annotation/MethodArgumentTypeMismatchException.html"));
        problemDetail.setTitle("wrong method argument type");
        return problemDetail;
    }

    @ExceptionHandler({DataIntegrityViolationException.class})
    public ErrorResponse resolveDuplicatedKey(DataIntegrityViolationException ex) {
        ErrorResponseException response = new ErrorResponseException(HttpStatus.BAD_REQUEST);
        response.setDetail("Duplicated key:" + ex.getMessage());
        response.setType(URI.create("https://example.com/errors/duplicated-key"));
        response.setTitle("Duplicated key");
        response.getHeaders()
                .add("Custom-Header", "Value");
        return response;
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(@SuppressWarnings("NullableProblems") Exception ex, @Nullable Object body, @SuppressWarnings("NullableProblems") HttpHeaders headers, @SuppressWarnings("NullableProblems") HttpStatusCode statusCode, @SuppressWarnings("NullableProblems") WebRequest request) {

        log.error(" handle error {}", ex.getMessage(), ex);
        ResponseEntity<Object> response = super.handleExceptionInternal(ex, body, headers, statusCode, request);

        if (response.getBody() instanceof ProblemDetail problemDetailBody) {
            problemDetailBody.setProperty("message", ex.getMessage());
            if (ex instanceof MethodArgumentNotValidException subEx) {
                BindingResult result = subEx.getBindingResult();
                problemDetailBody.setProperty("message", "Validation failed for object='" + result.getObjectName() + "'. " + "Error count: " + result.getErrorCount());
                problemDetailBody.setProperty("errors", result.getAllErrors());
            }
        }
        return response;
    }
}
