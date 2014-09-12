package net.sharemycode.security;

import javax.enterprise.context.ApplicationScoped;

import org.apache.deltaspike.security.api.authorization.Secures;
import net.sharemycode.security.annotations.LoggedIn;
import org.picketlink.Identity;

/**
 * Provides authorisation logic for typesafe security bindings
 *
 * @author Shane Bryzak
 */
@ApplicationScoped
public class CustomAuthorizer
{

   @Secures @LoggedIn
   public boolean checkLoggedIn(Identity identity)
   {
      return identity.isLoggedIn();
   }
}
