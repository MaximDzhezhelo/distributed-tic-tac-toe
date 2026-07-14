package com.tictactoe.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Constrains game/session ids to 1-64 characters of letters, digits, '-' and '_'.
 * Null is allowed where the id is optional (an id is then generated).
 */
@Documented
@Target({ElementType.PARAMETER, ElementType.FIELD, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {})
@Size(min = 1, max = 64, message = "id must be between 1 and 64 characters")
@Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9_-]*",
        message = "id may only contain letters, digits, '-' and '_'")
public @interface ValidId {

    String message() default "invalid id";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
