#
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
# Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
#
# The contents of this file are subject to the terms
# of the Common Development and Distribution License
# (the License). You may not use this file except in
# compliance with the License.
#
# You can obtain a copy of the License at
# https://opensso.dev.java.net/public/CDDLv1.0.html or
# opensso/legal/CDDLv1.0.txt
# See the License for the specific language governing
# permission and limitations under the License.
#
# When distributing Covered Code, include this CDDL
# Header Notice in each file and include the License file
# at opensso/legal/CDDLv1.0.txt.
# If applicable, add the following below the CDDL Header,
# with the fields enclosed by brackets [] replaced by
# your own identifying information:
# "Portions Copyrighted [year] [name of copyright owner]"
#
# $Id: amtune-env.pl,v 1.4 2008/08/19 19:08:35 veiming Exp $
#
#
################################################################################
use Win32::Registry;
use File::Basename;

require "amtune-utils.pl";

################################################################################
# Keys to performance tuning parameter documentation:    
#           IS      - OpenSSO (formerly Identity Server)
#           AM      - OpenSSO
#           DS      - Directory Server
#           WS      - Web Server
#           AS      - Application Server
#           amtune  - OpenSSO tuning scripts located in amtune directory
#           #x      - Number of x (eg. #Sessions - Number of Sessions) 
#           KB      - Kilo Bytes
#           MB      - Mega Bytes
#           GB      - Giga Bytes
#           MTS     - Minutes
#           SM      - OpenSSO Service Management Module
#           NYI     - Not yet incorporated
#
# You will need to modify/verify the parameters mentioned below for perftune/* 
# scripts to work as designed.
#
# All parameters prefixed by AMTUNE are used by the amtune scripts
# 
# The following entries can be modified to suit your deployment.
# Once you have edited this file, you may run any of the amtune scripts.
# Usage:
#       <amtune-script> <admin_password> <dirmanager_password> [<as8admin_password>|<ws7admin_password>] 
#
#       When running individual amtune script, you only need to supply no password or one password depending
#       on which password needed to run the script.  For example, amtune-identity <admin_password>
#
# Note that amtune currently is non-interactive.
#
# Also, there is a list of "DO NOT MODIFY" parameters towards the end of this file
# that the amtune scripts rely on. This section is maintained by OpenSSO engineers
# and modifications to these parameters are unwarranted and will not be supported.
#
################################################################################
############### Start : Performance Related User Inputs #######################
################################################################################
#-------------------------------------------------------------------------------
# Parameter     :   AMTUNE_MODE
# Values        :   REVIEW, CHANGE
# Default       :   REVIEW
#
# Description   :   Based on this setting, the amtune scripts will behave differently
#
#                   1. REVIEW - Suggest Tuning mode (default)
#                       - In this mode, amtune will suggest tuning recommendations but will not 
#                       make any changes to the deployment
#
#                       This mode will honor AMTUNE_DEBUG_FILE_PREFIX parameter.
#                       List of tuning recommendations along with the current values will be 
#                       noted in the debug file and console.
#
#                   2. CHANGE - Change Mode 
#                       - In this mode, amtune scripts will perform
#                       all changes deemed necessary. 
#                       (Except for Directory Tuning. Read Note below)
#
#                       This mode will honor AMTUNE_DEBUG_FILE_PREFIX parameter.
#                       List of changes along with the original values will be noted 
#                       in the debug file and console.
#
# Note          :   1. On CHANGE mode
#                   Please use extreme caution while using CHANGE mode.
#                   In CHANGE mode, amtune might need to restart the Web Container, OpenSSO
#                   and might recommend a system restart.
#   
#                   2. On Directory Server tuning   
#                   Tuning Directory Server requires extra levels of confirmation.
#                   Assumption is that OpenSSO will use an existing Directory Server in
#                   non-exclusive mode (other applications might use Directory Server)
#                   
#                   Irrespective of where the Directory Server is installed (local or remote),
#                   Directory Server will not be tuned automatically. Only a zip file
#                   containing the Directory Server tuner scripts will be created. You will need
#                   to uzip this in a temp location on the Directory Server machine
#                   and execute the amtune-ds script.
#                   
#                   3. On selectively tuning different components
#                   Different components (such as Web Container, OpenSSO, Directory)
#                   can be selectively based on AMTUNE_TUNE_* parameters. (Described in
#                   detail next in the next comment block)
#-------------------------------------------------------------------------------
$AMTUNE_MODE = "REVIEW";

#-------------------------------------------------------------------------------
# Parameter     :   AMTUNE_LOG_LEVEL
# Values        :   <logging level>
# Default       :   FILE
# Description   :   if AMTUNE_LOG_LEVEL is
#			NONE : Nothing is displayed and logged in the debug file
#			TERM : The output is only displayed on the terminal
#			FILE : The output is displayed on both terminal and debug file (Default)
#
#-------------------------------------------------------------------------------
$AMTUNE_LOG_LEVEL="FILE";

#-------------------------------------------------------------------------------
# Parameters    :   AMTUNE_TUNE_*
# Value         :   true or false
# Default       :   true
#
# Description   :   You can choose specific components to be tuned by the tuner scripts
#                       Components to tune:
#                       a) AMTUNE_TUNE_DS               - Directory Server
#                       b) AMTUNE_TUNE_WEB_CONTAINER    - Web Container - Application Server or Web Server
#                       c) AMTUNE_TUNE_IDENTITY         - OpenSSO
#                   These settings work in conjunction with AMTUNE_MODE parameter setting.
#                   You could review or change recommended tunings of any set of components.
#
# Note          :   Read note 2 on AMTUNE_MODE
#-------------------------------------------------------------------------------

my $AMTUNE_TUNE_DS="true";
my $AMTUNE_TUNE_WEB_CONTAINER="true";
my $AMTUNE_TUNE_IDENTITY="true";

#-------------------------------------------------------------------------------
# Installation Environment
#-------------------------------------------------------------------------------

#-------------------------------------------------------------------------------
# Parameter     :   OSTYPE
# 
# Description   :   There should be no need to change this value.
#                   Its here because this variable is referenced to construct
#                   other parameters
#-------------------------------------------------------------------------------
my $Register = "SYSTEM\\CurrentControlSet\\Control\\Session Manager\\Environment";
my ($hkey,@key_list,$key,%values);
$HKEY_LOCAL_MACHINE->Open($Register,$hkey)|| die $!;
$hkey->GetValues(\%values);
foreach $value (keys(%values))
{
	if($value eq "OS")
	{
		$key=$values{$value}->[0];
		$val2=$values{$value}->[2];
		$os_name=$val2;
	}
	if($value eq "PROCESSOR_ARCHITECTURE")
	{	
		$key=$values{$value}->[0];
		$val2=$values{$value}->[2];
		$processor_architecture=$val2;
	}
	
	
}
$hkey->Close();

$OSTYPE=$os_name;

#-------------------------------------------------------------------------------
# Parameter     :   OSPLATFORM
#
# Description   :   There should be no need to change this value.
#                   Its here because this variable is referenced to construct
#                   other parameters
#-------------------------------------------------------------------------------
$OSPLATFORM=$processor_architecture;

#-------------------------------------------------------------------------------
# Parameter     :   HOSTNAME
# Value         :   Host name 
#
# Description   :   If the hostname for your environment cannot be obtained, 
#                   please comment the following line and add a line setting the right hostname. 
#                   eg. HOSTNAME=xyz
#-------------------------------------------------------------------------------
my $Register = "SYSTEM\\CurrentControlSet\\Services\\Tcpip\\Parameters";
my ($hkey,@key_list,$key,%values);
$HKEY_LOCAL_MACHINE->Open($Register,$hkey)|| die $!;
$hkey->GetValues(\%values);
foreach $value (keys(%values))
{
	if($value eq "Hostname")
	{
		$key=$values{$value}->[0];
		$val2=$values{$value}->[2];
		$hostname=$val2;
	}
	
	if($value eq "Domain")
	{
		$key=$values{$value}->[0];
		$val2=$values{$value}->[2];
		$domain=$val2;
	}
	
}
$hkey->Close();
$HOSTNAME=$hostname;

#-------------------------------------------------------------------------------
# Parameter     :   DOMAINNAME
#
# Description   :   If domainname for your environment cannot be obtained,
#                   please comment the following line and add a line setting the right domainname. 
#                   eg. DOMAINNAME=yyy.com
#-------------------------------------------------------------------------------

$DOMAINNAME=$domain;

if(( $HOSTNAME ne "") && ($DOMAINNAME ne ""))
{	
	$HOSTNAME=$HOSTNAME.".".$DOMAINNAME;
}
#-------------------------------------------------------------------------------
# Parameter     :   BASEDIR
# Value         :   Java Enterprise System base directory
#
# Description   :   Other install directories are constructed from this.
#                    Ex: c:/sun 
#-------------------------------------------------------------------------------
$BASEDIR="";
if("$BASEDIR" eq "")
{
	print "Please Enter Base directory value:\n";
	exit (1);
}


#-------------------------------------------------------------------------------
# Parameter     :   IS_INSTALL_DIR
# Value         :   OpenSSO installation directory
#
# Description   :   Leave this variable blank to use default location
#-------------------------------------------------------------------------------
$IS_INSTALL_DIR="";

#-------------------------------------------------------------------------------
# Parameter     :   IS_SINGLE_WAR_FILE_DEPLOYMENT
# Value         :   TRUE/FALSE
#
# Description   :   Set to TRUE if AM installation is single war file deployment.
#-------------------------------------------------------------------------------

$IS_SINGLE_WAR_FILE_DEPLOYMENT="FALSE";

$PRODUCT_DIR=identity;

if("$IS_INSTALL_DIR" eq "" && $IS_SINGLE_WAR_FILE_DEPLOYMENT eq "FALSE")
{
	$IS_INSTALL_DIR="$BASEDIR/$PRODUCT_DIR";
	$IS_CONFIG_DIR="$IS_INSTALL_DIR/config";
}
elsif("$IS_INSTALL_DIR" eq "" && $IS_SINGLE_WAR_FILE_DEPLOYMENT eq "TRUE")
{
	$IS_INSTALL_DIR="@BASE_DIR@";
   	$IS_CONFIG_DIR="@CONFIG_DIR@";
}


#-------------------------------------------------------------------------------
# Parameter     :   AMTUNE_BIN_DIR
# Value         :   Amtune scripts location
# Default       :   
#
# Description   :   Typical value is the default value. There should be no 
#                   reason to change this value.
#-------------------------------------------------------------------------------
$AMTUNE_BIN_DIR=dirname($0);

#-------------------------------------------------------------------------------
# Parameter     :   WEB_CONTAINER
# Values        :   WS7 or AS8
#
# Description   :   Name of the Web Container that OpenSSO is deployed on.
#                   Accepted values for WEB_CONTAINER
#                   WebServer6.1=WS61, WebServer7=WS7, AppServer 7.x=AS7, AppServer 8.x=AS8
#
#                   If you specify any other value, you will receive a
#                   validation error
#-------------------------------------------------------------------------------
$WEB_CONTAINER=WS7;

if (( "$WEB_CONTAINER" eq "" ) || ( "$WEB_CONTAINER" eq "WS7" ))
{
	$CONTAINER_BASE_DIR="$BASEDIR/WebServer7";
}
elsif("$WEB_CONTAINER" eq "WS61")
{
	$CONTAINER_BASE_DIR="$BASEDIR/webserver";
}
elsif("$WEB_CONTAINER" eq "AS8")
{
	$CONTAINER_BASE_DIR="$BASEDIR/appserver";
}



#-------------------------------------------------------------------------------
# Parameter     :   WEB_CONTAINER_INSTANCE_NAME
# Values        :   <web container instance name>
# Default       :   If this parameter is not defined in WEB_CONTAINER_INSTANCE_NAME=  , the default 
#				    value will be: 
#				    ${HOSTNAME}                             - for Web Server 6.1 or later
#                   domains/server1                         - for Application Server 7.x
#                   domains/domain1                         - for Application Server 8.x
#
# Description   :   Typically, this value is the hostname where OpenSSO is deployed.
#                   When you have multiple instances for the web container,
#                   this value might be different from the hostname.
#                   Please customize this accordingly
#                   
#-------------------------------------------------------------------------------

@WEB_WS=split(undef,$WEB_CONTAINER);
$WEB_COMPARE=$WEB_WS[0].$WEB_WS[1];

if($WEB_CONTAINER_INSTANCE_NAME eq "")
{
	if(($WEB_CONTAINER eq "")||( $WEB_COMPARE eq "WS"))
	{
		$WEB_CONTAINER_INSTANCE_NAME=${HOSTNAME};
	}
	elsif($WEB_CONTAINER eq "AS8")
	{	
		$WEB_CONTAINER_INSTANCE_NAME="domains/domain1";
	}
	else
	{
		$WEB_CONTAINER_INSTANCE_NAME="domains/server1";
	}
}

#-------------------------------------------------------------------------------
# Parameter     :   CONTAINER_INSTANCE_DIR
# Value         :   OpenSSO Web Container instance directory
# Default       :   $CONTAINER_BASE_DIR/https-${WEB_CONTAINER_INSTANCE_NAME}    - for Web Server 6.1 or later
#                   $CONTAINER_BASE_DIR/${WEB_CONTAINER_INSTANCE_NAME}          - for Application Server 7.0 or later
#
# Description   :   Typical value is the default value. If you install OpenSSO in a
#                   non-default location, you will need to change this value
#-------------------------------------------------------------------------------

if(($WEB_CONTAINER eq "")||( $WEB_COMPARE eq "WS"))
{
	$CONTAINER_INSTANCE_DIR="$CONTAINER_BASE_DIR/https-$WEB_CONTAINER_INSTANCE_NAME";
}
else
{
	$CONTAINER_INSTANCE_DIR="$CONTAINER_BASE_DIR/$WEB_CONTAINER_INSTANCE_NAME";
}

#-------------------------------------------------------------------------------
# Parameters        : WSADMIN_*
# Parameter Details : The following parameters need to appropriately configured
#                     when the Web Container being tuned is WS7
#                     Each of these parameters are required for tuning the WS7 
#                     web container. In this note, you will find a simplied explanation
#                     for each of these parameters, their default values, and 
#                     acceptable values
#
# Parameter	    : WSADMIN_DIR
# Value             : WS7 installation location
# 
# Parameter         : WSADMIN
# Value             : WS7 admin utility location
# Default Value     : WSADMIN_DIR/bin/wadm
#
# Parameter         : WSADMIN_USER
# Value             : WS7 administrator user account
# Default Value     : admin
#
# Parameter         : WSADMIN_PASSFILE
# Value             : A temporary password file location used by the WSADMIN tool. 
#                     This file will be created and deleted by the amtune-ws7 script
#
# Parameter         : WSADMIN_HOST
# Value             : WS7 admin host name
# Default Value     : localhost
#
# Parameter         : WSADMIN_PORT
# Value             : WS7 admin port
#                     - If this port is a secure port, make sure WSADMIN_SECURE value is set to "--secure"
#                       If this port is not secure, make sure to set WSADMIN_SECURE value to ""
# Default Value     : 8989
#
# Parameter         : WSADMIN_SECURE
# Value             : Set this value to "--ssl=true" or blank if the WSADMIN_PORT is secure. Otherwise,
#                        set to "--ssl=false for non-secure WSADMIN_PORT
# Default Value     : "--ssl=true"
#
# Parameter         : WSADMIN_HTTPLISTENER
# Value             : WS7 HttpListner name
# Default Value     : http-listener-1
#
#-------------------------------------------------------------------------------
$WSADMIN_DIR="$BASEDIR/WebServer7";

$WSADMIN="$WSADMIN_DIR/bin/wadm.bat";

$WSADMIN_USER=admin;

$WSADMIN_PASSFILE="$SCRIPT_LOCATION/pass.txt";

$WSADMIN_HOST=$HOSTNAME;
$WSADMIN_PORT=8989;

$WSADMIN_SECURE="--ssl=true";
   
$WSADMIN_CONFIG=$WEB_CONTAINER_INSTANCE_NAME;

$WSADMIN_HTTPLISTENER="http-listener-1";

#Do not modify WSADMIN_PASSWORD_SYNTAX
$WSADMIN_PASSWORD_SYNTAX="wadm_password=";

@WSADMIN_COMMON_PARAMS_NO_CONFIG=("--user","$WSADMIN_USER","--password-file","$WSADMIN_PASSFILE","--host=$WSADMIN_HOST","--port=$WSADMIN_PORT","$WSADMIN_SECURE");

@WSADMIN_COMMON_PARAMS=(@WSADMIN_COMMON_PARAMS_NO_CONFIG,"--config=$WSADMIN_CONFIG");

#-------------------------------------------------------------------------------
# Parameters        : ASADMIN_*
# Parameter Details : The following parameters need to appropriately configured
#                     when the Web Container being tuned is AS8
#                     Each of these parameters are required for tuning the AS8 
#                     web container. In this note, you will find a simplied explanation
#                     for each of these parameters, their default values, and 
#                     acceptable values
#
# Parameter         : ASADMIN_DIR
# Value             : AS8 installation location
#
# Parameter         : ASADMIN
# Value             : AS8 admin utility location
# Default Value     : ASADMIN_DIR/bin/asadmin
#
# Parameter         : ASADMIN_USER
# Value             : AS8 administrator user account
# Default Value     : admin
#
# Parameter         : ASADMIN_PASSFILE
# Value             : A temporary password file location used by the ASADMIN tool. 
#                     This file will be created and deleted by the amtune-as8 script
#
# Parameter         : ASADMIN_HOST
# Value             : AS8 admin host name
# Default Value     : localhost
#
# Parameter         : ASADMIN_PORT
# Value             : AS8 admin port
#                     - If this port is a secure port, make sure ASADMIN_SECURE value is set to "--secure"
#                       If this port is not secure, make sure to set ASADMIN_SECURE value to ""
# Default Value     : 4849
#
# Parameter         : ASADMIN_SECURE
# Value             : Set this value to "--secure" if the ASADMIN_PORT is secure. Otherwise,
#                        leave this blank
# Default Value     : "--secure"
#
# Parameter         : ASADMIN_TARGET
# Value             : Typically, this value is set to 'server'  (with the assumption that
#                       this AS 8 installation is exclusively used for OpenSSO/Portal Server
# Default Value     : server
#
# Parameter         : ASADMIN_HTTPLISTENER
# Value             : AS HttpListner name
# Default Value     : http-listener-1
#
# Parameter         : ASADMIN_INTERACTIVE
# Value             : Please do not change this parameter.
#                       AS Admin will operate in an interactive way. But, this is not a tested option
#                       as of this release. Might be useful in debugging amtune problems. But, 
#                       its not advisable to change this value
# Default Value     : false
#
# Parameter         : AMTUNE_WEB_CONTAINER_JAVA_POLICY
# Value             : Please do not change this parameter.
#
#                       AS8 adds a significant overhead for evaluating Java Security Descriptors i
#                       specified in the server.policy file. In some cases, you might want this turned off
#                       
# Default Value     : false 
#
#-------------------------------------------------------------------------------
$ASADMIN_DIR="$BASEDIR/appserver";

$ASADMIN="$ASADMIN_DIR/bin/asadmin.bat";

$ASADMIN_USER="admin";

#$ASADMIN_PASSFILE="$SCRIPT_LOCATION/pass.txt";
$ASADMIN_PASSFILE="./pass.txt";

$ASADMIN_HOST="$HOSTNAME";

$ASADMIN_PORT=4849;

$ASADMIN_SECURE="--secure";
   
$ASADMIN_TARGET="server";

$ASADMIN_HTTPLISTENER="http-listener-1";

#Do not modify ASADMIN_INTERACTIVE
$ASADMIN_INTERACTIVE="false";

$AMTUNE_WEB_CONTAINER_JAVA_POLICY="false";

#Do not modify ASADMIN_PASSWORD_SYNTAX
$ASADMIN_PASSWORD_SYNTAX="AS_ADMIN_PASSWORD=";

@ASADMIN_COMMON_PARAMS_NO_TARGET=("--user",$ASADMIN_USER,"--passwordfile",$ASADMIN_PASSFILE,"--host",$ASADMIN_HOST,"--port",$ASADMIN_PORT,$ASADMIN_SECURE,"--interactive=$ASADMIN_INTERACTIVE");

@ASADMIN_COMMON_PARAMS=(@ASADMIN_COMMON_PARAMS_NO_TARGET,"--target",$ASADMIN_TARGET);


#-------------------------------------------------------------------------------
# Parameter     :   IS_INSTANCE_NAME
# Value         :   OpenSSO Instance Name
# Default       :   <empty>
#                   
#
# Description   :   This value is used in determining the property
#                   filenames for the OpenSSO install.
#                   Multiple instances of OpenSSO could be deployed in
#                   the same machine.
#                   Assumption is that there will be one set of property files
#                   per OpenSSO instance and the instance name will be
#                   appended to the 
#                   file names. If there is only one instance of OpenSSO in a machine,
#                   then the instance name will not be appended to the file names
#   
#                   e.g. If your OpenSSO is installed on a machine named
#                   server.sun.com,
#                   typically, your first instance of the web server will be 
#                   https-server.sun.com.
#                   The property files for the first OpenSSO instance will
#                   not have the instance name appended. (AMConfig.properties)
#                   In case of multiple instances, you will have different names.
#                   lets take an example of 3 instances. Instances could be 
#                   server.sun.com-instance1, server.sun.com-instance2, 
#                   server.sun.com-instance3. 
#                   If 3 instances of OpenSSO are deployed, one per
#                   container instance, then the property files will look
#                   something like the following:
#                   AMConfig-instance1.properties
#                   AMConfig-instance2.properties
#                   AMConfig-instance3.properties
#               
#                   You can specify IS_INSTANCE_NAME=instance1. 
#                   AMTUNE will resolve the property file names in the following order:
#                   in the following order (eg. AMConfig.properties) :
#                   1. AMConfig-<IS_INSTANCE_NAME>
#                   2. AMConfig-<WEB_CONTAINER_INSTANCE_NAME>
#                   3. AMConfig.properties
#                  
#                   The tool will use the first available property file in the list and use 
#                   it.
#                   
#                   Another important note here is that the tool "amadmin" should
#                   point to the correct server name as well (java option -Dserver.name=<IS_INSTANCE_NAME>)
#                   
# Significance  :   AMTune will automatically try to associate your instance names with 
#                   OpenSSO Property files using this parameter.
#                   Currently, only 2 files are based on this Instance Name
#                       a. AMConfig.properties file
#                       b. serverconfig.xml
#-------------------------------------------------------------------------------
$IS_INSTANCE_NAME="";

#-------------------------------------------------------------------------------
# Directory stuff
#-------------------------------------------------------------------------------
$DIRMGR_UID="\"cn=Directory Manager\"";

$RAM_DISK=$ENV{TEMP};

#-------------------------------------------------------------------------------
# Parameter     :   DEFAULT_ORG_PEOPLE_CONTAINER
# Value         :   Default Organization's People container name
# Default       :   <empty>
# Description   :   This value is used to tune the LDAP Auth Module's
#                   search base. This can be a very useful tuning 
#                   parameter when there are no sub-orgs in the default
#                   organization
#                   If this value is set to empty, the tuning will be skipped
# Note          :   Along with appending the people container to the search base,
#                   the search scope will be modified to "OBJECT" level. Default
#                   search scope is "SUBTREE"
#-------------------------------------------------------------------------------
$DEFAULT_ORG_PEOPLE_CONTAINER="";

#-------------------------------------------------------------------------------
# Parameter     :   AMTUNE_DEBUG_FILE_PREFIX
# Values        :   <log file name prefix>
# Default       :   amtune
# Note          :   Here, you specify just the prefix name for the file.
#                   The file will be automatically created in the debug directory for this deployment
#                   based on the AMConfig.property "com.iplanet.services.debug.directory"
#
# Description   :   If this value is set to a non-empty value, then
#                   all the operations performed by amtune scripts will be logged.
#                   AMConfig.property's "com.iplanet.services.debug.directory=" setting
#                   dictates where the file will be created.
#
#                   In future, amtune might be intelligent enough to also 
#                   provide different levels of information based on "com.iplanet.services.debug.level"
#                   setting.
#                   
# Significance  :   If this value is not specified, then no debugging information
#                   will be recorded. Essentially, all output will go to /dev/null
#-------------------------------------------------------------------------------
$AMTUNE_DEBUG_FILE_PREFIX=amtune;

#-------------------------------------------------------------------------------
# Parameter     :   AMTUNE_PCT_MEMORY_TO_USE
# Values        :   0-100
# Default       :   75
# Description   :   Percentage value; Dictates how much of available memory
#                   will be used by OpenSSO
# Note          :   Currently, OpenSSO can use a maximum of 4GB. This is the 
#                   per-process address space limit for 32-bit apps.
#
#                   OpenSSO currently requires a minimum of 256MB RAM
#
#                   When you set AMTUNE_PCT_MEMORY_TO_USE to 100, the maximum 
#                   space allocated for OpenSSO would be the minimum
#                   between 4GB and 100% of available RAM
#
#                   When you set AMTUNE_PCT_MEMORY_TO_USE to 0, OpenSSO
#                   will be configured to use 256MB RAM 
#                   
# Significance  :   This value is the driving force in tuning OpenSSO.
#                   The following values are derived from this setting:
#                   1. JVM memory usage - Heap Sizes, NewSizes, PermSizes
#                   2. ThreadPool sizes - WS RqThrottle, 
#                           Authentication LDAP Connection Pool,
#                           SM LDAP Connection Pool,
#                           Notification ThreadPools
#                           
#                   3. OpenSSO Caches - SDK Caches, Session Caches
#                           (NYI: Policy Caches)
#                   4. Max Sizes - Max. #Sessions, Max #CacheEntries
#                   
#-------------------------------------------------------------------------------
$AMTUNE_PCT_MEMORY_TO_USE=75;

#-------------------------------------------------------------------------------
# Parameter     :   AMTUNE_PER_THREAD_STACK_SIZE_IN_KB
#		    AMTUNE_PER_THREAD_STACK_SIZE_IN_KB_64_BIT
# Value         :   <value in KB>
# Default       :   128 (KB) for 32-bit and 256 for 64-bit
#               
#
# Description   :   This value is the available stack space per thread in Java(/Web Container).
#                   Per thread stack size is used to tune various thread related
#                   parameters in OpenSSO and the Web Container.
#                   
#                   128 KB is a reasonable value for this, and should not be 
#                   changed unless otherwise absolutely necessary
#-------------------------------------------------------------------------------
$AMTUNE_PER_THREAD_STACK_SIZE_IN_KB=128;
$AMTUNE_PER_THREAD_STACK_SIZE_IN_KB_64_BIT=512;

#-------------------------------------------------------------------------------
# Parameters    :   AMTUNE_SESSION_MAX_SESSION_TIME_IN_MTS
#                   AMTUNE_SESSION_MAX_IDLE_TIME_IN_MTS
#                   AMTUNE_SESSION_MAX_CACHING_TIME_IN_MTS
# Values        :   Max Session Time, Max Idle Time, Max Session Caching Time in minutes
# Default       :   AMTune will not tune these parameters by default. See Note below.
#                   Max Session Time default    - 60 minutes
#                   Session Max Idle Time       - 10 minutes
#                   Session Max Caching Time    - 02 mintues
#
# Description   :   The following three values will change the global session timeout 
#                   values. If Session Service is registered and customized at any other
#                   level, the tuning will not apply to them
#
# Significance  :   Setting this value to very high or very low values (minutes) will affect the
#                   number of users an OpenSSO deployment can support. Hence, these parameters
#                   could be optionally tuned using this tool
#
# Note          :   You will have to explicitly enable session timeout tuning by setting
#                   AMTUNE_DONT_TOUCH_SESSION_PARAMETERS=false.
#                   Dont assume that that the defaults provided are correct. 
#
#   USE THIS TUNING OPTION WITH CARE!!!
#-------------------------------------------------------------------------------
$AMTUNE_DONT_TOUCH_SESSION_PARAMETERS="true";
$AMTUNE_SESSION_MAX_SESSION_TIME_IN_MTS=60;
$AMTUNE_SESSION_MAX_IDLE_TIME_IN_MTS=10;
$AMTUNE_SESSION_MAX_CACHING_TIME_IN_MTS=2;

#-------------------------------------------------------------------------------
# Parameter     :   AMTUNE_MEM_MAX_HEAP_SIZE_RATIO
#                   AMTUNE_MEM_MIN_HEAP_SIZE_RATIO
# Value         :   Percentage to calculate for max heap size and min heap size
# Default       :   7/8 for max heap size
#                   1/2 for min heap size
#
# Description   :   These parameters are used to calculate the max and min heap sizes.
#                   You should only change these parameters for JVM 64 bits.  For JVM 32 bits
#                   leave the default values.
#                   Note: WS uses about 1/8 of the OpenSSO process memory. Hence
#                   this setting
#-------------------------------------------------------------------------------
$AMTUNE_MEM_MAX_HEAP_SIZE_RATIO=7/8;
$AMTUNE_MEM_MIN_HEAP_SIZE_RATIO=1/2;

#-------------------------------------------------------------------------------
# Parameter     :   AMTUNE_MIN_MEMORY_TO_USE_IN_MB
#                   AMTUNE_MAX_MEMORY_TO_USE_IN_MB_DEFAULT
#                   AMTUNE_MAX_MEMORY_TO_USE_IN_MB_X86
# Value         :
# Default       :
#
# Description	:   Minimum and maximum amount of memory in MB that should not be
#		    exceeded.
#-------------------------------------------------------------------------------
##
$AMTUNE_MIN_MEMORY_TO_USE_IN_MB=100;
$AMTUNE_MAX_MEMORY_TO_USE_IN_MB_WINDOWS=512;
$AMTUNE_MAX_MEMORY_TO_USE_IN_MB_DEFAULT=512;
$AMTUNE_MAX_MEMORY_TO_USE_IN_MB_X86=$AMTUNE_MAX_MEMORY_TO_USE_IN_MB_DEFAULT;

################################################################################
############### End: Performance Related User Inputs ###########################
################################################################################

################################################################################
############### Start: amtune Internals ########################################
############### DO NOT MODIFY BELOW THIS LINE ##################################
################################################################################
#===============================================================================
# amtune Constants
#===============================================================================

#Sizes are estimates based on actual usage obtained thru JProbe
$AMTUNE_AVG_PER_ENTRY_CACHE_SIZE_IN_KB=8;
$AMTUNE_AVG_PER_SESSION_SIZE_IN_KB=4;

#Per thread stack size is a JDK recommended value
$AMTUNE_DEF_PER_THREAD_STACK_SIZE_IN_KB=128;

#Out the memory available for Java part of the OpenSSO process memory, 
#the following is the breakdown of memory needs
$AMTUNE_MEM_MAX_NEW_SIZE=1/8;
$AMTUNE_MEM_MAX_PERM_SIZE=1/12;
$AMTUNE_MEM_THREADS_SIZE=1/16;
$AMTUNE_MEM_OPERATIONAL=19/48;
$AMTUNE_MEM_CACHES_SIZE=1/3;

#Out of the memory available for OpenSSO Caches, 
#the breakdown b/w SDK and Session Cache size is as follows:
#NOTE   :   Its not clear how much memory Policy, Liberty, SAML Caches use. 
#           These fall into the OPERATIONAL memory category. 
#           Once we have an estimate on them, will adjust these values appropriately. 
#           OPERATIONAL memory is large enough to handle these unknown quantities
#
$AMTUNE_MEM_SDK_CACHE_SIZE=2/3;
$AMTUNE_MEM_SESSION_CACHE_SIZE=1/3;

#Notification queue size is estimated to be 10% of concurrent sessions
#Basically, OpenSSO can operate properly as long as 10% or less sessions expire under a short period of time
#Current estimation of "short period of time" is 5-10 minutes.
$AMTUNE_NOTIFICATION_QUEUE_SIZE=1/1000;

$AMTUNE_MAX_NUM_THREADS="$AMTUNE_MEM_THREADS_SIZE*(1024/$AMTUNE_PER_THREAD_STACK_SIZE_IN_KB)";
$AMTUNE_MAX_NUM_THREADS_64_BIT="$AMTUNE_MEM_THREADS_SIZE*(1024/$AMTUNE_PER_THREAD_STACK_SIZE_IN_KB_64_BIT)";

#WS internal threads used. This is not really factored into any calculation.
#But, nevertheless, its useful to know how much we estimated. Typical value for this
#is about 50. But, we leave one more fold contingent threads
$AMTUNE_NUM_WS_INTERNAL_THREADS=100;

#Established Thread Counts 
$AMTUNE_NUM_JAVA_INTERNAL_THREADS=8;
$AMTUNE_NUM_JAVA_APPS_DEPLOYED=6;
$AMTUNE_NUM_IS_INTERNAL_THREADS=3;

#After all the known threads are taken into account, we still plan for about a 3rd more
#threads in the system.
#The tuner program will figure out how much memory can be used up by threads and reserves 
# 1/3 of them for unplanned threads
$AMTUNE_THREADS_UNPLANNED=1/3;

#Known threads breakdown
$AMTUNE_WS_RQTHROTTLE_THREADS=5/12;
$AMTUNE_IS_OPERATIONAL_THREADS=5/12;
$AMTUNE_IS_AUTH_LDAP_THREADS=1/12;
$AMTUNE_IS_SM_LDAP_THREADS=1/24;
$AMTUNE_IS_NOTIFICATION_THREADS=1/48;

#Defines how many notifications can be in the pending queue per notification thread
$AMTUNE_IS_NOTIFICATION_QUEUE_SIZE_PER_THREAD=100;

#Some known WS and OpenSSO Defaults
$AMTUNE_NUM_WS_RQTHROTTLE_MIN=10;
$AMTUNE_NUM_WS_THREAD_INCREMENT=10;
$AMTUNE_NUM_IS_MIN_AUTH_LDAP_THREADS=10;
$AMTUNE_NUM_WS_MIN_THREADS=10;
$AMTUNE_STATISTIC_ENABLED=false;

#Just plain constants
$AMTUNE_NUM_FILE_DESCRIPTORS=65536;
$AMTUNE_NUM_TCP_CONN_SIZE=8192;
$AMTUNE_NATIVE_STACK_SIZE_64_BIT=262144;

#AMTune Error Status Codes
$AMTUNE_INVALID_CMDLINE_PARAMETER=100;
$AMTUNE_INVALID_ENVIRON_SETTING=200;

#AMTune scripts records
#AMTune record fields=script_name|script type|script expected arguments
$AMTUNE_MAIN_SCRIPT="amtune.pl|all|2";
$AMTUNE_AM_SCRIPT="amtune-identity.pl|am|0";
$AMTUNE_WS7_SCRIPT="amtune-ws7.pl|ws|1";
$AMTUNE_AS8_SCRIPT="amtune-as8.pl|as|1";
$AMTUNE_DS_SCRIPT="amtune-prepareDSTuner.pl|ds|1";

#############################################################################
# amtune functions
#############################################################################
#-------------------------------------------------------------------------------
# Function      :   checkArgs
# Parameters    :   Number of arguments, argument1, argument2, and argument3
# Output        :   - None -
# Description   :   Check for required arguments when the script is executed and
#                   display Usage information
#-------------------------------------------------------------------------------

sub checkArgs
{
	($numOfArgs,$argString1,$argString2,$argString3)=@_;
		
	$NUM_PARAMS_EXPECTED=$AMTUNE_SCRIPT_ARGS;
		
	if(($argString1 eq "?") || ($argString1 eq "help") || ( $numOfArgs lt $NUM_PARAMS_EXPECTED))
	{
		&getUsage;
		exit($AMTUNE_INVALID_CMDLINE_PARAMETER);
	}
	
	if($AMTUNE_SCRIPT_TYPE eq ws)
	{
		$WSADMIN_PASSWORD=$argString1;
		$display_usage_password="<web server admin password>";
	}
	elsif($AMTUNE_SCRIPT_TYPE eq as)
	{
		$ASADMIN_PASSWORD=$argString1;
		$display_usage_password="<application server admin password>";
	}
	
	elsif($AMTUNE_SCRIPT_TYPE eq ds)
	{
		$DIRMGR_PASSWORD=$argString1;
		$display_usage_password="<directory server admin password>";
	}
	
	elsif($AMTUNE_SCRIPT_TYPE eq am)
	{
		$WSADMIN_PASSWORD=$argString1;
		$display_usage_password="[optional <web server admin password> if web server running in 64-bit mode]";
	}
	else
	{
		$ADMIN_PASSWORD=$argString1;
		$DIRMGR_PASSWORD=$argString2;
		if(($WEB_CONTAINER eq "AS7")||($WEB_CONTAINER eq "WS61"))
		{
			$NUM_PARAMS_EXPECTED=$NUM_PARAMS_EXPECTED - 1;
			$display_usage_password="";
		}
		elsif($WEB_CONTAINER eq "AS8")
		{
			$ASADMIN_PASSWORD=$argString2;
			$display_usage_password="<application server admin password>";
		}
		else
		{
			$WSADMIN_PASSWORD=$argString2;
			$display_usage_password="<web server admin password>";
		}
	}
}

#-------------------------------------------------------------------------------
# Function      :   getUsage()
# Parameters    :   <none>
# Output        :   <none>
# Description   :   Display help information of amtune scripts
#-------------------------------------------------------------------------------

sub getUsage
{
	print " Usage\n";
	print " You can use amtune scripts in two ways\n";
	print " 	1. Use the wrapper script - amtune \n";
	print " 	2. Use individual scripts - amtune-<component name>\n";
	print "\n";
	print " Mandatory Parameter for this script:";
	
	if($NUM_PARAMS_EXPECTED == 0)
	{
		print "$RUN_SCRIPT_NAME [No argument required]\n";
	}
	elsif($NUM_PARAMS_EXPECTED == 1)
	{
		print "$RUN_SCRIPT_NAME $display_usage_password\n";
	}
	elsif($NUM_PARAMS_EXPECTED == 2)
	{
		print "$RUN_SCRIPT_NAME <OpenSSO server password> $display_usage_password\n";
	}
	else
	{
		print "ERROR: Invalid number of expected parameter $NUM_PARAMS_EXPECTED\n";
	}
	
	print " \n To display this help menu, enter $RUN_SCRIPT_NAME ? or help\n";
	print " Please make sure to customize amtune-env to suit your deployment.\n";
}

#############################################################################
# Start of main program
#############################################################################

$SCRIPT_BASENAME=basename($0);
$SCRIPT_LOCATION=dirname($0);
$RUN_SCRIPT_NAME=$SCRIPT_BASENAME;

#-------------------------------------------------------------------------------
#Source utility scripts
#-------------------------------------------------------------------------------
$INSTALL_FILE_NOT_REQUIRED=true;

#import amtune utils
$AMTUNE_UTILS_SCRIPT="$AMTUNE_BIN_DIR/amtune-utils.pl";
if(! -f $AMTUNE_UTILS_SCRIPT)
{
	print "ERROR: $AMTUNE_UTILS_SCRIPT not found.\n";
}

$PERL_CMD="perl";
@args=($PERL_CMD,$AMTUNE_UTILS_SCRIPT);
system(@args)==0 or die "\n Error executing command @args\n";
#`perl $AMTUNE_UTILS_SCRIPT`;


$scriptRecord=$AMTUNE_SCRIPT_RECORD_STRING;
@split_name=split(m/\|/,$scriptRecord);
$AMTUNE_SCRIPT_NAME=$split_name[0];
$AMTUNE_SCRIPT_TYPE=$split_name[1];
$AMTUNE_SCRIPT_ARGS=$split_name[2];
checkArgs(($#ARGV+1),$ARGV[0],$ARGV[1],$ARGV[2]);

$g=localtime();
$SCRIPT_BASENAME="$SCRIPT_LOCATION/$SCRIPT_BASENAME : $g";

# OpenSSO Constants
$ADMIN_CLIENT="$IS_INSTALL_DIR/bin/amadmin.bat";
$LDAPSEARCH="$BASEDIR/share/bin/ldapsearch.exe";
$LDAPMODIFY="$BASEDIR/share/bin/ldapmodify.exe";
$LDAPDELETE="$BASEDIR/share/bin/ldapdelete.exe";

# Retrieve AM config file name
&setAMConfigPropertyFile;

# Only validate AM directories and configuration if amtune script is identity 
# or prepareDSTuner or wraper.
if(($AMTUNE_SCRIPT_TYPE eq "all")||($AMTUNE_SCRIPT_TYPE eq "am")||($AMTUNE_SCRIPT_TYPE eq "ds"))
{
	#-------------------------------------------------------------------------------
	# Validate Installation Directory
	# FIXME: Might be a better solution to determine this with respect to
	#        the directory this script is found in (using dirname)
	#-------------------------------------------------------------------------------
	if(($IS_INSTALL_DIR eq "")||(! -d $IS_INSTALL_DIR)||(! -d "$IS_INSTALL_DIR/bin"))
	{
		print "Invalid OpenSSO installation. Cannot proceed.\n";
	    print "OpenSSO installation directory: $IS_INSTALL_DIR\n";
	    exit($AMTUNE_INVALID_ENVIRON_SETTING);
	}
	
	if(($IS_CONFIG_DIR eq "")||(! -d $IS_CONFIG_DIR))
	{
		print "OpenSSO configuration directory not found. Cannot proceed.\n";
	    print "OpenSSO configuration directory: $IS_CONFIG_DIR\n";
	    print "You may need to customize the following file appropriately: amtune-env.pl\n";
	    exit($AMTUNE_INVALID_ENVIRON_SETTING);
	}
	
	if(($ADMIN_CLIENT eq "")||(! -f $ADMIN_CLIENT))
	{
		print "OpenSSO Admin Client Utility not found. Cannot proceed.\n";
	    print "Current Admin Client Utility: $ADMIN_CLIENT\n";
	    print "You may need to customize the following file appropriately: amtune-env.pl\n";
	    exit($AMTUNE_INVALID_ENVIRON_SETTING);
	}
	
	if(($AMCONFIG_PROPERTY_FILE eq "")||(! -f $AMCONFIG_PROPERTY_FILE))
	{
		print "OpenSSO configuration file not found. Cannot proceed.\n";
	    print "OpenSSO configuration file : $AMCONFIG_PROPERTY_FILE\n";
	    exit($AMTUNE_INVALID_ENVIRON_SETTING);
	}
}
	


#-------------------------------------------------------------------------------
#Validate amtune-env Customizable Enviroment.
#-------------------------------------------------------------------------------
# Assume Review mode if nothing is specified
if($AMTUNE_MODE eq "")
{
	$AMTUNE_MODE="REVIEW";
}

#validate AMTUNE_MODE
@match_review=grep(m/REVIEW/i,$AMTUNE_MODE);
@match_change=grep(m/CHANGE/i,$AMTUNE_MODE);
if($#match_review > -1)
{
	$AMTUNE_MODE="REVIEW";
}
elsif($#match_change > -1)
{
	$AMTUNE_MODE="CHANGE";
}
else
{	
	print " AMTUNE_MODE is not valid. Cannot proceed.\n";
	print " Current AMTUNE_MODE value: $AMTUNE_MODE\n";
	print " You may need to customise the following file appropriately: amtune-env.pl\n";
	exit($AMTUNE_INVALID_ENVIRON_SETTING);
}

#validate AMTUNE_TUNE_* parameters	
@match_ds=grep(m/TRUE/i,$AMTUNE_TUNE_DS);
if($#match_ds > -1)
{
	$AMTUNE_TUNE_DS=true;
}
else
{
	$AMTUNE_TUNE_DS=false;
}

@match_identity=grep(m/TRUE/i,$AMTUNE_TUNE_IDENTITY);
if($#match_identity > -1)
{
	$AMTUNE_TUNE_IDENTITY=true;
}
else
{
	$AMTUNE_TUNE_IDENTITY=false;
}

@match_web_container=grep(m/TRUE/i,$AMTUNE_TUNE_WEB_CONTAINER);
if($#match_web_container > -1)
{
	$AMTUNE_TUNE_WEB_CONTAINER=true;
}
else
{
	$AMTUNE_TUNE_WEB_CONTAINER=false;
}

#validate hostname
if($HOSTNAME eq "")
{
	print " Host name not set, Cannot proceed.\n";
	print " You may need to customise the following file appropriately: amtune-env.pl\n";
	exit($AMTUNE_INVALID_ENVIRON_SETTING);
}

#validate domainname
if($DOMAINNAME eq "")
{
	print " Domain name not set, Cannot proceed.\n";
	print " You may need to customise the following file appropriately: amtune-env.pl\n";
	exit($AMTUNE_INVALID_ENVIRON_SETTING);
}

if( $WEB_CONTAINER_INSTANCE_NAME eq "")
{
	$WEB_CONTAINER_INSTANCE_NAME=${HOSTNAME};
}


#Only validate if amtune script is wrapper script, web server script, or app server script 
if(($AMTUNE_SCRIPT_TYPE eq "all")||($AMTUNE_SCRIPT_TYPE eq "ws")||($AMTUNE_SCRIPT_TYPE eq "as"))
{
	if($AMTUNE_TUNE_WEB_CONTAINER eq "true")
	{
		#Web Container validation 
		if($WEB_CONTAINER eq "")
		{
			print " Web Container not specified. Cannot proceed.\n";
			print " You may need to customize the following file appropriately: amtune-env.pl\n";
			exit($AMTUNE_INVALID_ENVIRON_SETTING);
		}
		
		# Check to make sure a correct web server or app server amtune script running 
		# web container should match with the end part of the script name after the dash "-" (WS7 <=> amtune-ws7.pl)
		@pattern=split(m/-/,$RUN_SCRIPT_NAME);
		$pattern_split=$pattern[1];
		$pattern_split =~ tr/a-z/A-Z/;
		@filename1=split(m/\./,$pattern_split);
		$file_name=$filename1[0];  		
				
		if(($file_name ne $WEB_CONTAINER) && ($AMTUNE_SCRIPT_TYPE ne all))
		{
			print "Wrong Web Container specified or wrong amtune script was executed. Cannot proceed.\n";
        	print "Current Web Container : $WEB_CONTAINER\n";
        	print "Current amtune script: $RUN_SCRIPT_NAME\n";
        	print "You may need to customize the following file appropriately: amtune-env\n";
        	exit($AMTUNE_INVALID_ENVIRON_SETTING);
       	}
        	
		&webContainerToTune;
    	if($WS_CONFIG eq "")
    	{
      		print " Web Container specifed not valid. Valid Web Containers are WS7, WS61, AS7, AS8. Cannot proceed.\n";
      		print " Current Web Container : $WEB_CONTAINER\n";
      		print " You may need to customise the following file appropriately: amtune-env.pl\n";
      		exit($AMTUNE_INVALID_ENVIRON_SETTING);
    	}
      		
    	if(($CONTAINER_BASE_DIR eq "")||(! -d $CONTAINER_BASE_DIR))
    	{
	 		print "Web Container configuration directory not found. Cannot proceed.\n";
	        print "Current Web Container base directory in CONTAINER_BASE_DIR : $CONTAINER_BASE_DIR\n";
			print "You may need to customize the following file appropriately: amtune-env.pl\n";
		    exit($AMTUNE_INVALID_ENVIRON_SETTING);
      	}
      		
      	if(($CONTAINER_INSTANCE_DIR eq "")||(! -d $CONTAINER_INSTANCE_DIR))
      	{
      		print "Web Container instance directory not found. Cannot proceed.\n";
	        print "Current Web Container instance directory in CONTAINER_INSTANCE_DIR : $CONTAINER_INSTANCE_DIR\n";
	        print "Please check this directory in your Web Container installation.  You may need to customize the parameter\n";
	        print "WEB_CONTAINER_INSTANCE_NAME in the following file appropriately: amtune-env.pl\n" ;
	        
	        @array=grep(m/^WS/,$WEB_CONTAINER);

			if(($WEB_CONTAINER eq "")||($#array > -1))
			{
				print "The format of Web Container instance directory is [CONTAINER_INSTANCE_DIR]=$CONTAINER_BASE_DIR/https-[WEB_CONTAINER_INSTANCE_NAME]\n";
	        }
			else
	        {
	        	print "The format of Web Container instance directory is [CONTAINER_INSTANCE_DIR]=$CONTAINER_BASE_DIR/[WEB_CONTAINER_INSTANCE_NAME]\n";
	        }
	        exit($AMTUNE_INVALID_ENVIRON_SETTING);
		}
		

		if($WEB_CONTAINER eq "WS7")
		{
			if(($WSADMIN eq "")||(! -f $WSADMIN))
			{
				print "WSADMIN tool not found. Cannot proceed.\n";
				print "Current WSADMIN setting: $WSADMIN\n";
				print "You may need to customize the following file appropriately: amtune-env.pl\n";
				exit($AMTUNE_INVALID_ENVIRON_SETTING);
			}
			
			if($WSADMIN_USER eq "")
			{
				print "WSADMIN_USER not configured. Cannot proceed.\n";
				print "Current WSADMIN_USER setting: $WSADMIN_USER\n";
				print "You may need to customize the following file appropriately: amtune-env.pl\n";
				exit($AMTUNE_INVALID_ENVIRON_SETTING);
			}
			
		 	if($WSADMIN_PASSFILE eq "")
			{
				print "WSADMIN_PASSFILE not configured. Cannot proceed.\n";
			 	print "Current WSADMIN_PASSFILE setting: $WSADMIN_PASSFILE\n";
			 	print "You may need to customize the following file appropriately: amtune-env.pl\n";
			 	exit($AMTUNE_INVALID_ENVIRON_SETTING);
			}
			 
			 
			$g=time;
			@file1=$WSADMIN_PASSFILE;
			utime $g,$g,@file1;
			 
			open(FP,">$WSADMIN_PASSFILE");
			if( $? != 0)
			{
				print "WSADMIN_PASSFILE not configured correctly (Cannot create file). Cannot proceed.\n";
			 	print "Current WSADMIN_PASSFILE setting: $WSADMIN_PASSFILE\n";
			 	print "You may need to customize the following file appropriately: amtune-env.pl\n";
			 	exit($AMTUNE_INVALID_ENVIRON_SETTING);
			}
			close(FP);
			unlink($WSADMIN_PASSFILE);
			 
			if($WSADMIN_HOST eq "")
			{
				print "WSADMIN_HOST not configured. Cannot proceed.\n";
			 	print "Current WSADMIN_HOST setting: $WSADMIN_HOST\n";
			 	print "You may need to customize the following file appropriately: amtune-env.pl\n";
              	exit($AMTUNE_INVALID_ENVIRON_SETTING);
			}
             	 
            if($WSADMIN_PORT eq "")
			{
				print "WSADMIN_PORT not configured. Cannot proceed.\n";
			 	print "Current WSADMIN_PORT setting: $WSADMIN_PORT\n";
			 	print "You may need to customize the following file appropriately: amtune-env.pl\n";
			 	exit($AMTUNE_INVALID_ENVIRON_SETTING);
			}
			 
			&validateWSConfig("$WSADMIN_CONFIG");
			 
			&validateWSHttpListener("$WSADMIN_HTTPLISTENER");
		}
		
		elsif($WEB_CONTAINER eq AS8)
		{
			if(($ASADMIN eq "")||(! -f $ASADMIN))
			{
				print "ASADMIN tool not found. Cannot proceed.\n";
				print "Current ASADMIN setting: $ASADMIN\n";
				print "You may need to customize the following file appropriately: amtune-env.pl\n";
				exit($AMTUNE_INVALID_ENVIRON_SETTING);
			}
			
			
			if($ASADMIN_USER eq "")
			{
				print "ASADMIN_USER not configured. Cannot proceed.\n";
				print "Current ASADMIN_USER setting: $ASADMIN_USER\n";
				print "You may need to customize the following file appropriately: amtune-env.pl\n";
				exit($AMTUNE_INVALID_ENVIRON_SETTING);
			}
			
			 
			if($ASADMIN_PASSFILE eq "")
			{
				print "ASADMIN_PASSFILE not configured. Cannot proceed.\n";
				print "Current ASADMIN_PASSFILE setting: $ASADMIN_PASSFILE\n";
				print "You may need to customize the following file appropriately: amtune-env.pl\n";
				exit($AMTUNE_INVALID_ENVIRON_SETTING);
			}
			
			$g=time;
			@file1=$ASADMIN_PASSFILE;
			utime $g,$g,@file1;
			
			if($? == 1 )
			{
				print "ASADMIN_PASSFILE not configured correctly (Cannot create file). Cannot proceed.\n";
				print "Current ASADMIN_PASSFILE setting: $ASADMIN_PASSFILE\n";
				print "You may need to customize the following file appropriately: amtune-env.pl\n";
				exit($AMTUNE_INVALID_ENVIRON_SETTING);
			}
			close(FP);
			#unlink($ASADMIN_PASSFILE);
			
					
			if($ASADMIN_HOST eq "")
			{
				print "ASADMIN_HOST not configured. Cannot proceed.\n";
				print "Current ASADMIN_HOST setting: $ASADMIN_HOST\n";
				print "You may need to customize the following file appropriately: amtune-env.pl\n";
				exit($AMTUNE_INVALID_ENVIRON_SETTING);
			}
			
			if($ASADMIN_PORT eq "")
			{
				print "ASADMIN_PORT not configured. Cannot proceed.\n";
				print "Current ASADMIN_PORT setting: $ASADMIN_PORT\n";
				print "You may need to customize the following file appropriately: amtune-env.pl\n";
				exit($AMTUNE_INVALID_ENVIRON_SETTING);
			}
			
			&validateASInstance("$ASADMIN_TARGET");
		}
	}
}

# Only validate if amtune-script is wraper script or ds script
if(($AMTUNE_SCRIPT_TYPE eq "all")||($AMTUNE_SCRIPT_TYPE eq "ds"))
{
	if(($LDAPSEARCH eq "")||(! -f $LDAPSEARCH))
	{
		print "ldapsearch command line not found. Cannot proceed.\n";
		print "Current ldapsearch Utility: $LDAPSEARCH\n";
   		print "You may need to customize the following file appropriately: amtune-env.pl\n";
   		exit($AMTUNE_INVALID_ENVIRON_SETTING);
   	}
  
  	$DS_HOST=&getConfigEntry("com.iplanet.am.directory.host");
  	chomp($DS_HOST);
  	print "DS HOST DS_HOST\n";
  	$DS_PORT=&getConfigEntry("com.iplanet.am.directory.port");
	chomp($DS_PORT);

  	if(($DS_HOST eq "")||($DS_PORT eq "")||($RAM_DISK eq "")||($DIRMGR_UID eq "")||($DIRMGR_PASSWORD eq ""))
  	{	
  		#FIXME: message need to be expanded for each case
  		print "Directory Server configuration invalid. Cannot proceed.\n";
   		print "You may need to customize the following file appropriately: amtune-env.pl\n";
   		exit($AMTUNE_INVALID_ENVIRON_SETTING);
   	}
}
  
# If OpenSSO installed, use debug log directory to store debug file; otherwise, 
# save it in the current directory
if(-f "$AMCONFIG_PROPERTY_FILE")
{
	$debug_dir=getConfigEntry("com.iplanet.services.debug.directory")
}
else
{
	$debug_dir=$SCRIPT_LOCATION;
}


&setLogOutput($debug_dir);  

#session parameters to be touched? Default is not to touch it
@match_true=grep(m/true/i,$AMTUNE_DONT_TOUCH_SESSION_PARAMETERS);
if($#match_true > -1)
{
	$AMTUNE_DONT_TOUCH_SESSION_PARAMETERS=true;
}
else
{
	$AMTUNE_DONT_TOUCH_SESSION_PARAMETERS=false;
}

#validate the session values
if($AMTUNE_DONT_TOUCH_SESSION_PARAMETERS eq "false")
{
	if($AMTUNE_SESSION_MAX_SESSION_TIME_IN_MTS eq "")
	{	
		$AMTUNE_SESSION_MAX_SESSION_TIME_IN_MTS=60;
	}
    
    if($AMTUNE_SESSION_MAX_IDLE_TIME_IN_MTS eq "")
    {
    	$AMTUNE_SESSION_MAX_IDLE_TIME_IN_MTS=10;
    }
    	
    if($AMTUNE_SESSION_MAX_CACHING_TIME_IN_MTS eq "")
    {
    	$AMTUNE_SESSION_MAX_CACHING_TIME_IN_MTS=2;
    }
}

@match_true=grep(m/true/i,$AMTUNE_WEB_CONTAINER_JAVA_POLICY);
if($#match_true > -1)
{
	$AMTUNE_WEB_CONTAINER_JAVA_POLICY=true;
}
else
{
	$AMTUNE_WEB_CONTAINER_JAVA_POLICY=false;
}

&echo_msg("$CHAPTER_SEP\n");
&echo_msg("$SCRIPT_BASENAME\n");
&echo_msg("$CHAPTER_SEP\n");
&echo_msg("Initializing...\n")

&echo_msg("$LINE_SEP\n");

# check system env
&echo_msg("Checking System Environment...\n");
&check_env; 


# if amtune-prepareDSTuner is running, don't need to display OpenSSO machine info
if($AMTUNE_SCRIPT_TYPE ne "ds")
{

	$JVM64bitAvailable="false";

	$result=&checkWebContainer64BitEnabled;
	if($result == 0)
	{
		$JVM64bitAvailable="true";
	}
	elsif($result == 2)
	{
		&echo_msg("The tuning parameter will be calculated based on JVM 32-bit data model.\n");
   	}
   	elsif($result == 100) 
  	{
  		exit(1);
  	}

	# Print mode and components to tune
	&echo_msg("$LINE_SEP\n");
	&echo_msg("amtune Information...\n");
	&echo_msg("$LINE_SEP");
	&echo_msg("Amtune Mode      : $AMTUNE_MODE\n");
	&echo_msg("OpenSSO   : $AMTUNE_TUNE_IDENTITY\n");
	&echo_msg("Directory        : $AMTUNE_TUNE_DS\n");
	&echo_msg("Web Container    : $AMTUNE_TUNE_WEB_CONTAINER\n");
	if ($JVM64bitAvailable eq "true")
	{
		&echo_msg("Platform         : 64-bit\n");
	}
	else
	{
		&echo_msg("Platform         : 32-bit\n");
	}
	
	&echo_msg($LINE_SEP);
	&echo_msg("Detecting System Environment...\n");
	&echo_msg($LINE_SEP);
	
	$numCPUS=&getNumberOfCPUS;
	
	&echo_msg("Number of logical CPUs in the system :  $numCPUS\n");
	
	if($numCPUS eq "")
	{
		print "Unable to obtain available CPUs. Cannot proceed.\n";
    	exit;
    }
    	
    $gcThreads=$numCPUS;
    $gcThreads =~ s/\s+//g;
    
   	$acceptorThreads=$numCPUS;
   	$acceptorThreads =~ s/\s+//g;
    	
   	&echo_msg("WS Acceptor Threads :","$acceptorThreads\n");


	$memAvail=&getSystemMemory;
	&echo_msg("Memory Available (MB) : ", "$memAvail\n");
	if($memAvail eq "")
	{
		print "Unable to obtain available memory. Cannot proceed.\n";
		exit;
	}
	
	if($AMTUNE_MAX_MEMORY_TO_USE_IN_MB_WINDOWS eq "")
	{
		$amtuneMaxMemoryToUseInMB=$AMTUNE_MAX_MEMORY_TO_USE_IN_MB_DEFAULT;
	}
	else
	{
		$amtuneMaxMemoryToUseInMB=$AMTUNE_MAX_MEMORY_TO_USE_IN_MB_WINDOWS;
	}
	$memToUse=&removeDecimals($memAvail * $AMTUNE_PCT_MEMORY_TO_USE / 100);
	if(($memToUse >= $amtuneMaxMemoryToUseInMB)&&($JVM64bitAvailable eq "false"))
	{	
		$memToUse=$amtuneMaxMemoryToUseInMB;
	}
	
	&echo_msg("Memory to Use (MB) :","$memToUse\n");

	if($memToUse eq "")
	{
		print "Unable to compute memory requirements. Cannot proceed.\n";
		exit;
	}
	
	if ($memToUse >= $AMTUNE_MIN_MEMORY_TO_USE_IN_MB)
	{
		&echo_msg("There is enough memory.\n");
	}
	else
	{
		print "There is not enough memory.\n";
		exit;
	}
	
	&echo_msg($LINE_SEP);
	&echo_msg("Calculating Tuning Parameters...\n");
	&echo_msg($LINE_SEP);
	
	$maxHeapSize=&removeDecimals($memToUse * $AMTUNE_MEM_MAX_HEAP_SIZE_RATIO);
	&echo_msg("Max heap size (MB) :" ,"$maxHeapSize\n");

	$minHeapSize=$maxHeapSize;
	&echo_msg("Min Heap size (MB) :", "$minHeapSize\n");
	
	$maxNewSize=&removeDecimals($maxHeapSize * $AMTUNE_MEM_MAX_NEW_SIZE);
	&echo_msg("Max new size (MB) :" ,"$maxNewSize\n");

	# In WS7 and AS8 or later, we removed this JVM Option
	if(($WEB_CONTAINER eq "WS61")||($WEB_CONTAINER eq "AS7"))
	{
		$maxPermSize=&removeDecimals($maxHeapSize * $AMTUNE_MEM_MAX_PERM_SIZE);
		$maxPermSize=&roundOf($maxPermSize,1);
  		&echo_msg("Max perm size (MB) :", "$maxPermSize\n");
	}
	
	$cacheSize=&removeDecimals($maxHeapSize * $AMTUNE_MEM_CACHES_SIZE);
	&echo_msg("Cache Size (MB) :", "$cacheSize\n");
	
	$sdkCacheSize=&removeDecimals($cacheSize * $AMTUNE_MEM_SDK_CACHE_SIZE);
	&echo_msg("SDK Cache Size (KB) :", "$sdkCacheSize\n");
	
	$numSDKCacheEntries=&removeDecimals($sdkCacheSize * 1024 / $AMTUNE_AVG_PER_ENTRY_CACHE_SIZE_IN_KB);
	&echo_msg("Number of SDK Cache Entries :", "$numSDKCacheEntries\n" );
		
	$sessionCacheSize=&removeDecimals($cacheSize * $AMTUNE_MEM_SESSION_CACHE_SIZE);
	&echo_msg("Session Cache Size (KB) :", "$sessionCacheSize\n" );
	
	$numSessions=&removeDecimals($sessionCacheSize * 1024 / $AMTUNE_AVG_PER_SESSION_SIZE_IN_KB);
	&echo_msg("Number of Session Cache Entries :", "$numSessions\n");
    
	if($JVM64bitAvailable eq true)
	{	
		$maxThreads=$maxHeapSize * $AMTUNE_MAX_NUM_THREADS_64_BIT;
		$maxThreads=&removeDecimals($maxThreads);
	}
	else
	{
		$maxThreads=$maxHeapSize * $AMTUNE_MAX_NUM_THREADS;
		$maxThreads=&removeDecimals($maxThreads);
	}
	
	&echo_msg("Maximum Number of Java Threads :", "$maxThreads\n" );
	
	$numRQThrottle=&removeDecimals($maxThreads * $AMTUNE_WS_RQTHROTTLE_THREADS);
	$numOfMaxThreadPool=$numRQThrottle;
	if(($WEB_CONTAINER eq "WS61")||($WEB_CONTAINER eq "AS7"))
	{
		&echo_msg("RQThrottle :", "$numRQThrottle\n" );
	}
	else 
	{
		&echo_msg("Maximum Number of Thread Pool :", "$numOfMaxThreadPool\n" );
	}

	$numLdapAuthThreads=&removeDecimals($maxThreads * $AMTUNE_IS_AUTH_LDAP_THREADS);
	&echo_msg("LDAP Auth Threads :", "$numLdapAuthThreads\n");
	
	$numSMLdapThreads=&removeDecimals($maxThreads * $AMTUNE_IS_SM_LDAP_THREADS);
	&echo_msg("SM LDAP Threads :", "$numSMLdapThreads\n");
	
	$numNotificationThreads=&removeDecimals($maxThreads * $AMTUNE_IS_NOTIFICATION_THREADS);
	&echo_msg("Notification Threads :", "$numNotificationThreads\n");
	
	$numNotificationQueue=&removeDecimals($numNotificationThreads * $AMTUNE_IS_NOTIFICATION_QUEUE_SIZE_PER_THREAD);
	#Notification Queue must be to handle a case when all sessions in the system expire at the same time
	#Hence this change
	$numNotificationQueue=$numSessions;
	&echo_msg("Notification Queue Size :", "$numNotificationQueue\n");
}

&echo_msg($PARA_SEP);

$INIT_STATUS="INIT_COMPLETE";
