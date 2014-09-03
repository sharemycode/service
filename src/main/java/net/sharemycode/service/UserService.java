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

    /*
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<User> listAllUsers() {
        List<User> list = Repository.userRepo;
        System.out.println(list.toString());
        return list;
    }
    */
/*    
    @POST
    public Response registerUser(
            @FormParam("username") String username,
            @FormParam("email") String email,
            @FormParam("password") String password,
            @FormParam("emailc") String emailc,
            @FormParam("passwordc") String passwordc,
            @FormParam("firstName") String firstName,
            @FormParam("lastName") String lastName) {
        if(email.equals(emailc)) {  // email check success
            if(password.equals(passwordc)) {	// password check success
                User u = new User();
                u.setUsername(username);
                u.setFirstName(firstName);
                u.setLastName(lastName);
                u.setEmail(email);
                return Response.status(200).entity("{" + u.getUsername() + ", " + u.getEmail() + ", " + u.getFirstName() + ", " + u.getLastName() + "}").build();
            } else {
                return Response.status(400).entity("Error: Password confirmation does not match").build();
            }
        } else {
            return Response.status(400).entity("Error: Email confirmation does not match").build();
        }
    } 
*/
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response registerUser(Map<String,String> properties) {
    	int result = userController.registerUser(properties);
    	switch(result){
    	case 0:
    		return Response.status(200).entity("Registration successful!").build();
    	case 1:
    		return Response.status(400).entity("Username already exists").build();
    	case 2:
    		return Response.status(400).entity("Email address already exists").build();
    	case 3:
    		return Response.status(400).entity("Email confirmation does not match").build();
    	case 4:
    		return Response.status(400).entity("Password confirmation does not match").build();
    	case -1:
    	default:
    		return Response.status(500).entity("Unknown Server Error").build();
    	}
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
