package com.civicdesk.module.citizen.entity.converter;

import com.civicdesk.module.citizen.entity.enums.CitizenStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Persists {@link CitizenStatus} as its single-character code (A/V/F). */
@Converter
public class CitizenStatusConverter implements AttributeConverter<CitizenStatus, String> {

    @Override
    public String convertToDatabaseColumn(CitizenStatus status) {
        return status == null ? null : status.getCode();
    }

    @Override
    public CitizenStatus convertToEntityAttribute(String code) {
        return code == null ? null : CitizenStatus.fromCode(code);
    }
}
