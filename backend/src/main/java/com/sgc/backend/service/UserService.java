package com.sgc.backend.service;

import com.sgc.backend.domain.entity.User;
import com.sgc.backend.dto.request.CreateUserRequest;
import com.sgc.backend.dto.request.UpdateUserRequest;
import com.sgc.backend.dto.response.PageResponse;
import com.sgc.backend.dto.response.UserResponse;
import com.sgc.backend.exception.BusinessRuleException;
import com.sgc.backend.exception.DuplicateResourceException;
import com.sgc.backend.exception.ResourceNotFoundException;
import com.sgc.backend.mapper.UserMapper;
import com.sgc.backend.repository.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Cadastro de usuarios do sistema -- os "Gestores de Pessoas" (RF01, RF02, RF03).
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** RF01 -- cadastrar. */
    @Transactional
    public UserResponse create(CreateUserRequest request) {
        String email = normalizeEmail(request.email());

        // RN01: email unico. A verificacao aqui existe para produzir uma mensagem
        // clara; a unique constraint do banco continua sendo a garantia final.
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("email", "Ja existe um usuario com o email " + email);
        }

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFullName(request.fullName().trim());
        user.setRole(request.role());
        user.setActive(true);

        return UserMapper.toResponse(userRepository.save(user));
    }

    /** RF02 -- editar. */
    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request) {
        User user = findEntity(id);
        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmailAndIdNot(email, id)) {
            throw new DuplicateResourceException("email", "Ja existe outro usuario com o email " + email);
        }

        user.setEmail(email);
        user.setFullName(request.fullName().trim());
        user.setRole(request.role());
        user.setActive(Boolean.TRUE.equals(request.active()));

        // Senha em branco significa "manter a atual" -- trocar de perfil ou de
        // nome nao deveria obrigar o administrador a redefinir a senha.
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }

        return UserMapper.toResponse(user);
    }

    /**
     * RF03 -- excluir.
     *
     * <p>Aqui a exclusao e fisica porque o usuario do sistema nao carrega
     * historico funcional. Ja o colaborador (RF06/RN04) e apenas inativado.
     */
    @Transactional
    public void delete(Long id, String currentUserEmail) {
        User user = findEntity(id);

        // Sem isto, um administrador distraido poderia apagar a propria conta e,
        // se fosse o unico ADMIN, deixar o sistema sem quem gerencie usuarios.
        if (user.getEmail().equalsIgnoreCase(currentUserEmail)) {
            throw new BusinessRuleException("self-deletion",
                    "Um usuario nao pode excluir a propria conta.");
        }

        userRepository.delete(user);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> list(Pageable pageable) {
        return PageResponse.from(userRepository.findAll(pageable), UserMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return UserMapper.toResponse(findEntity(id));
    }

    private User findEntity(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Usuario", id));
    }

    /** RN01: minusculas sempre, para que a unicidade seja de fato case-insensitive. */
    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
