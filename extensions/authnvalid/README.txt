VALid Authentication Module

Notes
=====

* OpenSSO Libraries
The following files can be retrieved from the <OPENSSO_INSTALL_DIR>/WEB-INF/lib
amserver.jar
opensso-sharedlib.jar

* VALid Client Library
This authentication module is written for VALid Client Library version 4.15.1. All VALid library files need to be obtained from VALid and be placed in the extlib folder.

* Deployment
The quick-deploy target of the build.xml file contains instructions on how to copy relevant files to the OpenSSO section. 

* Configuration
Note that the VALid server name(s) need(s) to be specified in servicelist.xml and this file needs to be in the CLASSPATH of the OpenSSO web application (as all other .xml files from VALid).
The administration console setting for server names and SSL usage does not take effect and is meant to be used once the VALid client offers different ways to pass the parameters other than the .xml file.

Future Directions
=================

* Allow user to choose method & Progress Status
Going forward it will be important to leverage more the flexibility of VALid authentication process. This is giving the user the ability to chose the authentication method (e.g. ring back on landline or cell) and display the progress message of this process. This might have impact on the login process (login.jsp, view bean, can it all be done through JAAS ? ).
