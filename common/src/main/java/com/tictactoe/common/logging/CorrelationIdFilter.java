package com.tictactoe.common.logging;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;

import java.io.IOException;

/**
 * Gives every HTTP request a correlation id: reuses a well-formed
 * X-Correlation-Id header when the caller sent one, generates one otherwise.
 * The id is exposed in the MDC for the duration of the request and echoed
 * in the response so callers can reference it.
 */
public class CorrelationIdFilter implements Filter {

    private static final String SAFE_ID = "[A-Za-z0-9-]{1,64}";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        var cid = ((HttpServletRequest) request).getHeader(Correlation.HEADER);
        if (cid == null || !cid.matches(SAFE_ID)) {
            cid = Correlation.newId();
        }
        MDC.put(Correlation.CID, cid);
        ((HttpServletResponse) response).setHeader(Correlation.HEADER, cid);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
