package org.ebi.sportbets.exception;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.ebi.sportbets.dto.ErrorResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;
import java.util.List;

@Slf4j
@Hidden
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler({ResourceNotFoundException.class, EntityNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRule(BusinessRuleException ex, HttpServletRequest request) {
        return build(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicate(DuplicateResourceException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        // Log the root cause, but never expose SQL or constraint details to the client
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT, "Resource conflicts with existing data", request.getRequestURI(), null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), request.getRequestURI(), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error on {}", request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request.getRequestURI(), null);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        List<ErrorResponse.FieldErrorDto> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> ErrorResponse.FieldErrorDto.builder()
                        .field(error.getField())
                        .rejectedValue(error.getRejectedValue())
                        .message(error.getDefaultMessage())
                        .build())
                .toList();
        return ResponseEntity.badRequest()
                .body(errorBody(HttpStatus.BAD_REQUEST, "Validation failed", pathOf(request), fieldErrors));
    }

    /**
     * Validation errors of list bodies such as {@code List<@Valid TippDto>}, which Spring reports via method
     * validation instead of {@link MethodArgumentNotValidException}. Element errors are prefixed with their
     * index, e.g. {@code [1].spielId}; errors on the parameter itself use the parameter name.
     */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        List<ErrorResponse.FieldErrorDto> fieldErrors = ex.getParameterValidationResults().stream()
                .flatMap(result -> result instanceof ParameterErrors errors
                        ? errors.getFieldErrors().stream().map(error -> ErrorResponse.FieldErrorDto.builder()
                                .field(indexPrefix(errors) + error.getField())
                                .rejectedValue(error.getRejectedValue())
                                .message(error.getDefaultMessage())
                                .build())
                        : result.getResolvableErrors().stream().map(error -> ErrorResponse.FieldErrorDto.builder()
                                .field(result.getMethodParameter().getParameterName())
                                .rejectedValue(result.getArgument())
                                .message(error.getDefaultMessage())
                                .build()))
                .toList();
        return ResponseEntity.badRequest()
                .body(errorBody(HttpStatus.BAD_REQUEST, "Validation failed", pathOf(request), fieldErrors));
    }

    private static String indexPrefix(ParameterErrors errors) {
        return errors.getContainerIndex() == null ? "" : "[" + errors.getContainerIndex() + "].";
    }

    /**
     * Converts all other Spring MVC exceptions (malformed JSON, type mismatch,
     * unsupported method, ...) into an {@link ErrorResponse}, keeping their status code.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {
        log.debug("Spring MVC exception: {}", ex.getMessage());
        HttpStatus status = HttpStatus.valueOf(statusCode.value());
        return ResponseEntity.status(statusCode)
                .headers(headers)
                .body(errorBody(status, ex.getMessage(), pathOf(request), null));
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message, String path,
                                                List<ErrorResponse.FieldErrorDto> fieldErrors) {
        return ResponseEntity.status(status).body(errorBody(status, message, path, fieldErrors));
    }

    private ErrorResponse errorBody(HttpStatus status, String message, String path,
                                    List<ErrorResponse.FieldErrorDto> fieldErrors) {
        return ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .message(message)
                .path(path)
                .fieldErrors(fieldErrors)
                .build();
    }

    private String pathOf(WebRequest request) {
        return request instanceof ServletWebRequest servletRequest
                ? servletRequest.getRequest().getRequestURI()
                : null;
    }
}
