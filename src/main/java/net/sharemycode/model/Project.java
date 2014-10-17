package net.sharemycode.model;

import java.io.Serializable;
import java.security.SecureRandom;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.annotations.GenericGenerator;

/**
 * Project entity
 * 
 * @author Lachlan Archibald
 */
@SuppressWarnings("serial")
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = "ID"))
public class Project implements Serializable {

    /** Unique Project id */
    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    @Column(name = "ID")
    private String id;

    /**
     * Unique part of project URL.
     * eg. sharemycode.net/xZacJk
     */
    @NotNull
    @Size(min = 6)
    @Column(unique = true)
    private String url;

    /** Name of the project */
    @NotNull
    @Size(min = 1, max = 30)
    private String name;

    /** Project Description */
    @Size(max = 200)
    private String description;

    /** 
     * User ID of displayed project owner.
     * Referential Integrity not enforced
     */
    private String owner_id;

    /** Project Version - user-entered */
    private String version;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwner() {
        return owner_id;
    }

    public void setOwner(String owner_id) {
        this.owner_id = owner_id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String desc) {
        this.description = desc;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }
    
    /**
     * Generate a random six character string for URL
     * @return String
     */
    public static String generateURL() {
        // define URL alphabet - alphanumeric characters minus 0,1,i,l and o
        String alphabet = "23456789abcdefghjkmnpqrstuvwxyzABCDEFGHJKMNPQRSTUVWXYZ";
        SecureRandom random = new SecureRandom();
        String url = "";
        for (int i = 0; i < 6; i++) {
            char d = alphabet.charAt(random.nextInt(alphabet.length()));
            url = url + Character.toString(d);
        }
        return url;
    }
}