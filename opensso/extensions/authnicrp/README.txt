-----------------------------------------------------------------------------
OpenSSO Authentication Module for Information Cards version 0.9
By Patrick Petit (Patrick.Michel.Petit@Gmail.com)
-----------------------------------------------------------------------------

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

$Id: README.txt,v 1.17 2009/12/03 16:13:05 ppetitsm Exp $

Copyright 2008 Sun Microsystems Inc. All Rights Reserved
Portions Copyrighted 2008 Patrick Petit Consulting

---------------------------------------------------------

Standard Disclaimer
-------------------

The author,  does not provide any warranty of the Information Card authentication
module Authnicrp (the item) whatsoever, whether express, implied, or statutory, including,
but not limited to, any warranty of merchantability or fitness for a particular
purpose or any warranty that the contents of the item will be error-free.

Introduction
------------

The Information Card authentication module (Authnicrp) provides the ability for
a Relying Party (RP) to accept Information Cards as a means of authentication and
authorization to access secured Web resources. It has been successfully tested against
OpenSSO 8 build 1, running on Glassfish application server v2.1, and using self-issued
cards and managed cards issued from DigitalMe and Azigo Identity Selectors.

As in the previous version, Authnicrp relies on the OpenInfocard project to handle
the Security Token's decrypt operations.

Authnicrp-0.9 presents some important improvements compared to the initial version
of Authnicrp that was committed about year ago for proof-of-concept purposes.
In particular, the need of a companion JavaDB database has been removed.
Information Card support is now part of the user profile that is stored
in the Identity Repository (IdRepo) that can be either Active Directory,
OpenDJ, Sun Directory Server Enterprise Edition or MySQL through the JDBC
IdRepo plug-in.

A RP can now specify its Security Token requirements and security
policies dynamically though the service's configuration that is integrated
in the OpenSSO administration console. As such, different security policies for
secured Web resources can be expressed at the domain and sub-domain (realms) levels
in one instance of OpenSSO server. More fine-grained control levels are possible.

Thanks to the OpenSSO Agent 3.0, Information Card support can be added to any
Web application without programming. Information Card claims can be passed by
the agent to the application as HTTP request parameters, HTTP header attributes
or cookies. It also possible to obtain Information Card claims from the SSO
session attributes.

Features
--------

The Information Card authentication module (Authnicrp) supports three operating
modes:

1 - Ignored
2 - Required
3 - Dynamic

In Ignored mode, Authnicrp authenticates the Information Card bearer at the
condition the Security Token returned by the Identity Selector complies with
the RP's security policy requirements. For instance, security policy
requirements may mandate required claims and that those required claims
be verified. Once the user is authenticated, a session is granted on behalf of the
user, which is assigned a configurable anonymous user ID, which can be
be assigned a set of security policies to be used when evaluating the anonymous
user access rights to protected resources.

The Required mode, requires that the user has a user account in the Identity
Repository to authenticate the Information Card bearer successfully. The user is
asked to provide a user ID and password the first time the Information Card is
presented. Upon successful authentication of the user's credentials, the Information
Card is registered in the user's profile. Subsequent authentications will use the
Information Card instead of the user Id and password until the user changes his
credentials or remove the Information Card from his profile.

The Dynamic mode is used for self-provisioning of new user accounts using the
Information Card claims. The Authnicrp's configuration console provides a flexible
mechanism by which the administrator can define a claim to attribute mapping scheme,
by which a user profile attribute is created based on claim to IdRepo attributes
mapping. It is possible to define default roles that are assigned to he user
when the account is created.

Roadmap
-------

1 - The next release of Authnicrp (version 1.0) targets to fully support the Identity
Metasystem Interoperability Specification Version 1.0, OASIS Committee Draft 01.
2 - Support of SAML 2.0 security tokens


Build Instructions
------------------

You need to compile with JDK 1.6, and setup JAVA_HOME environment variable
accordingly. All external libraries dependencies are provided for convenience
in the 'extlib' directory.

Modify the 'opensso-root.dir' property in build.xml to make it point to OpenSSO's
deployment directory.
For example: /opt/glassfish/domains/domain1/applications/j2ee-modules/opensso

Open the project in Netbeans 6.5.1 or latter, and click 'Clean & Build", or simply
invoke the default build target.

# ant

To install the module in opensso you need to follow the instructions outlined 
below:

1 - Install OpenSSO (Download from 
    https://opensso.dev.java.net/public/use/index.html)

2 - Build the module either under NetBeams or simply using ant. The '-post-jar'
    target copies all the required files in the deployed OpenSSO instance.
    Prior to invoking the '-post-jar' target, you need to modify the
    opensso-root.dir property in build.xml in order to define the location of the
    opensso instance. For example:
    /usr/local/glassfish/domains/domain1/applications/j2ee-modules/opensso

    If you want to build Authnicrp for a different release than OpenSSO Express 8,
    you will need to replace the amserver.jar file and opensso-sharedlib.jar file
    under extlib/OpenSSO by the librairies of your actual OpenSSO version.

3-  Install the authentication module in the OpenSSO instance.
    Invoke the following commands in sequence:

    $ ssoadm create-svc -X amAuthInfocard.xml -u amadmin -f <password>

    $ ssoadm register-auth-module -a com.identarian.infocard.opensso.rp.Infocard \
      -u amadmin -f <password>
    
    Note: The file <password> (which mode must be read-only for the user
    (i.e. -r--------) constains the amAdmin password

    Then, you will need to modify the iPlanetAMUserService service in order to enable
    the ic-ppid attribute whithin the console. Check the amUser.xml file that is
    included in the module for differences with the standard version of the
    service.

    $ ssoadm delete-svc -u amadmin -f password -s iPlanetAMUserService

    And add it back with:

    $ ssoadm create-svc -u amadmin -f password -X amUser.xml

4 - Modify the LDAP schema to incorporate the authnicrp.ldif attributes and
    objectclass. There are multiple ways of doing it.
    Personaly, I use Apache Directory Studio.
    Another way is to modify the 99-user.ldif file manually to insert the
    authnicrp's schema extension under <OPENSSO_INSTALL_DIR>/opends/config/schema.

5 - Add the Information Card object class and attributes to the realm's data
    store.
    Add 'infocard' to the list of LDAP User Object Classes and click save
    Add 'ic-ppid' and 'ic-data' to the list of LDAP User Attributes and click
    save

6 - Restart the application server

7 - In the Authnicrp configuration pane, define the application server's key
    store password and alias which are respectively defined in the
    'iplanet-am-auth-infocard-keyStorePassword'
    property and 'iplanet-am-auth-infocard-keyAlias' property. These properties
    are defined in the Configuration>Information Cards pane of the admin console.
    On Glassfish V2.x, the default alias is 's1as' and the default password is
    the application's server admin password (i.e. 'adminadmin').
    If you have installed Glassfish via NetBeans 6.0, then the key store
    password is 'changeit'.
    On Glassfish you can verify the password and alias with keytool -list \
    -storepass changeit -keystore \
    <GLASSFISH_DIR>/domains/domain1/config/keystore.jks

8 - To access the module, point your browser at
    https://my.domain.name:port/opensso/UI/Login?module=Infocard

9 - In order to play with the module, you'll have to install an Information
    Card Identity Selector application. The initial login page provides links 
    for where you can download an Identity Selector as a Firefox extension for 
    Linux, Windows and Mac OS X.
    Windows CardSpace is included with Windows Vista. On Windows XP SP2 and 
    above and Windows Server 2003 SP and above you must install Internet 
    Explorer 7 and .NET Framework 3.0 (both available via Windows Update). At the
    time you deploy this module, Geneva CardSpace download may be available as well.
    Most common Information Card extensions for Firefox are DigitalMe, xmldap.org
    and Azigo. More informatin can be found on the Informaion Card Foundation from
    http://informationcard.net/user-information-center

Some reference articles:
------------------------

Creating Custom User Attributes in OpenSSO

http://wikis.sun.com/display/OpenSSO/Creating+Custom+User+Attributes+in+OpenSSO

About the sms.dtd Structure

* http://developers.sun.com/identity/reference/techart/authentication.html
* http://docs.sun.com/source/816-6774-10/prog_service.html#wp19647

For further explanations please email dev@opensso.dev.java.net with Information
Card in the subject.