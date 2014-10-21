package net.sharemycode.service;

import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * REST endpoints exposed for testing client connections.
 * 
 * @author Lachlan Archibald
 */

@Path("/system")
public class SystemService {
    @GET
    @Path("/test")
    public Response clientGetTest() {
        String response = "Hello client! Connection successful!";
        return Response.status(200).entity(response).build();
    }

    @POST
    @Path("/test")
    public Response clientPostTest(String test) {
        String response = "Hello client! You have posted " + test;
        return Response.status(200).entity(response).build();
    }

    @POST
    @Path("/test/json")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response clientPostJsonTest(Map<String, String> properties) {
        return Response.status(200).entity(properties).build();
    }

    @POST
    @Path("test/form")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    public Response clientPostFormTest(@FormParam("name") String name,
            @FormParam("value") String value) {
        String response = "name: hello, value: world";
        return Response.status(200).entity(response).build();
    }
}