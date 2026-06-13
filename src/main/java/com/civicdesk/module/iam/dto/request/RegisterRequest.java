package com.civicdesk.module.iam.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

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

    /** Optional. Format: YYYY-MM-DD. Validated only when provided. */
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "dateOfBirth must be in format YYYY-MM-DD")
    private String dateOfBirth;

    /** Optional. MALE | FEMALE | OTHER. Validated only when provided. */
    @Pattern(regexp = "(?i)MALE|FEMALE|OTHER", message = "gender must be MALE, FEMALE or OTHER")
    private String gender;

    /** Optional. Aadhaar / Voter ID — SHA-256 hashed before use, never stored raw. Validated only when provided. */
    @Pattern(regexp = "^[A-Za-z0-9]{6,20}$", message = "nationalId must be 6-20 alphanumeric characters")
    private String nationalId;

    @Size(max = 255, message = "address must not exceed 255 characters")
    private String address;

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

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getNationalId() {
        return nationalId;
    }

    public void setNationalId(String nationalId) {
        this.nationalId = nationalId;
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
