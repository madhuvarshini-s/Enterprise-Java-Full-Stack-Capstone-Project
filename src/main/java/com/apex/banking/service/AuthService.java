package com.apex.banking.service;

import com.apex.banking.dto.request.LoginRequest;
import com.apex.banking.dto.request.RegisterRequest;
import com.apex.banking.dto.response.ApiResponse;
import com.apex.banking.dto.response.JwtResponse;
import com.apex.banking.dto.response.UserProfileResponse;

public interface AuthService {

    JwtResponse login(LoginRequest request);

    ApiResponse<String> register(RegisterRequest request);

    UserProfileResponse getCurrentUserProfile(String username);
}
