package com.shivam.bookMyShow.service;
import com.shivam.bookMyShow.dto.request.*;
import com.shivam.bookMyShow.dto.response.AuthResponse;
public interface AuthService {
    AuthResponse signup(SignupRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refresh(String refreshToken);
}
