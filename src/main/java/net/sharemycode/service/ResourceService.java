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

    //TODO listResources - GET
	
    //TODO fetchResource(ResourcePath) - GET
	@GET
	@Path("/{id:[0-9]*}")
	@Produces(MediaType.APPLICATION_JSON)
	public ProjectResource fetchResource(@PathParam("id") Long id) {
		ProjectResource resource = projectController.lookupResource(id);
		if(resource == null)
			throw new WebApplicationException(Response.Status.NOT_FOUND);
		return resource;
	}
	
	//TODO deleteResource(ResourcePath) - DELETE
	/*
	@DELETE
	@Path("/{id:[0-9]}")
	public Response deleteResource(@PathParam("id") Long id) {
		int result = projectController.deleteResource(id);
		if(result == 0) {
			return Response.status(200).entity("Resource " + id + "deleted").build();
		} else
			return Response.status(400).entity("Error deleting resource").build();
	}*/

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
