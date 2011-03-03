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
# $Id: amtune-identity.pl,v 1.4 2008/08/19 19:08:35 veiming Exp $
#
#
##################################################################################
use File::Basename;
require 'amtune-utils.pl';
require 'amtune-env.pl';

$AMTUNE_AM_SCRIPT="amtune-identity.pl|am|1";

my $SCRIPT_LOCATION=dirname($0);
my $AMTUNE_SCRIPT_RECORD_STRING="$AMTUNE_AM_SCRIPT";

sub tune_AMConfig
{
	&setAMConfigPropertyFile;
	my $tune_file=$AMCONFIG_PROPERTY_FILE;
	&echo_msg($tune_file);
    
	&echo_msg("$LINE_SEP\n");
	&echo_msg("Tuning $AMCONFIG_PROPERTY_FILE...\n");
	&echo_msg("File                 : $tune_file\n");
	&echo_msg("Parameter tuning     :\n");
	&echo_msg("1.   com.iplanet.am.stats.interval\n");
	my $am_stats_interval=&getConfigEntry('com.iplanet.am.stats.interval=');
	&echo_msg("Current Value        : com.iplanet.am.stats.interval=$am_stats_interval\n");
	&echo_msg("Recommended Value    : com.iplanet.am.stats.interval=60\n");
	&echo_msg("\n");
	&echo_msg("2.   com.iplanet.services.stats.state\n");
	my $stats_state=&getConfigEntry('com.iplanet.services.stats.state=');
	&echo_msg("Current Value        : com.iplanet.services.stats.state=$stats_state\n");
	&echo_msg("Recommended Value    : com.iplanet.services.stats.state=file\n");
	&echo_msg("\n");
	&echo_msg("3.   com.iplanet.services.debug.level\n");
	my $debug_level=&getConfigEntry('com.iplanet.services.debug.level=');
	&echo_msg("Current Value        : com.iplanet.services.debug.level=$debug_level\n");
	&echo_msg("Recommended Value    : com.iplanet.services.debug.level=error\n");
	&echo_msg("\n");
	&echo_msg("4.   com.iplanet.am.sdk.cache.maxSize\n");
	my $cache_maxSize=&getConfigEntry('com.iplanet.am.sdk.cache.maxSize=');
	&echo_msg("Current Value        : com.iplanet.am.sdk.cache.maxSize=$cache_maxSize\n"); 
	&echo_msg("Recommended Value    : com.iplanet.am.sdk.cache.maxSize=$numSDKCacheEntries\n");
	&echo_msg("\n");
	&echo_msg("5.   com.iplanet.am.notification.threadpool.size\n");
	my $threadpool_size=&getConfigEntry('com.iplanet.am.notification.threadpool.size=');
	&echo_msg("Current Value        : com.iplanet.am.notification.threadpool.size=$threadpool_size\n");
	&echo_msg("Recommended Value    : com.iplanet.am.notification.threadpool.size=$numNotificationThreads\n");
	&echo_msg("\n");
	&echo_msg("6.   com.iplanet.am.notification.threadpool.threshold\n");
	my $threadpool_threshold=&getConfigEntry('com.iplanet.am.notification.threadpool.threshold=');
	&echo_msg("Current Value        : com.iplanet.am.notification.threadpool.threshold=$threadpool_threshold\n");
	&echo_msg("Recommended Value    : com.iplanet.am.notification.threadpool.threshold=$numNotificationQueue\n");
	&echo_msg("\n");
	&echo_msg("7.   com.iplanet.am.session.maxSessions\n");
	my $session_maxSessions=&getConfigEntry('com.iplanet.am.session.maxSessions=');
	&echo_msg("Current Value        : com.iplanet.am.session.maxSessions=$session_maxSessions\n");
	&echo_msg("Recommended Value    : com.iplanet.am.session.maxSessions=$numSessions\n");
	&echo_msg("\n");
	&echo_msg("8.   com.iplanet.am.session.httpSession.enabled\n");
	my $httpSession_enabled=&getConfigEntry('com.iplanet.am.session.httpSession.enabled=');
	&echo_msg("Current Value        : com.iplanet.am.session.httpSession.enabled=$httpSession_enabled\n");
	&echo_msg("Recommended Value    : com.iplanet.am.session.httpSession.enabled=false\n");
	&echo_msg("\n");
	&echo_msg("9.   com.iplanet.am.session.purgedelay\n");
	my $session_purgedelay=&getConfigEntry('com.iplanet.am.session.purgedelay=');
	&echo_msg("Current Value        : com.iplanet.am.session.purgedelay=$session_purgedelay\n");
	&echo_msg("Recommended Value    : com.iplanet.am.session.purgedelay=1\n");
	&echo_msg("\n");
	&echo_msg("10.  com.iplanet.am.session.invalidsessionmaxtime\n");
	my $session_invalidsessionmaxtime=&getConfigEntry('com.iplanet.am.session.invalidsessionmaxtime=');
	&echo_msg("Current Value        : com.iplanet.am.session.invalidsessionmaxtime=$session_invalidsessionmaxtime\n");
	&echo_msg("Recommended Value    : com.iplanet.am.session.invalidsessionmaxtime=1\n");
	&echo_msg("\n\n");
   
   	if($AMTUNE_MODE eq REVIEW )
   	{
   		return;
   	}
   	
   	&check_file_for_write($tune_file);
   	if( $? == 100)
   	{
   		return;
   	}
   	
   	&echo_msg("Tuning $tune_file\n");
   	
   	$time=localtime();
   	$perf_tune_start_str = "#Line modified for Performance Tuning by amtune = $time";
   	 	
   	&delete_line($tune_file,"#Line modified for Performance Tuning by amtune");
	
	&insert_line($tune_file,"com.iplanet.am.stats.interval=","$perf_tune_start_str\n");
	&replace_line($tune_file,"com.iplanet.am.stats.interval=","com.iplanet.am.stats.interval=60\n");
	
	&insert_line($tune_file,"com.iplanet.services.stats.state=","$perf_tune_start_str\n");
	&replace_line($tune_file,"com.iplanet.services.stats.state=","com.iplanet.services.stats.state=file\n");
	
	&insert_line($tune_file,"com.iplanet.services.debug.level=","$perf_tune_start_str\n");
	&replace_line($tune_file,"com.iplanet.services.debug.level=","com.iplanet.services.debug.level=error\n");
	
	&insert_line($tune_file,"com.iplanet.am.sdk.cache.maxSize=","$perf_tune_start_str\n");
	&replace_line($tune_file,"com.iplanet.am.sdk.cache.maxSize=","com.iplanet.am.sdk.cache.maxSize = $numSDKCacheEntries \n");
	
	&insert_line($tune_file,"com.iplanet.am.notification.threadpool.size=","$perf_tune_start_str\n");
	&replace_line($tune_file,"com.iplanet.am.notification.threadpool.size=","com.iplanet.am.notification.threadpool.size = $numNotificationThreads \n");
	
	&insert_line($tune_file,"com.iplanet.am.notification.threadpool.threshold=","$perf_tune_start_str\n");
	&replace_line($tune_file,"com.iplanet.am.notification.threadpool.threshold=","com.iplanet.am.notification.threadpool.threshold = $numNotificationQueue \n");
	
	&insert_line($tune_file,"com.iplanet.am.session.maxSessions=","$perf_tune_start_str\n");
	&replace_line($tune_file,"com.iplanet.am.session.maxSessions=","com.iplanet.am.session.maxSessions = $numSessions \n");
	
	&insert_line($tune_file,"com.iplanet.am.session.httpSession.enabled=","$perf_tune_start_str\n");
	&replace_line($tune_file,"com.iplanet.am.session.httpSession.enabled=","com.iplanet.am.session.httpSession.enabled=false\n");
	
	&insert_line($tune_file,"com.iplanet.am.session.purgedelay=","$perf_tune_start_str\n");
	&replace_line($tune_file,"com.iplanet.am.session.purgedelay=","com.iplanet.am.session.purgedelay=1\n");
	
	&insert_line($tune_file,"com.iplanet.am.session.invalidsessionmaxtime=","$perf_tune_start_str\n");
	&replace_line($tune_file,"com.iplanet.am.session.invalidsessionmaxtime=","com.iplanet.am.session.invalidsessionmaxtime=1\n");
	
	&echo_msg("Done\n");
}

sub tune_ServerConfig
{
	&setServerConfigXMLFile;
	my $tune_file=$SERVERCONFIG_XML_FILE;
	
	&echo_msg($LINE_SEP);
	&echo_msg("Tuning $tune_file...\n");	
	&echo_msg("File                 : $tune_file\n");
	&echo_msg("Recomended tuning parameters only. These paramters will not be tuned by the script.\n");
	&echo_msg("You need to modify them manually in $tune_file. \n");
	&echo_msg("The number should depend on number of OpenSSO instances and the memory of \n");
	&echo_msg("Directory Server.  Please refer to OpenSSO Performance Tuning Guide.\n");
    &echo_msg("\n");
	&echo_msg("1.   minConnPool\n");
	my $minConnPool_variable=get_token_in_file($tune_file,"minConnPool");
	&echo_msg("Current Value        : minConnPool=$minConnPool_variable\n");
	&echo_msg("Recommended Value    : minConnPool=1\n");
	&echo_msg("\n");
	&echo_msg("2.   maxConnPool\n");
	my $maxConnPool_variable=get_token_in_file($tune_file,"maxConnPool");
	&echo_msg("Current Value        : maxConnPool=$maxConnPool_variable\n");
	&echo_msg("Recommended Value    : maxConnPool=$numSMLdapThreads\n");
	&echo_msg("\n\n");
}
   	

sub uploadAmadminData
{
	($xml_file)=@_;
	
	&echo_msg("\n");
	
	$adminUser=getConfigValue("com.sun.identity.authentication.super.user");
	#if( $FILE_BASED_PASSWD eq true)
	#{
		$ADMIN_OPTION_RUN="--runasdn";
		$ADMIN_USER=$adminUser;
		$ADMIN_PASS="--password";
		$ADMIN_VERBOSE="--verbose";
		$ADMIN_NOLOG="--nolog";
		$ADMIN_DATA="--data";
		$ADMIN_XML="$xml_file";
		&echo_msg("Executing command\n");
		@args=($ADMIN_CLIENT,$ADMIN_OPTION_RUN,$adminUser,$ADMIN_PASS,$ADMIN_PASSWORD,$ADMIN_VERBOSE,$ADMIN_NOLOG,$ADMIN_DATA,$xml_file);
		system("@args")==0 or die "\nError executing command\n";
		#`$ADMIN_CLIENT --runasdn $adminUser --password $ADMIN_PASSWORD --verbose --nolog --data $xml_file`;
		
	#}
   	#else
   	#{
   	#	`$ADMIN_CLIENT --runasdn $adminUser -f "$FILE_ADMIN_PASSWORD" --verbose --nolog --data ${xml_file}`;
   	#}
   	
   	print "\n";
}

sub tune_LDAPConnPool
{
	my $temp=$ENV{TEMP};
	my $FILE="$temp/dsame-auth-core-tune.xml";
		
	open(FP,">$FILE");
	print FP '<?xml version="1.0" encoding="UTF-8"?>'."\n";
	print FP '<!DOCTYPE Requests PUBLIC "-//iPlanet//iDSAME 5.1 Admin CLI DTD//EN" "jar://com/iplanet/am/admin/cli/amAdmin.dtd">'."\n";
	print FP '<!--  MODIFY REQUESTS -->'."\n";
	print FP '<Requests>'."\n";
	print FP '<SchemaRequests serviceName="iPlanetAMAuthService" SchemaType="global">'."\n";
	print FP '<ModifyDefaultValues>'."\n";
	print FP '<AttributeValuePair>'."\n";
	print FP '<Attribute name="iplanet-am-auth-ldap-connection-pool-default-size "/>'."\n";
	print FP "<Value>$numLdapAuthThreads:$numLdapAuthThreads</Value>"."\n";
	print FP '</AttributeValuePair>'."\n";
	print FP '</ModifyDefaultValues>'."\n";
	print FP '</SchemaRequests>'."\n";
	print FP '</Requests>'."\n";
	close(FP);
	
	open(FP,$FILE);
	while(<FP>)
	{
		print $_;
	}
	close(FP);
	
	&echo_msg($LINE_SEP);
	&echo_msg("Tuning LDAP Connection Pool in Global iPlanetAMAuthService...\n");
	&echo_msg("Service              : iPlanetAMAuthService\n");
	&echo_msg("SchemaType           : global\n");
	&echo_msg("Recomended tuning parameters only. These paramters will not be tuned by the script.\n");
	&echo_msg("If you want to tune these parameters, review data file $FILE\n");
	&echo_msg("and run it with amadmin command.  The number should depend on number of OpenSSO\n"); 
	&echo_msg("instances and the memory of Directory Server.  Please refer to OpenSSO\n");
    &echo_msg("Performance Tuning Guide.\n");
	
	&echo_msg("1.   iplanet-am-auth-ldap-connection-pool-default-size\n");
#	&echo_msg("Current Value        : iplanet-am-auth-ldap-connection-pool-default-size=\n");
	&echo_msg("Recommended Value    : iplanet-am-auth-ldap-connection-pool-default-size=$numLdapAuthThreads:$numLdapAuthThreads\n");
		
}

sub tune_LDAPSearchCriteriaForDefaultOrg
{
	$defaultOrg=getConfigEntry("com.iplanet.am.default");
	
	&echo_msg($LINE_SEP);
	&echo_msg("Tuning LDAP Search Criteria in iPlanetAMAuthLDAPService For Default Org: $defaultOrg...\n");
	&echo_msg("Service              : iPlanetAMAuthLDAPService for Org $defaultOrg\n");
	&echo_msg("SchemaType           : organization\n");
	&echo_msg("Parameter tuning     :\n");
	&echo_msg("1.   iplanet-am-auth-ldap-base-dn\n");
#	&echo_msg("Current Value        : iplanet-am-auth-ldap-base-dn=\n");
	&echo_msg("Recommended Value    : iplanet-am-auth-ldap-base-dn=${DEFAULT_ORG_PEOPLE_CONTAINER},${defaultOrg}\n");
	&echo_msg("2.   iplanet-am-auth-ldap-search-scope\n");
# 	&echo_msg("Current Value        : iplanet-am-auth-ldap-search-scope=\n");
	&echo_msg("Recommended Value    : iplanet-am-auth-ldap-search-scope=OBJECT\n");
	
	if ( $AMTUNE_MODE eq "REVIEW")
	{
		return;
	}
		
	my $temp=$ENV{TEMP};
	my $FILE="$temp/dsame-auth-ldap-tune.xml";
	open(FP,">>$FILE");
	print FP '<?xml version="1.0" encoding="UTF-8"?>'."\n";
	print FP '<!DOCTYPE Requests PUBLIC "-//iPlanet//iDSAME 5.1 Admin CLI DTD//EN" "jar://com/iplanet/am/admin/cli/amAdmin.dtd">'."\n";
	print FP '<!--  MODIFY REQUESTS -->'."\n";
	print FP '<Requests>'."\n";
	print FP "<OrganizationRequests DN=\"$defaultOrg\">"."\n";
	print FP '<ModifyServiceTemplate serviceName="iPlanetAMAuthLDAPService" schemaType="organization">'."\n";
	print FP '<AttributeValuePair>'."\n";
	print FP '<Attribute name="iplanet-am-auth-ldap-base-dn"/>'."\n";
	print FP "<Value>${DEFAULT_ORG_PEOPLE_CONTAINER},${defaultOrg}</Value>"."\n";
	print FP '</AttributeValuePair>'."\n";
	print FP '<AttributeValuePair>'."\n";
	print FP '<Attribute name="iplanet-am-auth-ldap-search-scope"/>'."\n";
	print FP '<Value>OBJECT</Value>'."\n";
	print FP '</AttributeValuePair>'."\n";
	print FP '</ModifyServiceTemplate>'."\n";
	print FP '</OrganizationRequests>'."\n";
	print FP '</Requests>'."\n";
	close(FP);
	
	open(FP,$FILE);
	while(<FP>)
	{
		print $_;
	}
	close(FP);
	
	uploadAmadminData($FILE);
	unlink($FILE);
}
	

sub tune_SessionTimeouts
{
	&echo_msg($LINE_SEP);
	&echo_msg("Tuning Session Timeouts in Global iPlanetAMSessionService...\n");
	&echo_msg("Service              : iPlanetAMSessionService\n");
	&echo_msg("SchemaType           : Dynamic\n");
	&echo_msg("Parameter tuning     :\n");
	&echo_msg("1.   iplanet-am-session-max-session-time\n");
#	&echo_msg("Current Value        : iplanet-am-session-max-session-time=\n");
	&echo_msg("Recommended Value    : iplanet-am-session-max-session-time=$AMTUNE_SESSION_MAX_SESSION_TIME_IN_MTS\n");
	&echo_msg("2.   iplanet-am-session-max-idle-time\n");
#	&echo_msg("Current Value        : iplanet-am-session-max-idle-time=\n");
	&echo_msg("Recommended Value    : iplanet-am-session-max-idle-time=$AMTUNE_SESSION_MAX_IDLE_TIME_IN_MTS\n");
	&echo_msg("3.   iplanet-am-session-max-caching-time\n");
#	&echo_msg("Current Value        : iplanet-am-session-max-caching-time=\n");
	&echo_msg("Recommended Value    : iplanet-am-session-max-caching-time=$AMTUNE_SESSION_MAX_CACHING_TIME_IN_MTS\n");
	
	if($AMTUNE_MODE eq "REVIEW")
	{
		return 0;
	}
	
	$Temp=$ENV{TEMP};
	$FILE="$Temp/dsame-session-timeout-tune.xml";
	open(FP,">$FILE");
	print FP '<?xml version="1.0" encoding="UTF-8"?>'."\n";
	print FP '<!DOCTYPE Requests PUBLIC "-//iPlanet//iDSAME 5.1 Admin CLI DTD//EN" "jar://com/iplanet/am/admin/cli/amAdmin.dtd">'."\n";
	print FP '<!--  MODIFY REQUESTS -->'."\n";
	print FP '<Requests>'."\n";
	print FP '<SchemaRequests serviceName="iPlanetAMSessionService" SchemaType="Dynamic">'."\n";
	print FP '<ModifyDefaultValues>'."\n";
	print FP '<AttributeValuePair>'."\n";
	print FP '<Attribute name="iplanet-am-session-max-session-time"/>'."\n";
	print FP "<Value>$AMTUNE_SESSION_MAX_SESSION_TIME_IN_MTS</Value>"."\n";
	print FP '</AttributeValuePair>'."\n";
	print FP '<AttributeValuePair>'."\n";
	print FP '<Attribute name="iplanet-am-session-max-idle-time"/>'."\n";
	print FP "<Value>$AMTUNE_SESSION_MAX_IDLE_TIME_IN_MTS</Value>"."\n";
	print FP '</AttributeValuePair>'."\n";
	print FP '<AttributeValuePair>'."\n";
	print FP '<Attribute name="iplanet-am-session-max-caching-time"/>'."\n";
	print FP "<Value>$AMTUNE_SESSION_MAX_CACHING_TIME_IN_MTS</Value>"."\n";
	print FP '</AttributeValuePair>'."\n";
	print FP '</ModifyDefaultValues>'."\n";
	print FP '</SchemaRequests>'."\n";
	print FP '</Requests>'."\n";
	
	open(FP,$FILE);
	while(<FP>)
	{
		print $_;
	}
	close(FP);
	
	uploadAmadminData($FILE);
	unlink($FILE);
}

sub get_token_in_file
{
	
	($file,$match)=@_;
	
	if($match eq "")
	{
		return;
	}
	
	open(FP,$file);
	@filecontent=<FP>;
	close(FP);
	
	my @temp;
	my @temp3;
	@match=grep(m/$match/,@filecontent);
	
	foreach $i(@match)
	{
		@split_arr=split(m/\s+/,$i);
		push(@temp,@split_arr);
	}
	
	@temp1=grep(m/$match/,@temp);
	
	foreach $i(@temp1)
	{
		@temp2=split(m/=/,$i);
		push(@temp3,$temp2[1]);
	}
	
	foreach $i(@temp3)
	{
		@match=split(m/"/,$i);
		return $match[1];
	}
	return;
}

sub getConfigValue
{
	# Don't have to check for existence of AMConfig. Its already being done in amtune-env.pl
	($property)=@_;
	if($property eq "")
	{
		return;
	}
	
	open(FP,$AMCONFIG_PROPERTY_FILE);
	@filecontent=<FP>;
	close(FP);
	@propStr=grep(m/$property/,@filecontent);
	if($#propStr == -1)
	{
		return;
	}
	
	foreach $i(@propStr)
	{
		@array=split(m/\=/,$i,2);
		return $array[1]."\n";
	}
}


#############################################################################
# Start of main program
#############################################################################

#import the envionment
if(-f "$SCRIPT_LOCATION/amtune-env.pl")
{
	if($INIT_STATUS ne INIT_COMPLETE)
	{	
		my $PERL_CMD=perl;
		$SCRIPT="$SCRIPT_LOCATION\amtune-env.pl";
		my @args=($PERL_CMD,$SCRIPT);
		system(@args)==0 or die "\nError executing command\n";
		#`perl $SCRIPT_LOCATION\amtune-env.pl`;
		
	}
}

&echo_msg("OpenSSO - OpenSSO Server Tuning Script\n");

tune_AMConfig;
tune_ServerConfig;
tune_LDAPConnPool;

#If default org's people container is specified, then perform this tune
if( $DEFAULT_ORG_PEOPLE_CONTAINER ne "")
{
	tune_LDAPSearchCriteriaForDefaultOrg;
}

if( $AMTUNE_DONT_TOUCH_SESSION_PARAMETERS eq false)
{
	tune_SessionTimeouts;
}

echo_msg("$PARA_SEP\n");

