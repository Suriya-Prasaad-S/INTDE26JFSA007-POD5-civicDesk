package com.civicdesk.module.citizen.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Single citizen self-registration form: account fields (handed to IAM to create the {@code User})
 * plus the citizen profile fields. The identity-proof file is sent as a separate multipart part.
 *
 * <p>A class (not a record) with getters/setters so Spring can bind it from {@code multipart/
 * form-data} fields via {@code @ModelAttribute}.
 */
public class CitizenRegistrationRequest {

    // --- Account fields (-> IAM User) ---

    @NotBlank(message = "name is required")
    @Size(max = 100, message = "name must not exceed 100 characters")
    private String name;

    @NotBlank(message = "email is required")
    @Email(message = "email must be a valid email address")
    @Size(max = 150, message = "email must not exceed 150 characters")
    private String email;

    @NotBlank(message = "password is required")
    @Size(min = 8, max = 72, message = "password must be between 8 and 72 characters")
    private String password;

    @NotBlank(message = "phone is required")
    @Pattern(regexp = "^[6-9][0-9]{9}$",
            message = "phone must be a valid 10-digit Indian mobile number starting with 6-9")
    private String phone;

    // --- Citizen profile fields (-> CitizenProfile) ---

    @NotNull(message = "dateOfBirth is required")
    @Past(message = "dateOfBirth must be a date in the past")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateOfBirth;

    @NotBlank(message = "gender is required")
    @Pattern(regexp = "(?i)Male|Female|Other", message = "gender must be Male, Female or Other")
    private String gender;

    @NotBlank(message = "nationalIdNumber is required")
    @Pattern(regexp = "^[A-Za-z0-9]{6,20}$",
            message = "nationalIdNumber must be 6-20 alphanumeric characters")
    private String nationalIdNumber;

    @NotBlank(message = "address is required")
    @Size(max = 255, message = "address must not exceed 255 characters")
    private String address;

    @NotBlank(message = "ward is required")
    @Size(max = 50, message = "ward must not exceed 50 characters")
    private String ward;

    @Size(max = 50, message = "zone must not exceed 50 characters")
    private String zone;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(LocalDate dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getNationalIdNumber() {
        return nationalIdNumber;
    }

    public void setNationalIdNumber(String nationalIdNumber) {
        this.nationalIdNumber = nationalIdNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getWard() {
        return ward;
    }

    public void setWard(String ward) {
        this.ward = ward;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }
}
