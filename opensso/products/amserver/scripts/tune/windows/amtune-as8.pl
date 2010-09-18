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
# $Id: amtune-as8.pl,v 1.4 2008/08/19 19:08:33 veiming Exp $
#
#
#############################################################################
use File::Basename;

$AMTUNE_AS8_SCRIPT="amtune-as8.pl|as|1";
$AMTUNE_SCRIPT_RECORD_STRING=$AMTUNE_AS8_SCRIPT;

require "amtune-utils.pl";
require "amtune-env.pl";

my $SCRIPT_LOCATION=basename($0);




sub deleteJVMOptionUsingASAdmin
{
    ($jvmOptionString)=@_;
	
    if(($jvmOptionString eq <No value set>) || ( $jvmOptionString eq "") || ($jvmOptionString eq <Empty String>))
    {
        return;
    }
	
    print "\n";

    print "Deleting JVM Option : $jvmOptionString\n";
  
    $ASADMIN_DELETE_JVM_OPTION="delete-jvm-options";
  
    $ASADMIN_OPTIONSTRING="\"$jvmOptionString\"";

    @args=($ASADMIN,$ASADMIN_DELETE_JVM_OPTION,@ASADMIN_COMMON_PARAMS,$ASADMIN_OPTIONSTRING);
 
    system("@args")==0 or die "\nError executing command\n";
}


sub insertJVMOptionUsingASAdmin
{
    ($jvmOptionString)=@_;

    if($jvmOptionString ne "")
    {
        print "\n"."Inserting JVM Option : $jvmOptionString"."\n";
	$ASADMIN_CREATE_JVM_OPTION="create-jvm-options";
	$ASADMIN_OPTION_STRING="\"$jvmOptionString\"";
	@args=($ASADMIN,$ASADMIN_CREATE_JVM_OPTION,@ASADMIN_COMMON_PARAMS,$ASADMIN_OPTION_STRING);
	system("@args")==0 or die "\nError executing command\n";
    }
}

sub createJvmOptionString_orig
{
    ($jvmOptionMainString,$jvmOptionString)=@_;
	
    #Check for the case of -D<option>=<No value set>
    @break=split(m/=/,$jvmOptionString);
    $sub_jvmoptionString=$break[1];
    if(($sub_jvmOptionString eq <No value set>) || ($jvmOptionString == "") || ($sub_jvmOptionString eq <Empty value>))
    {
        return $jvmOptionMainString;
    }
    else
    {
        # To port
	#jvmOptionString=`$ECHO $jvmOptionString | $AWK ' BEGIN { FS = ":" ; ORS="" } {print ":"} { for (i = 1; i < NF; i++) { print $i; print "\\\:" } {print $i} }'`
	return "$jvmOptionMainString$jvmOptionString";
    }
}

sub createJvmOptionString
{
    ($jvmOptionMainString,$jvmOptionString)=@_;
    if($jvmOptionString ne "")
    {
        @array=grep(m/:/,$jvmOptionString);
	if($#array != -1)
	{
	    $jvmOptionString =~ s/:/\\:/g;
	}		
	$jvmOptionMainString="$jvmOptionMainString".":"."$jvmOptionString";
	return $jvmOptionMainString;
    }
    else 
    {
        return $jvmOptionMainString;
    }
}

sub tuneDomainXML
{
    $tune_file="$CONTAINER_INSTANCE_DIR/config/domain.xml";
	
    print $LINE_SEP;
    print "Tuning Application Server Instance..."."\n";
    print "File                   : $tune_file(using asadmin command line tool)"."\n";
    print "Parameter tuning       : "."\n";
	
    $acceptor_threads_string="server.http-service.http-listener.$ASADMIN_HTTPLISTENER.acceptor-threads";
    $count_threads_string="server.http-service.connection-pool.max-pending-count";
    $queue_size_string="server.http-service.connection-pool.queue-size-in-bytes";
	
    # Construct a parameter string to perform an asadmin get for acceptor-thread, queue-size,
    # and count-thread parameters
	
    $asadmin_get_params=$acceptor_threads_string;
    $asadmin_get_params="$asadmin_get_params $count_threads_string";
    $asadmin_get_params="$asadmin_get_params $queue_size_string";
    $ASADMIN_OPTION="get";
    @args=("$ASADMIN"," $ASADMIN_OPTION", "@ASADMIN_COMMON_PARAMS_NO_TARGET", "$asadmin_get_params "," >result.tmp");
    system ("@args")==0 or die "\nError executing command\n";
	
    open(FP,"result.tmp");
    @filecontent=<FP>;
    close(FP);
	
    $file_name="result.tmp";
	
    print "\n acceptor = $acceptor_threads_string\n";
	
    $asadmin_acceptor_threads=&get_token_in_file($file_name,"$acceptor_threads_string");
	
    print " 1. Acceptor Threads\n";
    print " Current Value          : acceptor-threads=$asadmin_acceptor_threads\n";
    print " Recommended Value      : accpetor-threads=$acceptorThreads\n\n";
	
    $asadmin_count_threads=&get_token_in_file($file_name,"$count_threads_string");
	
    print " 2. Maximum Pending Count Threads\n";
    print " Current Value          : max-pending-count=$asadmin_count_threads\n";
    print " Recommended Value      : max-pending-count=$AMTUNE_NUM_TCP_CONN_SIZE\n\n";
	
    $asadmin_queue_size=&get_token_in_file($file_name,"$queue_size_string");
	
    print " 3. Queue Size \n";
    print " Current Value          : queue-size=$asadmin_queue_size\n";
    print " Recommended Value      : queue-size=$AMTUNE_NUM_TCP_CONN_SIZE\n\n";
	
    $ASADMIN_GET="get";
    $ASADMIN_OPTION_TARGET="\"$ASADMIN_TARGET.java-config.jvm-options\"";
    @args=($ASADMIN,$ASADMIN_GET,@ASADMIN_COMMON_PARAMS_NO_TARGET,$ASADMIN_OPTION_TARGET,">jvm_option1.txt");
    system("@args")==0 or die "\nError executing command\n";
    #$option_string=`$ASADMIN get @ASADMIN_COMMON_PARAMS_NO_TARGET "$ASADMIN_TARGET.java-config.jvm-options"`;
		
    open(FP,"jvm_option1.txt");
    @jvmfilecontent=<FP>;
    close(FP);
	
    $asadmin_min_heap=getASJVMOptions('-Xms');
    $asadmin_max_heap=getASJVMOptions('-Xmx');
    $asadmin_new_min_heap="-Xms${maxHeapSize}M";
    $asadmin_new_max_heap="-Xmx${maxHeapSize}M";
	
    print " 4. Max and Min Heap Size\n";
    print " Current Value         : Min Heap: $asadmin_min_heap Max Heap: $asadmin_max_heap\n";
    print " Recommended Value     : $asadmin_new_min_heap $asadmin_new_max_heap\n\n";
	
    $asadmin_loggc=getASJVMOptions('-Xloggc');
    #$asadmin_loggc=&getASJVMOption('-Xloggc','flag',',');
    $asadmin_new_loggc="-Xloggc:$CONTAINER_INSTANCE_DIR/logs/gc.log";
	
    print " 5. LogGC Output\n";
    print " Current Value         : $asadmin_loggc\n"; 
    print " Recommended Value     : $asadmin_new_loggc\n\n";
	
    $asadmin_serveroption=getASJVMOptions('-server');
    #$asadmin_serveroption=&getASJVMOption('-server','flag',',');
    $asadmin_new_serveroption="-server";
	
    print " 6. JVM in Server mode\n";
    print " Current Value         : $asadmin_serveroption\n"; 
    print " Recommended Value     : $asadmin_new_serveroption\n\n";
	
    $asadmin_stacksize=getASJVMOptions('-Xss');
    #$asadmin_stacksize=&getASJVMOption('-Xss','flag',',');
    $asadmin_new_stacksize="-Xss${AMTUNE_PER_THREAD_STACK_SIZE_IN_KB}k";
	
    print " 7. Stack Size\n"; 
    print " Current Value         : $asadmin_stacksize\n";
    print " Recommended Value     : $asadmin_new_stacksize\n\n";
	
    $asadmin_newsize=getASJVMOptions('-XX:NewSize');
    #$asadmin_newsize=&getASJVMOption('-XX:NewSize','flag',',');
    $asadmin_new_newsize="-XX:NewSize=${maxNewSize}M";
	
    print " 8. New Size\n";
    print " Current Value         : $asadmin_newsize\n";
    print " Recommended Value     : $asadmin_new_newsize\n\n";
	
    $asadmin_maxnewsize=getASJVMOptions("-XX:MaxNewSize");
    #$asadmin_maxnewsize=&getASJVMOption("-XX:MaxNewSize","flag",",");
    $asadmin_new_maxnewsize="-XX:MaxNewSize=${maxNewSize}M";
	
    print " 9. Max New Size\n";
    print " Current Value         : $asadmin_maxnewsize\n";
    print " Recommended Value     : $asadmin_new_maxnewsize\n\n";
	
    $asadmin_disableExplicitGC=getASJVMOptions("-XX:+DisableExplitcitGC");
    #$asadmin_disableExplicitGC=&getASJVMOption("-XX:+DisableExplitcitGC","flag",",");
    $asadmin_new_disabledExplicitGC="-XX:+DisableExplicitGC";
	
    print " 10. Disabled Explicit GC\n";
    print " Current Value         : $asadmin_disableExplicitGC\n";
    print " Recommended Value     : $asadmin_new_disabledExplicitGC\n\n";
	
    $asadmin_parallelGC=getASJVMOptions("-XX:+UseParNewGC");
    #$asadmin_parallelGC=&getASJVMOption("-XX:+UseParNewGC","flag",",");
    $asadmin_new_parallelGC="-XX:+UseParNewGC";
	
    print " 11. Use Parallel GC\n";
    print " Current Value         : $asadmin_parallelGC\n";
    print " Recommended Value     : $asadmin_new_parallelGC\n\n";
	
    $asadmin_printclasshisto=getASJVMOptions("-XX:+PrintClassHistogram");
    #$asadmin_printclasshisto=&getASJVMOption("-XX:+PrintClassHistogram","flag",",");
    $asadmin_new_printclasshisto="-XX:+PrintClassHistogram";
	
    print " 12. Print Class Histogram\n";
    print " Current Value         : $asadmin_printclasshisto\n";
    print " Recommended Value     : $asadmin_new_printclasshisto\n\n";
	
    $asadmin_printGCTimeStamps=getASJVMOptions('-XX:+PrintGCTimeStamps');
    #$asadmin_printGCTimeStamps=&getASJVMOption('-XX:+PrintGCTimeStamps','flag',',');
    $asadmin_new_printGCTimeStamps="-XX:+PrintGCTimeStamps";

    print " 13. Print GC Time Stamps\n"; 
    print " Current Value 	      : $asadmin_printGCTimeStamps\n";
    print " Recommended Value     : $asadmin_new_printGCTimeStamps\n\n";
	
    $asadmin_useconcmarksweepgc=getASJVMOptions('-XX:+UseConcMarkSweepGC');
    #$asadmin_useconcmarksweepgc=&getASJVMOption('-XX:+UseConcMarkSweepGC','flag',',');
    $asadmin_new_useconcmarksweepgc="-XX:+UseConcMarkSweepGC";
    	
    print " 14. Enable Conc Mark Sweep GC\n";
    print " Current Value         : $asadmin_useconcmarksweepgc\n";
    print " Recommended Value     : $asadmin_new_useconcmarksweepgc\n\n";
    	
    if( $AMTUNE_WEB_CONTAINER_JAVA_POLICY eq false)
    {
    	$asadmin_serverpolicy=getASJVMOptions('-Djava.security.policy');
    	#$asadmin_serverpolicy=&getASJVMOption('-Djava.security.policy','nvp',',','=');
	$asadmin_new_serverpolicy="\${com.sun.aas.instanceRoot}/config/server.policy.NOTUSED";
		
	print " 15. Diable Server Security Policy checks\n";
	print " Current Value        : -Djava.security.policy=$asadmin_serverpolicy\n";
	print " Recommended Value    : -Djava.security.policy=$asadmin_new_serverpolicy\n";
    }
	

    print "\n";
	
    if($AMTUNE_MODE eq REVIEW)
    {
	return;
    }
	
    &backupConfigFile($tune_file);
	
    #---------------------------------------------------------------------------------------
    # Construct a parameter string to perform an asadmin set for acceptor-thread, queue-size, 
    # and count-thread parameters
    #---------------------------------------------------------------------------------------
        
    $asadmin_set_params="$acceptor_threads_string=$acceptorThreads";
    $asadmin_set_params="$asadmin_set_params $count_threads_string=$AMTUNE_NUM_TCP_CONN_SIZE";
    $asadmin_set_params="$asadmin_set_params $queue_size_string=$AMTUNE_NUM_TCP_CONN_SIZE";
        
    $ASADMIN_SET="set";
    @args=($ASADMIN,$ASADMIN_SET,@ASADMIN_COMMON_PARAMS_NO_TARGET,$asadmin_set_params);
    system("@args")==0 or die "\nError executing command\n";
    #`$ASADMIN set @ASADMIN_COMMON_PARAMS_NO_TARGET $asadmin_set_params`;
        
    #---------------------------------------------------------------------------------------
    # Delete current JVM Options
    #---------------------------------------------------------------------------------------
    $curJVMOptionString="";
    $curJVMOptionString=createJvmOptionString("$curJVMOptionString","$asadmin_min_heap");
    $curJVMOptionString=createJvmOptionString("$curJVMOptionString","$asadmin_max_heap");
    $curJVMOptionString=createJvmOptionString("$curJVMOptionString","$asadmin_loggc");
    #$curJVMOptionString=createJvmOptionString("$curJVMOptionString","$asadmin_serveroption");
    $curJVMOptionString=createJvmOptionString("$curJVMOptionString","$asadmin_stacksize");
    $curJVMOptionString=createJvmOptionString("$curJVMOptionString","$asadmin_newsize");
    $curJVMOptionString=createJvmOptionString("$curJVMOptionString","$asadmin_maxnewsize");
    $curJVMOptionString=createJvmOptionString("$curJVMOptionString","$asadmin_disableExplicitGC");
    $curJVMOptionString=createJvmOptionString("$curJVMOptionString","$asadmin_parallelGC");
    $curJVMOptionString=createJvmOptionString("$curJVMOptionString","$asadmin_useconcmarksweepgc");
    $curJVMOptionString=createJvmOptionString("$curJVMOptionString","$asadmin_printclasshisto");
    $curJVMOptionString=createJvmOptionString("$curJVMOptionString","$asadmin_printGCTimeStamps");
    $curJVMOptionString=createJvmOptionString("$curJVMOptionString","$asadmin_overrideDefaultLibthread");
    $curJVMOptionString=createJvmOptionString("$curJVMOptionString","$asadmin_parallel_gc_threads");
	
    if($AMTUNE_WEB_CONTAINER_JAVA_POLICY eq true)
    {	
	$curJVMOptionString=createJvmOptionString($curJVMOptionString,"-DJava.security.policy=$asadmin_serverpolicy");
    }
		
    if( $curJVMOptionString ne "")
    {
	deleteJVMOptionUsingASAdmin($curJVMOptionString);
    }
	
    #---------------------------------------------------------------------------------------
    # Insert new JVM Options
    #---------------------------------------------------------------------------------------
    $newJVMOptionString="";
    $newJVMOptionString=createJvmOptionString("$newJVMOptionString","$asadmin_new_min_heap");
    $newJVMOptionString=createJvmOptionString("$newJVMOptionString","$asadmin_new_max_heap");
    $newJVMOptionString=createJvmOptionString("$newJVMOptionString","$asadmin_new_loggc");
    #$newJVMOptionString=createJvmOptionString("$newJVMOptionString","$asadmin_new_serveroption");
    $newJVMOptionString=createJvmOptionString("$newJVMOptionString","$asadmin_new_stacksize");
    $newJVMOptionString=createJvmOptionString("$newJVMOptionString","$asadmin_new_newsize");
    $newJVMOptionString=createJvmOptionString("$newJVMOptionString","$asadmin_new_maxnewsize");
    $newJVMOptionString=createJvmOptionString("$newJVMOptionString","$asadmin_new_disableExplicitGC");
    $newJVMOptionString=createJvmOptionString("$newJVMOptionString","$asadmin_new_parallelGC");
    $newJVMOptionString=createJvmOptionString("$newJVMOptionString","$asadmin_new_useconcmarksweepgc");
    $newJVMOptionString=createJvmOptionString("$newJVMOptionString","$asadmin_new_printclasshisto");
    $newJVMOptionString=createJvmOptionString("$newJVMOptionString","$asadmin_new_printGCTimeStamps");
	
    if($AMTUNE_WEB_CONTAINER_JAVA_POLICY eq true)
    {
	$newJVMOptionString=createJvmOptionString($newJVMOptionString,"-Djava.security.policy=$asadmin_new_serverpolicy");
    }
	
    insertJVMOptionUsingASAdmin($newJVMOptionString);
}

sub tuneDomainXMLOld
{
     &substitute_token_in_line($tune_file,"acceptor_threads","acceptor_threads","$acceptorThreads",0,":");
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
    my @split_arr;
	
    @match=grep(m/$match/,@filecontent);
	
    foreach $i(@match)
    {
	@split_arr=split(m/=/,$i,2);
    }
	
    return $split_arr[1];
}

sub getASJVMOptions
{
    ($match)=@_;
    my @file;
    #print "match $match";
    #print @jvmfilecontent;
    foreach $i(@jvmfilecontent)
    {
	@match_temp=split(m/,/,$i);
	push(@file,@match_temp);
    }
	
    @match_array=grep(m/$match/,@file);
    @array=split(m/=/,$match_array[0],2);
    if(@match_array == @array)
    {
	return $array[0];
    }
    return $array[1];
}

#############################################################################
# Start of main program
#############################################################################

# import the environment
if(-f "$SCRIPT_LOCATION/amtune-env.pl")
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

&echo_msg("OpenSSO - Application Server Tuning Script\n");

open(FP,">$ASADMIN_PASSFILE");
print FP "$ASADMIN_PASSWORD_SYNTAX$ASADMIN_PASSWORD";
close(FP);
#open(FP,$ASADMIN_PASSFILE);
#while(<FP>)
#{
#     print $_;
#}
#close(FP);

tuneDomainXML;

#unlink($ASADMIN_PASSFILE);

&echo_msg($PARA_SEP);

