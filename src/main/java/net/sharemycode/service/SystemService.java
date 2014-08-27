package net.sharemycode.service;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

/**
 * sharemycode RESTEasy Web SystemService
 *
 */

@Path("")
public class SystemService {
    /*@GET
    @Path("/{pathParameter}")
    public Response responseMsg( @PathParam("pathParameter") String pathParameter,
        @DefaultValue("No response") @QueryParam("q") String queryParameter) {

        String response = "Hello from: " + pathParameter + ": " + queryParameter;
        return Response.status(200).entity(response).build();
    }*/
    
    
    @GET
    @Path("/client/test")
    public Response clientTest() {
        String response = "Hello client! Connection successful!";
        return Response.status(200).entity(response).build();
    }

}