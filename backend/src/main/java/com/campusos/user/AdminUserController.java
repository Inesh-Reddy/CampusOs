package com.campusos.user;

import com.campusos.common.exception.BusinessException;
import com.campusos.security.CurrentUser;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUser currentUser;

    public AdminUserController(UserRepository userRepository, PasswordEncoder passwordEncoder, CurrentUser currentUser) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentUser = currentUser;
    }

    public record UserResponse(Long id, String email, String displayName, String role, String department, boolean active) {}
    public record CreateUserRequest(
            @NotBlank @Email String email,
            @NotBlank String password,
            @NotBlank String displayName,
            @NotNull Role role,
            String department) {}

    @GetMapping
    public List<UserResponse> list() {
        requireCampusAdmin();
        return userRepository.findAll().stream()
                .map(u -> new UserResponse(u.getId(), u.getEmail(), u.getDisplayName(), u.getRole().name(), u.getDepartment(), u.isActive()))
                .toList();
    }

    @GetMapping("/technicians")
    public List<UserResponse> technicians() {
        var actor = currentUser.require();
        if (!actor.getRole().isAdmin() && !actor.getRole().isStaff()) throw BusinessException.forbidden("Staff access required.");
        return userRepository.findByRoleAndActiveTrue(Role.TECHNICIAN).stream()
                .map(u -> new UserResponse(u.getId(), u.getEmail(), u.getDisplayName(), u.getRole().name(), u.getDepartment(), u.isActive()))
                .toList();
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest req) {
        requireCampusAdmin();
        String email = req.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw BusinessException.conflict("DUPLICATE_EMAIL", "A user with this email already exists.");
        }
        User user = new User(email, passwordEncoder.encode(req.password()), req.displayName().trim(), req.role(),
                req.department() == null ? null : req.department().trim());
        userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getRole().name(), user.getDepartment(), user.isActive()));
    }

    private void requireCampusAdmin() {
        if (currentUser.require().getRole() != Role.CAMPUS_ADMIN) {
            throw BusinessException.forbidden("Campus administrator access required.");
        }
    }
}
