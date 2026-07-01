package com.civicdesk.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Centralized logger helper for the application.
 * Usage: private static final Logger log = AppLogger.getLogger(MyClass.class);
 */
public final class AppLogger {
    private AppLogger() {}

    /**
     * Returns an SLF4J logger for the given class.
     *
     * @param clazz the class for which the logger is requested
     * @return org.slf4j.Logger instance
     */
    public static Logger getLogger(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }
}
