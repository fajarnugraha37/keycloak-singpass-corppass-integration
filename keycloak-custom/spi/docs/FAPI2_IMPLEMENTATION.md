# FAPI 2.0 Implementation for Singpass Integration

## Overview

This document describes the implementation of FAPI 2.0 (Financial-grade API 2.0) compliance for Singpass authentication in Keycloak. The implementation follows the specifications outlined in [Singpass FAPI 2.0 Authentication API Changelog](https://docs.developer.singpass.gov.sg/docs/upcoming-changes/fapi-2.0-authentication-api/changelog).

## Key Changes from FAPI 1.0 to FAPI 2.0

### 1. **DPoP (Demonstrating Proof-of-Possession)**
- Binds access tokens to specific clients using cryptographic proof
- Prevents token theft and replay attacks
- Implemented via `DPoPUtil` class

### 2. **PAR (Pushed Authorization Requests)**
- Authorization parameters are pushed directly to the server before user redirection
- Reduces exposure of sensitive parameters in browser URLs
- Implemented via `PARUtil` class

### 3. **JARM (JWT-Secured Authorization Response Mode)**
- Authorization responses are returned as signed JWTs
- Provides integrity and authenticity of authorization responses
- Implemented in `CustomFAPI2Endpoint.handleJARMResponse()`

### 4. **Enhanced PKCE Requirements**
- Only S256 code challenge method is allowed (plain is deprecated)
- Mandatory for all authorization flows
- Automatically enforced in `createAuthorizationUrl()`

### 5. **Stricter Security Requirements**
- Stronger nonce requirements (32 bytes minimum)
- Mandatory state parameter validation
- Enhanced token validation

## Implementation Details

### New Utility Classes

#### 1. `DPoPUtil.java`
Handles DPoP proof generation for FAPI 2.0 compliance.

**Key Methods:**
- `generateDPoPProof()`: Creates DPoP JWT with required claims
- `generateAccessTokenHash()`: Creates SHA-256 hash for access token binding
- `generateNonce()`: Generates cryptographically secure nonces

**DPoP JWT Structure:**
```json
{
  "typ": "dpop+jwt",
  "alg": "ES256",
  "jwk": {
    "kty": "EC",
    "crv": "P-256",
    "x": "...",
    "y": "..."
  }
}
{
  "jti": "uuid",
  "htm": "POST",
  "htu": "https://token.endpoint",
  "iat": 1234567890,
  "ath": "base64url_encoded_hash" // Optional, for bound access tokens
}
```

#### 2. `PARUtil.java`
Manages Pushed Authorization Requests.

**Key Methods:**
- `pushAuthorizationRequest()`: Sends authorization parameters to PAR endpoint
- `isValidPAREndpoint()`: Validates PAR endpoint URLs

**PAR Flow:**
1. Client pushes all authorization parameters to PAR endpoint
2. Server validates and stores parameters
3. Server returns `request_uri` with limited lifetime
4. Client uses `request_uri` in authorization request

### Updated Classes

#### 1. `CustomOIDCIdentityProviderConfig.java`
Added FAPI 2.0 configuration options:

| Configuration | Type | Description |
|--------------|------|-------------|
| `fapi2Mode` | boolean | Enables FAPI 2.0 mode (enforces all requirements) |
| `dpopEnabled` | boolean | Enables DPoP for proof-of-possession |
| `dpopSigningKeyId` | String | Key ID for DPoP signing |
| `parEnabled` | boolean | Enables Pushed Authorization Requests |
| `parEndpoint` | String | PAR endpoint URL |
| `jarmEnabled` | boolean | Enables JWT-Secured Authorization Response Mode |
| `jarmResponseMode` | String | JARM response mode (jwt, query.jwt, fragment.jwt, form_post.jwt) |
| `senderConstrainedTokens` | boolean | Enables sender-constrained access tokens |
| `dpopNonce` | String | DPoP nonce for replay prevention |

#### 2. `CustomFAPIProvider.java`
Enhanced with FAPI 2.0 support:

**Key Enhancements:**

##### a) Authorization URL Creation (`createAuthorizationUrl`)
- Enforces S256 PKCE method
- Adds JARM response_mode parameter
- Implements PAR flow when enabled
- Generates 32-byte nonces for enhanced security

##### b) Token Request Authentication (`authenticateTokenRequest`)
- Adds DPoP header to token requests
- Maintains existing JWT-based client authentication
- Supports both private_key_jwt and client_secret authentication

##### c) Access Token Processing (`processAccessTokenResponse`)
- Validates DPoP token type
- Verifies token binding
- Enhanced JWT token validation

##### d) Custom Endpoint (`CustomFAPI2Endpoint`)
- Handles JARM responses
- Validates JWT-secured authorization responses
- Enhanced error handling and logging

### Configuration Example

To enable FAPI 2.0 for Singpass in Keycloak:

```json
{
  "alias": "singpass-fapi2",
  "providerId": "custom-oidc",
  "enabled": true,
  "config": {
    "fapi2Mode": "true",
    "dpopEnabled": "true",
    "dpopSigningKeyId": "your-signing-key-id",
    "parEnabled": "true",
    "parEndpoint": "https://api.singpass.gov.sg/oauth/par",
    "jarmEnabled": "true",
    "jarmResponseMode": "jwt",
    "authorizationUrl": "https://api.singpass.gov.sg/oauth/authorize",
    "tokenUrl": "https://api.singpass.gov.sg/oauth/token",
    "userInfoUrl": "https://api.singpass.gov.sg/oauth/userinfo",
    "clientId": "your-client-id",
    "clientAuthMethod": "private_key_jwt",
    "signingKeyId": "your-signing-key-id",
    "pkceEnabled": "true",
    "pkceMethod": "S256",
    "defaultScope": "openid"
  }
}
```

## Security Considerations

### 1. Key Management
- Use strong EC keys (P-256, P-384, or P-521)
- Rotate keys regularly
- Store private keys securely (use Keycloak vault)
- Separate keys for signing and encryption

### 2. DPoP Keys
- DPoP keys should be unique per client
- Keys should be rotated periodically
- Never expose private keys in logs or error messages

### 3. PAR Endpoint
- Must use HTTPS
- Validate server certificates
- Handle PAR errors gracefully
- Respect request_uri expiration times

### 4. JARM Validation
- Always validate JWT signatures
- Verify issuer and audience claims
- Check state parameter matches
- Validate expiration times

## Testing Checklist

- [ ] PKCE with S256 method works correctly
- [ ] DPoP proofs are generated and validated
- [ ] PAR requests succeed and return valid request_uri
- [ ] JARM responses are parsed and validated
- [ ] Token binding works with DPoP
- [ ] Error handling works for all flows
- [ ] Logging is comprehensive but doesn't expose secrets
- [ ] All FAPI 2.0 security requirements are met

## Migration from FAPI 1.0

### Step 1: Update Configuration
1. Set `fapi2Mode` to `true`
2. Configure DPoP settings if required
3. Add PAR endpoint URL
4. Enable JARM if supported by Singpass

### Step 2: Key Configuration
1. Ensure you have EC keys configured in Keycloak
2. Configure `dpopSigningKeyId` 
3. Test key access and signing operations

### Step 3: Test Integration
1. Test authorization flow with PAR
2. Verify DPoP token generation
3. Test JARM response handling
4. Validate end-to-end flow

### Step 4: Monitor and Validate
1. Check logs for FAPI 2.0 specific messages
2. Validate all security requirements are met
3. Test error scenarios
4. Performance testing

## Troubleshooting

### Common Issues

#### 1. "DPoP signing key not found"
**Solution:** Configure `dpopSigningKeyId` or `signingKeyId` in the identity provider config.

#### 2. "Invalid PAR endpoint"
**Solution:** Ensure PAR endpoint URL starts with `https://` and is correct.

#### 3. "State mismatch in JARM response"
**Solution:** Check session management and ensure state parameter is properly stored and retrieved.

#### 4. "Expected DPoP token type but got Bearer"
**Solution:** Singpass server may not support DPoP yet. Set `dpopEnabled` to `false` temporarily.

## Compliance Matrix

| FAPI 2.0 Requirement | Status | Implementation |
|---------------------|--------|----------------|
| PKCE with S256 | ✅ | Enforced in `createAuthorizationUrl()` |
| PAR Support | ✅ | `PARUtil` + `performPushedAuthorizationRequest()` |
| DPoP Support | ✅ | `DPoPUtil` + `generateDPoPProof()` |
| JARM Support | ✅ | `CustomFAPI2Endpoint.handleJARMResponse()` |
| Enhanced Nonce | ✅ | 32-byte nonces in `createAuthorizationUrl()` |
| State Validation | ✅ | Validated in all endpoints |
| JWT Client Auth | ✅ | `authenticateTokenRequest()` |
| Token Binding | ✅ | `processAccessTokenResponse()` |

## References

1. [Singpass FAPI 2.0 Changelog](https://docs.developer.singpass.gov.sg/docs/upcoming-changes/fapi-2.0-authentication-api/changelog)
2. [FAPI 2.0 Security Profile](https://openid.net/specs/fapi-2_0-security-profile.html)
3. [RFC 9449 - DPoP](https://datatracker.ietf.org/doc/html/rfc9449)
4. [RFC 9126 - PAR](https://datatracker.ietf.org/doc/html/rfc9126)
5. [JARM Specification](https://openid.net/specs/oauth-v2-jarm.html)

## Support

For issues or questions:
1. Check Keycloak logs for detailed error messages
2. Verify Singpass API documentation for latest changes
3. Review this documentation for configuration examples
4. Check compile errors and warnings in the IDE

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2025-01-11 | Initial FAPI 2.0 implementation |

