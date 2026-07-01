package com.civicdesk.module.serviceRequest.dto.response;

/**
 * Simple label/count tuple used by analytics responses.
 */
public record LabelCount(String label, Long count) {
}
