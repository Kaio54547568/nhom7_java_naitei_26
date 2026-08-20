package com.nhom7.coworkingspace.service;

import com.nhom7.coworkingspace.dto.request.SignUpRequest;

public interface AuthService {

    void signUp(SignUpRequest request);
}
