package com.bite.system.common.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 仅保留过滤器占位，不在 oj-system 内做 token 头存在性校验；
 * token 是否携带统一交由 gateway 的 LoginTokenFilter 负责。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class SysUserTokenHeaderFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        filterChain.doFilter(request, response);
    }
}
