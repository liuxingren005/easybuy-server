package org.maven.exception;

import org.maven.common.ResponseResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Optional;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseResult handleBusinessException(BusinessException e) {
        log.warn("业务异常：{}", e.getMessage());
        return ResponseResult.error(e.getMessage());
    }

    /**
     * 参数校验异常（@Valid / @NotBlank ...）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseResult handleValidException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> Optional.ofNullable(error.getDefaultMessage()).orElse("参数校验失败")) // ← 内
                .findFirst() // 对话式
                .orElse("参数校验失败"); // ← 外 空列表
        log.warn("参数校验异常：{}", message);
        return ResponseResult.error(message);
    }

    /**
     * 非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseResult handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("非法参数：{}", e.getMessage());
        return ResponseResult.error(e.getMessage());
    }

    /**
     * 非法状态异常（并发...）
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseResult handleIllegalStateException(IllegalStateException e) {
        log.warn("非法状态：{}", e.getMessage());
        return ResponseResult.error(e.getMessage());
    }

    /**
     * 其他未捕获异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseResult handleException(Exception e) {
        log.error("系统异常：{}", e.getMessage(), e);
        return ResponseResult.error("系统繁忙，请稍后再试");
    }
}
