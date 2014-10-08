package net.sharemycode.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

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

import net.sharemycode.events.NewProjectEvent;
import net.sharemycode.events.NewResourceEvent;
import net.sharemycode.model.Project;
import net.sharemycode.model.ProjectAccess;
import net.sharemycode.model.ProjectResource;
import net.sharemycode.model.ResourceAccess;
import net.sharemycode.model.ResourceContent;
import net.sharemycode.model.ProjectResource.ResourceType;
import net.sharemycode.model.ResourceAccess.AccessLevel;
import net.sharemycode.security.annotations.LoggedIn;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.picketlink.Identity;

@ApplicationScoped
public class ResourceController {
	
	@Inject
    private Instance<EntityManager> entityManager;

    @Inject Event<NewProjectEvent> newProjectEvent;

    @Inject Event<NewResourceEvent> newResourceEvent;

    @Inject
    private Identity identity;

    public ProjectResource lookupResource(Long id)
    {
        EntityManager em = entityManager.get();
        return em.find(ProjectResource.class, id);
    }
    
    public List<ProjectResource> listAllResources() {
    	EntityManager em = entityManager.get();
    	TypedQuery<ProjectResource> q = em.createQuery("select r from ProjectResource r", ProjectResource.class);
        return q.getResultList();
    }
    
    public List<ProjectResource> listResources(Project project) {
        // List ProjectResources associated with a Project 
        TypedQuery<ProjectResource> q = entityManager.get().<ProjectResource>createQuery("select r from ProjectResource r where r.project = :project", 
                ProjectResource.class);
        q.setParameter("project", project);
        return q.getResultList();
    }
    
    //@LoggedIn
    public int deleteResource(Long id) {
        // Delete individual ProjectResource by id
        EntityManager em = entityManager.get();
        try {
            ProjectResource pr = this.lookupResource(id);
            // TODO Lookup permissions
            // Also delete associated ResourceContent and all ResourceAccess
            this.deleteAllResourceAccess(pr);
            this.deleteResourceContent(pr);
            em.remove(pr);  // remove ResourceAccess from datastore
            return 200; // HTTP OK
        } catch (NoResultException e) {
            System.err.println("Specific ProjectResource not found - id: " + id);
            return 404; // HTTP 404 NOT FOUND
        }
    }
    
    public int deleteAllResources(Project p) {
        // Delete ALL resoruces associated with a project
        EntityManager em = entityManager.get();
        List<ProjectResource> prList = this.listResources(p);
        if(prList.size() == 0)
            return 404; // HTTP 404 resources not found
        for(ProjectResource pr : prList) {
            // remove all ResourceAccess and ResourceContent
            this.deleteAllResourceAccess(pr);
            this.deleteResourceContent(pr);
            em.remove(pr);  // remove ResourceAccess from datastore
        }
        return 200; // HTTP OK
    }

    public int deleteResourceAccess(Long id) {
        // Delete individual ResourceAccess by id
        EntityManager em = entityManager.get();
        try {
            ResourceAccess ra = em.find(ResourceAccess.class, id);
            em.remove(ra);// remove ResourceAccess from datastore
            return 200; // HTTP OK
        } catch (NoResultException e) {
            System.err.println("Specific ResourceAccess not found");
            return 404; // HTTP NOT FOUND
        }
        
        
    }
    
    public int deleteAllResourceAccess(ProjectResource pr) {
        // Delete ALL associated ResourceAccess entities for ProjectResource
        EntityManager em = entityManager.get();
        TypedQuery<ResourceAccess> qResourceAccess = em.createQuery("SELECT ra FROM ResourceAccess WHERE p.resource = :resource", ResourceAccess.class);
        qResourceAccess.setParameter("resource", pr);
        List<ResourceAccess> raList = qResourceAccess.getResultList();
        if(raList.size() == 0)
            return 404; // HTTP NOT FOUND
        for(ResourceAccess ra : raList) {
            em.remove(ra);  // delete ResourceAccess from datastore
        }
        return 200; // HTTP OK
    }

    public int deleteResourceContent(ProjectResource resource) {
        EntityManager em = entityManager.get();
        try {
            TypedQuery<ResourceContent> q = em.createQuery("SELECT rc FROM ResourceContent rc WHERE rc.resource = :resource", ResourceContent.class);
            q.setParameter("resource", resource);
            ResourceContent content = q.getSingleResult();
            em.remove(content);
            return 200; // HTTP OK
        } catch (NoResultException e) {
            System.err.println("NoResultException: ResourceContent not found");
            return 404; // HTTP NOT FOUND
        }
    }

    public ResourceContent getResourceContent(ProjectResource resource) {
        // TODO Auto-generated method stub
        EntityManager em = entityManager.get();
        try {
            TypedQuery<ResourceContent> q = em.createQuery("SELECT rc FROM ResourceContent rc WHERE rc.resource = :resource", ResourceContent.class);
            q.setParameter("resource", resource);
            ResourceContent content = q.getSingleResult();
            return content;
        } catch (NoResultException e) {
            System.err.println("ResourceContent not found " + e);
        }
        return null;
    }
    
    @LoggedIn
    public ResourceAccess getResourceAccess(Long id) {
        // get the access level for the given project
        EntityManager em = entityManager.get();
        try {
            ProjectResource r = em.find(ProjectResource.class, id);
            String userId = identity.getAccount().getId();
            TypedQuery<ResourceAccess> q = em.createQuery("SELECT ra FROM ResourceAccess ra WHERE ra.resource = :resource AND ra.userId = :userId", ResourceAccess.class);
            q.setParameter("resource", r);
            q.setParameter("userId", userId);
            ResourceAccess resourceAccess = q.getSingleResult();
            return resourceAccess;
        } catch (NoResultException e) {
            System.err.println("Could not retrieve access level for Resource " + id + "\n" + e);
            return null;
        }
    }
    
    @LoggedIn
    public ResourceAccess getUserAuthorisation(Long resourceId, String userId) {
        // get the access level for the given resource
        EntityManager em = entityManager.get();
        try {
            ProjectResource r = em.find(ProjectResource.class, resourceId);
            // if current user's access is restricted, return null
            // TODO is there a way to throw PicketLink UNAUTHORISED
            if(getResourceAccess(r.getId()) == null)
                return null;
            TypedQuery<ResourceAccess> q = em.createQuery("SELECT ra FROM ResourceAccess ra WHERE ra.resource = :resource AND ra.userId = :userId", ResourceAccess.class);
            q.setParameter("resource", r);
            q.setParameter("userId", userId);
            ResourceAccess resourceAccess = q.getSingleResult();
            return resourceAccess;
        } catch (NoResultException e) {
            System.err.println("Could not retrieve access level for Resource " + resourceId + "\n" + e);
            return null;
        }
    }
   
    @LoggedIn
    public int createUserAuthorisation(Long resourceId, ResourceAccess access) {
        // create project access for the given resource and user
        EntityManager em = entityManager.get();
        try {
            ProjectResource r = em.find(ProjectResource.class, resourceId);
            
            // if current user's access is not owner, fail
            // TODO is there a way to throw PicketLink UNAUTHORISED
            /*  // Option 1: Owns project
            Project p = r.getProject();
            if(projectController.getProjectAccess(p.getId()).getAccessLevel() != AccessLevel.OWNER)
                return null;
            */
            // Option 2: Owns resource
            if(getResourceAccess(r.getId()).getAccessLevel() != AccessLevel.OWNER)
                return 401;
            TypedQuery<ResourceAccess> q = em.createQuery("SELECT ra FROM ResourceAccess ra WHERE ra.resource = :resource AND ra.userId = :userId", ResourceAccess.class);
            q.setParameter("resource", r);
            q.setParameter("userId", access.getUserId());
            if(q.getResultList().size() == 0) {
                // userAuthorisation does not exist, create new.
                em.persist(access);
                return 201;
            } else {
                // resource already exists, return HTTP Conflict
                return 409;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 404;
    }
    
    @LoggedIn
    public int updateUserAuthorisation(Long resourceId, String userId, ResourceAccess access) {
        // update project access for the given project and user
        EntityManager em = entityManager.get();
        try {
            ProjectResource r = em.find(ProjectResource.class, resourceId);
            // if current user's access is not owner, fail
            // TODO is there a way to throw PicketLink UNAUTHORISED
            /*  // Option 1: Owns project
            Project p = r.getProject();
            if(projectController.getProjectAccess(p.getId()).getAccessLevel() != AccessLevel.OWNER)
                return null;
            */
            // Option 2: Owns resource
            if(getResourceAccess(r.getId()).getAccessLevel() != AccessLevel.OWNER)
                return 401;
            TypedQuery<ResourceAccess> q = em.createQuery("SELECT ra FROM ResourceAccess ra WHERE ra.resource = :resource AND ra.userId = :userId", ResourceAccess.class);
            q.setParameter("resource", r);
            q.setParameter("userId", userId);
            ResourceAccess ra = q.getSingleResult();
            // update the AccessLevel and persist.
            if(ra.getUserId().equals(access.getUserId())) {
                ra.setAccessLevel(access.getAccessLevel());
                em.persist(ra);
                return 200;
            } else  // otherwise, bad request.
                return 400;
        } catch (NoResultException e) {
            System.err.println("Could not find authorisation for user " + userId);
            e.printStackTrace();
        }
        return 404;
    }
    
    @LoggedIn
    public int removeUserAuthorisation(Long resourceId, String userId) {
        // update project access for the given project and user
        EntityManager em = entityManager.get();
        try {
            ProjectResource r = em.find(ProjectResource.class, resourceId);
            // if current user's access is not owner, fail
            // TODO is there a way to throw PicketLink UNAUTHORISED
            /*  // Option 1: Owns project
            Project p = r.getProject();
            if(projectController.getProjectAccess(p.getId()).getAccessLevel() != AccessLevel.OWNER)
                return null;
            */
            // Option 2: Owns resource
            if(getResourceAccess(r.getId()).getAccessLevel() != AccessLevel.OWNER)
                return 401; // HTTP Unauthorised
            TypedQuery<ResourceAccess> q = em.createQuery("SELECT ra FROM ResourceAccess ra WHERE ra.resource = :resource AND ra.userId = :userId", ResourceAccess.class);
            q.setParameter("resource", r);
            q.setParameter("userId", userId);
            ResourceAccess ra = q.getSingleResult();
            // remove the AccessLevel
            em.remove(ra);
            return 200; // HTTP OK
        } catch (NoResultException e) {
            System.err.println("Could not find authorisation for user " + userId);
            e.printStackTrace();
        }
        return 404; // HTTP NOT FOUND
    }

	public Boolean createUserAuthorisationForAll(Project p, String userId,
			AccessLevel resourceAccess) {
		if (p == null || userId == null || resourceAccess == null)
			return false;
		List<ProjectResource> resources = listResources(p);
		for(ProjectResource r : resources) {
			ResourceAccess access = new ResourceAccess();
			access.setResource(r);
			access.setUserId(userId);
			access.setAccessLevel(resourceAccess);
			createUserAuthorisation(r.getId(), access);
		}
		return true;
	}

    public Boolean updateUserAuthorisationForAll(Project p, String userId,
            AccessLevel resourceAccess) {
        if (p == null || userId == null || resourceAccess == null)
            return false;
        List<ProjectResource> resources = listResources(p);
        for(ProjectResource r : resources) {
            ResourceAccess access = new ResourceAccess();
            access.setResource(r);
            access.setUserId(userId);
            access.setAccessLevel(resourceAccess);
            updateUserAuthorisation(r.getId(), userId, access);
        }
        return true;
        
    }
    
    public Boolean removeUserAuthorisationForAll(Project p, String userId) {
        if (p == null || userId == null)
            return false;
        List<ProjectResource> resources = listResources(p);
        for(ProjectResource r : resources) {
            removeUserAuthorisation(r.getId(), userId);
        }
        return true;
        
    }
    /*
    // TODO publishResource via path 
    @LoggedIn
    public ProjectResource publishResource(Project p, String resourcePath,
            String data) {
        if(p == null)
            return null; //error
        String[] parts = resourcePath.split(ProjectResource.PATH_SEPARATOR);
        ProjectResource r = null;
        ProjectResource parent = null;
        int i;
        // create directory resources
        for(i = 0; i < parts.length -1; i++) {
            if(i == 0 && parts[i].equals(p.getName()))
                continue;   // if first directory is same as project name, ignore
            r = lookupResourceByName(p, parent, parts[i]);
            if(r == null) { // create directory
                r = new ProjectResource();
                r.setName(parts[i]);
                r.setParent(parent);
                r.setProject(p);
                r.setResourceType(ResourceType.DIRECTORY);
                createResource(r);
                parent = r;
            }
        }
        // now create file resource
        r = lookupResourceByName(p, parent, parts[i]);
        if(r == null) { // create resource
            r = new ProjectResource();
            r.setName(parts[i]);
            r.setParent(parent);
            r.setProject(p);
            r.setResourceType(ResourceType.FILE);
            createResource(r);
            
            // create Resource Content
            try {
                createResourceContent(r, data);
                return r;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
    */
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
    
    @LoggedIn
    public ProjectResource createResource(ProjectResource resource) {
        // persist resource
        EntityManager em = entityManager.get();
        em.persist(resource);

        newResourceEvent.fire(new NewResourceEvent(resource));
        return resource;
    }
    
    @LoggedIn
    public ResourceContent createResourceContent(ProjectResource resource, String data) throws IOException {
        EntityManager em = entityManager.get();
        ResourceAccess access = getUserAuthorisation(resource.getId(), identity.getAccount().getId());
        if(access.getAccessLevel().equals(AccessLevel.OWNER) || access.getAccessLevel().equals(AccessLevel.READ_WRITE)) {
            // extract data from file
            //Path path = Paths.get(dataPath);
            byte[] byteData = Base64.decodeBase64(data);
            // create Resource Content
            ResourceContent content = new ResourceContent();
            content.setResource(resource);
            content.setContent(byteData);
    
            em.persist(content);
            return content;
        } else
            return null;    // unauthorised
    }

    @LoggedIn
    public ProjectResource publishResource(ProjectResource r) {
        String userId = identity.getAccount().getId();
        createResource(r);
        // create resource access for current user
        createResourceAccess(r, userId, AccessLevel.OWNER);
        // and create resource access for project owner
        Project p = r.getProject();
        if(!userId.equals(p.getOwner()))
            createResourceAccess(r, p.getOwner(), AccessLevel.OWNER);
        
        return r;
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
}
