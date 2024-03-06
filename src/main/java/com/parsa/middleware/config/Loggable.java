package com.parsa.middleware.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to mark methods for logging purposes.
 * Methods annotated with {@code @Loggable} will have their entry and exit points logged.
 * Logging includes method name, parameter names, parameter values, and return value (if applicable).
 * The logging is performed by an aspect.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Loggable {
}
