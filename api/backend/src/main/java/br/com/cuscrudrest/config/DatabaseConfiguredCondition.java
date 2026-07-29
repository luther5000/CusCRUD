package br.com.cuscrudrest.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.StringUtils;

/**
 * Condition que habilita beans dependentes de banco apenas quando as credenciais estao configuradas.
 * Verifica URL, usuario e senha definidos para evitar falha de bootstrap em ambientes sem Postgres ativo.
 * Efeitos colaterais: nenhum.
 */
public class DatabaseConfiguredCondition implements Condition {

    /**
     * Determina se a configuracao minima de banco esta presente no ambiente atual.
     *
     * @param context contexto Spring com acesso ao ambiente e ao registry de beans.
     * @param metadata metadados do elemento anotado que esta sendo avaliado.
     * @return true quando URL, usuario e senha do banco possuem texto nao vazio; false caso contrario.
     */
    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        Environment environment = context.getEnvironment();
        return hasTextProperty(environment, "cuscrud.database.url")
                && hasTextProperty(environment, "cuscrud.database.user")
                && hasTextProperty(environment, "cuscrud.database.password");
    }

    /**
     * Verifica se uma propriedade do ambiente contem texto util.
     *
     * @param environment fonte de propriedades ativa no contexto Spring.
     * @param propertyName nome completo da propriedade que deve ser consultada.
     * @return true quando a propriedade existe e contem pelo menos um caractere nao vazio.
     */
    private boolean hasTextProperty(Environment environment, String propertyName) {
        return StringUtils.hasText(environment.getProperty(propertyName));
    }
}
