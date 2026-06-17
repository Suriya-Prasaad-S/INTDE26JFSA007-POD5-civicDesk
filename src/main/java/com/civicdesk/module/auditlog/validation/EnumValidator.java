package com.civicdesk.module.auditlog.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Backing validator for {@link EnumValid}. Pre-computes the accepted enum names once
 * in {@link #initialize} so each {@link #isValid} call is a single set lookup.
 */
public class EnumValidator implements ConstraintValidator<EnumValid, CharSequence> {

    private Set<String> accepted;
    private boolean ignoreCase;

    @Override
    public void initialize(EnumValid annotation) {
        this.ignoreCase = annotation.ignoreCase();
        this.accepted = Arrays.stream(annotation.enumClass().getEnumConstants())
                .map(Enum::name)
                .map(name -> ignoreCase ? name.toUpperCase() : name)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // null/blank handled by @NotBlank
        }
        String candidate = value.toString().trim();
        if (candidate.isEmpty()) {
            return true;
        }
        return accepted.contains(ignoreCase ? candidate.toUpperCase() : candidate);
    }
}
