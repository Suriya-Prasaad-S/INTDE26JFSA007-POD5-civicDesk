package com.civicdesk.module.permit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "citizen_profile")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CitizenProfile {

    @Id
    @Column(name = "citizenId", length = 36)
    private String citizenId;

    @Column(name = "userId", length = 36)
    private String userId;

    @Column(name = "gender", length = 10)
    private String gender;

    @Column(name = "nationalId", length = 50)
    private String nationalId;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "ward", length = 50)
    private String ward;

    @Column(name = "zone", length = 50)
    private String zone;

    @Column(name = "profileStatus", length = 20)
    private String profileStatus;
}