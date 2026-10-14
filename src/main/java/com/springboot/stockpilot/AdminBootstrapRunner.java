
package com.springboot.stockpilot;

import com.springboot.stockpilot.user.Role;
import com.springboot.stockpilot.user.RoleRepository;
import com.springboot.stockpilot.user.User;
import com.springboot.stockpilot.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin-bootstrap.enabled:false}")
    private boolean enabled;

    @Value("${app.admin-bootstrap.name:}")
    private String adminName;

    @Value("${app.admin-bootstrap.email:}")
    private String adminEmail;

    @Value("${app.admin-bootstrap.password:}")
    private String adminPassword;

    public AdminBootstrapRunner(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }

        if (adminName.isBlank()
                || adminEmail.isBlank()
                || adminPassword.isBlank()) {
            throw new IllegalStateException(
                    "Admin bootstrap is enabled, but required settings are missing.");
        }

        String email = adminEmail.trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new IllegalStateException(
                    "An account with this admin email already exists.");
        }

        Role adminRole = roleRepository.findByName("ADMIN")
                .orElseThrow(() -> new IllegalStateException(
                        "ADMIN role not found in the roles table."));

        User admin = new User();
        admin.setName(adminName.trim());
        admin.setEmail(email);
        admin.setPasswordHash(passwordEncoder.encode(adminPassword));
        admin.setRole(adminRole);
        admin.setActive(true);

        userRepository.save(admin);

        System.out.println("Admin account created successfully.");
    }
}