README.TXT

--------------------------------------------------------------------
OpenSSO Policy Agent 3.0
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
   APIs that can be used to login to an OpenSSO server.

   The file am_sso_test.c demonstrates the basic usage of SSO SDK APIs 
   that can be used to perform session operations.

   The file am_log_test.c demonstrates the basic usage of Logging SDK APIs 
   that can be used to log a message to the OpenSSO server logs.

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
      available on Linux, Sun Studio 11 C/C++ compiler on Solaris, 
      IBM C/C++ compiler on AIX and HP C/C++ compiler aCC on HP-UX.

   3. Under install directory>/is_csdk/lib, please do the following

       ln -s libamsdk.so.3 libamsdk.so
       ln -s libxml2.so.2 libxml2.so (if libxml2.so.2 is present)
	
      Note: For HP-UX platform, please do the following

       ln -s libxml2.so libxml2.so.9

   4. Include directory>/is_csdk/lib in LD_LIBRARY_PATH

   5. From the samples directory, simply invoke the make program. For
      example, 

	  $ gmake
   6. If compiling on a 64-bit machine, please execute as following:

	  $ gmake BUILD_VERSION=64

   6. This produces executables of the samples am_*_test in the same directory.

   On Solaris Sparc/x86 (32-bit machines) LD_LIBRARY_PATH must be set to have
   /usr/lib:/usr/lib/mps:<C-SDK install directory>/is_csdk/lib:/usr/dt/lib:/usr/ucblib

   On Solaris Sparc/x86 (64-bit machines) LD_LIBRARY_PATH must be set to have
   /usr/lib/64:/<C-SDK install directory>/is_csdk/lib:/usr/lib/mps/64:/usr/dt/lib/sparcv9:/usr/ucblib/sparcv9:/usr/lib/mps/sparcv9

   On Linux (32-bit/64-bit) machines LD_LIBRARY_PATH must be set to have
   <C-SDK install directory>/lib

   7. Rename ../config/OpenSSOAgentBootstrap.template.orig as ../config/OpenSSOAgentBootstrap.properties and update the tags appropriately.
   8. Rename ../config/OpenSSOAgentConfiguration.template.orig as ../config/OpenSSOAgentConfiguration.properties and update the tags appropriately.

   Note: Only 32-bit machines are supported for HP-UX and AIX platforms.

Building Sample Program on Windows
----------------------------------

   On Windows platform, the sample programs can be built using 
   Microsoft Visual Studio 7.0.  Be sure to do the following: 
       - Define WINNT in the compile flags.
       - On 32-bit Windows machine, run vcvars32.bat which will 
         set the all the required path,include libraries required to compile the samples.
         For setting these variables in 64-bit Windows, it is explained below.
       - Add ../bin as an additional PATH environment variable. 
       - Add ../include as an additional INCLUDE environment variable. 
       - Add ../bin as an additional LIB environment variable. 
       - Link with all libraries in the ../lib directory. 

   If gmake and MKS toolkit are installed on the system, the default 
   Makefile can be used to make all samples by simply typing a 
   gmake on the command. For example: 
  
       C:\samples>gmake

   On a 64-bit windows machine, install Visual Studio 2008 and cygwin 
   to build the samples. Then set the PATH,INCLUDE and LIB environment variables 
   with that of the Visual Studio compiler tools. Please note that while using 
   cygwin to build the samples, PATH, INCLUDE and LIB environment variables have to be set manually. 
   You can start a Visual Studio 2008 x64 Win64 command prompt and run echo %PATH%, 
   echo %LIB% and echo %INCLUDE% to get the values. You can then set them either by 
   using export in cygwin shell or through the system properties of Windows. 
   you can then run gmake in the cygwin shell and build the samples. For example:
   
       $ cd /cygdrive/<path to samples folder>/ 
       $ gmake
   
   While executing the sample programs, execute from a Windows command prompt and not from
   the cygwin shell.

Executing the Sample Program
----------------------------

   From version 3.0, the c-sdk requires a configurable key to encrypt the 
   agent profile password. The key has to be a minimum 7-letter(characters between 
   a-z, numbers 0-9) word. Please specify this key for the property below in the
   OpenSSOAgentBootstrap.properties file:
       com.sun.identity.agents.config.key

   The encryption utility, crypt_util(in Unix) and cryptit.exe(in Windows) can 
   generate the encrypted password using this key. 
   It can be found in the bin folder of C-SDK bundle. The usage is :
     a)in Unix :
          ./cryptutil <password> <key>  
     b)in Windows :
          cryptit.exe <password> <key>
   Please specify this password for the property below in the
   OpenSSOAgentBootstrap.properties file:
       com.sun.identity.agents.config.password

   The sample programs operate in a command line mode and demonstrate
   the use of C API for policy, auth and sso . Usage of sample programs
   are as follows:

am_policy_test

   Description: am_policy_test evaluates the policy for the given ssoToken, 
   resource name, action.

   Usage: 
      am_policy_test <bootstrap_property file> <config_properties_file> <ssoToken> <resource name> <action>

   <bootstrap_property_file> 
       is the path to the OpenSSOAgentBootstrap.properties bootstrap file. 
       For example:
       ../config/OpenSSOAgentBootstrap.properties

   <config_property_file> 
       is the path to the OpenSSOAgentConfiguration.properties file. Even if the
       repository type is "centralized", the user still needs to provide
       a value for this property (just a file name is enough). If the 
       repository type is "local", user needs to provide a location of the
       OpenSSOAgentConfiguration.properties.
       For example: ../config/OpenSSOAgentConfiguration.properties

   <ssoToken> 
       is a valid SSO Token issued by OpenSSO server. You can get this
       value out of your browser after logging into OpenSSO server. Please
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

   Note: You must have an OpenSSO server with a user profile set up with the 
   corresponding authentication module before running this program.

   Description: am_auth_test authenticates to the specified org (if any) using 
   the specified authentication module.

   Usage: am_auth_test [-u user] [-p password] [-f bootstrap_properties_file] 
                       [-r url] [-n cert_nick_name] [-o org_name] 
		       [-m auth_module]

   With no options supplied on the command line, login defaults to use the
   org_name specified in the properties file and auth_module LDAP. The
   user may specify the org_name (for example: dc=iplanet,dc=com) on the
   command line to override the value specified in the properties file. The
   user is prompted for "User Name: " and "Password: " in either case.

   For Cert based login, the user specifies auth_module Cert on the command
   line. The user may optionally specify the cert_nick_name on the command
   line to override the value specified in the properties files. Other values
   affecting certificate based login are taken from the properties file.

   The default properties file is ../../config/OpenSSOAgentBootstrap.properties. 
   Check to make sure the appropriate properties and values are set in the 
   properties file before calling this program. The following properties are 
   specific to authentication:
      com.sun.am.auth.orgName
      com.sun.am.auth.certificateAlias

am_sso_test

   Note: You must have an OpenSSO server with a user profile set up with the 
   LDAP authentication module before running this program.

   Description: am_sso_test logs into an OpenSSO server using the given user 
   and password using the LDAP authentication module, and performs sso token 
   operations on the session. Also please note that the internal ldap password
   should not be encrypted in OpenSSOAgentBootstrap.properties.
   

   Usage: 
      am_sso_test -u user -p password [-o org_name] [-f bootstrap_properties_file] [-c config_propeties_file] [-s sso_token_id] [-m auth_module]

   Where:

   <user> 
   <password> 
       user and password to login to OpenSSO server using the LDAP 
       authentication module.

   <bootstrap_properties_file> 
       the path to the bootstrap_properties file. If not set, the default 
       properties file ../../config/OpenSSOAgentBootstrap.properties is used. 
       Check to make sure appropriate values are set in the properties file 
       before calling this program. See the Agents documentation for more 
       information on the properties file.

   <config_properties_file> 
       the path to the config_properties file. If not set, the default 
       properties file ../../config/OpenSSOAgentConfiguration.properties is used. 
       Check to make sure appropriate values are set in the properties file 
       before calling this program. See the Agents documentation for more 
       information on the properties file.

   <org_name> 
       Organization Name

   <sso_token_id> 
       User SSO token id. 

   <auth_module> 
       Auth module to use

   Either "-u user -p password" or "-s sso_token_id" should be used.

am_log_test

   Description: am_log_test logs a message to the given log file on the 
   OpenSSO server using the given sso token.  

   Usage: 
       ./am_log_test -u <user_token_id> -n <log_name> -l <logged_by_token_id> -m <log_message> [-d <log_module>] [-f <bootstrap_properties_file>]

   <user_token_id> 
      SSO token ID of a user for the log. Can be the logged_by_token_id 
      or something else. 

   <log_name> 
      Name of Log file on OpenSSO server.

   <logged_by_token_id> 
      SSO token ID with access the Logging Service on OpenSSO server.

   <log_message> 
      The log message.

   <log_module> 
      The module name, if not specified, the default "TestModule" is used.

   <bootstrap_properties_file>
      path to the bootstrap properties file. If not set, the default properties 
      file ../../config/OpenSSOAgentBootstrap.properties is used. Check to make 
      sure appropriate values are set in the properties file before calling 
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
   lib directories, /usr/lib/mps:/usr/lib:/usr/ucblib
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

Executing the Sample Program on AIX
---------------------------------------
   On AIX you can run the sample programs by launching
   the generated executables on the command line. 

   Note: Set the LIBPATH environment variable to include the
   directory, <install-location>/agent/lib, which contains 
   libamsdk.so, libxml2.so, libssl3.so, libnss3.so, libplc4.so, 
   libplds4.so and libnspr4.so. 

Executing the Sample Program on HP-UX
---------------------------------------
   On HP-UX you can run the sample programs by launching
   the generated executables on the command line. 

   Note: Set the SHLIB_PATH environment variable to include the
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


