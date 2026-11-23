package vn.giadung.account;

import java.io.IOException;
import java.util.Set;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
class ApiWriteFilter extends OncePerRequestFilter {
    // A custom header requires CORS preflight across origins. The API only serves same-origin clients.
    private static final Set<String> SAFE = Set.of("GET","HEAD","OPTIONS");
    @Override
    protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)
            throws IOException, ServletException {
        if (req.getServletPath().startsWith("/api/") && !SAFE.contains(req.getMethod())
                && !"1".equals(req.getHeader("X-CSRF-Protection"))) {
            res.sendError(403);
            return;
        }
        chain.doFilter(req,res);
    }
}
