package tecgraf.openbus.assistant;

import tecgraf.openbus.PrivateKey;
import tecgraf.openbus.core.v2_0.services.access_control.LoginProcess;

/**
 * Informações de autenticação de entidades.
 * 
 * @author Tecgraf
 */
public class AuthArgs {

  /**
   * Enumeração dos tipos de autenticação suportados pelo assistente.
   * 
   * @author Tecgraf
   */
  enum AuthMode {
    /** Autenticação por senha */
    AuthByPassword,
    /** Autenticação por certificado */
    AuthByCertificate,
    /** Autenticação compartilhada */
    AuthBySharing;
  }

  /** Modo de autenticação */
  AuthMode mode;
  /** Entidade */
  String entity;
  /** Senha */
  byte[] password;
  /** Chave privada */
  PrivateKey privkey;
  /** Processo de compartilhamento de login */
  LoginProcess attempt;
  /** Segredo do compartilhamento de login */
  byte[] secret;

  /**
   * Construtor para realizar autenticação por senha
   * 
   * @param entity Identificador da entidade a ser autenticada.
   * @param password Senha de autenticação no barramento da entidade.
   */
  public AuthArgs(String entity, byte[] password) {
    if (entity == null || password == null) {
      throw new IllegalArgumentException(
        "Entidade e senha devem ser diferentes de nulo.");
    }
    this.entity = entity;
    this.password = password;
    this.mode = AuthMode.AuthByPassword;
  }

  /**
   * Construtor para realizar autenticação por senha
   * 
   * @param entity Identificador da entidade a ser autenticada.
   * @param privkey Chave privada correspondente ao certificado registrado a ser
   *        utilizada na autenticação.
   */
  public AuthArgs(String entity, PrivateKey privkey) {
    if (entity == null || privkey == null) {
      throw new IllegalArgumentException(
        "Entidade e chave privada devem ser diferentes de nulo.");
    }
    this.entity = entity;
    this.privkey = privkey;
    this.mode = AuthMode.AuthByCertificate;
  }

  /**
   * Construtor para realizar autenticação compartilhada
   * 
   * @param attempt Objeto que represeta o processo de login iniciado.
   * @param secret Segredo a ser fornecido na conclusão do processo de login.
   */
  public AuthArgs(LoginProcess attempt, byte[] secret) {
    if (attempt == null || secret == null) {
      throw new IllegalArgumentException(
        "Processo de login e segredo devem ser diferentes de nulo.");
    }
    this.attempt = attempt;
    this.secret = secret;
    this.mode = AuthMode.AuthBySharing;
  }
}
