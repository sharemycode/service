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

@SuppressWarnings("serial")
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = "ID"))
public class Project implements Serializable {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    @Column(name = "ID")
    private String id; // unique Project ID

    @NotNull
    @Size(min = 6)
    @Column(unique = true)
    private String url; // unique project URL (generated using generateURL)

    @NotNull
    @Size(min = 1, max = 20)
    private String name; // project name

    @Size(max = 100)
    private String description; // project description

    private String owner_id; // user ID of project owner (referential integrity
                             // not enforced)

    private String version; // version of the project

    // private String filePath; // path to temporary file on server

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

    /*
     * public String getFilePath() { return filePath; }
     * 
     * public void setFilePath(String filePath) { this.filePath = filePath; }
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