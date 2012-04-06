------------------------------------------------------------------------------
README file for Spring Security 2.0 Provider Example
------------------------------------------------------------------------------
   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  
Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
  
The contents of this file are subject to the terms
of the Common Development and Distribution License
(the License). You may not use this file except in
compliance with the License.

You can obtain a copy of the License at
https://opensso.dev.java.net/public/CDDLv1.0.html or
opensso/legal/CDDLv1.0.txt
See the License for the specific language governing
permission and limitations under the License.

When distributing Covered Code, include this CDDL
Header Notice in each file and include the License file
at opensso/legal/CDDLv1.0.txt.
If applicable, add the following below the CDDL Header,
with the fields enclosed by brackets [] replaced by
your own identifying information:
"Portions Copyrighted 2008 Miguel Angel Alonso Negro <miguelangel.alonso@gmail.com>"

$Id: readme.txt,v 1.3 2009/02/26 19:52:42 wstrange Exp $

------------------------------------------------------------------------------


---
Updated Feb 26/09 by warren.strange@gmail.com

- Updated to show use of Spring security tags

-----------

Example of web application with OpenSSO and Spring Security
===========================================================

Overview
--------

This is a maven project with a web application which uses the spring security 
provider found in the subversion URL:

https://opensso.dev.java.net/source/browse/opensso/extensions/spring2provider/provider/

The application has several pages to explain how is used the spring security 
provider of OpenSSO. The features are:

    - Single sign-on: we will see two web applications in the same browser with 
    the OpenSSO configuration need an unique authetication. And if the user 
    log out from one, then the another application has the session expired too.
    
    - Authorization management.- It is defined in OpenSSO manager application for
    every application which is in the security realm.
    
Layout
------

The main file is src/main/webapp/WEB-INF/web.xml where is defined the JEE configuration.
The components are:
    - Java Serve Faces
    - Spring Framework
    - Spring Security

But the main configuration is defined in Spring Framewok, in the main file:

src/main/webapp/WEB-INF/applicationContext.xml

This files has only an imported file which has the security configuration. (In a
more complex application would be persistence, web service, ... configuration. See
details in http://static.springframework.org/spring/docs/2.5.x/reference/index.html).
ac-security.xml is the most important file of the example, see below.

The file src/main/webapp/WEB-INF/faces-config.xml has the Java Server Faces 
configuration. In this example it is empty and it has only the integration with 
Spring Framework.

The file src/main/resources/AMConfig.properties has the OpenSSO configuration. 
And It is assumed the Open SSO administration application URL is:

http://localhost:8080/opensso

And the admin user has this credentials:

com.sun.identity.agents.app.username=amadmin
com.iplanet.am.service.password=adminadmin

(Change AMConfig.properties according to your settings.)

If you don't have installed OpenSSO see:

http://developers.sun.com/identity/reference/techart/opensso-glassfish.html

You will need a directory service, I advise to use OpenDJ (https://opendj.forgerock.org/),
and, specifically, I advise from version 2.4.4. And, after installing OpenDJ and
OpenSSO, you will ought to access to opensso web, select Access Control -> Realm 
-> Data Stores -> generic ldapv3. Then, remove "memberOf" in member group attribute 
and select "SCOPE_SUB" in "LDAPv3 Plug-in Search Scope" field of LDAP user configuration.


ac-security.xml
---------------

This file is the most important of the example. It shows how the application must 
be configured using Spring Security with the implementation of the OpenSSO provider.

In Spring Security 2.0 the configuration is simpler than in Acegi for Spring Framework
(see documentation: http://static.springframework.org/spring-security/site/reference/html/springsecurity.html) 
for an application with default configuration. But, with the opensso provider whe 
have a particular configuration. Then we must configure Spring Security as old way.  

This file has the following beans:

    - filterChainProxy.- This is the main bean. It defines every security filter which
    goes to receive the request. The order of each filter is very importante. In XML
    comments I explain the order of the filters and the order of all possible filter
    defined in Spring Security framework.
    
    - httpSessionContextIntegrationFilter.- It is responsible for storing a security 
    context between HTTP requests.
    
    - logoutFilter.- It is in charge of doing the logout in the application and in 
    every application where the user logged via Single sign-on.
    
    - openssoFilter.- It is responsible for processing authentications.
    
    - exceptionTranslationFilter.- It manages the requests which are rejected by the 
    rest of the filters. That requests do not have a authenticated session or have 
    denied access
    
    - filterInvocationInterceptor.- It is the last filter. It manages the authencation 
    and authoritation of each request.

    - authenticationManager.- Authentication manager used by filterInvocationInterceptor.
    
    - authenticationEntryPoint.- It defines the login page and the authentication URL
    
    - accessDecisionManager.- Authorization manager used by filterInvocationInterceptor

    
Code
----

The class com.sun.identity.shared.encode.CookieUtils is the unique code of the project. 
For this example, it is not necessary code. But this class is because it belongs to 
openssoclientsdk. It is a patch to get working in any servlet container without OpenSSO 
agent.


Building & deploy
-----------------

The building is very simple with the infrastructure of maven (see http://maven.apache.org/). 
First, it is necessary to install the OpenSSO provider in the maven repository 
(see the explained steps in readme.txt file located in https://opensso.dev.java.net/source/browse/opensso/extensions/spring2provider/providers). 
The next step is very simple:

    $ mvn package

To deploy the application in Tomcat you must execute:

    $ mvn tomcat:deploy

when tomcat is running. But first you must configure the file {user home}/.m2/settins.xml 
(See documentation http://mojo.codehaus.org/tomcat-maven-plugin/)

To deploy the application in Glassfish you must execute:

    $ mvn org.codehaus.mojo:exec-maven-plugin:exec

But first the property glassfish.home must be defined un the file {user home}/.m2/settins.xml 
(See details in http://mojo.codehaus.org/exec-maven-plugin/java-mojo.html)


Execution
---------

If you want to execute the application in a servlet container without OpenSSO agent, 
you must define the system properties "com.iplanet.am.cookie.name" and 
"com.sun.identity.federation.fedCookieName".

For example in Tomcat, you must write in a command shell:

    $ export CATALINA_OPTS=-Dcom.iplanet.am.cookie.name=iPlanetDirectoryPro -Dcom.sun.identity.federation.fedCookieName=fedCookie $CATALINA_OPTS

or in windows:
    > set CATALINA_OPTS=-Dcom.iplanet.am.cookie.name=iPlanetDirectoryPro -Dcom.sun.identity.federation.fedCookieName=fedCookie %CATALINA_OPTS%


If you are using glassfish you can set this properties in the JVM options settings in the admin application


Single sign-on
--------------

To test Single sign-on is very simple. Open two tabs in the same browser. In the first
tab open the example application, in the second tab open another OpenSSO application, 
for example, OpenSSO manager. And execute the following scenarios:

- Scenario 1
1.- Login in the example application with a LDAP user (amadmin, for example)
2.- Reload Opensso application page and see you are already login 
3.- Logout of example application
4.- Reload Opensso application page and see you are already logout 

- Scenario 2
1.- Login in the Opensso application with a LDAP user
2.- Reload example application page and see you are already login 
3.- Logout of Opensso application
4.- Reload example application page and see you are already logout 


Authorization
-------------

To test authoritation through OpenSSO configuration we must to define the
realm policies in OpenSSO manager web. But first web need a user group to 
assign it. Then we select Access Control -> Realm -> Subjects -> Group -> 
"New ...". We write a name, for example, "Staff". And in the "User" tab, 
we add several users.

In OpenSSO, create two groups "staff" and "admins"

Add a sample user to each group (e.g. put demo1 in the admins group,
demo2 is in staff)



Create a URL policy by selecting Policies -> "New Policy" and assign it
a name, for example, "Protected Info". 

In the Rules section we create one "URL Policy Agent" with the following data:
    Name: Protected Info
    Resource Name: http://*:*/faces/protected*
    Actions:
        GET     Deny
        POST    Deny
    Subject: Admins  ** Exclusive **

Make sure to check the "exclusive" box in the Subject selection. This will apply
this policy to everyone EXCEPT Admins

Create another blanket policy to allow access to the application:
Resource Name: http://*:*/faces/*
 Actions:
        GET     Allow
        POST    Allow
 Subject: All authenicated users
    

Experiment with the application by logging on with your demo accounts.

Note that:
- The user is redirected to OpenSSO for authentication
- There are sample staff pages that print a different message if you
are staff vs. an admin
- Only an admin user should have access to the protected/ section




Conclusion
----------

We can see the configuration for Single Sign-On is very simple. And we have got to 
centralize the security policy definition in an unique place.

The integration of OpenSSO with Spring Security gives additional features. For example, 
Spring AOP is very useful to associate policies to business objects without write it 
in the code. So the security configuration of the business objects is independient of
what application is using it. Therefore, it is very usefull for distributed application
within a security realm.

Besides, this provider can be used in others J2EE modules as an EJB which is accessed
by RMI.

If you are interested in how it is possible to defined new rule types, see the article 
http://developers.sun.com/identity/reference/techart/secureapps.html.


