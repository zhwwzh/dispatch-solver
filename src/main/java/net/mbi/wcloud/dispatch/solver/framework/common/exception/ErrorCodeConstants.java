package net.mbi.wcloud.dispatch.solver.framework.common.exception;

public final class ErrorCodeConstants {

    private ErrorCodeConstants() {
    }

    // HTTP 语义
    public static final int BAD_REQUEST = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int INTERNAL_ERROR = 500;

    // 业务 & 系统
    public static final int VALIDATE_FAILED = 1002001;
    public static final int DB_ERROR = 1001001;
    public static final int SQL_BAD_GRAMMAR = 1001002;
}
