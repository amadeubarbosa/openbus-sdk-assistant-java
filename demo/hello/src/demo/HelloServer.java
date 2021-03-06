package demo;

import java.security.interfaces.RSAPrivateKey;

import org.omg.CORBA.ORB;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import org.omg.PortableServer.POAManagerPackage.AdapterInactive;

import scs.core.ComponentContext;
import scs.core.ComponentId;
import scs.core.exception.SCSException;
import tecgraf.openbus.OpenBusContext;
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
public final class HelloServer {

  /**
   * Fun��o principal.
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
      System.out.println(String.format(Usage.serverUsage, "", ""));
      System.exit(1);
      return;
    }
    // - host
    String host = args[0];
    // - porta
    int port;
    try {
      port = Integer.parseInt(args[1]);
    }
    catch (NumberFormatException e) {
      System.out.println(Usage.port);
      System.exit(1);
      return;
    }
    // - entidade
    String entity = args[2];
    // - chave privada
    String privateKeyFile = args[3];
    RSAPrivateKey privateKey;
    try {
      privateKey = Cryptography.getInstance().readKeyFromFile(privateKeyFile);
    }
    catch (Exception e) {
      System.out.println(Usage.keypath);
      e.printStackTrace();
      System.exit(1);
      return;
    }

    // recuperando o assistente
    final Assistant assist =
      Assistant.createWithPrivateKey(new AssistantParams(host, port), entity,
        privateKey);
    final ORB orb = assist.orb();
    // - disparando a thread para que o ORB atenda requisi��es
    Thread run = new Thread() {
      @Override
      public void run() {
        orb.run();
      }
    };
    run.start();
    // - criando thread para parar e destruir o ORB ao fim da execu��o do processo 
    Thread shutdown = new Thread() {
      @Override
      public void run() {
        assist.shutdown();
        orb.shutdown(true);
        orb.destroy();
      }
    };
    Runtime.getRuntime().addShutdownHook(shutdown);

    // recuperando o gerente de contexto de chamadas � barramentos 
    OpenBusContext context =
      (OpenBusContext) orb.resolve_initial_references("OpenBusContext");
    // criando o servi�o a ser ofertado
    // - ativando o POA
    POA poa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
    poa.the_POAManager().activate();
    // - construindo o componente
    ComponentId id =
      new ComponentId("Hello", (byte) 1, (byte) 0, (byte) 0, "java");
    ComponentContext component = new ComponentContext(orb, poa, id);
    component.addFacet("Hello", HelloHelper.id(), new HelloImpl(context));

    // registrando servi�o no barramento
    ServiceProperty[] serviceProperties =
      new ServiceProperty[] { new ServiceProperty("offer.domain", "Demo Hello") };
    assist.registerService(component.getIComponent(), serviceProperties);
  }
}
