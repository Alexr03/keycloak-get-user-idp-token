package org.keycloak.getuseridptoken;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;

public class UserTokenEndpointProvider implements RealmResourceProvider {

    private final KeycloakSession session;

    public UserTokenEndpointProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Object getResource() {
        return new UserTokenEndpointResource(session);
    }

    @Override
    public void close() {
    }
}
