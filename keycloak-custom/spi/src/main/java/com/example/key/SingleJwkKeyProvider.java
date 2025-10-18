package com.example.key;

import com.example.utils.KeyUtil;
import org.keycloak.component.ComponentModel;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.keys.KeyProvider;
import org.keycloak.models.KeycloakSession;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class SingleJwkKeyProvider implements KeyProvider {
    private final List<KeyWrapper> keys;

    public SingleJwkKeyProvider(KeycloakSession session, ComponentModel model) {
        this.keys = Optional.ofNullable(KeyUtil.toKeyWrapper(model))
                .map(List::of)
                .orElse(List.of());
    }

    @Override
    public Stream<KeyWrapper> getKeysStream() {
        return this.keys.stream();
    }

    @Override
    public void close() {
        // nothing to close
    }
}
