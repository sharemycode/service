package net.sharemycode.service.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import net.sharemycode.service.ProjectServices;
import net.sharemycode.service.events.NewProjectEvent;
import net.sharemycode.service.events.NewResourceEvent;
//import net.sharemycode.service.security.annotations.LoggedIn;
import net.sharemycode.service.model.Project;
import net.sharemycode.service.model.ProjectAccess;
import net.sharemycode.service.model.ProjectAccess.AccessLevel;
import net.sharemycode.service.model.ProjectResource;
import net.sharemycode.service.model.ProjectResource.ResourceType;
import net.sharemycode.service.model.Project_;
import net.sharemycode.service.model.ResourceContent;

import org.picketlink.Identity;
import org.picketlink.common.properties.Property;
import org.picketlink.idm.jpa.annotations.Identifier;
import org.picketlink.idm.jpa.annotations.PartitionClass;
import org.picketlink.idm.model.Partition;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.core.ZipFile;

/**
 * Performs persistence operations for projects
 *
 * @author Shane Bryzak and Lachlan Archibald
 */
@ApplicationScoped
public class ProjectController
{
   @Inject
   private Instance<EntityManager> entityManager;

   @Inject Event<NewProjectEvent> newProjectEvent;

   @Inject Event<NewResourceEvent> newResourceEvent;

   @Inject
   private Identity identity;
   
  // @LoggedIn
   public void createProject(Project project) {
	   /*
	  // persist the project data 
	  EntityManager em = entityManager.get();
      em.persist(project);
      
      // set the project access
      ProjectAccess pa = new ProjectAccess();
      pa.setProject(project);
      pa.setAccessLevel(AccessLevel.OWNER);
      pa.setOpen(true);
      pa.setUserId(identity.getAccount().getId());
      em.persist(pa);
      */
      // extract the project temp files
      unzipProject(project.getFilePath(), ProjectServices.TEMP_PROJECT_PATH + project.getName(), null);
      // create resources from project
      try {
		createProjectResources(project, ProjectServices.TEMP_PROJECT_PATH + project.getName());
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

   public Project lookupProject(Long id)
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

      predicates.add(cb.equal(from.get("userId"), identity.getAccount().getId()));

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
         // If the directory resource doesn't exist, create it
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
	   File[] files = new File(projectLocation).listFiles();	// return list of files in directory
	   ProjectResource parent = null;
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
			   } else {
				   r.setResourceType(ResourceType.FILE);
				   createResource(r);
				   dataPath = file.getAbsolutePath();
				   createResourceContent(r, dataPath);
			   }
		   }
		   parent = r;
	   }
	   return true;
   }
}

