OpenSSO Authentication Module for Hitachi Finger Vein Biometric.

In order to run build.xml, edit build.properties as follows:

# path to your application server installation
appserver.home=C:/Sun/glassfish-v2-ur1
# path to your OpenSSO libraries
opensso.lib=<OpenSSO_INSTALL_DIR>/WEB-INF/lib
# URL at which Hitachi Finger Vein Authentication Server is running
authserver.url=http://xx.xx.xx.xx/AuthService/AuthService.asmx?WSDL

It builds two jar files, hitachi-fvauth.jar and am-fvauth.jar under the "dist" directory.  Copy those files to:

<OPENSSO_INSTALL_DIR>/WEB-INF/lib

There are 4 files under "config" directory.  Copy/Edit those files as follows:

1) am Fvauth.properties 

Copy it to <OPENSSO_INSTALL_DIR>/WEB-INF/classes.  You may need to adjust the nave/value settings.

2) FvAuth.xml, LoginFV1.jsp and LoginFV2.jsp

Copy these files to <OPENSSO_INSTALL_DIR>/auth/default.  You may need to copy and modify them depending on your native language.

Register "FvAuth" as an authentication method as follows:

1) Login to Access Manager Console as amadmin.
2) Click on "Configuration" tab. 
3) Select "Core" under the Authentication table. 
4) Add "com.sun.identity.authentication.modules.fvauth.FvAuth" to "Pluggable Auth Modules Classes" attribute. 
5) Click on save button to save the changes 

