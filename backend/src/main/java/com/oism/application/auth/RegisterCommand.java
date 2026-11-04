package com.oism.application.auth;

public record RegisterCommand(String storeName, String fullName, String email, String phone, String password) {
}
