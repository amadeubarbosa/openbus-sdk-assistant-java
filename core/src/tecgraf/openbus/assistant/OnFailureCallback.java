package tecgraf.openbus.assistant;

import scs.core.IComponent;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceProperty;

/**
 * Callback de notificação de falhas capturadas pelo assistente.
 * <p>
 * Interface a ser implementada pelo objeto de callback a ser chamado quando o
 * assistente captura um erro durante suas tarefas, tais como login, registro ou
 * busca de ofertas de serviço.
 * 
 * @author Tecgraf
 */
public interface OnFailureCallback {

  /**
   * Callback de notificação de falhas durante processo de login.
   * 
   * @param assistant assistant Assitente que chama a callback.
   * @param except Objeto que descreve a falha ocorrida.
   */
  void onLoginFailure(Assistant assistant, Exception except);

  /**
   * Callback de notificação de falhas durante processo de login.
   * 
   * @param assistant assistant Assitente que chama a callback.
   * @param component Componente sendo registrado.
   * @param properties Lista de propriedades com que o serviço deveria ter sido
   *        registrado.
   * @param except Objeto que descreve a falha ocorrida.
   */
  void onRegisterFailure(Assistant assistant, IComponent component,
    ServiceProperty[] properties, Exception except);

  /**
   * Callback de notificação de falhas durante processo de login.
   * 
   * @param assistant assistant Assitente que chama a callback.
   * @param except Objeto que descreve a falha ocorrida.
   */
  void onFindFailure(Assistant assistant, Exception except);

  /**
   * Callback de notificação de falhas durante a inicialização de autenticação
   * compartilhada.
   * 
   * @param assistant Assistente que chama a callback.
   * @param except Objeto que descreve a falha ocorrida.
   */
  void onStartSharedAuthFailure(Assistant assistant, Exception except);

}
