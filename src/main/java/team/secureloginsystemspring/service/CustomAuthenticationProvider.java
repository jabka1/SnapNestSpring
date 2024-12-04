package team.secureloginsystemspring.service;


import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import team.secureloginsystemspring.model.User;
import team.secureloginsystemspring.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CustomAuthenticationProvider implements AuthenticationProvider {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Override
    public Authentication authenticate(Authentication authentication) throws BadCredentialsException {
        String username = authentication.getName();
        String password = (String) authentication.getCredentials();

        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            if (user.getLockTime() > System.currentTimeMillis()) {
                throw new BadCredentialsException("User account is locked");
            }

            boolean isPasswordValid = passwordEncoder.matches(password, user.getPassword());

            if (!isPasswordValid) {
                user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);

                if (user.getFailedLoginAttempts() >= 3) {
                    user.setLockTime(System.currentTimeMillis() + 60000);
                }

                userRepository.save(user);

                throw new BadCredentialsException("Invalid credentials");
            } else {
                user.setFailedLoginAttempts(0);
                user.setLockTime(0);
                userRepository.save(user);
                return new UsernamePasswordAuthenticationToken(username, password, user.getAuthorities());
            }
        } else {
            throw new UsernameNotFoundException("User not found");
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}
