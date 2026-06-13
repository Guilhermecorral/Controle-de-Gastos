package com.controledegastos.backend.admin;

import com.controledegastos.backend.admin.dto.AdminOverviewResponseDTO;
import com.controledegastos.backend.admin.dto.AdminUserPasswordResetRequestDTO;
import com.controledegastos.backend.admin.dto.AdminUserResponseDTO;
import com.controledegastos.backend.admin.dto.AdminUserRoleUpdateRequestDTO;
import com.controledegastos.backend.admin.dto.AdminUserStatusUpdateRequestDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Expoe o painel administrativo protegido apenas para contas com role de administrador.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    /**
     * Entrega o resumo global usado pela sala de comando do administrador.
     */
    @GetMapping("/overview")
    public ResponseEntity<AdminOverviewResponseDTO> getOverview() {
        return ResponseEntity.ok(adminService.getOverview());
    }

    /**
     * Lista todas as contas para leitura e operacao administrativa.
     */
    @GetMapping("/users")
    public ResponseEntity<List<AdminUserResponseDTO>> listUsers() {
        return ResponseEntity.ok(adminService.listUsers());
    }

    /**
     * Ativa ou suspende uma conta sem excluir seus dados.
     */
    @PatchMapping("/users/{userId}/status")
    public ResponseEntity<AdminUserResponseDTO> updateStatus(
            @PathVariable Long userId,
            @RequestBody AdminUserStatusUpdateRequestDTO dto
    ) {
        return ResponseEntity.ok(adminService.updateUserStatus(userId, dto));
    }

    /**
     * Atualiza o papel de acesso da conta alvo.
     */
    @PatchMapping("/users/{userId}/role")
    public ResponseEntity<AdminUserResponseDTO> updateRole(
            @PathVariable Long userId,
            @Valid @RequestBody AdminUserRoleUpdateRequestDTO dto
    ) {
        return ResponseEntity.ok(adminService.updateUserRole(userId, dto));
    }

    /**
     * Redefine a senha de uma conta quando houver necessidade operacional.
     */
    @PostMapping("/users/{userId}/reset-password")
    public ResponseEntity<AdminUserResponseDTO> resetPassword(
            @PathVariable Long userId,
            @Valid @RequestBody AdminUserPasswordResetRequestDTO dto
    ) {
        return ResponseEntity.ok(adminService.resetUserPassword(userId, dto));
    }

    /**
     * Remove o segundo fator da conta alvo para recuperar o acesso.
     */
    @PostMapping("/users/{userId}/reset-two-factor")
    public ResponseEntity<AdminUserResponseDTO> resetTwoFactor(@PathVariable Long userId) {
        return ResponseEntity.ok(adminService.resetUserTwoFactor(userId));
    }
}
