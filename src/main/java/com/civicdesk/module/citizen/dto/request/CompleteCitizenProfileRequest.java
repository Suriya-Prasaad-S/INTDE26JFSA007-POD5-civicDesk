package com.civicdesk.module.citizen.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Body for {@code POST /citizenProfile/me} — the citizen completes their profile (the extra,
 * citizen-specific fields) after an officer has verified them. The citizen is identified by the
 * JWT, so no id is carried in the body.
 *
 * <p>All fields except {@code zone} are mandatory: a citizen cannot be considered "profile
 * complete" without them.
 */
public record CompleteCitizenProfileRequest(

        @NotNull(message = "dateOfBirth is required")
        @Past(message = "dateOfBirth must be a date in the past")
        LocalDate dateOfBirth,

        @NotBlank(message = "gender is required")
        @Pattern(regexp = "(?i)Male|Female|Other", message = "gender must be Male, Female or Other")
        String gender,

        @NotBlank(message = "nationalIdNumber is required")
        @Pattern(regexp = "^[A-Za-z0-9]{6,20}$",
                message = "nationalIdNumber must be 6-20 alphanumeric characters")
        String nationalIdNumber,

        @NotBlank(message = "address is required")
        @Size(max = 255, message = "address must not exceed 255 characters")
        String address,

        @NotBlank(message = "ward is required")
        @Size(max = 50, message = "ward must not exceed 50 characters")
        String ward,

        @Size(max = 50, message = "zone must not exceed 50 characters")
        String zone
) {
}
