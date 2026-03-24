package br.com.cuscrudrest.shared;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
}
