package com.barofarm.user.auth.application.usecase;

public record LoginCommand(String email, String password) {
}
