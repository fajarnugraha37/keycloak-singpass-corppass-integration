package com.example.events;

import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;

public class MyEventListenerProvider implements EventListenerProvider {
  private static final Logger logger = Logger.getLogger(MyEventListenerProvider.class);

  @Override
  public void onEvent(Event event) {
    if (event.getError() != null) {
      logger.errorf("auth-error type=%s realm=%s client=%s user=%s details=%s",
              event.getType(),
              event.getRealmId(),
              event.getClientId(),
              event.getUserId(),
              event.getDetails(),
              event.getError());
    } else {
      logger.infof("auth type=%s realm=%s client=%s user=%s details=%s",
              event.getType(),
              event.getRealmId(),
              event.getClientId(),
              event.getUserId(),
              event.getDetails());
    }
  }

  @Override
  public void onEvent(AdminEvent event, boolean includeRepresentation) {
    if (event.getError() != null) {
      logger.errorf("admin-error op=%s resType=%s realm=%s resPath=%s",
          event.getOperationType(), event.getResourceType(), event.getRealmId(), event.getResourcePath(),
          event.getError());
    } else {
      logger.infof("admin op=%s resType=%s realm=%s resPath=%s auth=%s",
          event.getOperationType(), event.getResourceType(), event.getRealmId(), event.getResourcePath(),
          event.getAuthDetails());
    }
  }

  @Override
  public void close() {
    logger.info("MyEventListenerProvider closed");
  }
}
