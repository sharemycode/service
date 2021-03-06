package net.sharemycode.service;

import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import net.sharemycode.controller.UserController;

/**
 * REST endpoint used to register a new User account (unprotected)
 * 
 * @author Lachlan Archibald
 *
 */
@Path("/register")
@Stateless
public class RegistrationService {

    @Inject
    UserController userController;

    /**
     * Registers a new User account
     * 
     * @param properties JSON registration data
     * @return Response.ok(), or Response.accepted() with error
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response registerUser(Map<String, String> properties) {
        System.out.println("registerService");
        int result = userController.registerUser(properties);
        switch (result) {
        case 0:
            return Response.status(200).entity("Registration successful!")
                    .build();
        case 1:
            return Response.status(202).entity("Username already exists")
                    .build();
        case 2:
            return Response.status(202).entity("Email address already exists")
                    .build();
        case 3:
            return Response.status(202)
                    .entity("Email confirmation does not match").build();
        case 4:
            return Response.status(202)
                    .entity("Password confirmation does not match").build();
        case -1:
        default:
            return Response.status(500).entity("Unknown Server Error").build();
        }
    }
}
