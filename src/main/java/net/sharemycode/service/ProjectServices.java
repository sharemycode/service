package net.sharemycode.service;

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.Consumes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import net.sharemycode.service.controller.ProjectController;
import net.sharemycode.service.model.Project;
import net.sharemycode.service.model.ProjectResource;
import net.sharemycode.service.model.ProjectResource.ResourceType;
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
	
	@Inject ProjectController projectController;
	
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
			//projectController.createProject(p);
			return Response.status(200).entity("{" + p.getName() + ", " + p.getVersion() + ", " + p.getDescription() + "}").build();

	}
	
	 @GET
	   @Path("/list{searchTerm:(/[^/]+?)?}")
	   @Produces(MediaType.APPLICATION_JSON)
	   public List<Project> listProjects(@PathParam("searchTerm") String searchTerm)
	   {
	      if (searchTerm.startsWith("/"))
	      {
	         searchTerm = searchTerm.substring(1);
	      }
	      return projectController.listProjects(searchTerm);
	   }

	   @POST
	   @Path("/newfolder")
	   @Consumes("application/json")
	   @Produces(MediaType.APPLICATION_JSON)
	   public ProjectResource[] createProjectFolder(Map<String,String> properties)
	   {
	      String[] parts = properties.get("name").split("/");
	      List<ProjectResource> resources = new ArrayList<ProjectResource>();

	      ProjectResource parent = null;

	      if (properties.containsKey("parentResourceId"))
	      {
	         Long resourceId = Long.valueOf(properties.get("parentResourceId"));
	         parent = projectController.lookupResource(resourceId);
	      }

	      for (String part : parts) 
	      {
	         ProjectResource r = new ProjectResource();
	         r.setName(part);
	         r.setResourceType(ResourceType.DIRECTORY);
	         r.setParent(parent);

	         if (properties.containsKey("projectId"))
	         {
	            Long projectId = Long.valueOf(properties.get("projectId"));
	            r.setProject(projectController.lookupProject(projectId));
	         }

	         projectController.createResource(r);
	         parent = r;
	      }

	      return resources.toArray(new ProjectResource[resources.size()]);
	   }

	   @POST
	   @Path("/newclass")
	   @Consumes("application/json")
	   @Produces(MediaType.APPLICATION_JSON)
	   public ProjectResource createProjectClass(Map<String,String> properties)
	   {
	      ProjectResource r = new ProjectResource();
	      r.setName(properties.get("name") + ".java");
	      r.setResourceType(ResourceType.FILE);

	      Project p = projectController.lookupProject(Long.valueOf(properties.get("projectId")));
	      r.setProject(p);

	      String folder = properties.get("folder");
	      String pkg = properties.get("package");

	      ProjectResource parentDir = projectController.createDirStructure(p, folder);
	      ProjectResource parentPkg = projectController.createPackage(p, parentDir, pkg);
	      r.setParent(parentPkg);

	      projectController.createResource(r);
	      return r;
	   }
}
