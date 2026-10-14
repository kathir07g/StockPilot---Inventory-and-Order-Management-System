package com.springboot.stockpilot.system;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class TestController {

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/test-protected")
    public Map<String, String> testProtected() {
        return Map.of(
                "status", "SUCCESS",
                "message", "JWT authentication successful"
        );
    }
}