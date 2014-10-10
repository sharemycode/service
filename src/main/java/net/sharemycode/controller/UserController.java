package net.sharemycode.controller;

import java.util.List;
import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;

import net.sharemycode.model.UserProfile;
import net.sharemycode.security.annotations.LoggedIn;
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
    
    
    /* Return full list of users */ // demonstration only
    @LoggedIn
    public List<User> listAllUsers() {
    	System.out.println("ListUsersCONTROLLER");
        IdentityQuery<User> q = im.createIdentityQuery(User.class);
        return q.getResultList();
    }
    
    /* return specific user by username */
    @LoggedIn
    public User lookupUserByUsername(String username) {
        // returns user information including email, first name and last name
        // may need to change for security purposes
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
    
    /* UPDATE USER PROFILE */
    @LoggedIn
    public UserProfile updateUserProfile(User u, UserProfile update) {
        if(identity.getAccount().getId().equals(u.getId())) {
            // if current logged in user is editing own user profile
            // possible admin update support
            UserProfile profile = em.find(UserProfile.class, u.getId());
            try {
                profile.setDisplayName(update.getDisplayName());
                profile.setAbout(update.getAbout());
                profile.setContact(update.getContact());
                profile.setInterests(update.getInterests());
                em.persist(profile);
                return profile;
            } catch (NoResultException e) {
                return null;
            }
        } else {
            System.err.println("Unauthorised action");
        }
        return null;
    }
    
    /* UPDATE USER ACCOUNT */
    @LoggedIn
    public User updateUserAccount(User u, String username, String email, String emailc, String password, String passwordc, String firstName, String lastName) {
        if(u == null)
            return null;
        if(identity.getAccount().getId().equals(u.getId())) {
            // if current logged in user is editing own user account
            // possible admin update support
            //IdentityQuery<User> q = im.createIdentityQuery(User.class);
            if(!username.isEmpty())
                u.setUsername(username);
            if(!email.isEmpty() && email.equals(emailc))
                u.setEmail(email);
            if(!firstName.isEmpty())
                u.setFirstName(firstName);
            if(!lastName.isEmpty())
                u.setLastName(lastName);
            if (!password.isEmpty() && password.equals(passwordc)) {
                // Password section
                Password pw = new Password(password);
                // TODO Remove current credentials when updating password?
                //im.removeCredential(u, arg1);
                im.updateCredential(u, pw);
            }
            im.update(u);
        } else {
            System.err.println("Unauthorised action");
        }
        return u;
    }

    /* LOGOUT */ // Temporary workaround until PicketLink is fixed
    @LoggedIn
    public int logout() {
        System.out.println("DEBUG: " + identity.getAccount().getId());
        System.out.println("DEBUG: LOGOUT");
        identity.logout();
        if(identity.getAccount() == null) {
            System.out.println("DEBUG: " + "null");
            return 200; // HTTP OK
        } else {
            System.out.println("DEBUG: " + identity.getAccount().getId());
            return 202; // HTTP ACCEPTED (Understood, but not processed)
        }
    }
}
