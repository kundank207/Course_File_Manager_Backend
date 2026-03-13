package com.mitmeerut.CFM_Portal.security.jwt;

import com.mitmeerut.CFM_Portal.security.user.CustomUserDetailsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.beans.factory.annotation.Qualifier;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

        private final JwtTokenProvider tokenProvider;
        private final CustomUserDetailsService userDetailsService;
        private final CorsConfigurationSource corsConfigurationSource;

        public JwtAuthFilter(
                        JwtTokenProvider tokenProvider,
                        CustomUserDetailsService userDetailsService,
                        @Qualifier("corsConfigurationSource") CorsConfigurationSource corsConfigurationSource) {
                this.tokenProvider = tokenProvider;
                this.userDetailsService = userDetailsService;
                this.corsConfigurationSource = corsConfigurationSource;
        }

        public CorsConfigurationSource getCorsConfigurationSource() {
                return corsConfigurationSource;
        }

        // 🔴 CRITICAL: only exclude public auth endpoints, NOT switch-role
        @Override
        protected boolean shouldNotFilter(HttpServletRequest request) {
                String path = request.getServletPath();
                // Allow switch-role to go through filter so it gets Authenticated
                return path.equals("/api/auth/login") || path.equals("/api/auth/register");
        }

        @Override
        protected void doFilterInternal(
                        HttpServletRequest request,
                        HttpServletResponse response,
                        FilterChain filterChain) throws ServletException, IOException {

                // Skip CORS preflight

                String header = request.getHeader("Authorization");

                if (header != null && header.startsWith("Bearer ")) {

                        String token = header.substring(7);

                        if (tokenProvider.validateToken(token)
                                        && SecurityContextHolder.getContext().getAuthentication() == null) {

                                String email = tokenProvider.getUsername(token);
                                String activeRole = tokenProvider.getRole(token);
                                java.util.List<String> perms = tokenProvider.getPermissions(token);

                                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                                java.util.List<org.springframework.security.core.GrantedAuthority> authorities = new java.util.ArrayList<>();

                                // 1. Add ACTIVE ROLE as the primary authority (ROLE_ prefixed for hasRole())
                                if (activeRole != null) {
                                        authorities.add(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                                        "ROLE_" + activeRole));
                                }

                                // 2. Add individual permissions
                                if (perms != null && !perms.isEmpty()) {
                                        authorities.addAll(perms.stream()
                                                        .map(p -> new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                                                        p))
                                                        .collect(java.util.stream.Collectors.toList()));
                                }

                                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                                userDetails,
                                                null,
                                                authorities);

                                authentication.setDetails(
                                                new WebAuthenticationDetailsSource()
                                                                .buildDetails(request));

                                SecurityContextHolder.getContext()
                                                .setAuthentication(authentication);
                        }
                }

                filterChain.doFilter(request, response);
        }
}
