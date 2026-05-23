package ru.ziovpo.backend.security;

import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ru.ziovpo.backend.user.UserEntity;
import ru.ziovpo.backend.user.UserRepository;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public AppUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity user = userRepository.findByName(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return new AppUserPrincipal(
                user.getId(),
                user.getName(),
                user.getPasswordHash(),
                List.of(new SimpleGrantedAuthority(user.getRole().name())),
                !user.isAccountExpired(),
                !user.isAccountLocked(),
                !user.isCredentialsExpired(),
                !user.isDisabled()
        );
    }
}
