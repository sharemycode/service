package net.sharemycode.service;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.TypedQuery;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
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
import net.sharemycode.controller.ResourceController;
import net.sharemycode.model.Project;
import net.sharemycode.model.ProjectResource;
import net.sharemycode.model.ProjectResource.ResourceType;
import net.sharemycode.model.ResourceContent;
import net.sharemycode.security.model.User;

/**
 * sharemycode.net ResourceService
 * 
 * Defines all RESTful services relating to Resource entities
 * @author Lachlan Archibald
 *
 */

@Path("/resources")
@Stateless
public class ResourceService {
    
	@Inject ProjectController projectController;
	@Inject ResourceController resourceController;
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public List<ProjectResource> listAllResources() {
		List<ProjectResource> resources = resourceController.listAllResources();
		return resources;
	}
	
    //TODO fetchResource(ResourcePath) - GET
	@GET
	@Path("/{id:[0-9]*}")
	//@Produces(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response fetchResource(@PathParam("id") Long id) {
	    // Returns Resource Content
		ProjectResource resource = resourceController.lookupResource(id);
		if(resource == null)
			throw new WebApplicationException(Response.Status.NOT_FOUND);
		if(resource.getResourceType() == ResourceType.DIRECTORY)
		    throw new WebApplicationException(Response.Status.BAD_REQUEST);
		ResourceContent rc = resourceController.getResourceContent(resource);
		final byte[] content = rc.getContent();
		return Response.ok(new StreamingOutput() {
		    public void write(OutputStream output) throws IOException, WebApplicationException {
		        output.write(content);
		    }
		}).header("Content-Disposition", "attachment; filename=\"" + resource.getName() + "\"").build();
	}
	// TODO createResource(ResourcePath, ResourceContent)  // Path to parentResource?
	
	// TODO deleteResource(ResourcePath) - DELETE   // path to resource (in relation to project hierarchy), or resourceID?
	@DELETE
	@Path("/{id:[0-9]}")
	public Response deleteResource(@PathParam("id") Long id) {
	    // Delete resource, assuming ResourceID
		int result = resourceController.deleteResource(id);
		switch (result) {
		case 200:
		    return Response.ok().entity("Resource deleted - id: " + id).build();
        case 401:
            return Response.status(401).entity("Not authorised to delete resource or child resource").build();
        case 404:
            return Response.status(404).entity("ProjectResource not found").build();
        case 400:
            return Response.status(400).entity("Malformed request").build();
        case 500:
        default:
            return Response.status(500).entity("Unexpected server error").build();
        }
	}

	//TODO publishResource(ResourcePath, ResourceContent) - POST
	/*
	@POST
	@Path("/upload")
	@Consumes(MediaType.APPLICATION_OCTET_STREAM)
	public Response uploadFile(InputStream input) {
	    String location = projectController.createAttachment(input);
	    if(location != null) {
	        return Response.status(200).entity(location).build();
	    } else {
	        return Response.status(400).entity("Upload failed!").build();
	    }
	}*/
}
