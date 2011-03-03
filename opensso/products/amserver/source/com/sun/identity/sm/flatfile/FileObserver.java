/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: FileObserver.java,v 1.3 2008/06/25 05:44:08 qcheng Exp $
 *
 */

package com.sun.identity.sm.flatfile;

import com.iplanet.am.util.SystemProperties;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSObjectListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

class FileObserver extends Thread {
    private static Debug debug = Debug.getInstance("amSMSEvent");
    private Map snapShot;
    private int interval;
    private boolean running;
    private FlatFileEventManager eventManager;

    FileObserver(FlatFileEventManager eventManager) {
        setDaemon(true);
        getPollingInterval();
        this.eventManager = eventManager;
    }

    private void getPollingInterval() {
        String time = SystemProperties.get(
            Constants.CACHE_POLLING_TIME_PROPERTY);
        interval = Constants.DEFAULT_CACHE_POLLING_TIME;
        if (time != null) {
            try {
                interval = Integer.parseInt(time); 
            } catch (NumberFormatException nfe) {
                debug.error(
                    "FileObserver.getCachePollingInterval", nfe);
            }
        }
        interval = interval * 60 * 1000;
    }
    
    /**
     * Returns <code>true</code> if thread is running.
     *
     * @return <code>true</code> if thread is running.
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Stops the thread.
     */
    public void stopThread() {
        running = false;
    }
    
    public void run() {
        running = true;
        snapShot = getCurrentSnapShot();
        try {
            while (running) {
                /*
                 * This flag set to false in the begin of the thread.
                 * when a node is added/delete from the file system, we need
                 * to toggle this flag which in turn ask the 
                 * SMSEnhancedFlatFileObject to rebuild the directory tree.
                 */
                boolean needReloadRootNode = false;
                sleep(interval);
                Map newSnapShot = getCurrentSnapShot();

                if (snapShot != null) {
                    for (Iterator i = newSnapShot.keySet().iterator();
                        i.hasNext();
                    ) {
                        String filename = (String)i.next();
                        if (snapShot.containsKey(filename)) {
                            long prev =((Long)snapShot.get(filename))
                                .longValue();
                            long curr =((Long)newSnapShot.get(filename))
                                .longValue();
                            if (prev != curr) {
                                eventManager.notify(getDN(filename),
                                    SMSObjectListener.MODIFY);
                            }
                        } else {
                            if (!needReloadRootNode) {
                                eventManager.reloadRootNode();
                                needReloadRootNode = true;
                            }
                            eventManager.notify(getDN(filename),
                                SMSObjectListener.ADD);
                        }
                    }

                    for (Iterator i = snapShot.keySet().iterator();
                        i.hasNext();
                    ) {
                        String filename = (String)i.next();
    
                        if (!newSnapShot.containsKey(filename)) {
                            if (!needReloadRootNode) {
                                eventManager.reloadRootNode();
                                needReloadRootNode = true;
                            }
                            eventManager.notify(getDN(filename),
                                SMSObjectListener.DELETE);
                        }
                    }
                }
                snapShot = newSnapShot;
            }
        } catch (InterruptedException e) {
            debug.warning("FileObserver.run", e);
        }
    }

    private String getDN(String filename) {
        BufferedReader buff = null;
        String dn = null;

        try{
            buff = new BufferedReader(new FileReader(filename));
            String line = buff.readLine();
            if ((line != null) && line.startsWith("#")) {
                dn = line.substring(1);
            }
        } catch (IOException e) {
            debug.warning("FileObserver.getDN", e);
        } finally {
            if (buff != null) {
                try {
                    buff.close();
                } catch (IOException ex) {
                    //ignored
                }
            }
        }
        return dn;
    }

    private Map getCurrentSnapShot() {
        Map snapshot = null;
        String baseDir = SystemProperties.get(
            SMSFlatFileObjectBase.SMS_FLATFILE_ROOTDIR_PROPERTY);
        File dir = new File(baseDir);
        String[] files = dir.list();

        // if the war is not configured we may not get any files here.
        if (files.length > 0) {
            snapshot = new HashMap(files.length *2);
            for (int i = 0; i < files.length; i++) {
                String filename = baseDir + "/" + files[i];
                File f = new File(filename);
                if (!f.isDirectory()) {
                    snapshot.put(filename, new Long(f.lastModified()));
                }
            }
        }
        return snapshot;
    }
}
