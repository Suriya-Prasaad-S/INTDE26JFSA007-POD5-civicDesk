package com.civicdesk.module.publicworks.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateWorkOrderStatusRequest {
    private String status;
    private String remarks;
}
