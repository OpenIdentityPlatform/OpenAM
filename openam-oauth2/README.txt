Add the following to the OpenAM web.xml in order to run the demo.
The usernames and passwords must match a user in OpenAM.
The demo will be available under openam/oauth2demo/
 


<!-- Servlet to Restlet adapter declaration (Mandatory) -->
    <servlet>
        <servlet-name>RestletAdapter</servlet-name>
        <servlet-class>org.restlet.ext.servlet.ServerServlet</servlet-class>

        <!-- Your application class name (Optional - For mode 3) -->
        <init-param>
            <param-name>org.restlet.application</param-name>
            <param-value>org.forgerock.openam.oauth2.OAuth2Application</param-value>
        </init-param>

        <!-- List of supported client protocols (Optional - Only in mode 3) -->
        <init-param>
            <param-name>org.restlet.clients</param-name>
            <param-value>RIAP CLAP FILE</param-value>
        </init-param>
    </servlet>
    <servlet>
        <servlet-name>RestletDemoAdapter</servlet-name>
        <servlet-class>org.restlet.ext.servlet.ServerServlet</servlet-class>

        <!-- Your application class name (Optional - For mode 3) -->
        <init-param>
            <param-name>org.restlet.application</param-name>
            <param-value>org.forgerock.openam.oauth2demo.OAuth2DemoApplication</param-value>
        </init-param>

        <!-- List of supported client protocols (Optional - Only in mode 3) -->
        <init-param>
            <param-name>org.restlet.clients</param-name>
            <param-value>HTTP RIAP CLAP FILE</param-value>
        </init-param>
		<init-param>
            <param-name>oauth2.client_id</param-name>
            <param-value>demo</param-value>
        </init-param>
		<init-param>
            <param-name>oauth2.client_secret</param-name>
            <param-value>Passw0rd</param-value>
        </init-param>
		<init-param>
            <param-name>oauth2.username</param-name>
            <param-value>demo</param-value>
        </init-param>
		<init-param>
            <param-name>oauth2.password</param-name>
            <param-value>Passw0rd</param-value>
        </init-param>
		<init-param>
            <param-name>oauth2.scope</param-name>
            <param-value>read write</param-value>
        </init-param>
    </servlet>

<!-- servlet declaration -->

    <servlet-mapping>
        <servlet-name>RestletAdapter</servlet-name>
        <url-pattern>/oauth2/*</url-pattern>
    </servlet-mapping>
	<servlet-mapping>
        <servlet-name>RestletDemoAdapter</servlet-name>
        <url-pattern>/oauth2demo/*</url-pattern>
    </servlet-mapping>

