package br.com.cuscrudrest.types;

import java.util.UUID;

/**
 * Projecao interna completa de um tipo retornado pela camada JDBC.
 * Expõe os campos necessarios para o endpoint de leitura unitaria.
 * Efeitos colaterais: nenhum.
 *
 * @param typeId identificador do tipo.
 * @param nome nome do tipo.
 * @param imagem conteudo binario da imagem associada, quando houver.
 * @param inventoryId identificador do inventario ao qual o tipo pertence.
 */
public record TypeDetails(
        long typeId,
        String nome,
        byte[] imagem,
        UUID inventoryId
) {
}
