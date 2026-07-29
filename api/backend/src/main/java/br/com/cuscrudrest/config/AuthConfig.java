package br.com.cuscrudrest.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Configuracao base das propriedades e utilitarios de autenticacao.
 * Registra beans compartilhados pelos fluxos de emissao e validacao de JWT.
 * Efeitos colaterais: registra beans tecnicos no contexto Spring.
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CusCrudAuthProperties.class)
public class AuthConfig {

    /**
     * Cria o relogio padrao da aplicacao.
     * Estrategia: usa o relogio UTC do sistema para gerar e validar claims temporais do JWT.
     * Efeitos colaterais: nenhum adicional alem do registro do bean.
     *
     * @return relogio do sistema em UTC.
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
