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
 * $Id: FSDefaultSPAdapter.java,v 1.6 2008/06/25 05:49:54 qcheng Exp $
 *
 */

package com.sun.identity.federation.plugins;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.federation.accountmgmt.FSAccountFedInfoKey;
import com.sun.identity.federation.accountmgmt.FSAccountManager;
import com.sun.identity.federation.accountmgmt.FSAccountMgmtException;
import com.sun.identity.federation.common.FederationException;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.common.LogUtil;
import com.sun.identity.federation.jaxb.entityconfig.SPDescriptorConfigElement;
import com.sun.identity.federation.message.FSAuthenticationStatement;
import com.sun.identity.federation.message.FSAssertion;
import com.sun.identity.federation.message.FSAuthnRequest;
import com.sun.identity.federation.message.FSAuthnResponse;
import com.sun.identity.federation.message.FSFederationTerminationNotification;
import com.sun.identity.federation.message.FSLogoutNotification;
import com.sun.identity.federation.message.FSLogoutResponse;
import com.sun.identity.federation.message.FSNameRegistrationRequest;
import com.sun.identity.federation.message.FSNameRegistrationResponse;
import com.sun.identity.federation.message.FSResponse;
import com.sun.identity.federation.message.FSSubject;
import com.sun.identity.federation.meta.IDFFMetaException;
import com.sun.identity.federation.meta.IDFFMetaManager;
import com.sun.identity.federation.services.util.FSServiceUtils;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdSearchOpModifier;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.saml.assertion.NameIdentifier;
import com.sun.identity.saml.assertion.Statement;
import com.sun.identity.saml.protocol.Status;
import com.sun.identity.saml.protocol.StatusCode;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.Constants;
import com.sun.identity.shared.encode.URLEncDec;
import java.security.AccessController;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class FSDefaultSPAdapter implements FederationSPAdapter {

    private final String ROOT_REALM = "/";
    private String realm = null;

    /**
     * Initializes the federation adapter, this method will only be executed
     * once after creation of the adapter instance.
     * @param hostedProviderID provider ID for the hosted SP
     * @param initParams  initial set of parameters configured in the service
     *          provider for this adapter
     */
    public void initialize(String hostedProviderID, Set initParams) {
        FSUtils.debug.message("In FSDefaultSPAdapter.initialize.");
        if ((initParams != null) && !initParams.isEmpty()) {
            Iterator iter = initParams.iterator();
            while (iter.hasNext()) {
                String envValue = (String) iter.next();
                if ((envValue.toUpperCase()).startsWith(
                    FederationSPAdapter.ENV_REALM))
                {
                    try {
                        realm = envValue.substring(
                            (FederationSPAdapter.ENV_REALM).length(),
                            envValue.length());
                    } catch (Exception e) {
                        if (FSUtils.debug.warningEnabled()) {
                           FSUtils.debug.warning(
                               "FSDefaultSPAdapter.init:Could not get realm:",
                               e);
                        }
                    }
                    break;
                }
            }
        }
        if ((realm == null) || (realm.length() == 0)) {
            realm = ROOT_REALM;
        }
    }

    /**
     * Invokes before federation manager sends the Single-Sing-On and Federation     * request to IDP.
     * @param hostedProviderID provider ID for the hosted SP
     * @param idpProviderID provider id for the IDP to which the request will
     *          be sent
     * @param request servlet request
     * @param response servlet response
     * @param authnRequest the authentication request to be send to IDP
     */
    public void preSSOFederationRequest(
        String hostedProviderID,
        String idpProviderID,
        HttpServletRequest request,
        HttpServletResponse response,
        FSAuthnRequest authnRequest)
    {
        FSUtils.debug.message("In FSDefaultSPAdapter.preSSOFederationRequest.");
    }

    /**
     * Invokes when the FM received the Single-Sign-On and Federation response
     * from the IDP, this is called before any processing started on SP side.
     * @param hostedProviderID provider ID for the hosted SP
     * @param request servlet request
     * @param response servlet response
     * @param authnRequest the original authentication request sent from SP
     * @param authnResponse response from IDP if Browser POST or LECP profile
     *        is used for the request, value will be null if Browser Artifact
     *        profile is used.
     * @param samlResponse response from IDP if Browser Artifact profile is used
     *        for the request, value will be null if Browser POST or LECP
     *        profile is used.
     * @exception FederationException if user want to fail the process.
     */
    public void preSSOFederationProcess(
        String hostedProviderID,
        HttpServletRequest request,
        HttpServletResponse response,
        FSAuthnRequest authnRequest,
        FSAuthnResponse authnResponse,
        FSResponse samlResponse)
    throws FederationException {
        FSUtils.debug.message("In FSDefaultSPAdapter.preSSOFederationProcess.");
    }

    /**
     * Invokes this method after the successful Single Sign-On or Federation.
     * @param hostedEntityID provider ID for the hosted SP
     * @param request servlet request
     * @param response servlet response
     * @param ssoToken user's SSO token
     * @param authnRequest the original authentication request sent from SP 
     * @param authnResponse response from IDP if Browser POST or LECP profile
     *        is used for the request, value will be null if Browser Artifact
     *        profile is used. 
     * @param samlResponse response from IDP if Browser Artifact profile is used
     *        for the request, value will be null if Browser POST or LECP 
     *        profile is used.
     * @exception FederationException if user want to fail the process.
     * @return true if browser redirection happened, false otherwise.
     */
    public boolean postSSOFederationSuccess(
        String hostedEntityID, 
        HttpServletRequest request, 
        HttpServletResponse response, 
        Object ssoToken,
        FSAuthnRequest authnRequest, 
        FSAuthnResponse authnResponse,
        FSResponse samlResponse
    ) throws FederationException {
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("FSDefaultSPAdapter.postFedSuccess, "
                + "process " + hostedEntityID);
        }
        // find out if this is a federation request
        boolean isFederation = false;
        if (authnRequest == null) {
            FSUtils.debug.error("FSDefaultSPAdapter.postFedSuccess null");
        } else {
            String nameIDPolicy = authnRequest.getNameIDPolicy();
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSDefaultSPAdapter.postSuccess " 
                    + nameIDPolicy);
            }
            if (nameIDPolicy.equals(IFSConstants.NAME_ID_POLICY_FEDERATED)) {
                isFederation = true;
            }
        }
        SSOToken adminToken = (SSOToken)
            AccessController.doPrivileged(AdminTokenAction.getInstance());
        if (isFederation && adminToken != null) {
            try {
                // get name Identifier
                String nameId = null;
                List assertions = null; 
                String idpEntityId = null; 
                if (authnResponse != null) { 
                    // POST profile
                    assertions = authnResponse.getAssertion();
                    idpEntityId = authnResponse.getProviderId();
                } else {
                    // Artifact profile
                    assertions = samlResponse.getAssertion();
                }
                FSAssertion assertion = 
                    (FSAssertion) assertions.iterator().next();
                if (idpEntityId == null) {
                    idpEntityId = assertion.getIssuer();
                }
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSAdapter.postSuccess: idp=" 
                        + idpEntityId);
                }
                Iterator stmtIter = assertion.getStatement().iterator();
                while (stmtIter.hasNext()) {
                    Statement statement = (Statement) stmtIter.next();
                    int stmtType = statement.getStatementType();
                    if (stmtType == Statement.AUTHENTICATION_STATEMENT) {
                        FSAuthenticationStatement authStatement =
                            (FSAuthenticationStatement) statement; 
                        FSSubject subject = 
                            (FSSubject) authStatement.getSubject();
                        NameIdentifier ni = 
                            subject.getIDPProvidedNameIdentifier();
                        if (ni == null) {
                            ni = subject.getNameIdentifier();
                        }
                        if (ni != null) {
                            nameId = ni.getName();  
                        }
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message("FSAdapter.postSuccess: "
                                + "found name id =" + nameId);
                        }
                        break;
                    }
                }
                if (nameId == null) {
                    FSUtils.debug.warning("FSAdapter.postSuc : null nameID");
                    return false;
                }

                Map map = new HashMap();
                Set set = new HashSet();
                set.add("|" + hostedEntityID + "|" + nameId + "|");
                map.put("iplanet-am-user-federation-info-key", set);

                AMIdentityRepository idRepo = new AMIdentityRepository(
                    adminToken, 
                    ((SSOToken) ssoToken).getProperty(
                        ISAuthConstants.ORGANIZATION));
                IdSearchControl searchControl = new IdSearchControl();
                searchControl.setTimeOut(0);
                searchControl.setMaxResults(0);
                searchControl.setAllReturnAttributes(false);
                searchControl.setSearchModifiers(IdSearchOpModifier.AND, map);
                IdSearchResults searchResults = idRepo.searchIdentities(
                    IdType.USER, "*", searchControl);
                Set amIdSet = searchResults.getSearchResults();
                if (amIdSet.size() > 1) {
                    String univId = ((SSOToken) ssoToken).getProperty(
                        Constants.UNIVERSAL_IDENTIFIER);
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message("FSAdapter.postSuccess: found "
                            + amIdSet.size() + " federation with same ID as "
                            + univId); 
                    }
                    String metaAlias = null;
                    try {
                        IDFFMetaManager metaManager = 
                            new IDFFMetaManager(ssoToken);
                        if (metaManager != null) {
                            SPDescriptorConfigElement spConfig =
                                metaManager.getSPDescriptorConfig(
                                    realm, hostedEntityID);
                            if (spConfig != null) {
                                metaAlias = spConfig.getMetaAlias();
                            }
                        }
                    } catch (IDFFMetaException ie) {
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message("FSAdapter.postSuccess: "
                                + "couldn't find meta alias:", ie);
                        }
                    }
                    FSAccountManager accManager = 
                        FSAccountManager.getInstance(metaAlias);
                    FSAccountFedInfoKey fedInfoKey =
                        new FSAccountFedInfoKey(hostedEntityID, nameId);
                    // previous federation exists with different users
                    Iterator it = amIdSet.iterator();
                    while (it.hasNext()) {
                        AMIdentity amId = (AMIdentity) it.next();
                        // compare with the SSO token
                        String tmpUnivId = IdUtils.getUniversalId(amId);
                        if (univId.equalsIgnoreCase(tmpUnivId)) {
                            continue;
                        } 
                        // remove federation information for this user
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message("FSAdapter.postSucces, "
                                + "remove fed info for user " + tmpUnivId);
                        }
                        accManager.removeAccountFedInfo(tmpUnivId, fedInfoKey,
                            idpEntityId);
                    }
                }
            } catch (FSAccountMgmtException f) {
                FSUtils.debug.warning("FSDefaultSPAdapter.postSSOSuccess", f);
            } catch (IdRepoException i) {
                FSUtils.debug.warning("FSDefaultSPAdapter.postSSOSuccess", i);
            } catch (SSOException e) {
                FSUtils.debug.warning("FSDefaultSPAdapter.postSSOSuccess", e);
            }
        }
        return false;
    }

    /**
     * Invokes this method if the Single-Sign-On or Federation fails 
     * for some reason.
     * @param request servlet request
     * @param response servlet response
     * @param authnRequest the original authentication request sent from SP 
     * @param authnResponse response from IDP if Browser POST or LECP profile
     *        is used for the request, value will be null if Browser Artifact
     *        profile is used. 
     * @param samlResponse response from IDP if Browser Artifact profile is used
     *        for the request, value will be null if Browser POST or LECP 
     *        profile is used.
     * @param failureCode an integer specifies the failure code.
     * @return true if browser redirection happened, false otherwise.
     */
    public boolean postSSOFederationFailure(String hostedEntityID, 
        HttpServletRequest request, 
        HttpServletResponse response, 
        FSAuthnRequest authnRequest, 
        FSAuthnResponse authnResponse,
        FSResponse samlResponse,
        int failureCode
    ) {
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("FSDefaultSPAdapter.postFedFailure, "
                 + "process " + hostedEntityID
                 + "\nfailure code=" + failureCode);
        }

        String baseURL = FSServiceUtils.getBaseURL(request);
        String relayState = null;

        if (authnRequest != null) {
            relayState = authnRequest.getRelayState();
        }

        String framedLoginPageURL = FSServiceUtils.getCommonLoginPageURL(
            FSServiceUtils.getMetaAlias(request),
            relayState, null, request, baseURL);

        StringBuffer sb = new StringBuffer();
        sb.append(framedLoginPageURL)
            .append("&").append(IFSConstants.FAILURE_CODE).append("=")
            .append(failureCode);

        if (failureCode == INVALID_AUTHN_RESPONSE ||
            failureCode == INVALID_RESPONSE)
        {
            Status status = null;
            if (failureCode == INVALID_AUTHN_RESPONSE) {
                status = authnResponse.getStatus();
            } else {
                status = samlResponse.getStatus();
            }

            StatusCode firstLevelStatusCode = status.getStatusCode();
            if (firstLevelStatusCode == null) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSDefaultSPAdapter.postSSO" +
                        "FederationFailure: Status is null");
                }
                return false;
            }

            StatusCode secondLevelStatusCode =
                firstLevelStatusCode.getStatusCode();
            if (secondLevelStatusCode == null) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSDefaultSPAdapter.postSSO" +
                        "FederationFailure: Second level status is empty");
                }
                return false;
            }

            String statusValue = URLEncDec.encode(
                secondLevelStatusCode.getValue());
            sb.append("&").append(IFSConstants.STATUS_CODE).append("=")
                .append(statusValue);
        }

        String redirectURL = sb.toString();
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("FSDefaultSPAdapter.postSSOFederation" +
                "Failure. URL to be redirected: " + redirectURL);
        }

        try {
            response.setHeader("Location", redirectURL); 
            response.sendRedirect(redirectURL);
        } catch (java.io.IOException io) {
            FSUtils.debug.error("FSDefaultSPAdapter.postSSOFedFailure", io);
            return false;
        }
        return true;
    }

    /**
     * Invokes after Register Name Identifier processing is successful
     * @param hostedProviderID provider ID for the hosted SP
     * @param request servlet request
     * @param response servlet response
     * @param userDN DN of the user with whom name identifier registration
     *        performed
     * @param regRequest register name identifier request, value will be
     *        null if the request object is not available
     * @param regResponse register name identifier response, value will be
     *        null if the response object is not available
     * @param regProfile register name identifier profile used, one of following
     *        <code>IFSConstants.NAME_REGISTRATION_SP_HTTP_PROFILE</code>
     *        <code>IFSConstants.NAME_REGISTRATION_SP_SOAP_PROFILE</code>
     *        <code>IFSConstants.NAME_REGISTRATION_IDP_HTTP_PROFILE</code>
     *        <code>IFSConstants.NAME_REGISTRATION_IDP_SOAP_PROFILE</code>
     */
    public void postRegisterNameIdentifierSuccess(
        String hostedProviderID,
        HttpServletRequest request,
        HttpServletResponse response,
        String userDN,
        FSNameRegistrationRequest regRequest,
        FSNameRegistrationResponse regResponse,
        String regProfile)
    {
        FSUtils.debug.message(
            "In FSDefaultSPAdapter.postRegistrationNameIdentifierSuccess");
    }

    /**
     * Invokes after the service provider successfully terminates federation
     * with IDP.
     * @param hostedProviderID provider ID for the hosted SP
     * @param request servlet request
     * @param response servlet response
     * @param userDN DN of the user with whom name identifier registration
     *        performed
     * @param notification federation termination notification message
     * @param termProfile federation termination profile used, one of following
     *        <code>IFSConstants.TERMINATION_SP_HTTP_PROFILE</code>
     *        <code>IFSConstants.TERMINATION_SP_SOAP_PROFILE</code>
     *        <code>IFSConstants.TERMINATION_IDP_HTTP_PROFILE</code>
     *        <code>IFSConstants.TERMINATION_IDP_SOAP_PROFILE</code>
     */
    public void postTerminationNotificationSuccess(
        String hostedProviderID,
        HttpServletRequest request,
        HttpServletResponse response,
        String userDN,
        FSFederationTerminationNotification notification,
        String termProfile)
    {
        FSUtils.debug.message(
            "In FSDefaultSPAdapter.postTerminationNotificationSuccess.");
    }

    /**
     * Invokes before single logout process started on FM side. This method
     * is called before the user token is invalidated on the service provider
     * side.
     * @param hostedProviderID provider ID for the hosted SP
     * @param request servlet request
     * @param response servlet response
     * @param userDN user DN
     * @param logoutRequest single logout request object
     * @param logoutResponse single logout response, value will be
     *          null if the response object is not available
     * @param sloProfile single logout profile used, one of following
     *        <code>IFSConstants.LOGOUT_SP_REDIRECT_PROFILE</code>
     *        <code>IFSConstants.LOGOUT_SP_SOAP_PROFILE</code>
     *        <code>IFSConstants.LOGOUT_IDP_REDIRECT_PROFILE</code>
     *        <code>IFSConstants.LOGOUT_IDP_SOAP_PROFILE</code>
     */
    public void preSingleLogoutProcess(
        String hostedProviderID,
        HttpServletRequest request,
        HttpServletResponse response,
        String userDN,
        FSLogoutNotification logoutRequest,
        FSLogoutResponse logoutResponse,
        String sloProfile)
    {
        FSUtils.debug.message("In FSDefaultSPAdapter.preSingleLogoutProcess.");
    }

    /**
     * Invokes after single logout is successful completed, i.e. user token
     * has been invalidated.
     *
     * @param hostedProviderID provider ID for the hosted SP
     * @param request servlet request
     * @param response servlet response
     * @param userDN user DN
     * @param logoutRequest single logout request, value will be
     *          null if the request object is not available
     * @param logoutResponse single logout response, value will be
     *          null if the response object is not available
     * @param sloProfile single logout profile used, one of following
     *        <code>IFSConstants.LOGOUT_SP_HTTP_PROFILE</code>
     *        <code>IFSConstants.LOGOUT_SP_SOAP_PROFILE</code>
     *        <code>IFSConstants.LOGOUT_IDP_HTTP_PROFILE</code>
     *        <code>IFSConstants.LOGOUT_IDP_SOAP_PROFILE</code>
     */
    public void postSingleLogoutSuccess(
        String hostedProviderID,
        HttpServletRequest request,
        HttpServletResponse response,
        String userDN,
        FSLogoutNotification logoutRequest,
        FSLogoutResponse logoutResponse,
        String sloProfile)
    {
        FSUtils.debug.message("In FSDefaultSPAdapter.postSingleLogoutSuccess.");
    }
} 
