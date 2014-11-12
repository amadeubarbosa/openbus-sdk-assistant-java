package tecgraf.openbus.assistant;

import java.security.interfaces.RSAPrivateKey;

import tecgraf.openbus.SharedAuthSecret;

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
  RSAPrivateKey privkey;
  /** Segredo do compartilhamento de login */
  SharedAuthSecret secret;

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
   * Construtor para realizar autenticação compartilhada
   * 
   * @param secret Segredo para compartilhamento de autenticação.
   */
  public AuthArgs(SharedAuthSecret secret) {
    if (secret == null) {
      throw new IllegalArgumentException("Segredo deve ser diferente de nulo.");
    }
    this.secret = secret;
    this.mode = AuthMode.AuthBySharing;
  }
}
