package net.sharemycode.security;

import org.picketlink.annotations.PicketLink;
import org.picketlink.authentication.BaseAuthenticator;
import org.picketlink.credential.DefaultLoginCredentials;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.credential.Password;
import org.picketlink.idm.model.Account;
import org.picketlink.idm.query.IdentityQuery;
import org.picketlink.idm.credential.Token;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import net.sharemycode.security.model.User;
/**
 * <p>A simple authenticator that supports two credential types: username/password or a simple token.</p>
 */
@RequestScoped
@PicketLink
public class CustomAuthenticator extends BaseAuthenticator {

    @Inject
    private DefaultLoginCredentials credentials;

    @Inject IdentityManager im;
    
    private Account userAccount;
    
    @Override
    public void authenticate() {
        if (this.credentials.getCredential() == null) {
            return;
        }
        if (isUsernamePasswordCredential()) {
            String userId = this.credentials.getUserId();
            Password password = (Password) this.credentials.getCredential();
            userAccount = getUserAccount(userId);
            if (userId.equals("jane") && String.valueOf(password.getValue()).equals("abcd1234")) {
                successfulAuthentication();
            }
        } else if (isCustomCredential()) {
            Token customCredential = (Token) this.credentials.getCredential();

            if (customCredential.getToken() != null && customCredential.getToken().equals("valid_token")) {
                successfulAuthentication();
            }
        }
    }

    private boolean isUsernamePasswordCredential() {
        return Password.class.equals(credentials.getCredential().getClass()) && credentials.getUserId() != null;
    }

    private boolean isCustomCredential() {
        return Token.class.equals(credentials.getCredential().getClass());
    }

    private User getUserAccount(String userId) {
        IdentityQuery<User> q = im.createIdentityQuery(User.class);
        q.setParameter(User.USERNAME, userId);
        return q.getResultList().get(0);
    }

    private void successfulAuthentication() {
        setStatus(AuthenticationStatus.SUCCESS);
        setAccount(userAccount);
    }

}
