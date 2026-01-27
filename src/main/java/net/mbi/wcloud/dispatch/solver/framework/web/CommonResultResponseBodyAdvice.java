package net.mbi.wcloud.dispatch.solver.framework.web;

import jakarta.servlet.http.HttpServletRequest;
import net.mbi.wcloud.dispatch.solver.framework.common.pojo.CommonResult;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice
public class CommonResultResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    // 这些路径必须排除，否则 /v3/api-docs 会被包装导致你现在的 ClassCastException
    private static final String[] IGNORE_PATTERNS = new String[] {
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/actuator/**",
            "/error"
    };

    @Override
    public boolean supports(MethodParameter returnType,
            Class<? extends HttpMessageConverter<?>> converterType) {

        Class<?> paramType = returnType.getParameterType();

        // 方式A：已经是 CommonResult 的接口，不需要再包装
        if (CommonResult.class.isAssignableFrom(paramType)) {
            return false;
        }

        // 避免 byte[] / String 的 converter 写出时发生类型转换异常
        if (byte[].class == paramType || String.class == paramType) {
            return false;
        }

        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response) {

        String path = resolvePath(request);
        if (isIgnoredPath(path)) {
            return body;
        }

        return CommonResult.success(body);
    }

    private boolean isIgnoredPath(String path) {
        if (path == null) {
            return false;
        }
        for (String pattern : IGNORE_PATTERNS) {
            if (PATH_MATCHER.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private String resolvePath(ServerHttpRequest request) {
        if (request instanceof ServletServerHttpRequest servletReq) {
            HttpServletRequest raw = servletReq.getServletRequest();
            // getRequestURI 不包含 query，适合做 path 匹配
            return raw.getRequestURI();
        }
        return request.getURI().getPath();
    }
}
