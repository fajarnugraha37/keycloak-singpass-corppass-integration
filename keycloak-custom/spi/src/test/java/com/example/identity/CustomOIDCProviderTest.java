package com.example.identity;

//import com.example.config.CustomOIDCIdentityProviderConfig;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.node.ArrayNode;
//import com.fasterxml.jackson.databind.node.ObjectNode;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.keycloak.broker.provider.BrokeredIdentityContext;
//import org.keycloak.models.*;
//import org.keycloak.representations.AccessTokenResponse;
//import org.keycloak.representations.JsonWebToken;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.io.IOException;
//import java.util.*;
//import java.util.stream.Stream;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;

/**
 * Unit tests for CustomOIDCProvider with focus on:
 * 1. Handling nested JSON objects and arrays
 * 2. Upsert functionality when subject tokens change
 * 3. User attribute mapping and updates
 */
//@ExtendWith(MockitoExtension.class)
class CustomOIDCProviderTest {
//
//    @Mock
//    private KeycloakSession session;
//
//    @Mock
//    private RealmModel realm;
//
//    @Mock
//    private UserProvider userProvider;
//
//    @Mock
//    private UserModel existingUser;
//
//    @Mock
//    private UserModel newUser;
//
//    @Mock
//    private CustomOIDCIdentityProviderConfig config;
//
//    private CustomOIDCProvider provider;
//    private ObjectMapper objectMapper;
//
//    @BeforeEach
//    void setUp() {
//        when(session.users()).thenReturn(userProvider);
//        when(config.getAlias()).thenReturn("test-provider");
//
//        provider = new CustomOIDCProvider(session, config);
//        objectMapper = new ObjectMapper();
//    }
//
//    @Test
//    void testUpsertWithChangingSubject_ExistingUserByEmail() {
//        // Arrange
//        String email = "test@example.com";
//        String oldSubject = "old-subject-123";
//        String newSubject = "new-subject-456";
//        String providerAlias = "test-provider";
//
//        BrokeredIdentityContext context = createBrokeredIdentityContext(newSubject, email);
//
//        when(userProvider.getUserByEmail(realm, email)).thenReturn(existingUser);
//        when(existingUser.getUsername()).thenReturn("testuser");
//        when(existingUser.getEmail()).thenReturn(email);
//        when(existingUser.getFirstName()).thenReturn("Test");
//        when(existingUser.getLastName()).thenReturn("User");
//
//        FederatedIdentityModel oldFederatedIdentity = new FederatedIdentityModel(
//                providerAlias, oldSubject, "testuser", "old-token");
//        when(userProvider.getFederatedIdentity(realm, existingUser, providerAlias))
//                .thenReturn(oldFederatedIdentity);
//
//        // Act
//        provider.preprocessFederatedIdentity(session, realm, context);
//
//        // Assert
//        verify(userProvider).removeFederatedIdentity(realm, existingUser, providerAlias);
//        verify(userProvider).addFederatedIdentity(eq(realm), eq(existingUser), any(FederatedIdentityModel.class));
//        assertEquals("testuser", context.getUsername());
//        assertEquals(email, context.getEmail());
//        assertEquals("Test", context.getFirstName());
//        assertEquals("User", context.getLastName());
//        assertTrue(context.getContextData().containsKey("EXISTING_USER_FOUND"));
//    }
//
//    @Test
//    void testUpsertWithStableAttribute_FindByUinfinValue() {
//        // Arrange
//        String uinfinValue = "S1234567A";
//        String newSubject = "new-subject-789";
//        String providerAlias = "test-provider";
//
//        BrokeredIdentityContext context = createBrokeredIdentityContext(newSubject, null);
//        context.setUserAttribute("uinfin.value", uinfinValue);
//
//        when(userProvider.getUserByEmail(any(), any())).thenReturn(null);
//        when(userProvider.searchForUserByUserAttributeStream(realm, "uinfin.value", uinfinValue))
//                .thenReturn(Stream.of(existingUser));
//        when(existingUser.getUsername()).thenReturn("existing-uinfin-user");
//        when(userProvider.getFederatedIdentity(realm, existingUser, providerAlias))
//                .thenReturn(null);
//
//        // Act
//        provider.preprocessFederatedIdentity(session, realm, context);
//
//        // Assert
//        verify(userProvider).addFederatedIdentity(eq(realm), eq(existingUser), any(FederatedIdentityModel.class));
//        assertEquals("existing-uinfin-user", context.getUsername());
//        assertTrue(context.getContextData().containsKey("EXISTING_USER_FOUND"));
//    }
//
//    @Test
//    void testExtractIdentityWithNestedJsonObject() throws IOException {
//        // Arrange
//        JsonWebToken idToken = createMockIdToken("test-subject", "test@example.com");
//        AccessTokenResponse tokenResponse = new AccessTokenResponse();
//        String accessToken = "access-token-123";
//
//        // Create nested JSON structure for userinfo
//        ObjectNode userInfo = objectMapper.createObjectNode();
//        userInfo.put("sub", "test-subject");
//        userInfo.put("email", "test@example.com");
//
//        // Nested personal info object
//        ObjectNode personalInfo = objectMapper.createObjectNode();
//        personalInfo.put("given_name", "John");
//        personalInfo.put("family_name", "Doe");
//        personalInfo.put("middle_name", "William");
//        userInfo.set("personal_info", personalInfo);
//
//        // Nested contact object
//        ObjectNode contact = objectMapper.createObjectNode();
//        contact.put("primary_phone", "+65-1234-5678");
//        contact.put("secondary_phone", "+65-8765-4321");
//        ArrayNode addresses = objectMapper.createArrayNode();
//        ObjectNode homeAddress = objectMapper.createObjectNode();
//        homeAddress.put("type", "home");
//        homeAddress.put("street", "123 Main St");
//        homeAddress.put("city", "Singapore");
//        addresses.add(homeAddress);
//        contact.set("addresses", addresses);
//        userInfo.set("contact", contact);
//
//        // Nested uinfin object
//        ObjectNode uinfin = objectMapper.createObjectNode();
//        uinfin.put("value", "S1234567A");
//        uinfin.put("verified", true);
//        userInfo.set("uinfin", uinfin);
//
//        when(config.isDisableUserInfoService()).thenReturn(false);
//
//        // Act
//        BrokeredIdentityContext result = provider.extractIdentity(tokenResponse, accessToken, idToken);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals("test-subject", result.getId());
//        assertEquals("test@example.com", result.getEmail());
//
//        // Verify nested attributes are flattened and accessible
//        assertTrue(result.getAttributes().containsKey("personal_info.given_name"));
//        assertEquals(List.of("John"), result.getAttributes().get("personal_info.given_name"));
//
//        assertTrue(result.getAttributes().containsKey("contact.primary_phone"));
//        assertEquals(List.of("+65-1234-5678"), result.getAttributes().get("contact.primary_phone"));
//
//        assertTrue(result.getAttributes().containsKey("contact.addresses[0].street"));
//        assertEquals(List.of("123 Main St"), result.getAttributes().get("contact.addresses[0].street"));
//
//        assertTrue(result.getAttributes().containsKey("uinfin.value"));
//        assertEquals(List.of("S1234567A"), result.getAttributes().get("uinfin.value"));
//    }
//
//    @Test
//    void testExtractIdentityWithJsonArray() throws IOException {
//        // Arrange
//        JsonWebToken idToken = createMockIdToken("test-subject", "test@example.com");
//        AccessTokenResponse tokenResponse = new AccessTokenResponse();
//        String accessToken = "access-token-123";
//
//        // Create JSON structure with arrays
//        ObjectNode userInfo = objectMapper.createObjectNode();
//        userInfo.put("sub", "test-subject");
//
//        // Array of roles
//        ArrayNode roles = objectMapper.createArrayNode();
//        roles.add("admin");
//        roles.add("user");
//        roles.add("manager");
//        userInfo.set("roles", roles);
//
//        // Array of permissions with nested objects
//        ArrayNode permissions = objectMapper.createArrayNode();
//        ObjectNode perm1 = objectMapper.createObjectNode();
//        perm1.put("resource", "documents");
//        perm1.put("action", "read");
//        permissions.add(perm1);
//
//        ObjectNode perm2 = objectMapper.createObjectNode();
//        perm2.put("resource", "users");
//        perm2.put("action", "write");
//        permissions.add(perm2);
//        userInfo.set("permissions", permissions);
//
//        when(config.isDisableUserInfoService()).thenReturn(false);
//
//        // Act
//        BrokeredIdentityContext result = provider.extractIdentity(tokenResponse, accessToken, idToken);
//
//        // Assert
//        assertNotNull(result);
//
//        // Verify array elements are accessible
//        assertTrue(result.getAttributes().containsKey("roles[0]"));
//        assertEquals(List.of("admin"), result.getAttributes().get("roles[0]"));
//
//        assertTrue(result.getAttributes().containsKey("roles[1]"));
//        assertEquals(List.of("user"), result.getAttributes().get("roles[1]"));
//
//        assertTrue(result.getAttributes().containsKey("permissions[0].resource"));
//        assertEquals(List.of("documents"), result.getAttributes().get("permissions[0].resource"));
//
//        assertTrue(result.getAttributes().containsKey("permissions[1].action"));
//        assertEquals(List.of("write"), result.getAttributes().get("permissions[1].action"));
//    }
//
//    @Test
//    void testUpdateBrokeredUserWithNestedAttributes() {
//        // Arrange
//        BrokeredIdentityContext context = new BrokeredIdentityContext("test-id", config);
//
//        // Set nested attributes that changed
//        context.setUserAttribute("personal_info.phone", "new-phone-number");
//        context.setUserAttribute("contact.addresses[0].city", "New York");
//        context.setUserAttribute("roles[0]", "super-admin");
//        context.setUserAttribute("unchanged_attr", "same-value");
//
//        // Mock existing user attributes
//        when(existingUser.getUsername()).thenReturn("testuser");
//        when(existingUser.getFirstAttribute("personal_info.phone")).thenReturn("old-phone-number");
//        when(existingUser.getFirstAttribute("contact.addresses[0].city")).thenReturn("Singapore");
//        when(existingUser.getFirstAttribute("roles[0]")).thenReturn("admin");
//        when(existingUser.getFirstAttribute("unchanged_attr")).thenReturn("same-value");
//
//        // Act
//        provider.updateBrokeredUser(session, realm, existingUser, context);
//
//        // Assert
//        verify(existingUser).setAttribute("personal_info.phone", List.of("new-phone-number"));
//        verify(existingUser).setAttribute("contact.addresses[0].city", List.of("New York"));
//        verify(existingUser).setAttribute("roles[0]", List.of("super-admin"));
//        verify(existingUser, never()).setAttribute(eq("unchanged_attr"), any());
//    }
//
//    @Test
//    void testUpsertWithComplexNestedIdentifiers() {
//        // Arrange
//        String employeeId = "EMP123456";
//        String departmentCode = "IT001";
//        String newSubject = "complex-subject-123";
//
//        BrokeredIdentityContext context = createBrokeredIdentityContext(newSubject, null);
//        context.setUserAttribute("employee.id", employeeId);
//        context.setUserAttribute("department.code", departmentCode);
//        context.setUserAttribute("organization.unit.level1", "Technology");
//
//        when(userProvider.getUserByEmail(any(), any())).thenReturn(null);
//        when(userProvider.searchForUserByUserAttributeStream(realm, "uinfin.value", any()))
//                .thenReturn(Stream.empty());
//        when(userProvider.searchForUserByUserAttributeStream(realm, "employee_id", any()))
//                .thenReturn(Stream.empty());
//        when(userProvider.searchForUserByUserAttributeStream(realm, "user_id", any()))
//                .thenReturn(Stream.empty());
//        when(userProvider.searchForUserByUserAttributeStream(realm, "external_id", any()))
//                .thenReturn(Stream.empty());
//        when(userProvider.searchForUserByUserAttributeStream(realm, "unique_identifier", any()))
//                .thenReturn(Stream.empty());
//
//        // Act
//        provider.preprocessFederatedIdentity(session, realm, context);
//
//        // Assert - Should not find existing user and proceed with new user creation
//        assertFalse(context.getContextData().containsKey("EXISTING_USER_FOUND"));
//        verify(userProvider, never()).addFederatedIdentity(any(), any(), any());
//        verify(userProvider, never()).removeFederatedIdentity(any(), any(), any());
//    }
//
//    @Test
//    void testHandleNullAndEmptyNestedValues() throws IOException {
//        // Arrange
//        JsonWebToken idToken = createMockIdToken("test-subject", null);
//        AccessTokenResponse tokenResponse = new AccessTokenResponse();
//        String accessToken = "access-token-123";
//
//        // Create JSON with null and empty values
//        ObjectNode userInfo = objectMapper.createObjectNode();
//        userInfo.put("sub", "test-subject");
//        userInfo.putNull("email");
//        userInfo.put("name", "");
//
//        ObjectNode profile = objectMapper.createObjectNode();
//        profile.putNull("avatar_url");
//        profile.put("bio", "");
//        ArrayNode emptyArray = objectMapper.createArrayNode();
//        profile.set("tags", emptyArray);
//        userInfo.set("profile", profile);
//
//        when(config.isDisableUserInfoService()).thenReturn(false);
//
//        // Act
//        BrokeredIdentityContext result = provider.extractIdentity(tokenResponse, accessToken, idToken);
//
//        // Assert
//        assertNotNull(result);
//        assertEquals("test-subject", result.getId());
//
//        // Verify null and empty values are handled gracefully
//        if (result.getAttributes().containsKey("email")) {
//            List<String> emailValues = result.getAttributes().get("email");
//            assertTrue(emailValues == null || emailValues.isEmpty() || emailValues.get(0) == null);
//        }
//
//        if (result.getAttributes().containsKey("name")) {
//            List<String> nameValues = result.getAttributes().get("name");
//            assertTrue(nameValues == null || nameValues.isEmpty() || nameValues.get(0).isEmpty());
//        }
//    }
//
//    @Test
//    void testMultipleStableIdentifiers() {
//        // Arrange
//        String uinfinValue = "S1234567A";
//        String employeeId = "EMP789";
//        String newSubject = "multi-identifier-subject";
//
//        BrokeredIdentityContext context = createBrokeredIdentityContext(newSubject, null);
//        context.setUserAttribute("uinfin.value", uinfinValue);
//        context.setUserAttribute("employee_id", employeeId);
//
//        // Mock that uinfin.value lookup fails but employee_id succeeds
//        when(userProvider.getUserByEmail(any(), any())).thenReturn(null);
//        when(userProvider.searchForUserByUserAttributeStream(realm, "uinfin.value", uinfinValue))
//                .thenReturn(Stream.empty());
//        when(userProvider.searchForUserByUserAttributeStream(realm, "employee_id", employeeId))
//                .thenReturn(Stream.of(existingUser));
//        when(existingUser.getUsername()).thenReturn("emp-user-789");
//        when(userProvider.getFederatedIdentity(any(), any(), any())).thenReturn(null);
//
//        // Act
//        provider.preprocessFederatedIdentity(session, realm, context);
//
//        // Assert
//        verify(userProvider).addFederatedIdentity(eq(realm), eq(existingUser), any(FederatedIdentityModel.class));
//        assertEquals("emp-user-789", context.getUsername());
//        assertTrue(context.getContextData().containsKey("EXISTING_USER_FOUND"));
//    }
//
//    // Helper methods
//
//    private BrokeredIdentityContext createBrokeredIdentityContext(String subject, String email) {
//        BrokeredIdentityContext context = new BrokeredIdentityContext(subject, config);
//        context.setUsername(subject);
//        if (email != null) {
//            context.setEmail(email);
//        }
//        return context;
//    }
//
//    private JsonWebToken createMockIdToken(String subject, String email) {
//        JsonWebToken token = mock(JsonWebToken.class);
//        when(token.getSubject()).thenReturn(subject);
//
//        Map<String, Object> otherClaims = new HashMap<>();
//        if (email != null) {
//            otherClaims.put("email", email);
//        }
//        otherClaims.put("nonce", "test-nonce");
//
//        when(token.getOtherClaims()).thenReturn(otherClaims);
//        return token;
//    }
}
