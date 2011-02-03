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
# $Id: amtune-prepareDSTuner.pl,v 1.4 2008/08/19 19:08:36 veiming Exp $
#
#
###########################################################################
use File::Copy;
use File::Basename;

$AMTUNE_DS_SCRIPT="amtune-prepareDSTuner.pl|ds|1";
$AMTUNE_SCRIPT_RECORD_STRING=$AMTUNE_DS_SCRIPT;

require "amtune-env.pl";
require "amtune-utils.pl";

$SCRIPT_LOCATION=dirname($0);


sub getListDefaultAMIndexes
{
	$index_ldif_file="$IS_CONFIG_DIR/ldif/index.ldif";
	if(! -f $index_ldif_file)
	{
		$index_ldif_file="$IS_CONFIG_DIR/index.ldif";
		if(! -f "$index_ldif_file")
		{
			print "\n";
			exit(1);
		}
	}
	
	open(FP,$index_ldif_file);
	@file=<FP>;
	close(FP);
	
	@grepdn=grep(m/dn:\s/,@file);
	@arrayspace=();
	foreach $i(@grepdn)
	{
		@arrayspace1=split(m/\s+/,$i);
		push(@arrayspace,$arrayspace1[1]);
	}
	
	@arraycomma=();
	foreach $i(@arrayspace)
	{
		@arraycomma1=split(m/,/,$i);
		push(@arraycomma,$arraycomma1[0]);
	}
	@arrayequal=();
	foreach $i(@arraycomma)
	{
		@arrayequal1=split(m/=/,$i);
		push(@arrayequal,$arrayequal1[1]);
	}
	return @arrayequal;
}
	


sub prepareDirectoryTuner
{
	&echo_msg("Preparing Directory Server Tuner...\n");
	&echo_msg("$LINE_SEP\n");
	&echo_msg("\n");
	&echo_msg("Determining Current Settings...\n");
	
	$configEntry=&getConfigEntry("com.iplanet.am.rootsuffix");
	$ROOT_SUFFIX=$configEntry;
	chomp($ROOT_SUFFIX);
	$instanceDir=&getInstanceDir;
	$DS_INSTANCE_DIR=$instanceDir;
	chomp($DS_INSTANCE_DIR);
	
	$temp=$ENV{TEMP};
	$tmp_file="$temp/amtune\-directory.pl";	
	
	$dsversion=&getDSVersion;
	chomp($dsversion);
	&echo_msg("The Directory Server version is $dsversion\n");
	
	
	$ds_version=$dsversion;
	#chomp($ds_version);
	
	@list_default_am_indexes=&getListDefaultAMIndexes;
	if($#list_default_am_indexes  == -1)
	{
		&echo_msg( "WARNING: Cann't find OpenSSO Index file  $IS_CONFIG_DIR/ldif/index.ldif.  Use default list.\n");
		@list_default_am_indexes=("nsRoleDN","memberof","iplanet-am-static-group-dn","iplanet-am-modifiable-by","iplanet-am-user-federation-info-key","sunxmlkeyvalue","o","ou","sunPreferredDomain","associatedDomain","sunOrganizationAlias");
	}
	
	$temp=$ENV{TEMP};
	copy($AMTUNE_UTILS_SCRIPT,"$temp/amtune\-utils.pl");
	copy("$SCRIPT_LOCATION/amtune-directory.template","$tmp_file");
	
	&replace_line($tmp_file,"DS_HOST=","DS_HOST=\"$DS_HOST\"\;");
	&replace_line($tmp_file,"DS_PORT=","DS_PORT=$DS_PORT\;");
	&replace_line($tmp_file,"ROOT_SUFFIX=","ROOT_SUFFIX=\"$ROOT_SUFFIX\"\;");
	&replace_line($tmp_file,"DIRMGR_UID=","DIRMGR_UID=$DIRMGR_UID\;");
	&replace_line($tmp_file,"DS_INSTANCE_DIR=","DS_INSTANCE_DIR=\"$DS_INSTANCE_DIR\"\;");
	&replace_line($tmp_file,"DS_VERSION=","DS_VERSION=\"$ds_version\"\;");
	#Convert string to array for token replacement
	$req_string="list_am_default_index=\(";
	for($i=0;$i<$#list_default_am_indexes;$i++)
	{
		
		$req_string=$req_string."\'$list_default_am_indexes[$i]\'\,";
	}
	
	$req_string=$req_string."\'$list_default_am_indexes[$#list_default_am_indexes]\'\)\;";
	&replace_line($tmp_file,"list_am_default_index=",$req_string);
	
	
	&echo_msg("\nDirectory Server Tuner tar file : $ds_tar_file\n");
	&echo_msg("Steps to tune directory server\n");
	&echo_msg("1.  Copy the DS Tuner files($tmp_file and $temp/amtume\-utils.pl) to the DS System\n");
	&echo_msg("2.  Execute the following script in 'REVIEW' mode : amtune-directory.pl\n");
	&echo_msg("3.  Review carefully the recommended tunings for DS\n");
	&echo_msg("4.  If you are sure of applying these changes to DS, modify the following lines in amtune-directory.pl\n");
	&echo_msg("    a.  AMTUNE_MODE\n");
	&echo_msg("    These parameters can also be modified or left unchanged to use default values\n");
	&echo_msg("    b.  AMTUNE_LOG_LEVEL=\n");
	&echo_msg("    c.  AMTUNE_DEBUG_FILE_PREFIX=\n");
	&echo_msg("    d.  DB_BACKUP_DIR_PREFIX=\n");
	
	@version=split(/\./,$ds_version);
	$total_numbers=$#version;
	&echo_msg( "$version[$total_numbers]");
	if($version[$total_numbers] < 5)
	{
		&echo_msg( "Its highly recommended to run db2bak before running amtune-directory.pl\n");
	}
	else 
	{
		&echo_msg( " Its highly recommended to run dsadm backup before running amtune-directory.pl\n");
	}
}

#############################################################################
# Start of main program
#############################################################################

#import the environment 
if( -f "$SCRIPT_LOCATION/amtune-env.pl")
{
	if($INIT_STATUS ne INIT_COMPLETE)
	{
		$PERL_CMD="perl";
		$SCRIPT="$SCRIPT_LOCATION\amtune-env.pl";
		@args=("$PERL_CMD","$SCRIPT");
		system(@args)==0 or die "\nError executing command\n";
		#`perl $SCRIPT_LOCATION\amtune-env.pl`;
	}
}

&echo_msg( "OpenSSO - Directory Server Tuner Preparation Script \n"); 

$DS_TAR_DIR=$SCRIPT_LOCATION;

&prepareDirectoryTuner;

&echo_msg($PARA_SEP);

	
