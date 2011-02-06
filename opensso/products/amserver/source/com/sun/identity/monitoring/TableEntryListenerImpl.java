/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: TableEntryListenerImpl.java,v 1.2 2009/10/21 00:03:15 bigfatrat Exp $
 *
 */

package com.sun.identity.monitoring;

import javax.management.Notification;
import javax.management.NotificationListener;
import com.sun.management.snmp.agent.SnmpTableEntryNotification;
import com.sun.management.snmp.agent.SnmpMibTable;

/**
 * This class receives SnmpTableEntryNotifications when an entry 
 * is added to or removed from the "Table".  
 */

public class TableEntryListenerImpl implements NotificationListener {

    public  void handleNotification(Notification notification,
                                    Object handback) {
        
        SnmpTableEntryNotification notif =
            (SnmpTableEntryNotification) notification;
        SnmpMibTable table = (SnmpMibTable) notif.getSource();
        String type = notif.getType();
        
        try {
            if (type.equals(SnmpTableEntryNotification.SNMP_ENTRY_ADDED)) {
                java.lang.System.out.println("NOTE: TableEntryListenerImpl " +
                                             "received event \"Entry added\":");
                SsoServerAuthModulesEntryImpl added =
                    (SsoServerAuthModulesEntryImpl) notif.getEntry();
                java.lang.System.out.println(
                    "    ModuleName = " +
                    added.getAuthModuleName());
                java.lang.System.out.println(
                    "    FailureCount = " +
                    added.getAuthModuleFailureCount());
                java.lang.System.out.println(
                    "    SuccessCount = " +
                    added.getAuthModuleSuccessCount());
            } else if (type.equals(
                            SnmpTableEntryNotification.SNMP_ENTRY_REMOVED))
            {
                java.lang.System.out.println("NOTE: TableEntryListenerImpl " +
                                             "received event " +
                                             "\"Entry removed\":");
            } else {
                java.lang.System.out.println(
                    "\n    >> Unknown event type (?)\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
            java.lang.System.exit(1);
        }
    }
}
