package com.exelynt.booking.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

        private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

        @Autowired
        private JwtUtils jwtUtils;

        @Autowired
        private CustomUserDetailsService userDetailsService;

        @Override
        protected void doFilterInternal(
                        HttpServletRequest request,
                        HttpServletResponse response,
                        FilterChain filterChain)
                        throws ServletException, IOException {

                String authHeader = request.getHeader("Authorization");

                String token = null;
                String username = null;

                if (authHeader != null && authHeader.startsWith("Bearer ")) {

                        token = authHeader.substring(7);

                        try {
                                username = jwtUtils.getUsernameFromToken(token);

                                if (username != null &&
                                                SecurityContextHolder.getContext().getAuthentication() == null) {

                                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                                        if (jwtUtils.validateToken(token, userDetails)) {

                                                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                                                userDetails,
                                                                null,
                                                                userDetails.getAuthorities());

                                                authToken.setDetails(
                                                                new WebAuthenticationDetailsSource()
                                                                                .buildDetails(request));

                                                logger.debug("Authenticated user: {}", userDetails.getUsername());
                                                logger.debug("Authorities: {}", userDetails.getAuthorities());

                                                SecurityContextHolder.getContext()
                                                                .setAuthentication(authToken);
                                        }
                                }

                        } catch (UsernameNotFoundException e) {

                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                response.setContentType("application/json");
                                response.getWriter().write(
                                                "{\"error\":\"User associated with token does not exist\"}");
                                return;

                        } catch (Exception e) {

                                logger.warn("JWT validation failed: {}", e.getMessage());

                                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                response.setContentType("application/json");
                                response.getWriter().write(
                                                "{\"error\":\"Invalid or expired token\"}");
                                return;
                        }
                }

                filterChain.doFilter(request, response);
        }
}