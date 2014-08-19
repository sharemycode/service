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

import net.sharemycode.service.model.Project;
import net.sharemycode.service.model.User;

/**
 * sharemycode.net ProjectServices
 * 
 * Defines all RESTful services relating to project entities
 * @author Lachlan Archibald
 *
 */

@Path("/project")
public class ProjectServices {
	@GET
	@Path("/list")
	public String displayProjects() {
		return "List of projects";
	}
	
	@GET
	@Path("/randomURL")
	public String returnURL() {
		return Project.generateURL();
	}
	/*
	@POST
	@Path("/create")
	@Consumes("multipart/form-data")
	public Response createProject(@MultipartForm UserRegForm input){
		
	}*/
	
	@POST
	@Path("/create")
	public Response createProject(
			@FormParam("pname") String projectName,
			@FormParam("version") String version,
			@FormParam("description") String description) {
			Project p = new Project();
			p.setName(projectName);
			p.setVersion(version);
			p.setDescription(description);
			return Response.status(200).entity("{" + p.getName() + ", " + p.getVersion() + ", " + p.getDescription() + "}").build();

	}
}
