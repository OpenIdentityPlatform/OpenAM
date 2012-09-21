
This enables SSO for acegi-security 1.3.x apps using OpenSSO.

Install:
- Copy the OpenSSO client sdk to WEB-INF/lib/
- Create/copy an AMConfig.properties to WEB-INF/classes/ (or somewhere in the classpath)
- Copy this jar to WEB-INF/lib/
- See applicationContext-acegi-security.xml for an example configuration

Build:
Use maven 2.x
You will need to grab opensso.zip and install the client sdk manually
until one appears in some public repository.


