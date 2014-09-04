package net.sharemycode.service;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.TypedQuery;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.Consumes;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.commons.io.IOUtils;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import net.sharemycode.controller.ProjectController;
import net.sharemycode.model.Project;
import net.sharemycode.model.ProjectResource;
import net.sharemycode.model.ProjectResource.ResourceType;
import net.sharemycode.security.model.User;

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

    @GET
    @Path("/randomURL")
    public String returnURL() {
        return Project.generateURL();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Project> listAllProjects() {
        return projectController.listAllProjects();
    }
    
    @GET
    @Path("/{id:[0-9a-z]*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Project lookupProjectById(@PathParam("id") String id) {
    	Project project = projectController.lookupProject(id);
    	if(project == null) {
    		throw new WebApplicationException(Response.Status.NOT_FOUND);
    	}
    	return project;
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadProject(MultipartFormDataInput input) {
        Project newProject = projectController.uploadProject(input);
        if(newProject == null) {
        	return Response.status(400).entity("Failed to create project").build();
        }
        return Response.status(200).entity(newProject.getUrl()).build();
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

    //TODO update authorisation     - POST
    //TODO remove authorisation     - GET?
    //TODO publish resourcePOST     - POST
    //TODO list resources           - GET
    //TODO list projects            - GET
    //TODO fetch resource           - GET
    //TODO delete resource          - GET?
}
