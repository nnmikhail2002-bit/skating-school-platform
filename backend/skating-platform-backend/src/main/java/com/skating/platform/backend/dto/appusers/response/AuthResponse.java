package com.skating.platform.backend.dto.appusers.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String type;
    private AppUserResponse user;
}
