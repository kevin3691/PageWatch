package dev.pagewatch.common.web;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.net.URI;
import java.util.Set;

/** The MVP is a local, single-user service, not an unauthenticated public crawler. */
@Component
public class LocalAccessFilter extends OncePerRequestFilter {
    private static final Set<String> HOSTS = Set.of("localhost", "127.0.0.1", "::1", "[::1]");
    @Override
    protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        if (!HOSTS.contains(request.getServerName())) { response.sendError(403); return; }
        if (!Set.of("GET","HEAD","OPTIONS").contains(request.getMethod())) {
            String origin=request.getHeader("Origin");
            boolean allowed=!"cross-site".equals(request.getHeader("Sec-Fetch-Site"));
            if (origin!=null) {
                try {
                    URI uri=URI.create(origin);
                    allowed &= "http".equals(uri.getScheme()) && HOSTS.contains(uri.getHost())
                            && (uri.getPort()==request.getServerPort() || uri.getPort()==5178 || uri.getPort()==4178);
                } catch (IllegalArgumentException e) { allowed=false; }
            }
            if (!allowed) { response.sendError(403); return; }
        }
        response.setHeader("X-Content-Type-Options","nosniff");
        response.setHeader("Cache-Control","no-store");
        chain.doFilter(request,response);
    }
}
