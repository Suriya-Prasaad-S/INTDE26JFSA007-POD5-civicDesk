package com.civicdesk.module.publicworks.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateMilestoneStatusRequest {
    private String status;
    private String remarks;
}
