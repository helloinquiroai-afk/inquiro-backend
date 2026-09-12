package com.inquiro.auth;

/** Principal established from a valid, unrevoked Inquiro access session. */
public record AuthenticatedUser(String userId, String email) {
}
