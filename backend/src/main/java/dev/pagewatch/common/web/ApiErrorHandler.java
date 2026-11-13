package dev.pagewatch.common.web;


import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import java.util.Map;

@RestControllerAdvice
public class ApiErrorHandler {
    @ExceptionHandler(ApiException.class)
    ResponseEntity<Map<String,String>> api(ApiException ex) { return ResponseEntity.status(ex.status()).body(Map.of("message",ex.getMessage())); }
    @ExceptionHandler({MethodArgumentNotValidException.class,HttpMessageNotReadableException.class,MethodArgumentTypeMismatchException.class})
    ResponseEntity<Map<String,String>> validation(Exception ex) {
        return ResponseEntity.badRequest().body(Map.of("message","输入不正确：名称需为 1–120 字符，检查间隔需为 1–10080 分钟，请检查网址和选择器长度"));
    }
}
