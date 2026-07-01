package com.civicdesk.module.serviceRequest.dto.response;

import java.time.LocalDate;

/**
 * Simple date/count tuple used by analytics responses.
 */
public record DateCount(LocalDate date, Long count) {
}
