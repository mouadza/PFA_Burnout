package com.burncare.burncare_app.services;

import com.burncare.burncare_app.dto.AuthRequest;
import com.burncare.burncare_app.dto.AuthResponse;
import com.burncare.burncare_app.dto.RegisterRequest;
import com.burncare.burncare_app.entities.Role;
import com.burncare.burncare_app.entities.User;
import com.burncare.burncare_app.repositories.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // 🔹 JwtService removed from constructor
    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse register(RegisterRequest req) {

        if (userRepository.existsByEmail(req.email())) {
            throw new RuntimeException("Email existe déjà");
        }

        User user = new User();
        user.setFullName(req.fullName());
        user.setEmail(req.email());
        user.setPassword(passwordEncoder.encode(req.password()));
        user.setProfession(req.profession());
        user.setRole(Role.USER);

        userRepository.save(user);

        // 🔹 IMPORTANT: no more JWT generation here
        String token = ""; // Keycloak will provide the real access_token to Flutter

        return new AuthResponse(
                token,
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getProfession()
        );
    }

    public AuthResponse login(AuthRequest req) {

        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new RuntimeException("Email invalide"));

        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new RuntimeException("Mot de passe incorrect");
        }

        // 🔹 Again: no custom JWT, only validation of password
        String token = ""; // Flutter should already have a Keycloak token

        return new AuthResponse(
                token,
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getProfession()
        );
    }
}
