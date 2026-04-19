package de.hhu.fscs.ultraqueue;

import de.hhu.fscs.ultraqueue.web.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Logs every HTTP request together with the automatically generated
 * user‑id cookie.  Execution time is measured for extra visibility.
 */
@Component
public class LoggingInterceptor implements HandlerInterceptor {
    private static final Logger log = LoggerFactory.getLogger(LoggingInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest req, @NonNull HttpServletResponse res, @NonNull Object handler) {
        long start = System.nanoTime();
        req.setAttribute("startTime", start);
        String userId = UserContext.getCurrentUserId(req);
        log.info("[{}] {} {} (user={})", req.getMethod(), req.getRequestURI(),
                req.getQueryString(), userId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest req, HttpServletResponse res, @NonNull Object handler, Exception ex) {
        long start = (Long) req.getAttribute("startTime");
        long ms = (System.nanoTime() - start) / 1_000_000;
        log.info("[{}] {} → {} ({} ms)", req.getMethod(),
                req.getRequestURI(), res.getStatus(), ms);
    }
}