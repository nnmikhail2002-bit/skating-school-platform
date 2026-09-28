package com.skating.platform.backend.dto.appusers.response;

import com.skating.platform.backend.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AppUserResponse {

    private Long id;
    private String email;
    private Role role;
    private Boolean active;

}
