package net.sharemycode.service;

import javax.inject.Inject;
import javax.persistence.TypedQuery;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.FormParam;
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
public class ProjectService {
    public static final String TEMP_PROJECT_PATH = "./projectstorage/";

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

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response createProject(MultipartFormDataInput input) {

        Map<String, List<InputPart>> formParts = input.getFormDataMap();

        // FILE UPLOAD SECTION
        String fileName = "";
        List<InputPart> inPart = formParts.get("projectFile");
        for (InputPart inputPart : inPart) {
            try {

                // Retrieve headers, read the Content-Disposition header to obtain the original name of the file
                MultivaluedMap<String, String> headers = inputPart.getHeaders();
                fileName = parseFileName(headers);

                // Handle the body of that part with an InputStream
                InputStream istream = inputPart.getBody(InputStream.class,null);

                fileName = TEMP_PROJECT_PATH + fileName;
                File path = new File(TEMP_PROJECT_PATH);
                if(!path.exists()) {	// if path does not exist
                    if(path.mkdirs()) {	// create directory
                        System.out.println("Created directory " + TEMP_PROJECT_PATH);
                        if(!path.canWrite() || !path.canRead()) {
                            // if not writable, change permissions
                            path.setWritable(true);
                            path.setReadable(true);
                        }
                        saveFile(istream,fileName);
                    } else {
                        System.out.println("Failed to create directory " + TEMP_PROJECT_PATH);

                    }
                } else {
                    // if directory exists but is not writable, change permissions
                    if(!path.canWrite() || !path.canRead()) {
                        path.setWritable(true);
                        path.setReadable(true);
                    }
                    saveFile(istream,fileName);
                }
                String uploadResult = "File saved to server location : " + fileName;
                System.out.println(uploadResult);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // PROJECT DETAIL SECTION

        Project p = new Project();
        try {
            p.setName(formParts.get("name").get(0).getBodyAsString());
            p.setVersion(formParts.get("version").get(0).getBodyAsString());
            p.setDescription(formParts.get("description").get(0).getBodyAsString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        p.setUrl(Project.generateURL());
        p.setFilePath(fileName);
        //projectController.createProject(p);
        String output = "Project Created: {" + p.getName() + ", " + p.getVersion() 
                + ", " + p.getDescription() + ", " + p.getUrl() + ", " + p.getFilePath() + "}";
        return Response.status(200).entity(output).build();
    }

    // Parse Content-Disposition header to get the original file name
    private String parseFileName(MultivaluedMap<String, String> headers) {
        String[] contentDispositionHeader = headers.getFirst("Content-Disposition").split(";");
        for (String name : contentDispositionHeader) {
            if ((name.trim().startsWith("filename"))) {
                String[] tmp = name.split("=");

                String fileName = tmp[1].trim().replaceAll("\"","");

                return fileName;
            }
        }
        return "unknownFile";
    }

    // save uploaded file to a defined location on the server
    private void saveFile(InputStream uploadedInputStream,
            String serverLocation) {

        try {
            OutputStream outpuStream = new FileOutputStream(new File(serverLocation));
            int read = 0;
            byte[] bytes = new byte[1024];

            outpuStream = new FileOutputStream(new File(serverLocation));
            while ((read = uploadedInputStream.read(bytes)) != -1) {
                outpuStream.write(bytes, 0, read);
            }
            outpuStream.flush();
            outpuStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
