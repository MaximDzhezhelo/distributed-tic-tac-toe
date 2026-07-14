package com.tictactoe.session.web;

import com.tictactoe.session.client.EngineClientException;
import com.tictactoe.session.exception.SessionNotFoundException;
import com.tictactoe.session.exception.SimulationConflictException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(SessionNotFoundException.class)
    public ProblemDetail sessionNotFound(SessionNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Session not found", ex.getMessage());
    }

    @ExceptionHandler(SimulationConflictException.class)
    public ProblemDetail simulationConflict(SimulationConflictException ex) {
        return problem(HttpStatus.CONFLICT, "Simulation conflict", ex.getMessage());
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

    @ExceptionHandler(EngineClientException.class)
    public ProblemDetail engineFailure(EngineClientException ex) {
        return problem(HttpStatus.BAD_GATEWAY, "Game engine error", ex.getMessage());
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail) {
        log.warn("Rejected request ({} {}): {}", status.value(), title, detail);
        var problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        return problem;
    }
}
