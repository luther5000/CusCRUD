package br.com.cuscrudrest.common.error;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Traduz excecoes da aplicacao para o formato padrao de erro da API.
 * Centraliza o mapeamento de validacao e conflitos para manter respostas consistentes entre endpoints.
 * Efeitos colaterais: intercepta excecoes lancadas durante o processamento das requisicoes HTTP.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Converte falhas de validacao do bean request em erro HTTP 400.
     * Estrategia: usa o primeiro FieldError reportado pelo Spring para preencher `details`.
     * Efeitos colaterais: nenhum alem da construcao da resposta HTTP.
     *
     * @param exception excecao produzida pela falha de validacao do request.
     * @return resposta 400 no formato padrao de erro da API.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        FieldError fieldError = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .orElse(null);
        String campo = fieldError != null ? fieldError.getField() : null;
        String info = fieldError != null ? fieldError.getDefaultMessage() : "invalid request body";
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "Payload invalido.",
                campo,
                info
        );
    }

    /**
     * Converte payloads JSON malformados ou campos com tipo invalido em erro HTTP 400.
     * Estrategia: tenta extrair o primeiro campo Jackson associado a falha e usa uma mensagem padrao para o cliente.
     * Efeitos colaterais: nenhum alem da construcao da resposta HTTP.
     *
     * @param exception excecao produzida por falha de desserializacao do corpo da requisicao.
     * @return resposta 400 no formato padrao de erro da API.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException exception) {
        InvalidFormatException invalidFormatException = findCause(exception, InvalidFormatException.class);
        JsonMappingException jsonMappingException = invalidFormatException != null
                ? invalidFormatException
                : findCause(exception, JsonMappingException.class);
        String campo = null;
        String info = "invalid request body";

        if (invalidFormatException != null) {
            campo = extractFirstField(invalidFormatException);
            info = "invalid field format";
        } else if (jsonMappingException != null) {
            campo = extractFirstField(jsonMappingException);
        }

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "Payload invalido.",
                campo,
                info
        );
    }

    /**
     * Converte falhas de validacao de negocio em erro HTTP 400.
     *
     * @param exception excecao de validacao lancada pela camada de servico.
     * @return resposta 400 no formato padrao de erro da API.
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(ValidationException exception) {
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                exception.getMessage(),
                exception.getCampo(),
                exception.getInfo()
        );
    }

    /**
     * Converte conflitos de negocio em erro HTTP 409.
     *
     * @param exception excecao de conflito lancada pela camada de servico.
     * @return resposta 409 no formato padrao de erro da API.
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(ConflictException exception) {
        return buildErrorResponse(
                HttpStatus.CONFLICT,
                "CONFLICT",
                exception.getMessage(),
                exception.getCampo(),
                exception.getInfo()
        );
    }

    /**
     * Converte falhas de autorizacao em erro HTTP 403.
     *
     * @param exception excecao de autorizacao lancada pela camada de servico.
     * @return resposta 403 no formato padrao de erro da API.
     */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiErrorResponse> handleForbidden(ForbiddenException exception) {
        return buildErrorResponse(
                HttpStatus.FORBIDDEN,
                "FORBIDDEN",
                exception.getMessage(),
                exception.getCampo(),
                exception.getInfo()
        );
    }

    /**
     * Converte recursos ausentes em erro HTTP 404.
     *
     * @param exception excecao de nao encontrado lancada pela camada de servico.
     * @return resposta 404 no formato padrao de erro da API.
     */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(NotFoundException exception) {
        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "NOT_FOUND",
                exception.getMessage(),
                exception.getCampo(),
                exception.getInfo()
        );
    }

    /**
     * Converte falhas de autenticacao em erro HTTP 401.
     *
     * @param exception excecao de autenticacao lancada pela camada de servico ou seguranca.
     * @return resposta 401 no formato padrao de erro da API.
     */
    @ExceptionHandler(UnauthenticatedException.class)
    public ResponseEntity<ApiErrorResponse> handleUnauthenticated(UnauthenticatedException exception) {
        return buildErrorResponse(
                HttpStatus.UNAUTHORIZED,
                "UNAUTHENTICATED",
                exception.getMessage(),
                exception.getCampo(),
                exception.getInfo()
        );
    }

    /**
     * Monta a resposta padrao de erro da API.
     *
     * @param status status HTTP a ser retornado.
     * @param code codigo interno estavel do erro.
     * @param message mensagem curta e acionavel para o cliente.
     * @param campo campo relacionado a falha, quando aplicavel.
     * @param info detalhe resumido adicional da falha.
     * @return resposta HTTP no formato padrao de erro.
     */
    private ResponseEntity<ApiErrorResponse> buildErrorResponse(
            HttpStatus status,
            String code,
            String message,
            String campo,
            String info
    ) {
        ApiErrorDetails details = new ApiErrorDetails(campo, info);
        ApiErrorBody errorBody = new ApiErrorBody(code, message, details);
        return ResponseEntity.status(status).body(new ApiErrorResponse(errorBody));
    }

    private String extractFirstField(JsonMappingException exception) {
        return exception.getPath().stream()
                .map(JsonMappingException.Reference::getFieldName)
                .filter(fieldName -> fieldName != null && !fieldName.isBlank())
                .findFirst()
                .orElse(null);
    }

    private <T extends Throwable> T findCause(Throwable throwable, Class<T> type) {
        Throwable current = throwable;

        while (current != null) {
            if (type.isInstance(current)) {
                return type.cast(current);
            }
            current = current.getCause();
        }

        return null;
    }
}
