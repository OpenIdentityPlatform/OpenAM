/*
 * Copyright 2014 ForgeRock AS.
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

package org.forgerock.openam.monitoring.policy;

import java.io.Serializable;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * The class is used for representing "FORGEROCK-OPENAM-POLICY-MIB".
 */
public class FORGEROCK_OPENAM_POLICY_MIBImpl extends FORGEROCK_OPENAM_POLICY_MIB implements Serializable {

    //Policy
    private SelfEvaluation selfPolicyEvaluation;
    private SubtreeEvaluation subtreePolicyEvaluation;
    private SelfTiming selfPolicyTiming;
    private SubtreeTiming subtreePolicyTiming;

    private PolicyEvaluation policyEvaluation;

    /**
     * Default constructor. Initialize the Mib tree.
     */
    public FORGEROCK_OPENAM_POLICY_MIBImpl() {
    }

    /**
     * Factory method for "SelfEvaluation" MBean.
     *
     * You can redefine this method if you need to replace the default
     * generated MBean class with your own customized class.
     *
     * @param groupName Name of the group ("SelfEvaluation")
     * @param groupOid  OID of this group
     * @param groupObjname ObjectName for this group (may be null)
     * @param server    MBeanServer for this group (may be null)
     *
     * @return An instance of the MBean class generated for the
     *         "SelfEvaluation" group (SelfEvaluation)
     *
     * Note that when using standard metadata,
     * the returned object must implement the "SelfEvaluation"
     * interface.
     **/
    protected Object createSelfEvaluationMBean(String groupName,
                                                         String groupOid,
                                                         ObjectName groupObjname, MBeanServer server)  {

        selfPolicyEvaluation = new SelfEvaluationImpl(this);
        return selfPolicyEvaluation;
    }

    /**
     * Factory method for "SubtreeEvaluation" MBean.
     *
     * You can redefine this method if you need to replace the default
     * generated MBean class with your own customized class.
     *
     * @param groupName Name of the group ("SubtreeEvaluation")
     * @param groupOid  OID of this group
     * @param groupObjname ObjectName for this group (may be null)
     * @param server    MBeanServer for this group (may be null)
     *
     * @return An instance of the MBean class generated for the
     *         "SubtreeEvaluation" group (SubtreeEvaluation)
     *
     * Note that when using standard metadata,
     * the returned object must implement the "SubtreeEvaluation"
     * interface.
     **/
    protected Object createSubtreeEvaluationMBean(String groupName,
                                               String groupOid,
                                               ObjectName groupObjname, MBeanServer server)  {

        subtreePolicyEvaluation = new SubtreeEvaluationImpl(this);
        return subtreePolicyEvaluation;

    }

    public SelfEvaluation getSelfEvaluation() {
        return selfPolicyEvaluation;
    }

    public SubtreeEvaluation getSubtreeEvaluation() {
        return subtreePolicyEvaluation;
    }

    /**
     * Factory method for "SelfTiming" MBean.
     *
     * You can redefine this method if you need to replace the default
     * generated MBean class with your own customized class.
     *
     * @param groupName Name of the group ("SelfTiming")
     * @param groupOid  OID of this group
     * @param groupObjname ObjectName for this group (may be null)
     * @param server    MBeanServer for this group (may be null)
     *
     * @return An instance of the MBean class generated for the
     *         "SelfTiming" group (SelfTiming)
     *
     * Note that when using standard metadata,
     * the returned object must implement the "SelfTiming"
     * interface.
     **/
    protected Object createSelfTimingMBean(String groupName,
                                                 String groupOid,
                                                 ObjectName groupObjname, MBeanServer server)  {

        selfPolicyTiming = new SelfTimingImpl(this);

        return selfPolicyTiming;
    }

    /**
     * Factory method for "SubtreeTiming" MBean.
     *
     * You can redefine this method if you need to replace the default
     * generated MBean class with your own customized class.
     *
     * @param groupName Name of the group ("SubtreeTiming")
     * @param groupOid  OID of this group
     * @param groupObjname ObjectName for this group (may be null)
     * @param server    MBeanServer for this group (may be null)
     *
     * @return An instance of the MBean class generated for the
     *         "SubtreeTiming" group (SubtreeTiming)
     *
     * Note that when using standard metadata,
     * the returned object must implement the "SubtreeTiming"
     * interface.
     **/
    protected Object createSubtreeTimingMBean(String groupName,
                                             String groupOid,
                                             ObjectName groupObjname, MBeanServer server)  {

        subtreePolicyTiming = new SubtreeTimingImpl(this);

        return subtreePolicyTiming;
    }

    public SelfTiming getSelfTiming() {
        return selfPolicyTiming;
    }

    public SubtreeTiming getSubtreeTiming() {
        return subtreePolicyTiming;
    }

    /**
     * Factory method for "PolicyEvaluation" MBean.
     *
     * You can redefine this method if you need to replace the default
     * generated MBean class with your own customized class.
     *
     * @param groupName Name of the group ("PolicyEvaluation")
     * @param groupOid  OID of this group
     * @param groupObjname ObjectName for this group (may be null)
     * @param server    MBeanServer for this group (may be null)
     *
     * @return An instance of the MBean class generated for the
     *         "PolicyEvaluation" group (PolicyEvaluation)
     *
     * Note that when using standard metadata,
     * the returned object must implement the "PolicyEvaluation"
     * interface.
     **/
    protected Object createPolicyEvaluationMBean(String groupName,
                                                  String groupOid,
                                                  ObjectName groupObjname, MBeanServer server)  {

        policyEvaluation = new PolicyEvaluationImpl(this);
        return policyEvaluation;

    }

    public PolicyEvaluation getPolicyEvaluation(){
        return policyEvaluation;
    }

}
