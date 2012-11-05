/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: FAMPersisterManager.java,v 1.3 2008/07/28 21:21:23 pawand Exp $
 *
 */

/*
 * Portions Copyrighted [2010] [ForgeRock AS]
 */

package com.sun.identity.ha;

import com.sun.identity.shared.debug.Debug;
import com.iplanet.dpro.session.service.SessionService;
import com.sun.identity.shared.configuration.SystemPropertiesManager;

/**
 * FAMPersisterFactory
 */
public class FAMPersisterManager {
    private static FAMRecordPersister recordPesister = null; 
    private static FAMPersisterManager instance = null;
    private static String famRecordPersisterImpl = null; 
    private static final String PERSISTER_KEY = 
        "com.sun.identity.ha.famrecordpersister"; 
    private static final String DEFAULT_PERSISTER_VALUE = 
        "com.sun.identity.ha.jmqdb.FAMRecordJMQPersister";
    private static Debug debug = SessionService.sessionDebug;

    static {
        try {
            famRecordPersisterImpl = SystemPropertiesManager.get(PERSISTER_KEY,
                DEFAULT_PERSISTER_VALUE);
        } catch (Exception e) {
            famRecordPersisterImpl = DEFAULT_PERSISTER_VALUE;
        }         
    }

    private FAMPersisterManager() throws Exception {
        recordPesister = (FAMRecordPersister) Class.forName(
            famRecordPersisterImpl).newInstance();        
    } 
    
    public synchronized static FAMPersisterManager getInstance() 
        throws Exception{
        if (instance == null) {
            instance = new FAMPersisterManager();           
        }
        return instance; 
    }
   
    public static FAMRecordPersister getFAMRecordPersister() {
        return recordPesister; 
    }

    public synchronized static void clearInstance() {
        try {
            recordPesister.close();
        } catch (Exception ex) {
            debug.error("FAMPersisterManager::clearInstance unable to close instancee", ex);
        }

        instance = null;
    }
}
