package de.hhu.fscs.ultraqueue;

import de.hhu.fscs.ultraqueue.web.UserContext;
import jakarta.servlet.http.Cookie;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.util.WebUtils;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final UltraQueueProperties props;
    private final UserContext userContext;

    /** -----------------------------------------------------------------
     *  Interceptor that creates (or reads) the user‑id cookie.
     *  ----------------------------------------------------------------- */
    @Bean
    public HandlerInterceptor userIdInterceptor() {
        return (request, response, handler) -> {
            var cookie = WebUtils.getCookie(request, UserContext.COOKIE_NAME);
            if (cookie == null) {
                // first visit – generate a UUID and set the cookie
                String uuid = java.util.UUID.randomUUID().toString();
                Cookie newCookie = new Cookie(UserContext.COOKIE_NAME, uuid);
                newCookie.setHttpOnly(true);
                newCookie.setPath("/");
                newCookie.setMaxAge(60 * 60 * 24 * 365 * 10); // 10 years
                response.addCookie(newCookie);
                request.setAttribute(UserContext.COOKIE_NAME, uuid);
            } else {
                request.setAttribute(UserContext.COOKIE_NAME, cookie.getValue());
            }
            return true;
        };
    }

    /** -----------------------------------------------------------------
     *  Register the interceptor for *all* web requests (including API).
     *  ----------------------------------------------------------------- */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userIdInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns("/error", "/actuator/**", "/static/**");
    }

    /** -----------------------------------------------------------------
     *  Logging interceptor (optional – can also be done with an
     *  `@Component` that implements `HandlerInterceptor`).
     *  ----------------------------------------------------------------- */
    @Bean
    public HandlerInterceptor loggingInterceptor() {
        return new LoggingInterceptor();   // implementation shown later
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // order matters – userId first, then logging
        registry.addInterceptor(userIdInterceptor()).order(0);
        registry.addInterceptor(loggingInterceptor()).order(1);
    }

    /** -----------------------------------------------------------------
     *  Static resources (CSS, JS, images) – served from classpath:/static/
     *  ----------------------------------------------------------------- */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
    }

    /** -----------------------------------------------------------------
     *  Thymeleaf view resolver is auto‑configured by Spring Boot.
     *  ----------------------------------------------------------------- */
}

/* -----------------------------------------------------------------------
 *  Security configuration – admin only via HTTP Basic Auth.
 * ----------------------------------------------------------------------- */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final UltraQueueProperties props;

    @Bean
    @Order(1) // make sure admin URLs are evaluated before the generic rule
    public SecurityFilterChain adminChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/admin/**", "/queue/remove/**")   // admin‑only ops
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
        var admin = User.withUsername(props.getAdmin().getUsername())
                .passwordEncoder(NoOpPasswordEncoder.getInstance()::encode)
                .password(props.getAdmin().getPassword())
                .roles("ADMIN")
                .build();
        return new InMemoryUserDetailsManager(admin);
    }
}
