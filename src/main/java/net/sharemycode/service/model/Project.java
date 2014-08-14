package net.sharemycode.service.model;

import java.security.SecureRandom;
import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.ManyToOne;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotEmpty;
import org.hibernate.annotations.GenericGenerator;

@SuppressWarnings("serial")
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = "projectID, url"))

public class Project implements Serializable {

    @Id
    @GeneratedValue(generator="system-uuid")
    @GenericGenerator(name="system-uuid", strategy = "uuid")
    private String projectID;	// unique Project ID

    @NotNull
    @Size(min=6)
    private String url;			// unique project URL (generated using generateURL)
    
    @NotNull
    @Size(min = 1, max = 20)
    private String name;		// project name

    @Size(max = 100)
    private String description;	// project description
    
    @NotNull
    private long owner_id;		// user ID of project owner (referential integrity not enforced)

    private String version;		// version of the project
    
    public String getId() {
        return projectID;
    }

    public void setId(String id) {
        this.projectID = id;
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
    
    public Long getOwner() {
        return owner_id;
    }

    public void setOwner(Long owner_id) {
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
    
    public static String generateURL() {
    	// define URL alphabet - alphanumeric characters minus 1,i,l and o
    	String alphabet = "23456789abcdefghjkmnpqrstuvwxyzABCDEFGHJKMNPQRSTUVWXYZ";
    	SecureRandom random = new SecureRandom();
    	String url = "";
    	for(int i = 0; i < 6; i++) {
    		char d = alphabet.charAt(random.nextInt(alphabet.length()));
    		System.out.println(d);
    		url = url + Character.toString(d);
    	}
    	return url;
    }

}