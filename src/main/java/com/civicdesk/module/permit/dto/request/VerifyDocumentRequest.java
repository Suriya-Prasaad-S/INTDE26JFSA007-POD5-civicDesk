package com.civicdesk.module.permit.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyDocumentRequest {
    private String verificationStatus;
    private String verificationRemarks;
}