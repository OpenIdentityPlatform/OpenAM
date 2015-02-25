Instructions
============

1. Build the tamauthnfilter project, either in NetBeans or from command line ant.

    tamauthnfilter.jar should be created in tamauthnfilter/dist/

2. Copy tamauthnfilter.jar to <OPENSSO_DEPLOY_DIR>/WEB-INF/lib

3. Configure HTTP-header name and value mapping

    1) Login to Access Manager Console as amadmin.

    2) Click on the "Configuration" tab.

    3) Click on the "Servers and Sites" tab.

    4) Click on your Server link.

    5) Click on the "Advanced" tab.

    6) Click the "Add..." button

        a. Fill in "be.is4u.eai.mapping" as the "Property Name".

        b. Fill in two values separated by a semicolon (;) for "Property Value".

            The first value is the name of the HTTP Response Header to be sent
            back to Tivoli Access Manager/WebSEAL.

            The second value is the value of the HTTP Response Header to be sent
            back to Tivoli Access Manager/WebSEAL, where the "%name%" marker
            will be replaced by the userid of the OpenSSO user resulting in a
            known user DN in the Tivoli Access Manager/WebSEAL LDAP store.

            e.g. the property value

                "am-eai-user-id;cn=%name%,ou=users,o=is4u,c=be"

            indicates that the HTTP Response Header to be sent back to Tivoli
            Access Manager/WebSEAL is named "am-eai-user-id" and will have a
            value of "cn=<userid of OpenSSO user>,ou=users,o=is4u,c=be".

    7) Click the "Save" button.

4. Configure a "Post Authentication Processing Class"

    in the "All Core Settings" or in an "Authentication Chaining" in the
     "Authentication" tab.

    1) Fill out the "New Value" field with:

        "com.sun.identity.authentication.spi.WebsealExternalAuthenticationIntegration"

    2) Click the "Add" button.

    3) Click the "Save" button.

5. Restart OpenSSO.

6. Test

    1) Authenticate to OpenSSO, using the authentication chain to which
        the "Post Authentication Processing Class" was added.

    2) Verify that the correct HTTP Response header name and value are sent to
        the browser (in FireFox, you could use the HttpFox add-on for this)

7. Configure an EAI junction for Tivoli Access Manager/WebSEAL
    allowing Tivoli Access Manager/WebSEAL to externalise its authentication to
    OpenSSO.

Questions and feedback about this extenstion can be sent to [info at is4u.be]