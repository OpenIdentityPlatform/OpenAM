/*
* The contents of this file are subject to the terms of the Common Development and
* Distribution License (the License). You may not use this file except in compliance with the
* License.
*
* You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
* specific language governing permission and limitations under the License.
*
* When distributing Covered Software, include this CDDL Header Notice in each file and include
* the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
* Header, with the fields enclosed by brackets [] replaced by your own identifying
* information: "Portions copyright [year] [name of copyright owner]".
*
* Copyright 2014 ForgeRock AS.
*/
package org.forgerock.openam.monitoring.session;

import com.iplanet.dpro.session.monitoring.SessionMonitoringStore;
import java.io.Serializable;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.cts.monitoring.impl.persistence.CtsPersistenceOperationsMonitor;

/**
 * The class is used for representing "FORGEROCK-OPENAM-SESSION-MIB".
 */
public class FORGEROCK_OPENAM_SESSION_MIBImpl extends FORGEROCK_OPENAM_SESSION_MIB implements Serializable {

    //Session
    private CtsSessions ctsSessions;
    private InternalSessions internalSessions;
    private RemoteSessions remoteSessions;


    /**
     * Default constructor. Initialize the Mib tree.
     */
    public FORGEROCK_OPENAM_SESSION_MIBImpl() {
    }

    /**
     * Factory method for "CtsSessions" MBean.
     *
     * You can redefine this method if you need to replace the default
     * generated MBean class with your own customized class.
     *
     * @param groupName Name of the group ("CtsSessions")
     * @param groupOid  OID of this group
     * @param groupObjname ObjectName for this group (may be null)
     * @param server    MBeanServer for this group (may be null)
     *
     * @return An instance of the MBean class generated for the
     *         "CtsSessions" group (CtsSessions)
     *
     * Note that when using standard metadata,
     * the returned object must implement the "CtsSessions"
     * interface.
     **/
    protected Object createCtsSessionsMBean(String groupName,
                                               String groupOid,
                                               ObjectName groupObjname, MBeanServer server)  {

        ctsSessions = new CtsSessionsImpl(InjectorHolder.getInstance(CtsPersistenceOperationsMonitor.class), this,
                InjectorHolder.getInstance(SessionMonitoringStore.class));
        return ctsSessions;
    }

    public CtsSessions getCtsSessions() {
        return ctsSessions;
    }

    /**
     * Factory method for "InternalSessions" MBean.
     *
     * You can redefine this method if you need to replace the default
     * generated MBean class with your own customized class.
     *
     * @param groupName Name of the group ("InternalSessions")
     * @param groupOid  OID of this group
     * @param groupObjname ObjectName for this group (may be null)
     * @param server    MBeanServer for this group (may be null)
     *
     * @return An instance of the MBean class generated for the
     *         "InternalSessions" group (InternalSessions)
     *
     * Note that when using standard metadata,
     * the returned object must implement the "InternalSessions"
     * interface.
     **/
    protected Object createInternalSessionsMBean(String groupName,
                                                  String groupOid,
                                                  ObjectName groupObjname, MBeanServer server)  {

        internalSessions = new InternalSessionsImpl(this, InjectorHolder.getInstance(SessionMonitoringStore.class));
        return internalSessions;

    }

    public InternalSessions getInternalSessions() {
        return internalSessions;
    }

    /**
     * Factory method for "RemoteSessions" MBean.
     *
     * You can redefine this method if you need to replace the default
     * generated MBean class with your own customized class.
     *
     * @param groupName Name of the group ("RemoteSessions")
     * @param groupOid  OID of this group
     * @param groupObjname ObjectName for this group (may be null)
     * @param server    MBeanServer for this group (may be null)
     *
     * @return An instance of the MBean class generated for the
     *         "RemoteSessions" group (RemoteSessions)
     *
     * Note that when using standard metadata,
     * the returned object must implement the "RemoteSessions"
     * interface.
     **/
    protected Object createRemoteSessionsMBean(String groupName,
                                           String groupOid,
                                           ObjectName groupObjname, MBeanServer server)  {

        remoteSessions = new RemoteSessionsImpl(this, InjectorHolder.getInstance(SessionMonitoringStore.class));

        return remoteSessions;
    }

    public RemoteSessions getRemoteSessions() {
        return remoteSessions;
    }

}
