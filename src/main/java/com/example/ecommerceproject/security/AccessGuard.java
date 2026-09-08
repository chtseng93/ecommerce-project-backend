package com.example.ecommerceproject.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import com.example.ecommerceproject.entity.UserVo;
import com.example.ecommerceproject.service.UserService;

@Component
public class AccessGuard {

    private final UserService userService;

    public AccessGuard(UserService userService) {
        this.userService = userService;
    }

    public boolean isSelfOrAdmin(int userId, Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        boolean admin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (admin) {
            return true;
        }
        UserVo me = userService.getUserInfoByEmail(auth.getName());
        return me != null && me.getUserId() == userId;
    }
}