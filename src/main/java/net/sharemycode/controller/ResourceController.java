package net.sharemycode.controller;

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

import org.picketlink.Identity;

@ApplicationScoped
public class ResourceController {
	
	@Inject
    private Instance<EntityManager> entityManager;

    @Inject Event<NewProjectEvent> newProjectEvent;

    @Inject Event<NewResourceEvent> newResourceEvent;

    @Inject
    private Identity identity;

    public List<ProjectResource> listAllResources() {
    	EntityManager em = entityManager.get();
    	TypedQuery<ProjectResource> q = em.createQuery("select r from ProjectResource r", ProjectResource.class);
        return q.getResultList();
    }
}
