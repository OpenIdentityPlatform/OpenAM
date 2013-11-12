To allow for SSO in Confluence 4.x using OpenAM follow the following steps:

Copy the OpenAM client sdk to $ATLASSIAN_CONFLUENCE_APPLICATION_DIR/confluence/WEB-INF/lib/
Create an AMConfig.properties to $ATLASSIAN_CONFLUENCE_APPLICATION_DIR/confluence/WEB-INF/classes/ (or somewhere in the classpath) NOTE: you can find a template on: https://svn.forgerock.org/openam/trunk/opensso/products/amserver/clientsdk/resources/AMClient.properties but be aware that you have to rename the file to AMConfig.properties.
Create an Agent 2.2 profile in the OpenAM Console in the top level realm, the agent profile name and password should matc
h those in the AMConfig.properties file.
Copy this projects jar to $ATLASSIAN_CONFLUENCE_APPLICATION_DIR/confluence/WEB-INF/lib/

Edit $ATLASSIAN_CONFLUENCE_APPLICATION_DIR/confluence/WEB-INF/classes/seraph-config.xml:
	
	1) Replace the following lines with the given values:
	
	<param-name>login.url</param-name>
	<param-value>http://yourlogin.yourdomain.com/openam/UI/Login?goto=${originalurl}</param-value>
	
	<param-name>link.login.url</param-name>
	<param-value>http://yourlogin.yourdomain.com/openam/UI/Login?goto=${originalurl}</param-value>

	!! NOTE !!
	Due to issue https://jira.atlassian.com/browse/CONF-4931 there is no need to provide a logout URL, since Confluence won't pick it up. See steps below to configure the logout URL.
	
	2) Comment out this line:
	<authenticator class="com.atlassian.confluence.user.ConfluenceAuthenticator"/>
	
	3) Add this line:
	<authenticator class="org.forgerock.openam.extensions.crowd.Confluence4Authenticator" />

Providing Single Log Out
	As registered in issue https://jira.atlassian.com/browse/CONF-4931 the Seraph logout.url property will have no effect if it's defined.
	Although this hampers Single Log Out functionality there is a workaround availabe and described in the same issue (https://jira.atlassian.com/browse/CONF-4931).
	In short you will have to follow these steps to fix SLO:
	
	1) Extract the xwork.xml file from the $ATLASSIAN_CONFLUENCE_APPLICATION_DIR/confluence/WEB-INF/lib/confluence-4.x.x.jar:
	(e.g.: jar xvf $ATLASSIAN_CONFLUENCE_APPLICATION_DIR/confluence/WEB-INF/lib/confluence-4.x.x.jar xwork.xml)
	
	2) Edit the following section in the xwork.xml file to provide the logout URL:
	...
	<action name="logout" class="com.atlassian.confluence.user.actions.LogoutAction">
		<interceptor-ref name="defaultStack"/>
		<result name="error" type="velocity">/logout.vm</result>
		<result name="success" type="redirect">http://yourlogin.yourdomain.com/openam/UI/Logout</result>
	</action>
	...
	
	3) Copy the modified xwork.xml file to $ATLASSIAN_CONFLUENCE_APPLICATION_DIR/confluence/WEB-INF/classes/
	Note that the modified xwork.xml file will take precedence on the bundled xwork.xml file.

 Restart Confluence for the changes to take effect.
 No newline at end of file

