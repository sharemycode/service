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
import java.util.Date;
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
import javax.ws.rs.core.MultivaluedMap;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
//import net.sharemycode.security.annotations.LoggedIn;
import net.sharemycode.events.NewProjectEvent;
import net.sharemycode.events.NewResourceEvent;
import net.sharemycode.model.Project;
import net.sharemycode.model.ProjectAccess;
import net.sharemycode.model.ProjectAccess.AccessLevel;
import net.sharemycode.model.ProjectAttachment;
import net.sharemycode.model.ProjectResource;
import net.sharemycode.model.ProjectResource.ResourceType;
import net.sharemycode.model.Project_;
import net.sharemycode.model.ResourceAccess;
import net.sharemycode.model.ResourceContent;
import net.sharemycode.security.annotations.LoggedIn;
import net.sharemycode.security.model.User;
import net.sharemycode.security.schema.IdentityType;
import net.sharemycode.security.schema.UserIdentity;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
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
    public static final String TEMP_STORAGE = "projectStorage/";
    public static final String ATTACHMENT_PATH = TEMP_STORAGE + "attachments/";
	public static final String PROJECT_PATH = TEMP_STORAGE + "projects/";
	public static final int MAX_UPLOAD = 10485760; // max upload filesize in bytes (10MB)
	
    @Inject
    private Instance<EntityManager> entityManager;

    @Inject Event<NewProjectEvent> newProjectEvent;

    @Inject Event<NewResourceEvent> newResourceEvent;
    @Inject UserController userController;
    @Inject ResourceController resourceController;

    @Inject
    private Identity identity;

    @LoggedIn
    public List<Project> listAllProjects() {
    	EntityManager em = entityManager.get();
    	String userId = identity.getAccount().getId();
    	TypedQuery<Project> q = em.createQuery("select p from Project p, ProjectAccess pa WHERE p = pa.project AND pa.userId = :userId AND pa.accessLevel = :accessLevel", Project.class);
        q.setParameter("userId", userId);
        q.setParameter("accessLevel", AccessLevel.OWNER);
    	return q.getResultList();
    }
    
    @LoggedIn
    public List<Project> listSharedProjects() {
        // returns a list of projects for which the user has access to
        EntityManager em = entityManager.get();
        String userId = identity.getAccount().getId();
        TypedQuery<Project> q = em.createQuery("select p from Project p, ProjectAccess pa WHERE p = pa.project AND pa.userId = :userId AND " + ""
                + "pa.accessLevel = :read OR pa.accessLevel = :write OR pa.accessLevel = :restricted", Project.class);
        q.setParameter("userId", userId);
        q.setParameter("read", AccessLevel.READ);
        q.setParameter("write", AccessLevel.READ_WRITE);
        q.setParameter("restricted", AccessLevel.RESTRICTED);
        return q.getResultList();
    }
    
    @LoggedIn
    public Project createProject(Project project) {
        // persist the project data
    	project.setOwner(identity.getAccount().getId());
        //project.setOwner("TestingOnly");    // testing project creation only.

        EntityManager em = entityManager.get();
        Boolean uniqueUrl = false;
        String newUrl = null;
        while(!uniqueUrl) {
            newUrl = Project.generateURL();
            TypedQuery<Project> q = em.createQuery("SELECT p FROM Project p WHERE p.url = :url", Project.class);
            q.setParameter("url", newUrl);
            if(q.getResultList().size() == 0)
                uniqueUrl = true;
        }
        project.setUrl(newUrl);
        em.persist(project);

        // set the project access
        ProjectAccess pa = new ProjectAccess();
        pa.setProject(project);
        pa.setAccessLevel(AccessLevel.OWNER);
        pa.setOpen(true);
        pa.setUserId(identity.getAccount().getId());
        em.persist(pa);
        
        newProjectEvent.fire(new NewProjectEvent(project));
        return project;
    }
    
    @LoggedIn
    public ProjectResource createResource(ProjectResource resource) {
        // persist resource
        EntityManager em = entityManager.get();
        em.persist(resource);

        newResourceEvent.fire(new NewResourceEvent(resource));
        return resource;
    }
    
    @LoggedIn
    public ResourceAccess createResourceAccess(ProjectResource resource, String userId, ResourceAccess.AccessLevel accessLevel) {
        EntityManager em = entityManager.get();
        ResourceAccess ra = new ResourceAccess();
        ra.setResource(resource);
        ra.setAccessLevel(accessLevel);
        ra.setUserId(userId);
        
        em.persist(ra);
        return ra;
    }
    
    private ResourceContent createResourceContent(ProjectResource resource, String dataPath) throws IOException {
        EntityManager em = entityManager.get();
        // extract data from file
        Path path = Paths.get(dataPath);
        byte[] data = Files.readAllBytes(path);
        // create Resource Content
        ResourceContent content = new ResourceContent();
        content.setResource(resource);
        content.setContent(data);

        em.persist(content);
        return content;
    }

    public Project lookupProject(String id)
    {
        EntityManager em = entityManager.get();
        return em.find(Project.class, id);
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
        //predicates.add(cb.equal(from.get("userId"), identity.getAccount().getId()));

        if (searchTerm != null && !"".equals(searchTerm))
        {
            predicates.add(cb.like(cb.lower(project.get(Project_.name)), "%" + searchTerm.toLowerCase() + "%"));
        }

        cq.where(predicates.toArray(new Predicate[predicates.size()]));
		
        TypedQuery<ProjectAccess> q = em.createQuery(cq);

        List<Project> projects = new ArrayList<Project>();
        for (ProjectAccess a : q.getResultList())
        {
            projects.add(a.getProject());
        }

        return projects;
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
    
    /*
     * UPLOAD PROJECT
     * Author: Lachlan Archibald
     * Description: Upload an existing project (zip file)
     */
/*
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

                fileName = PROJECT_PATH + fileName;
                File path = new File(PROJECT_PATH);
                if(!path.exists()) {	// if path does not exist
                    if(path.mkdirs()) {	// create directory
                        System.out.println("Created directory " + PROJECT_PATH);
                        if(!path.canWrite() || !path.canRead()) {
                            // if not writable, change permissions
                            path.setWritable(true);
                            path.setReadable(true);
                        }
                        saveFile(istream,fileName);
                    } else {
                        System.out.println("Failed to create directory " + PROJECT_PATH);

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
        unzipProject(fileName, PROJECT_PATH + p.getName(), null);
        //p.setFilePath(fileName);
        this.createProject(p);
        return p;
    }
*/    
    public Project submitProject(Map<String,Object> properties) {
        System.out.println("projectController");
        // First we test if the user entered non-unique username and email
        Project p = new Project();
        p.setName((String) properties.get("name"));
        p.setVersion((String) properties.get("version"));
        p.setDescription((String) properties.get("description"));
        @SuppressWarnings("unchecked")
        List<String> attachments = (List<String>) properties.get("attachments");
        Project result = this.createProject(p);
        // now create the resources from the attachments
        if(attachments != null) {   // if JSON data does not include attachments object, do not fail.
            if(attachments.size() > 0) {
                try {
                    this.createProjectResources(result, attachments, null);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;  
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
            OutputStream outputStream = new FileOutputStream(new File(serverLocation));
            int read = 0;
            byte[] bytes = new byte[MAX_UPLOAD];

            outputStream = new FileOutputStream(new File(serverLocation));
            while ((read = uploadedInputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }
            outputStream.flush();
            outputStream.close();
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
    @LoggedIn
    private Boolean createProjectResources(Project project, List<String> attachments, ProjectResource parent) throws IOException {
        EntityManager em = entityManager.get();
        // new method
        String userId = identity.getAccount().getId();
        //String userId = "TestingOnly";
        String tempProjectPath = PROJECT_PATH + ProjectResource.PATH_SEPARATOR +
                userId + ProjectResource.PATH_SEPARATOR + project.getName();
        for(String attachment : attachments) {
            Long id = Long.valueOf(attachment); // convert value of String representation to Long object
            ProjectAttachment pa = em.find(ProjectAttachment.class, id);
            if(pa.getUploadPath().endsWith(".zip")) {
                // attachment is a zip file, unzip it
                File zipFile = new File(pa.getUploadPath());
                if(!unzipProject(zipFile.getAbsolutePath(), tempProjectPath, null)) {
                    System.err.println("Problem extracting file " + zipFile.getName() + " to project directory.");
                    return false;
                }
                if(!processDirectory(project, tempProjectPath, parent))
                    return false;
            } else {
                // treat as a normal file
                if(!processFile(project, pa.getUploadPath(), parent))
                    return false;
            }
        }
        return true;
    }
    
    private boolean processFile(Project project, String path,
            ProjectResource parent) throws IOException {
        // process individual file
        File file = new File(path);
        String name = file.getName();
        ProjectResource r = lookupResourceByName(project, parent, name);
        if(r == null) {
            r = new ProjectResource();
            r.setProject(project);
            r.setName(name);
            r.setParent(parent);
            r.setResourceType(ResourceType.FILE);
            createResource(r);
            // For each user with access to the project, create ResourceAccess
            createResourceAccessForAll(project, r);
            // create resource content
            String dataPath = file.getAbsolutePath();
            createResourceContent(r, dataPath);
            return true;
        }
        return false;
    }

    private void createResourceAccessForAll(Project project, ProjectResource r) {
        // For each user with access to project, create appropriate ResourceAccess
        // By default, ResourceAccess.AccessLevel == ProjectAccess.AccessLevel
        EntityManager em = entityManager.get();
        TypedQuery<ProjectAccess> q = em.createQuery("SELECT pa FROM ProjectAccess pa WHERE pa.project = :project", ProjectAccess.class);
        q.setParameter("project", project);
        List<ProjectAccess> paList = q.getResultList();
        for(ProjectAccess pa : paList) {
            switch(pa.getAccessLevel()) {
            case OWNER:
                createResourceAccess(r, pa.getUserId(), ResourceAccess.AccessLevel.OWNER);
                break;
            case READ_WRITE:
                createResourceAccess(r, pa.getUserId(), ResourceAccess.AccessLevel.READ_WRITE);
                break;
            case READ:
                createResourceAccess(r, pa.getUserId(), ResourceAccess.AccessLevel.READ);
                break;
            case RESTRICTED:
                // create ResourceAccess for individual files, not bulk
            default:
                // do nothing
            }
        }
        
    }

    private Boolean processDirectory(Project project, String currentDir, ProjectResource parent) throws IOException {
        // return list of files in directory
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
                    // create resource
                    createResource(r);
                    
                    // For each user with access to the project, create ResourceAccess
                    createResourceAccessForAll(project, r);
                    
                    // Also create resource from children files
                    String childDir = currentDir + ProjectResource.PATH_SEPARATOR + name;
                    if(!processDirectory(project, childDir, r))
                    	System.err.println("Error processing files in " + childDir);
                } else {
                    r.setResourceType(ResourceType.FILE);
                    // create resource
                    createResource(r);
                    
                    // For each user with access to the project, create ResourceAccess
                    createResourceAccessForAll(project, r);
                    // create resource content
                    dataPath = file.getAbsolutePath();
                    createResourceContent(r, dataPath);
                }
            }
        }
        return true;
    }

    /*
     * LIST PROJECTS BY OWNER
     * Author: Lachlan Archibald
     * Description: Return list of projects owned by username
     */
	public List<Project> listProjectsByOwner(String username) {
		// TODO Auto-generated method stub
		EntityManager em = entityManager.get();
		User user = userController.lookupUserByUsername(username);
		TypedQuery<Project> q = em.createQuery("SELECT p FROM Project p WHERE p.owner_id = :user", Project.class);
		q.setParameter("user", user.getId());
		return q.getResultList();
	}
	
	/* 
	 * CREATE ATTACHMENT - Multipart, File, Persistence
	 * @Author: Lachlan Archibald
	 * Returns returns the id of an attachment as Long
	 */
	
	/* CREATE ATTACHMENT FROM MULTIPART */ // Not used
/*
	// returns multiple AttachmentIds in array of Longs.
	public List<Long> createAttachmentsFromMultipart(MultipartFormDataInput input) {
	    List<Long> attachments = new ArrayList<Long>();
        Map<String, List<InputPart>> formParts = input.getFormDataMap();
        
        // FILE UPLOAD SECTION
        String fileName = "";
        //String userID = identity.getAccount().getId();
        String userID = "TestingOnly";
        String uploadDirectory = ATTACHMENT_PATH + userID +  ProjectResource.PATH_SEPARATOR +
                System.currentTimeMillis() + ProjectResource.PATH_SEPARATOR;
        List<InputPart> inPart = formParts.get("file");
        for (InputPart inputPart : inPart) {
            try {

                // Retrieve headers, read the Content-Disposition header to obtain the original name of the file
                MultivaluedMap<String, String> headers = inputPart.getHeaders();
                fileName = parseFileName(headers);

                // Handle the body of that part with an InputStream
                InputStream istream = inputPart.getBody(InputStream.class,null);

                fileName = uploadDirectory + fileName;
                File path = new File(uploadDirectory);
                if(!path.exists()) {    // if path does not exist
                    if(path.mkdirs()) { // create directory
                        System.out.println("Created directory " + uploadDirectory);
                        if(!path.canWrite() || !path.canRead()) {
                            // if not writable, change permissions
                            path.setWritable(true);
                            path.setReadable(true);
                        }
                        saveFile(istream,fileName);
                    } else {
                        System.err.println("Failed to create directory " + uploadDirectory);
                        return null; // error occurred
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
                
                // now create the project attachment entity
                ProjectAttachment pa = createProjectAttachment(fileName);
                attachments.add(pa.getId());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return attachments;
	}
*/
	
	public Long createAttachmentFromService(String name, String data) {
	    // create project attachment from REST endpoint
	    String uploadDirectory = ATTACHMENT_PATH + System.currentTimeMillis() + "/";
	    File tempFile = new File(uploadDirectory + name);
	    try{
	        byte[] byteData = Base64.decodeBase64(data);
	        //FileOutputStream fos = new FileOutputStream(uploadDirectory + name);
	        //fos.write(byteData);
	        FileUtils.writeByteArrayToFile(tempFile, byteData);
	        Long attachmentId = createAttachmentFromFile(tempFile);
	        return attachmentId;
	    } catch(IOException e) {
	        e.printStackTrace();
	    }
	    return -1L;    // failure
	}
	
	public Long createAttachmentFromFile(File file) {
	    // create a project attachment from an existing file
	    ProjectAttachment pa = createProjectAttachment(file.getAbsolutePath());
	    return pa.getId();
	}
	
    private ProjectAttachment createProjectAttachment(String fileName) {    // TEST THIS FUNCTION returns pa with id;
        // create project attachment entity
        EntityManager em = entityManager.get();
        ProjectAttachment pa = new ProjectAttachment();
        pa.setUploadPath(fileName);
        pa.setUploadDate(new Date());
        
        em.persist(pa);
        return pa;
    }
	
    @LoggedIn
    public Project updateProject(String id, Project update) {
        EntityManager em = entityManager.get();
        try {
            Project p = em.find(Project.class, id);
            ProjectAccess pa = getProjectAccess(p.getId());
            if(!(pa.getAccessLevel().equals(AccessLevel.OWNER) || pa.getAccessLevel().equals(AccessLevel.READ_WRITE)))
                    return null; // unauthorised to modify project
            p.setName(update.getName());
            p.setVersion(update.getVersion());
            p.setDescription(update.getDescription());
            
            // persist changes
            em.persist(p);
            return p;
        } catch (NoResultException e) {
            System.err.println("No result for project id");
            e.printStackTrace();
            return null;
        }
    }
    
    @LoggedIn
    public Project changeProjectOwner(String id, String username) {
        // changes the displayed owner of the project
        EntityManager em = entityManager.get();
        // lookup project
        Project p = em.find(Project.class, id);
        // lookup userId
        User u = userController.lookupUserByUsername(username);
        if (u == null)
            return null;
        // get project permissions for current user
        ProjectAccess ca = getProjectAccess(p.getId());
        if(!(ca.getAccessLevel().equals(AccessLevel.OWNER)))
                return null; // unauthorised to modify project
        p.setOwner(u.getId());
        em.persist(p);
        // remove current authorisation for new user, replace with owner
        removeUserAuthorisation(p.getId(), u.getId());
        ProjectAccess pa = new ProjectAccess();
        pa.setOpen(false);
        pa.setProject(p);
        pa.setUserId(u.getId());
        pa.setAccessLevel(AccessLevel.OWNER);
        createUserAuthorisation(p.getId(), pa);
        
        return p;
    }
    
	@LoggedIn
    public int deleteProject(String id) {   // Time complexity: O(n^2)
        // Delete the project, and all associated resources.
        EntityManager em = entityManager.get();
        try {
            Project p = em.find(Project.class, id);
            // TODO Change permission lookup to within ProjectAccess (maybe)
            if(!identity.getAccount().getId().equals(p.getOwner()))
                return 401; // HTTP NOT AUTHORISED
            //TODO delete associated ProjectAccess
            TypedQuery<ProjectAccess> q = em.createQuery("SELECT pa FROM ProjectAccess pa WHERE pa.project = :project", ProjectAccess.class);
            q.setParameter("project", p);
            List<ProjectAccess> paList = q.getResultList();
            for(ProjectAccess pa : paList) {
                em.remove(pa);  // remove ProjectAccess from datastore 
            }
            //TODO delete associated ProjectResources, ResourceAccess and ResourceContent
            resourceController.deleteAllResources(p);
            // Finally, remove project.
            em.remove(p);
        } catch (NoResultException e) {
            System.err.println("Project resource not found - id: " + id);
            return 404;
        }
        return 200; // HTTP OK
    }
	
	@LoggedIn
	public ProjectAccess getProjectAccess(String id) {
	    // get the access level for the given project
	    EntityManager em = entityManager.get();
	    try {
	        Project p = em.find(Project.class, id);
	        String userId = identity.getAccount().getId();
	        TypedQuery<ProjectAccess> q = em.createQuery("SELECT pa FROM ProjectAccess pa WHERE pa.project = :project AND pa.userId = :userId", ProjectAccess.class);
	        q.setParameter("project", p);
	        q.setParameter("userId", userId);
	        ProjectAccess projectAccess = q.getSingleResult();
	        return projectAccess;
	    } catch (NoResultException e) {
	        System.err.println("Could not retrieve access level for Project " + id + "\n" + e);
	        return null;
	    }
	}
	
	@LoggedIn
    public ProjectAccess getUserAuthorisation(String projectId, String userId) {
        // get the access level for the given project
        EntityManager em = entityManager.get();
        try {
            Project p = em.find(Project.class, projectId);
            // if current user's access is denied, return null
            // TODO is there a way to throw PicketLink UNAUTHORISED
            if(getProjectAccess(p.getId()) == null)
                return null;
            TypedQuery<ProjectAccess> q = em.createQuery("SELECT pa FROM ProjectAccess pa WHERE pa.project = :project AND pa.userId = :userId", ProjectAccess.class);
            q.setParameter("project", p);
            q.setParameter("userId", userId);
            ProjectAccess projectAccess = q.getSingleResult();
            return projectAccess;
        } catch (NoResultException e) {
            System.err.println("Could not retrieve access level for Project " + projectId + "\n" + e);
            return null;
        }
    }
	@LoggedIn
    public int createUserAuthorisation(String projectId, ProjectAccess access) {
        // create project access for the given project and user
	    EntityManager em = entityManager.get();
        try {
            Project p = em.find(Project.class, projectId);
            // if current user's access is not owner, fail
            // TODO is there a way to throw PicketLink UNAUTHORISED
            if(getProjectAccess(p.getId()).getAccessLevel() != AccessLevel.OWNER)
                return 401;
            TypedQuery<ProjectAccess> q = em.createQuery("SELECT pa FROM ProjectAccess pa WHERE pa.project = :project AND pa.userId = :userId", ProjectAccess.class);
            q.setParameter("project", p);
            q.setParameter("userId", access.getUserId());
            if(q.getResultList().size() == 0) {
            	// userAuthorisation does not exist, create new.
                em.persist(access);
                // now create resourceAccess authorisation
                ResourceAccess.AccessLevel resourceAccess = null;
                switch (access.getAccessLevel()) {
                case OWNER:
                	resourceAccess = ResourceAccess.AccessLevel.OWNER;
                	break;
                case READ_WRITE:
                	resourceAccess = ResourceAccess.AccessLevel.READ_WRITE;
                	break;
                case READ:
                	resourceAccess = ResourceAccess.AccessLevel.READ;
                	break;
            	default:
                	// do nothing
                }
                resourceController.createUserAuthorisationForAll(p, access.getUserId(), resourceAccess);	// generate resourceAccess for all Resources
                return 201; // HTTP Created
            } else {
                /*
                // resource exists, update it instead
                int status = updateUserAuthorisation(projectId, userId, accessLevel);
                return status;
                */
                return 409; // HTTP Conflict
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 404;
    }

	@LoggedIn
    public int updateUserAuthorisation(String projectId, String userId, ProjectAccess access) {
        // update project access for the given project and user
        EntityManager em = entityManager.get();
        try {
            Project p = em.find(Project.class, projectId);
            // if current user's access is not owner, fail
            // TODO is there a way to throw PicketLink UNAUTHORISED
            if(getProjectAccess(p.getId()).getAccessLevel() != AccessLevel.OWNER)
                return 401;
            // now find the userAuthorisation object for the given userId and projectId
            TypedQuery<ProjectAccess> q = em.createQuery("SELECT pa FROM ProjectAccess pa WHERE pa.project = :project AND pa.userId = :userId", ProjectAccess.class);
            q.setParameter("project", p);
            q.setParameter("userId", userId);
            ProjectAccess pa = q.getSingleResult();
            // if current userId matches, update the current resource
            if(pa.getUserId().equals(access.getUserId())) {
            	pa.setAccessLevel(access.getAccessLevel());
            	em.persist(pa);
            	// now update all resource Authorisation for the user
            	ResourceAccess.AccessLevel resourceAccess = null;
                switch (access.getAccessLevel()) {
                case OWNER:
                    resourceAccess = ResourceAccess.AccessLevel.OWNER;
                    break;
                case READ_WRITE:
                    resourceAccess = ResourceAccess.AccessLevel.READ_WRITE;
                    break;
                case READ:
                    resourceAccess = ResourceAccess.AccessLevel.READ;
                    break;
                default:
                    // do nothing
                }
                resourceController.updateUserAuthorisationForAll(p, access.getUserId(), resourceAccess);    // generate resourceAccess for all Resources
                return 200;
            } else	// otherwise, bad request.
            	return 400;
            
        } catch (NoResultException e) {
            System.err.println("Could not find authorisation for user " + userId);
            e.printStackTrace();
        }
        return 404;
    }
	
	@LoggedIn
	public int removeUserAuthorisation(String projectId, String userId) {
        // update project access for the given project and user
        EntityManager em = entityManager.get();
        try {
            Project p = em.find(Project.class, projectId);
            // if current user's access is not owner, fail
            // TODO is there a way to throw PicketLink UNAUTHORISED
            if(getProjectAccess(p.getId()).getAccessLevel() != AccessLevel.OWNER)
                return 401;
            // now find the userAuthorisation object for the given userId and projectId
            TypedQuery<ProjectAccess> q = em.createQuery("SELECT pa FROM ProjectAccess pa WHERE pa.project = :project AND pa.userId = :userId", ProjectAccess.class);
            q.setParameter("project", p);
            q.setParameter("userId", userId);
            ProjectAccess pa = q.getSingleResult();
            // remove the access level
            em.remove(pa);
            // now remove access for all associated resources
            resourceController.removeUserAuthorisationForAll(p, userId);
            return 200; // HTTP OK
        } catch (NoResultException e) {
            System.err.println("Could not find authorisation for user " + userId);
        }
        return 404;
    }
}
