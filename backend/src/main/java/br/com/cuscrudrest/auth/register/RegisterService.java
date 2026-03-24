package br.com.cuscrudrest.auth.register;

import br.com.cuscrudrest.auth.support.EmailAddressValidator;
import br.com.cuscrudrest.auth.support.PasswordHasher;
import br.com.cuscrudrest.auth.user.UserAccount;
import br.com.cuscrudrest.auth.user.UserRepository;
import br.com.cuscrudrest.common.error.ConflictException;
import br.com.cuscrudrest.common.error.ValidationException;
import br.com.cuscrudrest.config.DatabaseConfiguredCondition;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

/**
 * Servico de autenticacao e cadastro da aplicacao.
 * Centraliza validacoes de negocio e coordenacao entre validadores, hash de senha e persistencia.
 * Efeitos colaterais: cria registros de usuario na base quando o cadastro e bem-sucedido.
 */
@Service
@Conditional(DatabaseConfiguredCondition.class)
public class RegisterService {

    private final EmailAddressValidator emailAddressValidator;
    private final PasswordHasher passwordHasher;
    private final UserRepository userRepository;

    /**
     * Cria o servico de autenticacao.
     *
     * @param emailAddressValidator validador de formato de email da aplicacao.
     * @param passwordHasher componente responsavel pelo hash das senhas.
     * @param userRepository repositorio JDBC dos usuarios.
     */
    public RegisterService(
            EmailAddressValidator emailAddressValidator,
            PasswordHasher passwordHasher,
            UserRepository userRepository
    ) {
        this.emailAddressValidator = emailAddressValidator;
        this.passwordHasher = passwordHasher;
        this.userRepository = userRepository;
    }

    /**
     * Registra um novo usuario no sistema.
     * Estrategia: valida email e unicidade do login, gera o hash Bcrypt da senha e persiste o usuario na tabela `users`.
     * Efeitos colaterais: cria um novo usuario persistido quando a operacao e valida.
     *
     * @param request payload de cadastro recebido pela camada HTTP.
     * @return dados publicos do usuario criado.
     * @throws ValidationException quando o email informado nao possui formato valido.
     * @throws ConflictException quando o login ja esta cadastrado no sistema.
     */
    public RegisterResponse register(RegisterRequest request) {
        validateEmail(request.login());
        ensureLoginIsAvailable(request.login());

        String encodedPassword = passwordHasher.hash(request.passwd());

        try {
            UserAccount insertedUser = userRepository.insertUser(request.name(), request.login(), encodedPassword);
            return new RegisterResponse(
                    insertedUser.userId(),
                    insertedUser.name(),
                    insertedUser.login(),
                    insertedUser.createdAt()
            );
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Login ja cadastrado no sistema.", "login", "already exists");
        }
    }

    /**
     * Valida o formato do email de cadastro.
     *
     * @param login email informado no request.
     * @throws ValidationException quando o email nao atende ao formato esperado.
     */
    private void validateEmail(String login) {
        if (!emailAddressValidator.isValid(login)) {
            throw new ValidationException("Login invalido.", "login", "must be a valid email");
        }
    }

    /**
     * Garante que ainda nao exista usuario com o login informado.
     *
     * @param login email a ser consultado na base.
     * @throws ConflictException quando o login ja estiver em uso.
     */
    private void ensureLoginIsAvailable(String login) {
        if (userRepository.existsByLogin(login)) {
            throw new ConflictException("Login ja cadastrado no sistema.", "login", "already exists");
        }
    }
}
