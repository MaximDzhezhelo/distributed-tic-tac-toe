package com.tictactoe.engine.web;

import com.tictactoe.engine.exception.GameAlreadyExistsException;
import com.tictactoe.engine.exception.GameNotFoundException;
import com.tictactoe.engine.exception.IllegalMoveException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(GameNotFoundException.class)
    public ProblemDetail gameNotFound(GameNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Game not found", ex.getMessage());
    }

    @ExceptionHandler(GameAlreadyExistsException.class)
    public ProblemDetail gameAlreadyExists(GameAlreadyExistsException ex) {
        return problem(HttpStatus.CONFLICT, "Game already exists", ex.getMessage());
    }

    @ExceptionHandler(IllegalMoveException.class)
    public ProblemDetail illegalMove(IllegalMoveException ex) {
        return problem(HttpStatus.CONFLICT, "Illegal move", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail invalidRequest(MethodArgumentNotValidException ex) {
        var details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", details);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ProblemDetail invalidParameters(HandlerMethodValidationException ex) {
        var details = Stream.concat(ex.getValueResults().stream(), ex.getBeanResults().stream())
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> result.getMethodParameter().getParameterName()
                                + ": " + error.getDefaultMessage()))
                .collect(Collectors.joining("; "));
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail unreadableBody(HttpMessageNotReadableException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request",
                "Malformed request body: expected JSON with a valid player (X or O) and position (0-8)");
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        log.warn("Rejected request ({} {}): {}", status.value(), title, detail);
        var problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }

}
