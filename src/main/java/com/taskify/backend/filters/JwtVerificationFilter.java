package com.taskify.backend.filters;

import com.taskify.backend.models.auth.User;
import com.taskify.backend.repository.auth.UserRepository;
import com.taskify.backend.services.shared.TokenService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class JwtVerificationFilter implements Filter {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UserRepository userRepository;

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/self",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/verifyEmailAndCreatePassword",
            "/api/v1/project/document/test"
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI();

        // Skip public routes
        if (PUBLIC_PATHS.contains(path)) {
            chain.doFilter(request, response);
            return;
        }

        try {
            String token = getToken(httpRequest);

            if (token == null) {
                sendError(httpResponse, "Unauthorized: Missing token");
                return;
            }

            Claims claims = tokenService.verifyToken(token);
            if (claims == null) {
                sendError(httpResponse, "Unauthorized: Invalid token");
                return;
            }

            Map<String, Object> userMap = claims.get("user", Map.class);
            if (userMap == null) {
                sendError(httpResponse, "Invalid token payload: user missing");
                return;
            }

            if (userMap.containsKey("user")) {
                Object nestedUser = userMap.get("user");
                if (nestedUser instanceof Map<?, ?>) {
                    userMap = (Map<String, Object>) nestedUser;
                } else {
                    sendError(httpResponse, "Invalid token payload: nested user is malformed");
                    return;
                }
            }

            if (!userMap.containsKey("id")) {
                sendError(httpResponse, "Invalid token payload: user ID missing");
                return;
            }

            String userId = String.valueOf(userMap.get("id"));
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                sendError(httpResponse, "Invalid token: user not found");
                return;
            }

            // Attach authenticated user to request
            httpRequest.setAttribute("user", userOpt.get());

            chain.doFilter(request, response);
        } catch (Exception e) {
            sendError(httpResponse, "Unauthorized: " + e.getMessage());
        }
    }

    private String getToken(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if ("accessToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        return null;
    }

    private void sendError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"message\": \"" + message + "\"}");
    }

    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void destroy() {}
}
