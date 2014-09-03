package net.sharemycode.controller;

import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import net.sharemycode.security.model.User;

import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.credential.Password;
import org.picketlink.idm.query.IdentityQuery;

/*
 * Performs persistence operations for users
 *
 * @author Lachlan Archibald
 */
@ApplicationScoped
public class UserController {
	// User status codes
	private static final int REGSUCCESS = 0;
	private static final int USEREXISTS = 1;
	private static final int EMAILEXISTS = 2;
	private static final int EMAILC = 3;
	private static final int PASSWORDC = 4;
	
	@Inject
    private IdentityManager im;

    public int registerUser(Map<String,String> properties) {
    	// First we test if the user entered non-unique username and email
    	if(this.lookupUserByUsername(properties.get("username")) != null){
    		System.err.println("Username already exists");
    		return USEREXISTS;
    	}
    	if(this.lookupUserByEmail(properties.get("email")) != null) {
    		System.err.println("Email already exists");
    		return EMAILEXISTS;
    	}
        if(!properties.get("email").equals(properties.get("emailc"))) {
        	System.err.println("Email confirmation failure");
        	// email confirmation failure
        	return EMAILC;
        }
        if(!properties.get("password").equals(properties.get("passwordc"))) {
        	System.err.println("Password confirmation failure");
        	// password confirmation failure
        	return PASSWORDC;
        }
        User u = new User();
        u.setUsername(properties.get("username").toLowerCase());
        u.setEmail(properties.get("email").toLowerCase());
        u.setFirstName(properties.get("firstName"));
        u.setLastName(properties.get("lastName"));
        Password password = new Password(properties.get("password"));
        //Repository.userRepo.add(u);
        im.add(u);
        im.updateCredential(u, password);
        // sucessful user registration
        return REGSUCCESS;	
    }
    
    
    /* Return full list of users */
    public List<User> listAllUsers() {
    	System.out.println("ListUsersCONTROLLER");
        IdentityQuery<User> q = im.createIdentityQuery(User.class);
        return q.getResultList();
    }
    
    /* return specific user by username */
    public User lookupUserByUsername(String username) {
        IdentityQuery<User> q = im.createIdentityQuery(User.class);
        q.setParameter(User.USERNAME, username);
        if(q.getResultCount() == 0) {
        	return null;
        } else {
        	User user = q.getResultList().get(0);	// usernames are unique
        	return user;
        }
    }
    
    /* Find user by Email */
    public User lookupUserByEmail(String email) {
        IdentityQuery<User> q = im.createIdentityQuery(User.class);
        q.setParameter(User.EMAIL, email);
        if(q.getResultCount() == 0) {
        	return null;
        } else {
        	User user = q.getResultList().get(0);	// emails are also unique
        	return user;
        }
    }

}
