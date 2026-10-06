package com.lmf.finpro.infrastructure.web.exception;

import com.lmf.finpro.domain.exception.AttachmentInvalidException;
import com.lmf.finpro.domain.exception.CategoryTypeMismatchException;
import com.lmf.finpro.domain.exception.CepNotFoundException;
import com.lmf.finpro.domain.exception.CepServiceUnavailableException;
import com.lmf.finpro.domain.exception.CurrencyChangeNotAllowedException;
import com.lmf.finpro.domain.exception.DocumentAlreadyInUseException;
import com.lmf.finpro.domain.exception.EmailAlreadyInUseException;
import com.lmf.finpro.domain.exception.EntityHasLinkedRecordsException;
import com.lmf.finpro.domain.exception.ExchangeRateUnavailableException;
import com.lmf.finpro.domain.exception.HouseholdPermissionException;
import com.lmf.finpro.domain.exception.HouseholdRuleException;
import com.lmf.finpro.domain.exception.ImportFileInvalidException;
import com.lmf.finpro.domain.exception.IncorrectCurrentPasswordException;
import com.lmf.finpro.domain.exception.InsufficientBalanceException;
import com.lmf.finpro.domain.exception.InvalidCredentialsException;
import com.lmf.finpro.domain.exception.InvalidHouseholdInviteException;
import com.lmf.finpro.domain.exception.InvalidPasswordResetTokenException;
import com.lmf.finpro.domain.exception.InvalidRecurrencePeriodException;
import com.lmf.finpro.domain.exception.InvalidTagException;
import com.lmf.finpro.domain.exception.PaidTransactionLockedException;
import com.lmf.finpro.domain.exception.ResourceNotFoundException;
import com.lmf.finpro.domain.exception.SameAccountTransferException;
import com.lmf.finpro.domain.exception.TagAlreadyExistsException;
import com.lmf.finpro.domain.exception.TransactionLinkedToTransferException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(EmailAlreadyInUseException.class)
    public ResponseEntity<ApiError> handleEmailAlreadyInUse(
            EmailAlreadyInUseException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(DocumentAlreadyInUseException.class)
    public ResponseEntity<ApiError> handleDocumentAlreadyInUse(
            DocumentAlreadyInUseException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiError> handleInvalidCredentials(
            InvalidCredentialsException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ApiError> handleTooManyRequests(
            TooManyRequestsException ex, HttpServletRequest request) {
        ResponseEntity<ApiError> response =
                build(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), request);
        return ResponseEntity.status(response.getStatusCode())
                .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
                .body(response.getBody());
    }

    @ExceptionHandler(IncorrectCurrentPasswordException.class)
    public ResponseEntity<ApiError> handleIncorrectCurrentPassword(
            IncorrectCurrentPasswordException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidPasswordResetTokenException.class)
    public ResponseEntity<ApiError> handleInvalidPasswordResetToken(
            InvalidPasswordResetTokenException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(HouseholdPermissionException.class)
    public ResponseEntity<ApiError> handleHouseholdPermission(
            HouseholdPermissionException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    @ExceptionHandler(HouseholdRuleException.class)
    public ResponseEntity<ApiError> handleHouseholdRule(
            HouseholdRuleException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidHouseholdInviteException.class)
    public ResponseEntity<ApiError> handleInvalidHouseholdInvite(
            InvalidHouseholdInviteException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(CepNotFoundException.class)
    public ResponseEntity<ApiError> handleCepNotFound(
            CepNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(CepServiceUnavailableException.class)
    public ResponseEntity<ApiError> handleCepServiceUnavailable(
            CepServiceUnavailableException ex, HttpServletRequest request) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request);
    }

    @ExceptionHandler(ExchangeRateUnavailableException.class)
    public ResponseEntity<ApiError> handleExchangeRateUnavailable(
            ExchangeRateUnavailableException ex, HttpServletRequest request) {
        return build(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request);
    }

    @ExceptionHandler(CurrencyChangeNotAllowedException.class)
    public ResponseEntity<ApiError> handleCurrencyChangeNotAllowed(
            CurrencyChangeNotAllowedException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(SameAccountTransferException.class)
    public ResponseEntity<ApiError> handleSameAccountTransfer(
            SameAccountTransferException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ApiError> handleInsufficientBalance(
            InsufficientBalanceException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(PaidTransactionLockedException.class)
    public ResponseEntity<ApiError> handlePaidTransactionLocked(
            PaidTransactionLockedException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(TransactionLinkedToTransferException.class)
    public ResponseEntity<ApiError> handleTransactionLinkedToTransfer(
            TransactionLinkedToTransferException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(CategoryTypeMismatchException.class)
    public ResponseEntity<ApiError> handleCategoryTypeMismatch(
            CategoryTypeMismatchException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidRecurrencePeriodException.class)
    public ResponseEntity<ApiError> handleInvalidRecurrencePeriod(
            InvalidRecurrencePeriodException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(EntityHasLinkedRecordsException.class)
    public ResponseEntity<ApiError> handleEntityHasLinkedRecords(
            EntityHasLinkedRecordsException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(ImportFileInvalidException.class)
    public ResponseEntity<ApiError> handleImportFileInvalid(
            ImportFileInvalidException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(AttachmentInvalidException.class)
    public ResponseEntity<ApiError> handleAttachmentInvalid(
            AttachmentInvalidException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidTagException.class)
    public ResponseEntity<ApiError> handleInvalidTag(
            InvalidTagException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(TagAlreadyExistsException.class)
    public ResponseEntity<ApiError> handleTagAlreadyExists(
            TagAlreadyExistsException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    /** Arquivo acima do limite de upload do Spring (spring.servlet.multipart.max-file-size). */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleMaxUploadSize(
            MaxUploadSizeExceededException ex, HttpServletRequest request) {
        return build(
                HttpStatus.PAYLOAD_TOO_LARGE,
                "O arquivo é grande demais. O limite é de 10 MB por arquivo.",
                request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {
        Stream<String> fieldMessages =
                ex.getBindingResult().getFieldErrors().stream()
                        .map(error -> error.getField() + ": " + error.getDefaultMessage());
        // Validações de nível de classe (ex: @ValidDocumentNumber) chegam como erros globais, não
        // de campo.
        Stream<String> globalMessages =
                ex.getBindingResult().getGlobalErrors().stream()
                        .map(ObjectError::getDefaultMessage);
        String message =
                Stream.concat(fieldMessages, globalMessages).collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleBadRequest(
            IllegalArgumentException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ApiError> handleNotAcceptable(
            HttpMediaTypeNotAcceptableException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_ACCEPTABLE, ex.getMessage(), request);
    }

    /** URL sem rota nem arquivo estático: erro do cliente, não do servidor. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResource(
            NoResourceFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "Recurso não encontrado", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, ex.getMessage(), request);
    }

    /** JSON malformado, tipo errado, parâmetro obrigatório ausente ou valor que não converte. */
    @ExceptionHandler({
        HttpMessageNotReadableException.class,
        MissingServletRequestParameterException.class,
        MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiError> handleMalformedRequest(
            Exception ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "Requisição inválida", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest request) {
        if (ex instanceof DataAccessException) {
            // A mensagem do driver traz os valores da linha (ex.: "Key (email)=(x@y) already
            // exists"); no log vão só os tipos da exceção.
            Throwable root = NestedExceptionUtils.getMostSpecificCause(ex);
            log.error(
                    "Erro de acesso a dados em {} {} (erro={}, causa={})",
                    request.getMethod(),
                    request.getRequestURI(),
                    ex.getClass().getSimpleName(),
                    root.getClass().getSimpleName());
        } else {
            log.error("Erro inesperado em {} {}", request.getMethod(), request.getRequestURI(), ex);
        }
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno inesperado", request);
    }

    private ResponseEntity<ApiError> build(
            HttpStatus status, String message, HttpServletRequest request) {
        logRejection(status, message, request);
        ApiError body =
                new ApiError(
                        LocalDateTime.now(),
                        status.value(),
                        status.getReasonPhrase(),
                        message,
                        request.getRequestURI());
        return ResponseEntity.status(status).body(body);
    }

    /**
     * Erros tratados (regra de negócio, validação, não autorizado...) não são falha do servidor,
     * mas precisam deixar rastro: é o que explica por que a tela do usuário mostrou um erro. Loga a
     * mesma mensagem que vai na resposta, sem quebras de linha. 5xx já é logado em {@code ERROR}
     * pelo handler genérico.
     */
    private void logRejection(HttpStatus status, String message, HttpServletRequest request) {
        if (status.is5xxServerError() && status != HttpStatus.SERVICE_UNAVAILABLE) {
            return;
        }
        String safeMessage = message == null ? "" : message.replaceAll("[\\r\\n]+", " ");
        if (status == HttpStatus.NOT_FOUND) {
            log.info(
                    "Requisição rejeitada {} {} -> {}: {}",
                    request.getMethod(),
                    request.getRequestURI(),
                    status.value(),
                    safeMessage);
        } else {
            log.warn(
                    "Requisição rejeitada {} {} -> {}: {}",
                    request.getMethod(),
                    request.getRequestURI(),
                    status.value(),
                    safeMessage);
        }
    }
}
