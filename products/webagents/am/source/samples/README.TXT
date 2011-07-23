README.TXT

--------------------------------------------------------------------
Sun(TM) Java(tm) System Access Manager Policy Agents 2.2
--------------------------------------------------------------------

DISCLAIMER
-----------
   THIS SAMPLE CODE IS PROVIDED AS-IS FOR REFERENCE PURPOSES ONLY. 
   SUN MICROSYSTEMS, INC. IS NOT LIABLE FOR ANY DAMAGE RESULTING 
   FROM THE USE OF THIS CODE OR ANY PORTION THEREOF.


SAMPLE CODE
-----------

   * What is provided.
   * Building Sample Program on Solaris
   * Building Sample Program on Windows
   * Executing the Sample Program
     * Executing the Sample Program on Solaris
     * Executing the Sample Program on Windows
   * Related References

What is provided
----------------
   The sample distribution contains source files:
          
          1. am_policy_test.c
          2. apache_agent.c
	  3. am_auth_test.c
          4. am_sso_test.c
	  5. am_log_test.c
	  6. am_web_agent_test.c

   The file am_policy_test.c demonstrates the basic usage of Policy SDK APIs 
   that can be used to evaluate policy for specified resources.
   
   The file am_auth_test.c demonstrates the basic usage of Authentication SDK 
   APIs that can be used to login to an Access Manager.

   The file am_sso_test.c demonstrates the basic usage of SSO SDK APIs 
   that can be used to perform session operations.

   The file am_log_test.c demonstrates the basic usage of Logging SDK APIs 
   that can be used to log a message to the Access Manager logs.

   The file apache_agent.c demonstrates how the Policy SDK APIs can be used
   to build a Web Agent for the Apache Web Server. Please note that this
   is a sample Web Agent and is not intended to serve as a real Web 
   Agent. 

   When you build the sample code, only apache_agent.c is not compiled. The
   apache_agent.c is provided for reference purposes only.


Building Sample Program on UNIX platforms
------------------------------------------

   On Solaris platform, the sample program can be built using gmake. This
   can be done by the following steps:

   1. Make sure you have gmake or any other compliant make program 
      available. We recommend the use of GNU's gmake program version
      3.76 or higher.

   2. Make sure you have gcc or any other compliant C compiler program
      available.

   3. From the samples directory, simply invoke the make program. For
      example, 

	  $ gmake

   4. This produces executables of the samples am_*_test in the same directory.

Note on Red Hat Linux Advanced Server release 2.1AS/i686 platform:

   On Linux platform, LD_LIBRARY_PATH must be set to have
   /usr/ucblib:/SDK install directory>/is_csdk/lib:/usr/lib/mps
   tcsh:
	setenv LD_LIBRARY_PATH ${LD_LIBRARY_PATH}:/usr/ucblib:/SDK install directory>/is_csdk/lib:/usr/lib/mps
   bash:
	export LD_LIBRARY_PATH=${LD_LIBRARY_PATH}:/usr/ucblib:/SDK install directory>/is_csdk/lib:/usr/lib/mps

   On the Red Hat Linux AS 2.1/i686 platform, the following versions 
   (or later) of gcc and glibc must be used due to a bug in the default 
   gcc and glibc that comes with RedHat Linux AS 2.1: 

   gcc-2.96-124.7.2
   glibc-2.2.4-32.11
 
   The rpms are available at: 

   ftp://rpmfind.net/linux/redhat/updates/enterprise/2.1AS/en/os/SRPMS/gcc-2.96-124.7.2.src.rpm
   ftp://rpmfind.net/linux/redhat/updates/enterprise/2.1AS/en/os/SRPMS/glibc-2.2.4-32.11.src.rpm


Building Sample Program on Windows
----------------------------------

   On Windows platform, the sample programs can be built using 
   Microsoft Visual Studio 6.0.  Be sure to do the following: 
       - Define WINNT in the compile flags.
       - Add ../include as an additional include path. 
       - Add ../lib as an additional lib path. 
       - Link with all libraries in the ../lib directory. 

   If gmake and MKS toolkit are installed on the system, the default 
   Makefile can be used to make all samples by simply typing a 
   gmake on the command. For example: 
  
       C:\samples>gmake

Executing the Sample Program
----------------------------

   The sample programs operate in a command line mode and demonstrate
   the use of C API for policy, auth and sso . Usage of sample programs
   are as follows:

am_policy_test

   Note: You must have a policy defined for the specified resource in 
   an Access Manager before running this program. Also the shared secret 
   (value of com.sun.am.policy.am.password ) should be in clear text and 
   not in encrypted form.

   Description: am_policy_test evaluates the policy for the given ssoToken, 
   resource name, action.

   Usage: 
      am_policy_test <init property file> <ssoToken> <resource name> <action>

   <init property file> 
       is the path to the AMAgent.properties file. For example:
       ../config/AMAgent.properties

   <ssoToken> 
       is a valid SSO Token issued by Access Manager. You can get this
       value out of your browser after logging into Access Manager. Please
       refer to the documentation for your browser on how to find out the
       cookie values. Once you have that information, you can use the value
       of the cookie iPlanetDirectoryPro as the value for this argument.

       Note: In case the browser you are using does not provide URL decoded
       cookie values, you may have to decode the value yourself before using
       it in this sample program. Alternatively, for test purposes, you can
       also use the SSO value displayed in the debug logs of the Access
       Manager.

   <resource name> 
       is the name of a resource that you wish to evaluate the policy for. 
       For example: http://myserver.mydomain.com:80/myresource.html

   <action>
       is the action name. For example GET or POST.


am_auth_test

   Note: You must have an Access Manager with a user profile set up with the 
   corresponding authentication module before running this program.

   Description: am_auth_test authenticates to the specified org (if any) using 
   the specified authentication module.

   Usage: am_auth_test [-u user] [-p password] [-f properties_file] [-r url]
                       [-n cert_nick_name] [-o org_name] [-m auth_module]

   With no options supplied on the command line, login defaults to use the
   org_name specified in the properties file and auth_module LDAP. The
   user may specify the org_name (for example: dc=iplanet,dc=com) on the
   command line to override the value specified in the properties file. The
   user is prompted for "User Name: " and "Password: " in either case.

   For Cert based login, the user specifies auth_module Cert on the command
   line. The user may optionally specify the cert_nick_name on the command
   line to override the value specified in the properties files. Other values
   affecting certificate based login are taken from the properties file.

   The default properties file is ../../config/AMAgent.properties. Check to
   make sure the appropriate properties and values are set in the properties
   file before calling this program. The following properties are specific
   to authentication:
      com.sun.am.auth.orgName
      com.sun.am.auth.certificateAlias

am_sso_test

   Note: You must have an Access Manager with a user profile set up with the 
   LDAP authentication module before running this program.

   Description: am_sso_test logs into an Access Manager using the given user 
   and password using the LDAP authentication module, and performs sso token 
   operations on the session. Also please note that the internal ldap password
   should not be encrypted in AMAgent.properties.
   

   Usage: 
      am_sso_test -u user -p password [-o org_name] [-f properties_file] [-s sso_token_id]

   Where:

   <user> 
   <password> 
       user and password to login to Access Manager using the LDAP 
       authentication module.

   <properties_file> 
       the path to the properties file. If not set, the default properties 
       file ../../config/AMAgent.properties is used. Check to make sure 
       appropriate values are set in the properties file before calling 
       this program. See the Agents documentation for more information on the 
       properties file.

   <org_name> 
       Organization Name

   <sso_token_id> 
       User SSO token id. 

   Either "-u user -p password" or "-s sso_token_id" should be used.

am_log_test

   Description: am_log_test logs a message to the given log file on the 
   Access Manager using the given sso token.  

   Usage: 
      am_log_test -n log_name -u logged_by_token_id -u user_token_id -m message 
		    [-d log_module] [-f properties_file]

   <log_name> 
      Name of Log file on Access Manager.

   <logged_by_token_id> 
      SSO token ID with access the Logging Service on Access Manager.

   <user_token_id> 
      SSO token ID of a user for the log. Can be the logged_by_token_id 
      or something else. 

   <message> 
      The log message.

   <log_module> 
      The module name, if not specified, the default "TestModule" is used.

   <properties_file>
      path to the properties file. If not set, the default properties 
      file ../../config/AMAgent.properties is used. Check to make sure 
      appropriate values are set in the properties file before calling 
      this program. See the Agents documentation for more information on the 
      properties file.
     
apache_agent.c

   Description: sample web agent plugin for the apache HTTP server. 
   This is a sample and should not be used as an actual web agent.


Executing the Sample Program on Solaris
---------------------------------------
   On Solaris platform you can run the sample programs by launching
   the generated executables on the command line. 

   Note: Set the LD_LIBRARY_PATH environment variable to include the
   lib directories, /usr/lib/mps:/opt/SUNWam/lib:/usr/lib:/usr/ucblib
   which contain libamsdk.so, libxml2.so, libssl3.so, libnss3.so,
   libplc4.so, libplds4.so, libnspr4.so, and libucb.so. The directory
   /usr/lib is included before /usr/ucblib so that common programs,
   like editors, will continue to function.

Executing the Sample Program on Linux
---------------------------------------
   On Linux you can run the sample programs by launching
   the generated executables on the command line. 

   Note: Set the LD_LIBRARY_PATH environment variable to include the
   directory, <install-location>/agent/lib, which contains 
   libamsdk.so, libxml2.so, libssl3.so, libnss3.so, libplc4.so, 
   libplds4.so and libnspr4.so. 

Executing the Sample Program on Windows
---------------------------------------
   On Windows platform you can run the sample programs by launching
   the generated executables on the command line
   
   Note: You must have the <sample-directory>/lib directory 
   in your path before launching the sample programs. 
   Alternatively, you can use the run.bat script to launch the sample programs 
   which will set your path appropriately.


Related References
------------------
   * RFC 1738: Uniform Resource Locators (URL). 
     Available at http://www.ietf.org/rfc/rfc1738.txt
   * GNU Project and Free Software Foundation (FSF) Website:
     http://www.gnu.org/


