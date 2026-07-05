package com.wimone.enjoytix.framework.web.filter;

import com.wimone.enjoytix.framework.base.trace.TraceConstants;
import com.wimone.enjoytix.framework.base.trace.TraceContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

public class TraceIdFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TraceIdFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {
        long startTime = System.currentTimeMillis();
        String requestId = request.getHeader(TraceConstants.REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        TraceContext.setRequestId(requestId);
        response.setHeader(TraceConstants.REQUEST_ID_HEADER, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - startTime;
            log.info("http_request method={} uri={} status={} durationMs={} requestId={}",
                    request.getMethod(), request.getRequestURI(), response.getStatus(), durationMs, requestId);
            TraceContext.clear();
        }
    }
}
