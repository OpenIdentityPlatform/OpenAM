Yubikey Authentication Module

Instructions
============

1) Obtain a Yubikey from http://www.yubico.com/products/yubikey/

2) Build project, either in NetBeans or from command line ant; authnyubikey.jar
should be created in authnyubikey/dist/

3) Copy authnyubikey/YubikeyLoginModule.xml to <OPENSSO_DEPLOY_DIR>/config/auth/default

4) Copy authnyubikey/dist/authnyubikey.jar to <OPENSSO_DEPLOY_DIR>/WEB-INF/lib

5) Add com.sun.identity.authentication.modules.yubikey.YubikeyLoginModule to the
list of pluggable authentication module classes on the Configuration/
Authentication/Core page. Don't forget to hit 'Save'!

6) Restart OpenSSO

7) We assume that the Yubikey ID (the first 12 characters of the Yubikey output)
has been provisioned into an attribute in the user's profile. At present, the
Yubikey ID attribute name is hardcoded as "employeeNumber". Thus, to test the
authentication module, you should set a user's employeeNumber to the Yubikey
ID - you can do this in the OpenSSO console: Access Control/select a realm/
Subjects/select a user/Employee Number.

8) Verify with
http://openssohost:openssoport/opensso/UI/Login?module=YubikeyLoginModule -
enter the username then click or tab into the Yubikey field and press the
Yubikey button.

Notes
=====

In this implementation, the user provides their username and the output from the
Yubikey. This is to reduce the possibility of compromise by an attacker finding
a lost key. Other implementations might simply ask for the Yubikey output alone,
perhaps assuming that a first level authentication has already been done.

Future Directions
=================

Set authentication service URL, client ID and Yubikey ID attribute name via the
console, rather than in the code.

Allow the admin to select whether or not users will be prompted for their
username as well as the Yubikey output.

Verify hash from server to allow use of non-SSL authentication service URLs.
