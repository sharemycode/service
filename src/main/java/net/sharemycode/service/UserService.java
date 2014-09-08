package net.sharemycode.service;

import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import net.sharemycode.Repository;
import net.sharemycode.controller.UserController;
import net.sharemycode.security.model.User;

import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.credential.Password;
import org.picketlink.idm.query.IdentityQuery;
/**
 * sharemycode.net UserService
 * 
 * Defines all RESTful services relating to user entities
 * @author Lachlan Archibald
 *
 */
@Path("/users")
@Stateless
public class UserService {
    
    @Inject UserController userController;
    
    /* Return full list of users */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<User> listAllUsers() {
    	System.out.println("ListUsersREST");
        return userController.listAllUsers();
    }
    
    /* return specific user by username */
    @GET
    @Path("/{username:[a-z0-9]*}")
    @Produces(MediaType.APPLICATION_JSON)
    public User lookupUserByUsername(@PathParam("username") String username) {
        User user = userController.lookupUserByUsername(username);
        if (user == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        return user;
    }
    
    /* Find user by Email */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public User listUsersByEmail(@QueryParam("email") String email) {
    	return userController.lookupUserByEmail(email);
    }
    
    // TODO user login
    @GET
    @Path("/login")
    public String userLogin() {
        return "Login - Coming soon";
    }

    // TODO user logout
    @GET
    @Path("/logout")
    public String userLogout() {
        return "Logout - Coming soon";
    }
    
}
