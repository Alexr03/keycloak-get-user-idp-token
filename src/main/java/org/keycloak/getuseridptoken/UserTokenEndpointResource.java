package org.keycloak.getuseridptoken;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.broker.provider.IdentityProviderFactory;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.models.*;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;

import java.util.Objects;
import java.util.stream.Stream;

public class UserTokenEndpointResource {

    private static final String RoleName = "read-users-idp-tokens";
    private final KeycloakSession session;
    private final AuthenticationManager.AuthResult auth;

    public UserTokenEndpointResource(KeycloakSession session) {
        this.session = session;
        this.auth = new AppAuthManager.BearerTokenAuthenticator(session).authenticate();
    }

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUserToken(
            @QueryParam("userId") String userId,
            @QueryParam("idpName") String idpName) {

        checkAccess();

//        // Check if the requester has the specific role
//        UserModel requester = session.getContext().getAuthenticationSession().getAuthenticatedUser();
//        if (!requester.hasRole(session.getContext().getRealm().getRole(RoleName))) {
//            return Response.status(Response.Status.FORBIDDEN).build();
//        }

        RealmModel realm = session.getContext().getRealm();
        UserModel user = session.users().getUserById(realm, userId);
        if (user == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        IdentityProviderModel idpModel = realm.getIdentityProvidersStream()
                .filter(idp -> idp.getAlias().equals(idpName))
                .findFirst()
                .orElse(null);
        
        if (idpModel == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        if(!idpModel.isStoreToken()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("IDP does not store tokens").build();
        }

        // Fetch the token from the IDP (details depend on your IDP implementation)
        // Here you need to interact with the IDP to get the token

        return fetchTokenForUserFromIdp(user, idpModel);
    }

    public static IdentityProviderFactory<?> getIdentityProviderFactory(KeycloakSession session, IdentityProviderModel model) {
        return Stream.concat(session.getKeycloakSessionFactory().getProviderFactoriesStream(IdentityProvider.class),
                        session.getKeycloakSessionFactory().getProviderFactoriesStream(SocialIdentityProvider.class))
                .filter(providerFactory -> Objects.equals(providerFactory.getId(), model.getProviderId()))
                .map(IdentityProviderFactory.class::cast)
                .findFirst()
                .orElse(null);
    }

    public static IdentityProvider<?> getIdentityProvider(KeycloakSession session, RealmModel realm, String alias) {
        IdentityProviderModel identityProviderModel = realm.getIdentityProviderByAlias(alias);

        if (identityProviderModel != null) {
            IdentityProviderFactory<?> providerFactory = getIdentityProviderFactory(session, identityProviderModel);

            if (providerFactory == null) {
                throw new IdentityBrokerException("Could not find factory for identity provider [" + alias + "].");
            }

            return providerFactory.create(session, identityProviderModel);
        }

        throw new IdentityBrokerException("Identity Provider [" + alias + "] not found.");
    }

    private Response fetchTokenForUserFromIdp(UserModel user, IdentityProviderModel idpModel) {
        // Implement your logic to fetch the token from the IDP here
        var identityProvider = getIdentityProvider(session, session.getContext().getRealm(), idpModel.getAlias());
        FederatedIdentityModel identity = this.session.users().getFederatedIdentity(session.getContext().getRealm(), user, idpModel.getAlias());
        return identityProvider.retrieveToken(session, identity);
    }

    private void checkAccess() {
        if (this.auth == null || this.auth.getToken() == null) {
            throw new NotAuthorizedException("Bearer");
        } else if (auth.getToken().getRealmAccess() == null) {
            throw new ForbiddenException("Does not have permission to fetch users");
        }
        
        var user = session.users().getUserById(session.getContext().getRealm(), auth.getToken().getSubject());
        var role = session.getContext().getRealm().getRole(RoleName);
        
        if (user == null || !user.hasRole(role)) {
            throw new ForbiddenException("Does not have permission to fetch idp tokens for users");
        }
    }
}
