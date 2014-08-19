package net.sharemycode.service.controller;

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

//import net.sharemycode.service.security.annotations.LoggedIn;
import net.sharemycode.service.model.Project;
import net.sharemycode.service.model.ProjectAccess;
import net.sharemycode.service.model.ProjectAccess.AccessLevel;
import net.sharemycode.service.model.ProjectResource;
import net.sharemycode.service.model.ProjectResource.ResourceType;
import net.sharemycode.service.model.ResourceContent;

import org.picketlink.Identity;
import org.picketlink.common.properties.Property;
import org.picketlink.idm.jpa.annotations.Identifier;
import org.picketlink.idm.jpa.annotations.PartitionClass;
import org.picketlink.idm.model.Partition;

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

   //@Inject Event<NewProjectEvent> newProjectEvent;

  // @Inject Event<NewResourceEvent> newResourceEvent;

   @Inject
   private Identity identity;
   
  // @LoggedIn
   public void createProject(Project project)
   {
      EntityManager em = entityManager.get();
      em.persist(project);

      ProjectAccess pa = new ProjectAccess();
      pa.setProject(project);
      pa.setAccessLevel(AccessLevel.OWNER);
      pa.setOpen(true);
      pa.setUserId(identity.getAccount().getId());

      em.persist(pa);

    //  newProjectEvent.fire(new NewProjectEvent(project));
   }

 //  @LoggedIn
   public void createResource(ProjectResource resource)
   {
      EntityManager em = entityManager.get();
      em.persist(resource);

      ResourceContent content = new ResourceContent();
      content.setResource(resource);
      content.setContent(new byte[0]);

      em.persist(content);

    //  newResourceEvent.fire(new NewResourceEvent(resource));
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
}

