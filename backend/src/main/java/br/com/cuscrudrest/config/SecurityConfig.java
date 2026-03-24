package br.com.cuscrudrest.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuracao base de seguranca da aplicacao.
 * Fornece o encoder de senha e uma politica HTTP inicial permissiva para manter o bootstrap funcional ate a entrada do JWT.
 * Efeitos colaterais: registra beans de seguranca no contexto Spring.
 */
@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

    /**
     * Cria o encoder Bcrypt padrao do projeto.
     * Estrategia: utiliza BCryptPasswordEncoder com parametros padrao do Spring Security.
     * Efeitos colaterais: nenhum adicional alem do registro do bean.
     *
     * @return encoder de senha para hash e verificacao de `passwd`.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Define a cadeia HTTP de seguranca inicial da aplicacao.
     * Estrategia: desabilita CSRF para a API stateless e permite todas as rotas ate a configuracao de autenticacao JWT.
     * Efeitos colaterais: intercepta requests HTTP pelo filtro do Spring Security.
     *
     * @param http builder da configuracao de seguranca web.
     * @return cadeia de filtros aplicada a todos os endpoints HTTP.
     * @throws Exception quando o Spring Security falha ao construir a cadeia de filtros.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .build();
    }
}
