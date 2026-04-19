package de.hhu.fscs.ultraqueue;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Logs every HTTP request together with the automatically generated
 * user‑id cookie.  Execution time is measured for extra visibility.
 */
@Slf4j
@Component
public class LoggingInterceptor implements HandlerInterceptor {
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
        long start = System.nanoTime();
        req.setAttribute("startTime", start);
        String userId = UserContextHolder.getCurrentUserId();
        log.info("[{}] {} {} (user={})", req.getMethod(), req.getRequestURI(),
                req.getQueryString(), userId);
        return true;
    }

    public void afterCompletion(HttpServletRequest req, HttpServletResponse res, Object handler, Exception ex) {
        long start = (Long) req.getAttribute("startTime");
        long ms = (System.nanoTime() - start) / 1_000_000;
        log.info("[{}] {} → {} ({} ms)", req.getMethod(),
                req.getRequestURI(), res.getStatus(), ms);
    }
}