package de.hhu.fscs.ultraqueue;

import de.hhu.fscs.ultraqueue.config.UltraQueueProperties;
import de.hhu.fscs.ultraqueue.web.UserContext;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.util.WebUtils;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final UltraQueueProperties props;
    private final UserContext userContext;
    private final LoggingInterceptor loggingInterceptor;

    public WebConfig(UltraQueueProperties props, UserContext userContext, LoggingInterceptor loggingInterceptor) {
        this.props = props;
        this.userContext = userContext;
        this.loggingInterceptor = loggingInterceptor;
    }

    /** -----------------------------------------------------------------
     *  Interceptor that creates (or reads) the user‑id cookie.
     *  ----------------------------------------------------------------- */
    @Bean
    public HandlerInterceptor userIdInterceptor() {
        return new HandlerInterceptor() {
            @Override
            public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
                var cookie = WebUtils.getCookie(request, UserContext.COOKIE_NAME);
                if (cookie == null) {
                    // first visit – generate a UUID and set the cookie
                    String uuid = java.util.UUID.randomUUID().toString();
                    Cookie newCookie = new Cookie(UserContext.COOKIE_NAME, uuid);
                    newCookie.setHttpOnly(true);
                    newCookie.setPath("/");
                    newCookie.setMaxAge(60 * 60 * 24 * 2); // 2 days
                    response.addCookie(newCookie);
                    request.setAttribute(UserContext.COOKIE_NAME, uuid);
                } else {
                    request.setAttribute(UserContext.COOKIE_NAME, cookie.getValue());
                }
                return true;
            }
        };
    }

    /** -----------------------------------------------------------------
     *  Register the interceptor for *all* web requests (including API).
     *  ----------------------------------------------------------------- */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userIdInterceptor()).order(0)
                .addPathPatterns("/**")
                .excludePathPatterns("/error", "/actuator/**", "/static/**");
        registry.addInterceptor(loggingInterceptor).order(1);
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
