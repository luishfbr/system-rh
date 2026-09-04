package com.sgc.backend.controller;

import com.sgc.backend.dto.request.CreateUserRequest;
import com.sgc.backend.dto.request.UpdateUserRequest;
import com.sgc.backend.dto.response.ApiResponse;
import com.sgc.backend.dto.response.PageResponse;
import com.sgc.backend.dto.response.UserResponse;
import com.sgc.backend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Cadastro dos Gestores de Pessoas e administradores (RF01, RF02, RF03).
 *
 * <p>O {@code @PreAuthorize} na classe vale para todos os metodos: gerenciar
 * quem acessa o sistema e atribuicao exclusiva do administrador (RNF02).
 * A anotacao so funciona por causa do {@code @EnableMethodSecurity} declarado
 * em {@code SecurityConfig}.
 */
@RestController
@RequestMapping("/api/v1/users")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Usuarios", description = "Gestores de Pessoas e administradores (somente ADMIN)")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @Operation(summary = "RF01 - Cadastrar usuario")
    public ResponseEntity<ApiResponse<UserResponse>> create(@Valid @RequestBody CreateUserRequest request) {
        UserResponse created = userService.create(request);
        // 201 com o header Location apontando para o recurso novo -- o que a
        // convencao REST espera de um POST que cria algo.
        return ResponseEntity
                .created(URI.create("/api/v1/users/" + created.id()))
                .body(ApiResponse.ok("Usuario cadastrado com sucesso.", created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "RF02 - Editar usuario")
    public ResponseEntity<ApiResponse<UserResponse>> update(@PathVariable Long id,
                                                            @Valid @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Usuario atualizado com sucesso.", userService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "RF03 - Excluir usuario")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        // authentication.getName() e o subject do JWT, ou seja, o email de quem
        // esta chamando -- usado para impedir a auto-exclusao.
        userService.delete(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "Lista usuarios paginados")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> list(
            @PageableDefault(size = 20, sort = "fullName", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(userService.list(pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta um usuario")
    public ResponseEntity<ApiResponse<UserResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(userService.findById(id)));
    }
}
