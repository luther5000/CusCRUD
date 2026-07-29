package br.com.cuscrudrest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point da aplicacao backend do projeto CusCRUD.
 * Inicializa o contexto Spring e sobe o servidor HTTP configurado para a API.
 * Efeitos colaterais: abre a porta HTTP e registra os beans da aplicacao.
 */
@SpringBootApplication
public class CusCrudRestApplication {

    /**
     * Inicializa a aplicacao Spring Boot.
     *
     * @param args argumentos de linha de comando repassados para o runtime.
     */
    public static void main(String[] args) {
        SpringApplication.run(CusCrudRestApplication.class, args);
    }
}
