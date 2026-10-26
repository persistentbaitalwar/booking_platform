package com.persistent.booking_platform.security;

import static com.persistent.booking_platform.dto.Requests.LoginRequest;
import static com.persistent.booking_platform.dto.Requests.RegisterRequest;
import static com.persistent.booking_platform.dto.Responses.AuthResponse;

import com.persistent.booking_platform.domain.Customer;
import com.persistent.booking_platform.domain.Role;
import com.persistent.booking_platform.domain.UserAccount;
import com.persistent.booking_platform.exception.ApiException;
import com.persistent.booking_platform.repository.CustomerRepository;
import com.persistent.booking_platform.repository.UserAccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private static final String BAD_CREDENTIALS = "Email or password is incorrect.";

    private final UserAccountRepository users;
    private final CustomerRepository customers;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final LoginAttemptService attempts;
    private final String dummyHash;

    public AuthService(UserAccountRepository users, CustomerRepository customers, PasswordEncoder encoder,
                       JwtService jwtService, LoginAttemptService attempts) {
        this.users = users;
        this.customers = customers;
        this.encoder = encoder;
        this.jwtService = jwtService;
        this.attempts = attempts;
        this.dummyHash = encoder.encode("not-a-real-password-1");
    }

    @Transactional
    public AuthResponse registerCustomer(RegisterRequest request) {
        String email = request.email().trim();
        requireFreeEmail(email);
        Customer customer = customers.save(new Customer(request.name().trim(), email));
        return jwtService.issue(users.save(new UserAccount(email, encoder.encode(request.password()), Role.CUSTOMER,
                request.name().trim(), customer.getId())));
    }

    @Transactional
    public AuthResponse registerPartner(RegisterRequest request) {
        String email = request.email().trim();
        requireFreeEmail(email);
        return jwtService.issue(users.save(new UserAccount(email, encoder.encode(request.password()), Role.PARTNER,
                request.name().trim(), null)));
    }

    /** Used by the bootstrap runner and the demo seeder; real admins are created from the environment. */
    @Transactional
    public AuthResponse createAdmin(String email, String password) {
        if (password == null || password.length() < 12) {
            throw new IllegalArgumentException("The admin password must be at least 12 characters long");
        }
        requireFreeEmail(email);
        return jwtService.issue(users.save(new UserAccount(email, encoder.encode(password), Role.ADMIN, "Administrator", null)));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim();
        attempts.assertNotLocked(email);
        UserAccount user = users.findByEmailIgnoreCase(email).orElse(null);
        // Always run one password check so that unknown emails take as long as wrong passwords.
        boolean matches = encoder.matches(request.password(), user != null ? user.getPasswordHash() : dummyHash);
        if (user == null || !matches) {
            attempts.recordFailure(email);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", BAD_CREDENTIALS);
        }
        attempts.recordSuccess(email);
        return jwtService.issue(user);
    }

    private void requireFreeEmail(String email) {
        if (users.existsByEmailIgnoreCase(email) || customers.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("EMAIL_TAKEN", "An account with this email address already exists");
        }
    }
}
