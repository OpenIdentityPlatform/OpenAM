# Copyright © 2006 Sun Microsystems, Inc.  All rights reserved.
#
# DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
#
# Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
# $Id: amsfo.pl,v 1.7 2009/05/07 06:58:43 kanduls Exp $
#

### To Debug this script set AMDEBUG to true ####
use File::Path;
use Win32;
use Win32::Process;


############ Initialize configuration properties############
if(@ARGV < 2 || @ARGV > 2) 
{
	print "Usage: amsfo.pl <configuration file name> start|stop";
        print "\n";
        print "\n";
        print "Example Usage: amsfo.pl c:/sun/AccessManager/lib/amsfo.conf start";
        print "\n";
        exit 1;
}
my $configFile = $ARGV[0];
my $debug = "true";
my %prop = ();
open(CONFIGFILE, $configFile) || die ("Cannot open input file $configFile");
my @infile = <CONFIGFILE>;
chomp (@infile);
my $line;
my $key;
my $value;
foreach $line (@infile)
{
   if ($line !~ /^#/) 
       {
         ($key, $value) = split (/\=/, $line);
          $prop{$key} = $value;
       }
}
my $am_bin_dir = "$prop{'AM_HOME_DIR'}/bin";
my $jmq_bin_dir = "$prop{'JMQ_INSTALL_DIR'}/bin";
my $jmq_shutdown_exe = "$jmq_bin_dir/imqcmd.exe";
my $jmq_pid_file = "$prop{'LOG_DIR'}/jmq.pid";
my $am_pid_file = "$prop{'LOG_DIR'}/amdb.pid";
my $jmqExecutable = "$jmq_bin_dir/imqbrokerd.exe";
my $broker_options = "-silent";
my $am_sfo_restart = $prop{'AM_SFO_RESTART'};
my $log_dir = $prop{'LOG_DIR'};
my $cluster_list = $prop{'CLUSTER_LIST'};
my $user_name = $prop{'USER_NAME'};
my $passwordfile = $prop{'PASSWORDFILE'};
my $database_dir = $prop{'DATABASE_DIR'};
my $amsessiondb_args = $prop{'AMSESSIONDB_ARGS'};
my $broker_vm_args = $prop{'BROKER_VM_ARGS'};
my $broker_port  = $prop{'BROKER_PORT'};
my $broker_instance_name = $prop{'BROKER_INSTANCE_NAME'};
my $jmq_password = $prop{'JMQ_PASSWORD'};
my $delete_database_dir = $prop{'DELETE_DATABASE'};
sub get_pid {
	my $pid_file = $_[0];
        # Open the pid file and read the pid
        print("the pid file is: $pid_file \n");
        open(PID_FILE, "< $pid_file");
        my $pid = <PID_FILE>;
        chomp($pid);
        close(PID_FILE);
        return $pid;
}
sub stop_jmq() {
       if ( $debug eq "true" ) 
       {
           print("stopping JMQ Broker..", "\n");
       }
       if (-f $jmq_pid_file) {
           $jmq_pid = get_pid($jmq_pid_file);
           print("Shutting the pid: $jmq_pid", "\n");
           $ret = kill(-9, $jmq_pid);
           if ($ret ne 0) {
	      	print("JMQ Broker is shutdown ", "\n");
	      	unlink($jmq_pid_file);
	   } else {
	     	print("Error stopping JMQ broker ", "\n");
	   }
		   #$ENV{IMQ_JAVAHOME} = $prop{'JAVA_HOME'};
           #my @shutdownCmd = ("$jmq_shutdown_exe", "shutdown", "bkr", "-f", "-b", "localhost:$broker_port", "-u", "admin", "-p", "$jmq_password");
           #exec(@shutdownCmd) or print STDERR "couldn't exec shutdown command: $!";
       }else {
           if ($debug eq "true") {
              print("JMQ Broker not running", "\n");
           }
           exit(1);
	   }
}
sub start_jmq {

    	mkpath($log_dir);

    	if ($debug eq "true") 
    	{
	    print("starting JMQ Broker", "\n");
	    }
    
    	if ($am_sfo_restart eq "true") 
    	{
    	    $broker_options="$broker_options";
    	}
        
    	$_jmqpid="";
            
    	if (-f $jmq_pid_file) 
    	{
            $brok_pid = get_pid($jmq_pid_file);
            $brok_status=Win32::Process::Open($obj,$brok_pid,$iflags);
    	}
    	else {
    	    $brok_status="";
    	}

    	if ($brok_status ne "") 
    	{
            if ($debug eq "true") 
            {
            	print("JMQ Broker is already running.", "\n");
            }
    	}
    	else {
    	    if ($debug eq "true") 
    	    {
            	print(" $jmqExecutable -bgnd $broker_options -vmargs $broker_vm_args -name $broker_instance_name -port $broker_port -cluster $cluster_list", "\n");
            }
            $ENV{IMQ_JAVAHOME} = $prop{'JAVA_HOME'};
            $jmq_args = "-bgnd $broker_options -vmargs $broker_vm_args -name $broker_instance_name -port $broker_port -cluster $cluster_list";
            $ret = Win32::Process::Create($ProcessObj,$jmqExecutable,$jmq_args,0,NORMAL_PRIORITY_CLASS,".");
            $_jmqpid = $ProcessObj->GetProcessID();
            # Open the pid file for writing
            open(PID_FILE, "> $jmq_pid_file");
            print(PID_FILE $_jmqpid);
            close(PID_FILE);
    	}
}   
    
sub start_am {
	mkpath($log_dir);
        if ($debug eq "true" ) 
        {
            print("starting amsessiondb client", "\n");
        }
        # Check if the server is already running.
        $_amqpid="";
        $amdb_status="";
        if (-f $am_pid_file) {
            $amdb_pid = get_pid($am_pid_file);
            $amdb_status=Win32::Process::Open($obj,$amdb_pid,$iflags);
        }
        else {
            $amdb_status="";          
        }
        if ($amdb_status ne "") {
            if ($debug eq "true") {
                print("AM Session DB client is already running.", "\n");
            }
        }
        else {
            if ($delete_database_dir eq "true") {
                File::Path::rmtree($database_dir);
            }
            my $java_home=$prop{'JAVA_HOME'};
	    my $imq_jar_path="$prop{'JMQ_INSTALL_DIR'}/lib";
	    my $jms_jar_path="$prop{'JMQ_INSTALL_DIR'}/lib";
	    my $bdb_jar_path="$prop{'AM_HOME_DIR'}/../share/lib";
	    my $classpath="$imq_jar_path/imq.jar;$jms_jar_path/jms.jar;$prop{'AM_HOME_DIR'}/ext/je.jar;$prop{'AM_HOME_DIR'}/locale;$prop{'AM_HOME_DIR'}/lib/am_sessiondb.jar;.";
	    my $java_opts="";
	    my $amExecutable = "$java_home/bin/java.exe";
	    my $cmd_args = " -classpath \"$classpath\" com.sun.identity.ha.jmqdb.client.FAMHaDB -a $cluster_list -u $user_name -f $passwordfile -b $database_dir $amsessiondb_args -m $configFile";
            if ($debug eq "true") {
                print("$amExecutable $cmd_args\n");
            }                       
            # creates dir  by  creating all  the  non-existing  parent directories first.
    	    mkpath($database_dir);
            $ret = Win32::Process::Create($amProcessObj,$amExecutable,
                $cmd_args,0,NORMAL_PRIORITY_CLASS,".") || 
                die print Win32::FormatMessage( Win32::GetLastError() );
            $_ampid = $amProcessObj->GetProcessID();
            # Open the pid file for writing
            open(pid_file, "> $am_pid_file");
            print(pid_file $_ampid);
            close(pid_file);
        }
}
sub stop_am() {
	if ($debug eq "true") {
            print("stopping amsessiondb client.\n");
        }
        if (-f $am_pid_file) {
            $am_pid = get_pid($am_pid_file);
            print("Shutting the pid: $am_pid", "\n");
            $? = kill(9, $am_pid);
            print("amsessiondb is shutdown", "\n");
            unlink($am_pid_file);
        }
        else {
            if ($debug eq "true") {
                print("amsessiondb not running", "\n");
            }
        }
}

############ Start of main ############

if ($ARGV[1] eq 'start') {
    if ($prop{'START_BROKER'} eq "true") {
         start_jmq();
         ## Wait for 5 sec for the broker to start ##
         sleep(10);
    }
    start_am();
    sleep(10);
}elsif($ARGV[1] eq 'stop') {
    stop_am();
    ## Wait untill the AMDB shuts down properly ##
    if ($prop{'START_BROKER'} eq "true") {
        sleep(10);
        stop_jmq();
    }
}elsif($ARGV[1] eq 'start-jmq') {
         start_jmq();
         sleep(10);
}elsif($ARGV[1] eq 'stop-jmq') {
         stop_jmq();
         sleep(10);
}elsif($ARGV[1] eq 'start-amsdb') {
         start_am();
         sleep(10);
}elsif($ARGV[1] eq 'stop-amsdb') {
         stop_am();
         sleep(10);
}else {
	print "Usage: amsfo.pl <configuration file name> start|stop|start-jmq|stop-jmq|start-amsdb|stop-amsdb";
        print "\n";
        print "\n";
        print "Example Usage: amsfo.pl c:/sun/AccessManager/lib/amsfo.conf start";
        print "\n";
        exit 1;
}

        
