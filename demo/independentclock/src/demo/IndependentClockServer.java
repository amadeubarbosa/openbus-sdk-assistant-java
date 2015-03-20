package demo;

import java.security.interfaces.RSAPrivateKey;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.omg.CORBA.ORB;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import org.omg.PortableServer.POAManagerPackage.AdapterInactive;

import scs.core.ComponentContext;
import scs.core.ComponentId;
import scs.core.exception.SCSException;
import tecgraf.openbus.assistant.Assistant;
import tecgraf.openbus.assistant.AssistantParams;
import tecgraf.openbus.core.v2_1.services.ServiceFailure;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceProperty;
import tecgraf.openbus.demo.util.Usage;
import tecgraf.openbus.exception.AlreadyLoggedIn;
import tecgraf.openbus.security.Cryptography;

/**
 * Parte servidora do demo Hello
 * 
 * @author Tecgraf
 */
public final class IndependentClockServer {

  private static String host;
  private static int port;
  private static String entity;
  private static RSAPrivateKey privateKey;
  private static float interval = 1.0f;

  /**
   * Função principal.
   * 
   * @param args argumentos.
   * @throws InvalidName
   * @throws AdapterInactive
   * @throws SCSException
   * @throws AlreadyLoggedIn
   * @throws ServiceFailure
   */
  public static void main(String[] args) throws InvalidName, AdapterInactive,
    SCSException, AlreadyLoggedIn, ServiceFailure {
    // verificando parametros de entrada
    if (args.length < 4) {
      String params = "[interval]";
      String desc =
        "\n  - [interval] = Tempo de espera entre tentativas de acesso ao barramento."
          + " Valor padrão é '1'";
      System.out.println(String.format(Usage.serverUsage, params, desc));
      System.exit(1);
      return;
    }
    // - host
    host = args[0];
    // - porta
    try {
      port = Integer.parseInt(args[1]);
    }
    catch (NumberFormatException e) {
      System.out.println(Usage.port);
      System.exit(1);
      return;
    }
    // - entidade
    entity = args[2];
    // - chave privada
    String privateKeyFile = args[3];
    try {
      privateKey = Cryptography.getInstance().readKeyFromFile(privateKeyFile);
    }
    catch (Exception e) {
      System.out.println(Usage.keypath);
      e.printStackTrace();
      System.exit(1);
      return;
    }
    if (args.length > 4) {
      try {
        interval = Float.parseFloat(args[4]);
      }
      catch (NumberFormatException e) {
        System.out.println("Valor de [interval] deve ser um número");
        System.exit(1);
        return;
      }
    }

    // recuperando o assistente
    AssistantParams params = new AssistantParams();
    params.interval = interval;
    final Assistant assist =
      Assistant.createWithPrivateKey(host, port, entity, privateKey);
    final ORB orb = assist.orb();
    // - disparando a thread para que o ORB atenda requisições
    Thread run = new Thread() {
      @Override
      public void run() {
        orb.run();
      }
    };
    run.start();
    // - criando thread para parar e destruir o ORB ao fim da execução do processo 
    Thread shutdown = new Thread() {
      @Override
      public void run() {
        assist.shutdown();
        orb.shutdown(true);
        orb.destroy();
      }
    };
    Runtime.getRuntime().addShutdownHook(shutdown);

    // criando o serviço a ser ofertado
    // - ativando o POA
    POA poa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
    poa.the_POAManager().activate();
    // - construindo os componentes
    ComponentId id =
      new ComponentId("Clock", (byte) 1, (byte) 0, (byte) 0, "java");
    final ComponentContext component = new ComponentContext(orb, poa, id);
    final ClockImpl clockImpl = new ClockImpl();
    component.addFacet("Clock", ClockHelper.id(), clockImpl);

    // thread do serviço de clock independente
    new Thread() {
      @Override
      public void run() {
        while (true) {
          Date date = new Date(clockImpl.getTimeInTicks());
          DateFormat formatter = new SimpleDateFormat("HH:mm:ss:SSS");
          System.out.println(formatter.format(date));
          try {
            Thread.sleep(1000);
          }
          catch (InterruptedException e) {
            // do nothing
          }
        }
      }
    }.start();

    // registrando serviço no barramento
    ServiceProperty[] serviceProperties = new ServiceProperty[1];
    serviceProperties[0] =
      new ServiceProperty("offer.domain", "Demo Independent Clock");
    assist.registerService(component.getIComponent(), serviceProperties);
  }

}
