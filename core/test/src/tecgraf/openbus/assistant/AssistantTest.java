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
import org.omg.PortableServer.POAManagerPackage.AdapterInactive;

import scs.core.ComponentContext;
import scs.core.IComponent;
import scs.core.exception.SCSException;
import tecgraf.openbus.Connection;
import tecgraf.openbus.OpenBusContext;
import tecgraf.openbus.SharedAuthSecret;
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.core.v2_0.services.access_control.AccessDenied;
import tecgraf.openbus.core.v2_0.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_0.services.access_control.MissingCertificate;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceProperty;
import tecgraf.openbus.security.Cryptography;
import tecgraf.openbus.util.Utils;

public class AssistantTest {

  private static String host;
  private static int port;
  private static String entity;
  private static byte[] password;
  private static String server;
  private static String privateKeyFile;
  private static RSAPrivateKey privateKey;
  private static String entityWithoutCert;
  private static String wrongKeyFile;
  private static RSAPrivateKey wrongKey;

  @BeforeClass
  public static void oneTimeSetUp() throws Exception {
    Cryptography crypto = Cryptography.getInstance();
    Properties properties = Utils.readPropertyFile("/test.properties");
    host = properties.getProperty("bus.host.name");
    port = Integer.valueOf(properties.getProperty("bus.host.port"));
    entity = properties.getProperty("user.entity.name");
    password = properties.getProperty("user.password").getBytes();
    server = properties.getProperty("system.entity.name");
    privateKeyFile = properties.getProperty("system.private.key");
    privateKey = crypto.readKeyFromFile(privateKeyFile);
    entityWithoutCert = properties.getProperty("system.wrong.name");
    wrongKeyFile = properties.getProperty("system.wrong.key");
    wrongKey = crypto.readKeyFromFile(wrongKeyFile);
  }

  @Test
  public void invalidHostTest() {
    String invHost = "unknown-host";
    ORB orb = ORBInitializer.initORB();
    Assistant assist =
      Assistant.createWithPassword(invHost, port, entity, password);
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
    ORB orb = ORBInitializer.initORB();
    Assistant assist =
      Assistant.createWithPassword(host, invPort, entity, password);
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
      Assistant.createWithPassword(host, port, null, password);
    }
    catch (IllegalArgumentException e) {
      failed = true;
    }
    Assert.assertTrue(failed);
    failed = false;
    try {
      Assistant.createWithPassword(host, port, entity, null);
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
      Assistant.createWithPrivateKey(host, port, null, privateKey);
    }
    catch (IllegalArgumentException e) {
      failed = true;
    }
    Assert.assertTrue(failed);
    failed = false;
    try {
      Assistant.createWithPrivateKey(host, port, entity, null);
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
    AssistantParams params = new AssistantParams();
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

    new Assistant(host, port, params) {

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
      Assistant.createWithPassword(host, port, entity, password);
    Assert.assertNotSame(assist.orb(), orb);
    AuthArgs args = assist.onLoginAuthentication();
    Assert.assertEquals(args.entity, entity);
    Assert.assertTrue(Arrays.equals(args.password, password));
    assist.shutdown();
    assist = Assistant.createWithPrivateKey(host, port, server, privateKey);
    Assert.assertNotSame(assist.orb(), orb);
    args = assist.onLoginAuthentication();
    Assert.assertEquals(args.entity, server);
    Assert.assertSame(args.privkey, privateKey);
    assist.shutdown();
  }

  @Test
  public void reuseORBTest() {
    ORB orb = ORBInitializer.initORB();
    AssistantParams params = new AssistantParams();
    params.orb = orb;
    Assistant assist =
      Assistant.createWithPassword(host, port, entity, password, params);
    Assert.assertSame(params.orb, orb);
    boolean failed = false;
    try {
      Assistant.createWithPrivateKey(host, port, entity, privateKey, params);
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
    AssistantParams params = new AssistantParams();
    params.orb = orb;
    OpenBusContext context =
      (OpenBusContext) orb.resolve_initial_references("OpenBusContext");
    Connection conn = context.createConnection(host, port);
    context.setDefaultConnection(conn);
    boolean failed = false;
    try {
      Assistant.createWithPassword(host, port, entity, password, params);
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
    AssistantParams params = new AssistantParams();
    params.orb = ORB.init(args, props);
    Assistant.createWithPassword(host, port, entity, password, params);
  }

  @Test(expected = IllegalArgumentException.class)
  public void intervalIsNaNTest() throws IllegalArgumentException {
    AssistantParams params = new AssistantParams();
    params.interval = Float.NaN;
    Assistant assist =
      Assistant.createWithPassword(host, port, entity, password, params);
    assist.shutdown();
  }

  @Test(expected = IllegalArgumentException.class)
  public void intervalIsPositiveInfinityTest() throws IllegalArgumentException {
    AssistantParams params = new AssistantParams();
    params.interval = Float.POSITIVE_INFINITY;
    Assistant assist =
      Assistant.createWithPassword(host, port, entity, password, params);
    assist.shutdown();
  }

  @Test(expected = IllegalArgumentException.class)
  public void intervalIsNegativeInfinityTest() throws IllegalArgumentException {
    AssistantParams params = new AssistantParams();
    params.interval = Float.NEGATIVE_INFINITY;
    Assistant assist =
      Assistant.createWithPassword(host, port, entity, password, params);
    assist.shutdown();
  }

  @Test(expected = IllegalArgumentException.class)
  public void intervalIsLowerTest() throws IllegalArgumentException {
    AssistantParams params = new AssistantParams();
    params.interval = 0.0f;
    Assistant assist =
      Assistant.createWithPassword(host, port, entity, password, params);
    assist.shutdown();
  }

  @Test
  public void intervalIsValidTest() {
    boolean failed = false;
    Assistant assist = null;
    AssistantParams params = new AssistantParams();
    params.interval = 1.0f;
    try {
      assist =
        Assistant.createWithPassword(host, port, entity, password, params);
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
    AssistantParams params = new AssistantParams();
    params.interval = 1.0f;
    Assistant assist =
      Assistant.createWithPrivateKey(host, port, server, privateKey, params);
    ORB orb = assist.orb();
    int index;
    for (index = 0; index < 5; index++) {
      ComponentContext context = Utils.buildComponent(orb);
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
    assist = Assistant.createWithPrivateKey(host, port, server, privateKey);
    found = assist.findServices(search, 3);
    Assert.assertEquals(0, found.length);
    assist.shutdown();
  }

  @Test
  public void registerAndGetAllTest() throws Throwable {
    AssistantParams params = new AssistantParams();
    params.interval = 1.0f;
    Assistant assist =
      Assistant.createWithPrivateKey(host, port, server, privateKey, params);
    ORB orb = assist.orb();
    int index;
    for (index = 0; index < 5; index++) {
      ComponentContext context = Utils.buildComponent(orb);
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
    AssistantParams params = new AssistantParams();
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
      Assistant.createWithPrivateKey(host, port, server, privateKey, params);
    ORB orb = assist.orb();
    ComponentContext context = Utils.buildComponent(orb);
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
    AssistantParams params = new AssistantParams();
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
      Assistant.createWithPrivateKey(host, port, server, privateKey, params);
    ORB orb = assist.orb();
    ComponentContext context = Utils.buildComponent(orb);
    context.removeFacet("IMetaInterface");
    ServiceProperty[] props =
      new ServiceProperty[] { new ServiceProperty("offer.domain",
        "Assistant Test") };
    assist.registerService(context.getIComponent(), props);
    Thread.sleep((int) (params.interval * 3 * 1000));
    Assert.assertTrue(failed.get());

    ComponentContext context2 = Utils.buildComponent(orb);
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
    AssistantParams params = new AssistantParams();
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
    Assistant assist = new Assistant(host, port, params) {

      @Override
      public AuthArgs onLoginAuthentication() {
        try {
          // connect using basic API
          OpenBusContext context =
            (OpenBusContext) orb().resolve_initial_references("OpenBusContext");
          Connection conn = context.createConnection(host, port);
          context.setCurrentConnection(conn);
          conn.loginByPassword(entity, password);
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
    AssistantParams params = new AssistantParams();
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
      Assistant.createWithPassword(host, port, entity, password, params);
    SharedAuthSecret secret = assist.startSharedAuth(1);
    Assert.assertFalse(failed.get());
    Assert.assertNotNull(secret);

    // connect using basic API
    OpenBusContext context =
      (OpenBusContext) assist.orb()
        .resolve_initial_references("OpenBusContext");
    Connection conn = context.createConnection(host, port);
    conn.loginBySharedAuth(secret);
    LoginInfo loginInfo = conn.login();
    Assert.assertEquals(entity, loginInfo.entity);
    conn.logout();
    assist.shutdown();
  }

  @Test
  public void nullLoginArgsTest() throws InterruptedException {
    final AtomicBoolean failed = new AtomicBoolean(false);
    AssistantParams params = new AssistantParams();
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
    Assistant assist = new Assistant(host, port, params) {

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
    AssistantParams params = new AssistantParams();
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

    Assistant
      .createWithPassword(host, port, "invalid-1", new byte[] {}, params);
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
    AssistantParams params = new AssistantParams();
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

    Assistant.createWithPrivateKey(host, port, server, wrongKey, params);
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
    AssistantParams params = new AssistantParams();
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

    Assistant.createWithPrivateKey(host, port, entityWithoutCert, privateKey,
      params);
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
    AssistantParams params = new AssistantParams();
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
      Assistant.createWithPassword(host, port, "invalid-2", new byte[] {},
        params);
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
    AssistantParams params = new AssistantParams();
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
      Assistant.createWithPassword(host, port, "invalid-3", new byte[] {},
        params);
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
    AssistantParams params = new AssistantParams();
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
      Assistant.createWithPrivateKey(host, port, server, wrongKey, params);
    try {
      Thread.sleep((int) (params.interval * 3 * 1000));
    }
    catch (InterruptedException e) {
      Assert.fail(e.getMessage());
    }
    Assert.assertTrue(failed.get());
    Assert.assertTrue(asExpected.get());

    ComponentContext context = Utils.buildComponent(assistant.orb());
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
}
