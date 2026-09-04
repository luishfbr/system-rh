package com.sgc.backend.security;

import com.sgc.backend.repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Ponte entre o banco de usuarios e o Spring Security.
 *
 * <p>E este bean que o {@code DaoAuthenticationProvider} chama durante o login
 * para carregar o usuario; a comparacao da senha com o hash BCrypt acontece
 * depois, dentro do provider -- nunca aqui.
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public AuthenticatedUser loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email.toLowerCase(Locale.ROOT))
                .map(AuthenticatedUser::from)
                // Mensagem generica de proposito: o handler converte em "email ou
                // senha incorretos", para nao revelar quais emails existem.
                .orElseThrow(() -> new UsernameNotFoundException("Credenciais invalidas"));
    }
}
