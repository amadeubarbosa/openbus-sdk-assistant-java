package demo;

import tecgraf.openbus.OpenBusContext;

/**
 * Implementação do componente Greetings
 * 
 * @author Tecgraf
 */
public class GreetingsImpl extends GreetingsPOA {

  /**
   * Línguas em que se realiza o cumprimento.
   * 
   * @author Tecgraf
   */
  static public enum Language {
    /** Inglês */
    English,
    /** Espanhol */
    Spanish,
    /** Português */
    Portuguese;
  };

  /**
   * Períodos em que se realiza o cumprimento.
   * 
   * @author Tecgraf
   */
  static public enum Period {
    /** Manhã */
    Morning,
    /** Tarde */
    Afternoon,
    /** Noite */
    Night;
  };

  /** Contexto com o barramento. */
  private OpenBusContext context;
  /** O período */
  private Period period;
  /** A língua */
  private Language language;

  /**
   * Construtor.
   * 
   * @param context Conexão com o barramento.
   * @param language A língua utilizada.
   * @param period O período.
   */
  public GreetingsImpl(OpenBusContext context, Language language, Period period) {
    this.context = context;
    this.language = language;
    this.period = period;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String sayGreetings() {
    String caller = context.getCallerChain().caller().entity;
    switch (language) {
      case English:
        return englishGreetings(caller);
      case Spanish:
        return spanishGreetings(caller);
      case Portuguese:
        return portugueseGreetings(caller);
    }
    return "BUG: lingua não especificada";
  }

  /**
   * Cumprimentações em inglês.
   * 
   * @param caller requisitante da chamada.
   * @return o cumprimento.
   */
  private String englishGreetings(String caller) {
    switch (this.period) {
      case Morning:
        return String.format("Good morning %s", caller);
      case Afternoon:
        return String.format("Good afternoon %s", caller);
      case Night:
        return String.format("Good night %s", caller);
    }
    return "BUG: periodo não especificado";
  }

  /**
   * Cumprimentações em espanhol.
   * 
   * @param caller requisitante da chamada.
   * @return o cumprimento.
   */
  private String spanishGreetings(String caller) {
    switch (this.period) {
      case Morning:
        return String.format("Buenos días %s", caller);
      case Afternoon:
        return String.format("Buenas tardes %s", caller);
      case Night:
        return String.format("Buenas noches %s", caller);
    }
    return "BUG: periodo não especificado";
  }

  /**
   * Cumprimentações em português.
   * 
   * @param caller requisitante da chamada.
   * @return o cumprimento.
   */
  private String portugueseGreetings(String caller) {
    switch (this.period) {
      case Morning:
        return String.format("Bom dia %s", caller);
      case Afternoon:
        return String.format("Boa tarde %s", caller);
      case Night:
        return String.format("Boa noite %s", caller);
    }
    return "BUG: periodo não especificado";
  }
}
