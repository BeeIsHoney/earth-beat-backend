package org.example.earthjukebox.temperature;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice(basePackages = "org.example.earthjukebox")
public class TemperatureExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> handleDataError(ResponseStatusException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(exception.getStatusCode(),
                exception.getReason() == null ? "Data ကို ဖတ်၍မရပါ။" : exception.getReason());
        return ResponseEntity.status(exception.getStatusCode()).body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException exception, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status,
                "Input range မမှန်ပါ။ year=1880–9999၊ month=1–12၊ rainfallMm>=0၊ ndvi=-1 မှ 1 အတွင်း ဖြစ်ရပါမည်။");
        return handleExceptionInternal(exception, problem, headers, status, request);
    }
}

