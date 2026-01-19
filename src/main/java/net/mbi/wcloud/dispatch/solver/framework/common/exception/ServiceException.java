package net.mbi.wcloud.dispatch.solver.framework.common.exception;

public class ServiceException extends RuntimeException {

    private final int code;

    public ServiceException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
