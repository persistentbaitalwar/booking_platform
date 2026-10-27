package com.persistent.booking_platform.security;

import com.persistent.booking_platform.repository.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/** Creates the first administrator from BOOTSTRAP_ADMIN_EMAIL and BOOTSTRAP_ADMIN_PASSWORD when both are set. */
@Component
public class AdminBootstrap implements CommandLineRunner {
    private static final Logger LOG = LoggerFactory.getLogger(AdminBootstrap.class);

    private final AuthService authService;
    private final UserAccountRepository users;
    private final String email;
    private final String password;

    public AdminBootstrap(AuthService authService, UserAccountRepository users,
                          @Value("${app.security.bootstrap-admin-email:}") String email,
                          @Value("${app.security.bootstrap-admin-password:}") String password) {
        this.authService = authService;
        this.users = users;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(String... args) {
        if (email.isBlank() || password.isBlank() || users.existsByEmailIgnoreCase(email)) {
            return;
        }
        authService.createAdmin(email, password);
        LOG.info("Created the bootstrap administrator account {}", email);
    }
}
