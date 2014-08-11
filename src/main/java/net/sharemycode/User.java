package net.sharemycode;

public class User {
	
	private int userID;
	private String username;
	private String password;
	private String email;
	private String givenName;
	private String surname;
	
	
	public User(String username, String password, String email, String given, String surname) {
		this.username = username;
		this.password = password;
		this.email = email;
		this.givenName = given;
		this.surname = surname;
		// TODO Add user to resource list
	}
	
	// Accessors
	public String getPassword() {
		return this.password;
	}
	
	public String getEmail() {
		return this.email;
	}
	
	public String getGivenName() {
		return this.givenName;
	}
	
	public String getSurname() {
		return this.surname;
	}
}
