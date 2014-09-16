package net.sharemycode.model;

import java.io.File;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

@Entity
public class ProjectAttachment {

    @Id
    @GeneratedValue
    private Long id;
    
    private String uploadPath;
    
    private Date uploadDate;
    
    public Long getId() {
        return id;
    }
    
    public void setUploadPath(String path) {
        this.uploadPath = path;
    }
    
    public String getUploadPath() {
        return uploadPath;
    }
    
    public void setUploadDate(Date date) {
        this.uploadDate = date;
    }
    
    public Date getUploadDate() {
        return uploadDate;
    }
    
    public Boolean deleteAttachment() {
        // delete the attachment file.
        try {
            File attachment = new File(uploadPath);
            return attachment.delete();
        } catch (Exception e) {
            System.err.println("Exception while deleting file " + uploadPath);
            e.printStackTrace();
            return false;
        }
    }
    
}
