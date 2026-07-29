package br.com.cuscrudrest.config;

import br.com.cuscrudrest.auth.security.ApiAuthenticationEntryPoint;
import br.com.cuscrudrest.auth.security.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

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
     * Define a cadeia HTTP de seguranca da aplicacao.
     * Estrategia: desabilita CSRF, torna a API stateless, libera apenas rotas publicas e aplica o filtro JWT nas demais.
     * Efeitos colaterais: intercepta requests HTTP pelo filtro do Spring Security.
     *
     * @param http builder da configuracao de seguranca web.
     * @param jwtAuthenticationFilterProvider provedor opcional do filtro JWT, disponivel quando o banco esta configurado.
     * @param authenticationEntryPoint entry point da API para respostas 401 no formato padrao.
     * @return cadeia de filtros aplicada a todos os endpoints HTTP.
     * @throws Exception quando o Spring Security falha ao construir a cadeia de filtros.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ObjectProvider<JwtAuthenticationFilter> jwtAuthenticationFilterProvider,
            ObjectProvider<ApiAuthenticationEntryPoint> authenticationEntryPointProvider,
            ObjectMapper objectMapper
    ) throws Exception {
        JwtAuthenticationFilter jwtAuthenticationFilter = jwtAuthenticationFilterProvider.getIfAvailable();
        ApiAuthenticationEntryPoint authenticationEntryPoint = authenticationEntryPointProvider.getIfAvailable(
                () -> new ApiAuthenticationEntryPoint(objectMapper)
        );

        HttpSecurity configuredHttp = http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception.authenticationEntryPoint(authenticationEntryPoint))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/health", "/auth/login", "/auth/register").permitAll()
                        .anyRequest().authenticated()
                );

        if (jwtAuthenticationFilter != null) {
            configuredHttp.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        }

        return configuredHttp.build();
    }
}
