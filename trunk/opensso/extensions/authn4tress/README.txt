OpenSSO Authentication Module for ActivIdentity 4Tress

In order to run the build.xml to compile the jar file the following files need to be in the lib dir:

The following can be retrieve from : http://apache.rmplc.co.uk/ws/axis/1_4/
axis.jar (from Apache Axis 1.4)
log4j-1.2.8.jar
axis.jar
wsdl4j-1.5.1.jar
commons-discovery-0.2.jar
commons-logging-1.0.4.jar

The following files can be retrieved from the <OPENSSO_INSTALL_DIR>/WEB-INF/lib
jaxrpc-api.jar
jaxrpc-impl.jar
jaxrpc-spi.jar
opensso.jar

Structure of authn4Tress

authn4Tress
|
|___> source !all source files
|
|---> lib !All libraries 
|
|---> config !All configuration files for module
|	|---> amAuthFortressAuthUserOTP.xml !Service Configuration
|	|--->*.properties 
|	|--->FortressAuthUserOTPModule.xml !Module Definition
|	|--->FortressAuthnRegistration !Attribute properties file for famadm
|
|
|--->built
|	|	
|	|--->/classes
|	|
|	|--->/dist
|
|--->build.xml /build.xml to create the jar file.


N.B There is no bin directory with a pre-compiled jar file
