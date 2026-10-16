package com.campusos.auth;

import com.campusos.auth.dto.AuthDtos;
import com.campusos.common.exception.BusinessException;
import com.campusos.security.AppUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.campusos.common.exception.ApiError;

@Tag(name = "Authentication", description = "Session login, logout, and current-user lookup.")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;

    public AuthController(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Operation(summary = "Log in and create a server-side session",
            description = "Public endpoint, exempt from CSRF. On success the response sets the JSESSIONID session cookie and an XSRF-TOKEN cookie for later mutating requests.")
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logged in; session cookie set."),
            @ApiResponse(responseCode = "400", description = "Validation failed.", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Invalid email or password.", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @PostMapping("/login")
    public AuthDtos.MeResponse login(@Valid @RequestBody AuthDtos.LoginRequest request, HttpServletRequest httpRequest) {
        try {
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email().trim().toLowerCase(), request.password()));
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);
            HttpSession session = httpRequest.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
            AppUserDetails user = (AppUserDetails) auth.getPrincipal();
            return toMe(user);
        } catch (BadCredentialsException ex) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Invalid email or password.");
        }
    }

    @Operation(summary = "Log out and invalidate the session")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Logged out; no body."),
            @ApiResponse(responseCode = "401", description = "Authentication is required.", content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "CSRF token missing or invalid.", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Return the currently authenticated user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current user."),
            @ApiResponse(responseCode = "401", description = "Authentication is required.", content = @Content(schema = @Schema(implementation = ApiError.class)))})
    @GetMapping("/me")
    public AuthDtos.MeResponse me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AppUserDetails user)) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Authentication is required.");
        }
        return toMe(user);
    }

    private AuthDtos.MeResponse toMe(AppUserDetails user) {
        return new AuthDtos.MeResponse(user.getId(), user.getUsername(), user.getDisplayName(),
                user.getRole().name(), user.getDepartment());
    }
}
