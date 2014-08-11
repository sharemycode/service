package net.sharemycode;

import java.util.Random;

public class Project {
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
		}
}
