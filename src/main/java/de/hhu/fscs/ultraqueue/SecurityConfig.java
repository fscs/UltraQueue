package de.hhu.fscs.ultraqueue;

import de.hhu.fscs.ultraqueue.config.UltraQueueProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/* -----------------------------------------------------------------------
 *  Security configuration – admin only via HTTP Basic Auth.
 * ----------------------------------------------------------------------- */
@Configuration
public class SecurityConfig {

    private final UltraQueueProperties props;

    public SecurityConfig(UltraQueueProperties props) {
        this.props = props;
    }

    @Bean
    @Order(1) // make sure admin URLs are evaluated before the generic rule
    public SecurityFilterChain adminChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/admin/**")   // admin‑only ops
                .authorizeHttpRequests(auth -> auth.anyRequest().hasRole("ADMIN"))
                .httpBasic(Customizer.withDefaults())
                .csrf(csrf -> csrf.ignoringRequestMatchers("/admin/**")); // CSRF not needed for Basic Auth

        return http.build();
    }

    @Bean
    public SecurityFilterChain publicChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/queue/**", "/nextsong", "/songfinished", "/static/**")
                        .permitAll()
                        .anyRequest().authenticated())
                .csrf(csrf -> csrf.ignoringRequestMatchers("/nextsong", "/songfinished")) // the game does not send a CSRF token
                .formLogin(form -> form.disable()) // we do not use form login
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    /** Simple in‑memory admin user – credentials come from application.yml. */
    @Bean
    public InMemoryUserDetailsManager userDetailsService() {
        var admin = User.withUsername(props.admin().username())
                .passwordEncoder(NoOpPasswordEncoder.getInstance()::encode)
                .password(props.admin().password())
                .roles("ADMIN")
                .build();
        return new InMemoryUserDetailsManager(admin);
    }
}
