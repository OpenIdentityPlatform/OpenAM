------------------------------------------------------------------------------
README file for OpenSSO Joomla Provider Plugin
------------------------------------------------------------------------------
   DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
  
Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
  
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

$Id: README.txt,v 1.2 2009/08/01 23:39:24 superpat7 Exp $

------------------------------------------------------------------------------

This plugin integrates Joomla 1.5.x with OpenSSO for single sign-on. Users are 
redirected to an OpenSSO server to login, their Joomla username being read from 
their OpenSSO profile.

Please address any issues to users@opensso.dev.java.net. To subscribe to this 
mailing list:

1. Go to https://www.dev.java.net/servlets/Join and register for a java.net 
account.
2. Go to https://opensso.dev.java.net/servlets/ProjectMembershipRequest and 
request 'Observer' role on OpenSSO.
3. Go to https://opensso.dev.java.net/servlets/ProjectMailingListList and 
subscribe to 'users@opensso.dev.java.net'.

Pre-requisites
--------------

You must have the pecl_http extension (http://pecl.php.net/package/pecl_http) 
installed into PHP, since Joomla does not seem to implement its own HTTP 
request function in the same way as Drupal/Wordpress do.

Installation
------------

Install the plugin into Joomla using one of the supported methods. I've had 
most success using the 'Upload Package File' method with the opensso.zip file, 
but even here I had to wrestle a bit with Joomla, making directories 
world-writable until it worked. Your mileage may vary with the exact version of 
Joomla, the underlying operating system and the phases of the moon.

Configuration
-------------

Log in as the Joomla admin, navigate to Extensions/Plugin Manager, enter 
'opensso' in the filter field and hit 'Go'. Click on the 'System - OpenSSO' 
plugin.

OpenSSO module settings:

Enable SSO: this is here so that you get a chance to configure the OpenSSO 
module before you enable it, otherwise you would be locked out of Joomla as 
soon as you installed the OpenSSO module, since you would not have configured 
it yet.

OpenSSO cookie name: only change this from the default, iPlanetDirectoryPro, if 
you have made the corresponding change in OpenSSO. An incorrect value for the 
OpenSSO cookie name will result in a redirect loop between Joomla and OpenSSO.

OpenSSO server URL: this is the base URL for OpenSSO - usually of the form 
http://demo.example.com:8080/opensso/

OpenSSO Joomla username attribute: set this to whichever OpenSSO profile 
attribute name you want to use for the Joomla username. The default, uid, 
assumes that users have the same username in Joomla as OpenSSO. You can set 
this to any attribute name - for example, cn.

Troubleshooting
---------------

It's possible that misconfiguration can result in a redirect loop between 
Joomla and OpenSSO as Joomla looks for one cookie and OpenSSO sets another. To 
fix this, you will need to disable the OpenSSO Plugin in the Joomla plugins 
database table. Log in to mysql as a user with appropriate permissions and do:

mysql> update joomla15.jos_plugins set params='' where element = 'opensso';

This will disable the OpenSSO Plugin and allow you to log in locally as admin 
and rectify the issue.

Future Development Directions
-----------------------------

Automatic new user registration, using attributes from OpenSSO
Redirect loop detection

If you want to work on any of the above, or have any more ideas for enhancing 
this module, email users@opensso.dev.java.net (instructions for subscribing are 
at the top of this file).