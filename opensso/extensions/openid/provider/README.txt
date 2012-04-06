------------------------------------------------------------------------------

DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  
Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
  
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

$Id: README.txt,v 1.4 2009/09/08 08:31:53 hubertlvg Exp $

------------------------------------------------------------------------------


Introduction
============

This OpenSSO extension implements an OpenID provider which supports both the 
OpenID Authentication 1.1 and OpenID Authentication 2.0 protocols, complete with 
full support for OpenID Simple Registration Extension 1.0 and Attribute Exchange 
Extension 1.0.  

It uses OpenID4Java for OpenID protocols and leverages OpenSSO for Authentication
and retrieving a user's profile attributes. This extension also provides an LDAP
implementation to persist a user's selected attributes per Relying Party to an
LDAP v3 compliant backend, this backend could be the same as the OpenSSO user
data store. A user can have a unique set of values of the attributes per Relying
party. By implementing a required interface, developers can plug in their own
implementation to persist the user's profile to their choice of a backend, like
a database.


Features
========

 * Standalone web application as a deployable WAR file
 * OpenID message object model; supports future consumer implementation
 * Trust management user interface 
 * Simple Registration Extension user interface
 * Attribute Exchange Extension user interface
 * LDAP implementation for persisting user's attribute per Relying Party
 * On-the-fly l10n and i18n (English, French and German included)
 * Integration with OpenSSO through servlet implementation
 

Contents
========
This distribution includes the following the directories and files
-README:			This file
-build.xml			ant build file
-build.properties	instance specific properties

--src
	--java			java source files
	--conf			properties files for OpenSSO and Provider
	--link			sample html to provide OpenID links
	 
--web				xhtml and images and style sheets
	--WEB-INF		deployment descriptors, faces-config 
		--classes	i18n files
		
--lib				list-of-required-external-jars.txt
					[This file lists all the required jar files]

	
	
-


Installing dependencies
=======================
1. Download and extract the Facelets [1.1.14 ] binary archive from
   https://facelets.dev.java.net/. 
   Copy the following files from the extracted archive root and lib/ directory to 
   the provider/lib directory:   
	   	* jsf-facelets.jar
	   	* el-api-1.0.jar
		* el-impl-1.0.jar
		* jsf-api-1.2_04-p02.jar
		* myfaces-all.jar

2. Build the OpenSSO client SDK inside of the OpenSSO amserver product by
   invoking the "clientsdk" target in the associated Ant project. Once built,
   copy the amclientsdk.jar file from the built/dist directory to the
   provider/lib directory.

3. Download the Java Enterprise Edition 5 SDK from
   http://java.sun.com/javaee/downloads/index.jsp. 
   Replace the value of the j2ee.home in build.properties file 
   with your javaee home directory 

   
4. Download and build openid4java from http://code.google.com/p/openid4java/. 
	For this build we have used 0.9.5-SNAPSHOT.566.
	copy build/openid4java.jar to provider/lib
	copy the following jars from /lib/ to provider/lib
		* commons-codec-1.3.jar
		* commons-httpclient-3.0.1.jar
		* commons-logging-1.03.jar
		* nekohtml-1.9.7.jar
		* xercesImpl-2.8.1.jar
    
   
5. Download jdom version 1.1 binary zipped archive  from http://www.jdom.org 
	extract the archive and copy jdom.jar from the /build directory
	to provider/lib




Building
========

The supplied ant build file will compile and build the war file with its default
target. The built war "openid.war" will be located in the build directory.
Before running ant replace the value of the j2ee.home in build.properties file 
with your javaee home directory.

For easier customizations, The built war does not include any of the property files 
located in the src/conf folder.


Configuration
=============

The WAR file does NOT contain the required configuration properties files for both the
provider itself and the OpenSSO client SDK library. Prior to deployment, change
these files located in the src-conf/ directory to suit your configuration:

1. Edit Provider.properties file

2. If you enable attribute exchange persistence, edit the ldap.properties file to
	provide host,port,binding credentials, attribute to persist a user's profile, 
	search base dn etc.
	
	The property ldap.people.return.attribute defines the attribute that will be used to
	store and retrieve the users profile. This attribute should be of the type "DirectoryString" 
	and "Single valued". It does not need to be indexed. You can:
		(a) choose an existing unused attribute from an existing Object Class of a user's entry, 
		(b) add it to an Object Class which is already a part of the user's entry
		(c) add it to a new object class and mofify each users entry to include this 
			object class
	
	This attribute will contain an XML string representing the attributes that the user 
	selects to persist per relying party

	For example if you are creating a new attribute, you can add the following entry
	to the user99.ldif.
    attributeTypes: ( openid-attributes-oid NAME 'openid-attributes'  SYNTAX 1.3.6.1.4.1.1466.115.121.1.15 SINGLE-VALUE X-ORIGIN 'user defined')
 
 	You can then add it to a class like:
 	objectClasses: ( openiduser-oid NAME 'openiduser' SUP inetorgperson STRUCTURAL MAY openid-attributes X-ORIGIN 'user defined' )

	
3. Copy the AMClient.properties file from the OpenSSO client SDK resources
   directory (products/amserver/clientsdk/resources) to the
   provider/resources directory (renaming it  AMConfig.properties). Configure:

   * Location of OpenSSO server (protocol, host, port)
   * Cookie name for OpenSSO session identifiers
   * Security credentials to access OpenSSO services
   * Encryption key to encrypt data to/from OpenSSO services
   * Naming URL in OpenSSO server

4. Create an agent identity in OpenSSO administration console with security
   credentials that match those set in AMConfig.properties file.
   

5. Create a directory and ensure that the application server user has read access to it
	Copy all the modified properties files from src/conf to this directory.
	Add this directory to the application server's classpath.



Deploying the application
=========================

Deploy the openid.war file on your application server. The usual context
path is /openid, though others should work fine (e.g. /provider).

Note: If your application server already has JavaServer Faces installed,
then you must ensure you remove all JSF-related files from your WAR file,
causing only the following JAR files to remain in the lib directory:

 * amclientsdk.jar
 * commons-codec-1.3.jar
 * jsf-facelets.jar


Linking to your identity provider
=================================

The main entry point to your identity provider is provided through the
/service servlet. It dispatches to internal actions and Facelets as required.

So, if you deployed the provider application in /openid on example.com,
then the URL for the OpenID provider service would be:

 * http://example.com/openid/service

This is what will be set in the <link /> tag in OpenID profile pages. or <URL /> tags in XRD page.
For example:

<link rel="openid.server" href="http://example.com/openid/service" />
or
<link rel="openid2.provider" href="http://example.com/openid/service"/>
or
<URI>http://example.com/openid/service</URI>

A sample index.html is provided in the src/link directory which provides HTML links for both
OpenID 1.1 and 2.0 support.



Customization
=============

* The user interface of this application is highly templated, facilitating easy
  customization. 
* Instead of using LDAP for attributes persistence, you can use a relational database or 
  file system to persist user's attributes. To do so, you need to provide a class that 
  implements the com.sun.identity.openid.persistence.AttributePersistor interface.
  Also the property openid.provider.pesistence.class.name in Provider.properties must
  then be changed to the full name of the custom class.
	 




------------------------------------------------------------------------------
