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
 * $Id: Archiver.java,v 1.3 2008/06/25 05:43:39 qcheng Exp $
 *
 */

package com.sun.identity.log.spi;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import com.sun.identity.log.LogConstants;
import com.sun.identity.log.LogManager;
import com.sun.identity.log.LogManagerUtil;
import com.sun.identity.log.Logger;
import com.sun.identity.log.handlers.SecureFileHandler;

/**
 * This Archiver archives the files by timestamping them and keeping them in
 * the same directory. At any point of time, the file being written to is a
 * non-timestamped file. Each logger object is associated with an archiver.
 * Archiver also keeps track of the number of files being written to the present
 * key store. once it reaches a limit, a new key store has to be generated.
 * <p>
 * Summarizing, This Archiver main functionalities are to timeStamp the file and
 * store it in the same directory and to keep track of the number of files
 * written to the current key store.
 * <p>
 * Any other implementation to the archiver can be given, if some other form
 * of archival mechanism is required.
 */
public class Archiver {
    
    private static SimpleDateFormat sdf =
        new SimpleDateFormat("ddMMyyyyHHmmss");
    private static final String PREFIX = "_secure.";
    private int filesPerKeystoreCounter = 0;
    private static LogManager lmanager =
        (LogManager)LogManagerUtil.getLogManager();

    /**
     * Creates new Archiver
     */
    public Archiver() {
        String filesPerKeyStoreString =
            lmanager.getProperty(LogConstants.FILES_PER_KEYSTORE);
        if ((filesPerKeyStoreString == null) ||
            (filesPerKeyStoreString.length() == 0))
        {
            Debug.error(
                "Archiver:could not get the files per keystore string." +
                " Setting it to 1.");
            filesPerKeyStoreString = "1";
        }
    }
    
    /**
     * This method generates a Date object, formatting according to
     * the "DDMMyyyyHHmmss" format and saves the files in the same directory.
     * <p>
     * also does some book keeping operations.
     *
     * @param fileName name of the archive file.
     * @param location location of the archive file.
     */
    public void archive(String fileName, String location) {
        if ((fileName == null) || (fileName.length() == 0)) {
            Debug.error("Archiver:archive:FileName is null");
            return;
        } else if ((location == null) || (location.length() == 0)) {
            Debug.error("Archiver:archive:Location is null");
            return;
        }
        Logger logger =
            (com.sun.identity.log.Logger)Logger.getLogger(fileName);
        filesPerKeystoreCounter ++;
        Date d = new Date();
        
        String timestampedFileName = location + PREFIX + fileName + "." +
            sdf.format(d).toString();
        String completePath = location + PREFIX + fileName;
        File f = new File(completePath);
        f.renameTo (new File(timestampedFileName));
        SecureFileHandler.addToCurrentFileList(fileName,
            fileName + "." + sdf.format(d).toString(), fileName);
        return;
    }
    
    /**
     *  Returns the current count of the archival.
     */
    public int checkCount() {
        return filesPerKeystoreCounter;
    }
    
    /**
     *  Increment the current count of the archival.
     */
    public void incrementCount() {
        filesPerKeystoreCounter++;
    }

    /**
     * Archives the keystore after the specified number of files have been
     * used with this keystore.
     * Archives according to the name of the last archives file in the
     * list of files used with this keystore.
     *
     * @param logName Name of the log which is to be archived.
     * @param location The location of the keystores.
     */
    public void archiveKeyStore(String logName, String location) {
        Logger logger = (com.sun.identity.log.Logger)Logger.getLogger(logName);
        ArrayList al = SecureFileHandler.getCurrentFileList(logName);

        /*
         * The -2 is to get the size and then pick the second last
         * element in the list.
         */
        String ts = ((String)al.get(al.size() - 2 )).
            substring(((String)al.get(al.size() - 2)).lastIndexOf("."));
        if (Debug.messageEnabled()) {
            Debug.message(
                "Archive:archiveKeyStore:Keystore timestamp = " + ts);
        }
        String LogKeyStoreArchiveName =
            location + PREFIX + "log." + logName + ts;
        String VerKeyStoreArchiveName =
            location + PREFIX + "ver." + logName + ts;
        String logKeyStoreOldName = location + PREFIX + "log." + logName;
        String verKeyStoreOldName = location + PREFIX + "ver." + logName;
        File logKeystore = new File(logKeyStoreOldName);
        logKeystore.renameTo(new File(LogKeyStoreArchiveName));
        File verKeystore = new File(verKeyStoreOldName);
        verKeystore.renameTo(new File(VerKeyStoreArchiveName));
        filesPerKeystoreCounter = 0;
    }
}
