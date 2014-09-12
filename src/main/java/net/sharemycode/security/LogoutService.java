package net.sharemycode.security;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import org.picketlink.Identity;

/**
 * <p>Simple logout service.</p>
 */
@Path("/logout")
public class LogoutService {

    @Inject
    private Identity identity;

    @POST
    public void logout() {
        if (this.identity.isLoggedIn()) {
            this.identity.logout();
        }
    }

}
