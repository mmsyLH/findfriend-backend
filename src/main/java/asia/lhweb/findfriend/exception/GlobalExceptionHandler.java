package asia.lhweb.findfriend.exception;

import lombok.extern.slf4j.Slf4j;
import asia.lhweb.findfriend.common.BaseResponse;
import asia.lhweb.findfriend.common.ErrorCode;
import asia.lhweb.findfriend.common.ResultUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理程序
 *
 * @author 罗汉
 * @date 2023/06/22
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    /**
     * 业务异常处理程序
     *
     * @param e e
     * @return {@link BaseResponse}
     */
    @ExceptionHandler(BusinessException.class)
    public BaseResponse businessExceptionHandler(BusinessException e) {
        log.error("businessException: " + e.getMessage(), e);
        return ResultUtils.error(e.getCode(), e.getMessage(), e.getDescription());
    }

    /**
     * 运行时异常处理程序
     *
     * @param e e
     * @return {@link BaseResponse}
     */
    @ExceptionHandler(RuntimeException.class)
    public BaseResponse runtimeExceptionHandler(RuntimeException e) {
        log.error("runtimeException", e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, e.getMessage(), "");
    }

}
