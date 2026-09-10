package com.example.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "client")
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "client_id")
    private Integer clientId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Column(name = "client_type", nullable = false)
    private String clientType;

    @Column(name = "nationality", nullable = false)
    private String nationality;

    @Column(name = "country_of_birth")
    private String countryOfBirth;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "tax_residency")
    private String taxResidency;

    @Column(name = "occupation")
    private String occupation;

    @Column(name = "employer")
    private String employer;

    @Column(name = "main_source_of_funds")
    private String mainSourceOfFunds;

    @Column(name = "annual_income_band")
    private String annualIncomeBand;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;
}