
This enables SSO for JIRA/Confluence and other apps utlizing Atlassian Seraph.

Install:
- Copy the OpenSSO client sdk to atlassian-jira/WEB-INF/lib/
- Create/copy an AMConfig.properties to atlassian-jira/WEB-INF/classes/ (or somewhere in the classpath)
- Copy this jar to atlassian-jira/WEB-INF/lib/

- edit atlassian-jira/WEB-INF/classes/seraph-config.xml:
    <param-name>login.url</param-name>
    <param-value>https://YOUR_SSO_SERVER/sso/UI/Login?goto=${originalurl}</param-value>

    <param-name>link.login.url</param-name>
    <param-value>https://YOUR_SSO_SERVER/sso/UI/Login?goto=${originalurl}</param-value>

    <param-name>logout.url</param-name>
    <param-value>https://YOUR_SSO_SERVER/sso/UI/Logout</param-value>

    <!-- COMMENT OUT THIS LINE authenticator class="com.atlassian.seraph.auth.DefaultAuthenticator"/-->
    <authenticator class="com.sun.identity.provider.seraph.OpenSsoAuthenticator"/>

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
- Use com.sun.identity.provider.seraph.OpenSsoConfluenceAuthenticator
- Thanks to Casey.BUTTERWORTH at suncorp.com.au
  * https://opensso.dev.java.net/servlets/ReadMsg?listName=users&msgNo=11574

Build:
- Use maven 2.x
- You will need to grab opensso.zip and install the client sdk manually
  until one appears in some public repository.
- Add https://maven.atlassian.com/repository/public to your repositories

