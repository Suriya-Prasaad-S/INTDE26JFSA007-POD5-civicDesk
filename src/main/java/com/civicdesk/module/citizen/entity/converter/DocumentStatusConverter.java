package com.civicdesk.module.citizen.entity.converter;

import com.civicdesk.module.citizen.entity.enums.DocumentStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** Persists {@link DocumentStatus} as its single-character code (V/E/R). */
@Converter
public class DocumentStatusConverter implements AttributeConverter<DocumentStatus, String> {

    @Override
    public String convertToDatabaseColumn(DocumentStatus status) {
        return status == null ? null : status.getCode();
    }

    @Override
    public DocumentStatus convertToEntityAttribute(String code) {
        return code == null ? null : DocumentStatus.fromCode(code);
    }
}
