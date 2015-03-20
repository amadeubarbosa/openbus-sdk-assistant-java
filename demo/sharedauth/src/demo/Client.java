package demo;

import java.io.FileOutputStream;
import java.io.IOException;

import org.omg.CORBA.COMM_FAILURE;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.TRANSIENT;
import org.omg.CORBA.ORBPackage.InvalidName;

import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.SharedAuthSecret;
import tecgraf.openbus.assistant.Assistant;
import tecgraf.openbus.core.v2_1.services.ServiceFailure;
import tecgraf.openbus.core.v2_1.services.access_control.InvalidRemoteCode;
import tecgraf.openbus.core.v2_1.services.access_control.NoLoginCode;
import tecgraf.openbus.core.v2_1.services.access_control.UnknownBusCode;
import tecgraf.openbus.core.v2_1.services.access_control.UnverifiedLoginCode;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceProperty;
import tecgraf.openbus.demo.util.Usage;

/**
 * Demo Single Sign On.
 * 
 * @author Tecgraf
 */
public final class Client {

  /**
   * Fun��o main.
   * 
   * @param args argumentos.
   * @throws InvalidName
   * @throws IOException
   */
  public static void main(String[] args) throws InvalidName, IOException {
    // verificando parametros de entrada
    if (args.length < 3) {
      String params = "[file]";
      String desc =
        "\n  - [file] = arquivo a ser gerado com informa��es do compartilhamento de autentica��o (opcional)";
      System.out.println(String.format(Usage.clientUsage, params, desc));
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
    // - senha (opcional)
    String password = entity;
    if (args.length > 3) {
      password = args[3];
    }
    // - dominio (opcional)
    String domain = "openbus";
    if (args.length > 4) {
      domain = args[4];
    }
    // - arquivo (opcional)
    String file = "sharedauth.dat";
    if (args.length > 5) {
      file = args[5];
    }

    // recuperando o assistente
    final Assistant assist =
      Assistant.createWithPassword(host, port, entity, password.getBytes(),
        domain);

    // iniciando compartilhamento de autentica��o
    SharedAuthSecret secret;
    try {
      secret = assist.startSharedAuth(-1);
    }
    // bus core
    catch (ServiceFailure e) {
      System.err.println(String.format(
        "falha severa no barramento em %s:%s : %s", host, port, e.message));
      System.exit(1);
      return;
    }
    catch (TRANSIENT e) {
      System.err.println(String.format(
        "o barramento em %s:%s esta inacess�vel no momento", host, port));
      System.exit(1);
      return;
    }
    catch (COMM_FAILURE e) {
      System.err
        .println("falha de comunica��o ao acessar servi�os n�cleo do barramento");
      System.exit(1);
      return;
    }
    catch (NO_PERMISSION e) {
      if (e.minor == NoLoginCode.value) {
        System.err.println(String.format(
          "n�o h� um login de '%s' v�lido no momento", entity));
      }
      System.exit(1);
      return;
    }
    // erros inesperados
    catch (Throwable e) {
      System.err
        .println("Erro inesperado durante compartilhamento de autentica��o.");
      e.printStackTrace();
      System.exit(1);
      return;
    }

    // recuperando o gerente de contexto de chamadas � barramentos 
    final OpenBusContext context =
      (OpenBusContext) assist.orb()
        .resolve_initial_references("OpenBusContext");
    // persistindo dados de compartilhamento de autentica��o em arquivo
    /*
     * OBS: talvez seja mais interessante para a aplica��o trocar esses dados de
     * outra forma. No m�nimo, essas informa��es deveriam ser encriptadas. Al�m
     * disso, escreveremos apenas uma vez esses dados, que t�m validade igual ao
     * lease do login atual. Caso o cliente demore a executar, esses dados n�o
     * funcionar�o, portanto uma outra forma mais din�mica seria mais eficaz. No
     * entanto, isso foge ao escopo dessa demo.
     */
    byte[] data = context.encodeSharedAuth(secret);
    FileOutputStream out = null;
    try {
      out = new FileOutputStream(file);
      out.write(data);
    }
    finally {
      if (out != null) {
        out.close();
      }
    }

    // busca por servi�o
    ServiceProperty[] properties = new ServiceProperty[1];
    properties[0] = new ServiceProperty("offer.domain", "Demo Hello");
    ServiceOfferDesc[] services;
    try {
      services = assist.findServices(properties, -1);
    }
    // bus core
    catch (ServiceFailure e) {
      System.err.println(String.format(
        "falha severa no barramento em %s:%s : %s", host, port, e.message));
      System.exit(1);
      return;
    }
    catch (TRANSIENT e) {
      System.err.println(String.format(
        "o barramento em %s:%s esta inacess�vel no momento", host, port));
      System.exit(1);
      return;
    }
    catch (COMM_FAILURE e) {
      System.err
        .println("falha de comunica��o ao acessar servi�os n�cleo do barramento");
      System.exit(1);
      return;
    }
    catch (NO_PERMISSION e) {
      if (e.minor == NoLoginCode.value) {
        System.err.println(String.format(
          "n�o h� um login de '%s' v�lido no momento", entity));
      }
      System.exit(1);
      return;
    }
    // erros inesperados
    catch (Throwable e) {
      System.err.println("Erro inesperado durante busca de servi�os.");
      e.printStackTrace();
      System.exit(1);
      return;
    }

    // analisa as ofertas encontradas
    for (ServiceOfferDesc offerDesc : services) {
      try {
        org.omg.CORBA.Object helloObj =
          offerDesc.service_ref.getFacet(HelloHelper.id());
        if (helloObj == null) {
          System.out
            .println("o servi�o encontrado n�o prov� a faceta ofertada");
          continue;
        }

        Hello hello = HelloHelper.narrow(helloObj);
        hello.sayHello();
      }
      catch (TRANSIENT e) {
        System.err.println("o servi�o encontrado encontra-se indispon�vel");
      }
      catch (COMM_FAILURE e) {
        System.err.println("falha de comunica��o com o servi�o encontrado");
      }
      catch (NO_PERMISSION e) {
        switch (e.minor) {
          case NoLoginCode.value:
            System.err.println(String.format(
              "n�o h� um login de '%s' v�lido no momento", entity));
            break;
          case UnknownBusCode.value:
            System.err
              .println("o servi�o encontrado n�o est� mais logado ao barramento");
            break;
          case UnverifiedLoginCode.value:
            System.err
              .println("o servi�o encontrado n�o foi capaz de validar a chamada");
            break;
          case InvalidRemoteCode.value:
            System.err
              .println("integra��o do servi�o encontrado com o barramento est� incorreta");
            break;
        }
      }
    }

    // Finaliza o assistente
    assist.shutdown();
  }
}
