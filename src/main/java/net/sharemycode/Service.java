package net.sharemycode;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

/**
 * sharemycode RESTEasy Web Service
 *
 */
@Path("")
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
	@Path("/projects")
	public String displayProjects() {
		return "List of projects";
	}
}
