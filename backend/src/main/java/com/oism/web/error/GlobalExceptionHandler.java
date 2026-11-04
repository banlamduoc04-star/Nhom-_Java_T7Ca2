package com.oism.web.error;

import com.oism.domain.exception.BusinessException;
import com.oism.domain.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;

/**
 * Doi moi loi thanh JSON {errorCode, message, details}. Ke thua ResponseEntityExceptionHandler
 * de cac loi MVC chuan (JSON hong, sai method, 404...) cung dung dinh dang nay.
 * Loi he thong khong lo stack trace/chi tiet noi bo ra client (NFR-SEC).
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<Object> handleBusiness(BusinessException ex) {
        ErrorCode code = ex.getErrorCode();
        return ResponseEntity.status(code.httpStatus()).body(ApiError.of(code.name(), ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<Object> handleAccessDenied(AccessDeniedException ex) {
        ErrorCode code = ErrorCode.AU40301;
        return ResponseEntity.status(code.httpStatus()).body(ApiError.of(code.name(), code.defaultMessage()));
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<Object> handleAuthentication(AuthenticationException ex) {
        ErrorCode code = ErrorCode.AU40101;
        return ResponseEntity.status(code.httpStatus()).body(ApiError.of(code.name(), code.defaultMessage()));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<Object> handleUnexpected(Exception ex) {
        log.error("Lỗi không mong đợi", ex);
        ErrorCode code = ErrorCode.SY50001;
        return ResponseEntity.status(code.httpStatus()).body(ApiError.of(code.name(), code.defaultMessage()));
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        List<FieldDetail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new FieldDetail(e.getField(), e.getDefaultMessage()))
                .toList();
        String code = validationCode(request);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(code, "Dữ liệu không hợp lệ", details));
    }

    /** Cac loi MVC chuan: 400 JSON hong, 404, 405, 415... */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        int status = statusCode.value();
        String code = status == 400 ? validationCode(request) : String.format("SY%d01", status);
        String message = switch (status) {
            case 400 -> "Dữ liệu gửi lên không hợp lệ";
            case 404 -> "Không tìm thấy tài nguyên";
            case 405 -> "Phương thức không được hỗ trợ";
            case 415 -> "Định dạng dữ liệu không được hỗ trợ";
            default -> "Yêu cầu không thể xử lý";
        };
        return ResponseEntity.status(statusCode).headers(headers).body(ApiError.of(code, message));
    }

    /** Ma loi "du lieu khong hop le" theo module, suy ra tu duong dan yeu cau. */
    static String validationCode(WebRequest request) {
        String description = request.getDescription(false); // "uri=/api/v1/..."
        String path = description.startsWith("uri=") ? description.substring(4) : description;
        String resource = path.replaceFirst("^/api/v1/", "");
        if (resource.startsWith("categories") || resource.startsWith("brands")
                || resource.startsWith("products") || resource.startsWith("skus")) {
            return ErrorCode.PR40001.name();
        }
        if (resource.startsWith("orders") || resource.startsWith("webhooks")) {
            return ErrorCode.OD40001.name();
        }
        if (resource.startsWith("reports") || resource.startsWith("dashboard")) {
            return ErrorCode.RP40001.name();
        }
        if (resource.startsWith("inventory") || resource.startsWith("stock")
                || resource.startsWith("transfers") || resource.startsWith("stocktakes")) {
            return ErrorCode.IV40001.name();
        }
        return ErrorCode.AU40001.name();
    }
}
