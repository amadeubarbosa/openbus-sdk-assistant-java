package tecgraf.openbus.assistant;

import java.security.interfaces.RSAPrivateKey;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.omg.CORBA.ORB;
import org.omg.CORBA.ORBPackage.InvalidName;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAHelper;
import org.omg.PortableServer.POAManagerPackage.AdapterInactive;

import scs.core.ComponentContext;
import scs.core.ComponentId;
import scs.core.IComponent;
import scs.core.exception.SCSException;
import tecgraf.openbus.Connection;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.SharedAuthSecret;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.core.v2_1.services.access_control.AccessDenied;
import tecgraf.openbus.core.v2_1.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_1.services.access_control.MissingCertificate;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.core.v2_1.services.offer_registry.ServiceProperty;
import tecgraf.openbus.security.Cryptography;
import tecgraf.openbus.utils.Configs;
import tecgraf.openbus.utils.Utils;

public class AssistantTest {

  private static String host;
  private static int port;
  private static String entity;
  private static byte[] password;
  private static String domain;
  private static String system;
  private static RSAPrivateKey systemKey;
  private static String systemWrongName;
  private static RSAPrivateKey systemWrongKey;
  private static AssistantParams paramsHostPort;

  @BeforeClass
  public static void oneTimeSetUp() throws Exception {
    Cryptography crypto = Cryptography.getInstance();
    Configs configs = Configs.readConfigsFile();
    Utils.setLibLogLevel(configs.log);
    host = configs.bushost;
    port = configs.busport;
    paramsHostPort = new AssistantParams(host, port);
    entity = configs.user;
    password = configs.password;
    domain = configs.domain;
    system = configs.system;
    systemKey = crypto.readKeyFromFile(configs.syskey);
    systemWrongName = configs.wrongsystem;
    systemWrongKey = crypto.readKeyFromFile(configs.wrongkey);
  }

  @Test
  public void invalidHostTest() {
    String invHost = "unknown-host";
    AssistantParams params = new AssistantParams(invHost, port);
    ORB orb = ORBInitializer.initORB();
    Assistant assist =
      Assistant.createWithPassword(params, entity, password, domain);
    Assert.assertNotSame(assist.orb(), orb);
    AuthArgs args = assist.onLoginAuthentication();
    Assert.assertEquals(args.entity, entity);
    Assert.assertTrue(Arrays.equals(args.password, password));
    assist.shutdown();
  }

  @Test
  public void invalidHostPortTest() {
    // chutando uma porta inválida
    int invPort = port + 111;
    AssistantParams params = new AssistantParams(host, invPort);
    ORB orb = ORBInitializer.initORB();
    Assistant assist =
      Assistant.createWithPassword(params, entity, password, domain);
    Assert.assertNotSame(assist.orb(), orb);
    AuthArgs args = assist.onLoginAuthentication();
    Assert.assertEquals(args.entity, entity);
    Assert.assertTrue(Arrays.equals(args.password, password));
    assist.shutdown();
  }

  @Test
  public void nullArgsToCreateWithPasswordTest() {
    boolean failed = false;
    try {
      Assistant.createWithPassword(paramsHostPort, null, password, domain);
    }
    catch (IllegalArgumentException e) {
      failed = true;
    }
    Assert.assertTrue(failed);
    failed = false;
    try {
      Assistant.createWithPassword(paramsHostPort, entity, null, domain);
    }
    catch (IllegalArgumentException e) {
      failed = true;
    }
    Assert.assertTrue(failed);
    failed = false;
    try {
      Assistant.createWithPassword(paramsHostPort, entity, password, null);
    }
    catch (IllegalArgumentException e) {
      failed = true;
    }
    Assert.assertTrue(failed);
  }

  @Test
  public void nullArgsToCreateWithPrivateKeyTest() {
    boolean failed = false;
    try {
      Assistant.createWithPrivateKey(paramsHostPort, null, systemKey);
    }
    catch (IllegalArgumentException e) {
      failed = true;
    }
    Assert.assertTrue(failed);
    failed = false;
    try {
      Assistant.createWithPrivateKey(paramsHostPort, entity, null);
    }
    catch (IllegalArgumentException e) {
      failed = true;
    }
    Assert.assertTrue(failed);
  }

  @Test
  public void nullArgsToCreateBySharedAuthTest() throws Throwable {
    final AtomicBoolean failed = new AtomicBoolean(false);
    final AtomicBoolean asExpected = new AtomicBoolean(false);
    AssistantParams params = new AssistantParams(host, port);
    params.interval = 1.0f;
    params.callback = new OnFailureCallback() {

      @Override
      public void onRegisterFailure(Assistant assistant, IComponent component,
        ServiceProperty[] properties, Exception except) {
        // do nothing
      }

      @Override
      public void onLoginFailure(Assistant assistant, Exception except) {
        failed.set(true);
        if (except instanceof IllegalArgumentException) {
          asExpected.set(true);
        }
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

    new Assistant(params) {

      @Override
      public AuthArgs onLoginAuthentication() {
        SharedAuthSecret secret = null;
        return new AuthArgs(secret);
      }
    };
    try {
      Thread.sleep((int) (params.interval * 3 * 1000));
    }
    catch (InterruptedException e) {
      Assert.fail(e.getMessage());
    }
    Assert.assertTrue(failed.get());
    Assert.assertTrue(asExpected.get());
  }

  @Test
  public void createTest() {
    ORB orb = ORBInitializer.initORB();
    Assistant assist =
      Assistant.createWithPassword(paramsHostPort, entity, password, domain);
    Assert.assertNotSame(assist.orb(), orb);
    AuthArgs args = assist.onLoginAuthentication();
    Assert.assertEquals(args.entity, entity);
    Assert.assertTrue(Arrays.equals(args.password, password));
    assist.shutdown();
    assist = Assistant.createWithPrivateKey(paramsHostPort, system, systemKey);
    Assert.assertNotSame(assist.orb(), orb);
    args = assist.onLoginAuthentication();
    Assert.assertEquals(args.entity, system);
    Assert.assertSame(args.privkey, systemKey);
    assist.shutdown();
  }

  @Test
  public void reuseORBTest() {
    ORB orb = ORBInitializer.initORB();
    AssistantParams params = new AssistantParams(host, port);
    params.orb = orb;
    Assistant assist =
      Assistant.createWithPassword(params, entity, password, domain);
    Assert.assertSame(params.orb, orb);
    boolean failed = false;
    try {
      Assistant.createWithPrivateKey(params, entity, systemKey);
    }
    catch (IllegalArgumentException e) {
      failed = true;
    }
    Assert.assertTrue(failed);
    assist.shutdown();
  }

  @Test
  public void reuseORBbyContextTest() throws Exception {
    ORB orb = ORBInitializer.initORB();
    AssistantParams params = new AssistantParams(host, port);
    params.orb = orb;
    OpenBusContext context =
      (OpenBusContext) orb.resolve_initial_references("OpenBusContext");
    Connection conn = context.connectByAddress(host, port);
    context.setDefaultConnection(conn);
    boolean failed = false;
    try {
      Assistant.createWithPassword(params, entity, password, domain);
    }
    catch (IllegalArgumentException e) {
      failed = true;
    }
    Assert.assertTrue(failed);
    context.setDefaultConnection(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void invalidORBTest() throws IllegalArgumentException {
    String[] args = null;
    Properties props = new Properties();
    props.setProperty("org.omg.CORBA.ORBClass", "org.jacorb.orb.ORB");
    props.setProperty("org.omg.CORBA.ORBSingletonClass",
      "org.jacorb.orb.ORBSingleton");
    AssistantParams params = new AssistantParams(host, port);
    params.orb = ORB.init(args, props);
    Assistant.createWithPassword(params, entity, password, domain);
  }

  @Test(expected = IllegalArgumentException.class)
  public void intervalIsNaNTest() throws IllegalArgumentException {
    AssistantParams params = new AssistantParams(host, port);
    params.interval = Float.NaN;
    Assistant assist =
      Assistant.createWithPassword(params, entity, password, domain);
    assist.shutdown();
  }

  @Test(expected = IllegalArgumentException.class)
  public void intervalIsPositiveInfinityTest() throws IllegalArgumentException {
    AssistantParams params = new AssistantParams(host, port);
    params.interval = Float.POSITIVE_INFINITY;
    Assistant assist =
      Assistant.createWithPassword(params, entity, password, domain);
    assist.shutdown();
  }

  @Test(expected = IllegalArgumentException.class)
  public void intervalIsNegativeInfinityTest() throws IllegalArgumentException {
    AssistantParams params = new AssistantParams(host, port);
    params.interval = Float.NEGATIVE_INFINITY;
    Assistant assist =
      Assistant.createWithPassword(params, entity, password, domain);
    assist.shutdown();
  }

  @Test(expected = IllegalArgumentException.class)
  public void intervalIsLowerTest() throws IllegalArgumentException {
    AssistantParams params = new AssistantParams(host, port);
    params.interval = 0.0f;
    Assistant assist =
      Assistant.createWithPassword(params, entity, password, domain);
    assist.shutdown();
  }

  @Test
  public void intervalIsValidTest() {
    boolean failed = false;
    Assistant assist = null;
    AssistantParams params = new AssistantParams(host, port);
    params.interval = 1.0f;
    try {
      assist = Assistant.createWithPassword(params, entity, password, domain);
    }
    catch (IllegalArgumentException e) {
      failed = true;
    }
    Assert.assertFalse(failed);
    if (assist != null) {
      assist.shutdown();
    }
  }

  @Test
  public void registerAndFindTest() throws Throwable {
    AssistantParams params = new AssistantParams(host, port);
    params.interval = 1.0f;
    Assistant assist =
      Assistant.createWithPrivateKey(params, system, systemKey);
    ORB orb = assist.orb();
    int index;
    for (index = 0; index < 5; index++) {
      ComponentContext context = buildComponent(orb);
      ServiceProperty[] props =
        new ServiceProperty[] {
            new ServiceProperty("offer.domain", "Assistant Test"),
            new ServiceProperty("loop.index", Integer.toString(index)) };
      assist.registerService(context.getIComponent(), props);
    }
    Thread.sleep((int) (params.interval * 5 * 1000));
    ServiceProperty[] search =
      new ServiceProperty[] { new ServiceProperty("offer.domain",
        "Assistant Test") };
    ServiceOfferDesc[] found = assist.findServices(search, 3);
    Assert.assertEquals(index, found.length);
    assist.shutdown();
    assist = Assistant.createWithPrivateKey(paramsHostPort, system, systemKey);
    found = assist.findServices(search, 3);
    Assert.assertEquals(0, found.length);
    assist.shutdown();
  }

  @Test
  public void registerAndGetAllTest() throws Throwable {
    AssistantParams params = new AssistantParams(host, port);
    params.interval = 1.0f;
    Assistant assist =
      Assistant.createWithPrivateKey(params, system, systemKey);
    ORB orb = assist.orb();
    int index;
    for (index = 0; index < 5; index++) {
      ComponentContext context = buildComponent(orb);
      ServiceProperty[] props =
        new ServiceProperty[] {
            new ServiceProperty("offer.domain", "Assistant Test"),
            new ServiceProperty("loop.index", Integer.toString(index)) };
      assist.registerService(context.getIComponent(), props);
    }
    Thread.sleep((int) (params.interval * 5 * 1000));
    ServiceOfferDesc[] found = assist.getAllServices(3);
    Assert.assertTrue(found.length >= index);
    assist.shutdown();
  }

  @Test
  public void invalidRegisterTest() throws AdapterInactive, InvalidName,
    SCSException, InterruptedException {
    final AtomicBoolean failed = new AtomicBoolean(false);
    AssistantParams params = new AssistantParams(host, port);
    params.interval = 1.0f;
    params.callback = new OnFailureCallback() {

      @Override
      public void onRegisterFailure(Assistant assistant, IComponent component,
        ServiceProperty[] properties, Exception except) {
        failed.set(true);
      }

      @Override
      public void onLoginFailure(Assistant assistant, Exception except) {
        // do nothing
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
    Assistant assist =
      Assistant.createWithPrivateKey(params, system, systemKey);
    ORB orb = assist.orb();
    ComponentContext context = buildComponent(orb);
    context.removeFacet("IMetaInterface");
    ServiceProperty[] props =
      new ServiceProperty[] { new ServiceProperty("offer.domain",
        "Assistant Test") };
    assist.registerService(context.getIComponent(), props);
    Thread.sleep((int) (params.interval * 3 * 1000));
    Assert.assertTrue(failed.get());
    assist.shutdown();
  }

  @Test
  public void invalidRegisterAndShutdownOnCallbackTest()
    throws AdapterInactive, InvalidName, SCSException, InterruptedException {
    final AtomicBoolean failed = new AtomicBoolean(false);
    boolean newRegisterFailed = false;
    AssistantParams params = new AssistantParams(host, port);
    params.interval = 1.0f;
    params.callback = new OnFailureCallback() {

      @Override
      public void onRegisterFailure(Assistant assistant, IComponent component,
        ServiceProperty[] properties, Exception except) {
        failed.set(true);
        assistant.shutdown();
      }

      @Override
      public void onLoginFailure(Assistant assistant, Exception except) {
        // do nothing
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
    Assistant assist =
      Assistant.createWithPrivateKey(params, system, systemKey);
    ORB orb = assist.orb();
    ComponentContext context = buildComponent(orb);
    context.removeFacet("IMetaInterface");
    ServiceProperty[] props =
      new ServiceProperty[] { new ServiceProperty("offer.domain",
        "Assistant Test") };
    assist.registerService(context.getIComponent(), props);
    Thread.sleep((int) (params.interval * 3 * 1000));
    Assert.assertTrue(failed.get());

    ComponentContext context2 = buildComponent(orb);
    try {
      assist.registerService(context2.getIComponent(), props);
    }
    catch (RejectedExecutionException e) {
      //as expected due to assistant shutdown
      newRegisterFailed = true;
    }
    Assert.assertTrue(newRegisterFailed);
  }

  @Test
  public void loginBySharedAuthTest() throws Throwable {
    final AtomicBoolean failed = new AtomicBoolean(false);
    AssistantParams params = new AssistantParams(host, port);
    params.interval = 1.0f;
    params.callback = new OnFailureCallback() {

      @Override
      public void onRegisterFailure(Assistant assistant, IComponent component,
        ServiceProperty[] properties, Exception except) {
        // do nothing
      }

      @Override
      public void onLoginFailure(Assistant assistant, Exception except) {
        failed.set(true);
      }

      @Override
      public void onFindFailure(Assistant assistant, Exception except) {
        failed.set(true);
      }

      @Override
      public void onStartSharedAuthFailure(Assistant assistant, Exception except) {
        // do nothing
      }
    };
    Assistant assist = new Assistant(params) {

      @Override
      public AuthArgs onLoginAuthentication() {
        try {
          // connect using basic API
          OpenBusContext context =
            (OpenBusContext) orb().resolve_initial_references("OpenBusContext");
          Connection conn = context.connectByAddress(host, port);
          context.setCurrentConnection(conn);
          conn.loginByPassword(entity, password, domain);
          SharedAuthSecret secret = conn.startSharedAuth();
          conn.logout();
          return new AuthArgs(secret);
        }
        catch (Exception e) {
          this.shutdown();
          Assert.fail("Falha durante login.");
        }
        return null;
      }
    };
    assist.getAllServices(1);
    Assert.assertFalse(failed.get());
    assist.shutdown();
  }

  @Test
  public void startSharedAuthTest() throws Throwable {
    final AtomicBoolean failed = new AtomicBoolean(false);
    AssistantParams params = new AssistantParams(host, port);
    params.interval = 1.0f;
    params.callback = new OnFailureCallback() {

      @Override
      public void onRegisterFailure(Assistant assistant, IComponent component,
        ServiceProperty[] properties, Exception except) {
        // do nothing
      }

      @Override
      public void onLoginFailure(Assistant assistant, Exception except) {
        failed.set(true);
      }

      @Override
      public void onFindFailure(Assistant assistant, Exception except) {
        // do nothing
      }

      @Override
      public void onStartSharedAuthFailure(Assistant assistant, Exception except) {
        failed.set(true);
      }
    };
    Assistant assist =
      Assistant.createWithPassword(params, entity, password, domain);
    SharedAuthSecret secret = assist.startSharedAuth(1);
    Assert.assertFalse(failed.get());
    Assert.assertNotNull(secret);

    // connect using basic API
    OpenBusContext context =
      (OpenBusContext) assist.orb()
        .resolve_initial_references("OpenBusContext");
    Connection conn = context.connectByAddress(host, port);
    conn.loginBySharedAuth(secret);
    LoginInfo loginInfo = conn.login();
    Assert.assertEquals(entity, loginInfo.entity);
    conn.logout();
    assist.shutdown();
  }

  @Test
  public void nullLoginArgsTest() throws InterruptedException {
    final AtomicBoolean failed = new AtomicBoolean(false);
    AssistantParams params = new AssistantParams(host, port);
    params.interval = 1.0f;
    params.callback = new OnFailureCallback() {

      @Override
      public void onRegisterFailure(Assistant assistant, IComponent component,
        ServiceProperty[] properties, Exception except) {
        // do nothing
      }

      @Override
      public void onLoginFailure(Assistant assistant, Exception except) {
        failed.set(true);
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
    Assistant assist = new Assistant(params) {

      @Override
      public AuthArgs onLoginAuthentication() {
        return null;
      }
    };
    Thread.sleep((int) (params.interval * 3 * 1000));
    Assert.assertTrue(failed.get());
    assist.shutdown();
  }

  @Test
  public void invalidPasswordTest() throws Throwable {
    final AtomicBoolean failed = new AtomicBoolean(false);
    final AtomicBoolean asExpected = new AtomicBoolean(false);
    AssistantParams params = new AssistantParams(host, port);
    params.interval = 1.0f;
    params.callback = new OnFailureCallback() {

      @Override
      public void onRegisterFailure(Assistant assistant, IComponent component,
        ServiceProperty[] properties, Exception except) {
        // do nothing
      }

      @Override
      public void onLoginFailure(Assistant assistant, Exception except) {
        failed.set(true);
        if (except instanceof AccessDenied) {
          asExpected.set(true);
        }
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

    Assistant.createWithPassword(params, "invalid-1", new byte[] {}, domain);
    try {
      Thread.sleep((int) (params.interval * 3 * 1000));
    }
    catch (InterruptedException e) {
      Assert.fail(e.getMessage());
    }
    Assert.assertTrue(failed.get());
    Assert.assertTrue(asExpected.get());
  }

  @Test
  public void invalidPrivateKeyTest() throws Throwable {
    final AtomicBoolean failed = new AtomicBoolean(false);
    final AtomicBoolean asExpected = new AtomicBoolean(false);
    AssistantParams params = new AssistantParams(host, port);
    params.interval = 1.0f;
    params.callback = new OnFailureCallback() {

      @Override
      public void onRegisterFailure(Assistant assistant, IComponent component,
        ServiceProperty[] properties, Exception except) {
        // do nothing
      }

      @Override
      public void onLoginFailure(Assistant assistant, Exception except) {
        failed.set(true);
        if (except instanceof AccessDenied) {
          asExpected.set(true);
        }
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

    Assistant.createWithPrivateKey(params, system, systemWrongKey);
    try {
      Thread.sleep((int) (params.interval * 3 * 1000));
    }
    catch (InterruptedException e) {
      Assert.fail(e.getMessage());
    }
    Assert.assertTrue(failed.get());
    Assert.assertTrue(asExpected.get());
  }

  @Test
  public void invalidLoginMissingCertificateTest() throws Throwable {
    final AtomicBoolean failed = new AtomicBoolean(false);
    final AtomicBoolean asExpected = new AtomicBoolean(false);
    AssistantParams params = new AssistantParams(host, port);
    params.interval = 1.0f;
    params.callback = new OnFailureCallback() {

      @Override
      public void onRegisterFailure(Assistant assistant, IComponent component,
        ServiceProperty[] properties, Exception except) {
        // do nothing
      }

      @Override
      public void onLoginFailure(Assistant assistant, Exception except) {
        failed.set(true);
        if (except instanceof MissingCertificate) {
          asExpected.set(true);
        }
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

    Assistant.createWithPrivateKey(params, systemWrongName, systemKey);
    try {
      Thread.sleep((int) (params.interval * 3 * 1000));
    }
    catch (InterruptedException e) {
      Assert.fail(e.getMessage());
    }
    Assert.assertTrue(failed.get());
    Assert.assertTrue(asExpected.get());
  }

  @Test
  public void findWithInvalidLoginByPasswordTest() throws Throwable {
    final AtomicBoolean failed = new AtomicBoolean(false);
    final AtomicBoolean findCalled = new AtomicBoolean(false);
    AssistantParams params = new AssistantParams(host, port);
    params.interval = 1.0f;
    params.callback = new OnFailureCallback() {

      @Override
      public void onRegisterFailure(Assistant assistant, IComponent component,
        ServiceProperty[] properties, Exception except) {
        // do nothing
      }

      @Override
      public void onLoginFailure(Assistant assistant, Exception except) {
        failed.set(true);
      }

      @Override
      public void onFindFailure(Assistant assistant, Exception except) {
        // nem chega a ser chamado por conta de não existir um login válido
        findCalled.set(true);
      }

      @Override
      public void onStartSharedAuthFailure(Assistant assistant, Exception except) {
        // do nothing
      }
    };

    Assistant assistant =
      Assistant.createWithPassword(params, "invalid-2", new byte[] {}, domain);
    try {
      Thread.sleep((int) (params.interval * 3 * 1000));
    }
    catch (InterruptedException e) {
      Assert.fail(e.getMessage());
    }
    Assert.assertTrue(failed.get());

    ServiceProperty[] search =
      new ServiceProperty[] { new ServiceProperty("offer.domain",
        "Assistant Test") };
    ServiceOfferDesc[] services = assistant.findServices(search, 3);
    Assert.assertFalse(findCalled.get());
    Assert.assertEquals(0, services.length);
    assistant.shutdown();
  }

  @Test
  public void shutdownOnLoginCallbackThenFindTest() throws Throwable {
    final AtomicBoolean failed = new AtomicBoolean(false);
    final AtomicBoolean asExpected = new AtomicBoolean(false);
    final AtomicBoolean findCalled = new AtomicBoolean(false);
    AssistantParams params = new AssistantParams(host, port);
    params.interval = 1.0f;
    params.callback = new OnFailureCallback() {

      @Override
      public void onRegisterFailure(Assistant assistant, IComponent component,
        ServiceProperty[] properties, Exception except) {
        // do nothing
      }

      @Override
      public void onLoginFailure(Assistant assistant, Exception except) {
        failed.set(true);
        if (except instanceof AccessDenied) {
          asExpected.set(true);
        }
        assistant.shutdown();
      }

      @Override
      public void onFindFailure(Assistant assistant, Exception except) {
        // nem chega a ser chamado por conta de não existir um login válido
        findCalled.set(true);
      }

      @Override
      public void onStartSharedAuthFailure(Assistant assistant, Exception except) {
        // do nothing
      }
    };

    Assistant assistant =
      Assistant.createWithPassword(params, "invalid-3", new byte[] {}, domain);
    try {
      Thread.sleep((int) (params.interval * 3 * 1000));
    }
    catch (InterruptedException e) {
      Assert.fail(e.getMessage());
    }
    Assert.assertTrue(failed.get());
    Assert.assertTrue(asExpected.get());

    ServiceProperty[] search =
      new ServiceProperty[] { new ServiceProperty("offer.domain",
        "Assistant Test") };
    ServiceOfferDesc[] services = assistant.findServices(search, -1);
    Assert.assertEquals(0, services.length);
  }

  @Test
  public void shutdownOnLoginCallbackThenRegisterTest() throws Throwable {
    final AtomicBoolean failed = new AtomicBoolean(false);
    final AtomicBoolean asExpected = new AtomicBoolean(false);
    boolean registerFailed = false;
    AssistantParams params = new AssistantParams(host, port);
    params.interval = 1.0f;
    params.callback = new OnFailureCallback() {

      @Override
      public void onRegisterFailure(Assistant assistant, IComponent component,
        ServiceProperty[] properties, Exception except) {
        // do nothing
      }

      @Override
      public void onLoginFailure(Assistant assistant, Exception except) {
        failed.set(true);
        if (except instanceof AccessDenied) {
          asExpected.set(true);
        }
        else {
          System.err.println("Erro inesperado!");
          except.printStackTrace();
        }
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

    Assistant assistant =
      Assistant.createWithPrivateKey(params, system, systemWrongKey);
    try {
      Thread.sleep((int) (params.interval * 3 * 1000));
    }
    catch (InterruptedException e) {
      Assert.fail(e.getMessage());
    }
    Assert.assertTrue(failed.get());
    Assert.assertTrue(asExpected.get());

    ComponentContext context = buildComponent(assistant.orb());
    ServiceProperty[] props =
      new ServiceProperty[] { new ServiceProperty("offer.domain",
        "Assistant Test") };
    try {
      assistant.registerService(context.getIComponent(), props);
    }
    catch (RejectedExecutionException e) {
      //as expected due to assistant shutdown
      registerFailed = true;
    }
    Assert.assertTrue(registerFailed);
  }

  /**
   * Constrói um componente SCS
   * 
   * @param orb o orb em uso
   * @return um componente
   * @throws SCSException
   * @throws AdapterInactive
   * @throws InvalidName
   */
  private ComponentContext buildComponent(ORB orb) throws SCSException,
    AdapterInactive, InvalidName {
    POA poa = POAHelper.narrow(orb.resolve_initial_references("RootPOA"));
    poa.the_POAManager().activate();
    ComponentId id =
      new ComponentId("TestComponent", (byte) 1, (byte) 0, (byte) 0, "java");
    return new ComponentContext(orb, poa, id);
  }
}
