package net.sharemycode.security;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import net.sharemycode.security.model.User;

import org.picketlink.config.SecurityConfigurationBuilder;
import org.picketlink.event.SecurityConfigurationEvent;

/**
 * Security Configuration
 * 
 * @author shane
 *
 */
@ApplicationScoped
public class SecurityConfiguration {

    @SuppressWarnings("unchecked")
    public void initConfig(@Observes SecurityConfigurationEvent event)
    {
       SecurityConfigurationBuilder builder = event.getBuilder();

       // Http authentication configured here
       builder
         .identity()
             .stateless()
         .http()
             .forPath("/foo/rest/*")
                 .authenticateWith()
                     .token();

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
