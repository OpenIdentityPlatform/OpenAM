/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: VerifierList.java,v 1.5 2008/06/25 05:43:38 qcheng Exp $
 *
 */



package com.sun.identity.log.secure;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.TreeMap;
import java.util.Vector;

import com.sun.identity.log.util.LogFileFilter;

/**
 *  Implements a class for getting the list of keyfiles and the associated
 *  logfiles to be used when verifying archives of logs.
 */
public class VerifierList {
    /**
     *   Returns the list of keyfiles and associated logfiles
     *   in a treemap structure.
     *
     *   @param dir path to the location of the files.
     *   @param filter the filter to be used in searching.
     *   @return a treemap of the keyfiles and associates logfiles.
     */
    public TreeMap getKeysAndFiles(File dir, String filter) {
        TreeMap tm = new TreeMap( new Comparator() {
            public int compare(Object o1, Object o2) {
                return ((String)o1).compareTo((String) o2);
            }
        }
        );
        
        String[] keyFiles = 
                 getSortedKeyFileNames(dir, "_secure.log."+filter+"*");
        
        // Once key files are got sorted, get the corresponding log files
        for (int i = keyFiles.length - 1; i >= 0 ; i--) {
//          System.out.println("KeyFile ="+keyFiles[i]);
            Vector logFiles = null;
            if(i > 0 ) {
                logFiles = 
                    getLogFilesForKey(dir, filter, keyFiles[i], keyFiles[i-1]);
            } else {
                logFiles = getLogFilesForKey(dir, filter, keyFiles[i], null);
                /* This is to account for files whose timestamp is greater
                 * than the last timestamped keystore which belong to 
                 * the current set.
                 */
                
                SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyyHHmmss");
                Date d = new Date();
                logFiles.addAll(getLogFilesForKey(dir, filter,
                                "_secure.log." + filter + "." + sdf.format(d), 
                                keyFiles[keyFiles.length - 1]));
            }
            //Printout the results received:
            tm.put(keyFiles[i], logFiles);
        }
        return tm;
    }
    
    /**
     *  Sort the files and return.
     *
     *  @param  dir path to the files.
     *  @param  basestring the filter to be used for selecting files.
     *  @return String array of the sorted results.
     */
    public String[] getSortedKeyFileNames(File dir, String basestring) {
        // base string is where you would pass key.logname.logtype
        String[] keyFiles = dir.list(new LogFileFilter(basestring) );
        Arrays.sort(keyFiles);
        return keyFiles;
    }
    
    /**
     *  Returns the logfiles associated with the given keyfile with reference
     *  to the next keyfile.
     *
     *  @param dir  path to the location of the log files.
     *  @param log  The filter on which the search is to be applied.
     *  @param key1 The key on which the search is conducted and assocaited.
     *  @param key2 The key against which the search is conducted.
     *  @return a Vector of the results of the search in sorted order.
     */
    public Vector getLogFilesForKey(
        File dir,
        String log, 
        String key1,
        String key2
    ) {
        Vector logList = new Vector();
        // get the list of logfiles for a log+type

        String[] logFiles = dir.list(new LogFileFilter("_secure."+log+"*") );
        Arrays.sort(logFiles);

        // find the logfiles for a given key file
        String startLog = "_secure." + key1.substring( key1.indexOf(log) );
        String endLog = null;
        if(key2 != null) {
            endLog = "_secure." + key2.substring( key2.indexOf(log) );
        }
        
        for(int i = 0; i < logFiles.length; i++) {
            if( startLog.compareTo( logFiles[i] ) >= 0) {
                if( endLog == null) {
                    logList.add(logFiles[i]);
                } else if( endLog.compareTo( logFiles[i] ) < 0) {
                    logList.add(logFiles[i]);
                }
            }
        }
        return logList;
    }
}
