package com.civicdesk.common.util;

import jakarta.servlet.http.HttpServletRequest;

public final class ClientIpUtil {

    private ClientIpUtil() {
    }

    /**
     * Resolves the real client IP, honouring the X-Forwarded-For header set by
     * proxies/load balancers, and normalising the IPv6 loopback to 127.0.0.1.
     */
    public static String resolve(HttpServletRequest request) {
        if (request == null) {
            return "UNKNOWN";
        }

        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            // X-Forwarded-For may be a comma-separated list; the first is the client.
            ip = ip.split(",")[0].trim();
        } else {
            ip = request.getRemoteAddr();
        }

        return normalize(ip);
    }

    private static String normalize(String ip) {
        if (ip == null || ip.isBlank()) {
            return "UNKNOWN";
        }
        // IPv6 loopback -> IPv4 loopback for readability.
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            return "127.0.0.1";
        }
        return ip;
    }
}
