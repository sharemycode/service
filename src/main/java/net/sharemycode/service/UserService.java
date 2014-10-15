package net.sharemycode.service;

import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import net.sharemycode.controller.UserController;
import net.sharemycode.model.UserProfile;
import net.sharemycode.security.model.User;

/**
 * sharemycode.net UserService
 * 
 * Defines all RESTful services relating to user entities
 * 
 * @author Lachlan Archibald
 * 
 */
@Path("/users")
@Stateless
public class UserService {

    @Inject
    UserController userController;

    @GET
    @Path("/logout")
    @Produces(MediaType.TEXT_PLAIN)
    public Response logout() {
        int result = userController.logout();
        if (result == 200)
            return Response.ok().build();
        else
            return Response.status(result).build();
    }

    /* Return full list of users */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<User> listUsers() {
        System.out.println("ListUsersREST");
        return userController.listAllUsers();
    }

    /* return specific user by username */
    @GET
    @Path("/{username:[a-zA-Z0-9]*}")
    @Produces(MediaType.APPLICATION_JSON)
    public User lookupUserByUsername(@PathParam("username") String username) {
        User user = userController.lookupUserByUsername(username);
        if (user == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        return user;
    }

    /* Find user */
    @GET
    @Path("/search")
    @Produces(MediaType.APPLICATION_JSON)
    public User findUser(@QueryParam("username") String username,
            @QueryParam("email") String email) {
        // find a user based on username or email, must be exact
        User user = null;
        if (username.length() > 0) {
            user = userController.lookupUserByUsername(username);
            if (user != null)
                return user;
        }
        if (email.length() > 0) {
            user = userController.lookupUserByEmail(email);
            if (user != null)
                return user;
        }
        // no results
        return null;
    }

    @GET
    @Path("/{username:[a-zA-Z0-9]*}/profile")
    // return user profile
    @Produces(MediaType.APPLICATION_JSON)
    public UserProfile getUserProfile(@PathParam("username") String username) {
        return userController.lookupUserProfile(username);
    }

    @PUT
    @Path("/{username:[a-zA-Z0-9]*}/profile")
    // update user profile
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateUserProfile(@PathParam("username") String username,
            UserProfile profile) {
        User u = userController.lookupUserByUsername(username);
        if (u == null)
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        UserProfile updated = userController.updateUserProfile(u, profile);
        if (updated == null)
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        else
            return Response.ok().entity(updated).build();
    }

    @PUT
    @Path("/{username:[a-zA-Z0-9]*}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateUserAccount(@PathParam("username") String username,
            Map<String, String> properties) {
        User u = userController.lookupUserByUsername(username);
        if (u == null)
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        User updated = userController.updateUserAccount(u,
                properties.get("username"), properties.get("email"),
                properties.get("emailc"), properties.get("password"),
                properties.get("passwordc"), properties.get("firstName"),
                properties.get("lastName"));
        if (updated == null)
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        return Response.ok().entity(updated).build();
    }
}
