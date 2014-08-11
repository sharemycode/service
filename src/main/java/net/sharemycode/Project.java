package net.sharemycode;

import java.util.Random;
import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.Digits;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import javax.xml.bind.annotation.XmlRootElement;

import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotEmpty;

@SuppressWarnings("serial")
@Entity
@XmlRootElement
@Table(uniqueConstraints = @UniqueConstraint(columnNames = "url"))

public class Project implements Serializable {

    @Id
    @GeneratedValue
    private Long id;

    @NotNull
    @GeneratedValue
    @Size(min=6)
    private String url;
    
    @NotNull
    @Size(min = 1, max = 20)
    private String name;
    
    @NotNull
    private long owner_id;

    public Long getId() {
        return id;
    }

    @NotNull
    @Size(min=1)
    public void setId(Long id) {
        this.id = id;
    }
    
    @Size(max = 50)
    private String description;

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

}

/*
private String url;
	private String description;
	private String path;
	private String creator;
	private String archive;

	public Project(String description, String creator) {
		this.description = description;
		this.creator = creator;
		// TODO Add other instance variables to Project constructor
	}
	
	private String generateURL() {
		Random random = new Random();
		long randomLong = random.nextLong();
		String result = "";
		// TODO Generate random string for URL, verify not taken
		return result;
	}
	
	// Accessors
		public String getProjectURL() {
			return this.url;
		}
		
		public String getDescription() {
			return this.description;
		}
		
		public String getProjectPath() {
			return this.path;
		}
		
		public String getCreator() {
			return this.creator;
		}
		
		public String getArchive() {
			return this.archive;
		}*/
