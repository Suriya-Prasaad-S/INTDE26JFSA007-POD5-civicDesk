package com.civicdesk.module.auditlog.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates that a {@code CharSequence} field matches one of the names of the given
 * enum. A {@code null}/blank value passes (pair with {@code @NotBlank} to require a
 * value), so this constraint only enforces "if present, it must be a known enum name".
 */
@Documented
@Constraint(validatedBy = EnumValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface EnumValid {

    Class<? extends Enum<?>> enumClass();

    String message() default "must be a valid value";

    boolean ignoreCase() default true;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
