------------------------------------------------------------------------------
README file for OpenSSO Drupal Provider Module
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

$Id: README.txt,v 1.1 2009/07/25 22:51:28 superpat7 Exp $

------------------------------------------------------------------------------

This module integrates Drupal 6.x with OpenSSO for single sign-on. Users are 
redirected to an OpenSSO server to login, their Drupal username being read from 
their OpenSSO profile.

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

Simply copy the opensso directory to a Drupal modules directory. For example, 
to enable OpenSSO for all sites, copy the opensso directory to 
sites/all/modules.

Configuration
-------------

Log in as the Drupal admin and navigate to Administer/Site 
Configuration/OpenSSO module settings, or simply go to 
http://www.mydrupalsite.com/?q=admin/settings/opensso .

OpenSSO module settings:

Enable SSO: this is here so that you get a chance to configure the OpenSSO 
module before you enable it, otherwise you would be locked out of Drupal as 
soon as you installed the OpenSSO module, since you would not have configured 
it yet.

OpenSSO cookie name: only change this from the default, iPlanetDirectoryPro, if 
you have made the corresponding change in OpenSSO. An incorrect value for the 
OpenSSO cookie name will result in a redirect loop between Drupal and OpenSSO.

OpenSSO server URL: this is the base URL for OpenSSO - usually of the form 
http://demo.example.com:8080/opensso/

OpenSSO Drupal username attribute: set this to whichever OpenSSO profile 
attribute name you want to use for the Drupal username. The default, uid, 
assumes that users have the same username in Drupal as OpenSSO. You can set 
this to any attribute name - for example, cn.

Troubleshooting
---------------

Most configuration errors will result in an error page with the 'local' Drupal 
login fields - from here, you can log in as the Drupal admin and fix things or 
just disable the OpenSSO module.

The OpenSSO module also logs errors using Drupal's watchdog function, so you 
will be able to log in as the Drupal admin, go to Administer/Reports/Recent log 
entries and get a clue as to the problem.

One exception to the above is an incorrect value for the OpenSSO cookie name, 
which will cause a redirect loop between Drupal and OpenSSO as Drupal looks for 
one cookie and OpenSSO sets another. To fix this, simply remove the OpenSSO 
module directory and you will be able to login to Drupal as before. Once you 
have logged in as the Drupal admin, you can copy the OpenSSO module back and 
fix its configuration.

Future Development Directions
-----------------------------

Automatic new user registration, using attributes from OpenSSO
Redirect loop detection

If you want to work on any of the above, or have any more ideas for enhancing 
this module, email users@opensso.dev.java.net (instructions for subscribing are 
at the top of this file).