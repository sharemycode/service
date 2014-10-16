package net.sharemycode.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

import net.sharemycode.events.NewProjectEvent;
import net.sharemycode.events.NewResourceEvent;
import net.sharemycode.model.Project;
import net.sharemycode.model.ProjectResource;
import net.sharemycode.model.ProjectResource.ResourceType;
import net.sharemycode.model.ResourceAccess;
import net.sharemycode.model.ResourceAccess.AccessLevel;
import net.sharemycode.model.ResourceContent;
import net.sharemycode.security.annotations.LoggedIn;

import org.apache.commons.codec.binary.Base64;
import org.picketlink.Identity;

/** Performs persistence operations for ProjectResources
 * 
 * @author Lachlan Archibald
 */
@ApplicationScoped
public class ResourceController {

    @Inject
    private Instance<EntityManager> entityManager;

    @Inject
    Event<NewProjectEvent> newProjectEvent;

    @Inject
    Event<NewResourceEvent> newResourceEvent;

    /** Logged in User's PicketLink identity */
    @Inject
    private Identity identity;

    /**
     * Lookup ProjectResource by id
     * 
     * @param id    Long id of ProjectResource
     * @return ProjectResource
     */
    public ProjectResource lookupResource(Long id) {
        EntityManager em = entityManager.get();
        return em.find(ProjectResource.class, id);
    }

    /**
     * List all resources for all projects
     * @deprecated this function was only for testing, should not be used.
     * @return List of ProjectResources
     */
    @Deprecated
    public List<ProjectResource> listAllResources() {
        EntityManager em = entityManager.get();
        TypedQuery<ProjectResource> q = em.createQuery(
                "select r from ProjectResource r", ProjectResource.class);
        return q.getResultList();
    }

    /**
     * List all resources of a project,
     * use root=1 to limit to only root resources (no parent)
     * @param project   Project to obtain resources for
     * @param root
     * if root=1 or non-zero, list only resources with no parent;
     * if root=0 or undefined, list all resources
     * @return List of ProjectResources
     */
    public List<ProjectResource> listResources(Project project, int root) {
        // List ProjectResources associated with a Project
        EntityManager em = entityManager.get();
        TypedQuery<ProjectResource> q = em.createQuery(
                "select r from ProjectResource r where r.project = :project",
                ProjectResource.class);
        q.setParameter("project", project);
        if (root == 0)  // if not only root directory
            return q.getResultList();
        // if root is non-zero, return only root resources
        List<ProjectResource> queryResult = q.getResultList();
        List<ProjectResource> resources = new ArrayList<ProjectResource>();
        for(ProjectResource r : queryResult) {
            if (r.getParent() == null)
                resources.add(r);
        }
        return resources;
    }

    /**
     * List ChildResources of a ProjectResource
     * 
     * @param parent    ProjectResource parent
     * @return List of ProjectResources
     */
    public List<ProjectResource> listChildResources(ProjectResource parent) {
        // List ProjectResources with parent ProjectResource
        EntityManager em = entityManager.get();
        TypedQuery<ProjectResource> q = em.createQuery(
                "select r from ProjectResource r where r.parent = :parent",
                ProjectResource.class);
        q.setParameter("parent", parent);
        return q.getResultList();
    }

    /**
     * Delete a single ProjectResource, and all children resources and associated ResourceAccess.
     * Handles child ProjectResources recursively.
     * Does not lookup permissions for Resource yet
     * 
     * @param pr    ProjectResource to delete
     * @return int status, 200 if successful
     */
    @LoggedIn
    public int deleteResource(ProjectResource pr) {
        // Delete individual ProjectResource by id
        EntityManager em = entityManager.get();
        try {
            // TODO Lookup permissions
            // Also delete associated ResourceContent and all ResourceAccess
            if (pr.getResourceType().equals(ResourceType.DIRECTORY)) {
                List<ProjectResource> children = listChildResources(pr);
                for (ProjectResource r : children)
                    deleteResource(r);
                deleteAllResourceAccess(pr);
            } else {
                deleteAllResourceAccess(pr);
                deleteResourceContent(pr);
            }
            em.remove(pr); // remove ResourceAccess from datastore
            return 200; // HTTP OK
        } catch (NoResultException e) {
            System.err.println("Exception occurred: " + e);
            return 404; // HTTP 404 NOT FOUND
        }
    }

    /**
     * Delete all ProjectResources for a given Project.
     * Gets all root resources, then handles children recursively
     * @param p Project
     * @return int status, 200 if successful
     */
    public int deleteAllResources(Project p) {
        // Delete ALL resoruces associated with a project
        List<ProjectResource> prList = this.listResources(p, 1);
        if (prList.size() == 0)
            return 404; // HTTP 404 resources not found
        for (ProjectResource pr : prList) {
            if (pr.getParent() != null) // child resources should be handled via recursion
                continue;
            deleteResource(pr);
        }
        return 200; // HTTP OK
    }

    /**
     * Delete all ResourceAccess for a ProjectResource
     * 
     * @param pr ProjectResource to remove authorisation for
     * 
     * @return int status, 200 if successful
     */
    public int deleteAllResourceAccess(ProjectResource pr) {
        // Delete ALL associated ResourceAccess entities for ProjectResource
        EntityManager em = entityManager.get();
        TypedQuery<ResourceAccess> qResourceAccess = em
                .createQuery(
                        "SELECT ra FROM ResourceAccess ra WHERE ra.resource = :resource",
                        ResourceAccess.class);
        qResourceAccess.setParameter("resource", pr);
        List<ResourceAccess> raList = qResourceAccess.getResultList();
        if (raList.size() == 0)
            return 404; // HTTP NOT FOUND
        for (ResourceAccess ra : raList) {
            em.remove(ra); // delete ResourceAccess from datastore
        }
        return 200; // HTTP OK
    }

    /**
     * Delete ResourceContent for ProjectResource
     * 
     * @param resource ProjectResource to remove content
     * @return int status, 200 if successful
     */
    public int deleteResourceContent(ProjectResource resource) {
        EntityManager em = entityManager.get();
        try {
            TypedQuery<ResourceContent> q = em
                    .createQuery(
                            "SELECT rc FROM ResourceContent rc WHERE rc.resource = :resource",
                            ResourceContent.class);
            q.setParameter("resource", resource);
            ResourceContent content = q.getSingleResult();
            em.remove(content);
            return 200; // HTTP OK
        } catch (NoResultException e) {
            System.err.println("NoResultException: ResourceContent not found");
            return 404; // HTTP NOT FOUND
        }
    }

    /**
     * Get ResourceContent for ProjectResource
     * 
     * @param resource ProjectResource to get content
     * @return ResourceContent entity
     */
    public ResourceContent getResourceContent(ProjectResource resource) {
        EntityManager em = entityManager.get();
        try {
            TypedQuery<ResourceContent> q = em
                    .createQuery(
                            "SELECT rc FROM ResourceContent rc WHERE rc.resource = :resource",
                            ResourceContent.class);
            q.setParameter("resource", resource);
            ResourceContent content = q.getSingleResult();
            return content;
        } catch (NoResultException e) {
            System.err.println("ResourceContent not found " + e);
        }
        return null;
    }

    /**
     * Get ResourceAccess for current user
     * 
     * @param id Long
     * @return ResourceAccess
     */
    @LoggedIn
    public ResourceAccess getResourceAccess(Long id) {
        // get the access level for the given project
        EntityManager em = entityManager.get();
        try {
            ProjectResource r = em.find(ProjectResource.class, id);
            String userId = identity.getAccount().getId();
            TypedQuery<ResourceAccess> q = em
                    .createQuery(
                            "SELECT ra FROM ResourceAccess ra WHERE ra.resource = :resource AND ra.userId = :userId",
                            ResourceAccess.class);
            q.setParameter("resource", r);
            q.setParameter("userId", userId);
            ResourceAccess resourceAccess = q.getSingleResult();
            return resourceAccess;
        } catch (NoResultException e) {
            System.err.println("Could not retrieve access level for Resource "
                    + id + "\n" + e);
            return null;
        }
    }
    /**
     * Get UserAuthorisation for ProjectResource
     * 
     * @param resourceId Long
     * @param userId String
     * @return int status, 200 if successful
     */
    @LoggedIn
    public ResourceAccess getUserAuthorisation(Long resourceId, String userId) {
        // get the access level for the given resource
        EntityManager em = entityManager.get();
        try {
            ProjectResource r = em.find(ProjectResource.class, resourceId);
            // if current user's access is restricted, return null
            // TODO is there a way to throw PicketLink UNAUTHORISED
            if (getResourceAccess(r.getId()) == null)
                return null;
            TypedQuery<ResourceAccess> q = em
                    .createQuery(
                            "SELECT ra FROM ResourceAccess ra WHERE ra.resource = :resource AND ra.userId = :userId",
                            ResourceAccess.class);
            q.setParameter("resource", r);
            q.setParameter("userId", userId);
            ResourceAccess resourceAccess = q.getSingleResult();
            return resourceAccess;
        } catch (NoResultException e) {
            System.err.println("Could not retrieve access level for Resource "
                    + resourceId + "\n" + e);
            return null;
        }
    }
    /**
     * Create user authorisation for ProjectResource
     * 
     * @param resourceId Long
     * @param access ResourceAccess
     * @return int status, 200 if successful
     */
    @LoggedIn
    public int createUserAuthorisation(Long resourceId, ResourceAccess access) {
        // create project access for the given resource and user
        EntityManager em = entityManager.get();
        try {
            ProjectResource r = em.find(ProjectResource.class, resourceId);

            // if current user's access is not owner, fail
            // TODO is there a way to throw PicketLink UNAUTHORISED

            // Option 1: Owns project Project p = r.getProject();
            // if(projectController.getProjectAccess(p.getId())
            //      .getAccessLevel() != AccessLevel.OWNER) return null;

            // Option 2: Owns resource
            if (getResourceAccess(r.getId()).getAccessLevel() != AccessLevel.OWNER)
                return 401;
            TypedQuery<ResourceAccess> q = em
                    .createQuery(
                            "SELECT ra FROM ResourceAccess ra WHERE ra.resource = :resource AND ra.userId = :userId",
                            ResourceAccess.class);
            q.setParameter("resource", r);
            q.setParameter("userId", access.getUserId());
            if (q.getResultList().size() == 0) {
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

    /**
     * Update user authorisation for ProjectResource
     * 
     * @param resourceId Long
     * @param userId String
     * @param access ResourceAccess
     * @return int status, 200 if successful
     */
    @LoggedIn
    public int updateUserAuthorisation(Long resourceId, String userId,
            ResourceAccess access) {
        // update project access for the given project and user
        EntityManager em = entityManager.get();
        try {
            ProjectResource r = em.find(ProjectResource.class, resourceId);
            // if current user's access is not owner, fail
            // TODO is there a way to throw PicketLink UNAUTHORISED

            // Option 1: Owns project Project p = r.getProject();
            // if(projectController.getProjectAccess(p.getId())
            //      .getAccessLevel() != AccessLevel.OWNER) return null;

            // Option 2: Owns resource
            if (getResourceAccess(r.getId()).getAccessLevel() != AccessLevel.OWNER)
                return 401;
            TypedQuery<ResourceAccess> q = em
                    .createQuery(
                            "SELECT ra FROM ResourceAccess ra WHERE ra.resource = :resource AND ra.userId = :userId",
                            ResourceAccess.class);
            q.setParameter("resource", r);
            q.setParameter("userId", userId);
            ResourceAccess ra = q.getSingleResult();
            // update the AccessLevel and persist.
            if (ra.getUserId().equals(access.getUserId())) {
                ra.setAccessLevel(access.getAccessLevel());
                em.persist(ra);
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
     * Remove user authorisation for ProjectResource
     * 
     * @param resourceId Long
     * @param userId String
     * @return int status, 200 if successful
     */
    @LoggedIn
    public int removeUserAuthorisation(Long resourceId, String userId) {
        // update project access for the given project and user
        EntityManager em = entityManager.get();
        try {
            ProjectResource r = em.find(ProjectResource.class, resourceId);
            // if current user's access is not owner, fail
            // TODO is there a way to throw PicketLink UNAUTHORISED

            // Option 1: Owns project Project p = r.getProject();
            //if(projectController.getProjectAccess(p.getId())
            //        .getAccessLevel() != AccessLevel.OWNER) return null;

            // Option 2: Owns resource
            if (getResourceAccess(r.getId()).getAccessLevel() != AccessLevel.OWNER)
                return 401; // HTTP Unauthorised
            TypedQuery<ResourceAccess> q = em
                    .createQuery(
                            "SELECT ra FROM ResourceAccess ra WHERE ra.resource = :resource AND ra.userId = :userId",
                            ResourceAccess.class);
            q.setParameter("resource", r);
            q.setParameter("userId", userId);
            ResourceAccess ra = q.getSingleResult();
            // remove the AccessLevel
            em.remove(ra);
            return 200; // HTTP OK
        } catch (NoResultException e) {
            System.err.println("Could not find authorisation for user "
                    + userId);
            e.printStackTrace();
        }
        return 404; // HTTP NOT FOUND
    }

    /**
     * Create user authorisation for all resources in a Project
     * 
     * @param p Project
     * @param userId String
     * @param resourceAccess ResourceAccess.AccessLevel
     * @return Boolean, true if successful, false if invalid parameters
     */
    public Boolean createUserAuthorisationForAll(Project p, String userId,
            AccessLevel resourceAccess) {
        if (p == null || userId == null || resourceAccess == null)
            return false;
        List<ProjectResource> resources = listResources(p, 1);
        for (ProjectResource r : resources) {
            ResourceAccess access = new ResourceAccess();
            access.setResource(r);
            access.setUserId(userId);
            access.setAccessLevel(resourceAccess);
            createUserAuthorisation(r.getId(), access);
        }
        return true;
    }
    /**
     * Update user authorisation for all resources in a Project
     * 
     * @param p Project
     * @param userId String
     * @param resourceAccess ResourceAccess.AccessLevel
     * @return Boolean, true if successful, false if invalid parameters
     */
    public Boolean updateUserAuthorisationForAll(Project p, String userId,
            AccessLevel resourceAccess) {
        if (p == null || userId == null || resourceAccess == null)
            return false;
        List<ProjectResource> resources = listResources(p, 0);
        for (ProjectResource r : resources) {
            ResourceAccess access = new ResourceAccess();
            access.setResource(r);
            access.setUserId(userId);
            access.setAccessLevel(resourceAccess);
            updateUserAuthorisation(r.getId(), userId, access);
        }
        return true;

    }

    /**
     * Remove user authorisation for all resources in a Project
     * 
     * @param p Project
     * @param userId String
     * @return Boolean, true if successful, false if invalid parameters
     */
    public Boolean removeUserAuthorisationForAll(Project p, String userId) {
        if (p == null || userId == null)
            return false;
        List<ProjectResource> resources = listResources(p, 0);
        for (ProjectResource r : resources) {
            removeUserAuthorisation(r.getId(), userId);
        }
        return true;

    }

    // TODO Publish Resource via path eg. projects/{id}/{resourceName}/{resourceName}

    /**
     * Lookup ProjectResource by name
     * 
     * @param project Project that the resource belongs to
     * @param parent parent ProjectResource
     * @param name Name of the ProjectResource
     * @return ProjectResource
     */
    public ProjectResource lookupResourceByName(Project project,
            ProjectResource parent, String name) {
        EntityManager em = entityManager.get();

        TypedQuery<ProjectResource> q = em
                .createQuery(
                        "select r from ProjectResource r where r.project = :project and r.parent = :parent and r.name = :name",
                        ProjectResource.class);
        q.setParameter("project", project);
        q.setParameter("parent", parent);
        q.setParameter("name", name);

        try {
            return q.getSingleResult();
        } catch (NoResultException ex) {
            return null;
        }
    }

    /** 
     * Persist a new ProjectResource
     * 
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
     * Create ResourceContent from Base64Encoded String
     * @param resource associated ProjectResource
     * @param data Base64Encoded String data
     * @return ResourceContent entity
     * @throws IOException error reading byte data from Base64
     */
    @LoggedIn
    public ResourceContent createResourceContent(ProjectResource resource,
            String data) throws IOException {
        EntityManager em = entityManager.get();
        ResourceAccess access = getUserAuthorisation(resource.getId(), identity
                .getAccount().getId());
        if (access.getAccessLevel().equals(AccessLevel.OWNER)
                || access.getAccessLevel().equals(AccessLevel.READ_WRITE)) {
            // extract data from file
            // Path path = Paths.get(dataPath);
            byte[] byteData = Base64.decodeBase64(data);
            // create Resource Content
            ResourceContent content = null;
            TypedQuery<ResourceContent> q = em
                    .createQuery(
                            "SELECT c FROM ResourceContent c WHERE c.resource = :resource",
                            ResourceContent.class);
            q.setParameter("resource", resource);
            if (q.getResultList().size() == 0) // create if not exists, otherwise update
                content = new ResourceContent();
            else
                content = q.getSingleResult();
            content.setResource(resource);
            content.setContent(byteData);

            em.persist(content);
            return content;
        } else
            return null; // unauthorised
    }

    /**
     * PublishResource
     * Create the ProjectResource object from metadata,
     * Create ResourceAccess for current user and offical project owner
     * 
     * @param r ProjectResource
     * @return ProjectResource
     */
    @LoggedIn
    public ProjectResource publishResource(ProjectResource r) {
        String userId = identity.getAccount().getId();
        createResource(r);
        // create resource access for current user
        createResourceAccess(r, userId, AccessLevel.OWNER);
        // and create resource access for project owner
        Project p = r.getProject();
        if (!userId.equals(p.getOwner()))
            createResourceAccess(r, p.getOwner(), AccessLevel.OWNER);

        return r;
    }

    /**
     * Create ResourceAccess for given user with AccessLevel
     * 
     * @param resource ProjectResource
     * @param userId String
     * @param accessLevel ResourceAccess.AccessLevel
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
     * Update ProjectResource metadata
     * 
     * @param r ProjectResource
     * @param update ProjectResource containing updated data
     * @return udpated ProjectResource
     */
    @LoggedIn
    public ProjectResource updateResourceInfo(ProjectResource r,
            ProjectResource update) {
        EntityManager em = entityManager.get();
        ResourceAccess access = getUserAuthorisation(r.getId(), identity
                .getAccount().getId());
        if (access.getAccessLevel().equals(AccessLevel.OWNER)
                || access.getAccessLevel().equals(AccessLevel.READ_WRITE)) {
            r.setName(update.getName());
            if (update.getParent().getResourceType()
                    .equals(ResourceType.DIRECTORY))
                r.setParent(update.getParent());
            em.persist(r);
            return r;
        } else
            return null; // unauthorised
    }
}
