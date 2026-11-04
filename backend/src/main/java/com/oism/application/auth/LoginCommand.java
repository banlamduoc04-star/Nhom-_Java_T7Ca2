package com.oism.application.auth;

/** identifier la email hoac so dien thoai. */
public record LoginCommand(String identifier, String password) {
}
