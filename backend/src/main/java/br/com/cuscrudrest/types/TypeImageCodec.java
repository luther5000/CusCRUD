package br.com.cuscrudrest.types;

import br.com.cuscrudrest.common.error.ValidationException;

import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utilitario de serializacao e validacao de imagens do dominio de tipos.
 * Centraliza a conversao entre bytes persistidos e o formato data URI exposto pela API.
 * Efeitos colaterais: nenhum.
 */
public final class TypeImageCodec {

    private static final String DEFAULT_IMAGE_MIME_TYPE = "image/png";
    private static final Pattern DATA_URI_PATTERN = Pattern.compile("^data:([^;]+);base64,(.+)$", Pattern.DOTALL);
    private static final int MAX_IMAGE_BYTES = 5 * 1024 * 1024;

    private TypeImageCodec() {
    }

    /**
     * Valida e decodifica uma imagem em data URI.
     *
     * @param imageDataUri imagem em data URI.
     * @return bytes decodificados da imagem.
     * @throws ValidationException quando a imagem nao esta em data URI valido ou excede 5 MiB.
     */
    public static byte[] parseDataUri(String imageDataUri) {
        Matcher matcher = DATA_URI_PATTERN.matcher(imageDataUri);
        if (!matcher.matches() || matcher.group(1).isBlank()) {
            throw new ValidationException(
                    "Imagem invalida.",
                    "imagem",
                    "must be a valid data URI with mime and base64 content"
            );
        }

        byte[] imageBytes;
        try {
            imageBytes = Base64.getDecoder().decode(matcher.group(2));
        } catch (IllegalArgumentException exception) {
            throw new ValidationException(
                    "Imagem invalida.",
                    "imagem",
                    "must be a valid data URI with mime and base64 content"
            );
        }

        if (imageBytes.length > MAX_IMAGE_BYTES) {
            throw new ValidationException(
                    "Imagem excede o tamanho maximo permitido.",
                    "imagem",
                    "must not exceed 5 MiB"
            );
        }

        return imageBytes;
    }

    /**
     * Serializa bytes persistidos da imagem para data URI.
     *
     * @param imageBytes bytes persistidos da imagem, quando houver.
     * @return data URI padrao da API, ou `null` quando nao houver imagem.
     */
    public static String toDataUri(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            return null;
        }

        return "data:" + DEFAULT_IMAGE_MIME_TYPE + ";base64," +
                Base64.getEncoder().encodeToString(imageBytes);
    }
}
