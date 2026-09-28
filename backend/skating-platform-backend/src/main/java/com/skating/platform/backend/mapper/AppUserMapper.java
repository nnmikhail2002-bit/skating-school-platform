package com.skating.platform.backend.mapper;

import com.skating.platform.backend.dto.appusers.response.AppUserResponse;
import com.skating.platform.backend.entity.AppUser;
import org.springframework.stereotype.Component;

@Component
public class AppUserMapper {

    public AppUserResponse toResponse(AppUser user) {
        return new AppUserResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getActive()
        );
    }
}