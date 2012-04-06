OpenSSO identity provider for Information Cards. This server hosts OpenSSO 
Security Token Service (IP/STS) and Information Card issuing servlet.

Currently, configuration of the IP/STS is somewhat complex. This will improve
as the code is developed further.

--------------------------------------------------------------------------------
There are several libraries required to build the authnicip extension. Create a 
directory named lib under the authnicip root (opensso/extensions/authnicip) 
containing the following JAR files:
--------------------------------------------------------------------------------

xmldap-1.0.jar:
    The Xmldap.org code can be retrieved from the 'openinfocard' project at
    http://code.google.com/p/openinfocard/
    To build:
        svn checkout http://openinfocard.googlecode.com/svn/trunk/ \
        openinfocard-read-only
        cd openinfocard-read-only/ant
        ant
    Copy xmldap-1.0.jar from openinfocard-read-only/build/xmldap-1.0/ to 
    lib

opensso.jar:
opensso-sharedlib.jar
openfedlib.jar
fam.jar
    The above files can be retrieved from the <OPENSSO_INSTALL_DIR>/WEB-INF/lib
    or by downloading and building the OpenSSO source.

webservices-api.jar
webservices-rt.jar
    You should get these files from the latest Metro 1.2 nightly build - see
    http://tinyurl.com/5vr2pm 

j2ee.jar:
    Copy this from your application server's lib directory or download the Java 
    EE 5 SDK from http://java.sun.com/javaee/downloads

--------------------------------------------------------------------------------
Build authnicip
--------------------------------------------------------------------------------
    cd opensso/extensions/authnicip
    ant

--------------------------------------------------------------------------------
To install Security Token Service and Information Card issuing servlet in 
OpenSSO:
--------------------------------------------------------------------------------

1 - Install OpenSSO (Download latest OpenSSO v1 build from 
    https://opensso.dev.java.net/public/use/index.html or build from source). 

2 - Configure the OpenSSO web application with "https" protocol, rather than 
    http. This is important - a CardSpace STS MUST be deployed on an https URL!

3 - Copy contents of xml/web.xml into <OPENSSO_INSTALL_DIR>/WEB-INF/web.xml in
    <context-param> section. 
    Change "path to sun_logo_rgb.gif" to appropriate image path on the server.
    <OPENSSO_INSTALL_DIR> on Glassfish will be something like 
    <GLASSFISH_DIR>/domains/domain1/applications/j2ee-modules/opensso/

4 - Merge the contents of build/dist/Authnicip.jar into OpenSSO's fam.war. You
    can use the following sequence of commands for this:
        mkdir $TMPDIR/fam
        cd $TMPDIR/fam
        jar xvf <OPENSSO_INSTALL_DIR>/WEB-INF/lib/fam.jar
        jar xvf <SOME_PATH>/opensso/extensions/authnicip/build/dist/Authnicip.jar 
        jar cvf <OPENSSO_INSTALL_DIR>/WEB-INF/lib/fam.jar *
        rm -r $TMPDIR/fam

5 - Copy lib/xmldap-1.0.jar into <OPENSSO_INSTALL_DIR>/WEB-INF/lib

6 - (Optional) Copy your keystore.jks file into 
    <OPENSSO_INSTALL_DIR>/WEB-INF/template/keystore
    Make sure your keypass and storepass is "secret" and certificate alias is 
    "test".
    You can use the default OpenSSO keystore.jks for testing - note that the 
    token signing cert must be the same as the SSL cert!!!

7 - Copy all files under images/ into <OPENSSO_INSTALL_DIR>/images

8 - Edit the <OPENSSO_INSTALL_DIR>/WEB-INF/wsdl/famsts.wsdl file to define
    - Required binding type at "<sp:TransportBinding>"
    - Required Authentication token at "<sp:SignedSupportingTokens>"
    - Required Validation configuration at "<sc:ValidatorConfiguration>"
    You can use wsdl/famsts.wsdl as a starting point - this default wsdl 
    configures CardSpace username/password authentication with TransportBinding.

9 - Add a binding attribute with value 
    "http://www.w3.org/2003/05/soap/bindings/HTTP/" to the <endpoint> element in 
    <OPENSSO_INSTALL_DIR>/WEB-INF/sun-jaxws.xml, so it looks like this:

        <endpoint
            name="sts_mex"
            implementation="com.sun.xml.ws.mex.server.MEXEndpoint"
            binding="http://www.w3.org/2003/05/soap/bindings/HTTP/"
            url-pattern="/sts/mex" />

10 - Reload the OpenSSO web application. If you have used autodeploy on 
    Glassfish to deploy OpenSSO, you can simply do
        touch <OPENSSO_INSTALL_DIR>/.reload

11 - Login to OpenSSO Administration console (e.g. 
    https://server.example.com:8181/opensso) as "amadmin" user.
    Go to Configuration → Agents → Web Service Provider → click "wsp" agent 
    profile → set "Authentication chain" as "ldapService" from the available 
    drop down list.

    Note that the 'security mechanism' on this screen controls the default token 
    type.

12 - Go to Configuration → Global → Security Token Service → change the value of 
    "Token Implementation Class" attribute to 
    "com.sun.xml.ws.security.trust.impl.ic.ICContractImpl"
    You should also verify that the certificate alias and issuer URL on this 
    screen match your configuration.

13 - Access https://server.example.com:8181/opensso/GetCard
    This will redirect to OpenSSO Authentication service. Authenticate using
    any existing OpenSSO user. Upon successful authentication, an InfoCard for 
    the authenticated OpenSSO user will be issued.

11 - You can use this saved OpenSSO InfoCard to login to any RP 
    (e.g. https://xmldap.org/relyingparty) which accepts Infocard login.

For further explanations please email dev@opensso.dev.java.net