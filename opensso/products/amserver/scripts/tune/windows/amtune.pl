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
# $Id: amtune.pl,v 1.4 2008/08/19 19:08:36 veiming Exp $
#
#
#############################################################################
# Start of main program
#############################################################################
use File::Basename;

require "amtune-utils.pl";

my $SCRIPT_LOCATION=basename($0);
my $AMTUNE_SCRIPT_RECORD_STRING="AMTUNE_MAIN_SCRIPT";
my $AMTUNE_BIN_DIR;

$PERL_CMD="perl";


#import the environment
if(-f "$SCRIPT_LOCATION\amtune-env.pl")
{
	if($INIT_STATUS ne INIT_COMPLETE)
	{
		my $SCRIPT="$SCRIPT_LOCATION\amtune-env.pl";
		@args=($PERL_CMD,$SCRIPT);
		system(@args)==0 or die "\n Error executing command @args\n";
		#`perl "$SCRIPT_LOCATION\amtune-env.pl";
	}
}

print "OpenSSO Tuning Script\n";
print "$LINE_SEP"."\n";;


#Tune Web Server/Application Server
@webappserver=grep(m/$AMTUNE_TUNE_WEB_CONTAINER=true/,@filecontent);
if($#webappserver>-1)
{
	&webContainerToTune;
	@wc_config=grep(m/$WC_CONFIG=\s*/,@filecontent);
	if(@wc_config>-1)
	{	
		if(-f $AMTUNE_BIN_DIR\$WC_CONFIG)
		{	
			my $SCRIPT="$AMTUNE_BIN_DIR/$WC_CONFIG";
			@args=($PERL_CMD,$SCRIPT);
			system(@args)==0 or die "\n Error executing command @args\n";
			#`perl $AMTUNE_BIN_DIR/$WC_CONFIG`;
			if($? != 0)
			{	
				print "WARNING:$WC_CONFIG is not running. Script is not found in $AMTUNE_BIN_DIR\n";
			}
		}
	}
}


#Tune Directory Server
@amtune_tune_ds=grep(m/AMTUNE_TUNE_DS=true/,@filecontent);
if($#amtune_tune_ds>-1)
{	
	if(-f "$AMTUNE_BIN_DIR\amtune-prepareDSTuner.pl")
	{
		my $SCRIPT="$AMTUNE_BIN_DIR\amtune-prepareDSTuner.pl";
		@args=($PERL_CMD,$SCRIPT);
		system(@args)==0 or die "\n Error executing command @args\n";
		#`perl $AMTUNE_BIN_DIR\amtune-prepareDSTuner.pl`;
		if($?!=0)
		{
			print "WARNING:amtune-prepareDSTuner.pl is not running. Script is not found in $AMTUNE_BIN_DIR\n";
		}
	}
}


#Tune OpenSSO
@amtune_tune_identity=grep(m/$AMTUNE_TUNE_IDENTITY=true/,@filecontent);
if($#amtune_tune_identity>-1)
{
	if(-f "$AMTUNE_BIN_DIR\amtune-identity.pl")
	{
		my $SCRIPT="$AMTUNE_BIN_IDENTITY\amtune-identity.pl";
		@args=($PERL_CMD,$SCRIPT);
		system(@args)==0 or die "\n Error executing command @args\n";
		#`perl $AMTUNE_BIN_IDENTITY\amtune-identity.pl`;
		if($?!=0)
		{
			print "WARNING:amtune-identity.pl is not running. Script is not found in $AMTUNE_BIN_DIR\n";
		}
	}
}

print "Tuning Complete\n":
print "$CHAPTER_SEP\n";
