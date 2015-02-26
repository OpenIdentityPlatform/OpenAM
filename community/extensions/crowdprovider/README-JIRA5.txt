To allow for SSO in JIRA 5.x using OpenAM follow the following steps:

Copy the OpenAM client sdk to $ATLASSIAN_JIRA_APPLICATION_DIR/atlassian-jira/WEB-INF/lib/
Create an AMConfig.properties to $ATLASSIAN_JIRA_APPLICATION_DIR/atlassian-jira/WEB-INF/classes/ (or somewhere in the classpath) NOTE: you can find a template on: https://svn.forgerock.org/openam/trunk/opensso/products/amserver/clientsdk/resources/AMClient.properties but be aware that you have to rename the file to AMConfig.properties.
Create an Agent 2.2 profile in the OpenAM Console in the top level realm, the agent profile name and password should match those in the AMConfig.properties file.
Copy this projects jar to $ATLASSIAN_JIRA_APPLICATION_DIR/atlassian-jira/WEB-INF/lib/

Edit $ATLASSIAN_JIRA_APPLICATION_DIR/atlassian-jira/WEB-INF/classes/seraph-config.xml:
	
	1) Replace the following lines with the given values:
	
	<param-name>login.url</param-name>
	<param-value>http://yourlogin.yourdomain.com/openam/UI/Login?goto=${originalurl}</param-value>
	
	<param-name>link.login.url</param-name>
	<param-value>http://yourlogin.yourdomain.com/openam/UI/Login?goto=${originalurl}</param-value>
	
	<param-name>logout.url</param-name>
	<param-value>http://yourlogin.yourdomain.com/openam/UI/Logout</param-value>
	
	2) Comment out this line:
	<authenticator class="com.atlassian.jira.security.login.JiraSeraphAuthenticator"/>
	
	3) Add this line:
	<authenticator class="org.forgerock.openam.extensions.crowd.Jira5Authenticator" />


If you have a private JIRA and users need always to be logged in using OpenAM add the following to $ATLASSIAN_JIRA_APPLICATION_DIR/atlassian-jira/WEB-INF/classes/seraph-paths.xml:
	 <path name="user">
		 <url-pattern>/*</url-pattern>
		 <role-name>user</role-name>
	 </path>
 
 Restart JIRA for the changes to take effect.
 No newline at end of file
