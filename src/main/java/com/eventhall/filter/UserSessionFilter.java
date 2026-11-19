package com.eventhall.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebFilter("/user/*")
public class UserSessionFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String path = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length());
        if ("/user/login".equals(path) || "/user/signup".equals(path)) {
            chain.doFilter(request, response);
            return;
        }
        if (httpRequest.getSession(false) == null
                || httpRequest.getSession(false).getAttribute("customer") == null) {
            httpResponse.sendRedirect(httpRequest.getContextPath() + "/user/login");
            return;
        }
        chain.doFilter(request, response);
    }
}