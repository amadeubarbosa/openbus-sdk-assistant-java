package demo;

import org.omg.CORBA.COMM_FAILURE;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.Object;
import org.omg.CORBA.TRANSIENT;

import tecgraf.openbus.CallerChain;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.core.v2_1.services.access_control.InvalidRemoteCode;
import tecgraf.openbus.core.v2_1.services.access_control.NoLoginCode;
import tecgraf.openbus.core.v2_1.services.access_control.UnknownBusCode;
import tecgraf.openbus.core.v2_1.services.access_control.UnverifiedLoginCode;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.demo.util.Utils;

public class ProxyMessengerImpl extends MessengerPOA {

  private OpenBusContext context;
  private String entity;
  private ServiceOfferDesc[] offers;

  public ProxyMessengerImpl(OpenBusContext context, String entity) {
    this.context = context;
    this.entity = entity;
  }

  @Override
  public void showMessage(String message) throws Unavailable, Unauthorized {
    CallerChain chain = context.getCallerChain();
    System.out.println(String.format("repassando mensagem de %s", Utils
      .chain2str(chain)));
    context.joinChain(chain);
    for (ServiceOfferDesc offer : this.offers) {
      boolean failed = true;
      try {
        Object facet = offer.service_ref.getFacet(MessengerHelper.id());
        Messenger messenger = MessengerHelper.narrow(facet);
        messenger.showMessage(message);
        failed = false;
      }
      // Service
      catch (TRANSIENT e) {
        System.err.println("o serviço encontrado encontra-se indisponível");
      }
      catch (COMM_FAILURE e) {
        System.err.println("falha de comunicação com o serviço encontrado");
      }
      catch (NO_PERMISSION e) {
        switch (e.minor) {
          case NoLoginCode.value:
            System.err.println(String.format(
              "não há um login de '%s' válido no momento", entity));
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
      finally {
        if (!failed) {
          return;
        }
      }
    }
    System.err.println("serviços encontrados não estão disponíveis");
    throw new Unavailable();
  }

  public void setOffers(ServiceOfferDesc[] offers) {
    this.offers = offers;
  }
}
