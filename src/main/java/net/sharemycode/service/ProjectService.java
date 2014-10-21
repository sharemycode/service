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
import javax.ws.rs.QueryParam;
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
 * Defines all RESTful services relating to project entities
 * 
 * @author Lachlan Archibald
 * 
 */

@Path("/projects")
@Stateless
public class ProjectService {

    @Inject
    ProjectController projectController;
    @Inject
    ResourceController resourceController;

    /** 
     * Generates a random 6 character URL
     * @deprecated Used only for testing
     * @return String
     */
    @GET
    @Path("/randomURL")
    public String returnURL() {
        // test the random URL generation
        return Project.generateURL();
    }

    /**
     * Lists all Project owned by the current User
     * @return List of Projects for the logged in User
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Project> listAllProjects() {
        // list of all user's projects
        return projectController.listAllProjects();
    }

    /**
     * Creates a new project with a list of ProjectAttachment ids
     * @param properties JSON project information and list of attachmentId
     * @return Response.created() with Project
     * @throws URISyntaxException if resulting URI is invalid
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadProject(Map<String, Object> properties)
            throws URISyntaxException {
        // create new project, accepts JSON with project name, version,
        // description, attachmentIDs
        Project newProject = projectController.submitProject(properties);
        if (newProject == null) {
            return Response.status(400).entity("Failed to create project")
                    .build();
        }
        // String output = newProject.getUrl();
        return Response.created(new URI("/projects/" + newProject.getId()))
                .entity(newProject).build();
    }

    /**
     * Lists Shared Projects
     * @return List of Projects that the User has owner permissions
     */
    @GET
    @Path("/shared")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Project> listSharedProjects() {
        return projectController.listSharedProjects();
    }

    /**
     * Returns a Project entity by id
     * @param id String Project id
     * @return Project
     */
    @GET
    @Path("/{id:[0-9a-z]*}")
    @Produces(MediaType.APPLICATION_JSON)
    public Project lookupProjectById(@PathParam("id") String id) {
        // return specific project information
        Project project = projectController.lookupProject(id);
        if (project == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        return project;
    }

    /**
     * Downloads the entire project as a .zip
     * @param id ProjectID to download
     * @return Response.ok() with application/zip stream
     */
    @GET
    @Path("/{id:[0-9a-z]*}/download")
    @Produces("application/zip")
    public Response fetchProject(@PathParam("id") String id) {
        Project p = projectController.lookupProject(id);
        final byte[] data = projectController.fetchProject(p);
        return Response
                .ok(new StreamingOutput() {
                    @Override
                    public void write(OutputStream output) throws IOException,
                            WebApplicationException {
                        output.write(data);
                    }
                })
                .header("Content-Disposition",
                        "attachment; filename=\"" + p.getName() + "_"
                                + p.getVersion() + ".zip" + "\"").build();
    }

    /**
     * Lists Project Resources
     * @param projectid ProjectID to get resources
     * @param root 0: list all, 1: list only top-level resources
     * @return List of ProjectResource
     */
    @GET
    @Path("/{projectid:[0-9a-z]*}/resources")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ProjectResource> listProjectResources(
            @PathParam("projectid") String projectid,
            @QueryParam("root") int root) {
        // List all resources associated with a project.
        Project project = projectController.lookupProject(projectid);
        if (project == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        List<ProjectResource> resources = resourceController
                .listResources(project, root);  // if root is non-zero, return only root resources
        return resources;
    }

    /**
     * Adds Attachments to Project
     * @param id Existing ProjectId to add attachments
     * @param attachments List of String Ids of attachments
     * @return Response.ok() if successful, or Response.notModifed()
     */
    @PUT
    @Path("/{id:[0-9a-z]*}/attachments")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addAttachmentsToProject(@PathParam("id") String id,
            List<String> attachments) {
        // add attachments to existing project
        Project p = projectController.lookupProject(id);
        Boolean result = projectController.addAttachmentsToProject(p,
                attachments);
        if (result)
            return Response.ok().build();
        else
            return Response.notModified().build();
    }

    /**
     * Uploads Attachment using the REST endpoint
     * @param name Filename
     * @param data Base64EncodedString data
     * @return Response.ok() with attachmentId if successful
     */
    @POST
    @Path("/attachments/{filename}")
    public Response uploadAttachment(@PathParam("filename") String name,
            String data) {
        Long attachment = projectController.createAttachmentFromService(name,
                data);
        if (attachment > -1L)
            return Response.ok().entity(attachment.toString()).build();
        else
            return Response.noContent().build();
    }

    /**
     * Lists Projects with search ?
     * @param searchTerm String name of project to search
     * @return List of own Projects that match search term (not tested)
     */
    @GET
    @Path("/list{searchTerm:(/[^/]+?)?}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Project> listProjects(@PathParam("searchTerm") String searchTerm)
    // returns list of projects that match search term.
    // TODO edit to use QueryParam?
    {
        if (searchTerm.startsWith("/")) {
            searchTerm = searchTerm.substring(1);
        }
        return projectController.listProjects(searchTerm);
    }

    /**
     * Creates new folder in a Project ?
     * @param properties
     * @return ProjectResource[] ?
     */
    @POST
    @Path("/newfolder")
    @Consumes("application/json")
    @Produces(MediaType.APPLICATION_JSON)
    public ProjectResource[] createProjectFolder(Map<String, String> properties)
    // create a new directory resource under provided parent resource
    {
        String[] parts = properties.get("name").split("/");
        List<ProjectResource> resources = new ArrayList<ProjectResource>();

        ProjectResource parent = null;

        if (properties.containsKey("parentResourceId")) {
            Long resourceId = Long.valueOf(properties.get("parentResourceId"));
            parent = resourceController.lookupResource(resourceId);
        }

        for (String part : parts) {
            ProjectResource r = new ProjectResource();
            r.setName(part);
            r.setResourceType(ResourceType.DIRECTORY);
            r.setParent(parent);

            if (properties.containsKey("projectId")) {
                String projectId = properties.get("projectId");
                r.setProject(projectController.lookupProject(projectId));
            }

            projectController.createResource(r);
            parent = r;
        }

        return resources.toArray(new ProjectResource[resources.size()]);
    }

    /**
     * Creates new Java class in Project ?
     * @param properties
     * @return ProjectResource
     */
    @POST
    @Path("/newclass")
    @Consumes("application/json")
    @Produces(MediaType.APPLICATION_JSON)
    public ProjectResource createProjectClass(Map<String, String> properties)
    // create new Java Class?
    // TODO Re-evaluate this method
    {
        ProjectResource r = new ProjectResource();
        r.setName(properties.get("name") + ".java");
        r.setResourceType(ResourceType.FILE);

        Project p = projectController
                .lookupProject(properties.get("projectId"));
        r.setProject(p);

        String folder = properties.get("folder");
        String pkg = properties.get("package");

        ProjectResource parentDir = projectController.createDirStructure(p,
                folder);
        ProjectResource parentPkg = projectController.createPackage(p,
                parentDir, pkg);
        r.setParent(parentPkg);

        projectController.createResource(r);
        return r;
    }

    /**
     * Lists Projects by owner. Not tested recently
     * @param username Username to search projects for.
     * @return List of Project
     */
    @GET
    @Path("/user/{username:[0-9a-z]*}")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Project> listProjectsByOwner(
            @PathParam("username") String username) {
        // list projects by owner
        // this may not be necessary, or may need to use a QueryParam if for
        // general searching
        List<Project> projects = projectController
                .listProjectsByOwner(username);
        return projects;
    }

    /**
     * Updates Project information. Requires READ_WRITE permission
     * @param id ProjectId to update
     * @param update Project containing updated data
     * @return Response.ok() with updated Project
     */
    @PUT
    @Path("/{id:[0-9a-z]*}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateProject(@PathParam("id") String id, Project update) {
        Project result = projectController.updateProject(id, update);
        if (result != null)
            return Response.ok().entity(result).build();
        else
            return Response.notModified().entity("Project not modified").build();
    }

    /**
     * Changes Displayed Project Owner. Requires OWNER permission
     * @param id ProjectId to update
     * @param username Username to set as new owner
     * @return Response.ok()
     */
    @PUT
    @Path("/{id:[0-9a-z]*}/owner")
    @Produces(MediaType.APPLICATION_JSON)
    public Response changeProjectOwner(@PathParam("id") String id,
            String username) {
        Project p = projectController.changeProjectOwner(id, username);
        if (p != null)
            return Response.ok().entity("Project owner updated").build();
        else
            return Response.notModified().entity("Project or userId not found")
                    .build();
    }

    /**
     * Deletes Project. Requires OWNER permission
     * 
     * @param id ProjectId to delete
     * @return Reponse.ok() if successful
     */
    @DELETE
    @Path("/{id:[0-9a-z]*}")
    public Response deleteProject(@PathParam("id") String id) {
        // Delete project resource, and all associated resources
        int result = projectController.deleteProject(id);
        switch (result) {
        case 200:
            return Response.ok().entity("Project deleted successfully").build();
        case 401:
            return Response
                    .status(401)
                    .entity("Not authorised to delete resource or child resource")
                    .build();
        case 404:
            return Response.status(404).entity("Project not found").build();
        case 400:
            return Response.status(400).entity("Malformed request").build();
        case 500:
        default:
            return Response.status(500).entity("Unexpected server error")
                    .build();
        }
    }

    /**
     * Gets ProjectAccess for current User
     * @param id ProjectId
     * @return ProjectAccess entity for current user
     */
    @GET
    @Path("/{id:[0-9a-z]*}/access")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProjectAccess(@PathParam("id") String id) {
        ProjectAccess access = projectController.getProjectAccess(id);
        if (access != null)
            return Response.ok().entity(access).build();
        else
            return Response.status(404).build();
    }

    /**
     * Gets UserAuthorisation for a given User
     * @param projectId ProjectId
     * @param userId User to get ProjectAccess for
     * @return ProjectAccess for given user
     */
    @GET
    @Path("/{id:[0-9a-z]*}/access/{userId:([0-9a-zA-Z]|-)*}")
    @Produces(MediaType.APPLICATION_JSON)
    // get resource access for the given user
    public Response getUserAuthorisation(@PathParam("id") String projectId,
            @PathParam("userId") String userId) {
        ProjectAccess access = projectController.getUserAuthorisation(
                projectId, userId);
        if (access != null)
            return Response.ok().entity(access).build();
        else
            return Response.status(404).build();
    }

    /**
     * Creates User Authorisation for Project
     * @param projectId ProjectId
     * @param access ProjectAccess
     * @return Response.created() with URI
     * @throws URISyntaxException if invalid URI created
     */
    @POST
    @Path("/{id:[0-9a-z]*}/access/")
    // create resource access for the given user
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createUserAuthorisation(@PathParam("id") String projectId,
            ProjectAccess access) throws URISyntaxException {
        int status = projectController.createUserAuthorisation(projectId,
                access);
        switch (status) {
        case 201:
            return Response
                    .created(
                            new URI("/projects/" + projectId + "/access/"
                                    + access.getUserId())).entity("Success")
                    .build();
        case 400:
            return Response.status(400).entity("Invalid accessLevel").build();
        case 401:
            return Response.status(401)
                    .entity("Unauthorised access to project").build();
        case 404:
            return Response.status(404).entity("Project not found").build();
        case 409:
            return Response.status(409)
                    .entity("UserAuthorisation already exists").build();
        default:
            return Response.status(500).build();
        }
    }

    /**
     * Updates User Authorisation for Project
     * @param projectId ProjectId
     * @param userId UserId to update authorisation
     * @param access ProjectAccess
     * @return Response.ok() if successful
     */
    @PUT
    @Path("/{id:[0-9a-z]*}/access/{userId:([0-9a-zA-Z]|-)*}")
    @Consumes(MediaType.APPLICATION_JSON)
    // update resource access for the given user with the provided data
    public Response updateUserAuthorisation(@PathParam("id") String projectId,
            @PathParam("userId") String userId, ProjectAccess access) {
        int status = projectController.updateUserAuthorisation(projectId,
                userId, access);
        switch (status) {
        case 200:
            return Response.ok().entity("Success").build();
        case 400:
            return Response.status(400).entity("Invalid accessLevel").build();
        case 401:
            return Response.status(401)
                    .entity("Unauthorised access to project").build();
        case 404:
            return Response.status(404).entity("Project not found").build();
        default:
            return Response.status(500).build();
        }
    }

    /**
     * Removes User Authorisation for Project
     * @param projectId ProjectId
     * @param userId UserId to de-authorise
     * @return Response.ok() if successful
     */
    @DELETE
    @Path("/{id:[0-9a-z]*}/access/{userId:([0-9a-zA-Z]|-)*}")
    public Response removeUserAuthorisation(@PathParam("id") String projectId,
            @PathParam("userId") String userId) {
        int status = projectController.removeUserAuthorisation(projectId,
                userId);
        switch (status) {
        case 200:
            return Response.ok().entity("Success").build();
        case 400:
            return Response.status(400).entity("Invalid accessLevel").build();
        case 401:
            return Response.status(401)
                    .entity("Unauthorised access to project").build();
        case 404:
            return Response.status(404).entity("Project not found").build();
        default:
            return Response.status(500).build();
        }
    }
}
