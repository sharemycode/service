package net.sharemycode.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
import net.sharemycode.model.ProjectResource;
import net.sharemycode.model.ResourceAccess;
import net.sharemycode.model.ResourceContent;

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
}
