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
 * $Id: FlatFileEventManager.java,v 1.3 2008/06/25 05:44:08 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.sm.flatfile;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.SMSObjectListener;
import com.sun.identity.sm.SMSUtils;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This registers the listeners for flat file depository.
 */
public class FlatFileEventManager {
    private static Debug debug = Debug.getInstance("amSMSEvent");
    private Map listeners = new HashMap();
    private FileObserver fObserver;
    private SMSEnhancedFlatFileObject flatFileInstance;

    public FlatFileEventManager(SMSEnhancedFlatFileObject flatFileInstance) {
        this.flatFileInstance = flatFileInstance;
    }

    synchronized String addObjectChangeListener(
        SMSObjectListener changeListener) {
        String id = SMSUtils.getUniqueID();
        listeners.put(id, changeListener);

        if (fObserver == null) {
            fObserver = new FileObserver(this);
            fObserver.start();
        } else {
            if (!fObserver.isRunning()) {
                fObserver.start();
            }
        }
        return (id);
    }

    synchronized void removeObjectChangeListener(String id) {
        listeners.remove(id);
        if (listeners.isEmpty()) {
            if (fObserver != null) {
                fObserver.stopThread();
            }
        }
    }

    void notify(String dn, int eventType) {
        if (dn != null) {
            for (Iterator i = listeners.values().iterator(); i.hasNext(); ) {
                SMSObjectListener l = (SMSObjectListener)i.next();
                l.objectChanged(dn, eventType);
            }
        }
    }
    
    void reloadRootNode() {
        try {
            flatFileInstance.loadMapper();
        } catch (SMSException e) {
            debug.error("FlatFileEventManager.reloadRootNode", e);
        }
    }
}
