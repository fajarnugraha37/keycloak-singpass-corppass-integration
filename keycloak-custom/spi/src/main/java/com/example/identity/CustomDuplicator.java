package com.example.identity;

import com.example.config.CustomOIDCIdentityProviderConfig;
import org.keycloak.broker.oidc.OIDCIdentityProvider;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

/**
 * CustomDuplicator is an abstract base class that extends OIDCIdentityProvider
 * to provide advanced user deduplication and upsert functionality for OIDC authentication flows.
 *
 * <h3>Primary Purpose:</h3>
 * This class addresses the common issue where external identity providers frequently change
 * the subject (sub) claim in tokens, which would normally result in duplicate user accounts
 * in Keycloak. Instead, it performs intelligent user lookup and linking based on stable
 * identifiers to maintain user continuity.
 *
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><strong>Multi-tier User Lookup:</strong> Searches for existing users using username, email, and custom attributes</li>
 *   <li><strong>Subject Token Upsert:</strong> Updates federated identity links when subject tokens change</li>
 *   <li><strong>Stable Identifier Matching:</strong> Uses reliable identifiers like uinfin.value, employee_id to find users</li>
 *   <li><strong>Federated Identity Management:</strong> Automatically manages identity provider links</li>
 * </ul>
 *
 * <h3>Typical Use Cases:</h3>
 * <ul>
 *   <li>Singapore Government OIDC providers that change subject tokens</li>
 *   <li>Corporate identity providers with rotating user identifiers</li>
 *   <li>Multi-tenant environments requiring user consolidation</li>
 *   <li>Migration scenarios where user identifiers change</li>
 * </ul>
 *
 * <h3>Workflow:</h3>
 * <ol>
 *   <li>User attempts login with potentially new subject token</li>
 *   <li>System searches for existing user by username → email → custom attributes</li>
 *   <li>If found, updates federated identity mapping with new subject</li>
 *   <li>If not found, allows normal user creation process</li>
 *   <li>Preserves all existing user data and attributes</li>
 * </ol>
 *
 * @author Your Organization
 * @version 1.0
 * @since Keycloak 22.x
 */
public abstract class CustomDuplicator extends OIDCIdentityProvider {

    /**
     * Constructs a new CustomDuplicator instance.
     *
     * @param session The current Keycloak session providing access to realm data and user operations
     * @param config  The custom OIDC identity provider configuration containing provider-specific settings
     */
    public CustomDuplicator(KeycloakSession session,
                            CustomOIDCIdentityProviderConfig config) {
        super(session, config);
    }

    /**
     * Preprocesses federated identity before user import/update operations.
     * This is the main entry point for the deduplication logic, called automatically
     * by Keycloak during the authentication flow after token validation but before
     * user creation or update.
     *
     * <p>The method performs the following operations:</p>
     * <ol>
     *   <li>Logs the incoming federated identity information</li>
     *   <li>Executes upsert logic to find and link existing users</li>
     *   <li>Calls the parent implementation for standard processing</li>
     * </ol>
     *
     * @param session The current Keycloak session
     * @param realm   The realm where the user authentication is taking place
     * @param context The brokered identity context containing user information from the external provider
     *
     * @see #performUpsertBasedOnStableIdentifier(KeycloakSession, RealmModel, BrokeredIdentityContext)
     */
    @Override
    public void preprocessFederatedIdentity(KeycloakSession session, RealmModel realm, BrokeredIdentityContext context) {
        logger.infof("[preprocessFederatedIdentity] Preprocessing federated identity in CustomOIDCProvider: %s", context.getUsername());

        // Implement upsert logic for changing subject tokens
        performUpsertBasedOnStableIdentifier(session, realm, context);

        super.preprocessFederatedIdentity(session, realm, context);
    }

    /**
     * Performs intelligent user upsert operation when subject tokens change frequently.
     * This method implements a three-tier lookup strategy to find existing users and
     * maintain user account continuity despite changing external identifiers.
     *
     * <h3>Lookup Priority Order:</h3>
     * <ol>
     *   <li><strong>Username:</strong> Direct username match (fastest, most reliable)</li>
     *   <li><strong>Email:</strong> Email address match (common stable identifier)</li>
     *   <li><strong>Custom Attributes:</strong> Stable identifiers like uinfin.value, employee_id</li>
     * </ol>
     *
     * <h3>Upsert Logic:</h3>
     * <ul>
     *   <li>If user found with existing federated identity → Updates subject mapping</li>
     *   <li>If user found without federated identity → Creates new identity link</li>
     *   <li>If no user found → Allows normal user creation process</li>
     * </ul>
     *
     * <h3>Data Preservation:</h3>
     * When an existing user is found, their core information (username, email, names)
     * is preserved and the context is updated to prevent duplicate account creation.
     *
     * @param session           The current Keycloak session for database operations
     * @param realm            The realm where the operation is taking place
     * @param context          The brokered identity context containing new user information
     *
     * @see #findUserByStableAttribute(KeycloakSession, RealmModel, BrokeredIdentityContext)
     * @see #updateContextWithExistingUserInfo(BrokeredIdentityContext, UserModel)
     */
    void performUpsertBasedOnStableIdentifier(KeycloakSession session,
                                              RealmModel realm,
                                              BrokeredIdentityContext context) {
        // Extract key identifiers from the incoming authentication context
        var username = context.getUsername();
        var email = context.getEmail();
        var providerAlias = getConfig().getAlias(); // The identity provider's alias (e.g., "singpass-oidc")
        var currentBrokerUserId = context.getId();  // The new subject/user ID from the external provider

        logger.infof("[performUpsertBasedOnStableIdentifier] Starting upsert with username: %s, email: %s, currentBrokerUserId: %s",
                username,
                email,
                currentBrokerUserId);

        // Primary lookup: Search by username (most direct and efficient method)
        UserModel existingUser = null;
        if (username != null && !username.isEmpty()) {
            existingUser = session.users().getUserByUsername(realm, username);
            logger.infof("[performUpsertBasedOnStableIdentifier] Looking up user by username: %s, found: %s",
                    username,
                    existingUser != null ? existingUser.getUsername() : "null");
        }

        // Secondary lookup: Search by email if username lookup failed
        // Email is often more stable than username in enterprise environments
        if (existingUser == null && email != null && !email.isEmpty()) {
            existingUser = session.users().getUserByEmail(realm, email);
            logger.infof("[performUpsertBasedOnStableIdentifier] Looking up user by email: %s, found: %s",
                    email,
                    existingUser != null ? existingUser.getUsername() : "null");
        }

        // Tertiary lookup: Search by custom stable attributes as last resort
        // This handles cases where both username and email may have changed
        if (existingUser == null) {
            existingUser = findUserByStableAttribute(session, realm, context);
        }

        if (existingUser != null) {
            logger.infof("[performUpsertBasedOnStableIdentifier] Found existing user: %s, performing upsert",
                    existingUser.getUsername());

            // Check if the found user already has a federated identity link with this provider
            var existingFederatedIdentity = session.users()
                    .getFederatedIdentity(realm, existingUser, providerAlias);

            if (existingFederatedIdentity != null) {
                // User has existing federated identity - check if subject has changed
                var existingBrokerUserId = existingFederatedIdentity.getUserId();

                if (!existingBrokerUserId.equals(currentBrokerUserId)) {
                    // Subject has changed - update the federated identity mapping
                    logger.infof("[performUpsertBasedOnStableIdentifier] Subject changed from %s to %s, updating federated identity",
                            existingBrokerUserId,
                            currentBrokerUserId);

                    // Remove the old federated identity link
                    session
                            .users()
                            .removeFederatedIdentity(realm, existingUser, providerAlias);

                    // Create new federated identity with updated subject but preserve token
                    var newFederatedIdentity = new FederatedIdentityModel(
                            providerAlias,                              // Provider alias (e.g., "singpass-oidc")
                            currentBrokerUserId,                        // New subject from external provider
                            existingUser.getUsername(),                 // Preserve existing username
                            existingFederatedIdentity.getToken());     // Preserve existing token if any

                    // Add the updated federated identity link
                    session.users()
                            .addFederatedIdentity(realm, existingUser, newFederatedIdentity);
                    logger.infof("[performUpsertBasedOnStableIdentifier] Updated federated identity for user %s with new subject", existingUser.getUsername());
                } else {
                    // Subject is unchanged - no update needed
                    logger.infof("[performUpsertBasedOnStableIdentifier] Subject unchanged for user %s, no federated identity update needed", existingUser.getUsername());
                }
            } else {
                // User exists but has no federated identity with this provider - create new link
                logger.infof("[performUpsertBasedOnStableIdentifier] Adding federated identity for existing user: %s", existingUser.getUsername());
                var newFederatedIdentity = new FederatedIdentityModel(
                        providerAlias,          // Provider alias
                        currentBrokerUserId,    // Subject from external provider
                        existingUser.getUsername()); // Link to existing user

                session.users()
                        .addFederatedIdentity(realm, existingUser, newFederatedIdentity);
            }

            // Update the context to use existing user's information and prevent duplicate creation
            updateContextWithExistingUserInfo(context, existingUser);

            logger.infof("[performUpsertBasedOnStableIdentifier] Upsert completed for user: %s", existingUser.getUsername());
        } else {
            // No existing user found - allow normal user creation process to proceed
            logger.infof("[performUpsertBasedOnStableIdentifier] No existing user found by username, email, or stable attributes - will create new user");
        }
    }

    /**
     * Updates the BrokeredIdentityContext with information from an existing user
     * and marks that an existing user was found to prevent duplicate creation.
     *
     * <p>This method serves two critical purposes:</p>
     * <ol>
     *   <li><strong>Data Preservation:</strong> Ensures existing user data takes precedence over incoming data</li>
     *   <li><strong>Duplicate Prevention:</strong> Marks the context to prevent Keycloak from creating a new user</li>
     * </ol>
     *
     * <h3>Information Preserved:</h3>
     * <ul>
     *   <li>Username (always preserved from existing user)</li>
     *   <li>Email address (if exists in existing user)</li>
     *   <li>First name (if exists in existing user)</li>
     *   <li>Last name (if exists in existing user)</li>
     * </ul>
     *
     * <h3>Context Markers:</h3>
     * <ul>
     *   <li><code>EXISTING_USER_FOUND</code>: Contains the existing user's ID</li>
     *   <li><code>EXISTING_USER_USERNAME</code>: Contains the existing user's username</li>
     * </ul>
     *
     * @param context      The brokered identity context to update
     * @param existingUser The existing user whose information should be preserved
     */
    private void updateContextWithExistingUserInfo(BrokeredIdentityContext context, UserModel existingUser) {
        // Always preserve the existing user's username to maintain identity consistency
        context.setUsername(existingUser.getUsername());

        // Preserve existing email if available (prevents email conflicts)
        if (existingUser.getEmail() != null) {
            context.setEmail(existingUser.getEmail());
        }

        // Preserve existing first name if available (maintains user profile consistency)
        if (existingUser.getFirstName() != null) {
            context.setFirstName(existingUser.getFirstName());
        }

        // Preserve existing last name if available (maintains user profile consistency)
        if (existingUser.getLastName() != null) {
            context.setLastName(existingUser.getLastName());
        }

        // Mark that we found an existing user to prevent duplicate creation
        // These markers can be used by other parts of the authentication flow
        context.getContextData().put("EXISTING_USER_FOUND", existingUser.getId());
        context.getContextData().put("EXISTING_USER_USERNAME", existingUser.getUsername());

        logger.infof("[updateContextWithExistingUserInfo] Updated context with existing user info - username: %s, email: %s, first nam: %s, last name: %s",
                existingUser.getUsername(),
                existingUser.getEmail(),
                existingUser.getFirstName(),
                existingUser.getLastName());
    }

    /**
     * Searches for existing users based on stable custom attributes when username and email lookups fail.
     * This method implements the third tier of the lookup strategy, using custom attributes that are
     * less likely to change over time.
     *
     * <h3>Search Strategy:</h3>
     * The method iterates through a prioritized list of stable identifiers and performs
     * attribute-based user searches. It returns the first user found with a matching attribute value.
     *
     * <h3>Stable Identifiers (in priority order):</h3>
     * <ol>
     *   <li><strong>uinfin.value:</strong> Singapore NRIC/FIN number (highly stable)</li>
     *   <li><strong>id_token.entityInfo.CPEntID:</strong> Corporate entity ID from ID token</li>
     *   <li><strong>user_id:</strong> Generic user identifier</li>
     *   <li><strong>external_id:</strong> External system identifier</li>
     *   <li><strong>unique_identifier:</strong> Custom unique identifier</li>
     * </ol>
     *
     * <h3>Search Process:</h3>
     * <ol>
     *   <li>Extract attribute value from the authentication context</li>
     *   <li>Search for users in the realm with matching attribute value</li>
     *   <li>Return the first matching user (assumes uniqueness of stable identifiers)</li>
     *   <li>Log all search attempts for debugging purposes</li>
     * </ol>
     *
     * @param session The current Keycloak session for user search operations
     * @param realm   The realm to search within
     * @param context The brokered identity context containing attribute values
     * @return The first user found with a matching stable attribute, or null if none found
     *
     * @see KeycloakSession#users()
     * @see org.keycloak.models.UserProvider#searchForUserByUserAttributeStream(RealmModel, String, String)
     */
    UserModel findUserByStableAttribute(KeycloakSession session,
                                        RealmModel realm,
                                        BrokeredIdentityContext context) {
        // Define stable identifiers in priority order
        // These attributes are expected to remain constant across authentication sessions
        var stableIdentifiers = new String[]{
                "uinfin.value",                    // Singapore NRIC/FIN - most stable for SG users
                "id_token.entityInfo.CPEntID",     // Corporate entity ID from ID token
                "user_id",                         // Generic user identifier
                "external_id",                     // External system identifier
                "unique_identifier"                // Custom unique identifier
        };

        // Iterate through each stable identifier to find a match
        for (var attributeName : stableIdentifiers) {
            // Get the attribute values from the context (may be multiple values)
            var attributeValues = context.getAttributes().get(attributeName);

            if (attributeValues != null && !attributeValues.isEmpty()) {
                // Use the first value for searching (assumes single-valued stable identifiers)
                var attributeValue = attributeValues.get(0);
                logger.infof("[findUserByStableAttribute] Looking up user by attribute %s: %s", attributeName, attributeValue);

                // Search for users with this attribute value in the realm
                // Note: This may return multiple users, but stable identifiers should be unique
                var users = session.users()
                        .searchForUserByUserAttributeStream(realm, attributeName, attributeValue)
                        .peek(item -> logger.infof("[findUserByStableAttribute] Checking user: %s", item.getUsername()))
                        .toList();

                if (!users.isEmpty()) {
                    // Return the first matching user (assumes uniqueness of stable identifiers)
                    var foundUser = users.get(0);
                    logger.infof("[findUserByStableAttribute] Found user by attribute %s=%s: %s",
                            attributeName, attributeValue, foundUser.getUsername());
                    return foundUser;
                }
            } else {
                // Log when no value is found for an attribute (helps with debugging)
                logger.infof("[findUserByStableAttribute] No value found for attribute %s in context", attributeName);
            }
        }

        // No user found with any of the stable identifiers
        logger.infof("[findUserByStableAttribute] No user found by stable attributes");
        return null;
    }
}
