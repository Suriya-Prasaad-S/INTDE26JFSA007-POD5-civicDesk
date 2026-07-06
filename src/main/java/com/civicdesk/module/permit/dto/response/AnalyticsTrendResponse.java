package com.civicdesk.module.permit.dto.response;

import java.time.LocalDate;

public interface AnalyticsTrendResponse {

    LocalDate getDate();

    Long getCount();
}