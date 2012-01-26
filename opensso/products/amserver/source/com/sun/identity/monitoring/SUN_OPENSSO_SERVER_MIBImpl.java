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
 * $Id: SUN_OPENSSO_SERVER_MIB.java,v 1.2 2009/10/20 23:53:12 bigfatrat Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */
package com.sun.identity.monitoring;

import java.io.Serializable;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * The class is used for representing "SUN-OPENSSO-SERVER-MIB".
 * You can edit the file if you want to modify the behavior of the MIB.
 */
public class SUN_OPENSSO_SERVER_MIBImpl extends SUN_OPENSSO_SERVER_MIB implements Serializable {

    private SsoServerInstanceImpl instanceGroup;
    private SsoServerAuthSvcImpl authSvcGroup;
    private SsoServerSessSvcImpl sessSvcGroup;
    private SsoServerLoggingSvcImpl loggingSvcGroup;
    private SsoServerPolicySvcImpl policySvcGroup;
    private SsoServerIdRepoSvcImpl idrepoSvcGroup;
    private SsoServerSvcMgmtSvcImpl svcmgmtSvcGroup;
    private SsoServerSAML1SvcImpl saml1SvcGroup;
    private SsoServerSAML2SvcImpl saml2SvcGroup;
    private SsoServerIDFFSvcImpl idffSvcGroup;
    private SsoServerTopologyImpl topologyGroup;
    private SsoServerWSSAgentsImpl wssAgentsGroup;
    private SsoServerFedCOTsImpl fedCotsGroup;
    private SsoServerPolicyAgentsImpl policyAgentsGroup;
    private SsoServerFedEntitiesImpl fedEntitiesGroup;
    private SsoServerEntitlementSvcImpl entitlementsGroup;
    private SsoServerConnPoolSvcImpl connPoolGroup;

    /**
     * Default constructor. Initialize the Mib tree.
     */
    public SUN_OPENSSO_SERVER_MIBImpl() {
    }

    // ------------------------------------------------------------
    // 
    // Initialization of the "SsoServerInstance" group.
    // 
    // ------------------------------------------------------------

    /**
     * Factory method for "SsoServerInstance" group MBean.
     * 
     * You can redefine this method if you need to replace the default
     * generated MBean class with your own customized class.
     * 
     * @param groupName Name of the group ("SsoServerInstance")
     * @param groupOid  OID of this group
     * @param groupObjname ObjectName for this group (may be null)
     * @param server    MBeanServer for this group (may be null)
     * 
     * @return An instance of the MBean class generated for the
     *         "SsoServerInstance" group (SsoServerInstance)
     * 
     * Note that when using standard metadata,
     * the returned object must implement the "SsoServerInstanceMBean"
     * interface.
     **/
    @Override
    protected Object createSsoServerInstanceMBean(
        String groupName,
        String groupOid,
        ObjectName groupObjname,
        MBeanServer server)
    {
        // Note that when using standard metadata,
        // the returned object must implement the "SsoServerInstanceMBean"
        // interface.
        //
        if (server != null) 
            instanceGroup = new SsoServerInstanceImpl(this,server);
        else 
            instanceGroup = new SsoServerInstanceImpl(this);

        return instanceGroup;
    }


    // ------------------------------------------------------------
    // 
    // Initialization of the "SsoServerFedEntities" group.
    // 
    // ------------------------------------------------------------

    /**
     * Factory method for "SsoServerFedEntities" group MBean.
     * 
     * You can redefine this method if you need to replace the default
     * generated MBean class with your own customized class.
     * 
     * @param groupName Name of the group ("SsoServerFedEntities")
     * @param groupOid  OID of this group
     * @param groupObjname ObjectName for this group (may be null)
     * @param server    MBeanServer for this group (may be null)
     * 
     * @return An instance of the MBean class generated for the
     *         "SsoServerFedEntities" group (SsoServerFedEntities)
     * 
     * Note that when using standard metadata,
     * the returned object must implement the "SsoServerFedEntitiesMBean"
     * interface.
     **/
    @Override
    protected Object createSsoServerFedEntitiesMBean(
        String groupName,
        String groupOid,
        ObjectName groupObjname,
        MBeanServer server)
    {

        // Note that when using standard metadata,
        // the returned object must implement the "SsoServerFedEntitiesMBean"
        // interface.
        //
        if (server != null) 
            fedEntitiesGroup = new SsoServerFedEntitiesImpl(this,server);
        else 
            fedEntitiesGroup = new SsoServerFedEntitiesImpl(this);

        return fedEntitiesGroup;
    }


    // ------------------------------------------------------------
    // 
    // Initialization of the "SsoServerFedCOTs" group.
    // 
    // ------------------------------------------------------------

    /**
     * Factory method for "SsoServerFedCOTs" group MBean.
     * 
     * You can redefine this method if you need to replace the default
     * generated MBean class with your own customized class.
     * 
     * @param groupName Name of the group ("SsoServerFedCOTs")
     * @param groupOid  OID of this group
     * @param groupObjname ObjectName for this group (may be null)
     * @param server    MBeanServer for this group (may be null)
     * 
     * @return An instance of the MBean class generated for the
     *         "SsoServerFedCOTs" group (SsoServerFedCOTs)
     * 
     * Note that when using standard metadata,
     * the returned object must implement the "SsoServerFedCOTsMBean"
     * interface.
     **/
    @Override
    protected Object createSsoServerFedCOTsMBean(
        String groupName,
        String groupOid,
        ObjectName groupObjname,
        MBeanServer server)
    {

        // Note that when using standard metadata,
        // the returned object must implement the "SsoServerFedCOTsMBean"
        // interface.
        //
        if (server != null) 
            fedCotsGroup = new SsoServerFedCOTsImpl(this,server);
        else 
            fedCotsGroup = new SsoServerFedCOTsImpl(this);

        return fedCotsGroup;
    }
    // ------------------------------------------------------------
    // 
    // Initialization of the "SsoServerSAML2Svc" group.
    // 
    // ------------------------------------------------------------

    /**
     * Factory method for "SsoServerSAML2Svc" group MBean.
     * 
     * You can redefine this method if you need to replace the default
     * generated MBean class with your own customized class.
     * 
     * @param groupName Name of the group ("SsoServerSAML2Svc")
     * @param groupOid  OID of this group
     * @param groupObjname ObjectName for this group (may be null)
     * @param server    MBeanServer for this group (may be null)
     * 
     * @return An instance of the MBean class generated for the
     *         "SsoServerSAML2Svc" group (SsoServerSAML2Svc)
     * 
     * Note that when using standard metadata,
     * the returned object must implement the "SsoServerSAML2SvcMBean"
     * interface.
     **/
    @Override
    protected Object createSsoServerSAML2SvcMBean(
        String groupName,
        String groupOid,
        ObjectName groupObjname,
        MBeanServer server)
    {

        // Note that when using standard metadata,
        // the returned object must implement the "SsoServerSAML2SvcMBean"
        // interface.
        //
        if (server != null) 
            saml2SvcGroup = new SsoServerSAML2SvcImpl(this,server);
        else 
            saml2SvcGroup = new SsoServerSAML2SvcImpl(this);

        return saml2SvcGroup;
    }

    // ------------------------------------------------------------
    // 
    // Initialization of the "SsoServerSAML1Svc" group.
    // 
    // ------------------------------------------------------------

    /**
     * Factory method for "SsoServerSAML1Svc" group MBean.
     * 
     * You can redefine this method if you need to replace the default
     * generated MBean class with your own customized class.
     * 
     * @param groupName Name of the group ("SsoServerSAML1Svc")
     * @param groupOid  OID of this group
     * @param groupObjname ObjectName for this group (may be null)
     * @param server    MBeanServer for this group (may be null)
     * 
     * @return An instance of the MBean class generated for the
     *         "SsoServerSAML1Svc" group (SsoServerSAML1Svc)
     * 
     * Note that when using standard metadata,
     * the returned object must implement the "SsoServerSAML1SvcMBean"
     * interface.
     **/
    @Override
    protected Object createSsoServerSAML1SvcMBean(
        String groupName,
        String groupOid,
        ObjectName groupObjname,
        MBeanServer server)
    {

        // Note that when using standard metadata,
        // the returned object must implement the "SsoServerSAML1SvcMBean"
        // interface.
        //
        if (server != null) 
            saml1SvcGroup = new SsoServerSAML1SvcImpl(this,server);
        else 
            saml1SvcGroup = new SsoServerSAML1SvcImpl(this);

        return saml1SvcGroup;
    }


    // ------------------------------------------------------------
    // 
    // Initialization of the "SsoServerIdRepoSvc" group.
    // 
    // ------------------------------------------------------------

    /**
     * Factory method for "SsoServerIdRepoSvc" group MBean.
     * 
     * You can redefine this method if you need to replace the default
     * generated MBean class with your own customized class.
     * 
     * @param groupName Name of the group ("SsoServerIdRepoSvc")
     * @param groupOid  OID of this group
     * @param groupObjname ObjectName for this group (may be null)
     * @param server    MBeanServer for this group (may be null)
     * 
     * @return An instance of the MBean class generated for the
     *         "SsoServerIdRepoSvc" group (SsoServerIdRepoSvc)
     * 
     * Note that when using standard metadata,
     * the returned object must implement the "SsoServerIdRepoSvcMBean"
     * interface.
     **/
    @Override
    protected Object createSsoServerIdRepoSvcMBean(
        String groupName,
        String groupOid,
        ObjectName groupObjname,
        MBeanServer server)
    {

        // Note that when using standard metadata,
        // the returned object must implement the "SsoServerIdRepoSvcMBean"
        // interface.
        //
        if (server != null) 
            idrepoSvcGroup = new SsoServerIdRepoSvcImpl(this,server);
        else 
            idrepoSvcGroup = new SsoServerIdRepoSvcImpl(this);

        return idrepoSvcGroup;
    }


    // ------------------------------------------------------------
    // 
    // Initialization of the "SsoServerSvcMgmtSvc" group.
    // 
    // ------------------------------------------------------------

    /**
     * Factory method for "SsoServerSvcMgmtSvc" group MBean.
     * 
     * You can redefine this method if you need to replace the default
     * generated MBean class with your own customized class.
     * 
     * @param groupName Name of the group ("SsoServerSvcMgmtSvc")
     * @param groupOid  OID of this group
     * @param groupObjname ObjectName for this group (may be null)
     * @param server    MBeanServer for this group (may be null)
     * 
     * @return An instance of the MBean class generated for the
     *         "SsoServerSvcMgmtSvc" group (SsoServerSvcMgmtSvc)
     * 
     * Note that when using standard metadata,
     * the returned object must implement the "SsoServerSvcMgmtSvcMBean"
     * interface.
     **/
    @Override
    protected Object createSsoServerSvcMgmtSvcMBean(
        String groupName,
        String groupOid,
        ObjectName groupObjname,
        MBeanServer server)
    {

        // Note that when using standard metadata,
        // the returned object must implement the "SsoServerSvcMgmtSvcMBean"
        // interface.
        //
        if (server != null) 
            svcmgmtSvcGroup = new SsoServerSvcMgmtSvcImpl(this,server);
        else 
            svcmgmtSvcGroup = new SsoServerSvcMgmtSvcImpl(this);

        return svcmgmtSvcGroup;
    }

    // ------------------------------------------------------------
    // 
    // Initialization of the "SsoServerPolicySvc" group.
    // 
    // ------------------------------------------------------------

    /**
     * Factory method for "SsoServerPolicySvc" group MBean.
     * 
     * You can redefine this method if you need to replace the default
     * generated MBean class with your own customized class.
     * 
     * @param groupName Name of the group ("SsoServerPolicySvc")
     * @param groupOid  OID of this group
     * @param groupObjname ObjectName for this group (may be null)
     * @param server    MBeanServer for this group (may be null)
     * 
     * @return An instance of the MBean class generated for the
     *         "SsoServerPolicySvc" group (SsoServerPolicySvc)
     * 
     * Note that when using standard metadata,
     * the returned object must implement the "SsoServerPolicySvcMBean"
     * interface.
     **/
    @Override
    protected Object createSsoServerPolicySvcMBean(
        String groupName,
        String groupOid,
        ObjectName groupObjname,
        MBeanServer server)
    {

        // Note that when using standard metadata,
        // the returned object must implement the "SsoServerPolicySvcMBean"
        // interface.
        //
        if (server != null) 
            policySvcGroup = new SsoServerPolicySvcImpl(this,server);
        else 
            policySvcGroup = new SsoServerPolicySvcImpl(this);

        return policySvcGroup;
    }

    // ------------------------------------------------------------
    // 
    // Initialization of the "SsoServerLoggingSvc" group.
    // 
    // ------------------------------------------------------------

    /**
     * Factory method for "SsoServerLoggingSvc" group MBean.
     * 
     * You can redefine this method if you need to replace the default
     * generated MBean class with your own customized class.
     * 
     * @param groupName Name of the group ("SsoServerLoggingSvc")
     * @param groupOid  OID of this group
     * @param groupObjname ObjectName for this group (may be null)
     * @param server    MBeanServer for this group (may be null)
     * 
     * @return An instance of the MBean class generated for the
     *         "SsoServerLoggingSvc" group (SsoServerLoggingSvc)
     * 
     * Note that when using standard metadata,
     * the returned object must implement the "SsoServerLoggingSvcMBean"
     * interface.
     **/
    @Override
    protected Object createSsoServerLoggingSvcMBean(
        String groupName,
        String groupOid,
        ObjectName groupObjname,
        MBeanServer server)
    {

        // Note that when using standard metadata,
        // the returned object must implement the "SsoServerLoggingSvcMBean"
        // interface.
        //
        if (server != null) 
            loggingSvcGroup = new SsoServerLoggingSvcImpl(this,server);
        else 
            loggingSvcGroup = new SsoServerLoggingSvcImpl(this);

        return loggingSvcGroup;
    }

    // ------------------------------------------------------------
    // 
    // Initialization of the "SsoServerSessSvc" group.
    // 
    // ------------------------------------------------------------

    /**
     * Factory method for "SsoServerSessSvc" group MBean.
     * 
     * You can redefine this method if you need to replace the default
     * generated MBean class with your own customized class.
     * 
     * @param groupName Name of the group ("SsoServerSessSvc")
     * @param groupOid  OID of this group
     * @param groupObjname ObjectName for this group (may be null)
     * @param server    MBeanServer for this group (may be null)
     * 
     * @return An instance of the MBean class generated for the
     *         "SsoServerSessSvc" group (SsoServerSessSvc)
     * 
     * Note that when using standard metadata,
     * the returned object must implement the "SsoServerSessSvcMBean"
     * interface.
     **/
    @Override
    protected Object createSsoServerSessSvcMBean(
        String groupName,
        String groupOid,
        ObjectName groupObjname,
        MBeanServer server)
    {

        // Note that when using standard metadata,
        // the returned object must implement the "SsoServerSessSvcMBean"
        // interface.
        //
        if (server != null) 
            sessSvcGroup = new SsoServerSessSvcImpl(this,server);
        else 
            sessSvcGroup = new SsoServerSessSvcImpl(this);

        return sessSvcGroup;
    }

    // ------------------------------------------------------------
    // 
    // Initialization of the "SsoServerAuthSvc" group.
    // 
    // ------------------------------------------------------------

    /**
     * Factory method for "SsoServerAuthSvc" group MBean.
     * 
     * You can redefine this method if you need to replace the default
     * generated MBean class with your own customized class.
     * 
     * @param groupName Name of the group ("SsoServerAuthSvc")
     * @param groupOid  OID of this group
     * @param groupObjname ObjectName for this group (may be null)
     * @param server    MBeanServer for this group (may be null)
     * 
     * @return An instance of the MBean class generated for the
     *         "SsoServerAuthSvc" group (SsoServerAuthSvc)
     * 
     * Note that when using standard metadata,
     * the returned object must implement the "SsoServerAuthSvcMBean"
     * interface.
     **/
    @Override
    protected Object createSsoServerAuthSvcMBean(
        String groupName,
        String groupOid,
        ObjectName groupObjname,
        MBeanServer server)
    {

        // Note that when using standard metadata,
        // the returned object must implement the "SsoServerAuthSvcMBean"
        // interface.
        //
        if (server != null) 
            authSvcGroup = new SsoServerAuthSvcImpl(this,server);
        else 
            authSvcGroup = new SsoServerAuthSvcImpl(this);

        return authSvcGroup;
    }

    // ------------------------------------------------------------
    // 
    // Initialization of the "SsoServerTopology" group.
    // 
    // ------------------------------------------------------------

    /**
     * Factory method for "SsoServerTopology" group MBean.
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
     *         "SsoServerTopology" group (SsoServerTopology)
     * 
     * Note that when using standard metadata,
     * the returned object must implement the "SsoServerTopologyMBean"
     * interface.
     **/
    @Override
    protected Object createSsoServerTopologyMBean(
        String groupName,
        String groupOid,
        ObjectName groupObjname,
        MBeanServer server)
    {

        // Note that when using standard metadata,
        // the returned object must implement the "SsoServerTopologyMBean"
        // interface.
        //
        if (server != null) 
            topologyGroup = new SsoServerTopologyImpl(this,server);
        else 
            topologyGroup = new SsoServerTopologyImpl(this);

        return topologyGroup;
    }

    // ------------------------------------------------------------
    // 
    // Initialization of the "SsoServerEntitlementSvc" group.
    // 
    // ------------------------------------------------------------

    /**
     * Factory method for "SsoServerEntitlementSvc" group MBean.
     * 
     * You can redefine this method if you need to replace the default
     * generated MBean class with your own customized class.
     * 
     * @param groupName Name of the group ("SsoServerEntitlementSvc")
     * @param groupOid  OID of this group
     * @param groupObjname ObjectName for this group (may be null)
     * @param server    MBeanServer for this group (may be null)
     * 
     * @return An instance of the MBean class generated for the
     *         "SsoServerEntitlementSvc" group (SsoServerEntitlementSvc)
     * 
     * Note that when using standard metadata,
     * the returned object must implement the "SsoServerEntitlementSvcMBean"
     * interface.
     **/
    @Override
    protected Object createSsoServerEntitlementSvcMBean(
        String groupName,
        String groupOid,
        ObjectName groupObjname,
        MBeanServer server)
    {

        // Note that when using standard metadata,
        // the returned object must implement the "SsoServerEntitlementSvcMBean"
        // interface.
        //
        if (server != null) 
            entitlementsGroup = new SsoServerEntitlementSvcImpl(this,server);
        else 
            entitlementsGroup = new SsoServerEntitlementSvcImpl(this);

        return entitlementsGroup;
    }

    @Override
    protected Object createSsoServerConnPoolSvcMBean(
            String groupName,
            String groupOid,
            ObjectName groupObjname,
            MBeanServer server)
    {
        // Note that when using standard metadata,
        // the returned object must implement the "SsoServerEntitlementSvcMBean"
        // interface.
        //
        if (server != null) {
            connPoolGroup = new SsoServerConnPoolSvcImpl(this, server);
        } else {
            connPoolGroup = new SsoServerConnPoolSvcImpl(this);
        }

        return connPoolGroup;
    }

    // ------------------------------------------------------------
    // 
    // Initialization of the "SsoServerWSSAgents" group.
    // 
    // ------------------------------------------------------------

    /**
     * Factory method for "SsoServerWSSAgents" group MBean.
     * 
     * You can redefine this method if you need to replace the default
     * generated MBean class with your own customized class.
     * 
     * @param groupName Name of the group ("SsoServerWSSAgents")
     * @param groupOid  OID of this group
     * @param groupObjname ObjectName for this group (may be null)
     * @param server    MBeanServer for this group (may be null)
     * 
     * @return An instance of the MBean class generated for the
     *         "SsoServerWSSAgents" group (SsoServerWSSAgents)
     * 
     * Note that when using standard metadata,
     * the returned object must implement the "SsoServerWSSAgentsMBean"
     * interface.
     **/
    @Override
    protected Object createSsoServerWSSAgentsMBean(
        String groupName,
        String groupOid,
        ObjectName groupObjname,
        MBeanServer server)
    {

        // Note that when using standard metadata,
        // the returned object must implement the "SsoServerWSSAgentsMBean"
        // interface.
        //
        if (server != null) 
            wssAgentsGroup = new SsoServerWSSAgentsImpl(this,server);
        else 
            wssAgentsGroup = new SsoServerWSSAgentsImpl(this);

        return wssAgentsGroup;
    }

    // ------------------------------------------------------------
    // 
    // Initialization of the "SsoServerPolicyAgents" group.
    // 
    // ------------------------------------------------------------

    /**
     * Factory method for "SsoServerPolicyAgents" group MBean.
     * 
     * You can redefine this method if you need to replace the default
     * generated MBean class with your own customized class.
     * 
     * @param groupName Name of the group ("SsoServerPolicyAgents")
     * @param groupOid  OID of this group
     * @param groupObjname ObjectName for this group (may be null)
     * @param server    MBeanServer for this group (may be null)
     * 
     * @return An instance of the MBean class generated for the
     *         "SsoServerPolicyAgents" group (SsoServerPolicyAgents)
     * 
     * Note that when using standard metadata,
     * the returned object must implement the "SsoServerPolicyAgentsMBean"
     * interface.
     **/
    @Override
    protected Object createSsoServerPolicyAgentsMBean(
        String groupName,
        String groupOid,
        ObjectName groupObjname,
        MBeanServer server)
    {

        // Note that when using standard metadata,
        // the returned object must implement the "SsoServerPolicyAgentsMBean"
        // interface.
        //
        if (server != null) 
            policyAgentsGroup = new SsoServerPolicyAgentsImpl(this,server);
        else 
            policyAgentsGroup = new SsoServerPolicyAgentsImpl(this);

        return policyAgentsGroup;
    }


    // ------------------------------------------------------------
    // 
    // Initialization of the "SsoServerIDFFSvc" group.
    // 
    // ------------------------------------------------------------

    /**
     * Factory method for "SsoServerIDFFSvc" group MBean.
     * 
     * You can redefine this method if you need to replace the default
     * generated MBean class with your own customized class.
     * 
     * @param groupName Name of the group ("SsoServerIDFFSvc")
     * @param groupOid  OID of this group
     * @param groupObjname ObjectName for this group (may be null)
     * @param server    MBeanServer for this group (may be null)
     * 
     * @return An instance of the MBean class generated for the
     *         "SsoServerIDFFSvc" group (SsoServerIDFFSvc)
     * 
     * Note that when using standard metadata,
     * the returned object must implement the "SsoServerIDFFSvcMBean"
     * interface.
     **/
    @Override
    protected Object createSsoServerIDFFSvcMBean(
        String groupName,
        String groupOid,
        ObjectName groupObjname,
        MBeanServer server)
    {

        // Note that when using standard metadata,
        // the returned object must implement the "SsoServerIDFFSvcMBean"
        // interface.
        //
        if (server != null) 
            idffSvcGroup = new SsoServerIDFFSvcImpl(this,server);
        else 
            idffSvcGroup = new SsoServerIDFFSvcImpl(this);

        return idffSvcGroup;
    }

    protected SsoServerAuthSvcImpl getAuthSvcGroup() {
        return authSvcGroup;
    }

    protected SsoServerConnPoolSvcImpl getConnPoolGroup() {
        return connPoolGroup;
    }

    protected SsoServerEntitlementSvcImpl getEntitlementsGroup() {
        return entitlementsGroup;
    }

    protected SsoServerFedCOTsImpl getFedCotsGroup() {
        return fedCotsGroup;
    }

    protected SsoServerFedEntitiesImpl getFedEntitiesGroup() {
        return fedEntitiesGroup;
    }

    protected SsoServerIDFFSvcImpl getIdffSvcGroup() {
        return idffSvcGroup;
    }

    protected SsoServerIdRepoSvcImpl getIdrepoSvcGroup() {
        return idrepoSvcGroup;
    }

    protected SsoServerInstanceImpl getSvrInstanceGroup() {
        return instanceGroup;
    }

    protected SsoServerLoggingSvcImpl getLoggingSvcGroup() {
        return loggingSvcGroup;
    }

    protected SsoServerPolicyAgentsImpl getPolicyAgentsGroup() {
        return policyAgentsGroup;
    }

    protected SsoServerPolicySvcImpl getPolicySvcGroup() {
        return policySvcGroup;
    }

    protected SsoServerSAML1SvcImpl getSaml1SvcGroup() {
        return saml1SvcGroup;
    }

    protected SsoServerSAML2SvcImpl getSaml2SvcGroup() {
        return saml2SvcGroup;
    }

    protected SsoServerSessSvcImpl getSessSvcGroup() {
        return sessSvcGroup;
    }

    protected SsoServerSvcMgmtSvcImpl getSmSvcGroup() {
        return svcmgmtSvcGroup;
    }

    protected SsoServerTopologyImpl getTopologyGroup() {
        return topologyGroup;
    }

    protected SsoServerWSSAgentsImpl getWssAgentsGroup() {
        return wssAgentsGroup;
    }
}
