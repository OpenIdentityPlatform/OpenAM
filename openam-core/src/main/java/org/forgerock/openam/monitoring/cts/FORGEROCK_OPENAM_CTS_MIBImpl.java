/*
 * Copyright 2013-2014 ForgeRock AS.
 *
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
 */

package org.forgerock.openam.monitoring.cts;

import java.io.Serializable;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import org.forgerock.openam.cts.api.TokenType;

/**
 * The class is used for representing "FORGEROCK-OPENAM-CTS-MIB".
 * You can edit the file if you want to modify the behavior of the MIB.
 */
public class FORGEROCK_OPENAM_CTS_MIBImpl extends FORGEROCK_OPENAM_CTS_MIB implements Serializable {

    //CTS
    private CtsMonitoring ctsMonitoringGroup;

    private CtsConnectionSuccessRate ctsConnectionSuccessRate;
    private CtsConnectionFailureRate ctsConnectionFailureRate;

    /**
     * Default constructor. Initialize the Mib tree.
     */
    public FORGEROCK_OPENAM_CTS_MIBImpl() {
    }

    // ------------------------------------------------------------
    //
    // Initialization of the "CTSMonitoring" group.
    //
    // ------------------------------------------------------------

    /**
     * Factory method for "CtsMonitoring" group MBean.
     *
     * You can redefine this method if you need to replace the default
     * generated MBean class with your own customized class.
     *
     * @param groupName Name of the group ("SsoServerTopology")
     * @param groupOid  OID of this group
     * @param groupObjname ObjectName for this group (may be null)
     * @param server    MBeanServer for this group (may be null)
     *
     * @return An instance of the MBean class generated for the
     *         "CtsMonitoring" group (CtsMonitoringImpl)
     *
     * Note that when using standard metadata,
     * the returned object must implement the "CtsMonitoringMBean"
     * interface.
     **/
    @Override
    protected Object createCtsMonitoringMBean(
            String groupName,
            String groupOid,
            ObjectName groupObjname,
            MBeanServer server)
    {
        ctsMonitoringGroup = new CtsMonitoringImpl<OperationType, TokenType>(this, OperationType.class, TokenType.class);

        return ctsMonitoringGroup;
    }

    public CtsMonitoring getCtsMonitoringGroup() {
        return ctsMonitoringGroup;
    }

    public CtsConnectionSuccessRate getCtsConnectionSuccessRate() {
        return ctsConnectionSuccessRate;
    }

    public CtsConnectionFailureRate getCtsConnectionFailureRate() {
        return ctsConnectionFailureRate;
    }

    /**
     * Factory method for "CtsConnectionSuccessRate" group MBean.
     *
     * You can redefine this method if you need to replace the default
     * generated MBean class with your own customized class.
     *
     * @param groupName Name of the group ("CtsConnectionSuccessRate")
     * @param groupOid  OID of this group
     * @param groupObjname ObjectName for this group (may be null)
     * @param server    MBeanServer for this group (may be null)
     *
     * @return An instance of the MBean class generated for the
     *         "CtsConnectionSuccessRate" group (CtsConnectionSuccessRate)
     *
     * Note that when using standard metadata,
     * the returned object must implement the "CtsConnectionSuccessRateMBean"
     * interface.
     **/
    protected Object createCtsConnectionSuccessRateMBean(String groupName,
                                                         String groupOid,
                                                         ObjectName groupObjname, MBeanServer server)  {

        ctsConnectionSuccessRate = new CtsConnectionSuccessRateImpl(this);

        return ctsConnectionSuccessRate;
    }

    /**
     * Factory method for "CtsConnectionFailureRate" group MBean.
     *
     * You can redefine this method if you need to replace the default
     * generated MBean class with your own customized class.
     *
     * @param groupName Name of the group ("CtsConnectionFailureRate")
     * @param groupOid  OID of this group
     * @param groupObjname ObjectName for this group (may be null)
     * @param server    MBeanServer for this group (may be null)
     *
     * @return An instance of the MBean class generated for the
     *         "CtsConnectionFailureRate" group (CtsConnectionFailureRate)
     *
     * Note that when using standard metadata,
     * the returned object must implement the "CtsConnectionFailureRateMBean"
     * interface.
     **/
    protected Object createCtsConnectionFailureRateMBean(String groupName,
                                                         String groupOid,
                                                         ObjectName groupObjname, MBeanServer server)  {

        ctsConnectionFailureRate = new CtsConnectionFailureRateImpl(this);

        return ctsConnectionFailureRate;
    }

}
