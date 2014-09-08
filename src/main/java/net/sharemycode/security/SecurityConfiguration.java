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
                   .authenticateWith()
                       .token()
               .forPath("/rest/projects/*")
                   .authenticateWith()
                       .token()
               .forPath("/rest/resources/*")
                   .authenticateWith()
                       .token()
               .forPath("/rest/authenticate")   // HTTP Basic used for initial login only.
                   .authenticateWith()
                       .basic()
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
