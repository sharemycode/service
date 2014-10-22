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
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;
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

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;

import org.picketlink.Identity;

/** 
 * Performs persistence operations for projects
 * 
 * @author Shane Bryzak
 * @author Lachlan Archibald
 */
@ApplicationScoped
public class ProjectController {
    /** Temporary storage location on disk
     *  in the directory where the WildFly server is executed */
    public static final String TEMP_STORAGE = "projectStorage/";
    public static final String ATTACHMENT_PATH = TEMP_STORAGE + "attachments/";
    public static final String PROJECT_PATH = TEMP_STORAGE + "projects/";
    public static final int MAX_UPLOAD = 10485760; // max upload filesize in bytes (10MB)

    @Inject
    private Instance<EntityManager> entityManager;

    @Inject
    Event<NewProjectEvent> newProjectEvent;

    @Inject
    Event<NewResourceEvent> newResourceEvent;
    @Inject
    UserController userController;
    @Inject
    ResourceController resourceController;

    @Inject
    private Identity identity;

    /** 
     * Lists all projects for the logged in user
     * @return List of Projects
     */
    @LoggedIn
    public List<Project> listAllProjects() {
        EntityManager em = entityManager.get();
        String userId = identity.getAccount().getId();
        TypedQuery<Project> q = em
                .createQuery("SELECT p FROM Project p, ProjectAccess pa "
                    + "WHERE p = pa.project AND pa.userId = :userId "
                    + "AND pa.accessLevel = :accessLevel", Project.class);
        q.setParameter("userId", userId);
        q.setParameter("accessLevel", AccessLevel.OWNER);
        return q.getResultList();
    }
    
    /** Lists all projects shared with the current user
     * - that is, current user has READ, READ_WRITE or RESTRICTED access
     * 
     * @return List of Projects
     */
    @LoggedIn
    public List<Project> listSharedProjects() {
        // returns a list of projects for which the user has access to
        EntityManager em = entityManager.get();
        String userId = identity.getAccount().getId();
        TypedQuery<Project> q = em
                .createQuery("SELECT p FROM Project p, ProjectAccess pa "
                    + "WHERE p = pa.project AND pa.userId = :userId "
                    + "AND pa.accessLevel = :read OR pa.accessLevel = :write "
                    + "OR pa.accessLevel = :restricted", Project.class);
        q.setParameter("userId", userId);
        q.setParameter("read", AccessLevel.READ);
        q.setParameter("write", AccessLevel.READ_WRITE);
        q.setParameter("restricted", AccessLevel.RESTRICTED);
        return q.getResultList();
    }

    /** 
     * Creates a new project with unique URL and persists
     * 
     * @param project Takes the project information from submitProject()
     * and adds server-defined attributes for persistence
     * @return Project
     */
    @LoggedIn
    public Project createProject(Project project) {
        // persist the project data
        project.setOwner(identity.getAccount().getId());
        EntityManager em = entityManager.get();
        Boolean uniqueUrl = false;
        String newUrl = null;
        while (!uniqueUrl) {
            newUrl = Project.generateURL();
            TypedQuery<Project> q = em
                    .createQuery("SELECT p FROM Project p WHERE p.url = :url",
                            Project.class);
            q.setParameter("url", newUrl);
            if (q.getResultList().size() == 0)
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
    
    /** 
     * Persists a new ProjectResource
     * @param resource  ProjectResource to be persisted
     * @return ProjectResource
     */
    @LoggedIn
    public ProjectResource createResource(ProjectResource resource) {
        // persist resource
        EntityManager em = entityManager.get();
        em.persist(resource);

        newResourceEvent.fire(new NewResourceEvent(resource));
        return resource;
    }

    /** 
     * Creates a new ResourceAccess object and persists
     * @param resource      ProjectResource to be associated
     * @param userId        User to be given access
     * @param accessLevel   Project AccessLevel to use
     * @return ResourceAccess
     */
    @LoggedIn
    public ResourceAccess createResourceAccess(ProjectResource resource,
            String userId, ResourceAccess.AccessLevel accessLevel) {
        EntityManager em = entityManager.get();
        ResourceAccess ra = new ResourceAccess();
        ra.setResource(resource);
        ra.setAccessLevel(accessLevel);
        ra.setUserId(userId);

        em.persist(ra);
        return ra;
    }

    /** 
     * Persists a ResourceContent object
     * @param resource  ProjectResource to be persisted
     * @return ProjectResource
     * @throws IOException
     */
    private ResourceContent createResourceContent(ProjectResource resource,
            String dataPath) throws IOException {
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

    /**
     * Returns a Project with given id
     * @param id String id
     * @return Project
     */
    public Project lookupProject(String id) {
        EntityManager em = entityManager.get();
        return em.find(Project.class, id);
    }

    /**
     * Returns a ProjectResource by name
     * @param project   associated project
     * @param parent    parentResource
     * @param name      name of the resource
     * @return          ProjectResource
     */
    public ProjectResource lookupResourceByName(Project project,
            ProjectResource parent, String name) {
        EntityManager em = entityManager.get();
        TypedQuery<ProjectResource> q = null;
        // if parent is null, search for IS_NULL property in database
        if (parent == null) { 
            q = em.createQuery("SELECT r FROM ProjectResource r " 
                        + "WHERE r.project = :project AND r.parent IS NULL "
                        + "AND r.name = :name", ProjectResource.class);
            q.setParameter("project", project);
            q.setParameter("name", name);
        } else {    // otherwise perform query as normal
            q = em.createQuery("SELECT r FROM ProjectResource r " 
                        + "WHERE r.project = :project AND r.parent = :parent "
                        + "AND r.name = :name", ProjectResource.class);
            q.setParameter("project", project);
            q.setParameter("parent", parent);
            q.setParameter("name", name);
        }

        try {
            return q.getSingleResult();
        } catch (NoResultException ex) {
            return null;
        }
    }
    
    /**
     * Lists projects from a search
     * Relates to current user
     * @param searchTerm String
     * @return List of Projects
     */
    public List<Project> listProjects(String searchTerm) {
        EntityManager em = entityManager.get();

        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<ProjectAccess> cq = cb.createQuery(ProjectAccess.class);
        Root<ProjectAccess> from = cq.from(ProjectAccess.class);
        Join<ProjectAccess, Project> project = from.join("project");

        List<Predicate> predicates = new ArrayList<Predicate>();
        // TODO test this function after enabling identity
        predicates.add(cb.equal(from.get("userId"),
                identity.getAccount().getId()));

        if (searchTerm != null && !"".equals(searchTerm)) {
            predicates.add(cb.like(cb.lower(project.get(Project_.name)), "%"
                    + searchTerm.toLowerCase() + "%"));
        }

        cq.where(predicates.toArray(new Predicate[predicates.size()]));

        TypedQuery<ProjectAccess> q = em.createQuery(cq);

        List<Project> projects = new ArrayList<Project>();
        for (ProjectAccess a : q.getResultList()) {
            projects.add(a.getProject());
        }

        return projects;
    }

    /**
     * Creates directory resources, files may not have existing data
     * @author Shane Bryzak
     * @param project Project to create directories for
     * @param directory Path to process
     * @return ProjectResource
     */
    public ProjectResource createDirStructure(Project project, String directory) {
        String[] parts = directory.split(ProjectResource.PATH_SEPARATOR);

        ProjectResource parent = null;
        for (int i = 0; i < parts.length; i++) {
            // If the first part is equal to the project name, ignore it
            if (i == 0 && parts[i].equals(project.getName())) {
                continue;
            }

            ProjectResource r = lookupResourceByName(project, parent, parts[i]);
            // If the directory resource doesn't exist, create it
            if (r == null) {
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

    /**
     * Creates a new package, not implemented
     * @author Shane Bryzak
     * @param project
     * @param folder
     * @param pkgName
     * @return ProjectResource
     */
    public ProjectResource createPackage(Project project,
            ProjectResource folder, String pkgName) {
        return null;
    }

    /**
     * Submits a new project JSON with attachments for creation
     * @author Lachlan Archibald
     * @param properties JSON Object data: name, version, description, attachments[]
     * @return Project
     */
    public Project submitProject(Map<String, Object> properties) {
        // First we test if the user entered non-unique username and email
        Project p = new Project();
        p.setName((String) properties.get("name"));
        p.setVersion((String) properties.get("version"));
        p.setDescription((String) properties.get("description"));
        @SuppressWarnings("unchecked")
        List<String> attachments = (List<String>) properties.get("attachments");
        Project result = this.createProject(p);
        // now create the resources from the attachments
        if (attachments != null) { // if JSON data does not include attachments
                                   // object, do not fail.
            if (attachments.size() > 0) {
                try {
                    this.createProjectResources(result, attachments, null);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    /**
     * Adds attachments to existing project, generate ProjectResources
     * @param p Project to add attachments to
     * @param attachments List of attachmentId Strings
     * @return Boolean, if an error occurred
     */
    public Boolean addAttachmentsToProject(Project p, List<String> attachments) {
        // add list of attachments to existing project's root
        if (attachments == null || attachments.size() > 0) {
            try {
                return this.createProjectResources(p, attachments, null);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // if we get to here, no attachments or error
        return false;
    }

    /** 
     * Parses Content-Disposition header to get the original file name 
     * Used in old multipart file upload
     * @deprecated
     * @param headers
     * @return String fileName
     */
    // 
    private String parseFileName(MultivaluedMap<String, String> headers) {
        String[] contentDispositionHeader = headers.getFirst(
                "Content-Disposition").split(";");
        for (String name : contentDispositionHeader) {
            if ((name.trim().startsWith("filename"))) {
                String[] tmp = name.split("=");

                String fileName = tmp[1].trim().replaceAll("\"", "");

                return fileName;
            }
        }
        return "unknownFile";
    }

    /** 
     * Saves uploaded file to a defined location on the server
     * @deprecated Used in old multipart file upload
     * @param uploadedInputStream
     * @param serverLocation
     */
    private void saveFile(InputStream uploadedInputStream, String serverLocation) {

        try {
            OutputStream outputStream = new FileOutputStream(new File(
                    serverLocation));
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

    /**
     * Extracts project files into temporary project directory
     * @author Lachlan Archibald
     * @param sourcePath    path to the zip file to be extracted
     * @param destPath      path to the destination folder to extract
     * @param password      password needed to extract zip (unused)
     * @return Boolean, if error occured returns false
     */
    private static Boolean unzipProject(String sourcePath, String destPath,
            String password) {
        // Takes the path to a project archive, and extracts to destination path
        // using zip4j
        try {
            ZipFile projectZip = new ZipFile(sourcePath);
            if (projectZip.isValidZipFile()) {
                if (projectZip.isEncrypted()) {
                    projectZip.setPassword(password);
                }
                projectZip.extractAll(destPath);
            } else {
                System.err
                        .println("Error: Attempted to process invalid zip file");
                return false;
            }
        } catch (ZipException e) {
            System.err.println("Error while processing zip archive");
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Creates resources from EXISTING project.
     * That is, already has byte data.
     * Removes temporary files from disk after operations
     * @author Lachlan Archibald
     * @param project       Project to create resources for
     * @param attachments   List<String> of attachments (Long, encoded as String)
     * @param parent        parent ProjectResource to create resources under
     * @return Boolean, returns false if error occurred
     * @throws IOException
     */
    @LoggedIn
    private Boolean createProjectResources(Project project,
            List<String> attachments, ProjectResource parent)
            throws IOException {
        EntityManager em = entityManager.get();
        // new method
        String userId = identity.getAccount().getId();
        // String userId = "TestingOnly";
        String tempProjectPath = PROJECT_PATH + userId
                + ProjectResource.PATH_SEPARATOR + project.getName();
        for (String attachment : attachments) {
            Long id = Long.valueOf(attachment); // convert value of String
                                                // representation to Long object
            ProjectAttachment pa = em.find(ProjectAttachment.class, id);
            if (pa.getUploadPath().endsWith(".zip")) {
                // attachment is a zip file, unzip it
                File zipFile = new File(pa.getUploadPath());
                if (!unzipProject(zipFile.getAbsolutePath(), tempProjectPath,
                        null)) {
                    System.err.println("Problem extracting file "
                            + zipFile.getName() + " to project directory.");
                    return false;
                }
                // now process the directory to create resources
                String startDirectory = tempProjectPath;
                File[] files = new File(startDirectory).listFiles();
                // if the only file in the root directory
                // is a directory with the same name as the project
                if(files.length == 1 && files[0].isDirectory()
                        && files[0].getName().equals(project.getName()) ) {
                    // skip this directory, start from the child
                    startDirectory = files[0].getAbsolutePath();
                }
                if (!processDirectory(project, startDirectory, parent))
                    return false;
            } else {
                // treat as a normal file
                if (!processFile(project, pa.getUploadPath(), parent))
                    return false;
            }
            // remove attachment files from disk
            System.out.println("AttachmentPath: " + pa.getUploadPath());
            pa.deleteAttachment();
            // delete files if not handled
        }
        // now remove all files on disk
        System.out.println("ProjectPath: " + tempProjectPath);
        FileUtils.deleteDirectory(new File(tempProjectPath).getParentFile());
        return true;
    }

    /** 
     * Processes a directory into ProjectResources
     * @param project       Project to generate ProjectResources for
     * @param currentDir    Current directory we are processing
     * @param parent        ProjectResource of parent directory
     * @return Boolean      true if successfully processed directory
     * @throws IOException
     */
    private Boolean processDirectory(Project project, String currentDir,
            ProjectResource parent) throws IOException {
        // return list of files in directory
        File[] files = new File(currentDir).listFiles();
        String dataPath = null; // path to file for extracting byte array
        for (File file : files) {
            String name = file.getName();
            ProjectResource r = lookupResourceByName(project, parent, name);
            if (r == null) { // if current resource does not exist, create it
                r = new ProjectResource();
                r.setProject(project);
                r.setName(name);
                r.setParent(parent);
                if (file.isDirectory()) { // if current file is a directory
                    r.setResourceType(ResourceType.DIRECTORY);
                    // create resource
                    createResource(r);

                    // For each user with access to the project, create
                    // ResourceAccess
                    createResourceAccessForAll(project, r);

                    // Also create resource from children files
                    String childDir = currentDir
                            + ProjectResource.PATH_SEPARATOR + name;
                    if (!processDirectory(project, childDir, r))
                        System.err.println("Error processing files in "
                                + childDir);
                } else {
                    r.setResourceType(ResourceType.FILE);
                    // create resource
                    createResource(r);

                    // For each user with access to the project, create
                    // ResourceAccess
                    createResourceAccessForAll(project, r);
                    // create resource content
                    dataPath = file.getAbsolutePath();
                    createResourceContent(r, dataPath);
                }
            }
        }
        return true;
    }
    
    /**
     * Processes an individual file, convert from file to ProjectResource
     * @param project   Project the resource belongs to
     * @param path      Path to the File
     * @param parent    ProjectResource parent
     * @return  Boolean, true if resource created successfully
     * @throws IOException
     */
    private boolean processFile(Project project, String path,
            ProjectResource parent) throws IOException {
        // process individual file
        File file = new File(path);
        String name = file.getName();
        ProjectResource r = lookupResourceByName(project, parent, name);
        if (r == null) {
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

    /**
     * Creates ResourceAccess permissions for resource,
     * for all users with ProjectAccess.
     * AccessLevel is determined by project accessLevel
     * @param project   Project to create permissions for
     * @param r         Resource to create permissions for
     */
    private void createResourceAccessForAll(Project project, ProjectResource r) {
        // For each user with access to project, create appropriate
        // ResourceAccess
        // By default, ResourceAccess.AccessLevel == ProjectAccess.AccessLevel
        EntityManager em = entityManager.get();
        TypedQuery<ProjectAccess> q = em.createQuery(
                "SELECT pa FROM ProjectAccess pa WHERE pa.project = :project",
                ProjectAccess.class);
        q.setParameter("project", project);
        List<ProjectAccess> paList = q.getResultList();
        for (ProjectAccess pa : paList) {
            switch (pa.getAccessLevel()) {
            case OWNER:
                createResourceAccess(r, pa.getUserId(),
                        ResourceAccess.AccessLevel.OWNER);
                break;
            case READ_WRITE:
                createResourceAccess(r, pa.getUserId(),
                        ResourceAccess.AccessLevel.READ_WRITE);
                break;
            case READ:
                createResourceAccess(r, pa.getUserId(),
                        ResourceAccess.AccessLevel.READ);
                break;
            case RESTRICTED:
                // create ResourceAccess for individual files, not bulk
            default:
                // do nothing
            }
        }

    }

    /**
     * Returns projects owned by a given username
     * Uses the project owner attribute
     * @param username String
     * @return List of Projects
     */
    // TODO test function
    public List<Project> listProjectsByOwner(String username) {
        EntityManager em = entityManager.get();
        User user = userController.lookupUserByUsername(username);
        TypedQuery<Project> q = em.createQuery(
                "SELECT p FROM Project p WHERE p.owner_id = :user",
                Project.class);
        q.setParameter("user", user.getId());
        return q.getResultList();
    }

    /** 
     * Creates attachment from REST endpoint method
     * @param name  name of the resource file to create
     * @param data  Base64encoded String data
     * @return Long attachmentId
     */
    public Long createAttachmentFromService(String name, String data) {
        String uploadDirectory = ATTACHMENT_PATH + System.currentTimeMillis() + "/";
        File tempFile = new File(uploadDirectory + name);
        try {
            byte[] byteData = Base64.decodeBase64(data);
            FileUtils.writeByteArrayToFile(tempFile, byteData);
            Long attachmentId = createAttachmentFromFile(tempFile);
            return attachmentId;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return -1L; // failure
    }

    /** 
     * Creates Attachment From a File already on the server,
     * used with the Java FileUpload servlet to process files into attachments
     * @param file File stored on server
     * @return Long attachmentId
     */
    public Long createAttachmentFromFile(File file) {
        // create a project attachment from an existing file
        ProjectAttachment pa = createProjectAttachment(file.getAbsolutePath());
        return pa.getId();
    }

    /** 
     * Creates ProjectAttachment entity and persist 
     * @param filePath  path to the file on the server
     * @return  ProjectAttachment entity   
     */
    private ProjectAttachment createProjectAttachment(String filePath) {
        // create project attachment entity
        EntityManager em = entityManager.get();
        ProjectAttachment pa = new ProjectAttachment();
        pa.setUploadPath(filePath);
        pa.setUploadDate(new Date());

        em.persist(pa);
        return pa;
    }

    /**
     * Updates Project information: name version and description.
     * Requires READ_WRITE permission
     * @param id        String id of project
     * @param update    Project entity containing updated attributes
     * @return  updated Project
     */
    @LoggedIn
    public Project updateProject(String id, Project update) {
        EntityManager em = entityManager.get();
        try {
            Project p = em.find(Project.class, id);
            ProjectAccess pa = getProjectAccess(p.getId());
            if (!(pa.getAccessLevel().equals(AccessLevel.OWNER) || pa
                    .getAccessLevel().equals(AccessLevel.READ_WRITE)))
                return null; // unauthorised to modify project
            if(update.getName() != null)
                p.setName(update.getName());
            if(update.getVersion() != null)
                p.setVersion(update.getVersion());
            if(update.getDescription() != null)
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

    /**
     * Updates the displayed owner of the project.
     * Requires OWNER access
     * @param id        String project id
     * @param username  String username to make the new owner
     * @return Project
     */
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
        if (!(ca.getAccessLevel().equals(AccessLevel.OWNER)))
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

    /**
     * Deletes the entire project by id.
     * Removes all associated ProjectAccess, ProjectResource and Project
     * @param id    String project id
     * @return  int status
     */
    @LoggedIn
    public int deleteProject(String id) {
        // Delete the project, and all associated resources.
        EntityManager em = entityManager.get();
        try {
            Project p = em.find(Project.class, id);
            // TODO Change permission lookup to within ProjectAccess (maybe)
            // if not displayed owner of project, deny
            if (!identity.getAccount().getId().equals(p.getOwner()))
                return 401; // HTTP NOT AUTHORISED
            // Remove all associated ProjectAccess
            TypedQuery<ProjectAccess> q = em
                    .createQuery("SELECT pa FROM ProjectAccess pa "
                    + "WHERE pa.project = :project", ProjectAccess.class);
            q.setParameter("project", p);
            List<ProjectAccess> paList = q.getResultList();
            for (ProjectAccess pa : paList) {
                em.remove(pa);
            }
            // delete associated ProjectResources, ResourceAccess and
            // ResourceContent
            resourceController.deleteAllResources(p);
            // Finally, remove project.
            em.remove(p);
        } catch (NoResultException e) {
            System.err.println("Project resource not found - id: " + id);
            return 404;
        }
        return 200; // HTTP OK
    }

    /**
     * Returns ProjectAccess for the project relating to current logged in user
     * 
     * @param id    String project id
     * @return ProjectAccess
     */
    @LoggedIn
    public ProjectAccess getProjectAccess(String id) {
        // get the access level for the given project
        EntityManager em = entityManager.get();
        try {
            Project p = em.find(Project.class, id);
            String userId = identity.getAccount().getId();
            TypedQuery<ProjectAccess> q = em
                    .createQuery("SELECT pa FROM ProjectAccess pa "
                        + "WHERE pa.project = :project "
                        + "AND pa.userId = :userId", ProjectAccess.class);
            q.setParameter("project", p);
            q.setParameter("userId", userId);
            ProjectAccess projectAccess = q.getSingleResult();
            return projectAccess;
        } catch (NoResultException e) {
            System.err.println("Could not retrieve access level for Project "
                    + id + "\n" + e);
            return null;
        }
    }

    /**
     * Gets the authorisation for a user to access a given project
     * 
     * @param projectId     String id of project
     * @param userId        String id of user
     * @return ProjectAccess
     */
    @LoggedIn
    public ProjectAccess getUserAuthorisation(String projectId, String userId) {
        // get the access level for the given project
        EntityManager em = entityManager.get();
        try {
            Project p = em.find(Project.class, projectId);
            // if current user's access is denied, return null
            // TODO is there a way to throw PicketLink UNAUTHORISED - not yet
            if (getProjectAccess(p.getId()) == null)
                return null;
            TypedQuery<ProjectAccess> q = em
                    .createQuery("SELECT pa FROM ProjectAccess pa "
                        + "WHERE pa.project = :project "
                        + "AND pa.userId = :userId", ProjectAccess.class);
            q.setParameter("project", p);
            q.setParameter("userId", userId);
            ProjectAccess projectAccess = q.getSingleResult();
            return projectAccess;
        } catch (NoResultException e) {
            System.err.println("Could not retrieve access level for Project "
                    + projectId + "\n" + e);
            return null;
        }
    }

    /**
     * Creates authorisation for a user to access a given project
     * 
     * @param projectId String id of project
     * @param access ProjectAccess entity containing project, userId and accessLevel
     * 
     * @return int status 201 if created
     */
    @LoggedIn
    public int createUserAuthorisation(String projectId, ProjectAccess access) {
        // create project access for the given project and user
        EntityManager em = entityManager.get();
        try {
            Project p = em.find(Project.class, projectId);
            // if current user's access is not owner, fail
            // TODO is there a way to throw PicketLink UNAUTHORISED - not yet
            if (getProjectAccess(p.getId()).getAccessLevel() != AccessLevel.OWNER)
                return 401;
            TypedQuery<ProjectAccess> q = em
                    .createQuery("SELECT pa FROM ProjectAccess pa "
                    + "WHERE pa.project = :project "
                    + "AND pa.userId = :userId", ProjectAccess.class);
            q.setParameter("project", p);
            q.setParameter("userId", access.getUserId());
            if (q.getResultList().size() == 0) {
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
                // generate resourceAccess for all Resources
                resourceController.createUserAuthorisationForAll(p,
                        access.getUserId(), resourceAccess); 
                return 201; // HTTP Created
            } else {
                // resource exists, update it instead
                // int status = updateUserAuthorisation(projectId, userId, accessLevel);
                // return status;
     
                return 409; // HTTP Conflict
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 404;
    }

    /**
     * Updates the authorisation for a user to access a project
     * @param projectId String id of project
     * @param userId    String id of user
     * @param access    ProjectAccess containing updated information
     * @return int status 200 if successful
     */
    @LoggedIn
    public int updateUserAuthorisation(String projectId, String userId,
            ProjectAccess access) {
        // update project access for the given project and user
        EntityManager em = entityManager.get();
        try {
            Project p = em.find(Project.class, projectId);
            // if current user's access is not owner, fail
            // TODO is there a way to throw PicketLink UNAUTHORISED - not yet
            if (getProjectAccess(p.getId()).getAccessLevel() != AccessLevel.OWNER)
                return 401;
            // now find the userAuthorisation object for the given userId and
            // projectId
            TypedQuery<ProjectAccess> q = em
                    .createQuery("SELECT pa FROM ProjectAccess pa "
                        + "WHERE pa.project = :project "
                        + "AND pa.userId = :userId", ProjectAccess.class);
            q.setParameter("project", p);
            q.setParameter("userId", userId);
            ProjectAccess pa = q.getSingleResult();
            // if current userId matches, update the current resource
            if (pa.getUserId().equals(access.getUserId())) {
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
                // generate resourceAccess for all Resources
                resourceController.updateUserAuthorisationForAll(p,
                        access.getUserId(), resourceAccess);
                return 200;
            } else
                // otherwise, bad request.
                return 400;

        } catch (NoResultException e) {
            System.err.println("Could not find authorisation for user "
                    + userId);
            e.printStackTrace();
        }
        return 404;
    }

    /**
     * Removes authorisation for user to access a project
     * @param projectId String project id
     * @param userId    String user id
     * @return int status 200 if successful
     */
    @LoggedIn
    public int removeUserAuthorisation(String projectId, String userId) {
        // update project access for the given project and user
        EntityManager em = entityManager.get();
        try {
            Project p = em.find(Project.class, projectId);
            // if current user's access is not owner, fail
            // TODO is there a way to throw PicketLink UNAUTHORISED - not yet.
            if (getProjectAccess(p.getId()).getAccessLevel() != AccessLevel.OWNER)
                return 401;
            // now find the userAuthorisation object for the given userId and
            // projectId
            TypedQuery<ProjectAccess> q = em
                    .createQuery("SELECT pa FROM ProjectAccess pa "
                        + "WHERE pa.project = :project "
                        + "AND pa.userId = :userId", ProjectAccess.class);
            q.setParameter("project", p);
            q.setParameter("userId", userId);
            ProjectAccess pa = q.getSingleResult();
            // remove the access level
            em.remove(pa);
            // now remove access for all associated resources
            resourceController.removeUserAuthorisationForAll(p, userId);
            return 200; // HTTP OK
        } catch (NoResultException e) {
            System.err.println("Could not find authorisation for user "
                    + userId);
        }
        return 404;
    }

    /**
     * Returns a project zip archive as byte array
     * 
     * @param p Project to download
     * @return byte[]   byte data of project zip file
     */
    @LoggedIn
    public byte[] fetchProject(Project p) {
        // convert project resources into file, zip and download.
        ProjectAccess access = getProjectAccess(p.getId());
        if (!(access.getAccessLevel().equals(AccessLevel.OWNER)
                || access.getAccessLevel().equals(AccessLevel.READ_WRITE) || access
                .getAccessLevel().equals(AccessLevel.READ)))
            return null; // unauthorised
        List<ProjectResource> resources = resourceController.listResources(p, 1);
        // create temp directory
        String tempDirectory = PROJECT_PATH + System.currentTimeMillis();
        String projectDir = tempDirectory + "/" + p.getName();
        File cDir = new File(projectDir);
        cDir.mkdirs();
        // convert project resources into files
        for (ProjectResource r : resources) {
            if (r.getParent() == null)  // (listResources should have returned only root resources)
                if (r.getResourceType().equals(ResourceType.DIRECTORY))
                    directoryToFiles(r, projectDir);
                else
                    resourceToFile(r, projectDir);
        }
        // zip all project files
        try {
            ZipFile zip = new ZipFile(projectDir + "/" + p.getName() + "_"
                    + p.getVersion());
            ZipParameters parameters = new ZipParameters();
            parameters.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_NORMAL);
            parameters.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
            zip.addFolder(projectDir, parameters);
            byte[] data = FileUtils.readFileToByteArray(zip.getFile());
            return data;
        } catch (ZipException e) {
            System.err.println("Exception occured: " + e);
        } catch (IOException e) {
            System.err.println("Exception occured: " + e);
        }
        // if we get to this point, a problem occurred.
        return null;
    }

    /**
     * Processes a directory ProjectResource into Files
     * 
     * @param parent ProjectResource directory to process
     * @param parentDirectory Directory on disk to place files   
     */
    private void directoryToFiles(ProjectResource parent, String parentDirectory) {
        // convert the current directory's resources to files
        String currentDirectory = parentDirectory + "/" + parent.getName();
        File directory = new File(currentDirectory);
        directory.mkdir();
        List<ProjectResource> resources = resourceController
                .listChildResources(parent);
        for (ProjectResource r : resources) {
            if (r.getResourceType().equals(ResourceType.DIRECTORY))
                directoryToFiles(r, currentDirectory);
            else
                resourceToFile(r, currentDirectory);
        }
    }

    /**
     * Processes a file ProjectResource into a File on disk
     * 
     * @param r ProjectResource file to process
     * @param directory Directory on disk to place file
     */
    private void resourceToFile(ProjectResource r, String directory) {
        // convert resource into file
        try {
            byte[] data = resourceController.getResourceContent(r).getContent();
            FileUtils.writeByteArrayToFile(
                    new File(directory + "/" + r.getName()), data);
        } catch (IOException e) {
            System.err.println("Exception occured: " + e);
        }
    }
}
