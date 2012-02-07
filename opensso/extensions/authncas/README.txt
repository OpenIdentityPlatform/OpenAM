CAS Authentication Module
=========================

Qingfeng Zhang has recorded a video showing the integration
 http://qingfeng.tech.officelive.com/Documents/cas+am.html

If you have any questions on the video, please email qingfeng.zhang@gmail.com

Instructions
============

1. Obtain the CAS Client JAR file. You can download it from
http://www.ibiblio.org/maven/cas/jars/casclient-2.0.11.jar or get it from
cas.rar. To build the authentication module via NetBeans or ant, you will need
to copy casclient-2.0.11.jar to opensso/products/extlib/casclient.jar. The
easiest way to do the latter, assuming you have wget, is simply

$ cd opensso/products/extlib/
$ wget -O casclient.jar http://www.ibiblio.org/maven/cas/jars/casclient-2.0.11.jar

2. Build the authncas project, either in NetBeans or from command line ant.
authncas.jar should be created in authncas/dist/

3. Install CAS Server Certificate to OpenSSO hosted Web Server or Application
Server.

4.
Copy CASLoginModule.xml to <OPENSSO_DEPLOY_DIR>/config/auth/default
Copy dist/authncas.jar to <OPENSSO_DEPLOY_DIR>/WEB-INF/lib
Copy casclient.jar to <OPENSSO_DEPLOY_DIR>/WEB-INF/lib
Copy amAuthCAS.properties to <OPENSSO_INSTALL_DIR>/locale

5. Register "CAS" as an authentication method as follows:

a. Register CAS Service using command line (for example):
$ amadmin --runasdn uid=amadmin,ou=people,dc=red,dc=iplanet,dc=com --password \
        password --schema amAuthCAS.xml

b. Add Pluggable Auth Module Class 
1) Login to Access Manager Console as amadmin.
2) Click on "Service Configuration" tab. 
3) Select "Core" under the Authentication table. 
4) Add "com.sun.identity.authentication.modules.cas.CASLoginModule" to
    "Pluggable Auth Modules Classes" attribute.
5) Click on save button to save the changes 

6. Change the Global Setting for CAS Module
1) Login to Access Manager Console as amadmin.
2) Click on "Service Configuration" tab. 
3) Click the arrow next to "CAS".
4) Enter the appropriate CAS Login service properties. 
5) Click on save button to save the changes 

7. Add CAS Service for Organization (Root or Sub-Org)
1) Login to Access Manager Console as amadmin.
2) Click on "Identity Configuration" tab. 
3) Select View "Service". 
4) Add "CAS" Service. 
5) Click on save button to save the changes    
   
8. Restart

9. Test (Example links)
   Root Org: http://qingfeng:8080/amserver/UI/Login?module=CAS&goto=http://qingfeng:8080/portal/dt    
   Sub Org: http://qingfeng:8080/amserver/UI/Login?module=CAS&org=DeveloperSample&goto=http://qingfeng:8080/portal/dt      