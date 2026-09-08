package com.example.ecommerceproject.security;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
@Slf4j
@Component
public class JwtAuthorizationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final CustomUserDetailsService userDetailsService;

   public JwtAuthorizationFilter(JwtUtil jwtUtil, ObjectMapper objectMapper,
                                  CustomUserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
        this.userDetailsService = userDetailsService;
    }
    @Override
    protected void doFilterInternal(HttpServletRequest httpRequest, HttpServletResponse httpResponse, FilterChain filterChain) throws ServletException, IOException {
        Map<String, Object> errorInfos = new HashMap<>();

        try {
            String jwtToken = jwtUtil.resolveToken(httpRequest);
            if (jwtToken == null ) {
                filterChain.doFilter(httpRequest, httpResponse);
                return;
            }
     
            Claims claims = jwtUtil.resolveClaims(httpRequest);

            if(claims != null && jwtUtil.isValid(claims)){
                String email = claims.getSubject();
                log.info("email:"+email);
                UserDetails user = userDetailsService.loadUserByUsername(email);
                Authentication auth = new UsernamePasswordAuthenticationToken(email,"",user.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);
            }

        }catch (Exception ex){
            errorInfos.put("details", ex.getMessage());
            errorInfos.put("message", "Authentication Error");
            httpResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
            httpResponse.setStatus(HttpStatus.UNAUTHORIZED.value());
            objectMapper.writeValue(httpResponse.getWriter(), errorInfos);
            return;
        }
        filterChain.doFilter(httpRequest, httpResponse);
    }
}
