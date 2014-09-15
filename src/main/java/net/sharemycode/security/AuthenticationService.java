package net.sharemycode.security;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.picketlink.Identity;
import org.picketlink.credential.DefaultLoginCredentials;
import org.picketlink.idm.credential.TokenCredential;
import org.picketlink.idm.model.Account;

import net.sharemycode.security.jws.JWSToken;
import net.sharemycode.security.jws.JWSTokenProvider;

@Path("/authenticate")
public class AuthenticationService {

    public static final String USERNAME_PASSWORD_CREDENTIAL_CONTENT_TYPE = "application/x-authc-username-password+json";
    public static final String TOKEN_CONTENT_CREDENTIAL_TYPE = "application/x-authc-token";

    @Inject
    private Identity identity;

    @Inject
    private JWSTokenProvider tokenProvider;
    
    @Inject
    private DefaultLoginCredentials credentials;

    @GET
    public Response getToken() {
    	Account account = this.identity.getAccount();
        // issue JWS token to account
        JWSToken token = tokenProvider.issue(account);
        return Response.ok().entity(token).build();
    }
/*    @POST
    @Consumes({USERNAME_PASSWORD_CREDENTIAL_CONTENT_TYPE})
    @Produces({TOKEN_CONTENT_CREDENTIAL_TYPE})
    public Response authenticate(DefaultLoginCredentials credential) {
        if (!this.identity.isLoggedIn()) {
            this.credentials.setUserId(credential.getUserId());
            this.credentials.setPassword(credential.getPassword());
            this.identity.login();
        }

        Account account = this.identity.getAccount();
        // issue JWS token to account
        JWSToken token = tokenProvider.issue(account);
        return Response.ok().entity(token).build();
    }
    
    @POST
    @Consumes({TOKEN_CONTENT_CREDENTIAL_TYPE})
    public Response authenticate(String token) {
        if (!this.identity.isLoggedIn()) {
            //SimpleTokenCredential credential = new SimpleTokenCredential(token);
        	TokenCredential credential = new TokenCredential(token);
            this.credentials.setCredential(credential);

            this.identity.login();
        }

        Account account = this.identity.getAccount();

        return Response.ok().entity(account).type(MediaType.APPLICATION_JSON_TYPE).build();
    }
*/
    @POST
    @Consumes({ "*/*" })
    public Response unsupportedCredentialType() {
        return Response.status(Response.Status.UNSUPPORTED_MEDIA_TYPE).build();
    }
}
