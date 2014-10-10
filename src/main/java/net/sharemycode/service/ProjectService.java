package net.sharemycode.service;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import net.sharemycode.controller.ProjectController;
import net.sharemycode.controller.ResourceController;
import net.sharemycode.model.Project;
import net.sharemycode.model.ProjectAccess;
import net.sharemycode.model.ProjectResource;
import net.sharemycode.model.ProjectResource.ResourceType;

/**
 * sharemycode.net ProjectService
 * 
 * Defines all RESTful services relating to project entities
 * @author Lachlan Archibald
 *
 */

@Path("/projects")
@Stateless
public class ProjectService {
    

    @Inject ProjectController projectController;
    @Inject ResourceController resourceController;

    @GET
    @Path("/randomURL")
    public String returnURL() {
        return Project.generateURL();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Project> listAllProjects() {
        // list of all user's projects
        return projectController.listAllProjects();
    }
    
    @GET
    @Path("/shared")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Project> listSharedProjects() {
        return projectController.listSharedProjects();
    }
    
    
    @GET
    @Path("/{id:[0-9a-z]*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Project lookupProjectById(@PathParam("id") String id) {
        // return specific project information
    	Project project = projectController.lookupProject(id);
    	if(project == null) {
    		throw new WebApplicationException(Response.Status.NOT_FOUND);
    	}
    	return project;
    }
    
    @GET
    @Path("/{id:[0-9a-z]*}/download")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response fetchProject(@PathParam("id") String id) {
        Project p = projectController.lookupProject(id);
        final byte[] data = projectController.fetchProject(p);
        return Response.ok(new StreamingOutput() {
            public void write(OutputStream output) throws IOException, WebApplicationException {
                output.write(data);
            }
        }).header("Content-Disposition", "attachment; filename=\"" + p.getName() + "_" + p.getVersion() + ".zip" + "\"").build();
    }
    
    @GET
    @Path("/{projectid:[0-9a-z]*}/resources")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ProjectResource> listProjectResources(@PathParam("projectid") String projectid) {
        // List all resources associated with a project.
    	Project project = projectController.lookupProject(projectid);
    	if(project == null) {
    		throw new WebApplicationException(Response.Status.NOT_FOUND);
    	}
    	List<ProjectResource> resources = resourceController.listResources(project);
    	return resources;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadProject(Map<String,Object> properties) throws URISyntaxException {
        // create new project, accepts JSON with project name, version, description, attachmentIDs
        Project newProject = projectController.submitProject(properties);
        if(newProject == null) {
        	return Response.status(400).entity("Failed to create project").build();
        }
        //String output = newProject.getUrl();
        return Response.created(new URI("/projects/" + newProject.getId())).entity(newProject).build();
    }

    @POST
    @Path("/attachments/{filename}")
    public Response uploadAttachment(@PathParam("filename") String name, String data) {
        Long attachment = projectController.createAttachmentFromService(name, data);
        if (attachment > -1L)
            return Response.ok().entity(attachment.toString()).build();
        else
            return Response.noContent().build();
    }
    @GET
    @Path("/list{searchTerm:(/[^/]+?)?}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Project> listProjects(@PathParam("searchTerm") String searchTerm)
    // returns list of projects that match search term.
    //TODO edit to use QueryParam?
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
    // create a new directory resource under provided parent resource
    {
        String[] parts = properties.get("name").split("/");
        List<ProjectResource> resources = new ArrayList<ProjectResource>();

        ProjectResource parent = null;

        if (properties.containsKey("parentResourceId"))
        {
            Long resourceId = Long.valueOf(properties.get("parentResourceId"));
            parent = resourceController.lookupResource(resourceId);
        }

        for (String part : parts) 
        {
            ProjectResource r = new ProjectResource();
            r.setName(part);
            r.setResourceType(ResourceType.DIRECTORY);
            r.setParent(parent);

            if (properties.containsKey("projectId"))
            {
                String projectId = properties.get("projectId");
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
    // create new Java Class?
    //TODO Re-evaluate this method
    {
        ProjectResource r = new ProjectResource();
        r.setName(properties.get("name") + ".java");
        r.setResourceType(ResourceType.FILE);

        Project p = projectController.lookupProject(properties.get("projectId"));
        r.setProject(p);

        String folder = properties.get("folder");
        String pkg = properties.get("package");

        ProjectResource parentDir = projectController.createDirStructure(p, folder);
        ProjectResource parentPkg = projectController.createPackage(p, parentDir, pkg);
        r.setParent(parentPkg);

        projectController.createResource(r);
        return r;
    }

    @GET
    @Path("/user/{username:[0-9a-z]*}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Project> listProjectsByOwner(@PathParam("username") String username) {
        // list projects by owner
        // this may not be necessary, or may need to use a QueryParam if for general searching
    	List<Project> projects = projectController.listProjectsByOwner(username);
    	return projects;
    }

    @PUT
    @Path("/{id:[0-9a-z]*}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateProject(@PathParam("id") String id, Project update) {
        Project result = projectController.updateProject(id, update);
        if(result != null)
            return Response.ok().entity(result).build();
        else
            return Response.notModified().build();
    }
    
    @PUT
    @Path("/{id:[0-9a-z]*}/owner")
    @Produces(MediaType.APPLICATION_JSON)
    public Response changeProjectOwner(@PathParam("id") String id, String username) {
        Project p = projectController.changeProjectOwner(id, username);
        if (p != null)
            return Response.ok().entity("Project owner updated").build();
        else
            return Response.notModified().entity("Project or userId not found").build();
    }
    
    @DELETE
    @Path("/{id:[0-9a-z]*}")
    public Response deleteProject(@PathParam("id") String id) {
        // Delete project resource, and all associated resources
        int result = projectController.deleteProject(id);
        switch (result) {
        case 200:
            return Response.ok().entity("Project deleted successfully").build();
        case 401:
            return Response.status(401).entity("Not authorised to delete resource or child resource").build();
        case 404:
            return Response.status(404).entity("Project not found").build();
        case 400:
            return Response.status(400).entity("Malformed request").build();
        case 500:
        default:
            return Response.status(500).entity("Unexpected server error").build();
        }
    }
    
    @GET
    @Path("/{id:[0-9a-z]*}/access")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProjectAccess(@PathParam("id") String id) {
        ProjectAccess access = projectController.getProjectAccess(id);
        if(access != null)
            return Response.ok().entity(access).build();
        else
            return Response.status(404).build();
    }
    
    @GET
    @Path("/{id:[0-9a-z]*}/access/{userId:([0-9a-zA-Z]|-)*}")
    @Produces(MediaType.APPLICATION_JSON)
    // get resource access for the given user
    public Response getUserAuthorisation(@PathParam("id") String projectId,
            @PathParam("userId") String userId) {
        ProjectAccess access = projectController.getUserAuthorisation(projectId, userId);
        if(access != null)
            return Response.ok().entity(access).build();
        else
            return Response.status(404).build();
    }
      
    @POST
    @Path("/{id:[0-9a-z]*}/access/")
    // create resource access for the given user 
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createUserAuthorisation(@PathParam("id") String projectId, ProjectAccess access) throws URISyntaxException {
        int status = projectController.createUserAuthorisation(projectId, access);
        switch(status) {
        case 201:
            return Response.created(new URI("/projects/" + projectId + "/access/" + access.getUserId())).entity("Success").build();
        case 400:
            return Response.status(400).entity("Invalid accessLevel").build();
        case 401:
            return Response.status(401).entity("Unauthorised access to project").build();
        case 404:
            return Response.status(404).entity("Project not found").build();
        case 409:
            return Response.status(409).entity("UserAuthorisation already exists").build();
        default:
            return Response.status(500).build();
        }
    }
    
    @PUT
    @Path("/{id:[0-9a-z]*}/access/{userId:([0-9a-zA-Z]|-)*}")
    @Consumes(MediaType.APPLICATION_JSON)
    // update resource access for the given user with the provided data
    public Response updateUserAuthorisation(@PathParam("id") String projectId,
            @PathParam("userId") String userId, ProjectAccess access) {
        int status = projectController.updateUserAuthorisation(projectId, userId, access);
        switch(status) {
        case 200:
            return Response.ok().entity("Success").build();
        case 400:
            return Response.status(400).entity("Invalid accessLevel").build();
        case 401:
            return Response.status(401).entity("Unauthorised access to project").build();
        case 404:
            return Response.status(404).entity("Project not found").build();
        default:
            return Response.status(500).build();
        }
    }
    
    @DELETE
    @Path("/{id:[0-9a-z]*}/access/{userId:([0-9a-zA-Z]|-)*}")
    public Response removeUserAuthorisation(@PathParam("id") String projectId,
            @PathParam("userId") String userId) {
        int status = projectController.removeUserAuthorisation(projectId, userId);
        switch(status) {
        case 200:
            return Response.ok().entity("Success").build();
        case 400:
            return Response.status(400).entity("Invalid accessLevel").build();
        case 401:
            return Response.status(401).entity("Unauthorised access to project").build();
        case 404:
            return Response.status(404).entity("Project not found").build();
        default:
            return Response.status(500).build();
        }
    }
}
