package com.civicdesk.module.citizen.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

/**
 * Body for POST /registerCitizen.
 *
 * <p>Required fields (per spec): name, ward, email, phone. {@code gender} is carried as a String
 * and validated against the allowed set in the service layer so the API returns a precise
 * "Invalid gender" message rather than a generic deserialization error.
 */
public record RegisterCitizenRequest(
        @NotBlank(message = "Missing required field: name") String name,
        @Past(message = "dateOfBirth must be in the past") LocalDate dateOfBirth,
        String gender,
        String nationalIdNumber,
        String address,
        @NotBlank(message = "Missing required field: ward") String ward,
        String zone,
        @NotBlank(message = "Missing required field: email")
        @Email(message = "Invalid email format") String email,
        @NotBlank(message = "Missing required field: phone")
        @Pattern(regexp = "\\d{10}", message = "phone must be exactly 10 digits") String phone
) {
}
