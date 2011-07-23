------------------------------------------------------------------------------
README file for OpenSSO MediaWiki Provider Plugin
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

$Id: README.txt,v 1.1 2009/08/11 03:55:20 superpat7 Exp $

------------------------------------------------------------------------------

This plugin integrates MediaWiki 1.15.x with OpenSSO for single sign-on. Users 
are redirected to an OpenSSO server to login, their MediaWiki username being 
read from their OpenSSO profile.

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
installed into PHP, since MediaWiki's built in http functions do not allow 
us to set cookies.

Installation
------------

Copying the opensso directory to the MediaWiki extensions directory, and append 
the contents of 'append_to_LocalSettings.php' to MediaWiki's LocalSettings.php.

Configuration
-------------

Edit what you just added to LocalSettings.php:

setOpenssoEnabled: allows you to easily enable/disable the extension - useful 
for troubleshooting

setOpenssoCookieName: only change this from the default, iPlanetDirectoryPro, 
if you have made the corresponding change in OpenSSO. An incorrect value for 
the OpenSSO cookie name will result in a redirect loop between MediaWiki and 
OpenSSO.

setOpenssoBaseUrl: this is the base URL for OpenSSO - usually of the form 
http://demo.example.com:8080/opensso/

setOpenssoMediaWikiUsernameAttribute: set this to whichever OpenSSO profile 
attribute name you want to use for the MediaWiki username. The default, uid, 
assumes that users have the same username in MediaWiki as OpenSSO. You can set 
this to any attribute name - for example, cn.

Troubleshooting
---------------

It's possible that misconfiguration can result in a redirect loop between 
MediaWiki and OpenSSO as MediaWiki looks for one cookie and OpenSSO sets 
another. To fix this, simply edit the call to setOpenssoCookieName in 
LocalSettings.php.

Future Development Directions
-----------------------------

Automatic new user registration, using attributes from OpenSSO
Redirect loop detection

If you want to work on any of the above, or have any more ideas for enhancing 
this module, email users@opensso.dev.java.net (instructions for subscribing are 
at the top of this file).