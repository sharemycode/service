package net.sharemycode.controller;

import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
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

import net.sharemycode.model.UserProfile;
import net.sharemycode.security.model.User;

import org.picketlink.Identity;
import org.picketlink.idm.IdentityManagementException;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.credential.Password;
import org.picketlink.idm.query.IdentityQuery;

/*
 * Performs persistence operations for users
 *
 * @author Lachlan Archibald
 */
@RequestScoped
public class UserController {
	// User status codes
	private static final int REGSUCCESS = 0;
	private static final int USEREXISTS = 1;
	private static final int EMAILEXISTS = 2;
	private static final int EMAILC = 3;
	private static final int PASSWORDC = 4;
	
	@Inject
    private IdentityManager im;
	
	@Inject
	private Identity identity;
	
	@Inject EntityManager em;

    public int registerUser(Map<String,String> properties) {
        System.out.println("registerController");
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
        try {
            im.add(u);
            im.updateCredential(u, password);
        } catch(IdentityManagementException e) {
            e.printStackTrace();
        }
        // now create the user profile
        User newUser = lookupUserByUsername(u.getUsername());
        if(newUser == null)
            return -1;  // failed to create user, return error
        else 
            if(createUserProfile(newUser.getId(), newUser.getUsername()) == null)
                return -1;  // failed to create profile, return error.
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
        q.setParameter(User.USERNAME, username.toLowerCase());
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
        q.setParameter(User.EMAIL, email.toLowerCase());
        if(q.getResultCount() == 0) {
        	return null;
        } else {
        	User user = q.getResultList().get(0);	// emails are also unique
        	return user;
        }
    }
    /* Find User Profile by username */
    public UserProfile lookupUserProfile(String username) {
        User u = this.lookupUserByUsername(username);
        try {
            UserProfile profile = em.find(UserProfile.class, u.getId());
            return profile;
        } catch (NoResultException e) {
            return null;
        }
    }
    
    public UserProfile createUserProfile(String id, String name) {
        // profile created on user Registration, default DisplayName is username.
        UserProfile profile = new UserProfile();
        profile.setId(id);
        profile.setDisplayName(name);
        em.persist(profile);
        return profile;
    }
    //@LoggedIn
    public UserProfile updateUserProfile(String id, String name, String about, String contact, String interests) {
        //String id = identity.getAccount().getId();
        UserProfile profile = em.find(UserProfile.class, id);
        try {
            em.getTransaction().begin();
                if (!name.isEmpty())
                    profile.setDisplayName(name);
                if (!about.isEmpty())
                    profile.setAbout(about);
                if(!contact.isEmpty())
                    profile.setContact(contact);
                if(!interests.isEmpty())
                    profile.setInterests(interests);
            em.getTransaction().commit();   // commit the changes to the existing EntityBean
            return profile;
        } catch (NoResultException e) {
            return null;
        }
    }
    
    public User updateUserAccount(String id, String username, String email, String password, String firstName, String lastName) {
        IdentityQuery<User> q = im.createIdentityQuery(User.class);
        q.setParameter(User.ID, id);
        User u = q.getResultList().get(0);
        if(u == null)
            return null;
        em.getTransaction().begin();
            if(!username.isEmpty())
                u.setUsername(username);
            if(!email.isEmpty())
                u.setEmail(email);
            if(!firstName.isEmpty())
                u.setFirstName(firstName);
            if(!lastName.isEmpty())
                u.setLastName(lastName);
            if (!password.isEmpty()) {
                // Password section
                Password pw = new Password(password);
                // TODO Remove current credentials when updating password?
                //im.removeCredential(u, arg1);
                im.updateCredential(u, pw);
            }
        em.getTransaction().commit();
        return u;
    }
}
