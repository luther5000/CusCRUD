package br.com.cuscrudrest.types;

/**
 * Projecao interna de um tipo retornado pela camada JDBC.
 * Expõe apenas os dados necessarios para a listagem do endpoint.
 * Efeitos colaterais: nenhum.
 *
 * @param typeId identificador do tipo.
 * @param nome nome do tipo.
 * @param hasImage indica se o tipo possui imagem associada.
 */
public record TypeSummary(
        long typeId,
        String nome,
        boolean hasImage
) {
}
