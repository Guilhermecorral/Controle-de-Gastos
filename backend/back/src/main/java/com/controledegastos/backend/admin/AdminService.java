package com.controledegastos.backend.admin;

import com.controledegastos.backend.admin.dto.AdminOverviewResponseDTO;
import com.controledegastos.backend.admin.dto.AdminUserPasswordResetRequestDTO;
import com.controledegastos.backend.admin.dto.AdminUserResponseDTO;
import com.controledegastos.backend.admin.dto.AdminUserRoleUpdateRequestDTO;
import com.controledegastos.backend.admin.dto.AdminUserStatusUpdateRequestDTO;
import com.controledegastos.backend.config.ResourceNotFoundException;
import com.controledegastos.backend.security.AuthenticatedUserService;
import com.controledegastos.backend.transactions.Repository.TransactionRepository;
import com.controledegastos.backend.transactions.Transaction;
import com.controledegastos.backend.user.Repository.UserRepository;
import com.controledegastos.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Centraliza os fluxos administrativos de leitura global e gestao segura de contas.
 */
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final AuthenticatedUserService authenticatedUserService;
    private final PasswordEncoder passwordEncoder;
    @Value("${app.admin.allowed-emails:}")
    private String allowedAdminEmails;

    /**
     * Resume os numeros principais do produto sem expor detalhes individuais desnecessarios.
     */
    @Transactional(readOnly = true)
    public AdminOverviewResponseDTO getOverview() {
        BigDecimal totalReceitas = transactionRepository.sumAmountByType(Transaction.TransactionType.RECEITA);
        BigDecimal totalDespesas = transactionRepository.sumAmountByType(Transaction.TransactionType.DESPESA);

        return new AdminOverviewResponseDTO(
                userRepository.count(),
                userRepository.countByActiveTrue(),
                userRepository.countByRole(User.Role.ADMIN),
                totalReceitas,
                totalDespesas,
                totalReceitas.subtract(totalDespesas)
        );
    }

    /**
     * Devolve a lista consolidada de contas para o painel administrativo.
     */
    @Transactional(readOnly = true)
    public List<AdminUserResponseDTO> listUsers() {
        return userRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(User::getCreatedAt).reversed())
                .map(this::toAdminUserResponse)
                .toList();
    }

    /**
     * Atualiza o status de uma conta e impede que o admin suspenda a si mesmo sem querer.
     */
    @Transactional
    public AdminUserResponseDTO updateUserStatus(Long userId, AdminUserStatusUpdateRequestDTO dto) {
        User currentAdmin = authenticatedUserService.getAuthenticatedUser();
        User targetUser = findUser(userId);

        if (currentAdmin.getId().equals(targetUser.getId()) && !dto.active()) {
            throw new IllegalArgumentException("Voce nao pode suspender a propria conta administradora");
        }

        targetUser.setActive(dto.active());
        targetUser.setSuspendedAt(dto.active() ? null : LocalDateTime.now());
        return toAdminUserResponse(userRepository.save(targetUser));
    }

    /**
     * Altera a role de uma conta, mantendo o admin atual protegido contra perda acidental de acesso.
     */
    @Transactional
    public AdminUserResponseDTO updateUserRole(Long userId, AdminUserRoleUpdateRequestDTO dto) {
        User currentAdmin = authenticatedUserService.getAuthenticatedUser();
        User targetUser = findUser(userId);
        User.Role newRole;

        try {
            newRole = User.Role.valueOf(dto.role().trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Role informada e invalida");
        }

        if (currentAdmin.getId().equals(targetUser.getId()) && newRole != User.Role.ADMIN) {
            throw new IllegalArgumentException("Voce nao pode remover o proprio acesso administrativo");
        }

        if (newRole == User.Role.ADMIN && !isAdminPromotionAllowed(targetUser.getEmail())) {
            throw new IllegalArgumentException("Este e-mail nao esta autorizado para receber acesso administrativo");
        }

        targetUser.setRole(newRole);
        return toAdminUserResponse(userRepository.save(targetUser));
    }

    /**
     * Permite redefinir uma senha de forma direta quando houver necessidade operacional.
     */
    @Transactional
    public AdminUserResponseDTO resetUserPassword(Long userId, AdminUserPasswordResetRequestDTO dto) {
        User targetUser = findUser(userId);
        validatePasswordStrength(dto.newPassword());
        targetUser.setPassword(passwordEncoder.encode(dto.newPassword()));
        return toAdminUserResponse(userRepository.save(targetUser));
    }

    /**
     * Remove o segundo fator da conta alvo para destravar o acesso quando o usuario perder o autenticador.
     */
    @Transactional
    public AdminUserResponseDTO resetUserTwoFactor(Long userId) {
        User targetUser = findUser(userId);
        targetUser.setTwoFactorEnabled(false);
        targetUser.setTwoFactorEnabledAt(null);
        targetUser.setTwoFactorSecretEncrypted(null);
        targetUser.setTwoFactorPendingSecretEncrypted(null);
        return toAdminUserResponse(userRepository.save(targetUser));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado"));
    }

    private AdminUserResponseDTO toAdminUserResponse(User user) {
        return new AdminUserResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                user.isActive(),
                user.isTwoFactorEnabled(),
                user.getCreatedAt(),
                user.getSuspendedAt(),
                transactionRepository.countByUser(user),
                transactionRepository.findLastTransactionDateByUser(user)
        );
    }

    private void validatePasswordStrength(String password) {
        boolean hasUppercase = password.chars().anyMatch(Character::isUpperCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(character -> !Character.isLetterOrDigit(character));

        if (!hasUppercase || !hasDigit || !hasSpecial || password.length() < 8) {
            throw new IllegalArgumentException("A senha precisa ter pelo menos 8 caracteres, letra maiuscula, numero e caractere especial");
        }
    }

    private boolean isAdminPromotionAllowed(String email) {
        Set<String> whitelist = Arrays.stream(allowedAdminEmails.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        if (whitelist.isEmpty()) {
            return false;
        }

        return whitelist.contains(email.toLowerCase(Locale.ROOT));
    }
}
