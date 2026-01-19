package net.mbi.wcloud.dispatch.solver.framework.web;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import net.mbi.wcloud.dispatch.solver.framework.common.exception.ErrorCodeConstants;
import net.mbi.wcloud.dispatch.solver.framework.common.exception.ServiceException;
import net.mbi.wcloud.dispatch.solver.framework.common.pojo.CommonResult;
import org.springframework.dao.DataAccessException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.validation.ConstraintViolationException;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ServiceException.class)
    public CommonResult<Void> handleService(ServiceException ex, HttpServletRequest request) {
        log.warn("[ServiceException] uri={}, code={}, msg={}",
                request.getRequestURI(), ex.getCode(), ex.getMessage());
        return CommonResult.error(ex.getCode(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public CommonResult<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + " " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return CommonResult.error(ErrorCodeConstants.VALIDATE_FAILED, msg);
    }

    @ExceptionHandler(BindException.class)
    public CommonResult<Void> handleBind(BindException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + " " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return CommonResult.error(ErrorCodeConstants.VALIDATE_FAILED, msg);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public CommonResult<Void> handleConstraintViolation(ConstraintViolationException ex) {
        return CommonResult.error(ErrorCodeConstants.VALIDATE_FAILED, ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public CommonResult<Void> handleNotReadable() {
        return CommonResult.error(ErrorCodeConstants.BAD_REQUEST, "请求体格式错误或 JSON 解析失败");
    }

    @ExceptionHandler(BadSqlGrammarException.class)
    public CommonResult<Void> handleBadSql(BadSqlGrammarException ex, HttpServletRequest request) {
        log.error("[BadSqlGrammar] uri={}, msg={}", request.getRequestURI(), ex.getMessage(), ex);
        return CommonResult.error(ErrorCodeConstants.SQL_BAD_GRAMMAR, "SQL 语法错误");
    }

    @ExceptionHandler(DataAccessException.class)
    public CommonResult<Void> handleDataAccess(DataAccessException ex, HttpServletRequest request) {
        log.error("[DataAccessException] uri={}, msg={}", request.getRequestURI(), ex.getMessage(), ex);
        return CommonResult.error(ErrorCodeConstants.DB_ERROR, "数据库异常");
    }

    @ExceptionHandler(Exception.class)
    public CommonResult<Void> handleUnknown(Exception ex, HttpServletRequest request) {
        log.error("[UnknownException] uri={}, msg={}", request.getRequestURI(), ex.getMessage(), ex);
        return CommonResult.error(ErrorCodeConstants.INTERNAL_ERROR, "系统异常，请稍后再试");
    }
}
