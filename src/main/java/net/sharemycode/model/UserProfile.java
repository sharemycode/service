package net.sharemycode.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * User profile information
 * 
 * @author Lachlan Archibald
 */

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = "id"))
public class UserProfile implements Serializable {

    /** UserId that this profile belongs to */
    @Id
    @Column(name = "id")
    private String id;

    /**
     * User's custom display name for profile.
     * Can be real name, nickname, pseudonym, anything.
     */
    private String displayName;

    /** About Information */
    private String aboutContent;

    /** Contact information */
    private String contactContent;

    /** Interests information */
    private String interestsContent;

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setDisplayName(String name) {
        this.displayName = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setAbout(String content) {
        this.aboutContent = content;
    }

    public String getAbout() {
        return aboutContent;
    }

    public void setContact(String content) {
        this.contactContent = content;
    }

    public String getContact() {
        return contactContent;
    }

    public void setInterests(String content) {
        this.interestsContent = content;
    }

    public String getInterests() {
        return interestsContent;
    }
}
