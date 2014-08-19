package net.sharemycode.service;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.Consumes;

import java.util.Map;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

//import net.sharemycode.service.model.Project;
//import net.sharemycode.service.model.User;

/**
 * sharemycode RESTEasy Web Service
 *
 */
@Path("/old")
public class Service {
	/*@GET
	@Path("/{pathParameter}")
	public Response responseMsg( @PathParam("pathParameter") String pathParameter,
    		@DefaultValue("No response") @QueryParam("q") String queryParameter) {
		
		String response = "Hello from: " + pathParameter + ": " + queryParameter;
		return Response.status(200).entity(response).build();
	}*/
	
	@GET
	@Path("/register")
	public String userRegister() {
		return "Let's add a new User";
	}
	
	@GET
	@Path("/login")
	public String userLogin() {
		return "Login - Coming soon";
	}
	
	@GET
	@Path("/users")
	public String displayUsers() {
		return "List of users";
	}
	
	@GET
	@Path("/projects")
	public String displayProjects() {
		return "List of projects";
	}
/*	
	@GET
	@Path("/project/randomURL")
	public String returnURL() {
		return Project.generateURL();
	}*/
	/*
	@POST
	@Path("/project/create")
	@Consumes("multipart/form-data")
	public Response createProject(@MultipartForm UserRegForm input){
		
	}*/
	
/*	
	@POST
	@Path("/user/create")
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
	}*/
/*
	@POST
	@Path("/user/create")
	@Consumes("application/json")
	//@Produces(MediaType.APPLICATION_JSON)
	@Produces("text/plain")
	public Response registerUser(Map<String,String> properties) {
		if(properties.get("email").equals(properties.get("emailc"))) {	// email check success
			if(properties.get("password").equals(properties.get("passwordc"))) {	// password check success
				User u = new User();
				u.setUsername(properties.get("username"));
				u.setEmail(properties.get("email"));
				u.setGivenName(properties.get("gname"));
				u.setSurname(properties.get("sname"));
				System.out.println("{" + u.getUsername() + ", " + u.getEmail() + ", " + u.getGivenName() + ", " + u.getSurname() + "}");
				//u.setPassword(properties.get("password"));
				return Response.status(200).entity("{" + u.getUsername() + ", " + u.getEmail() + ", " + u.getGivenName() + ", " + u.getSurname() + "}").build();
			} else {
				return Response.status(400).entity("Error: Password confirmation does not match").build();
			}
		} else {
			return Response.status(400).entity("Error: Email confirmation does not match").build();
		}
	}*/
/*	@POST
	@Path("/project/create")
	public Response createProject(
			@FormParam("pname") String projectName,
			@FormParam("version") String version,
			@FormParam("description") String description) {
			Project p = new Project();
			p.setName(projectName);
			p.setVersion(version);
			p.setDescription(description);
			return Response.status(200).entity("{" + p.getName() + ", " + p.getVersion() + ", " + p.getDescription() + "}").build();

	}*/
}