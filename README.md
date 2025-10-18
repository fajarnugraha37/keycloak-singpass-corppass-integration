# SSO Playground

This repository provides a self-contained single sign-on (SSO) playground built with Docker. It demonstrates how multiple applications can share authentication using [Keycloak](https://www.keycloak.org/) and a custom Node.js OpenID Connect (OIDC) provider.

The stack consists of two example single-page applications (SPAs), their backend APIs, Keycloak realms, and an OIDC provider that brokers login to Keycloak. Everything is wired together through an Nginx reverse proxy.

## Architecture

All components run as containers defined in `docker-compose.yml`:

- **Keycloak (internal)** – primary identity provider loaded with the `agency-realm` configuration and a demo user.
- **Keycloak (external)** – mock external identity provider used for federated login.
- **ids** – Node.js OIDC provider that federates to the agency Keycloak and mints application tokens for CPDS.
- **app-1-api** – sample API secured by tokens issued directly by Keycloak.
- **app-2-api** – sample API secured by tokens from the `ids` provider.
- **web** – Nginx serving the SPAs and routing API requests to the proper services.

### How It Works

```
  Client Browser
        |
        v
  +-------------+
  |     web     |  (Nginx reverse proxy)
  +------+------+
         |                      
         +--------------------------------------+
         |                                      |
         v                                      v
    +----------+                           +----------+
    | App-1 SPA|                           | App-2 SPA|
    +-----+----+                           +----+-----+
          |                                      |
          v                                      v
    +----------+                           +----------+
    |app-1-api |                           |app-2-api |
    +-----+----+                           +----+-----+
          |                                      |
          v                                      v
    +----------+         federated login    +---------+
    | Keycloak | <--------------------------|   IDS   |
    | (agency) |                            +----+----+
    +-----+----+                                 |
          |                                      v
          |                                +-----------+
          |                                | Keycloak  |
          |                                | (mock IdP)|
          |                                +-----------+
          +------------------------------------+
               direct tokens for App-1
```

#### Cross-App SSO Flow

When a user already authenticated in **App-1** opens **App-2**, the existing Keycloak
session is reused so no extra login is required:

```
Client Browser
    |
    v
+-----------+
| App-2 SPA |
+-----+-----+
      |
      | (1) redirect to IDS if no App-2 token
      v
+-----------+
|    IDS    |
+-----+-----+
      |
      | (2) redirect to Keycloak if no IDS session
      v
+-----------+
| Keycloak  |
+-----+-----+
      |
      | (3) existing session -> issue auth code
      v
+-----------+
|    IDS    |
+-----+-----+
      |
      | (4) mint App-2 token & set cookie
      v
+-----------+
| App-2 SPA |
+-----+-----+
      |
      | (5) call App-2 API with token
      v
+-----------+
| App-2 API |
+-----------+
```

This flow shows how the IDS provider leverages the Keycloak session established with
App-1, enabling seamless navigation between apps without a second login.

See the compose file for exact settings and ports.

## Prerequisites

- [Docker](https://docs.docker.com/get-docker/) and Docker Compose
- [Make](https://www.gnu.org/software/make/) (optional, used for convenience commands)
- A host entry for `eservice.localhost` pointing to `127.0.0.1` (add to `/etc/hosts` or similar)

## Getting Started

1. **Start the stack**

   ```bash
   make up
   ```

   This builds all images and starts the containers in the background.

2. **Access the apps**

   - App-1 SPA: [Link](http://eservice.localhost/aceas/)
   - App-2 SPA: [Link](http://eservice.localhost/cpds/)

3. **Default credentials**

   - Keycloak demo user: `demo` / `demo123`
   - Mock IdP user: `idpuser` / `idp123`

4. **Stop the stack**

   ```bash
   make down
   ```

## Service Overview

| Service      | Description |
|--------------|-------------|
| `keycloak`   | Keycloak server hosting the `agency-realm` and backing App-2 and App-1 |
| `keycloak_idp` | Secondary Keycloak instance acting as a mock external identity provider |
| `ids`        | Node.js OIDC provider that brokers to `keycloak` and issues tokens for App-2 |
| `app-1-api`  | Example API protected by Keycloak-issued tokens |
| `app-2-api`   | Example API protected by tokens from the `ids` provider |
| `web`        | Nginx reverse proxy serving static SPAs and routing requests |

## Useful Make Targets

- `make up` – build and start all containers
- `make down` – stop and remove containers and images
- `make re-ids` / `make re-cpds` / `make re-aceas` – rebuild a specific service
- `make log-ids` (and similar) – follow logs for a service

## Directory Structure

```
.
├─ keycloak/         # Realm exports and mock IdP configuration
├─ services/         # Node.js services (ids, app-2-api, app-1-api)
├─ webroot/          # Static SPA frontends
├─ nginx.conf        # Reverse proxy configuration
├─ docker-compose.yml
└─ Makefile
```

## Notes

- The SPAs assume the site is served from `http://eservice.localhost`. Ensure your hosts file maps this name to `127.0.0.1`.
- The `ids` service demonstrates exchanging Keycloak tokens for application-specific tokens and includes endpoints for login, refresh and logout.
