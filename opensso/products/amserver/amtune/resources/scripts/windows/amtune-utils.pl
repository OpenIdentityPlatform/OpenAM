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
# $Id: amtune-utils.pl,v 1.2 2008/06/25 05:41:19 qcheng Exp $
#
#
###################################################################################
use File::Copy;
use Win32::Registry;
use File::Basename;
use Win32::OLE qw(in);

my $DIR=""; 

$LINE_SEP="---------------------------------------------------------------------\n";
$PARA_SEP="=====================================================================\n";
$CHAPTER_SEP="#####################################################################\n";
#$temp=$ENV{TEMP};
#$OUTPUT_FILE="$temp/output\.txt";

################################################################################
#===============================================================================
# amtune Functions
#===============================================================================

#-------------------------------------------------------------------------------
# Function      :   getSystemMemory
# Parameters    :   -None-
# Output        :   Returns memory (RAM) available in MB in system
# Description   :   This function returns memory available in Megabytes 
#-------------------------------------------------------------------------------


sub getSystemMemory
{
	$Win32::OLE::Warn = 3;
	use constant vbTab => "\x09";
	# ------ SCRIPT CONFIGURATION ------
	$strComputer = '.';
	# ------ END CONFIGURATION ---------
	$objWMI = Win32::OLE->GetObject('winmgmts:\\\\' . $strComputer . '\\root\\cimv2');
	$colOS = $objWMI->InstancesOf('Win32_OperatingSystem');
	foreach my $objOS (in $colOS) 
	{
		$mem_in_MB=$objOS->TotalVisibleMemorySize/1024;
		$mem_in_MB=removeDecimals($mem_in_MB);
	}
	return $mem_in_MB;
}

#-------------------------------------------------------------------------------
# Function      :   getNumberOfCPUS
# Parameters    :   -None-
# Output        :   Returns number of "on-line" CPUs in a ystem   
# Description   :   This function returns the number of on-line CPUs 
#-------------------------------------------------------------------------------


sub getNumberOfCPUS
{
	my $MIN_NUM_CPU=1;
	my $MAX_NUM_CPU=4;
	my $num_cpu;
	
	my $Register = "SYSTEM\\CurrentControlSet\\Control\\Session Manager\\Environment";
	my ($hkey,@key_list,$key,%values);
	$HKEY_LOCAL_MACHINE->Open($Register,$hkey)|| die $!;
	$hkey->GetValues(\%values);
	foreach $value (keys(%values))
	{
		if($value eq "NUMBER_OF_PROCESSORS")
		{
			$key=$values{$value}->[0];
			$val2=$values{$value}->[2];
			$num_cpu=$val2;
		}
	}
	$hkey->Close();
	return $num_cpu;
}

#-------------------------------------------------------------------------------
# Function      :   removeDecimals
# Parameters    :   1. number input (most likely with decimals)
# Output        :   Returns a whole number stripping out decimals
# Description   :   This function returns a whole number stripping out decimals
#                   from the input number. The call does not round the input 
#                   values
#-------------------------------------------------------------------------------


sub removeDecimals
{
	($value)=@_;
	@array=split(/\./,$value);
	$z=$array[0];
	return $z;
}

#-------------------------------------------------------------------------------
# Function      :   roundOf
# Parameters    :   1. number input (with or without decimals)
#                   2. roundoff - How many digits from the right to round of
# Output        :   Returns a rounded off number 
# Description   :   This function returns a rounded off number derived from
#                   the input number. 
#                   - Round off works on both numbers with decimals
#                   and numbers without decimals. 
#                   - Rounds off numbers to the position passed in as the second
#                   parameter
#                   - If you would like to round off only the left of the decimal,
#                   then you will need to call removeDecimals first and then call
#                   roundOf.
#-------------------------------------------------------------------------------

sub roundOf
{
	($value,$scale)=@_;
	@number=split(m/\./,$value);
	$value=$number[0];
	if(length($value)>$scale)
	{
		$value =~ s/\d{$scale}$//;
		for($i=0;$i<$scale;$i++)
		{
			$value=$value*10;
		}
	}
	else
	{
		print "Cannot calculate\n";
	}
	return $value;
}

#-------------------------------------------------------------------------------
# Function      :   ECHO_MESG
# Parameters    :   Message
# Output        :   Display the output on terminal and/or debug log file
#-------------------------------------------------------------------------------
sub echo_msg
{
	($output_msg_1,$output_msg_2)=@_;
	$output_msg="$output_msg_1"."$output_msg_2";
		
	print $output_msg;
		
		
	open(FP1,">>$OUTPUT_FILE");
	print FP1 $output_msg;
	print FP1 "\n";
	close(FP1);
}

#-------------------------------------------------------------------------------
# Function      :   setLogOutput
# Parameters    :   Log directory
# Output        :   -None-
# Description   :   Determinate the device type to display the output.
#-------------------------------------------------------------------------------

sub setLogOutput
{
	($log_dir)=@_;
	
	$OUTPUT_FILE="output.log";
	if($AMTUNE_LOG_LEVEL eq NONE)
	{
		$OUTPUT_FILE="debug.log";
		print "AMTUNE_MODE=$AMTUNE_MODE and AMTUNE_LOG_LEVEL=$AMTUNE_LOG_LEVEL\n";
		if($AMTUNE_MODE eq REVIEW)
		{
			print "Please modify AMTUNE_LOG_LEVEL to display the output in REVIEW mode. Cannot proceed.\n";
			exit 1;
		}
		else 
		{
			print " No output will be displayed.\n";
		}
	}
	elsif($AMTUNE_LOG_LEVEL eq TERM)
	{
		$OUTPUT_FILE="debug.log";
	}
	elsif($AMTUNE_LOG_LEVEL eq FILE)
	{
		$DEBUG_FILE="debug.log";
		$g=localtime(); @array=split(/\s/,$g);
		$OUTPUT_FILE="$AMTUNE_DEBUG_FILE_PREFIX-$array[4]-$array[1]-$array[2].log";
		if( $log_dir eq "")
		{
			if(-d $log_dir)
			{
				$g=localtime(); @array=split(/\s+/,$g);
				$OUTPUT_FILE="$log_dir/$AMTUNE_DEBUG_FILE_PREFIX-$array[4]-$array[1]-$array[2].log";
			}
		}
		
		#Make sure that you write to debug file
		
		open(FP,">$OUTPUT_FILE");
		if($? != 0)
		{
			print "Cannot create amtune debug file.Cannot proceed.\n";
			print "Debug file location : $OUTPUT_FILE\n";
			exit($AMTUNE_INVALID_ENVIRON_SETTING); 
		}
		
		print "Debug information log can be found in file: $OUTPUT_FILE\n";
	}
	else
	{
		print "ERROR: Invalid AMTUNE_LOG_LEVEL: $AMTUNE_LOG_LEVEL. Cannot proceed.\n";
	}
}

#-------------------------------------------------------------------------------
# Function      :   checkWebContainer64BitEnabled
# Parameters    :   -None-
# Output        :   1 if web container is running with 64-bit JVM
#		    		0 if web container is running with 32-bit JVM
#		    		2 if can't be determined
# Description   :   Check and return the status if web container is running with 64-bit JVM or not
#-------------------------------------------------------------------------------

sub checkWebContainer64BitEnabled
{
	if( ! -f $WSADMIN_PASSFILE)
	{
		print " File not found\n";
		open(FP,">$WSADMIN_PASSFILE");
		print FP "$WSADMIN_PASSWORD_SYNTAX $WSADMIN_PASSWORD\n";
		close(FP);
		open(FP,"$WSADMIN_PASSFILE");
		while(<FP>)
		{
			print $_;
		}
		close(FP);
	}
	if (($WEB_CONTAINER eq WS7) || ($WEB_CONTAINER eq ""))
	{
		if( ! -f $WSADMIN)
		{
			$rc=2;
		}
		else
		{
			#open(FP,">wsadmin.tmp");
			$WSADMIN_OPTION="get-config-prop";
			$WSADMIN_OPTION1="platform";
			@arg=($WSADMIN,$WSADMIN_OPTION,@WSADMIN_COMMON_PARAMS,$WSADMIN_OPTION1,">wsadmin.tmp");
			system("@arg")==0 or die "\nError executing command @arg\n";
			open(FP,"wsadmin.tmp");
			@array=<FP>;
			close(FP);
			@data=grep(m/64/,@array);
			if($#data == 0)
			{
				$rc=0;
			}
			else
			{
				$rc=1;
			}
		}
		unlink("wsadmin.tmp");
	}
	else
	{
		$rc=1;
	}
	return $rc;
}

#-------------------------------------------------------------------------------
# Function      :   displayJVM64bitMessage
# Parameters    :   -None-
# Output        :   -None-
# Description   :   Display the message to recommend the user to customize the parameter
#                   in amtune-env for JVM 64-bit.
#-------------------------------------------------------------------------------

sub displayJVM64bitMessage
{
	($calculateMaxHeapSize,$calculateMinHeapSize)=@_;
	print "Recommended Value    : The Web Container's JVM on your system supports 64-bit data model with \n";
	print "                     : ${memToUse} MB memory available to use.\n";
	print "                     : We recommend you modify a parameter, AMTUNE_MEM_MAX_HEAP_SIZE_RATIO,\n";
	print "                     : in amtune-env that is used to calculate the Max Heap and Min Heap sizes.\n";
	print "                     : Please make sure this ratio is not set too high if the available physical memory\n";
	print "                     : is very large - i.e., if the available physical memory is a big multiple of 32-bit\n";
	print "                     : JVM memory limit, 4 GB.\n";
	print "                     : The current setting for Max Heap size ratio is:\n";
	print "                     : 	AMTUNE_MEM_MAX_HEAP_SIZE_RATIO=$AMTUNE_MEM_MAX_HEAP_SIZE_RATIO\n";
	# Currently Max Heap Size and Min Heap Size are the same so we don't use a parameter below
	# print "                   : 	AMTUNE_MEM_MIN_HEAP_SIZE_RATIO=$AMTUNE_MEM_MIN_HEAP_SIZE_RATIO\n";
	print "                     : The current JVM Max Heap and Min Heap sizes calculated from the above ratio are:\n";
    print "                     :	Min Heap: ${calculatedMaxHeapSize} Max Heap: ${calculatedMinHeapSize}\n";
}

#-------------------------------------------------------------------------------
# Function      :   backupConfigFile
# Parameters    :   configuration file, backup directory (optional).  Default
#		    is current directory
# Output        :   <none>
# Description   :   Backup an existing configuration file 
#-------------------------------------------------------------------------------

sub backupConfigFile
{
	($configFile,$backupDir)=@_;
	if(($configFile eq " ") || (! -f $configFile))
	{
		print "ERROR: Configuration file missing : $configFile\n";
		exit(1);
	}
	
	if(($backupDir ne "") && (! -d $backupDir))
	{
		mkdir $backupDir;
	}
	
	$base_file_name = basename($configFile);
	if( $backupDir eq "")
	{
		$dir_name=dirname($configFile);
	}
	else
	{
		$dir_name=$backupDir;
	}
	$randNo=int(rand(10000));
	$backup_file_name = "$dir_name/$base_file_name-orig-$randNo";
	
	print "Backup file $configFile to $backup_file_name\n";print "\n";
	copy($configFile,$backup_file_name);
}

#-------------------------------------------------------------------------------
# Function      :   webContainerToTune
# Parameters    :   -None-
# Output        :   Sets a variable WC_CONFIG 
# Description   :   Identifies what container the identity is installed on and
#                   returns the script name to run for tuning the container
#-------------------------------------------------------------------------------

sub webContainerToTune
{
	if($WEB_CONTAINER eq WS7)
	{	
		$WS_CONFIG="\./amtune-ws7.pl";
	}
	elsif($WEB_CONTAINER eq WS61)
	{	
		$WS_CONFIG="\./amtune-ws61.pl";
	}
	elsif($WEB_CONTAINER eq AS7)
	{	
		$WS_CONFIG="\./amtune-as7.pl";
	}
	elsif($WEB_CONTAINER eq AS8)
	{	
		$WS_CONFIG="\./amtune-as8.pl";
	}
	else
	{
		$WS_CONFIG="";
	}
}


#-------------------------------------------------------------------------------
# Function      :   getEntry
# Parameters    :   1. Entry to fetch
# Output        :   Returns the the config entry value read from AMConfig.properties
# Description   :   This function reads a configuration entry from AMConfig.properties
#-------------------------------------------------------------------------------

sub getEntry
{
	($property,$file)=@_;
	if($property eq "")
	{
		return;
	}
	
	if(($file eq "") || (! -f $file))
	{
		return;
	}
	open(FP,$file);
	@filecontent=<FP>;
	close(FP);
	@propStr=grep(m/$property/,@filecontent);
	if($#propStr == -1)
	{
		return;
	}
	foreach $i(@propStr)
	{
		@array=split(m/=/,$i);
		print $array[1]."\n";
	}
	return;
}

#-------------------------------------------------------------------------------
# Function      :   getConfigEntry
# Parameters    :   1. Config Entry to fetch
# Output        :   Returns the the config entry value read from AMConfig.properties
# Description   :   This function reads a configuration entry from AMConfig.properties
#-------------------------------------------------------------------------------

sub getConfigEntry
{
	# Don't have to check for existence of AMConfig. Its already being done in amtune-env.pl
	($property)=@_;
	if($property eq "")
	{
		return;
	}
	
	setAMConfigPropertyFile;
	
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
		return ($array[1]);
	}
}

#-------------------------------------------------------------------------------
# Function      :   setAMConfigPropertyFile
# Parameters    :   <none>
# Output        :   Returns the AMConfig.properties file name
#                   AMConfig.properties coule be instance specific
# Description   :
#-------------------------------------------------------------------------------

sub setAMConfigPropertyFile
{
	if ($IS_INSTANCE_NAME ne "")
	{
		$AMCONFIG_PROPERTY_FILE="$IS_CONFIG_DIR/$AMConfig-$IS_INSTANCE_NAME.properties";
	}
	elsif($WEB_CONTAINER_INSTANCE_NAME ne "")
	{
		$AMCONFIG_PROPERTY_FILE="$IS_CONFIG_DIR/$AMCONFIG-$WEB_CONTAINER_INSTANCE_NAME.properties";
	}
	
	if( ! -f $AMCONFIG_PROPERTY_FILE)
	{
		$AMCONFIG_PROPERTY_FILE="$IS_CONFIG_DIR/AMConfig.properties";
	}
	print "Property File : $AMCONFIG_PROPERTY_FILE\n";
}


sub getMagnusEntry() 
{
	($entry_file,$entry_key)=@_;
	open(FP,$entry_file);
	@filecontent=<FP>;
	close(FP);
	@entry_value=grep(m/$entry_key/,@filecontent);
	if($#entry_value == -1)
	{
		print "<No value set>\n";
		return;
	}
	
	foreach $i(@entry_value)
	{
		@array=split(/\s+/,$i,2);
		return $array[1];
	}
}

sub getServerXMLJVMOptionEntry
{
	($entry_file,$entry_key,$jvmoption_key)=@_;
	$entry_key="\\$entry_key";
	
	if($jvmoption_key eq "")
	{
		$jvmoption="JVMOPTIONS";
	}
	open(FP,"$entry_file");
	@filecontent=<FP>;
	close(FP);
	@match_jvmoption_key=grep(m/$jvmoption_key/i,@filecontent);
	@entry_value=grep(m/$entry_key/i,@match_jvmoption_key);
	if($#entry_value == -1)
	{
		print "<No value set>\n";
		return;
	}
	
	foreach $i(@entry_value)
	{
		@match=split(m/>/,$i);
		@pattern=split(m/</,$match[1]);
		print $pattern[0];
		print "\n";
	}
	return;
}
		
sub get_token_in_line
{
	my @file;
	my @args;
	@args=@_;
	for($i=0;$i<=($#args-6);$i++)
	{
		push(@file,$args[$i]);
	}
	$match=$args[($#args-5)];
	$token=$args[($#args-4)];
	$option_type=$args[($#args-3)];
	$file_or_stream=$args[($#args-2)];
	$field_separator=$args[($#args-1)];
	$nvp_separator=$args[($#args)];
	if($field_separator eq "")
	{
		$field_separator=" ";
	}

	if($option_type eq "")
	{
		$option_type="nvp_quoted";
	}
	
	if($nvp_separator eq "")
	{
		$nvp_separator="=";
	}
	
	if(($file_or_stream eq "") || ($file_or_stream eq "file"))
	{
		if($#file <0 )
		{
			#file does not exists. So just return silently
			return;
		}
		#Step 1 : get the line in the file 
		@orig_line=grep(m/$match/,@file);
	}
	else
	{
		$match="\\$match";
		@orig_line=grep(m/$match/,@file);
		#$orig_line=$file;
	}
	
	if($#orig_line == -1)
	{
		return;
	}
	
	#Step 2 : Get the number of tokens in the line 
	
	if($field_separator eq " ")
	{
		@tokenArr=split(m/ /,$orig_line[0]);   
	}
	else
	{
		
	}
	$number_of_tokens=$#tokenArr;
	
	#Step 3: Get the values for the token
	$count=0;
	$newline="";
	while($count le $number_of_tokens)
	{
	        
	    $cToken=$tokenArr[$count];
		@desiredTokenStr=grep(m/$token/,$cToken);
		if($#desiredTokenStr != -1 )
		{
			if($option_type eq "flag")
			{
				
				$desiredTokenValue=$desiredTokenStr[0];
			}
			else
			{
				
			}
			
			if($option_type eq "nvp_quoted")
			{
				
			}
			
			if($desiredTokenValue eq "")
			{
				print "<Empty value>\n";
			}
			
			return $desiredTokenValue;
			
		}
		
		$count = $count + 1;
	}
	
	return;
}

#-------------------------------------------------------------------------------
# Function      :   setServerConfigXMLFile
# Parameters    :   <none>
# Output        :   Returns the serverconfig.xml file name
#                   Currently, serverconfig.xml file could be instance specific
# Description   :
#-------------------------------------------------------------------------------


sub setServerConfigXMLFile
{
	if($IS_INSTANCE_NAME ne "")
	{
		$SERVERCONFIG_XML_FILE="$IS_CONFIG_DIR/serverconfig-$IS_INSTANCE_NAME.xml";
	}
	elsif($WEB_CONTAINER_INSTANCE_NAME ne "")
	{
		$SERVERCONFIG_XML_FILE="$IS_CONFIG_DIR/serverconfig-$WEB_CONTAINER_INSTANCE_NAME.xml";
	}
	
	if(! -f $SERVERCONFIG_XML_FILE)
	{
		$SERVERCONFIG_XML_FILE="$IS_CONFIG_DIR/serverConfig.xml";
	}
}

#===============================================================================
# WS 7.0 related functions
#===============================================================================

sub validateWSConfig
{
	($wsconfig)=@_;
		
	if( ! -f $WSADMIN_PASSFILE)
	{
		open(FP,">$WSADMIN_PASSFILE");
		print FP "$WSADMIN_PASSWORD_SYNTAX$WSADMIN_PASSWORD\n";
		close(FP);
	}
	
	$temp=$ENV{TEMP};
	$WSADMIN_OPTION="list-configs";
	@args = ($WSADMIN,$WSADMIN_OPTION,@WSADMIN_COMMON_PARAMS_NO_CONFIG,">$temp/temp1.txt");
	system(@args)==0 or die "\n Error executing command @args\n";
		
	open(FP,"$temp/temp1.txt");
	@filecontent=<FP>;
	close(FP);
	$ws_result=$filecontent[0];
	
	if($? == 0)
	{
		@data=grep(m/$wsconfig/,$ws_result);
		if($#data < 0)
		{
			print "ERROR: Web Server configuration in $WSADMIN_CONFIG is invalid\n";
			print "Current $WSADMIN_CONFIG setting: $WSADMIN_CONFIG\n";
			print "Current Configuration(s) from web server : $ws_result\n";
			print "You may need to customize the following file appropriately: amtune-env\.pl\n";
			exit($AMTUNE_INVALID_ENVIRON_SETTING);
		}
	}	
	else
	{
		print "Error:Cannot validate Web Server Configuration. Please see the wadm error message below\n";
		print "$ws_result\n";
		exit(1);
	}
}

sub validateWSHttpListener
{
	($httplistener)=@_;
	
	if(! -f $WSADMIN_PASSFILE)
	{
		open(FP,">>$WSADMIN_PASSFILE");
		print FP "$WSADMIN_PASSWORD_SYNTAX$WSDAMIN_PASSWORD\n";
		close(FP);
		open(FP,"$WSADMIN_PASSFILE");
		while(<FP>)
		{
			print $_."\n";
		}
		close(FP);
	}
	
	$WSADMIN_OPTION="list-http-listeners";
	$temp=$ENV{TEMP};
	@arg = ($WSADMIN,$WSADMIN_OPTION,@WSADMIN_COMMON_PARAMS,">$temp/temp.txt");
	system(@arg)==0 or "\n Error executing command @args\n";
	open(FP,"$temp/temp.txt");
	@filecontent=<FP>;
	close(FP);
	$ws_result=$filecontent[0];
	
	if($? == 0)
	{
		@data=grep(m/httplistener/,$ws_result);	
		if($#data == 0)
		{
			print "ERROR :Web Server Http Listener in WSADMIN_HTTPLISTENER is invalid\n";
			print "Current WSADMIN_HTTPLISTENER setting : $WSADMIN_HTTPLISTENER\n";
			print "Current Http Listener(s) from web server : $ws_result\n";
			print "You may need to customise the following file appropriately :amtune-env.pl\n";
			
			exit($AMTUNE_INVALID_ENVIRON_SETTING);
		}
	}
	else
	{
		print "ERROR: Cannot validate Web Server Http Listener. Please see the wadm error message below.\n";
		print "$ws_result"."\n";
		exit(0);
	}
}

#===============================================================================
# AS 8.1 related functions
#===============================================================================

sub validateASInstance 
{	
	($astarget)=@_;
	if(! -f $ASADMIN_PASSFILE)
	{	
		open(FP,">$ASADMIN_PASSFILE");
		print FP "$ASADMIN_PASSWORD_SYNTAX$ASADMIN_PASSWORD\n";
		close(FP);
		open(FP,"$ASADMIN_PASSFILE");
		while(<FP>)
		{
			print $_."\n";
		}
		close(FP);	
	}
	
	$temp=$ENV{TEMP};
	$ASADMIN_OPTION_LIST="list";
	$ASADMIN_OPTION_USER="--user";
	$ASADMIN_OPTION_PASS="--passwordfile";
	$ASADMIN_OPTION_HOST="--host";
	$ASADMIN_OPTION_PORT="--port";
	$ASADMIN_OPTION_INTER="--interactive=$ASADMIN_INTERACTIVE";
	$ASADMIN_OPTION_DOMAIN="\"domain.servers\"";
	@args=($ASADMIN,$ASADMIN_OPTION_LIST,$ASADMIN_OPTION_USER,$ASADMIN_USER,$ASADMIN_OPTION_PASS,$ASADMIN_PASSFILE,$ASADMIN_OPTION_HOST,$ASADMIN_HOST,$ASADMIN_OPTION_PORT,$ASADMIN_PORT,$ASADMIN_SECURE,$ASADMIN_OPTION_INTER,$ASADMIN_OPTION_DOMAIN,">$temp/temp.txt");
	system(@args)==0 or "\n Error executing command @args\n";
	open(FP,"$temp/temp.txt");
	@as_result=<FP>;
	close(FP);
	
	if($? == 0)
	{
		@data=grep(m/$astarget/,chomp($as_result[0]));
		print "The data @data and the size is $#data";
		if($#data == 0)
		{	
			print "ERROR: Application Server Instance/Target in ASADMIN_TARGET is invalid.\n";
			print "Current ASADMIN_TARGET setting: $ASADMIN_TARGET\n";
			print "Current Instance(s)/Target(s) from application server: $as_result\n";
			print "You may need to customize the following file appropriately: amtune-env\n";
			exit($AMTUNE_INVALID_ENVIRON_SETTING);
		}
	}
	else
	{
		print "ERROR:Cannot validate Application Server Instance/Target. Please see the asadmin error message below:\n";
		print "$as_result\n";
	}
	
	#unlink($ASADMIN_PASSFILE);
}

#$asadmin_max_heap=&getASJVMOption('-Xmx','flag',',');

sub getASJVMOption
{
	($option_key,$option_type,$field_separator,$nvp_separator)=@_;
	
	#------------------------------------------------------------------------------------
	#if($option_string = "")
	#{
	#	`$ASADMIN get --user $ASADMIN_USER --password_file $ASADMIN_PASSFILE --host $ASDMIN_HOST --port $ASDMIN_PORT $ASDMIN_SECURE --interactive=$ASADMIN_INTERACTIVE "$ASADMIN_TARGET.java-config.jvm-options"`;
	#}
	#if($option_string ="")
	#{
	#	return 0;
	#}
	
	get_token_in_line(@jvmfilecontent,$option_key,$option_key,$option_type,"stream",$field_separator,$nvp_separator);
}

sub test_as_admin
{
	my $ASADMIN="";
	my $ASDMIN_USER="admin";
	my $ASADMIN_PASSFILE="/tmp/passfile";
	my $ASADMIN_PORT=4849;
	my $ASADMIN_HOST="cal1.red.iplanet.com";
	my $ASADMIN_SECURE="--secure";
	my $ASADMIN_INTERACTIVE=false;
	my $ASADMIN_TARGET="server";
	
	rmdir "/tmp/test";
	
	open(FP,">>$ASADMIN_PASSFILE");
	print FP "$ASADMIN_PASSWORD=password\n";
	close(FP);
	open(FP,"$ASADMIN_PASSFILE");
	while(<FP>)
	{
		print $_."\n";
	}
	close(FP);
	
	print "Starting\n";
	print "-Xmx\n";
	$test= getASJVMOption("-Xmx","flag",",");
	print $test;
	
	print "-Xloggc\n";
	$test=getASJVMOption("-Xloggc","nvp",",",":");
	print "nvp: $test\n";
	$test=getASJVMOption("-Xloggc","flag",",",":");
	print "flag= $test\n";
	
	print "acceptor-threads\n";
	$test=get_token_in_line("\/tmp\/domain\.xml","acceptor-threads","acceptor-threads");
	print $test."\n";
	
	print "-Xrs\n";
	$test=getASJVMOption("-Xrs","flag",",",":");
	print $test."\n";
	
	print "-XX:PermSize\n";
	$test=getASJVMOption("-XX:PermSize","nvp",",",":");
	print $test."\n";
}

sub test_validate_as_instance
{
	my $ASADMIN="";
	my $ASADMIN_USER=admin;
	my $ASADMIN_PASSFILE="/tmp/passfile";
	my $ASADMIN_PORT=4849;
	my $ASADMIN_HOST="cal1.red.iplanet.com";
	my $ASADMIN_SECURE="--secure";
	my $ASADMIN_INTERACTIVE=false;
	my $ASADMIN_TARGET="server";
	my $ASADMIN_PASSWORD_SYNTAX="AS_ADMIN_PASSWORD=";
	my $ASADMIN_PASSWORD=secret12;
	
	validateASInstance($ASADMIN_TARGET);
}

sub getASVersionUsingASAdmin
{
	if(! -f $ASADMIN_PASSFILE)
	{
		open(FP,">>$ASADMIN_PASSFILE");
		print FP "$ASADMIN_PASSWORD_SYNTAX$ASADMIN_PASSWORD\n";
		close(FP);
		open(FP,"$ASADMIN_PASSFILE");
		while(<FP>)
		{
			print $_."\n";
		}
		close(FP);
	}
	
	$ASADMIN_OPTION_VERSION="version";
	$ASADMIN_OPTION_USER="--user";
	$ASADMIN_OPTION_PASS="--passwordfile";
	$ASADMIN_OPTION_HOST="--host";
	$ASADMIN_OPTION_PORT="--port";
	$ASADMIN_OPTION_INTER="--interactive=$ASADMIN_INTERACTIVE";
	$ASADMIN_OPTION_DOMAIN="domain.servers";
	@args=("$ASADMIN","$ASADMIN_OPTION_VERSION","$ASADMIN_OPTION_USER","$ASADMIN_USER","$ASADMIN_OPTION_PASS","$ASADMIN_PASSFILE","$ASADMIN_OPTION_HOST","$ASADMIN_PORT","$ASADMIN_SECURE","$ASADMIN_OPTION_INTER","$temp/temp.txt");
	system(@args)==0 or die "\n Error executing command @args\n";
	open(FP,"$temp/temp.txt");
	@variable=<FP>;
	close(FP);
	#@variable = `$ASADMIN version --user $ASADMIN_USER --passwordfile $ASADMIN_PASSFILE --host $ASADMIN_HOST --port $ASADMIN_PORT $ASADMIN_SECURE --interactive=$ASADMIN_INTERACTIVE`;
	$version=grep(m/version/,@variable);
	print $version."\n";
}

#===============================================================================
# LDAP related functions
#===============================================================================

sub getNumberOfWorkerThreads
{
	@LDAPSEARCH_OPTIONS=("-h","$DS_HOST","-p","$DS_PORT","-D","\"$DIRMGR_UID\"","-w","$DIRMGR_PASSWORD","-b","cn=config","-s", "base","(objectclass=*)","nsslapd-threadnumber",">ldapsearch.tmp");
	@args=($LDAPSEARCH,@LDAPSEARCH_OPTIONS);
	system("@args")==0 or die "\n Error executing command @args\n";
	#open(FP,">ldapsearch.tmp");
	#print FP `$LDAPSEARCH -h $DS_HOST -p $DS_PORT -D "$DIRMGR_UID" -w "$DIRMGR_PASSWORD" -b "cn=config" -s "base" "(objectclass=*)" "nsslapd-threadnumber"`;
	#close(FP);
	open(FP,"ldapsearch.tmp");
	@filecontent=<FP>;
	close(FP);
	unlink("ldapsearch.tmp");
	@array=grep(m/nsslapd-threadnumber:/,@filecontent);
	@data=split(m/:\s*/,$array[0],2);
	return $data[1];
}

sub getAccessLogStatus
{
	@LDAPSEARCH_OPTIONS=("-h","$DS_HOST","-p","$DS_PORT","-D","\"$DIRMGR_UID\"","-w","$DIRMGR_PASSWORD","-b","cn=config","-s", "base","(objectclass=*)","nsslapd-accesslog-logging-enabled",">ldapsearch.tmp");
	@args=($LDAPSEARCH,@LDAPSEARCH_OPTIONS);
	system("@args")==0 or die "\n Error executing command @args\n";
	#open(FP,">ldapsearch.tmp");
	#print FP `$LDAPSEARCH -h $DS_HOST -p $DS_PORT -D "$DIRMGR_UID" -w "$DIRMGR_PASSWORD" -b "cn=config" -s "base" "(objectclass=*)" "nsslapd-accesslog-logging-enabled"`;  
	#close(FP);
	open(FP,"ldapsearch.tmp");
	@filecontent=<FP>;
	close(FP);
	unlink("ldapsearch.tmp");
	@array=grep(m/nsslapd-accesslog-logging-enabled:(\s*)/,@filecontent);
	@data=split(m/:\s*/,$array[0],2);
	return $data[1];
}

sub getInstanceDir
{
	@LDAPSEARCH_OPTIONS=("-h",$DS_HOST,"-p",$DS_PORT,"-D","$DIRMGR_UID","-w",$DIRMGR_PASSWORD,"-b","cn=config","-s", "base","(objectclass=*)","nsslapd-instancedir",">ldapsearch.tmp");
	@args=($LDAPSEARCH,@LDAPSEARCH_OPTIONS);
	system("@args")==0 or die "\n Error executing command @args\n";
	#open(FP,">ldapsearch.tmp");
	#print FP `$LDAPSEARCH -h $DS_HOST -p $DS_PORT -D "$DIRMGR_UID" -w "$DIRMGR_PASSWORD" -b "cn=config" -s "base" "(objectclass=*)" "nsslapd-instancedir"`;  
	#close(FP);
	open(FP,"ldapsearch.tmp");
	@filecontent=<FP>;
	close(FP);
	unlink("ldapsearch.tmp");
	@array=grep(m/nsslapd-instancedir:/,@filecontent);
	@data=split(m/:\s*/,$array[0],2);
	return $data[1];
}

sub getDSVersion
{
	@LDAPSEARCH_OPTIONS=("-h","$DS_HOST","-p","$DS_PORT","-D","$DIRMGR_UID","-w","$DIRMGR_PASSWORD","-b","cn=config","-s", "base","(objectclass=*)","nsslapd-versionstring",">ldapsearch.tmp");
	@args=($LDAPSEARCH,@LDAPSEARCH_OPTIONS);
	system("@args")==0 or die "\n Error executing command @args\n";
	#open(FP,">ldapsearch.tmp");
	#print FP `$LDAPSEARCH -h $DS_HOST -p $DS_PORT -D "$DIRMGR_UID" -w "$DIRMGR_PASSWORD" -b "cn=config" -s "base" "(objectclass=*)" "nsslapd-versionstring"`;  
	#close(FP);
	open(FP,"ldapsearch.tmp");
	@filecontent=<FP>;
	close(FP);
	unlink("ldapsearch.tmp");
	@array=grep(m/nsslapd-versionstring:/,@filecontent);
	@data=split(m/:\s*/,$array[0],2);
	return $data[1];	
}

sub getDBDirectory
{	
	@LDAPSEARCH_OPTIONS=("-h","$DS_HOST","-p","$DS_PORT","-D","\"$DIRMGR_UID\"","-w","$DIRMGR_PASSWORD","-b","cn=config","(nsslapd-suffix=$ROOT_SUFFIX)","nsslapd-directory",">ldapsearch.tmp");
	@args=($LDAPSEARCH,@LDAPSEARCH_OPTIONS);
	system("@args")==0 or die "\n Error executing command @args\n";
	#open(FP,">ldapsearch.tmp");
	#print FP `$LDAPSEARCH -h $DS_HOST -p $DS_PORT -D "$DIRMGR_UID" -w "$DIRMGR_PASSWORD" -b "cn=config" "(nsslapd-suffix=$ROOT_SUFFIX)" "nsslapd-directory"`;  
	#close(FP);
	open(FP,"ldapsearch.tmp");
	@filecontent=<FP>;
	close(FP);
	unlink("ldapsearch.tmp");
	@array=grep(m/nsslapd-directory:/,@filecontent);
	@data=split(m/:\s*/,$array[0],2);
	return $data[1];	
}

sub getDBDN
{
	@LDAPSEARCH_OPTIONS=("-h","$DS_HOST","-p","$DS_PORT","-D","\"$DIRMGR_UID\"","-w","$DIRMGR_PASSWORD","-b","cn=config","(nsslapd-suffix=$ROOT_SUFFIX)","dn",">ldapsearch.tmp");
	@args=($LDAPSEARCH,@LDAPSEARCH_OPTIONS);
	system("@args")==0 or die "\n Error executing command @args\n";
	#open(FP,">ldapsearch.tmp");
	#print FP `$LDAPSEARCH -h $DS_HOST -p $DS_PORT -D "$DIRMGR_UID" -w "$DIRMGR_PASSWORD" -b "cn=config" "(nsslapd-suffix=$ROOT_SUFFIX)" "dn"`;  
	#close(FP);
	open(FP,"ldapsearch.tmp");
	@filecontent=<FP>;
	close(FP);
	unlink("ldapsearch.tmp");
	@array=grep(m/dn:/,@filecontent);
	@data=split(m/:\s*/,$array[0],2);
	return $data[1];	
}

sub getDBDNbyBackend
{
    ($backend_name)=@_;
    @LDAPSEARCH_OPTIONS=("-h","$DS_HOST","-p","$DS_PORT","-D","\"$DIRMGR_UID\"","-w","$DIRMGR_PASSWORD","-b","\"cn=config\"","\"(&(nsslapd-suffix=$ROOT_SUFFIX)(cn=$backend_name))\"","\"dn\"",">ldapsearch.tmp");
	@args=($LDAPSEARCH,@LDAPSEARCH_OPTIONS);
	system("@args")==0 or die "\n Error executing command @args\n";
	#open(FP,">ldapsearch.tmp");
	#print FP `$LDAPSEARCH -h $DS_HOST -p $DS_PORT -D "$DIRMGR_UID" -w "$DIRMGR_PASSWORD" -b "cn=config" "(&(nsslapd-suffix=*$ROOT_SUFFIX)(cn=$backend_name))" "dn" | $GREP "dn:" | $CUT -f2 -d":"`
	#close(FP);
	open(FP,"ldapsearch.tmp");
	@filecontent=<FP>;
	close(FP);
	unlink("ldapsearch.tmp");
	@array=grep(m/dn:/,@filecontent);
	@data=split(m/:\s*/,$array[0],2);
	return $data[1];
}



# Retrieve the suffix and sub-suffix in the same root DN.
sub getBackend
{

	my @value=();  
	my @finalarray=();
	@LDAPSEARCH_OPTIONS=("-h","$DS_HOST","-p","$DS_PORT","-D","\"$DIRMGR_UID\"","-w","$DIRMGR_PASSWORD","-b","\"cn=mapping tree,cn=config\"","\"(&(|(cn=$ROOT_SUFFIX)(cn=\"$ROOT_SUFFIX\")(nsslapd-parent-suffix=$ROOT_SUFFUX))(nsslapd-backend=*))\"","\"nsslapd-backend\"",">ldapsearch.tmp");
	@args=($LDAPSEARCH,@LDAPSEARCH_OPTIONS);
	system("@args")==0 or die "\n Error executing command @args\n";
	open(FP,"ldapsearch.tmp");
	@filecontent=<FP>;
	close(FP);
	unlink("ldapsearch.tmp");
	@array=grep(m/nsslapd-backend/,@filecontent);
	@data=grep(!/NetscapeRoot/,@array);
	foreach $i(@data)
	{
		@var=split(m/:\s+/,$i);
		push(@finalarray,$var[1]);
	}
	foreach $i(@finalarray)
	{
		push(@value,$i);
	}
	return(@value);
}


sub getDBEntryCacheSize
{
	@LDAPSEARCH_OPTIONS=("-h","$DS_HOST","-p","$DS_PORT","-D","\"$DIRMGR_UID\"","-w","$DIRMGR_PASSWORD","-b","cn=config","(nsslapd-suffix=$ROOT_SUFFIX)","-s", "base","(objectclass=*)","nsslapd-cachememsize",">ldapsearch.tmp");
	@args=($LDAPSEARCH,@LDAPSEARCH_OPTIONS);
	system("@args")==0 or die "\n Error executing command @args\n";
	#open(FP,">ldapsearch.tmp");
	#print FP `$LDAPSEARCH -h $DS_HOST -p $DS_PORT -D "$DIRMGR_UID" -w "$DIRMGR_PASSWORD" -b "cn=config" "(nsslapd-suffix=$ROOT_SUFFIX)" "nsslapd-cachememsize"`;  
	#close(FP);
	open(FP,"ldapsearch.tmp");
	@filecontent=<FP>;
	close(FP);
	unlink("ldapsearch.tmp");
	@array=grep(m/nsslapd-cachememsize:/,@filecontent);
	@data=split(m/:\s*/,$array[0],2);
	return $data[1];	
}

sub getDBEntryCacheSizebyBackend
{
    ($backend_name)=@_;
	@LDAPSEARCH_OPTIONS=("-h","$DS_HOST","-p","$DS_PORT","-D","\"$DIRMGR_UID\"","-w","$DIRMGR_PASSWORD","-b","\"cn=config\"","\"(&(nsslapd-suffix=$ROOT_SUFFIX)(cn=$backend_name))\"","\"nsslapd-cachememsize\"",">ldapsearch.tmp");
	@args=($LDAPSEARCH,@LDAPSEARCH_OPTIONS);
	system("@args")==0 or die "\n Error executing command @args\n";
	#open(FP,">ldapsearch.tmp");
	#print FP `$LDAPSEARCH -h $DS_HOST -p $DS_PORT -D "$DIRMGR_UID" -w "$DIRMGR_PASSWORD" -b "cn=config" "(&(nsslapd-suffix=*$ROOT_SUFFIX)(cn=$backend_name))" "nsslapd-cachememsize"`
	#close(FP);
	open(FP,"ldapsearch.tmp");
	@filecontent=<FP>;
	close(FP);
	unlink("ldapsearch.tmp");
	@array=grep(m/nsslapd-cachememsize:/,@filecontent);
	@data=split(m/:\s*/,$array[0],2);
	return $data[1];
}

sub getSuffixbyBackend() 
{
    ($backend_name)=@_;
    @LDAPSEARCH_OPTIONS=("-h","$DS_HOST","-p","$DS_PORT","-D","\"$DIRMGR_UID\"","-w","$DIRMGR_PASSWORD","-b","cn=config","(cn=$backend_name)","nsslapd-suffix",">ldapsearch.tmp");
	@args=($LDAPSEARCH,@LDAPSEARCH_OPTIONS);
	system("@args")==0 or die "\n Error executing command @args\n";
	#open(FP,">ldapsearch.tmp");
	#print FP `$LDAPSEARCH -h $DS_HOST -p $DS_PORT -D "$DIRMGR_UID" -w "$DIRMGR_PASSWORD" -b "cn=config" "(cn=$backend_name)" "nsslapd-suffix"`
	#close(FP);
	open(FP,"ldapsearch.tmp");
	@filecontent=<FP>;
	close(FP);
	unlink("ldapsearch.tmp");
	@array=grep(m/nsslapd-suffix:/,@filecontent);
	@data=split(m/:\s*/,$array[0],2);
	return $data[1];
}

sub getDBCacheSize
{
	@LDAPSEARCH_OPTIONS=("-h","$DS_HOST","-p","$DS_PORT","-D","\"$DIRMGR_UID\"","-w","$DIRMGR_PASSWORD","-b","\"cn=config,cn=ldbm database,cn=plugins,cn=config\"","-s", "base","(objectclass=*)","nsslapd-dbcachesize",">ldapsearch.tmp");
	@args=($LDAPSEARCH,@LDAPSEARCH_OPTIONS);
	system("@args")==0 or die "\n Error executing command @args\n";
	#open(FP,">ldapsearch.tmp");
	#print FP `$LDAPSEARCH -h $DS_HOST -p $DS_PORT -D "$DIRMGR_UID" -w "$DIRMGR_PASSWORD" -b "cn=config,cn=ldbm database,cn=plugins,cn=config" -s "base" "(objectclass=*)" "nsslapd-dbcachesize" `;  
	#close(FP);
	open(FP,"ldapsearch.tmp");
	@filecontent=<FP>;
	close(FP);
	unlink("ldapsearch.tmp");
	@array=grep(m/nsslapd-dbcachesize:/,@filecontent);
	@data=split(m/:\s*/,$array[0],2);
	return $data[1];	
}

sub getDBLocation
{
	@LDAPSEARCH_OPTIONS=("-h","$DS_HOST","-p","$DS_PORT","-D","\"$DIRMGR_UID\"","-w","$DIRMGR_PASSWORD","-b","\"cn=config,cn=ldbm database,cn=plugins,cn=config\"","-s", "base","(objectclass=*)","nsslapd-directory",">ldapsearch.tmp");
	@args=($LDAPSEARCH,@LDAPSEARCH_OPTIONS);
	system("@args")==0 or die "\n Error executing command @args\n";
	#open(FP,">ldapsearch.tmp");
	#print FP `$LDAPSEARCH -h $DS_HOST -p $DS_PORT -D "$DIRMGR_UID" -w "$DIRMGR_PASSWORD" -b "cn=config,cn=ldbm database,cn=plugins,cn=config" -s "base" "(objectclass=*)" "nsslapd-directory"`;  
	#close(FP);
	open(FP,"ldapsearch.tmp");
	@filecontent=<FP>;
	close(FP);
	unlink("ldapsearch.tmp");
	@array=grep(m/nsslapd-directory:/,@filecontent);
	@data=split(m/:\s*/,$array[0],2);
	return $data[1];	
}

sub getDBHomeLocation
{
	@LDAPSEARCH_OPTIONS=("-h","$DS_HOST","-p","$DS_PORT","-D","\"$DIRMGR_UID\"","-w","$DIRMGR_PASSWORD","-b","\"cn=config,cn=ldbm database,cn=plugins,cn=config\"","-s", "base","(objectclass=*)","nsslapd-db-home-directory",">ldapsearch.tmp");
	@args=($LDAPSEARCH,@LDAPSEARCH_OPTIONS);
	system("@args")==0 or die "\n Error executing command @args\n";
	#open(FP,">ldapsearch.tmp");
	#print FP `$LDAPSEARCH -h $DS_HOST -p $DS_PORT -D "$DIRMGR_UID" -w "$DIRMGR_PASSWORD" -b "cn=config,cn=ldbm database,cn=plugins,cn=config" -s "base" "(objectclass=*)" "nsslapd-db-home-directory"`;  
	#close(FP);
	open(FP,"ldapsearch.tmp");
	@filecontent=<FP>;
	close(FP);
	@array=grep(m/nsslapd-db-home-directory:/,@filecontent);
	@data=split(m/:\s*/,$array[0],2);
	return $data[1];	
}

#===============================================================================
# Functions imported from amutils
#===============================================================================

sub replace_line
{
	($file,$match,$new)=@_;
	
	if( ! -f "$file-orig")
	{
		if(-f $file)
		{
			copy($file,"$file-orig");
		}
	}
	
	open(FP,$file);
	@array=<FP>;
	close(FP);
	@gre=grep(s/$match.*/$new/,@array);
	open(FP,">$file-tmp");
	print FP @array;
	close(FP);
	copy("$file-tmp",$file);
	@file_delete="$file-tmp";
	unlink(@file_delete);
}


sub cat_line 
{
	($file,$new)=@_;
	if( ! -f "$file-orig.tmp")
	{
		if(-f $file)
		{
			copy($file,"$file-orig.tmp");
		}
	}
	
	open(FP,">>$file");
	print FP $new;
	close(FP);
	open(FP,$file);
	while(<FP>)
	{
		print $_."\n";
	}
	close(FP);
}

sub insert_line 
{
	($file,$match,$new)=@_;
	if(! -f "$file-orig.tmp")
	{
		if( -f $file)
		{
			copy($file,"$file-orig.tmp");
		}
	}
	
	open(FP,$file);
	@filecontent=<FP>;
	close(FP);
	foreach $i(@filecontent)
	{
		$i =~ s/(.*)($match)(.*)/$new $i/g;
		open(FP,">>$file.tmp");
		print FP $i;
		close(FP);
	}	
	copy("$file.tmp",$file);
	unlink("$file.tmp");
}

sub delete_line
{
	($file,$match)=@_;
	if(! -f "$file-orig.tmp")
	{
		if(-f $file)
		{
			copy($file,"$file-orig.tmp");
		}
	}

	open(FP,$file);
	@filecontent=<FP>;
	close(FP);
	foreach $i(@filecontent)
	{
		$i =~ s/(.*)($match)(.*)//g;
		open(FP,">>$file.tmp");
		print FP $i;
		close(FP);
	}	

	copy("$file.tmp",$file);
	@file_delete=("$file.tmp");
	unlink @file_delete;
}

sub append_line
{
	($file,$match,$new)=@_;
	if( ! -f "$file-orig.tmp")
	{
	 	if(-f $file)
	 	{
	 		copy($file,"$file-orig.tmp");
	 	}
	}
	 
	open(FP,$file);
	@filecontent=<FP>;
	close(FP);
	foreach $i(@filecontent)
	{
		$i =~ s/(.*)($match)(.*)/$i $new/g;
		open(FP,">>$file-tmp");
		print FP $i;
		close(FP);
	}	
	 
	 copy("$file-tmp",$file);
	 unlink("$file-tmp");
}

sub add_to_end 
{
	($file,$new)=@_;

	if(! -f "$file-orig-tmp")
	{
		if(-f $file)
		{
			copy($file,"$file-orig-tmp");
		}
	}

	open(FP,"$file");
	@file_content=<FP>;
	close(FP);
	$length=($#file_content+1);
	if($length == 0)
	{
		open(FP,">$file");
		print FP $new;
		close(FP);  
	}
	else 
	{
		open(FP,">>$file");
		print FP $new;
		close(FP);
	}
}

#----------------------------------------------------------------
# Check local box environment
#----------------------------------------------------------------
# To check for version
sub check_env
{
	my $Register = "SYSTEM\\CurrentControlSet\\Control\\Session Manager\\Environment";
	my ($hkey,@key_list,$key,%values);
	$HKEY_LOCAL_MACHINE->Open($Register,$hkey)|| die $!;
	$hkey->GetValues(\%values);
	foreach $value (keys(%values))
	{
		if($value eq "PROCESSOR_ARCHITECTURE")
		{
			#$key=$values{$value}->[0];
			$val2=$values{$value}->[2];
			$num_cpu=$val2;
			#print "Key:$key, RegValue :$val2";
		}
		if($value eq "OS")
		{
			$val2=$values{$value}->[2];
			$os_name=$val2;
		}
					
	}
	$hkey->Close();
	print "Processor_Architecture:$num_cpu\n";
	print "OS Name : $os_name\n";
}


#----------------------------------------------------------------
# Check file for write permission
#----------------------------------------------------------------
sub check_file_for_write
{
	($file,$verbose)=@_;

	if($verbose eq "")
    {
	        $verbose=1;
	}
    
	if (! -f $file)
	{
		if($verbose eq "1")
	    {
	    	print "File not found:  $file";
		}
	    return 100;
	}

	open(FP,">>$file");
	close(FP);

	if($? eq 1)
	{
		return 100;
	}
}


#--------------------------------------------------------------------------------------------------
# substitute_token_in_line
# This function is useful in replacing tokens of the type token="value" in any line of a given file
# (especially useful in replacing values for certain tokens in XML files
# match     - matching pattern which identifies the line where the token is to be replaced
# token     - token name fully spelt
# newvalue  - new value to be used
# mode      - 0 is replace
#             1 is prepend
#             2 is append
# delimiter - delimiter to be used while appending or prepending b/w the old and new values
#--------------------------------------------------------------------------------------------------
sub substitute_token_in_line
{
	($file,$match,$token,$newvalue,$mode,$delimiter)=@_;
	
	#step1: grep for classpath suffix in server.xml
	open(FP,"$file");
	@filecontent=<FP>;
	close(FP);
	@orig_line=grep(m/$match/,@filecontent);
	
	#step2: Get the number of tokens in the line
	foreach $i(@orig_line)
	{
		@array=split(/s+/,$i);
		push(@number_of_tokens,($#array+1));
	}
	
	
	#step3: Replace the value for the token
	$count=1;
	$newline="";
	while($count le $number_of_tokens)
	{
		foreach $i(@orig_line)
		{
			@pattern=split(m/\s+/,$i);
			push(@pat,$pattern[$count]);
		}
		$currentToken=$pat[0];
		
		foreach $i(@pat)
		{
			@pattern=split(m/\s+/,$i);
			push(@desired,$pattern[$count]);
		}
		@desiredTokenStr=grep(m/$token/,@desired);
		
		if($#desiredTokenStr > -1)
		{
			
			foreach $i(@desiredTokenStr)
			{
				@array=split(m/=/,$i);
				@array1=split(m/"/,$array[1]);
				push(@desiredTokenValue,$array1[1]);
				push(@left_over_str,$array1[2]);
			}
			
			
			foreach $i(@desiredTokenValue)
			{
				@char=split(m//,$i);
				$count=$count+$#char+1;
			}
			
			if(($mode eq "0")||($count eq "1"))
			{
				$desiredTokenValue="\"$newvalue\"";
			}
			elsif($mode  eq "1")
			{
				$desiredTokenValue="\"$newvalue$delimiter$desiredTokenValue\"";
			}
			else
			{
				$desiredTokenValue="\"$desiredTokenValue$delimiter$newvalue\"";
			}
			
			$new_token_str="$token=$desiredTokenValue$left_over_str";
			$newline="$newline $new_token_str";
		}
		else
		{
			$newline="$newline $currentToken";
		}
		$count=$count + 1;
	}
	&replace_line("$file",$match,"$newline");
}

