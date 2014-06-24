package demo;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import org.omg.CORBA.COMM_FAILURE;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.TRANSIENT;

import tecgraf.openbus.assistant.Assistant;
import tecgraf.openbus.assistant.AuthArgs;
import tecgraf.openbus.core.v2_0.services.ServiceFailure;
import tecgraf.openbus.core.v2_0.services.access_control.InvalidRemoteCode;
import tecgraf.openbus.core.v2_0.services.access_control.LoginProcess;
import tecgraf.openbus.core.v2_0.services.access_control.LoginProcessHelper;
import tecgraf.openbus.core.v2_0.services.access_control.NoLoginCode;
import tecgraf.openbus.core.v2_0.services.access_control.UnknownBusCode;
import tecgraf.openbus.core.v2_0.services.access_control.UnverifiedLoginCode;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceProperty;
import tecgraf.openbus.demo.util.Utils;

public class SharedAuthClient {
  public static void main(String[] args) {
    String help =
      "Usage: 'demo' <host> <port> [file] \n"
        + "  - host = é o host do barramento\n"
        + "  - port = é a porta do barramento\n"
        + "  - file = arquivo com informações do compartilhamento de autenticação (opcional)";
    // verificando parametros de entrada
    if (args.length < 2) {
      System.out.println(String.format(help, "", ""));
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
      System.out.println(Utils.port);
      System.exit(1);
      return;
    }
    // - arquivo
    final String file;
    if (args.length > 2) {
      file = args[2];
    }
    else {
      file = "sharedauth.dat";
    }

    // criando o assistente.
    Assistant assist = new Assistant(host, port) {

      @Override
      public AuthArgs onLoginAuthentication() {
        // recuperando informações de compartilhamento de autenticação
        /*
         * OBS: talvez seja mais interessante para a aplicação trocar esses
         * dados de outra forma. No mínimo, essas informações deveriam estar
         * encriptadas. Além disso, o cliente escreve apenas uma vez esses
         * dados, que têm validade igual ao lease do login dele, portanto uma
         * outra forma mais dinâmica seria mais eficaz. No entanto, isso foge ao
         * escopo dessa demo.
         */
        try {
          FileReader freader = new FileReader(file);
          BufferedReader breader = new BufferedReader(freader);
          try {
            LoginProcess process =
              LoginProcessHelper.narrow(orb().string_to_object(
                breader.readLine()));
            byte[] secret = breader.readLine().getBytes();
            breader.close();
            // repassa os argumentos de login
            return new AuthArgs(process, secret);
          }
          finally {
            breader.close();
          }
        }
        catch (IOException e) {
          System.err.println("Erro ao ler recuperar dados de arquivo.");
          e.printStackTrace();
        }
        return null;
      }
    };

    // busca por serviço
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
        "o barramento em %s:%s esta inacessível no momento", host, port));
      System.exit(1);
      return;
    }
    catch (COMM_FAILURE e) {
      System.err
        .println("falha de comunicação ao acessar serviços núcleo do barramento");
      System.exit(1);
      return;
    }
    catch (NO_PERMISSION e) {
      if (e.minor == NoLoginCode.value) {
        System.err.println("não há um login válido no momento");
      }
      System.exit(1);
      return;
    }
    // erros inesperados
    catch (Throwable e) {
      System.err.println("Erro inesperado durante busca de serviços.");
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
            .println("o serviço encontrado não provê a faceta ofertada");
          continue;
        }

        Hello hello = HelloHelper.narrow(helloObj);
        hello.sayHello();
      }
      catch (TRANSIENT e) {
        System.err.println("o serviço encontrado encontra-se indisponível");
      }
      catch (COMM_FAILURE e) {
        System.err.println("falha de comunicação com o serviço encontrado");
      }
      catch (NO_PERMISSION e) {
        switch (e.minor) {
          case NoLoginCode.value:
            System.err.println("não há um login válido no momento");
            break;
          case UnknownBusCode.value:
            System.err
              .println("o serviço encontrado não está mais logado ao barramento");
            break;
          case UnverifiedLoginCode.value:
            System.err
              .println("o serviço encontrado não foi capaz de validar a chamada");
            break;
          case InvalidRemoteCode.value:
            System.err
              .println("integração do serviço encontrado com o barramento está incorreta");
            break;
        }
      }
    }

    // Finaliza o assistente
    assist.shutdown();
  }

}
