------------------------------------------------------------------------------
README file for the .NET Fedlet
------------------------------------------------------------------------------
   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

Copyright (c) 2009-2010 Sun Microsystems Inc. All Rights Reserved

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

Portions Copyright 2012-2013 ForgeRock AS


%% Contents:
    %%  1. Contents of this directory
    %%  2. What is currently supported
    %%  3. How to configure and deploy the Fedlet for .NET
    %%  4. How to use the Sample Application to test your deployment
    %%  5. How to integrate with existing application after Single Sign-on
    %%  6. How to enable support for multiple Identity Providers
    %%  7. How to enable Identity Provider Discovery Service
    %%  8. How to enable the Windows Event Log for debugging
    %%  9. How to enable Signing of Requests/Responses
    %% 10. Subtle differences between Java and .NET Fedlet


%% 1. Contents of this directory
   This README file provides information on the Fedlet for .NET without a
   pre-configured Identity Provider (IDP) and Fedlet (SP) metadata.  Manual
   steps are needed to set up the Fedlet for .NET to work with a remote IDP.

   Fedlet-unconfigured.zip
     |- asp.net
         |
         |- bin
         |   |- Fedlet.dll        The DLL to deploy in the bin/ folder of
         |                        your application.
         |
         |- conf                  Folder containing template metadata files
         |                        for use by the Fedlet for .NET applications.
         |
         |- SampleApp             The sample application to demonstrate
         |                        connectivity between the remote IDP and
         |                        the Fedlet (SP).
         |
         |- readme.txt            This README file. The file shows how to
                                  install, configure, and use the Fedlet.


%% 2. What is currently supported
   The Fedlet.dll currently supports IDP and SP initiated Single Sign On (SSO)
   with POST and Artifact binding.  Multiple IDP and IDP Discovery are also
   supported with the aforementioned SSO.  In addition, IDP and SP initiated
   Single Logout Out is also supported. These features are made available by
   providing ASP.NET developers an API and an example sample application to
   retrieve an AuthnResponse from their IDP.  Details are described in
   Section 5 below.

%% 3. How to configure and deploy the Fedlet for .NET
   The Fedlet.dll contains all the necessary bits for the Fedlet provide
   ASP.NET developers an interface to a light-weight SAMLv2 Service Provider
   API. These developers can use the API to initiate single sign on to an
   Identity Service Provider and receive an HTTP-POST in their application to
   retrieve useful information provided in the AuthnResponse.

   Steps to configure and deploy the Fedlet for .NET:
      a) Extract the asp.net/ folder within the Fedlet-unconfigured.zip to a
         temporary directory.
      b) Within the conf/ folder, edit the template files by changing
         the following tags:

    FEDLET_COT        : Replace with the name of your circle of trust.
    FEDLET_ENTITY_ID  : Replace with the name of the entity id for your Fedlet.
    FEDLET_DEPLOY_URI : Replace with the url of the Fedlet.
                        http://sp.example.com/SampleApp/
    IDP_ENTITY_ID     : Replace with the name of the entity id of the remote
                        Identity Provider.

      c) Copy the edited files above to your application's App_Data/ folder.
      d) Obtain the standard metadata XML file from your IDP, name it idp.xml,
         and place in your application's App_Data/ folder.  If your IDP is
         an OpenAM deployment, this can be exported by accessing the export
         URL.  For example:
         http://idp.example.com:8080/openam/saml2/jsp/exportmetadata.jsp
      e) Provide the Fedlet metadata XML file "sp.xml" to your IDP.  The
         metadata must be imported to the IDP machine and must be associated
         with the same Circle of Trust as the IDP. If your IDP is an OpenAM
         deployment, use the Register Remote Service Provider workflow
         available from the Common Tasks page to import your Fedlet's
         metadata.
      f) Configuration is now complete.
      g) Copy the Fedlet.dll from the bin/ folder to your application's bin/
         folder.

   By having the Fedlet artifacts deployed specifically in the App_Data/ and
   bin/ folder of your hosted application, multiple instances of the Fedlet with
   its own configuration can be co-deployed in the same Internet Information
   Server (IIS).

   NOTE REGARDING MODIFICATIONS TO SAMLv2 METADATA:

   Be sure to convey information regarding any changes made in the service
   provider metadata to the identity provider so it can make the corresponding
   changes to its own configuration. A modified sp.xml file may be sent to
   the identity provider but any modifications made to sp-extended.xml should
   be conveyed to the identity provider using a different method. Once the
   identity provider receives the appropriate standard and extended metadata
   values, it can make the changes using the OpenAM console. Information on
   customizing SAMLv2 providers using the OpenAM console is available in the
   OpenAM documentation.

   * SAMLv2 Service Provider Customization link:
     https://forgerock.org/openam/doc/bootstrap/saml2-guide/index.html#saml2-create-hosted-sp
   * SAMLv2 Identity Provider Customization link:
     https://forgerock.org/openam/doc/bootstrap/saml2-guide/index.html#saml2-configure-remote-idp

   If the identity provider is using a product other than OpenAM,
   they would make the changes according to their product's documentation.

%% 4. How to use the Sample Application to test your deployment
   The Sample Application could be used to test your deployment of the Fedlet
   for your .NET applications.

   Steps to deploy the Sample Application:
      a) Install the Sample Application on your Service Provider
          i) Navigate to the asp.net/ folder extracted from the
             Fedlet-unconfigured.zip as described in Section 3 above.
         ii) Copy over the metadata files edited in Section 3 above and place
             into the SampleApp/App_Data folder.
             - The following files should have been copied over:
               idp.xml, idp-extended.xml, sp.xml, sp-extended.xml, fedlet.cot
             - The files in the existing sample application were configured
               for idp.example.com and sp.example.com and are expected to be
               replaced for your installation.
        iii) Within Internet Information Server, create a virtual directory
             with the SampleApp/ folder found within the unzipped folder.
             - IIS 6 has Add Virtual Directory.  Be sure to have Read and
               Script permissions set for the application.
             - IIS 7 has Add Application with no additional options required
               to be altered.
      b) Try out the Sample Application.
           i) Open the SampleApp in your browser. For example:
              http://sp.example.com/SampleApp
          ii) Click the link to perform the IDP initiated SSO.
         iii) Enter in your credentials (such as demo / changeit ).
          iv) After the form submission, you should be at the
              fedletapplication.aspx page with access to the AuthnResponse
              information.

%% 5. How to integrate with existing application after Single Sign-on
   The Sample Application described above demonstrates possible usage by
   ASP.NET developers. Once the application has the necessary artifacts
   installed, a specific URI is required to receive the HTTP-POST containing
   the SAMLv2 response after successful authentication by the IDP.  The
   following example shows how the developer would retrieve this information:

       AuthnResponse authnResponse = null;
       try
       {
           ServiceProviderUtility spu = new ServiceProviderUtility(Context);
           authnResponse = spu.GetAuthnResponse(Context);
       }
       catch (Saml2Exception se)
       {
           // invalid AuthnResponse received
       }
       catch (ServiceProviderUtilityException spue)
       {
           // issues with deployment (reading metadata)
       }

   If a SAML response was received the authnResponse object will be populated
   with the assertion information.  The sample application demonstrates how to
   retreive attributes and sujbect information from this object.

%% 6. How to enable support for multiple Identity Providers
   To configure a second Identity Provider with this Fedlet:

      a) Get the standard metadata XML file for the new Identity Provider, name
         the XML file as "idp2.xml" and copy it to the App_Data/ folder.
      b) Decide on the circle-of-trust (COT) the new Identity Provider would
         belong. This IDP could be added to an existing COT (e.g. "fedlet.cot")
         or a new COT.
          i)  To add the Identity Provider to an existing COT, edit the
              corresponding COT file (e.g. "fedlet.cot") within the App_Data/
              folder, and append the new IDP entity ID (specified by the
              "entityID" attribute in the "idp2.xml" metadata file) to the
              value of "sun-fm-trusted-providers" attribute using "," as the
              separator.
         ii)  To add to a new circle-of-trust:
              - Create a new file named "fedlet2.cot" and place within the
                App_Data/ folder. Use the existing fedlet.cot as a template,
                but change the value of attribute "cot-name" to the actual name
                of the new COT (e.g. "cot2"), and include both the new IDP
                entity ID and the Fedlet entity ID as value for
                "sun-fm-trusted-providers" attribute (two entity IDs separated
                by ",").
              - Edit the sp-extended.xml file, add the new COT name to the
                value of "cotlist" attribute. For example:

                <Attribute name="cotlist">
                  <Value>saml2cot</Value>
                  <Value>cot2</Value>
                </Attribute>

      c) Create a new "idp2-extended.xml" file as the extended metadata for the
         new Identity Provider. Use the existing idp-extended.xml as a template
         but change the "entityID" to the new IDP entity ID, change the value
         for "cotlist" attribute to the COT name if a new COT is created for
         the IDP.

         Note: Make sure the second IDP is a remote IDP by setting the
               "hosted" attribute in the EntityConfig element to "false".

      d) Send the .NET Fedlet metadata XML file (i.e. "sp.xml" within the
         App_Data/ folder) to the second IDP, import the metadata in the remote
         IDP and add it to the same circle-of-trust as the IDP.

   Repeat the same steps for the third, fourth, ... and [X]th IDP, using
   idpX.xml/idpX-extended.xml/fedletX.cot as standard meta/extended meta/COT
   name for the new IDP. Restart the Application Pool associated with your
   .NET application to make the change effective.

   If you have performed the above with the Sample Application, returning to the
   default page will now provide you with a list of IDPs to perform single sign
   on.

%% 7. How to enable Identity Provider Discovery Service

   When the .NET Fedlet is configured with multiple Identity Providers in a
   COT, it could additionally be configured to use an IDP Discovery Service to
   determine the preferred IDP.

   In order to leverage this functionality, you first need to have the Identity
   Provider Discovery Service set up and deployed before performing the steps
   listed below.  If you installed the OpenAM WAR, the IDP Discovery Service is
   already bundled with the product.  Alternately, you could follow the
   documented process in creating a separate WAR for just the IDP Discovery
   Service.  Please refer to the OpenAM documentation on how to set up and use
   the IDP Discovery Service.  After configuring this service, take note of the
   reader service URL (URL to find out the preferred IDP) and the writer service
   URL (URL to write the preferred IDP), they are needed in the steps below. If
   you are using OpenAM, the reader service URL is typically:

     <protocol>://<host>:<port>/deploy_uri/saml2reader
     (for example: http://discovery.common.com/openam/saml2reader)

   Likewise, the writer service URL is typically:

     <protocol>://<host>:<port>/deploy_uri/saml2writer
     (for example: http://discovery.common.com/openam/saml2writer)

   To configure the .NET Fedlet to support IDP discovery:

      a) Edit the COT file (e.g. "fedlet.cot"), and set the value for attribute
         "sun-fm-saml2-readerservice-url" to the SAML2 reader service URL
         (e.g. http://discovery.common.com/openam/saml2reader), set the value
         for attribute "sun-fm-saml2-writerservice-url" to the SAML2 writer
         service URL (e.g. http://discovery.common.com/openam/saml2writer).
      b) Restart the Application Pool associated with your .NET application to
         make the change effective.
      c) Setup IDP discovery on each of your remote IDPs. If the IDP is an
         OpenAM server instance, you need go to the administration console,
         find the COT for the IDP and .NET Fedlet, and specify the SAML2 reader
         service URL and SAML2 writer service URL, and Save.
      d) If you have performed the above with the Sample Application and have
         configured it with multiple Identity Providers, returning to the
         default page will now provide you a link to "use the IDP Discovery
         Service".

          i) If no IDP has been established as the preferred IDP,
             clicking on this link will arbitrarily redirect you to one of the
             configured IDPs for authentication.  Once authenticated, this IDP
             will be designated as the preferred IDP by the discovery service.
         ii) If an IDP has already been established as the preferred IDP,
             clicking on this link will again redirect you to this IDP for
             authentication.

%% 8. How to enable the Windows Event Log for debugging

   Since the .NET Fedlet does not require an installer, minimal manual steps are
   required to enable log messages to be written to the Windows Event Log.  The
   steps below require edits to the Windows Registry so please take necesary
   precautions as described in the Microsoft Knowledge Base article referenced
   below.

   To configure the .NET Fedlet to write to the Windows Event Log:

      a) The instructions for enabling a .NET application to write events to the
         Windows Event Log are described at the following Microsoft Help and
         Support article:

         http://support.microsoft.com/default.aspx?scid=kb;en-us;329291

         Following these instructions, add a new key under
         HKEY_LOCAL_MACHINE\SYSTEM\CurrentControlSet\Services\Eventlog\Application
         with "Fedlet" as the key.

      b) Edit the <appSettings/> element within your .NET application's
         Web.config file to specify the .NET Fedlet log level.  The key to add
         is called "fedletLogLevel" and the possible values are "info",
         "warning", and "error".  An example for enabling "info" level logging
         is shown below:

            <appSettings>
                <add key="fedletLogLevel" value="info"/>
            </appSettings>

      c) Restart the Application Pool associated with your .NET application to
         make the change effective.

   Current implementation will log AuthnRequests and AuthnResponses with the
   "info" log level for successful SAML exchanges and AuthnResponses that are
   not successful with "warning" log level.  Log messages for "error"
   are not currently used since errors are thrown as exceptions (except for
   the Saml2Exception captured with the "warning" log level noted above).

%% 9. How to enable Signing of Requests/Responses

   Since the .NET Fedlet does not require an installer, additional manual steps
   are required to enable signing of outgoing requests and responses to the
   identity provider.

   To configure the .NET Fedlet to sign outgoing requests and responses:

      a) Import your X509 certificate to the Personal folder within the
         Local Computer account using the Certificates Snap-in for the
         Microsoft Management Console.  See the following information on basic
         usage of this snap-in:

         http://msdn.microsoft.com/en-us/library/ms788967.aspx

      b) Specify a friendly name for this certificate by viewing the Properties
         dialog and entering a value. Note this value for step d) below.

      c) Set the appropriate permissions to allow read access to the certificate
         for the user account used by Internet Information Server (IIS) as
         described at the aformentioned article. For example, using the menu in
         the aforementioned snap-in above, navigate to:

            Action > All Tasks > Manage Private Keys

         From here, specify Allow Read permissions for the user acccount running
         IIS (commonly NETWORK SERVICE).

      d) Update the .NET Fedlet's extended metadata (sp-extended.xml) to specify
         the friendly name specified in step b) as the value for the
         signingCertAlias attribute. For example:

            <Attribute name="signingCertAlias">
                <Value>MyFedlet</Value>
            </Attribute>

      e) Update the .NET Fedlet's metadata (sp.xml) to include the key
         descriptor for the signing key. Please follow the links
         below on creating a key store and using the certificate.
         https://forgerock.org/openam/doc/bootstrap/saml2-guide/index.html#import-fedlet-key-pairs-windows

         For the Windows environment, use the Certificates Snap-in for the
         Microsoft Management Console used earlier to now export the public key
         of your certificate in Base64 encoding to be included in the
         KeyDescriptor XML block.  The sp.xml should have a KeyDescriptor as the
         first child element within the SPSSODescriptor and look similar to the
         following:

            <KeyDescriptor use="signing">
              <ds:KeyInfo xmlns:ds="http://www.w3.org/2000/09/xmldsig#">
                <ds:X509Data>
                  <ds:X509Certificate>
MIICQDCCAakCBEeNB0swDQYJKoZIhvcNAQEEBQAwZzELMAkGA1UEBhMCVVMxEzARBgNVBAgTCkNh
bGlmb3JuaWExFDASBgNVBAcTC1NhbnRhIENsYXJhMQwwCgYDVQQKEwNTdW4xEDAOBgNVBAsTB09w
ZW5TU08xDTALBgNVBAMTBHRlc3QwHhcNMDgwMTE1MTkxOTM5WhcNMTgwMTEyMTkxOTM5WjBnMQsw
CQYDVQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTEUMBIGA1UEBxMLU2FudGEgQ2xhcmExDDAK
BgNVBAoTA1N1bjEQMA4GA1UECxMHT3BlblNTTzENMAsGA1UEAxMEdGVzdDCBnzANBgkqhkiG9w0B
AQEFAAOBjQAwgYkCgYEArSQc/U75GB2AtKhbGS5piiLkmJzqEsp64rDxbMJ+xDrye0EN/q1U5Of\+
RkDsaN/igkAvV1cuXEgTL6RlafFPcUX7QxDhZBhsYF9pbwtMzi4A4su9hnxIhURebGEmxKW9qJNY
Js0Vo5+IgjxuEWnjnnVgHTs1+mq5QYTA7E6ZyL8CAwEAATANBgkqhkiG9w0BAQQFAAOBgQB3Pw/U
QzPKTPTYi9upbFXlrAKMwtFf2OW4yvGWWvlcwcNSZJmTJ8ARvVYOMEVNbsT4OFcfu2/PeYoAdiDA
cGy/F2Zuj8XJJpuQRSE6PtQqBuDEHjjmOQJ0rV/r8mO1ZCtHRhpZ5zYRjhRC9eCbjx9VrFax0JDC
/FfwWigmrW0Y0Q==
                  </ds:X509Certificate>
                </ds:X509Data>
              </ds:KeyInfo>
            </KeyDescriptor>

      f) Restart the Application Pool associated with your .NET application to
         make the change effective.

   To test the configuration, use the provided sample application and perform
   the steps as described above.  Afterwards, access "exportmetadata.aspx" to
   generate the .NET Fedlet's metadata and optionally have it signed by passing
   the "sign=true" query string parameter. For example:

      http://sp.example.com/SampleApp/exportmetadata.aspx?sign=true

   The .NET Fedlet is now able to sign requests and responses to the identity
   provider with the appropriate changes to the configured metadata.

      AuthnRequest
      Set AuthnRequestsSigned attribute within the sp.xml metadata file or the
      WantAuthnRequestsSigned within the idp.xml metadata file to true.

      ArtifactResolve
      Set wantArtifactResolveSigned attribute within the idp-extended.xml
      metadata file to true.

      LogoutRequests
      Set wantLogoutRequestSigned attribute within the idp-extended.xml
      metadata file to true.

      LogoutResponse
      Set wantLogoutResponseSigned attribute within the idp-extended.xml
      metadata file to true.

%% 10. Subtle differences between Java and .NET Fedlet

   Beyond the obvious differences of language and deployment, there are subtle
   differences between the Java Fedlet and the .NET Fedlet.  Those differences
   are described below.

      SP Extended Metadata - relayStateUrlList attribute
      In the .NET Fedlet, the values for this optional attribute are expected
      to be written as regular expressions. More information about .NET
      Framework Regular Expressions is available at:
      http://msdn.microsoft.com/en-us/library/hs600312.aspx
