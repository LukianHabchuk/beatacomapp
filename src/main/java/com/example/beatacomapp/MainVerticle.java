package com.example.beatacomapp;

import com.example.beatacomapp.constants.Constants;
import com.example.beatacomapp.constants.Parameters;
import com.example.beatacomapp.constants.Routes;
import com.example.beatacomapp.constants.StatusMessages;
import com.example.beatacomapp.entity.Item;
import com.example.beatacomapp.entity.User;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.jwt.JWTOptions;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.templ.freemarker.FreeMarkerTemplateEngine;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class MainVerticle extends AbstractVerticle {
  private MongoClient mongoClient;
  private FreeMarkerTemplateEngine templateEngine;
  private JWTAuth provider;
  private String errorMessage = "";
  private User currentUser;

  @Override
  public void start(Future<Void> startFuture) {
    Future<Void> future = setupDatabase().compose(v -> setupHttpServer()).compose(v -> setupAuth());
    future.setHandler(asyncResult -> {
      if (asyncResult.succeeded()) {
        startFuture.complete();
      } else {
        startFuture.fail(asyncResult.cause().toString());
      }
    });
  }

  @Override
  public void stop() throws Exception {
    mongoClient.close();
  }

  //setup routes, server and port
  private Future<Void> setupHttpServer() {
    Future<Void> future = Future.future();

    HttpServer server = vertx.createHttpServer();
    Router router = Router.router(vertx);
    router.get(Routes.MAIN_PAGE).handler(this::mainPage);
    router.get(Routes.ITEMS).handler(this::getItems);
    router.get(Routes.LOGOUT).handler(this::logout);
    router.post().handler(BodyHandler.create());
    router.post(Routes.LOGIN).handler(this::login);
    router.post(Routes.REGISTER).handler(this::register);
    router.post(Routes.ITEMS).handler(this::addItem);
    router.post(Routes.DELETE).handler(this::deleteItem);

    //setup website renderer
    templateEngine = FreeMarkerTemplateEngine.create(vertx);
    server.requestHandler(router)
      .listen(
      Constants.VERTICLE_PORT_NUMBER, asyncResult -> {
        if (asyncResult.succeeded()) {
          Logger.getAnonymousLogger().log(Level.INFO, "HTTP server running on port {0}", Constants.VERTICLE_PORT_NUMBER);
          future.complete();
        } else {
          Logger.getAnonymousLogger().log(Level.INFO, MessageFormat.format("Could not start a HTTP server\n {0}", asyncResult.cause().toString()));
          future.fail(asyncResult.cause());
        }
      });
    return future;
  }

  //setup mongo client
  private Future<Void> setupDatabase() {
    config().remove(Constants.DATA_BASE_NAME);
    mongoClient = MongoClient.createShared(vertx, config().put(Constants.DATA_BASE_NAME, "microservices")); //creates pool on the first call
    Future<Void> future = Future.future();
    future.complete();
    return future;
  }

  //setup provider
  private Future<Void> setupAuth() {
    JWTAuthOptions config = new JWTAuthOptions()
      .addPubSecKey(new PubSecKeyOptions()
        .setAlgorithm(Constants.AUTH_ALGORITHM)
        .setPublicKey(Constants.AUTH_PUBLIC_KEY)
        .setSymmetric(true));
    provider = JWTAuth.create(vertx, config);
    Future<Void> future = Future.future();
    future.complete();
    return future;
  }

  //Add new item to the collection for current user
  private void addItem(RoutingContext context) {
    Item item = getItem(context);
    JsonObject query = new JsonObject().put(Parameters.OWNER, currentUser.getId()).put(Parameters.NAME, item.getName());
    mongoClient.save(Parameters.ITEMS, query, resultHandler -> {
      if (resultHandler.succeeded()) {
        context.response().setStatusCode(204).setStatusMessage(StatusMessages.ADD_ITEM_SUCCESS);
        context.response().putHeader(Parameters.LOCATION, Routes.ITEMS).setStatusCode(Constants.REDIRECT_STATUS_CODE).end();
      } else {
        context.response().setStatusCode(401).setStatusMessage(StatusMessages.ADD_ITEM_FAILURE);
      }
    });
  }

  //Delete item from the db
  private void deleteItem(RoutingContext context) {
    Item item = getItem(context);
    JsonObject query = new JsonObject().put(Parameters.OWNER, currentUser.getId()).put(Parameters.NAME, item.getName());
    mongoClient.removeDocument(Parameters.ITEMS, query, resultHandler -> {
      if (resultHandler.succeeded()) {
        context.response().putHeader(Parameters.LOCATION, Routes.ITEMS);
        context.response().setStatusCode(Constants.REDIRECT_STATUS_CODE); //Redirection http response(3xx) - Constants.SERVER_STATUS_CODE moved permanently
        context.response().end();
      } else {
        context.fail(resultHandler.cause());
      }
    });
  }

  //route to login/register page
  private void mainPage(RoutingContext context) {
    context.put(Parameters.TITLE, "Log in");
    context.put("errorMessage", errorMessage); //sets "Wrong login or password! Please try again" above input form
    websiteRender(context, "templates/main.ftl");
  }

  //logout from the system
  private void logout(RoutingContext context) {
    context.response().putHeader(Parameters.LOCATION, Routes.MAIN_PAGE);
    context.response().setStatusCode(Constants.REDIRECT_STATUS_CODE); //Redirection http response(3xx) - Constants.SERVER_STATUS_CODE moved permanently
    context.response().end();
  }

  //find all items for current user
  private void getItems(RoutingContext context) {
    JsonObject query = new JsonObject().put((Parameters.USERNAME), currentUser.getLogin());
    //check is such user exists
    mongoClient.find(Parameters.USERS, query, result -> {
      if (result.succeeded()) {
        context.put(Parameters.TITLE, "My Account");
        context.put("user", currentUser.getLogin());
        //finding all items for current user
        mongoClient.find(Parameters.ITEMS, new JsonObject().put(Parameters.OWNER, currentUser.getId()), resultHandler -> {
          List<String> items = new ArrayList<>();
          if (resultHandler.succeeded()) {
            if (!resultHandler.result().isEmpty()) {
              //collecting available items
              items = resultHandler.result().stream().map(json -> json.getString(Parameters.NAME)).sorted().collect(Collectors.toList());
            }
            context.response().setStatusCode(200);
            //parameter needed to show all items on the web page
            context.put(Parameters.ITEMS, items);
            websiteRender(context, "templates/itemPage.ftl");
          } else {
            context.response().setStatusCode(401).setStatusMessage(StatusMessages.GET_ITEMS_FAILURE);
          }
        });
      } else {
        context.response().setStatusCode(401).setStatusMessage(StatusMessages.GET_ITEMS_FAILURE);
      }
    });
  }


  private void login(RoutingContext context) {
    User user = getUserFromForm(context);
    JsonObject query = new JsonObject().put((Parameters.USERNAME), user.getLogin()).put(Parameters.PASSWORD, user.getPassword());

    //check is such user exists
    mongoClient.find(Parameters.USERS, query, result -> {
      if (result.succeeded()) {
        if (result.result().isEmpty()) {
          //if user is not exists in db
          errorMessage = StatusMessages.LOGIN_FAILURE;
          context.response().putHeader(Parameters.LOCATION, Routes.MAIN_PAGE);
          context.response().setStatusCode(Constants.REDIRECT_STATUS_CODE); //Redirection http response(3xx) - Constants.SERVER_STATUS_CODE moved permanently
          context.response().end();
        } else {
          errorMessage = "";
          String tokenLogin = provider.generateToken(new JsonObject().put("sub", user.getLogin()), new JWTOptions());
          Logger.getAnonymousLogger().log(Level.INFO, tokenLogin);
          provider.authenticate(new JsonObject().put("jwt", tokenLogin), resultHandler -> {
            if (resultHandler.succeeded()) {
              currentUser = user;
              currentUser.setId(result.result().get(0).getString(Parameters.ID));
              context.response().setStatusCode(200).setStatusMessage(StatusMessages.LOGIN_SUCCESS);
              context.response().putHeader(Parameters.LOCATION, Routes.ITEMS).setStatusCode(Constants.REDIRECT_STATUS_CODE).end();
            } else {
              resultHandler.cause().printStackTrace();
            }
          });
        }
      }
    });
  }

  private void register(RoutingContext context) {
    User user = getUserFromForm(context);
    JsonObject query = new JsonObject().put(Parameters.USERNAME, user.getLogin());
    Logger.getAnonymousLogger().log(Level.INFO, "query {0}", query);

    //check is such user already exists
    mongoClient.find(Parameters.USERS, query, result -> {
      //if result is empty user is new
      if (result.succeeded() && result.result().isEmpty()) {
        //put password to query to save new user
        query.put(Parameters.PASSWORD, user.getPassword());
        mongoClient.save(Parameters.USERS, query, res -> {
          if (res.succeeded()) {
            errorMessage = "Account created successfully";
            context.response().setStatusCode(204).setStatusMessage(StatusMessages.REGISTER_SUCCESS);
            context.response().putHeader(Parameters.LOCATION, Routes.MAIN_PAGE).setStatusCode(Constants.REDIRECT_STATUS_CODE).end();
          } else {
            res.cause().printStackTrace();
          }
        });
      } else {
        errorMessage = StatusMessages.REGISTER_FAILURE;
        context.response().putHeader(Parameters.LOCATION, Routes.MAIN_PAGE).setStatusCode(Constants.REDIRECT_STATUS_CODE).end();
      }
    });
  }

  //render web pages
  private void websiteRender(RoutingContext context, String filename) {
    templateEngine.render(context.data(), filename, ar -> {
      if (ar.succeeded()) {
        context.response().putHeader("Content-Type", "text/html");
        context.response().end(ar.result());
      } else {
        context.fail(ar.cause());
      }
    });
  }

  //return user object from context
  private User getUserFromForm(RoutingContext context) {
    return new User(context.request().getParam(Parameters.LOGIN_ID), context.request().getParam(Parameters.PASSWORD_ID));
  }

  //return item object from context
  private Item getItem(RoutingContext context) {
    return new Item(context.request().getParam(Parameters.OWNER), context.request().getParam(Parameters.NAME));
  }
}
