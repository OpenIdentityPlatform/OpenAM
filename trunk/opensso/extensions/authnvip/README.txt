Verisign Identity Protection Authentication Module


1) Download the Verisign Identity Protection Developer Test Drive
https://vipdeveloper.verisign.com/vip/home.jsp

2) Unpack the zip file.

3) Use the vip_testdrive.wsdl, viptestdrive.xsd, and vip_testdrive_common to create webservice client
<Unpacked VIP Directory>/API

4) This code packages the generated JAX-WS client into a jar file called vipSoapInterfaceService.jar
It is packaged as: com.verisign._2006._08.vipservice.*

5) VIPWebServiceClient is a client file that uses the generated code above and makes necessary endpoint changes.  See VIP Developer API and VIPWebServiceClient.java for more information

5) Add VIPLoginModule.xml to <OPENSSO_INSTALL_DIR>/auth/default

6) Compile Files.   Build.xml hasn't been uploaded yet.  On To-Do list

7) Create jar file containing VIPLoginModule.class, VIPPrincipal.class, and VIPWebServiceClient.class

8) Add vipSoapInterface.jar and the jar file created above to <OPENSSO_INSTALL_DIR>/WEB-INF/lib

9) Import the testdrive.p12 cert into the JKS keystore.   Easiest mechanism is to use PKCS12Import

10) Add com.sun.VIPLoginModule to the list of pluggable authentication module classes
Configuration->Authentication->Core page

11) Verify with http://openssohost:port/opensso/UI/Login?module=VIPLoginModule

12) Be sure to read the VIP Developer Test Drive documentation on how to generate test tokens


Structure of authnvip

authnvip
|
|---> source 
|   |
|   |--->/com
|       |
|       |--->/sun  !all source files
|
|
|
|
|--->build.xml /build.xml to create the jar file.

