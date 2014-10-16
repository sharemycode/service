package net.sharemycode.model;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

/**
 * Defines access level for project resources
 * 
 * @author Shane Bryzak
 * 
 */
@Entity
public class ResourceAccess implements Serializable {
    private static final long serialVersionUID = -790604070028430439L;

    /** Enumerate ResourceAccess accessLevel: OWNER, READ, READ_WRITE */
    public enum AccessLevel {
        OWNER, READ, READ_WRITE
    };

    @Id
    @GeneratedValue
    private Long id;

    /** Resource that this entity relates to */
    @ManyToOne
    private ProjectResource resource;

    /** User to give access to */
    private String userId;

    /** AccessLevel to give, OWNER, READ or READ_WRITE */
    private AccessLevel accessLevel;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ProjectResource getResource() {
        return resource;
    }

    public void setResource(ProjectResource resource) {
        this.resource = resource;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public AccessLevel getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(AccessLevel accessLevel) {
        this.accessLevel = accessLevel;
    }
}