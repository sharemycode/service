sharemycode.net
===============
sharemycode.net is a Java EE application that provides a free web service to allow developers to share project source code and resources to facilitate collaboration and reduce the time it takes to solve problems. The service provides an open API allowing any number of clients to connect and consume the service, encouraging developers to contribute to the project by creating new clients to support their needs and increase the number of available clients, so that new users of the service can select an appropriate client for their existing development environment.

This user manual provides basic instruction for deploying the service on your local machine, configuring the the client library, and using the basic functions of the service to create and manage projects. This manual assumes you have Git, Apache Maven, an Integrated Development Environment (IDE) such as Eclipse, and the Google Chrome browser installed.

Deploying the Service
---------------------
1. Download the latest stable version of Wildfly from http://wildfly.org/, and install the application server by following the installation guide at https://docs.jboss.org/author/display/WFLY8/Getting+Started+Guide
2. Clone the latest version of the sharemycode.net service from the github repository into your workspace by opening a terminal emulator and issuing the command:

      git clone https://github.com/sharemycode/service.git

3. Navigate to the service directory in your workspace and deploy the service to your Wildfly application server with the command:

      mvn clean wildfly:deploy

4. Open Google Chrome and go to the URL http://localhost:8080/sharemycode/. If deployment to Wildfly succeeded, the webclient will load in your browser.
5. Refer to the Javadocs API by opening the service project in your chosen IDE and select Project >  “Generate Javadocs”.

Developing a Client Application
--------------------------------
1. Create a new Java project in your chosen IDE, and import the client library by adding

      import net.sharemycode.client.Client;

2. Create a new instance of the sharemycode.net client with

      Client client = new Client(“localhost:8080”, “”, “sharemycode/rest”);

3. Use the Client instance to run the provided methods to connect and use the service. For example:

  i. Test connection to your service: 

      Boolean success = client.testConnection();

  ii. Create a new user:

      client.createUser(String username, String password, String passwordc, String email, String emailc, String firstName, String lastName);
  
  iii. Login:

      client.login(String username, String password);

  iv. Create a project:

      client.createProject(String projectName, String version, String description);

  v. Logout:

      client.logout();

4. When you exit your client application, close the Client instance with client.close();
