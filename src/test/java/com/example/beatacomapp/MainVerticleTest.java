package com.example.beatacomapp;
import com.example.beatacomapp.constants.Constants;
import com.example.beatacomapp.constants.Parameters;
import com.example.beatacomapp.constants.Routes;
import com.example.beatacomapp.constants.StatusMessages;
import com.example.beatacomapp.entity.Item;
import com.example.beatacomapp.entity.User;
import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.IMongodConfig;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.RoutingContext;
import org.junit.*;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.ServerSocket;

@RunWith(VertxUnitRunner.class)
public class MainVerticleTest {

  private Vertx vertx;
  private Integer port;
  private static MongodProcess mongo;
  private static int MONGO_PORT = 12345;

  @BeforeClass
  public static void initialize() throws IOException {
    MongodStarter starter = MongodStarter.getDefaultInstance();

    IMongodConfig mongodConfig = new MongodConfigBuilder()
      .version(Version.Main.PRODUCTION)
      .net(new Net(MONGO_PORT, Network.localhostIsIPv6()))
      .build();

    MongodExecutable mongodExecutable = starter.prepare(mongodConfig);
    mongo = mongodExecutable.start();
  }

  @AfterClass
  public static void shutdown() {
    mongo.stop();
  }

  @Before
  public void setUp(TestContext context) throws IOException {
    Async async = context.async();
    vertx = Vertx.vertx();
    vertx.deployVerticle(MainVerticle.class.getName(), h -> {
      if(h.succeeded()){
        async.complete();
      }else{
        context.fail();
      }
    });

    // Let's configure the verticle to listen on the 'test' port (randomly picked).
    // We create deployment options and set the _configuration_ json object:
    ServerSocket socket = new ServerSocket(0);
    port = Constants.VERTICLE_PORT_NUMBER;
    socket.close();

    DeploymentOptions options = new DeploymentOptions()
      .setConfig(new JsonObject()
        .put("http.port", port)
        .put("db_name", "items-test")
        .put("connection_string", "mongodb://localhost:" + MONGO_PORT)
      );

    // We pass the options as the second parameter of the deployVerticle method.
    vertx.deployVerticle(MainVerticle.class.getName(), options, context.asyncAssertSuccess());
  }

  @After
  public void tearDown(TestContext context) {
    vertx.close(context.asyncAssertSuccess());
  }

  @Test
  public void testMyApplication(TestContext context) {
    final Async async = context.async();
    vertx.createHttpClient().getNow(port, "localhost", "/", response -> response.handler(body -> {
      context.assertTrue(body.toString().contains("Betacom"));
      async.complete();
    }));
  }

  @Test
  public void testThatTheServerIsStarted(TestContext tc) {
    Async async = tc.async();
    vertx.createHttpClient().getNow(port, "localhost", "/", response -> {
      tc.assertEquals(response.statusCode(), 200);
      response.bodyHandler(body -> {
        tc.assertTrue(body.length() > 0);
        async.complete();
      });
    });
  }
}
