package net.sharemycode.model;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * Controls access levels for projects
 * 
 * @author Shane Bryzak
 * 
 */
@Entity
@Table(name = "PROJECT_ACCESS", uniqueConstraints = { @UniqueConstraint(columnNames = {
        "PROJECT_ID", "userId" }) })
public class ProjectAccess implements Serializable {
    private static final long serialVersionUID = 6539427720605504095L;
    /** Enumerate accessLevel:
     * owner, read, read_write or restricted
     * */
    public enum AccessLevel {
        OWNER, READ, READ_WRITE, RESTRICTED
    };

    @Id
    @GeneratedValue
    private Long id;

    /** Project that this ProjectAccess relates to */
    @ManyToOne
    @JoinColumn(name = "PROJECT_ID")
    private Project project;
    
    /** User that this ProjectAccess relates to */
    private String userId;

    /** Indicates whether the user currently has this project open */
    private boolean open;

    /** Access level for project */
    private AccessLevel accessLevel;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public AccessLevel getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(AccessLevel accessLevel) {
        this.accessLevel = accessLevel;
    }
}
