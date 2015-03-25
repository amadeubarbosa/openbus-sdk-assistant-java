package tecgraf.openbus.assistant;

import java.util.Properties;

import org.omg.CORBA.ORB;
import org.omg.CORBA.Object;

/**
 * Representa um conjunto de par�metros que s�o utilizados para definir
 * par�metros de configura��o na constru��o do Assistente.
 * <p>
 * Os par�metros opicionais s�o descritos abaixo:
 * <ul>
 * <li>interval: Tempo em segundos indicando o tempo m�nimo de espera antes de
 * cada nova tentativa ap�s uma falha na execu��o de uma tarefa. Por exemplo,
 * depois de uma falha na tentativa de um login ou registro de oferta, o
 * assistente espera pelo menos o tempo indicado por esse par�metro antes de
 * tentar uma nova tentativa.
 * <li>orb: O ORB a ser utilizado pelo assistente para realizar suas tarefas. O
 * assistente tamb�m configura esse ORB de forma que todas as chamadas feitas
 * por ele sejam feitas com a identidade do login estabelecido pelo assistente.
 * Esse ORB deve ser iniciado de acordo com os requisitos do projeto OpenBus,
 * como feito pela opera��o 'ORBInitializer::initORB()'.
 * <li>connprops: Propriedades da conex�o a ser criada com o barramento
 * espeficiado. Para maiores informa��es sobre essas propriedades, veja a
 * opera��o 'OpenBusContext::createConnection()'.
 * <li>callback: Objeto de callback que recebe notifica��es de falhas das
 * tarefas realizadas pelo assistente.
 * </ul>
 * 
 * @author Tecgraf
 */
public class AssistantParams {

  /** Host com o qual o assistente quer se conectar */
  protected String host;
  /** Porta com a qual o assistente quer se conectar */
  protected int port;
  /**
   * Refer�ncia para componentes SCS que representa os servi�os n�cleo do
   * barramento
   */
  protected Object reference;
  /**
   * Tempo em segundos indicando o tempo m�nimo de espera antes de cada nova
   * tentativa ap�s uma falha na execu��o de uma tarefa. Por exemplo, depois de
   * uma falha na tentativa de um login ou registro de oferta, o assistente
   * espera pelo menos o tempo indicado por esse par�metro antes de tentar uma
   * nova tentativa. N�o pode ser menor do que 1 segundo.
   */
  public Float interval;
  /**
   * O ORB a ser utilizado pelo assistente para realizar suas tarefas. O
   * assistente tamb�m configura esse ORB de forma que todas as chamadas feitas
   * por ele sejam feitas com a identidade do login estabelecido pelo
   * assistente. Esse ORB deve ser iniciado de acordo com os requisitos do
   * projeto OpenBus, como feito pela opera��o 'ORBInitializer::initORB()'.
   */
  public ORB orb;
  /**
   * Propriedades da conex�o a ser criada com o barramento espeficiado. Para
   * maiores informa��es sobre essas propriedades, veja a opera��o
   * 'OpenBusContext::createConnection()'.
   */
  public Properties connprops;
  /**
   * Objeto de callback que recebe notifica��es de falhas das tarefas realizadas
   * pelo assistente.
   */
  public OnFailureCallback callback;

  /**
   * Par�metros de configura��o de assistente que realizar� a conex�o a um
   * barramento utilizando host e porta.
   * 
   * @param host Endere�o ou nome de rede onde os servi�os n�cleo do barramento
   *        est�o executando.
   * @param port Porta onde os servi�os n�cleo do barramento est�o executando.
   */
  public AssistantParams(String host, int port) {
    this.host = host;
    this.port = port;
  }

  /**
   * Par�metros de configura��o de assistente que realizar� a conex�o a um
   * barramento utilizando uma refer�ncia CORBA a um componente SCS que
   * representa os servi�os n�cleo do barramento.
   * 
   * @param reference Refer�ncia CORBA a um componente SCS que representa os
   *        servi�os n�cleo do barramento.
   */
  public AssistantParams(Object reference) {
    this.reference = reference;
  }
}
