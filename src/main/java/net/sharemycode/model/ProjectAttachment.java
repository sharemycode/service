package net.sharemycode.model;

import java.io.File;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import org.apache.commons.io.FileUtils;

/**
 * ProjectAttachment entity.
 * Stores the path to uploaded file on disk, handles deletion of file.
 * 
 * @author Lachlan Archibald
 */

@Entity
public class ProjectAttachment {

    @Id
    @GeneratedValue
    private Long id;

    /** Path to file on disk */
    private String uploadPath;

    /** Date uploaded */
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

    /** 
     * Delete the attached file, and the directory it is stored in 
     * @return Boolean, true if file and directory deleted successfully
     */
    public Boolean deleteAttachment() {
        // delete the attachment file.
        try {
            File attachment = new File(uploadPath);
            FileUtils.deleteDirectory(new File(attachment.getParent()));
            return true;
        } catch (Exception e) {
            System.err.println("Exception while deleting file " + uploadPath);
            e.printStackTrace();
            return false;
        }
    }

}
