Swekey Authentication Module

Instructions
============

1) Obtain a Swekey from http://store.swekey.com/index.php?sel=buy

2) Build the authnswekey project, either in NetBeans or from command line ant.
authnswekey.jar should be created in authnswekey/dist/

3) Copy SwekeyLoginModule.xml and swekeyLogin.jsp from authnswekey to
<OPENSSO_DEPLOY_DIR>/config/auth/default

4) Copy swekey.js and swekey_integrate.js from the Swekey PHP Integration Kit
(available at http://www.swekey.com/?sel=support&option=downloads under the
'Developer Tools' category) to <OPENSSO_DEPLOY_DIR>/js

5) Copy authnswekey/dist/authnswekey.jar to <OPENSSO_DEPLOY_DIR>/WEB-INF/lib

6) Add com.sun.identity.authentication.modules.swekey.SwekeyLoginModule to the
list of pluggable authentication module classes on the Configuration/
Authentication/Core page. Don't forget to hit 'Save'!

7) Restart OpenSSO

8) We assume that the Swekey ID (you can determine the ID for a Swekey at
http://managment.swekey.com/index.php?sel=manage) has been
provisioned into an attribute in the user's profile. At present, the
Swekey ID attribute name is hardcoded as "employeeNumber". Thus, to test the
authentication module, you should set a user's employeeNumber to the Swekey
ID - you can do this in the OpenSSO console: Access Control/select a realm/
Subjects/select a user/Employee Number.

9) Verify with
http://openssohost:openssoport/opensso/UI/Login?module=SwekeyLoginModule -
enter the username and hit return, or click 'Log In'. If no Swekey is present
then an error message is shown directing the user to insert one and try again.
If all is well then the user is authenticated and you will see their OpenSSO
profile page.

Notes
=====

In this implementation, the user provides their username and the Swekey. This
is to reduce the possibility of compromise by an attacker finding a lost key.
Other implementations might simply ask for the Swekey output alone, perhaps
assuming that a first level authentication has already been done.

Future Directions
=================

Set Swekey random token service URL, Swekey check service URL and Swekey ID
attribute name via the console, rather than in the code.

Allow the admin to select whether or not users will be prompted for their
username as well as the Swekey output.
