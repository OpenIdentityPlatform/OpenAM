
This enables SSO for JIRA/Confluence and other apps utlizing Atlassian Seraph.

Install:
- Copy the OpenAM client sdk to atlassian-jira/WEB-INF/lib/
- Create/copy an AMConfig.properties to atlassian-jira/WEB-INF/classes/ (or somewhere in the classpath)
- Copy this projects jar to atlassian-jira/WEB-INF/lib/

- edit atlassian-jira/WEB-INF/classes/seraph-config.xml:
    <param-name>login.url</param-name>
    <param-value>http://yourlogin.yourdomain.com/openam/UI/Login?goto=${originalurl}</param-value>

    <param-name>link.login.url</param-name>
    <param-value>http://yourlogin.yourdomain.com/openam/UI/Login?goto=${originalurl}</param-value>

    <param-name>logout.url</param-name>
    <param-value>http://yourlogin.yourdomain.com/openam/UI/Logout</param-value>

In older versions of Jira comment out this line
    <!-- COMMENT OUT THIS LINE authenticator class="com.atlassian.seraph.auth.DefaultAuthenticator"/-->

In newer versions of Jira you will need to comment out this line:

    <!--
    <authenticator class="com.atlassian.crowd.integration.seraph.JIRAAuthenticator"/>
    -->

For very old versions of Jira, add this line.

    <authenticator class="com.sun.identity.provider.seraph.OpenSsoAuthenticator"/>

For recent versions of Jira, add this line.

<authenticator class="com.sun.identity.provider.seraph.OpenSsoJiraAuthenticator"/>

You will need to restart JIRA for changes to take effect.

- You will need to do one of two things:

  1)  If you have a public JIRA, you're done.  Although you will still see the login form so you
      may want to remove that template.

  2)  If you have a private JIRA and always to be redirected to SSO - see no login form/links -
      add to atlassian-jira/WEB-INF/classes/seraph-paths.xml:
      <path name="user">
        <url-pattern>/*</url-pattern>
        <role-name>user</role-name>
      </path>

Confluence 3.0+:

Installation is much the same for Confluence. 

Install:
- Copy the OpenAM client sdk to atlassian-confluence/WEB-INF/lib/
- Create/copy an AMConfig.properties to atlassian-confluence/WEB-INF/classes/ (or somewhere in the classpath)
- Copy this projects jar to atlassian-confluence/WEB-INF/lib/

- edit atlassian-confluence/WEB-INF/classes/seraph-config.xml:

Update the login.url, link.login.url and logout.url parameters as before. 

Comment out this line.

<!--  <authenticator class="com.atlassian.confluence.user.ConfluenceAuthenticator"/>  -->

replace with this:

<authenticator class="com.sun.identity.provider.seraph.OpenSsoConfluenceAuthenticator"/>

You will need to restart Confluence for changes to take effect.

- Use com.sun.identity.provider.seraph.OpenSsoConfluenceAuthenticator
- Thanks to Casey.BUTTERWORTH at suncorp.com.au
  * https://opensso.dev.java.net/servlets/ReadMsg?listName=users&msgNo=11574

Build:
- Use maven 2.x

