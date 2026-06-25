package com.hrms.security.service;

import com.hrms.common.exception.ResourceNotFoundException;
import com.hrms.security.AppUserPrincipal;
import com.hrms.security.domain.AppUser;
import com.hrms.security.dto.LoginRequest;
import com.hrms.security.dto.LoginResponse;
import com.hrms.security.dto.UserDto;
import com.hrms.security.jwt.JwtService;
import com.hrms.security.repository.AppUserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Login and current-user lookup (FTDD Vol.2 Ch.31).
 */
@Service
@Transactional
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AppUserRepository userRepository;
    private final UserService userService;

    public AuthService(AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       AppUserRepository userRepository,
                       UserService userService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.userService = userService;
    }

    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        AppUserPrincipal principal = (AppUserPrincipal) authentication.getPrincipal();

        String token = jwtService.generateToken(principal);

        AppUser user = userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + principal.getUsername()));
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        UserDto dto = userService.toDto(user);
        return new LoginResponse(token, jwtService.getExpirationMinutes(), dto);
    }

    @Transactional(readOnly = true)
    public UserDto currentUser(UUID userId) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        return userService.toDto(user);
    }
}
