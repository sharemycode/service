package net.sharemycode.service;

	import java.util.List;
import java.util.logging.Logger;
import java.util.Map;

	import javax.persistence.NoResultException;
import javax.persistence.EntityManager;
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

	import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import net.sharemycode.service.model.User;
//import net.sharemycode.service.data.UserRepository;

	/**
	 * sharemycode.net UserServices
	 * 
	 * Defines all RESTful services relating to user entities
	 * @author Lachlan Archibald
	 *
	 */
	@Path("/user")
	//@RequestScoped
	public class UserServices {

		/*@GET
		@Path("/{pathParameter}")
		public Response responseMsg( @PathParam("pathParameter") String pathParameter,
	    		@DefaultValue("No response") @QueryParam("q") String queryParameter) {
			
			String response = "Hello from: " + pathParameter + ": " + queryParameter;
			return Response.status(200).entity(response).build();
		}*/
		
		//@Inject
	   // private Validator validator;

	   // @Inject
	   // private UserRepository repository;
	    
	    //@Inject
	    //private Logger log;

	  //  @Inject
	   // private EntityManager em;

	  //  @Inject
	  //  private Event<User> userEventSrc;
/*
	    @GET
	    @Produces(MediaType.APPLICATION_JSON)
	    public List<User> listAllMembers() {
	        return repository.findAllOrderedByName();
	    }*/
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
		
		@GET
		@Path("/list")
		public String displayUsers() {
			return "List of users";
		}
		
		
		@POST
		@Path("/create")
		public Response registerUser(
				@FormParam("username") String username,
				@FormParam("email") String email,
				@FormParam("password") String password,
				@FormParam("emailc") String emailc,
				@FormParam("passwordc") String passwordc,
				@FormParam("gname") String givenName,
				@FormParam("sname") String surname) {
			
			if(email.equals(emailc)) {	// email check success
				if(password.equals(passwordc)) {	// password check success
					User u = new User();
					u.setUsername(username);
					u.setGivenName(givenName);
					u.setSurname(surname);
					u.setEmail(email);
					return Response.status(200).entity("{" + u.getUsername() + ", " + u.getEmail() + ", " + u.getGivenName() + ", " + u.getSurname() + "}").build();
				} else {
					return Response.status(400).entity("Error: Password confirmation does not match").build();
				}
			} else {
				return Response.status(400).entity("Error: Email confirmation does not match").build();
			}
		} 
	
		@POST
		@Path("/create")
		@Consumes(MediaType.APPLICATION_JSON)
		public Response registerUser(Map<String,String> properties) {
			if(properties.get("email").equals(properties.get("emailc"))) {	// email check success
				if(properties.get("password").equals(properties.get("passwordc"))) {	// password check success
					User u = new User();
					u.setUsername(properties.get("username"));
					u.setEmail(properties.get("email"));
					u.setGivenName(properties.get("gname"));
					u.setSurname(properties.get("sname"));
					//u.setPassword(properties.get("password"));
					return Response.status(200).entity("{" + u.getUsername() + ", " + u.getEmail() + ", " + u.getGivenName() + ", " + u.getSurname() + "}").build();
				} else {
					return Response.status(400).entity("Error: Password confirmation does not match").build();
				}
			} else {
				return Response.status(400).entity("Error: Email confirmation does not match").build();
			}
		}
		/*
		 @GET
	    @Path("/{id:[0-9][0-9]*}")
	    @Produces(MediaType.APPLICATION_JSON)
	    public User lookupMemberById(@PathParam("id") long id) {
	        User user = repository.findById(id);
	        if (user == null) {
	            throw new WebApplicationException(Response.Status.NOT_FOUND);
	        }
	        return user;
	    }
		*/
}
