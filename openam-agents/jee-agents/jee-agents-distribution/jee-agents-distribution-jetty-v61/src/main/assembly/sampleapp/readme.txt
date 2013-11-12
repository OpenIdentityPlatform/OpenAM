<!--
   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  
   Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
  
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
   "Portions Copyrighted [year] [name of copyright owner]"

   $Id: readme.txt,v 1.1 2009/01/21 18:36:18 kanduls Exp $

-->

<!--
     Portions Copyrighted 2013 ForgeRock AS.
-->

------------------------------------
J2EE Policy Agent Sample Application
------------------------------------

This document describes how to use the agent sample application in conjunction 
with the Jetty Server and the J2EE Agent. Please note that 
the agent needs to be installed first before deploying this sample application.

    * Overview
    * Configure the OpenAM server
    * Configure the agent properties
    * Deploying the Sample Application
    * Running the Sample Application
    * Troubleshooting
    * Optional Steps
    **Compiling and Assembling the Application


Overview
--------

The sample application is a collection of servlets and JSPs that demonstrate the salient 
features of the J2EE policy Agent. These features include SSO, web-tier declarative security, 
URL policy evaluation and session/policy/profile attribute fetch.

The sample application agentsample.war is located in the <agent_install_root>/sampleapp/dist 
directory. It can however be rebuilt is required and the next section describes the
steps required to rebuild the sample application.

The sample application is supported for Policy Agent 3.0.

Configure the OpenAM server
----------------------------------------------
This agent sample application requires that the OpenAM server is configured
with the subjects and policies required by the sample application.

On OpenAM admin console, do the following configuration.
1.  Create the following users:
    Here is the following list of users with username/password :

    * andy/andy
    * bob/bob
    * chris/chris
    * dave/dave
    * ellen/ellen
    * frank/frank
    * gina/gina

2. Assign Users to Groups
   Create new groups for employee, manager, everyone and customer. Then assign 
   the users to the groups as follows:

    * employee:
          o andy, bob, chris, dave, ellen, frank
    * manager:
          o andy, bob, chris
    * everyone:
          o andy, bob, chris, dave, ellen, frank, gina
    * customer:
          o chris, ellen
    
3. Create the following URL Policies:
   In the following URLs, replace the <hostname> and <port> with the 
   actual fully qualified host name and port on which the sample 
   application will be running.

    * Policy 1:
          o allow:
                + http://<hostname>:<port>/agentsample/jsp/*
                + http://<hostname>:<port>/agentsample/protectedservlet
                + http://<hostname>:<port>/agentsample/securityawareservlet
          o Subject: all authenticated users.                     
    * Policy 2:
          o allow:
                + http://<hostname>:<port>/agentsample/urlpolicyservlet
          o Subject: Group: customer

Configure the agent properties
--------------------------------------------

   If the agent configuration is centralized, then do the following steps.
   1). Login to OpenAM console as amadmin user
   2). Navigate to Access Control/realm/Agents/J2EE, and click on the agent 
       instance link (assume the agent instance is already created, otherwise 
       refer to the agent doc to create the agent instance).
   3). In tab "Application", section "Access Denied URI Processing", property 
       "Resource Access Denied URI", enter agentsample in the Map Key field, 
       /agentsample/authentication/accessdenied.html in the Map Value field, and
       SAVE the change.
   4). In tab "Application", section "Login Processing", property "Login Form URI",
       add /agentsample/authentication/login.html, and SAVE the change.
   5). In tab "Application", section "Not Enforced URI Processing", property 
       "Not Enforced URIs", add the following entries:
          /agentsample/public/*
          /agentsample/images/*
          /agentsample/styles/*
          /agentsample/index.html
          /agentsample
       and SAVE the change. 
   
   If the agent configuration is local, then edit the local agent configuration
   file OpenSSOAgentConfiguration.properties located at the directory 
   <agent_install_root>/Agent_<instance_number>/config with following changes: 

    * Not enforced List:
      com.sun.identity.agents.config.notenforced.uri[0] = /agentsample/public/*
      com.sun.identity.agents.config.notenforced.uri[1] = /agentsample/images/*
      com.sun.identity.agents.config.notenforced.uri[2] = /agentsample/styles/*
      com.sun.identity.agents.config.notenforced.uri[3] = /agentsample/index.html
      com.sun.identity.agents.config.notenforced.uri[4] = /agentsample

    * Access Denied URI:
      com.sun.identity.agents.config.access.denied.uri[agentsample] = /agentsample/authentication/accessdenied.html
    * Form List:
      com.sun.identity.agents.config.login.form[0] = /agentsample/authentication/login.html

   Optionally, you can try out the fetch mode features that allow the agent to
   fetch some values and make them available to your application. For example, 
   you can fetch user profile values(like email or full name) from the user data
   store of your OpenAM setup and make them available to your application code
   (through cookies, headers, or request attributes) for application 
   customization. See the Policy Agent 3.0 for details about the fetching 
   attributes for details on using this feature. If you change the agent's 
   configuartion for the attribute fetching, the showHttpHeaders.jsp page of the
   sample application will show all the attributes being fetched. You can choose
   to try this later after you have already installed and deployed the agent and
   sample application in order to learn about this feature.

Running the Sample Application
----------------------------
You can run the application through the following URL:

http://<hostname>:<port>/agentsample

Traverse the various links to understand each agent feature.


Troubleshooting
----------------------------
If you encounter problems when running the application, review the log files to learn what exactly 
went wrong. J2EE Agent logs can be found at <agent_install_root>/<agent_instance>/logs/debug directory.


Optional Steps   
----------------------------

Compiling and Assembling the Application (Optional)
----------------------------------------------------

This section contains instructions to build and assemble the sample application using a Command 
Line Interface (CLI).

To rebuild the entire application from scratch, follow these steps:

   1. Set your JAVA_HOME and CLASSPATH to JDK1.4 or above.
   2. Replace 'SERVER_LIB_DIR' in build.xml with the directory where servlet-api.jar is located. 
      For Example: Replace SERVER_LIB_DIR with /opt/jetty/lib 
   3. Compile and assemble the application.
      Download a binary distribution of Ant 1.6.x.
	  Set the ANT_HOME env variable to the directory where ant has been installed.
      Execute the command <ant_home>/bin/ant 
      under <agent_install_root>/sampleapp/ to execute the default target build 
      and rebuild the WAR file.

      The build target creates a built and dist directory with the WAR file.
      By default, the deployment descriptors assume that the OpenAM
      product was installed under default Org/Realm "dc=opensso,dc=java,dc=net". If the 
      Org/Realm for the deployment scenario is different from the default root 
      suffix, the Universal Id(uuid) for the role/principal mappings should be changed 
      accordingly.  The Universal Id can be verified in OpenAM console.

   4. Deploy the application. After you have re-created the sample application 
   	  from scratch, you may proceed directly to Deploying the Sample Application, 
   	  or optionally perform step 3.
   5. Optionally you can run 'ant rebuild' to clean the application project area 
   	  and run a new build.

Now you are ready to use the dist/agentsample.war file for deployment.

