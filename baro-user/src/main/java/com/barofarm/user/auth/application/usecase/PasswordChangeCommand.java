package com.barofarm.user.auth.application.usecase;

public record PasswordChangeCommand(String currentPassword, String newPassword) {
}
