package net.sharemycode.security;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import net.sharemycode.security.model.User;

import org.picketlink.config.SecurityConfigurationBuilder;
import org.picketlink.event.SecurityConfigurationEvent;

/**
 * Security Configuration
 * 
 * @author Shane Bryzak
 * @author Lachlan Archibald
 *
 */
@ApplicationScoped
public class SecurityConfiguration {

    @SuppressWarnings("unchecked")
    public void initConfig(@Observes SecurityConfigurationEvent event)
    {
       SecurityConfigurationBuilder builder = event.getBuilder();

       // HTTP authentication configured here
       builder
           .identity()
               .stateless()
           .http()
               .forPath("/rest/users/*")
                   //.unprotected() // only for testing!
                   .authenticateWith()
                       .token()
               .forPath("/rest/projects/*")
                   .unprotected() // only for testing!
                   //.authenticateWith()
                   //   .token()
               .forPath("/rest/resources/*")
                   .unprotected() // only for testing!
                   //.authenticateWith()
                   //    .token()
                .forPath("/rest/auth/login")
                    .authenticateWith()
                        .token()
                .forPath("/rest/auth/status")
                    .authenticateWith()
                        .token()
                .forPath("/rest/auth/logout")
                    .logout()
               .forPath("/rest/register")
                   .unprotected();

       // IDM configured here
       builder
           .identity()
               .idmConfig()
                   .named("default")
                   .stores()
                       .jpa()
                           .supportType(User.class)
                           .supportAllFeatures();
    }
}
