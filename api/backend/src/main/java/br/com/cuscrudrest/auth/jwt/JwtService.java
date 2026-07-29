package br.com.cuscrudrest.auth.jwt;

import br.com.cuscrudrest.config.CusCrudAuthProperties;
import br.com.cuscrudrest.common.error.UnauthenticatedException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Servico responsavel pela emissao e validacao de tokens JWT HS256.
 * Centraliza a montagem do payload, assinatura HMAC-SHA256 e verificacao das claims exigidas pela especificacao.
 * Efeitos colaterais: nenhum. Opera apenas sobre dados em memoria.
 */
@Service
public class JwtService {

    private static final String JWT_ALGORITHM = "HS256";
    private static final String JWT_TYPE = "JWT";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final ZoneId APPLICATION_TIME_ZONE = ZoneId.of("America/Recife");

    private final CusCrudAuthProperties authProperties;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    /**
     * Cria o servico de JWT da aplicacao.
     *
     * @param authProperties propriedades com segredo e TTL fixo do token.
     * @param objectMapper serializador JSON usado para montar e ler header/payload do JWT.
     * @param clock relogio da aplicacao usado para emissao e validacao temporal do token.
     */
    public JwtService(CusCrudAuthProperties authProperties, ObjectMapper objectMapper, Clock clock) {
        this.authProperties = authProperties;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    /**
     * Emite um novo token JWT para o usuario informado.
     * Estrategia: cria header/payload conforme a especificacao, calcula assinatura HS256 e retorna o token com metadados temporais.
     * Efeitos colaterais: nenhum.
     *
     * @param userId identificador do usuario que sera colocado na claim `sub`.
     * @return token assinado e seus metadados temporais.
     */
    public IssuedJwtToken issueToken(UUID userId) {
        Instant issuedInstant = clock.instant();
        Instant expiresInstant = issuedInstant.plusSeconds(authProperties.jwtTtlSeconds());

        String headerSegment = encodeJsonSegment(Map.of(
                "alg", JWT_ALGORITHM,
                "typ", JWT_TYPE
        ));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", userId.toString());
        payload.put("iat", issuedInstant.getEpochSecond());
        payload.put("exp", expiresInstant.getEpochSecond());

        String payloadSegment = encodeJsonSegment(payload);
        String signingInput = headerSegment + "." + payloadSegment;
        String signatureSegment = base64UrlEncode(sign(signingInput));

        return new IssuedJwtToken(
                signingInput + "." + signatureSegment,
                authProperties.jwtTtlSeconds(),
                toApplicationOffsetDateTime(issuedInstant),
                toApplicationOffsetDateTime(expiresInstant)
        );
    }

    /**
     * Resolve o token JWT bruto a partir do header Authorization.
     * Estrategia: exige o prefixo `Bearer ` e remove esse prefixo do valor recebido.
     * Efeitos colaterais: nenhum.
     *
     * @param authorizationHeader valor completo do header `Authorization`.
     * @return token JWT sem o prefixo `Bearer `.
     * @throws UnauthenticatedException quando o header estiver ausente, vazio ou fora do formato esperado.
     */
    public String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new UnauthenticatedException("Token nao enviado ou invalido.", "Authorization", "missing bearer token");
        }

        if (!authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new UnauthenticatedException("Token nao enviado ou invalido.", "Authorization", "invalid bearer prefix");
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isBlank()) {
            throw new UnauthenticatedException("Token nao enviado ou invalido.", "Authorization", "blank bearer token");
        }

        return token;
    }

    /**
     * Valida um token JWT assinado pela aplicacao e extrai suas claims.
     * Estrategia: valida estrutura, header, assinatura HS256 e expiracao antes de converter as claims para tipos de dominio.
     * Efeitos colaterais: nenhum.
     *
     * @param token token JWT compacto a ser validado.
     * @return claims do token convertidas para um tipo imutavel de dominio.
     * @throws UnauthenticatedException quando o token estiver malformado, adulterado, invalido ou expirado.
     */
    public ValidatedJwtToken validateToken(String token) {
        String[] segments = token != null ? token.split("\\.") : new String[0];
        if (segments.length != 3) {
            throw new UnauthenticatedException("Token nao enviado ou invalido.", "Authorization", "malformed jwt");
        }

        String signingInput = segments[0] + "." + segments[1];
        byte[] expectedSignature = sign(signingInput);
        byte[] providedSignature = base64UrlDecode(segments[2]);

        if (!MessageDigest.isEqual(expectedSignature, providedSignature)) {
            throw new UnauthenticatedException("Token nao enviado ou invalido.", "Authorization", "invalid jwt signature");
        }

        JsonNode headerNode = readJsonNode(base64UrlDecode(segments[0]));
        JsonNode payloadNode = readJsonNode(base64UrlDecode(segments[1]));

        validateHeader(headerNode);

        UUID userId = parseUserId(payloadNode);
        long issuedAtEpochSecond = parseEpochSecondClaim(payloadNode, "iat");
        long expiresAtEpochSecond = parseEpochSecondClaim(payloadNode, "exp");

        Instant issuedInstant = Instant.ofEpochSecond(issuedAtEpochSecond);
        Instant expiresInstant = Instant.ofEpochSecond(expiresAtEpochSecond);
        Instant now = clock.instant();

        if (!issuedInstant.isBefore(expiresInstant) && !issuedInstant.equals(expiresInstant)) {
            throw new UnauthenticatedException("Token nao enviado ou invalido.", "Authorization", "invalid jwt timestamps");
        }

        if (!now.isBefore(expiresInstant)) {
            throw new UnauthenticatedException("Token ausente, invalido ou expirado.", "Authorization", "expired jwt");
        }

        return new ValidatedJwtToken(
                userId,
                toApplicationOffsetDateTime(issuedInstant),
                toApplicationOffsetDateTime(expiresInstant),
                expiresAtEpochSecond - issuedAtEpochSecond
        );
    }

    /**
     * Valida o header do token conforme a especificacao HS256/JWT.
     *
     * @param headerNode JSON decodificado do header do token.
     * @throws UnauthenticatedException quando o header estiver ausente ou com algoritmo/tipo inesperados.
     */
    private void validateHeader(JsonNode headerNode) {
        String algorithm = headerNode.path("alg").asText(null);
        String type = headerNode.path("typ").asText(null);

        if (!JWT_ALGORITHM.equals(algorithm) || !JWT_TYPE.equals(type)) {
            throw new UnauthenticatedException("Token nao enviado ou invalido.", "Authorization", "invalid jwt header");
        }
    }

    /**
     * Extrai o `user_id` da claim `sub`.
     *
     * @param payloadNode JSON decodificado do payload do token.
     * @return identificador UUID do usuario autenticado.
     * @throws UnauthenticatedException quando a claim `sub` estiver ausente ou nao puder ser convertida para UUID.
     */
    private UUID parseUserId(JsonNode payloadNode) {
        String subject = payloadNode.path("sub").asText(null);
        if (subject == null || subject.isBlank()) {
            throw new UnauthenticatedException("Token nao enviado ou invalido.", "Authorization", "missing jwt sub");
        }

        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException exception) {
            throw new UnauthenticatedException("Token nao enviado ou invalido.", "Authorization", "invalid jwt sub");
        }
    }

    /**
     * Extrai uma claim temporal numérica do payload.
     *
     * @param payloadNode JSON decodificado do payload do token.
     * @param claimName nome da claim a ser lida.
     * @return valor epoch second da claim solicitada.
     * @throws UnauthenticatedException quando a claim estiver ausente ou nao for numerica.
     */
    private long parseEpochSecondClaim(JsonNode payloadNode, String claimName) {
        JsonNode claimNode = payloadNode.path(claimName);
        if (!claimNode.isIntegralNumber()) {
            throw new UnauthenticatedException("Token nao enviado ou invalido.", "Authorization", "invalid jwt claim " + claimName);
        }
        return claimNode.asLong();
    }

    /**
     * Codifica um objeto simples em JSON e Base64URL sem padding para formar um segmento JWT.
     *
     * @param value objeto serializavel para JSON.
     * @return segmento JWT codificado em Base64URL.
     */
    private String encodeJsonSegment(Object value) {
        try {
            return base64UrlEncode(objectMapper.writeValueAsBytes(value));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize JWT segment", exception);
        }
    }

    /**
     * Converte bytes JSON em uma arvore de leitura.
     *
     * @param jsonBytes representacao em bytes do JSON decodificado do JWT.
     * @return arvore JsonNode para leitura de claims e header.
     * @throws UnauthenticatedException quando o JSON do token estiver corrompido ou invalido.
     */
    private JsonNode readJsonNode(byte[] jsonBytes) {
        try {
            return objectMapper.readTree(jsonBytes);
        } catch (IOException exception) {
            throw new UnauthenticatedException("Token nao enviado ou invalido.", "Authorization", "invalid jwt json");
        }
    }

    /**
     * Gera a assinatura HMAC-SHA256 do conteudo informado.
     *
     * @param signingInput combinacao `base64url(header) + "." + base64url(payload)`.
     * @return bytes crus da assinatura.
     */
    private byte[] sign(String signingInput) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(authProperties.jwtSecret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException | InvalidKeyException exception) {
            throw new IllegalStateException("Could not sign JWT token", exception);
        }
    }

    /**
     * Codifica bytes em Base64URL sem padding.
     *
     * @param bytes dados a serem codificados.
     * @return string Base64URL sem padding.
     */
    private String base64UrlEncode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Decodifica um segmento Base64URL do JWT.
     *
     * @param encodedSegment segmento JWT codificado.
     * @return bytes decodificados.
     * @throws UnauthenticatedException quando o segmento nao puder ser decodificado.
     */
    private byte[] base64UrlDecode(String encodedSegment) {
        try {
            return Base64.getUrlDecoder().decode(encodedSegment);
        } catch (IllegalArgumentException exception) {
            throw new UnauthenticatedException("Token nao enviado ou invalido.", "Authorization", "invalid jwt encoding");
        }
    }

    /**
     * Converte um instante UTC para OffsetDateTime na timezone padrao da aplicacao.
     *
     * @param instant instante UTC a ser convertido.
     * @return representacao com offset da timezone `America/Recife`.
     */
    private OffsetDateTime toApplicationOffsetDateTime(Instant instant) {
        return OffsetDateTime.ofInstant(instant, APPLICATION_TIME_ZONE);
    }
}
