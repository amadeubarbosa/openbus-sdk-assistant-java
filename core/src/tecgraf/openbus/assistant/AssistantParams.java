package tecgraf.openbus.assistant;

import java.util.Properties;

import org.omg.CORBA.ORB;

/**
 * 
 * Representa um conjunto de parâmetros opcionais que podem ser utilizados para
 * definir parâmetros de configuração na construção do Assistente.
 * <p>
 * Os parâmetros opicionais são descritos abaixo:
 * <ul>
 * <li>interval: Tempo em segundos indicando o tempo mínimo de espera antes de
 * cada nova tentativa após uma falha na execução de uma tarefa. Por exemplo,
 * depois de uma falha na tentativa de um login ou registro de oferta, o
 * assistente espera pelo menos o tempo indicado por esse parâmetro antes de
 * tentar uma nova tentativa.
 * <li>orb: O ORB a ser utilizado pelo assistente para realizar suas tarefas. O
 * assistente também configura esse ORB de forma que todas as chamadas feitas
 * por ele sejam feitas com a identidade do login estabelecido pelo assistente.
 * Esse ORB deve ser iniciado de acordo com os requisitos do projeto OpenBus,
 * como feito pela operação 'ORBInitializer::initORB()'.
 * <li>connprops: Propriedades da conexão a ser criada com o barramento
 * espeficiado. Para maiores informações sobre essas propriedades, veja a
 * operação 'OpenBusContext::createConnection()'.
 * <li>callback: Objeto de callback que recebe notificações de falhas das
 * tarefas realizadas pelo assistente.
 * </ul>
 * 
 * @author Tecgraf
 */
public class AssistantParams {
  /**
   * Tempo em segundos indicando o tempo mínimo de espera antes de cada nova
   * tentativa após uma falha na execução de uma tarefa. Por exemplo, depois de
   * uma falha na tentativa de um login ou registro de oferta, o assistente
   * espera pelo menos o tempo indicado por esse parâmetro antes de tentar uma
   * nova tentativa. Não pode ser menor do que 1 segundo.
   */
  public Float interval;
  /**
   * O ORB a ser utilizado pelo assistente para realizar suas tarefas. O
   * assistente também configura esse ORB de forma que todas as chamadas feitas
   * por ele sejam feitas com a identidade do login estabelecido pelo
   * assistente. Esse ORB deve ser iniciado de acordo com os requisitos do
   * projeto OpenBus, como feito pela operação 'ORBInitializer::initORB()'.
   */
  public ORB orb;
  /**
   * Propriedades da conexão a ser criada com o barramento espeficiado. Para
   * maiores informações sobre essas propriedades, veja a operação
   * 'OpenBusContext::createConnection()'.
   */
  public Properties connprops;
  /**
   * Objeto de callback que recebe notificações de falhas das tarefas realizadas
   * pelo assistente.
   */
  public OnFailureCallback callback;
}
