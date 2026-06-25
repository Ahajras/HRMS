package com.hrms.common.domain;

import java.time.LocalDate;

/**
 * Contract for entities that vary over time (valid-time dimension of the bi-temporal model).
 *
 * <p>An effective-dated record is valid from {@link #getEffectiveFrom()} (inclusive) up to
 * {@link #getEffectiveTo()} (inclusive, null = open-ended). Resolution "as of a date" returns
 * the version whose effective window contains that date. History is never overwritten;
 * a change creates a new version (FTDD: effective dating, immutability, reproducibility).
 */
public interface EffectiveDated {

    LocalDate getEffectiveFrom();

    LocalDate getEffectiveTo();

    /** True when this version is in effect on the given date. */
    default boolean isEffectiveOn(LocalDate date) {
        LocalDate from = getEffectiveFrom();
        LocalDate to = getEffectiveTo();
        boolean afterStart = from == null || !date.isBefore(from);
        boolean beforeEnd = to == null || !date.isAfter(to);
        return afterStart && beforeEnd;
    }
}
