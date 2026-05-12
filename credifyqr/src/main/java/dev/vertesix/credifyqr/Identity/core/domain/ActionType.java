package dev.vertesix.credifyqr.Identity.core.domain;

// Created enum to enforce strict categorization of compliance events
public enum ActionType {
    LOGIN_SUCCESS,
    LOGIN_FAILED,
    ACCOUNT_CLAIMED,
    ROLE_MODIFIED,
    PASSWORD_CHANGED,
    USER_PROVISIONED,
    DOMAIN_UPDATED,
    CACHE_CLEARED
}