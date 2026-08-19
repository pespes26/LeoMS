package org.leoms.admin.config;

import org.leoms.admin.security.ClientAddressAccess;
import org.leoms.admin.security.LoginAttemptService;
import org.leoms.admin.security.LoginThrottleFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    UserDetailsService users(@Value("${leoms.admin.username}") String username,
                             @Value("${leoms.admin.password-hash-file}") String passwordHashFile) {
        String hash = SecretFiles.readRequired("leoms.admin.password-hash-file", passwordHashFile);
        if (!hash.matches("^\\$2[aby]\\$12\\$.{53}$")) {
            throw new IllegalStateException("Admin password must be a bcrypt cost-12 hash");
        }
        return new InMemoryUserDetailsManager(User.withUsername(username).password(hash).roles("ADMIN").build());
    }

    @Bean
    SecurityFilterChain security(HttpSecurity http, LoginAttemptService attempts) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/css/**", "/actuator/health").permitAll()
                        .anyRequest().hasRole("ADMIN"))
                .formLogin(form -> form
                        .loginPage("/login")
                        .failureHandler((request, response, exception) -> {
                            attempts.recordFailure(ClientAddressAccess.from(request));
                            response.sendRedirect("/login?error");
                        })
                        .successHandler((request, response, authentication) -> {
                            attempts.clear(ClientAddressAccess.from(request));
                            response.sendRedirect("/");
                        }))
                .logout(logout -> logout.logoutSuccessUrl("/login?logout").invalidateHttpSession(true))
                .sessionManagement(session -> session
                        .sessionFixation(fixation -> fixation.migrateSession()))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; style-src 'self'; img-src 'self'; form-action 'self'; frame-ancestors 'none'"))
                        .frameOptions(frame -> frame.deny()))
                .addFilterBefore(new LoginThrottleFilter(attempts), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
