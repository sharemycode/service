package net.sharemycode.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.sharemycode.Repository;
//import net.sharemycode.security.annotations.LoggedIn;
import net.sharemycode.events.NewProjectEvent;
import net.sharemycode.events.NewResourceEvent;
import net.sharemycode.model.Project;
import net.sharemycode.model.ProjectAccess;
import net.sharemycode.model.ProjectAccess.AccessLevel;
import net.sharemycode.model.ProjectResource;
import net.sharemycode.model.ProjectResource.ResourceType;
import net.sharemycode.model.Project_;
import net.sharemycode.model.ResourceContent;
import net.sharemycode.service.ProjectService;

import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.picketlink.Identity;

/*
 * Performs persistence operations for projects
 *
 * @author Shane Bryzak
 * @author Lachlan Archibald
 */
@ApplicationScoped
public class ProjectController
{
	public static final String TEMP_PROJECT_PATH = "./projectstorage/";
	
    @Inject
    private Instance<EntityManager> entityManager;

    @Inject Event<NewProjectEvent> newProjectEvent;

    @Inject Event<NewResourceEvent> newResourceEvent;

    @Inject
    private Identity identity;

    public List<Project> listAllProjects() {
    	EntityManager em = entityManager.get();
    	TypedQuery<Project> q = em.createQuery("select p from Project p", Project.class);
        return q.getResultList();
    }
    
    // @LoggedIn
    public void createProject(Project project) {
        // persist the project data
    	
    	//project.setOwner(identity.getAccount().getId());
        EntityManager em = entityManager.get();
        em.persist(project);

        // set the project access
        ProjectAccess pa = new ProjectAccess();
        pa.setProject(project);
        pa.setAccessLevel(AccessLevel.OWNER);
        pa.setOpen(true);
        //pa.setUserId(identity.getAccount().getId());
        em.persist(pa);

        // create resources from project
        try {
            createProjectResources(project, TEMP_PROJECT_PATH + project.getName());
        } catch (IOException e) {
            System.err.println("Error creating project resources");
            e.printStackTrace();
        }
        newProjectEvent.fire(new NewProjectEvent(project));
    }
    //  @LoggedIn
    public void createResource(ProjectResource resource) {
        // persist resource
        EntityManager em = entityManager.get();
        em.persist(resource);

        newResourceEvent.fire(new NewResourceEvent(resource));
    }

    private void createResourceContent(ProjectResource resource, String dataPath) throws IOException {
        EntityManager em = entityManager.get();
        // extract data from file
        Path path = Paths.get(dataPath);
        byte[] data = Files.readAllBytes(path);
        // create Resource Content
        ResourceContent content = new ResourceContent();
        content.setResource(resource);
        content.setContent(data);

        em.persist(content);
    }

    public Project lookupProject(String id)
    {
        EntityManager em = entityManager.get();
        return em.find(Project.class, id);
    }

    public ProjectResource lookupResource(Long id)
    {
        EntityManager em = entityManager.get();
        return em.find(ProjectResource.class, id);
    }

    public ProjectResource lookupResourceByName(Project project, ProjectResource parent, String name)
    {
        EntityManager em = entityManager.get();

        TypedQuery<ProjectResource> q = em.createQuery(
                "select r from ProjectResource r where r.project = :project and r.parent = :parent and r.name = :name",
                ProjectResource.class);
        q.setParameter("project", project);
        q.setParameter("parent", parent);
        q.setParameter("name", name);

        try
        {
            return q.getSingleResult();
        }
        catch (NoResultException ex)
        {
            return null;
        }
    }

    public List<Project> listProjects(String searchTerm)
    {
        EntityManager em = entityManager.get();

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<ProjectAccess> cq = cb.createQuery(ProjectAccess.class);
        Root<ProjectAccess> from = cq.from(ProjectAccess.class);
        Join<ProjectAccess,Project> project = from.join("project");

        List<Predicate> predicates = new ArrayList<Predicate>();
        // This section requires identities, disabled for now
        /*
        predicates.add(cb.equal(from.get("userId"), identity.getAccount().getId()));

        if (searchTerm != null && !"".equals(searchTerm))
        {
            predicates.add(cb.like(cb.lower(project.get(Project_.name)), "%" + searchTerm.toLowerCase() + "%"));
        }

        cq.where(predicates.toArray(new Predicate[predicates.size()]));
		*/
        TypedQuery<ProjectAccess> q = em.createQuery(cq);

        List<Project> projects = new ArrayList<Project>();
        for (ProjectAccess a : q.getResultList())
        {
            projects.add(a.getProject());
        }

        return projects;
    }

    public List<ProjectResource> listResources(Project project)
    {
        TypedQuery<ProjectResource> q = entityManager.get().<ProjectResource>createQuery("select r from ProjectResource r where r.project = :project", 
                ProjectResource.class);
        q.setParameter("project", project);
        return q.getResultList();
    }
    /*
     * CREATE DIRECTORY STRUCTURE
     * Author: Shane Bryzak
     * Description: Create directory resources, files may not have existing data
     */
    public ProjectResource createDirStructure(Project project, String directory)
    {
        String[] parts = directory.split(ProjectResource.PATH_SEPARATOR);

        ProjectResource parent = null;
        for (int i = 0; i < parts.length; i++)
        {
            // If the first part is equal to the project name, ignore it
            if (i == 0 && parts[i].equals(project.getName()))
            {
                continue;
            }

            ProjectResource r = lookupResourceByName(project, parent, parts[i]);
            //If the directory resource doesn't exist, create it
            if (r == null)
            {
                r = new ProjectResource();
                r.setProject(project);
                r.setResourceType(ResourceType.DIRECTORY);
                r.setName(parts[i]);
                r.setParent(parent);
                createResource(r);
            }

            parent = r;
        }
    
        return parent;
    }
    
    public ProjectResource createPackage(Project project, ProjectResource folder, String pkgName) {
        return null;
    }

    public Project uploadProject(MultipartFormDataInput input) {

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
        
        // extract the project temp files
        unzipProject(fileName, TEMP_PROJECT_PATH + p.getName(), null);
        //p.setFilePath(fileName);
        this.createProject(p);
        return p;
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


    /*
     * UNZIP PROJECT
     * Author: Lachlan Archibald
     * Description: Extract project files into temp directory
     */
    private static Boolean unzipProject(String sourcePath, String destPath, String password) {
        // Takes the path to a project archive, and extracts to destination path using zip4j
        try {
            ZipFile projectZip = new ZipFile(sourcePath);
            if(projectZip.isValidZipFile()) {
                if(projectZip.isEncrypted()) {
                    projectZip.setPassword(password);
                }
                projectZip.extractAll(destPath);
            } else {
                System.err.println("Error: Attempted to process invalid zip file");
                return false;
            }
        } catch(ZipException e) {
            System.err.println("Error while processing zip archive");
            e.printStackTrace();
            return false;
        }
        return true;
    }
    /*
     * CREATE PROJECT RESOURCES
     * Author: Lachlan Archibald
     * Description: Create resources from EXISTING project (ie. Already has byte data)
     */
    private Boolean createProjectResources(Project project, String projectLocation) throws IOException {
        // return list of files in directory
        ProjectResource parent = null;
        if(!processFiles(project, projectLocation, parent)) {
        	return false;
        } else
        	return true;
    }
    
    private Boolean processFiles(Project project, String currentDir, ProjectResource parent) throws IOException {
    	File[] files = new File(currentDir).listFiles();
        String dataPath = null;	// used to give path to file for extracting byte array
        for(File file : files) {
            String name = file.getName();
            ProjectResource r = lookupResourceByName(project, parent, name);
            if(r == null) { // if current resource does not exist, create it
                r = new ProjectResource();
                r.setProject(project);
                r.setName(name);
                r.setParent(parent);
                if(file.isDirectory()) {	// if current file is a directory
                    r.setResourceType(ResourceType.DIRECTORY);
                    createResource(r);
                    //TODO add the children files as well
                    String childDir = currentDir + "/" + name;
                    if(!processFiles(project, childDir, r))
                    	System.err.println("Error processing files in " + childDir);
                } else {
                    r.setResourceType(ResourceType.FILE);
                    createResource(r);
                    dataPath = file.getAbsolutePath();
                    createResourceContent(r, dataPath);
                }
            }
        }
        return true;
    }
}
