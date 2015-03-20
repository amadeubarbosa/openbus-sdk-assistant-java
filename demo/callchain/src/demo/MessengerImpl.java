package demo;

import tecgraf.openbus.CallerChain;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.utils.LibUtils;

public class MessengerImpl extends MessengerPOA {

  private OpenBusContext context;
  private String entity;

  public MessengerImpl(OpenBusContext context, String entity) {
    this.context = context;
    this.entity = entity;
  }

  @Override
  public void showMessage(String message) throws Unavailable, Unauthorized {
    CallerChain chain = context.getCallerChain();
    if (this.entity.equals(chain.caller().entity)) {
      System.out.println(String.format("aceitando mensagem de %s: %s", LibUtils
        .chain2str(chain), message));
    }
    else {
      System.out.println(String.format("recusando mensagem de %s", LibUtils
        .chain2str(chain)));
      throw new Unauthorized();
    }
  }
}
