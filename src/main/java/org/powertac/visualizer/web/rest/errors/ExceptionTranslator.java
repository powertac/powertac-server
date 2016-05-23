package org.powertac.visualizer.web.rest.errors;

import java.io.FileNotFoundException;
import java.util.List;

import org.apache.commons.io.FileExistsException;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ResponseEntity.BodyBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartException;

/**
 * Controller advice to translate the server side exceptions to client-friendly json structures.
 */
@ControllerAdvice
public class ExceptionTranslator {

    @ExceptionHandler(ConcurrencyFailureException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    @ResponseBody
    public ErrorDTO processConcurencyError(ConcurrencyFailureException ex) {
        return new ErrorDTO(ErrorConstants.ERR_CONCURRENCY_FAILURE);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ErrorDTO processValidationError(MethodArgumentNotValidException ex) {
        BindingResult result = ex.getBindingResult();
        List<FieldError> fieldErrors = result.getFieldErrors();

        return processFieldErrors(fieldErrors);
    }

    @ExceptionHandler(CustomParameterizedException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ParameterizedErrorDTO processParameterizedValidationError(CustomParameterizedException ex) {
        return ex.getErrorDTO();
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ResponseBody
    public ErrorDTO processAccessDeniedExcpetion(AccessDeniedException e) {
        return new ErrorDTO(ErrorConstants.ERR_ACCESS_DENIED, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorDTO processIllegalArgumentException (IllegalArgumentException e) {
      return new ErrorDTO(ErrorConstants.ERR_ILLEGAL_ARGUMENT, e.getMessage());
    }

    @ExceptionHandler(FileNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public ErrorDTO processFileNotFoundException(FileNotFoundException e) {
        return new ErrorDTO(ErrorConstants.ERR_FILE_NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(FileExistsException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    @ResponseBody
    public ErrorDTO processFileExistsException(FileExistsException e) {
        return new ErrorDTO(ErrorConstants.ERR_FILE_EXISTS, e.getMessage());
    }

    @ExceptionHandler(MultipartException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    @ResponseBody
    public ErrorDTO processMultipartException(MultipartException e) {
        return new ErrorDTO(ErrorConstants.ERR_FILE_TOO_LARGE, e.getMessage());
    }

    @ExceptionHandler(SecurityException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ResponseBody
    public ErrorDTO processSecurityException(SecurityException e) {
        return new ErrorDTO(ErrorConstants.ERR_FORBIDDEN, e.getMessage());
    }

    private ErrorDTO processFieldErrors(List<FieldError> fieldErrors) {
        ErrorDTO dto = new ErrorDTO(ErrorConstants.ERR_VALIDATION);

        for (FieldError fieldError : fieldErrors) {
            dto.add(fieldError.getObjectName(), fieldError.getField(), fieldError.getCode());
        }

        return dto;
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public ErrorDTO processMethodNotSupportedException(HttpRequestMethodNotSupportedException exception) {
        return new ErrorDTO(ErrorConstants.ERR_METHOD_NOT_SUPPORTED, exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDTO> processRuntimeException(Exception ex) throws Exception {
        BodyBuilder builder;
        ErrorDTO errorDTO;
        ResponseStatus responseStatus = AnnotationUtils.findAnnotation(ex.getClass(), ResponseStatus.class);
        if (responseStatus != null) {
            builder = ResponseEntity.status(responseStatus.value());
            errorDTO = new ErrorDTO("error." + responseStatus.value().value(), responseStatus.reason() + ": " + ex.getMessage());
        } else {
            builder = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR);
            errorDTO = new ErrorDTO(ErrorConstants.ERR_INTERNAL_SERVER_ERROR, "Internal server error: " + ex.getMessage());
        }
        return builder.body(errorDTO);
    }
}
