package ma.portnet.authservice.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import ma.portnet.authservice.dto.request.CreateUserRequest;
import ma.portnet.authservice.dto.request.UpdateUserRequest;
import ma.portnet.authservice.dto.request.UserStatusRequest;
import ma.portnet.authservice.dto.response.ApiResponse;
import ma.portnet.authservice.dto.response.UserResponse;
import ma.portnet.authservice.service.KeycloakUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "Administration des utilisateurs")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMINISTRATEUR')")
public class UserController {

    private final KeycloakUserService userService;

    public UserController(KeycloakUserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.ok(userService.listAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok(userService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> create(
            @RequestBody CreateUserRequest req
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Utilisateur créé", userService.create(req)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> update(
            @PathVariable String id,
            @RequestBody UpdateUserRequest req
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Utilisateur modifié", userService.update(id, req)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        userService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Utilisateur supprimé", null));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<UserResponse>> setStatus(
            @PathVariable String id,
            @RequestBody UserStatusRequest req
    ) {
        return ResponseEntity.ok(ApiResponse.ok(userService.setEnabled(id, req.enabled())));
    }
}