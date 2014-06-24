package tecgraf.openbus.assistant;

import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

import junit.framework.Assert;

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
import tecgraf.openbus.core.ORBInitializer;
import tecgraf.openbus.core.OpenBusPrivateKey;
import tecgraf.openbus.core.v2_0.OctetSeqHolder;
import tecgraf.openbus.core.v2_0.services.access_control.LoginInfo;
import tecgraf.openbus.core.v2_0.services.access_control.LoginProcess;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceOfferDesc;
import tecgraf.openbus.core.v2_0.services.offer_registry.ServiceProperty;
import tecgraf.openbus.util.Utils;

public class AssistantTest {

  private static String host;
  private static int port;
  private static String entity;
  private static byte[] password;
  private static String server;
  private static String privateKeyFile;
  private static OpenBusPrivateKey privateKey;

  @BeforeClass
  public static void oneTimeSetUp() throws Exception {
    Properties properties = Utils.readPropertyFile("/test.properties");
    host = properties.getProperty("openbus.host.name");
    port = Integer.valueOf(properties.getProperty("openbus.host.port"));
    entity = properties.getProperty("entity.name");
    password = properties.getProperty("entity.password").getBytes();
    server = properties.getProperty("server.entity.name");
    privateKeyFile = properties.getProperty("server.private.key");
    privateKey = OpenBusPrivateKey.createPrivateKeyFromFile(privateKeyFile);
    Utils.setLogLevel(Level.FINE);
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
    Thread.sleep((int) (params.interval * 3 * 1000));
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
    Thread.sleep((int) (params.interval * 3 * 1000));
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
        ServiceProperty[] properties, Throwable except) {
        failed.set(true);
      }

      @Override
      public void onLoginFailure(Assistant assistant, Throwable except) {
        // do nothing
      }

      @Override
      public void onFindFailure(Assistant assistant, Throwable except) {
        // do nothing
      }

      @Override
      public void onStartSharedAuthFailure(Assistant assistant, Throwable except) {
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
  public void loginBySharedAuthTest() throws Throwable {
    final AtomicBoolean failed = new AtomicBoolean(false);
    AssistantParams params = new AssistantParams();
    params.interval = 1.0f;
    params.callback = new OnFailureCallback() {

      @Override
      public void onRegisterFailure(Assistant assistant, IComponent component,
        ServiceProperty[] properties, Throwable except) {
        // do nothing
      }

      @Override
      public void onLoginFailure(Assistant assistant, Throwable except) {
        failed.set(true);
      }

      @Override
      public void onFindFailure(Assistant assistant, Throwable except) {
        failed.set(true);
      }

      @Override
      public void onStartSharedAuthFailure(Assistant assistant, Throwable except) {
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
          OctetSeqHolder secret = new OctetSeqHolder();
          LoginProcess loginProcess = conn.startSharedAuth(secret);
          conn.logout();
          return new AuthArgs(loginProcess, secret.value);
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
        ServiceProperty[] properties, Throwable except) {
        // do nothing
      }

      @Override
      public void onLoginFailure(Assistant assistant, Throwable except) {
        failed.set(true);
      }

      @Override
      public void onFindFailure(Assistant assistant, Throwable except) {
        // do nothing
      }

      @Override
      public void onStartSharedAuthFailure(Assistant assistant, Throwable except) {
        failed.set(true);
      }
    };
    Assistant assist =
      Assistant.createWithPassword(host, port, entity, password, params);
    OctetSeqHolder secret = new OctetSeqHolder();
    LoginProcess attempt = assist.startSharedAuth(secret, 1);
    Assert.assertFalse(failed.get());
    Assert.assertNotNull(attempt);

    // connect using basic API
    OpenBusContext context =
      (OpenBusContext) assist.orb()
        .resolve_initial_references("OpenBusContext");
    Connection conn = context.createConnection(host, port);
    conn.loginBySharedAuth(attempt, secret.value);
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
        ServiceProperty[] properties, Throwable except) {
        // do nothing
      }

      @Override
      public void onLoginFailure(Assistant assistant, Throwable except) {
        failed.set(true);
      }

      @Override
      public void onFindFailure(Assistant assistant, Throwable except) {
        // do nothing
      }

      @Override
      public void onStartSharedAuthFailure(Assistant assistant, Throwable except) {
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

}
