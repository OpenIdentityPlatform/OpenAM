------------------------------------------------------------------------------
README file for OpenSSO WordPress Provider Plugin
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

$Id: README.txt,v 1.1 2009/07/27 21:40:37 superpat7 Exp $

------------------------------------------------------------------------------

This module integrates WordPress 2.8.x with OpenSSO for single sign-on. Users 
are redirected to an OpenSSO server to login, their WordPress username being 
read from their OpenSSO profile.

Please address any issues to users@opensso.dev.java.net. To subscribe to this 
mailing list:

1. Go to https://www.dev.java.net/servlets/Join and register for a java.net 
account.
2. Go to https://opensso.dev.java.net/servlets/ProjectMembershipRequest and 
request 'Observer' role on OpenSSO.
3. Go to https://opensso.dev.java.net/servlets/ProjectMailingListList and 
subscribe to 'users@opensso.dev.java.net'.

Installation
------------

Simply copy opensso.php to the WordPress plugins directory - wp-content/plugins

Configuration
-------------

Log in as the WordPress admin and navigate to Settings/OpenSSO Plugin, or 
simply go to 
http://www.my-wordpress-site.com/wp-admin/options-general.php?page=opensso .

OpenSSO Plugin Settings:

Enable SSO: this is here so that you get a chance to configure the OpenSSO 
module before you enable it, otherwise you would be locked out of WordPress as 
soon as you installed the OpenSSO module, since you would not have configured 
it yet.

OpenSSO cookie name: only change this from the default, iPlanetDirectoryPro, if 
you have made the corresponding change in OpenSSO. An incorrect value for the 
OpenSSO cookie name will result in a redirect loop between WordPress and 
OpenSSO.

OpenSSO server URL: this is the base URL for OpenSSO - usually of the form 
http://demo.example.com:8080/opensso/

OpenSSO Drupal username attribute: set this to whichever OpenSSO profile 
attribute name you want to use for the WordPress username. The default, uid, 
assumes that users have the same username in WordPress as OpenSSO. You can set 
this to any attribute name - for example, cn.

Troubleshooting
---------------

Most configuration errors will result in a redirect loop between WordPress and 
OpenSSO as Drupal looks for one cookie and OpenSSO sets another. To fix this, 
you will need to disable the OpenSSO Plugin in the WordPress options database 
table. Log in to mysql as a user with appropriate permissions and do:

mysql> update wordpress.wp_options set option_value = '0' where option_name = 'opensso_enabled';

This will disable the OpenSSO Plugin and allow you to log in locally as admin 
and rectify the issue.

Future Development Directions
-----------------------------

Redirect loop detection

If you want to work on any of the above, or have any more ideas for enhancing 
this module, email users@opensso.dev.java.net (instructions for subscribing are 
at the top of this file).