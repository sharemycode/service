package net.sharemycode.service;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.Map;

import javax.persistence.NoResultException;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.event.Event;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.ValidationException;
import javax.validation.Validator;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import net.sharemycode.model.Project;
import net.sharemycode.model.ProjectAccess;
import net.sharemycode.model.ProjectResource;
import net.sharemycode.model.Project_;
import net.sharemycode.security.model.User;
import net.sharemycode.Repository;

import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.credential.Password;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
/**
 * sharemycode.net UserService
 * 
 * Defines all RESTful services relating to user entities
 * @author Lachlan Archibald
 *
 */
@Path("/users")
@RequestScoped
public class UserService {
    @Inject
    private EntityManager em;

    @Inject
    private IdentityManager im;
    
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

/*
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<User> listAllUsers() {
        TypedQuery<User> q = em.createQuery("SELECT u FROM User u", User.class);
        return q.getResultList();
    }
*/
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<User> listAllUsers() {
        List<User> list = Repository.userRepo;
        System.out.println(list.toString());
        return list;
    }
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
        if(properties.get("email").equals(properties.get("emailc"))) {	// email check success
            if(properties.get("password").equals(properties.get("passwordc"))) {	// password check success
                User u = new User();
                u.setUsername(properties.get("username"));
                u.setEmail(properties.get("email"));
                u.setFirstName(properties.get("firstName"));
                u.setLastName(properties.get("lastName"));
                Password password = new Password(properties.get("password"));
                Repository.userRepo.add(u);
                //im.add(u);
                //im.updateCredential(u, password);
                return Response.status(200).entity("User \"" + u.getUsername() + "\" created!").build();
            } else {
                return Response.status(400).entity("Error: Password confirmation does not match").build();
            }
        } else {
            return Response.status(400).entity("Error: Email confirmation does not match").build();
        }
    }
    
    @GET
    @Path("/{username:[a-z]*}")
    @Produces(MediaType.APPLICATION_JSON)
    public User lookupMemberByUsername(@PathParam("username") String username) {
        User user = em.find(User.class, username);
        if (user == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        return user;
    }
    @GET
    @Path("/search")
    public List<User> listUsers(@DefaultValue("") @QueryParam("q") String searchTerm)
    {
        TypedQuery<User> q = em.createQuery("SELECT u FROM User u"
                + "WHERE LOWER(u.username) LIKE " + searchTerm + "%", User.class);
        return q.getResultList();
    }
    
}
