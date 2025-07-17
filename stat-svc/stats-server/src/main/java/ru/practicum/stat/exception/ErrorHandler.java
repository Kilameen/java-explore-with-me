package ru.practicum.stat.exception;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(BAD_REQUEST)
    public ErrorResponse handleConstraintViolationException(ConstraintViolationException ex) {
        return new ErrorResponse("BAD_REQUEST", ex.getMessage());
    }

    @ExceptionHandler(Throwable.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handlerValidationException(Throwable e) {
        String errorMessage = "Произошла внутренняя ошибка сервера: ";
        errorMessage += "Тип исключения - " + e.getClass().getSimpleName() + ". ";
        if (e.getMessage() != null && !e.getMessage().isEmpty()) {
            errorMessage += "Сообщение: " + e.getMessage();
        } else {
            errorMessage += "Сообщение отсутствует.";
        }
        return new ErrorResponse("INTERNAL_SERVER_ERROR",errorMessage);
    }
}