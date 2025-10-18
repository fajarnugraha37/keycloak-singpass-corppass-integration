package com.example.events;

import org.jboss.logging.Logger;
import org.keycloak.Config.Scope;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class MyEventListenerFactory implements EventListenerProviderFactory {
  private static final Logger logger = Logger.getLogger(MyEventListenerFactory.class);
  public static final String ID = "my-listener";

  @Override
  public EventListenerProvider create(KeycloakSession session) {
    logger.info("MyEventListenerFactory create provider");
    return new MyEventListenerProvider();
  }

  @Override
  public void init(Scope config) {
    logger.info("MyEventListenerFactory initialized");
  }

  @Override
  public void postInit(KeycloakSessionFactory factory) {
    logger.info("MyEventListenerFactory post-initialized");
  }

  @Override
  public void close() {
    logger.info("MyEventListenerFactory closed");
  }

  @Override
  public String getId() {
    return ID;
  }
}
