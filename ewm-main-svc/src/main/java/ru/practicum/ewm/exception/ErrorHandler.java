package ru.practicum.ewm.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(BAD_REQUEST)
    public ErrorResponse handleConstraintViolationException(ConstraintViolationException ex) {
        return new ErrorResponse("BAD_REQUEST", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        return new ErrorResponse("Ошибка валидации: ", e.getMessage());
    }

    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handlerValidationException(ValidationException e) {
        return new ErrorResponse("BAD_REQUEST", e.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(NOT_FOUND)
    public ErrorResponse handleNotFoundException(NotFoundException ex) {
        return new ErrorResponse("NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(DuplicatedDataException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDuplicatedDataException(DuplicatedDataException ex) {
        log.warn("DuplicatedDataException: {}", ex.getMessage());
        return new ErrorResponse("CONFLICT", ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleConflictException(ConflictException ex) {
        log.warn("ConflictException: {}", ex.getMessage());
        return new ErrorResponse("CONFLICT", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(BAD_REQUEST)
    public ErrorResponse handleIllegalArgumentException(IllegalArgumentException ex) {
        return new ErrorResponse("BAD_REQUEST", ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(BAD_REQUEST)
    public ErrorResponse handleIllegalStateException(IllegalStateException ex) {
        return new ErrorResponse("BAD_REQUEST", ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        log.warn("DataIntegrityViolationException: {}", ex.getMessage());
        return new ErrorResponse("CONFLICT", "Нарушение уникальности данных: " + ex.getMessage());
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiError handleConflict(final ForbiddenException e) {
        log.info("403 {}", e.getMessage(), e);
        return new ApiError(
                HttpStatus.FORBIDDEN.value(),
                "For the requested operation the conditions are not met.",
                e.getMessage()
        );
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiError handleIncorrectRequestException(final IncorrectRequestException e) {
        log.info("400 {}", e.getMessage(), e);
        return new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                "Incorrectly made request.",
                e.getMessage()
        );
    }
}