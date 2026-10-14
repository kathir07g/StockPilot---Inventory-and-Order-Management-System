package com.springboot.stockpilot.auth;

import com.springboot.stockpilot.security.JwtService;
import com.springboot.stockpilot.user.User;
import com.springboot.stockpilot.user.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public AuthenticationService(AuthenticationManager authenticationManager, UserRepository userRepository, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    public LoginResponse login(LoginRequest request){
        String email=request.getEmail().trim().toLowerCase(Locale.ROOT);

        Authentication authentication=authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getPassword())
        );

        User user=userRepository.findByEmail(authentication.getName())
                .orElseThrow();

        String token=jwtService.generateToken(user);

        return new LoginResponse(token, jwtService.getExpirationMs()/1000,
                user.getId(), user.getName(), user.getEmail(), user.getRole().getName());
    }

}
