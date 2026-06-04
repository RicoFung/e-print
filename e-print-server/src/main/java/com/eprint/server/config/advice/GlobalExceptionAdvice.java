package com.eprint.server.config.advice;

import com.niko.boot.model.result.NikoResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionAdvice {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public NikoResult methodArgumentNotValidExceptionHandler(MethodArgumentNotValidException e) {
        Map<String, String> errors = new LinkedHashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = error instanceof FieldError fieldError
                    ? fieldError.getField()
                    : error.getObjectName();
            errors.put(fieldName, error.getDefaultMessage());
        });

        String message = String.join("; ", errors.values());
        log.warn("Request validation failed: {}", errors);
        return error(message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseBody
    public NikoResult constraintViolationExceptionHandler(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                .collect(Collectors.joining("; "));
        log.warn("Request constraint validation failed: {}", message);
        return error(message);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseBody
    public NikoResult missingServletRequestParameterExceptionHandler(MissingServletRequestParameterException e) {
        String message = "Missing request parameter: " + e.getParameterName();
        log.warn(message);
        return error(message);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseBody
    public NikoResult methodArgumentTypeMismatchExceptionHandler(MethodArgumentTypeMismatchException e) {
        String message = "Invalid request parameter: " + e.getName();
        log.warn("{} value={}", message, e.getValue());
        return error(message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseBody
    public NikoResult httpMessageNotReadableExceptionHandler(HttpMessageNotReadableException e) {
        log.warn("Request body is not readable: {}", e.getMessage());
        return error("Request body is not readable");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    public NikoResult illegalArgumentExceptionHandler(IllegalArgumentException e) {
        log.warn("Business argument validation failed: {}", e.getMessage());
        return error(e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseBody
    public NikoResult illegalStateExceptionHandler(IllegalStateException e) {
        log.warn("Business state validation failed: {}", e.getMessage(), e);
        return error(e.getMessage());
    }

    @ExceptionHandler(BadSqlGrammarException.class)
    @ResponseBody
    public NikoResult badSqlGrammarExceptionHandler(HttpServletRequest request, BadSqlGrammarException e) {
        log.error("SQL grammar error, path={}", request.getRequestURI(), e);
        return error("Database operation failed");
    }

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public NikoResult defaultExceptionHandler(HttpServletRequest request, Exception e) {
        log.error("Unhandled server exception, path={}", request.getRequestURI(), e);
        return error("Server error");
    }

    private NikoResult error(String message) {
        String resultMessage = message == null || message.trim().isEmpty() ? "Server error" : message;
        NikoResult result = NikoResult.error(resultMessage);
        result.set("message", result.getMsg());
        return result;
    }
}
