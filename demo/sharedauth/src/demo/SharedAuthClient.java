package demo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.omg.CORBA.COMM_FAILURE;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.TRANSIENT;
import org.omg.CORBA.ORBPackage.InvalidName;

import scs.core.IComponent;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.assistant.Assistant;
import tecgraf.openbus.assistant.AssistantParams;
import tecgraf.openbus.assistant.AuthArgs;
import tecgraf.openbus.assistant.OnFailureCallback;
import tecgraf.openbus.core.v2_1.services.ServiceFailure;
import tecgraf.openbus.core.v2_1.services.access_control.InvalidRemoteCode;
import tecgraf.openbus.core.v2_1.services.access_control.NoLoginCode;
import tecgraf.openbus.core.v2_1.services.access_control.UnknownBusCode;
import tecgraf.openbus.core.v2_1.services.access_control.UnverifiedLoginCode;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceProperty;
import tecgraf.openbus.demo.util.Usage;
import tecgraf.openbus.exception.InvalidEncodedStream;

/**
 * Demo de compartilhamento de autentica��o
 *
 * @author Tecgraf/PUC-Rio
 */
public class SharedAuthClient {
  /**
   * Fun��o main
   * 
   * @param args
   */
  public static void main(String[] args) {
    String help =
      "Usage: 'demo' <host> <port> [file] \n"
        + "  - host = � o host do barramento\n"
        + "  - port = � a porta do barramento\n"
        + "  - file = arquivo com informa��es do compartilhamento de autentica��o (opcional)";
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
      System.out.println(Usage.port);
      System.exit(1);
      return;
    }
    // - arquivo
    final String path;
    if (args.length > 2) {
      path = args[2];
    }
    else {
      path = "sharedauth.dat";
    }

    AssistantParams params = new AssistantParams(host, port);
    params.callback = new OnFailureCallback() {

      @Override
      public void onRegisterFailure(Assistant assistant, IComponent component,
        ServiceProperty[] properties, Exception except) {
        // do nothing
      }

      @Override
      public void onLoginFailure(Assistant assistant, Exception except) {
        System.err.println("Erro ao tentar realizar login");
        except.printStackTrace();
        assistant.shutdown();
      }

      @Override
      public void onFindFailure(Assistant assistant, Exception except) {
        // do nothing
      }

      @Override
      public void onStartSharedAuthFailure(Assistant assistant, Exception except) {
        // do nothing
      }
    };
    // criando o assistente.
    Assistant assist = new Assistant(params) {

      @Override
      public AuthArgs onLoginAuthentication() {
        // recuperando informa��es de compartilhamento de autentica��o
        /*
         * OBS: talvez seja mais interessante para a aplica��o trocar esses
         * dados de outra forma. No m�nimo, essas informa��es deveriam estar
         * encriptadas. Al�m disso, o cliente escreve apenas uma vez esses
         * dados, que t�m validade igual ao lease do login dele, portanto uma
         * outra forma mais din�mica seria mais eficaz. No entanto, isso foge ao
         * escopo dessa demo.
         */
        try {
          // recuperando o gerente de contexto de chamadas � barramentos 
          final OpenBusContext context =
            (OpenBusContext) this.orb().resolve_initial_references(
              "OpenBusContext");
          byte[] data;
          File file = new File(path);
          FileInputStream is = new FileInputStream(file);
          try {
            int length = (int) file.length();
            data = new byte[length];
            int offset = is.read(data);
            while (offset < length) {
              int read = is.read(data, offset, length - offset);
              if (read < 0) {
                System.err.println("N�o foi poss�vel ler todo o arquivo");
                System.exit(1);
              }
              offset += read;
            }
            return new AuthArgs(context.decodeSharedAuth(data));
          }
          finally {
            is.close();
          }
        }
        catch (IOException e) {
          System.err.println("Erro ao recuperar dados de arquivo.");
          e.printStackTrace();
        }
        catch (InvalidName e) {
          System.err.println("Erro ao recuperar contexto.");
          e.printStackTrace();
        }
        catch (InvalidEncodedStream e) {
          System.err.println("Dado n�o corresponde a um segredo v�lido");
          e.printStackTrace();
        }
        this.shutdown();
        return null;
      }
    };

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
        System.err.println("n�o h� um login v�lido no momento");
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
            System.err.println("n�o h� um login v�lido no momento");
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
