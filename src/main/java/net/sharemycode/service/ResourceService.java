package net.sharemycode.service;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

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
import net.sharemycode.model.ProjectResource;
import net.sharemycode.model.ProjectResource.ResourceType;
import net.sharemycode.model.ResourceAccess;
import net.sharemycode.model.ResourceContent;

/**
 * ResourceService
 * - Defines all RESTful services relating to Resource entities
 * 
 * @author Lachlan Archibald
 * 
 */

@Path("/resources")
@Stateless
public class ResourceService {

    @Inject
    ProjectController projectController;
    @Inject
    ResourceController resourceController;

    /**
     * Lists all resources for all projects
     * @deprecated Used only for testing clients
     * @see net.sharemycode.service.ProjectService#listResources()
     * @return List of ProjectResource
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<ProjectResource> listAllResources() {
        List<ProjectResource> resources = resourceController.listAllResources();
        return resources;
    }

    /**
     * Lists children of the given ProjectResource
     * @param id Long ProjectResource id
     * @return List of ProjectResource
     */
    @GET
    @Path("/{id:[0-9]*}/children")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ProjectResource> listChildResources(@PathParam("id") Long id) {
        // return child resources of directory
        ProjectResource r = resourceController.lookupResource(id);
        List<ProjectResource> resources = resourceController.listChildResources(r);
        return resources;
    }
    
    /**
     * Downloads ProjectResource content as byte stream
     * @param id Long ProjectResource id
     * @return byte[] stream
     */
    @GET
    @Path("/{id:[0-9]*}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response fetchResource(@PathParam("id") Long id) {
        // Returns Resource Content
        ProjectResource resource = resourceController.lookupResource(id);
        if (resource == null)
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        if (resource.getResourceType() == ResourceType.DIRECTORY)
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        ResourceContent rc = resourceController.getResourceContent(resource);
        final byte[] content = rc.getContent();
        return Response
                .ok(new StreamingOutput() {
                    @Override
                    public void write(OutputStream output) throws IOException,
                            WebApplicationException {
                        output.write(content);
                    }
                })
                .header("Content-Disposition",
                        "attachment; filename=\"" + resource.getName() + "\"")
                .build();
    }

    /**
     * Deletes given ProjectResource, Children, Content and Access
     * @param id Long ProjectResource id
     * @return Response.ok() if successful
     */
    @DELETE
    @Path("/{id:[0-9]*}")
    public Response deleteResource(@PathParam("id") Long id) {
        // Delete resource, assuming ResourceID
        ProjectResource r = resourceController.lookupResource(id);
        int result = resourceController.deleteResource(r);
        switch (result) {
        case 200:
            return Response.ok().entity("Resource deleted - id: " + id).build();
        case 401:
            return Response
                    .status(401)
                    .entity("Not authorised to delete resource or child resource")
                    .build();
        case 404:
            return Response.status(404).entity("ProjectResource not found")
                    .build();
        case 400:
            return Response.status(400).entity("Malformed request").build();
        case 500:
        default:
            return Response.status(500).entity("Unexpected server error")
                    .build();
        }
    }

    /**
     * Gets current user's ResourceAccess for given ProjectResource
     * @param id Long ProjectResource id
     * @return ResourceAccess
     */
    @GET
    @Path("/{id:[0-9]*}/access")
    @Produces(MediaType.APPLICATION_JSON)
    // Get resource access for the current logged in user
    public Response getResourceAccess(@PathParam("id") Long id) {
        ResourceAccess access = resourceController.getResourceAccess(id);
        if (access != null)
            return Response.ok().entity(access).build();
        else
            return Response.status(404).build();
    }

    /**
     * Gets ResourceAccess for a given user
     * @param resourceId Long ProjectResource id
     * @param userId String userId
     * @return ResourceAccess
     */
    @GET
    @Path("/{id:[0-9]*}/access/{userId:([0-9a-zA-Z]|-)*}")
    @Produces(MediaType.APPLICATION_JSON)
    // get resource access for the given user
    public Response getUserAuthorisation(@PathParam("id") String resourceId,
            @PathParam("userId") String userId) {
        ResourceAccess access = resourceController.getUserAuthorisation(
                Long.valueOf(resourceId), userId);
        if (access != null)
            return Response.ok().entity(access).build();
        else
            return Response.status(404).build();
    }

    /**
     * Authorises a User to access a ProjectResource
     * @param resourceId Long ProjectResource id
     * @param access ResourceAccess with userId and AccessLevel
     * @return Response.created() with URI
     * @throws URISyntaxException if created URI is invalid
     */
    @POST
    @Path("/{id:[0-9]*}/access/")
    @Consumes(MediaType.APPLICATION_JSON)
    // create resource access for the given user (include object, or just
    // accessLevel?)
    public Response createUserAuthorisation(@PathParam("id") String resourceId,
            ResourceAccess access) throws URISyntaxException {
        int status = resourceController.createUserAuthorisation(
                Long.valueOf(resourceId), access);
        switch (status) {
        case 201:
            return Response
                    .created(
                            new URI("/resources/" + resourceId + "/access/"
                                    + access.getUserId())).entity("Success")
                    .build();
        case 400:
            return Response.status(400).entity("Invalid accessLevel").build();
        case 401:
            return Response.status(401)
                    .entity("Unauthorised access to resource").build();
        case 404:
            return Response.status(404).entity("Resource not found").build();
        case 409:
            return Response.status(409)
                    .entity("UserAuthorisation already exists").build();
        default:
            return Response.status(500).build();
        }
    }
    /**
     * Updates authorisation for a User to access a ProjectResource
     * 
     * @param resourceId Long ProjectResource id
     * @param userId String userId
     * @param access ResourceAccess containing updated AccessLevel
     * @return Response.ok()
     */
    @PUT
    @Path("/{id:[0-9]*}/access/{userId:([0-9a-zA-Z]|-)*}")
    @Consumes(MediaType.APPLICATION_JSON)
    // update resource access for the given user with the provided data
    public Response updateUserAuthorisation(@PathParam("id") String resourceId,
            @PathParam("userId") String userId, ResourceAccess access) {
        int status = resourceController.updateUserAuthorisation(
                Long.valueOf(resourceId), userId, access);
        switch (status) {
        case 200:
            return Response.ok().entity("Success").build();
        case 400:
            return Response.status(400).entity("Invalid accessLevel").build();
        case 401:
            return Response.status(401)
                    .entity("Unauthorised access to resource").build();
        case 404:
            return Response.status(404).entity("Project not found").build();
        default:
            return Response.status(500).build();
        }
    }
    /**
     * Deauthorises User to access a ProjectResource
     * @param resourceId Long ProjectResource id
     * @param userId String userId
     * @return Response.ok() if successful
     */
    @DELETE
    @Path("/{id:[0-9]*}/access/{userId:([0-9a-zA-Z]|-)*}")
    public Response removeUserAuthorisation(@PathParam("id") String resourceId,
            @PathParam("userId") String userId) {
        int status = resourceController.removeUserAuthorisation(
                Long.valueOf(resourceId), userId);
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
     * Publishes a new ProjectResource with metadata
     * 
     * @param resource ProjectResource containing metadata
     * @return Response.created() with URI and ProjectResource
     * @throws URISyntaxException if created URI is invalid
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response publishResource(ProjectResource resource)
            throws URISyntaxException {
        ProjectResource r = resourceController.publishResource(resource);
        return Response.created(new URI("/resources/" + r.getId())).entity(r)
                .build();
    }

    /**
     * Updates ProjectResource Metadata
     * @param id Long ProjectResource id
     * @param update ProjectResource containing updated info
     * @return Response.ok() with updated ProjectResource
     */
    @PUT
    @Path("/{id:[0-9]*}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateResourceInfo(@PathParam("id") Long id,
            ProjectResource update) {
        ProjectResource r = resourceController.lookupResource(id);
        ProjectResource result = resourceController.updateResourceInfo(r,
                update);
        if (result != null)
            return Response.ok().entity(result).build();
        else
            return Response.notModified().build();

    }

    /**
     * Publishes ProjectResource update to content
     * @param id Long ProjectResource id
     * @param data Base64Encoded String data
     * @return Response.ok() if successful, else Response.notModified()
     */
    @PUT
    @Path("/{id:[0-9]*}/content")
    public Response updateResourceContent(@PathParam("id") Long id, String data) {
        ProjectResource r = resourceController.lookupResource(id);
        try {
            ResourceContent content = resourceController.createResourceContent(
                    r, data);
            if (content != null)
                return Response.ok().entity("ResourceUpdated").build();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Response.notModified().build();
    }
}
