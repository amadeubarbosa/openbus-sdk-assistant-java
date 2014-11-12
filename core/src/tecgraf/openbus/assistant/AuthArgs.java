package tecgraf.openbus.assistant;

import java.security.interfaces.RSAPrivateKey;

import tecgraf.openbus.SharedAuthSecret;

/**
 * Informa��es de autentica��o de entidades.
 * 
 * @author Tecgraf
 */
public class AuthArgs {

  /**
   * Enumera��o dos tipos de autentica��o suportados pelo assistente.
   * 
   * @author Tecgraf
   */
  enum AuthMode {
    /** Autentica��o por senha */
    AuthByPassword,
    /** Autentica��o por certificado */
    AuthByCertificate,
    /** Autentica��o compartilhada */
    AuthBySharing;
  }

  /** Modo de autentica��o */
  AuthMode mode;
  /** Entidade */
  String entity;
  /** Senha */
  byte[] password;
  /** Chave privada */
  RSAPrivateKey privkey;
  /** Segredo do compartilhamento de login */
  SharedAuthSecret secret;

  /**
   * Construtor para realizar autentica��o por senha
   * 
   * @param entity Identificador da entidade a ser autenticada.
   * @param password Senha de autentica��o no barramento da entidade.
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
   * Construtor para realizar autentica��o por senha
   * 
   * @param entity Identificador da entidade a ser autenticada.
   * @param privkey Chave privada correspondente ao certificado registrado a ser
   *        utilizada na autentica��o.
   */
  public AuthArgs(String entity, RSAPrivateKey privkey) {
    if (entity == null || privkey == null) {
      throw new IllegalArgumentException(
        "Entidade e chave privada devem ser diferentes de nulo.");
    }
    this.entity = entity;
    this.privkey = privkey;
    this.mode = AuthMode.AuthByCertificate;
  }

  /**
   * Construtor para realizar autentica��o compartilhada
   * 
   * @param secret Segredo para compartilhamento de autentica��o.
   */
  public AuthArgs(SharedAuthSecret secret) {
    if (secret == null) {
      throw new IllegalArgumentException("Segredo deve ser diferente de nulo.");
    }
    this.secret = secret;
    this.mode = AuthMode.AuthBySharing;
  }
}
