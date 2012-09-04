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
 * $Id: FSFedTerminationHandler.java,v 1.7 2009/11/03 00:49:26 madan_ranganath Exp $
 *
 */

package com.sun.identity.federation.services.termination;


import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.federation.accountmgmt.FSAccountManager;
import com.sun.identity.federation.accountmgmt.FSAccountMgmtException;
import com.sun.identity.federation.accountmgmt.FSAccountFedInfo;
import com.sun.identity.federation.accountmgmt.FSAccountFedInfoKey;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.LogUtil;
import com.sun.identity.federation.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.federation.message.FSFederationTerminationNotification;
import com.sun.identity.federation.message.common.FSMsgException;
import com.sun.identity.federation.meta.IDFFMetaUtils;
import com.sun.identity.federation.plugins.FederationSPAdapter;
import com.sun.identity.federation.services.util.FSSignatureUtil;
import com.sun.identity.federation.services.util.FSServiceUtils;
import com.sun.identity.federation.services.logout.FSLogoutUtil;
import com.sun.identity.federation.services.FSSOAPService;
import com.sun.identity.liberty.ws.meta.jaxb.ProviderDescriptorType;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.saml.assertion.NameIdentifier;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLResponderException;
import com.sun.identity.saml.xmlsig.XMLSignatureManager;
import com.sun.identity.shared.encode.URLEncDec;
import com.sun.identity.shared.encode.CookieUtils;
import javax.xml.soap.SOAPMessage;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;
import java.io.IOException;
import java.util.List;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import org.w3c.dom.Document;

/**
 * Work engine that handles termination request/response.
 */
public class FSFedTerminationHandler {
    
    protected HttpServletResponse response;
    protected HttpServletRequest request;
    protected String locale = null;
    protected Object ssoToken = null;
    protected String userID = null;
    protected FSAccountFedInfo acctInfo = null;
    protected ProviderDescriptorType remoteDescriptor = null;
    protected ProviderDescriptorType hostedDescriptor = null;
    protected BaseConfigType hostedConfig = null;
    protected String relayState = "";
    protected FSAccountManager managerInst = null;
    protected static String termination_done_url = null;
    protected static String error_page_url = null;
    protected static final String FEDERATE_COOKIE_NAME =
        SystemConfigurationUtil.getProperty(IFSConstants.FEDERATE_COOKIE_NAME);
    protected static final String RELAY_STATE =
        IFSConstants.TERMINATION_RELAY_STATE;
    protected String realm = "";
    protected String hostedEntityId = "";
    protected String remoteEntityId = "";
    protected String metaAlias = "";
    protected String hostedProviderRole = null;

    /**
     * Constructor. Initializes FSAccountManager, FSAllianceManager instance.
     */
    public FSFedTerminationHandler() {
        FSUtils.debug.message("FSFedTerminationHandler Constructor...");
    }
    
    /**
     * Invoked to set some commonly used URLs based on hosted provider.
     */
    protected void setTerminationURL() {
        termination_done_url = FSServiceUtils.getTerminationDonePageURL(
            request, hostedConfig, metaAlias);
        error_page_url = FSServiceUtils.getErrorPageURL(
            request, hostedConfig, metaAlias);
    }
    
    /**
     * Sets state to the Federation Termination handler that is handling the
     * current federation termination. The hosted provider identifies the 
     * provider who is handling the termnation request or initiating it locally.
     * @param hostedDescriptor the Hosted provider Descriptor
     */
    public void setHostedDescriptor(ProviderDescriptorType hostedDescriptor)
    {
        FSUtils.debug.message(
            "Entered FSSPFedTerminationHandler::setHostedDescriptor");
        this.hostedDescriptor = hostedDescriptor;
    }
    
    /**
     * Sets hosted provider's extended meta.
     * @param hostedConfig hosted provider's extended config
     */
    public void setHostedDescriptorConfig(BaseConfigType hostedConfig) {
        this.hostedConfig = hostedConfig;
    }

    /**
     * Sets hosted provider's entity ID.
     * @param hostedId hosted provider's entity id
     */
    public void setHostedEntityId(String hostedId) {
        hostedEntityId = hostedId;
    }

    /**
     * Sets hosted provider's role.
     * @param hostedProviderRole hosted provider's role
     */
    public void setHostedProviderRole(String hostedProviderRole) {
        this.hostedProviderRole = hostedProviderRole;
    }

    /**
     * Sets hosted provider's meta alias.
     * @param metaAlias hosted provider's meta alias
     */
    public void setMetaAlias(String metaAlias) {
        this.metaAlias = metaAlias;
        try {
            managerInst = FSAccountManager.getInstance(metaAlias);
        } catch (Exception e){
            FSUtils.debug.error("FSFedTerminationHandler " +
                FSUtils.bundle.getString(
                    IFSConstants.FEDERATION_FAILED_ACCOUNT_INSTANCE));
            managerInst = null;
        }
    }

    /**
     * Sets realm.
     * @param realm The realm under which the entity resides.
     */
    public void setRealm(String realm) {
        this.realm = realm;
    }

    /**
     * Sets remote provider's entity ID.
     * @param remoteId remote provider's entity id
     */
    public void setRemoteEntityId(String remoteId) {
        remoteEntityId = remoteId;
    }

    /**
     * Sets state to the Federation Termination handler that is handling the 
     * current federation termination. The remote provider identifies the 
     * provider who sent a request or with whom termination is to be initiated.
     * @param remoteDescriptor the Remote provider Descriptor
     */
    public void setRemoteDescriptor(ProviderDescriptorType remoteDescriptor) {
        FSUtils.debug.message(
            "Entered FSFedTerminationHandler::setRemoteDescriptor");
        this.remoteDescriptor =  remoteDescriptor;
    }
    

    /**
     * Sets the UserID.
     * @param userID the user who is initiating the termination process
     */
    public void setUserID(String userID) {
        this.userID = userID;
    }
    
    /**
     * Sets the federation account information for the user with a specific
     * remote provider.
     * @param acctInfo the account fed info object
     */
    public void setAccountInfo(FSAccountFedInfo acctInfo) {
        this.acctInfo = acctInfo;
    }
    
    /**
     * Finds the user based on the termination request received from a remote
     * provider.
     * @param reqTermination the termination request
     * @return <code>true</code> if the user is found; <code>false</code>
     *  otherwise.
     */
    public boolean setUserID(FSFederationTerminationNotification reqTermination)
    {
        try {
            
            // UserDN needs to be figured from termination request
            String sourceProviderId = "";
            if (managerInst != null) {
                sourceProviderId = reqTermination.getProviderId();
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("sourceProviderId : " +
                        sourceProviderId);
                }
                String opaqueHandle =
                    (reqTermination.getNameIdentifier()).getName().trim();
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("processTerminationRequest Handle : "
                    + opaqueHandle);
                }
                String associatedDomain =
                (reqTermination.getNameIdentifier().getNameQualifier()).trim();
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("Name Qualifier : "
                        + associatedDomain);
                }
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("Realm : " + realm);
                }
                
                String searchDomain = hostedEntityId;
                if ((associatedDomain != null) &&
                    (associatedDomain.length() != 0) &&
                    (!sourceProviderId.equals(associatedDomain)))
                {
                   searchDomain = associatedDomain;
                }
                FSAccountFedInfoKey acctkey = new FSAccountFedInfoKey(
                    searchDomain, opaqueHandle);
                Map env = new HashMap();
                env.put(IFSConstants.FS_USER_PROVIDER_ENV_TERMINATION_KEY,
                    reqTermination);
                this.userID = managerInst.getUserID(acctkey, realm, env);
                if (this.userID == null) {
                    acctkey = new FSAccountFedInfoKey(
                        remoteEntityId, opaqueHandle);
                    this.userID = managerInst.getUserID(acctkey, realm, env);
                    if (this.userID == null) {
                        FSUtils.debug.message("UserID is null");
                        return false;
                    }
                }
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("user id is "+ userID);
                }
                return true;
            }
        } catch(FSAccountMgmtException e) {
            FSUtils.debug.error("In FSAccountMgmtException :: ", e);
        }
        this.userID = null;
        return false;
    }
    
    /**
     * Initiates the federation termination operation.
     * @param request HTTP request
     * @param response HTTP response
     * @param ssoToken corresponding to the user's session
     * @return <code>true</code> if the termination initiation operation is
     *  successful; <code>false</code> otherwise.
     */
    public boolean handleFederationTermination(
        HttpServletRequest request,
        HttpServletResponse response,
        Object ssoToken)
    {
        FSUtils.debug.message(
            "Entered FSFedTerminationHandler::handleFederationTermination");
        this.request = request;
        this.locale = FSServiceUtils.getLocale(request);
        this.response = response;
        this.ssoToken = ssoToken;
        setTerminationURL();
        if (managerInst == null) {
            FSUtils.debug.error("FSSPFedTerminationHandler " +
                "Account Manager instance is null");
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSSPFedTerminationHandler::handleFederationTermination" +
                        "failed to get Account Manager instance");
            }
            FSServiceUtils.returnLocallyAfterOperation(
                response, termination_done_url, false,
                IFSConstants.TERMINATION_SUCCESS,
                IFSConstants.TERMINATION_FAILURE);
            return false;
        }
        
        try {
            this.userID =
                SessionManager.getProvider().getPrincipalName(ssoToken);
        } catch(SessionException e) {
            FSUtils.debug.error(
                "FSFedTerminationHandler::handleFederationTermination:", e);
            // cannot proceed without user
            LogUtil.error(Level.INFO,LogUtil.USER_NOT_FOUND,null,ssoToken);
            return false;
        }
        boolean bStatus = updateAccountInformation(null);
        FSUtils.debug.message("After updateAccountInformation");
        if (!bStatus) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                "FSSPFedTerminationHandler::handleFederationTermination "
                    + "Federation Termination failed locally. Cannot update "
                    + "account");
            }
            String[] data = { userID };
            LogUtil.error(Level.INFO,LogUtil.TERMINATION_FAILED,data, ssoToken);
            FSServiceUtils.returnLocallyAfterOperation(
                response, termination_done_url, false,
                IFSConstants.TERMINATION_SUCCESS,
                IFSConstants.TERMINATION_FAILURE);
            return false;
        }
        FSUtils.debug.message("Status of local update true");
        String[] data = { userID };
        LogUtil.access(Level.INFO,LogUtil.TERMINATION_SUCCESS,data, ssoToken);
        resetFederateCookie();
        boolean bRemoteStatus = doFederationTermination(
            request, response, acctInfo);
        return bRemoteStatus;
    }
    
    /**
     * Updates the user account information. After sucessful operation,
     * the federation status corresponding to the user with the remote provider
     * is set to inactive.
     * @param ni <code>NameIdentifier</code> object corresponding to a user
     * @return boolean containing the status of the update operation
     */
    protected boolean updateAccountInformation(NameIdentifier ni) {
        try {
            FSUtils.debug.message(
                "FSFedTerminationHandler::updateAccountInformation: start");
            String searchDomain = remoteEntityId;
            // get name identifier to remove it from federation info key
            String nameId = null;
            String nameQualifier = null;
            if(ni != null) {
                nameQualifier = ni.getNameQualifier();
                if(nameQualifier != null && 
                    (nameQualifier.length() != 0) &&
                    !nameQualifier.equals(remoteEntityId))
                {
                    searchDomain = nameQualifier;
                }
                nameId = ni.getName();
            }
            if (nameId == null && acctInfo != null) {
                FSUtils.debug.message("FSAccountManager: getnameId in accInfo");
                NameIdentifier temp = acctInfo.getLocalNameIdentifier();
                if (temp != null) {
                    nameId = temp.getName();
                    nameQualifier = temp.getNameQualifier();
                } else {
                    temp = acctInfo.getRemoteNameIdentifier();
                    if (temp != null) {
                        nameId = temp.getName();
                        nameQualifier = temp.getNameQualifier();
                    }
                }
            }
            FSAccountFedInfoKey fedInfoKey = 
                  new FSAccountFedInfoKey(nameQualifier, nameId);
            managerInst.removeAccountFedInfo(userID, fedInfoKey, searchDomain);

            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSFedTerminationHandler:: " +
                "updateAccountInformation deactivate successfully completed");
            }
        } catch (FSAccountMgmtException e) {
            FSUtils.debug.error(
                "FSFedTerminationHandler::updateAccountInformation " +
                FSUtils.bundle.getString(
                    IFSConstants.TERMINATION_LOCAL_FAILED));
            String[] data = { userID };
            LogUtil.error(Level.INFO,LogUtil.TERMINATION_FAILED,data, ssoToken);
            return false;
        }
        // Clean SessionMap off the partner to be done here.
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message(
                "Cleaning Session manager for user : " + userID);
            FSUtils.debug.message(
                "Cleaning Session manager for remote provider: " +
                remoteEntityId);
            FSUtils.debug.message(
                "Cleaning Session manager for hosted provider: " +
                hostedEntityId);
        }
        FSLogoutUtil.cleanSessionMapPartnerList(
            userID, remoteEntityId, metaAlias, null);
        return true;
    }
    
    /**
     * Processes the termination request received from a
     * remote provider. Invoded when Http redirect profile is used.
     * @param request HTTP request
     * @param response HTTP response
     * @param reqTermination the federation termination request received from
     *  remote provider
     */
    public void processTerminationRequest(
        HttpServletRequest request,
        HttpServletResponse response,
        FSFederationTerminationNotification reqTermination) 
    {
        FSUtils.debug.message(
            "Entered FSFedTerminationHandler::processTerminationRequest...");
        this.request = request;
        this.locale = FSServiceUtils.getLocale(request);
        this.response = response;
        this.relayState = reqTermination.getRelayState();
        setTerminationURL();
        if (managerInst == null) {
            FSUtils.debug.error("FSSPFedTerminationHandler " +
                FSUtils.bundle.getString(
                    IFSConstants.FEDERATION_FAILED_ACCOUNT_INSTANCE));
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSSPFedTerminationHandler::handleFederationTermination" +
                    "failed to get Account Manager instance");
            }
            returnToSource();
            return;
        }
        boolean bStatus = updateAccountInformation(
            reqTermination.getNameIdentifier());
        if (!bStatus) {
            FSUtils.debug.message("Termination request processing failed");
            String[] data = { FSUtils.bundle.getString(
                IFSConstants.TERMINATION_REQUEST_PROCESSING_FAILED) };
            LogUtil.error(Level.INFO,LogUtil.TERMINATION_FAILED,data, ssoToken);
            returnToSource();
            return;
        }
        FSUtils.debug.message("User sucessfully defederated");
        String[] data = { FSUtils.bundle.getString(
            IFSConstants.TERMINATION_SUCCEEDED) };
        LogUtil.access(Level.INFO,LogUtil.TERMINATION_SUCCESS,data, ssoToken);
        // Call SP Adaper for remote IDP initiated HTTP profile
        if (hostedProviderRole != null &&
            hostedProviderRole.equalsIgnoreCase(IFSConstants.SP))
        {
            FederationSPAdapter spAdapter =
                FSServiceUtils.getSPAdapter(hostedEntityId, hostedConfig);
            if (spAdapter != null) {
                FSUtils.debug.message("FSFedTerminationHandler.HTTP");
                try {
                    spAdapter.postTerminationNotificationSuccess(
                        hostedEntityId,
                        request,
                        response,
                        userID,
                        reqTermination,
                        IFSConstants.TERMINATION_IDP_HTTP_PROFILE);
                } catch (Exception e) {
                    // ignore adapter exception
                    FSUtils.debug.error("postTermNotification.IDP/HTTP", e);
                }
            }
        }
        returnToSource();
        return;
    }
    
    /**
     * Processes the termination request received from a
     * remote provider. Invoded when SOAP profile is used.
     * @param reqTermination the federation termination request received from
     *  remote provider
     * @return <code>true</code> when the process is successful;
     *  <code>false</code> otherwise.
     */
    public boolean processSOAPTerminationRequest(
        HttpServletRequest request,
        HttpServletResponse response,
        FSFederationTerminationNotification reqTermination)
    {
        FSUtils.debug.message(
            "Entered FSFedTerminationHandler::processSOAPTerminationRequest");
        if (managerInst == null) {
            FSUtils.debug.error("FSSPFedTerminationHandler " +
                "Account Manager instance is null");
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSSPFedTerminationHandler::handleFederationTermination" +
                    "failed to get Account Manager instance");
            }
            return false;
        }
        
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message(
                "Begin processTerminationRequest SOAP profile...");
        }
        boolean bStatus = false;
        if (reqTermination != null) {
            boolean bUserStatus = setUserID(reqTermination);
            if (bUserStatus) {
                bStatus = updateAccountInformation(
                    reqTermination.getNameIdentifier());
                if (!bStatus) {
                    FSUtils.debug.error("FSFedTerminationHandler " +
                        FSUtils.bundle.getString(
                        IFSConstants.TERMINATION_REQUEST_PROCESSING_FAILED));
                    return false;
                } else {
                    FSUtils.debug.message("User sucessfully defederated");
                    // Call SP Adapter for remote IDP initiated SOAP case
                    if (hostedProviderRole != null &&
                        hostedProviderRole.equalsIgnoreCase(IFSConstants.SP))
                    {
                        FederationSPAdapter spAdapter =
                            FSServiceUtils.getSPAdapter(
                                hostedEntityId, hostedConfig);
                        if (spAdapter != null) {
                            FSUtils.debug.message(
                                "FSFedTerminationHandler.SOAP");
                            try {
                                spAdapter.postTerminationNotificationSuccess(
                                    hostedEntityId,
                                    request,
                                    response,
                                    userID,
                                    reqTermination,
                                    IFSConstants.TERMINATION_IDP_SOAP_PROFILE);
                            } catch (Exception e) {
                                // ignore adapter exception
                                FSUtils.debug.error("postTerm.IDP/SOAP", e);
                            }
                        }
                    }

                    return true;
                }
            } else {
                FSUtils.debug.message(
                    "Failed to get UserDN. Invalid termination request");
                return false;
            }
        } else{
            FSUtils.debug.error(
                "FSFedTerminationHandler::processTerminationRequest " +
                "Federation termination request is improper");
            return false;
        }
    }
    
    
    /**
     * Resets ederate cookie when termination is done with one remote provider.
     * If no active federations exists then the cookie is set to "no"; otherwise
     * it is set to "yes".
     */
    public void resetFederateCookie() {
        try {
            if (userID == null || userID.length() < 1) {
                return;
            } else {
                Cookie fedCookie;
                String cookieValue;
                if (managerInst.hasAnyActiveFederation(userID)) {
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message(
                            "User : " + userID +
                            " Federation Exists : " + IFSConstants.YES);
                    }
                    cookieValue = IFSConstants.YES;
                } else {
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message(
                            "User : " + userID +
                            " Federation Exists : " + IFSConstants.NO);
                    }
                    cookieValue = IFSConstants.NO;
                }
                FSUtils.debug.message("Setting Path to /");
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("Setting Age to " +
                        IFSConstants.PERSISTENT_COOKIE_AGE + " Age");
                }
                List cookieDomainList  = FSServiceUtils.getCookieDomainList();
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("Provider cookie domain list is " +
                        cookieDomainList);
                }
                Iterator iter = null;
                if(cookieDomainList != null) {
                    iter = cookieDomainList.iterator();
                    while(iter != null && iter.hasNext()) {
                        fedCookie = CookieUtils.newCookie(FEDERATE_COOKIE_NAME,
                                        cookieValue, 
                                        IFSConstants.PERSISTENT_COOKIE_AGE,
                                        "/", (String) iter.next());
                        CookieUtils.addCookieToResponse(response, fedCookie);
                    } 
                } else  {
                    fedCookie = CookieUtils.newCookie(FEDERATE_COOKIE_NAME,
                                        cookieValue, 
                                        IFSConstants.PERSISTENT_COOKIE_AGE,
                                        "/",null);
		    CookieUtils.addCookieToResponse(response, fedCookie);
                }
            }
        } catch (FSAccountMgmtException e) {
            FSUtils.debug.error("Unable to read user federation information",e);
            return;
        }
    }
    
    /**
     * Determines the return location and redirects based on
     * federation termination Return URL of the provider that sent the
     * termination request.
     */
    private void returnToSource() {
        FSUtils.debug.message(
            "Entered FSFedTerminationHandler::returnToSource");
        try {
            StringBuffer finalReturnURL = new StringBuffer();
            String retURL =
                remoteDescriptor.getFederationTerminationServiceReturnURL();
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("Redirecting to : " + retURL);
            }
            resetFederateCookie();
            FSUtils.debug.message("Checking retURL for null value");
            if (retURL == null || retURL.length() < 1) {
                FSUtils.debug.error("Return URL is null");
                
                FSServiceUtils.showErrorPage(
                    response,
                    error_page_url,
                    IFSConstants.TERMINATION_INVALID_REDIRECT_URL,
                    IFSConstants.METADATA_ERROR);
                return;
            } else {
                finalReturnURL.append(retURL);
                if (!(relayState == null || relayState.length() < 1)) {
                    char delimiter;
                    if (retURL.indexOf(IFSConstants.QUESTION_MARK) < 0) {
                        delimiter = IFSConstants.QUESTION_MARK;
                    } else {
                        delimiter = IFSConstants.AMPERSAND;
                    }
                    finalReturnURL.append(delimiter).
                        append(RELAY_STATE).
                        append(IFSConstants.EQUAL_TO).
                        append(URLEncDec.encode(relayState));
                }
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("Now sendRedirecting to : " +
                        finalReturnURL.toString());
                }
                response.sendRedirect(finalReturnURL.toString());
                return;
            }
        } catch (IOException e) {
            FSUtils.debug.error("Unable to get LRURL. No location to redirect."
            + " processing completed", e);
        }
        // create new bundle entry for redirect failure
        FSUtils.debug.message("After exception calling response.sendError");
        FSServiceUtils.showErrorPage(
            response,
            error_page_url,
            IFSConstants.TERMINATION_INVALID_REDIRECT_URL,
            IFSConstants.METADATA_ERROR);
        return;
    }
    
    /**
     * Signs Federation termination request before sending it to the remote 
     * provider.
     * @param msg <code>SOAPMessage</code> which includes termination request
     *  to be sent to remote provider
     * @param idAttrName name of the id attribute to be signed
     * @param id the value of the id attributer to be signed
     * @return signed termination request in <code>SOAPMessage</code>
     * @exception SAMLException if an error occurred during signing
     */
    protected SOAPMessage signTerminationRequest(
        SOAPMessage msg,
        String idAttrName,
        String id)
        throws SAMLException
    {
        FSUtils.debug.message(
            "FSSPFedTerminationHandler.signTerminationRequest: Called");
        String certAlias =  IDFFMetaUtils.getFirstAttributeValueFromConfig(
            hostedConfig, IFSConstants.SIGNING_CERT_ALIAS);
        if (certAlias == null || certAlias.length() == 0) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSSPFedTerminationHandler.signTerminationRequest: couldn't"
                    + "obtain this site's cert alias.");
            }
            throw new SAMLResponderException(
                FSUtils.bundle.getString(IFSConstants.NO_CERT_ALIAS));
        }
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message(
                "FSSPFedTerminationHandler.signTerminationRequest: Provider's "
                    + "certAlias is found: " + certAlias);
        }
        XMLSignatureManager manager = XMLSignatureManager.getInstance();
        Document doc = (Document)FSServiceUtils.createSOAPDOM(msg);
        String xpath = "//*[local-name()=\'ProviderID\']";
        
        manager.signXML(doc,
                certAlias,
                SystemConfigurationUtil.getProperty(
                    SAMLConstants.XMLSIG_ALGORITHM),
                idAttrName,
                id,
                false,
                xpath);
        return FSServiceUtils.convertDOMToSOAP(doc);
    }
    
    /**
     * Generates Federation termination request based onthe
     * <code>FSAccountFedInfo</code> object that represents the account 
     * federation for a user between 2 providers.
     * @param acctInfo represents the current user account information
     * @return termination request message
     */
    private FSFederationTerminationNotification
        createFederationTerminationRequest(FSAccountFedInfo acctInfo) 
    {
        FSUtils.debug.message(
            "FSFedTerminationHandler::createFederationTerminationRequest:");
        FSFederationTerminationNotification reqName =
            new FSFederationTerminationNotification();
        if (reqName != null) {
            NameIdentifier nameIdentifier = acctInfo.getRemoteNameIdentifier();
            if (nameIdentifier == null) {
                nameIdentifier = acctInfo.getLocalNameIdentifier();
            }
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("SP Provider Id : " + hostedEntityId);
            }
            reqName.setProviderId(hostedEntityId);
            reqName.setNameIdentifier(nameIdentifier);
            // TODO: Any more member settings + signature
            return reqName;
        } else {
            FSUtils.debug.message("failed to create termination request");
            FSUtils.debug.error(
                "FSFedTerminationHandler::createFederationTerminationRequest " +
                FSUtils.bundle.getString(
                    IFSConstants.TERMINATION_REQUEST_CREATION));
            return null;
        }
    }
    
    /**
     * Initiates federation termination at remote end.
     * The termination requested is constructed and based on the profile the
     * request is sent over SOAP or as HTTP redirect. Profile is always based on
     * the SPs profile
     * @param acctInfo represents the user account federation information
     * @return <code>true</code> if termination request is sent to remote
     *  provider successfully; <code>false</code> otherwise.
     */
    private boolean doFederationTermination(
        HttpServletRequest request,
        HttpServletResponse response,
        FSAccountFedInfo acctInfo)
    {
        FSUtils.debug.message(
            "Entered FSFedTerminationHandler::doFederationTermination");
        try {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSFedTerminationHandler::doFederationTermination create" +
                    " request start");
            }
            FSFederationTerminationNotification reqFedTermination =
                createFederationTerminationRequest(acctInfo);
            reqFedTermination.setMinorVersion(FSServiceUtils.getMinorVersion(
                    remoteDescriptor.getProtocolSupportEnumeration()));
            if (reqFedTermination == null) {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message(
                        "FSIDPFedTerminationHandler::Termination request could "
                        + "not be formed");
                }
                // Always show success page since local termination succeeded
                FSServiceUtils.returnLocallyAfterOperation(
                    response, termination_done_url, true,
                    IFSConstants.TERMINATION_SUCCESS, 
                    IFSConstants.TERMINATION_FAILURE);
                return false;
            }
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSIDPFedTerminationHandler::Termination request formed" +
                    "successfully");
            }
            // Find out which profile to use
            boolean isSOAPProfile = true;
            if (acctInfo.isRoleIDP()) {
                List hostProfiles = hostedDescriptor.
                    getFederationTerminationNotificationProtocolProfile();
                if (hostProfiles == null || hostProfiles.isEmpty()) {
                    FSUtils.debug.error("FSFedTerminationHandler::" +
                        "doFederationTermination no termination profile" +
                        " cannot process request");
                    FSServiceUtils.returnLocallyAfterOperation(
                        response, termination_done_url, true,
                        IFSConstants.TERMINATION_SUCCESS,
                        IFSConstants.TERMINATION_FAILURE);
                    return false;
                }
                String profile = (String) hostProfiles.iterator().next();
                if (profile.equalsIgnoreCase(
                        IFSConstants.TERMINATION_SP_SOAP_PROFILE) ||
                    profile.equalsIgnoreCase(
                        IFSConstants.TERMINATION_IDP_SOAP_PROFILE))
                {
                    isSOAPProfile = true;
                } else if (profile.equalsIgnoreCase(
                        IFSConstants.TERMINATION_SP_HTTP_PROFILE) ||
                    profile.equalsIgnoreCase(
                        IFSConstants.TERMINATION_IDP_HTTP_PROFILE))
                {
                    isSOAPProfile = false;
                } else {
                    FSUtils.debug.error("FSFedTerminationHandler::" +
                        "doFederationTermination Invalid termination profile" +
                        " cannot process request");
                    FSServiceUtils.returnLocallyAfterOperation(
                        response, termination_done_url, true,
                        IFSConstants.TERMINATION_SUCCESS,
                        IFSConstants.TERMINATION_FAILURE);
                    return false;
                }
            } else {
                List remoteProfiles = remoteDescriptor.
                    getFederationTerminationNotificationProtocolProfile();
                if (remoteProfiles == null || remoteProfiles.isEmpty()) {
                    FSUtils.debug.error("FSFedTerminationHandler::" +
                        "doFederationTermination no termination profile" +
                        " cannot process request");
                    FSServiceUtils.returnLocallyAfterOperation(
                        response, termination_done_url, true,
                        IFSConstants.TERMINATION_SUCCESS,
                        IFSConstants.TERMINATION_FAILURE);
                    return false;
                }

                String profile = (String)remoteProfiles.iterator().next();
                if (profile.equalsIgnoreCase(
                        IFSConstants.TERMINATION_SP_SOAP_PROFILE) ||
                    profile.equalsIgnoreCase(
                        IFSConstants.TERMINATION_IDP_SOAP_PROFILE))
                {
                    isSOAPProfile = true;
                } else if (profile.equalsIgnoreCase(
                        IFSConstants.TERMINATION_SP_HTTP_PROFILE) ||
                    profile.equalsIgnoreCase(
                        IFSConstants.TERMINATION_IDP_HTTP_PROFILE))
                {
                    isSOAPProfile = false;
                } else {
                    FSUtils.debug.error("FSFedTerminationHandler::" +
                        "doFederationTermination Invalid termination profile" +
                        " cannot process request");
                    FSServiceUtils.returnLocallyAfterOperation(
                        response, termination_done_url, true,
                        IFSConstants.TERMINATION_SUCCESS,
                        IFSConstants.TERMINATION_FAILURE);
                    return false;
                }
            }
            if (isSOAPProfile) {
                FSSOAPService instSOAP = FSSOAPService.getInstance();
                if (instSOAP != null) {
                    FSUtils.debug.message(
                        "Signing suceeded. To call bindTerminationRequest");
                    //String id = reqFedTermination.getRequestID();
                    reqFedTermination.setID(IFSConstants.TERMINATIONID);
                    SOAPMessage msgTermination = instSOAP.bind(
                        reqFedTermination.toXMLString(true, true));
                    if (msgTermination != null) {
                        try {
                            if (FSServiceUtils.isSigningOn()) {
                                int minorVersion =
                                    reqFedTermination.getMinorVersion();  
                                if (minorVersion ==
                                    IFSConstants.FF_11_PROTOCOL_MINOR_VERSION)
                                {
                                    msgTermination = signTerminationRequest(
                                        msgTermination,
                                        IFSConstants.ID,
                                        reqFedTermination.getID());
                                } else if(minorVersion ==
                                    IFSConstants.FF_12_PROTOCOL_MINOR_VERSION)
                                {
                                    msgTermination = signTerminationRequest(
                                        msgTermination,
                                        IFSConstants.REQUEST_ID,
                                        reqFedTermination.getRequestID());
                                } else { 
                                    FSUtils.debug.message(
                                        "invalid minor version."); 
                                }
                            }
                            boolean sendStatus = 
                                instSOAP.sendTerminationMessage(
                                    msgTermination,
                                    remoteDescriptor.getSoapEndpoint());
                            // Call SP Adapter for SP initiated SOAP profile
                            if (hostedProviderRole != null &&
                                hostedProviderRole.equalsIgnoreCase(
                                    IFSConstants.SP))
                            {
                                FederationSPAdapter spAdapter =
                                  FSServiceUtils.getSPAdapter(
                                      hostedEntityId, hostedConfig);
                                if (spAdapter != null) {
                                    try {
                                        spAdapter.
                                            postTerminationNotificationSuccess(
                                            hostedEntityId,
                                            request,
                                            response,
                                            userID,
                                            reqFedTermination,
                                            IFSConstants.
                                                TERMINATION_SP_SOAP_PROFILE);
                                    } catch (Exception e) {
                                        // ignore adapter exception
                                        FSUtils.debug.error("postTerm.SP/SOAP",
                                         e);
                                    }
                                }
                            }

                            // Always show success page since local termination
                            // succeeded and that is what is important
                            FSServiceUtils.returnLocallyAfterOperation(
                                response, termination_done_url, true,
                                IFSConstants.TERMINATION_SUCCESS,
                                IFSConstants.TERMINATION_FAILURE);
                            return sendStatus;
                        } catch (Exception e) {
                            FSUtils.debug.error(
                                "FSFedTerminationHandler::" +
                                "doFederationTermination " +
                                FSUtils.bundle.getString(
                                IFSConstants.TERMINATION_FAILED_SEND_REMOTE));
                            // Always show success page since local
                            // termination succeeded
                            FSServiceUtils.returnLocallyAfterOperation(
                                response, termination_done_url, true,
                                IFSConstants.TERMINATION_SUCCESS,
                                IFSConstants.TERMINATION_FAILURE);
                            return false;
                        }
                    } else {
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message(
                                "FSSPFedTerminationHandler::doFederation" +
                                "Termination failed. Error in forming Message");
                        }
                        FSUtils.debug.error(
                            "FSSPFedTerminationHandler.doFederationTermination "
                            + FSUtils.bundle.getString(
                                IFSConstants.TERMINATION_FAILED_SEND_REMOTE));
                        // Always show success page since local termination
                        // succeeded
                        FSServiceUtils.returnLocallyAfterOperation(
                            response, termination_done_url, true,
                            IFSConstants.TERMINATION_SUCCESS,
                            IFSConstants.TERMINATION_FAILURE);
                        return false;
                    }
                }
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message(
                        "FSFedTerminationHandler::doFederationTermination " +
                        "failed. Cannot get Service Manager instance");
                }
                FSUtils.debug.error(
                    "FSSPFedTerminationHandler::doFederationTermination " +
                    FSUtils.bundle.getString(
                        IFSConstants.TERMINATION_FAILED_SEND_REMOTE));
                // Always show success page since local termination succeeded
                FSServiceUtils.returnLocallyAfterOperation(
                    response, termination_done_url, true,
                    IFSConstants.TERMINATION_SUCCESS,
                    IFSConstants.TERMINATION_FAILURE);
                return false;
            } else {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message(
                        "FSFedTerminationHandler::doFederationTermination " +
                        "In Redirect profile");
                }
                String urlEncodedRequest =
                    reqFedTermination.toURLEncodedQueryString();
                // Sign the request querystring
                if (FSServiceUtils.isSigningOn()) {
                    String certAlias = 
                        IDFFMetaUtils.getFirstAttributeValueFromConfig(
                            hostedConfig, IFSConstants.SIGNING_CERT_ALIAS);
                    if (certAlias == null || certAlias.length() == 0) {
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message(
                                "FSBrowserArtifactConsumerHandler:: " +
                                "signSAMLRequest:" +
                                "couldn't obtain this site's cert alias.");
                        }
                        throw new SAMLResponderException(
                            FSUtils.bundle.getString(
                                IFSConstants.NO_CERT_ALIAS));
                    }
                    urlEncodedRequest =
                        FSSignatureUtil.signAndReturnQueryString(
                            urlEncodedRequest,
                            certAlias);
                }
                StringBuffer redirectURL = new StringBuffer();
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("Request to be sent : " +
                        urlEncodedRequest);
                }
                String retURL =
                    remoteDescriptor.getFederationTerminationServiceURL();
                redirectURL.append(retURL);
                if (retURL.indexOf(IFSConstants.QUESTION_MARK) == -1) {
                    redirectURL.append(IFSConstants.QUESTION_MARK);
                } else {
                    redirectURL.append(IFSConstants.AMPERSAND);
                }
                redirectURL.append(urlEncodedRequest);
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message(
                        "FSFedTerminationHandler::Redirect URL is " +
                        redirectURL.toString());
                }
                // Call SP Adaper for SP initiated HTTP profile
                // ideally this should be called from the
                // FSTerminationReturnServlet, but info not available there
                if (hostedProviderRole != null &&
                    hostedProviderRole.equalsIgnoreCase(IFSConstants.SP)) 
                {
                    FederationSPAdapter spAdapter = FSServiceUtils.getSPAdapter(
                        hostedEntityId, hostedConfig);
                    if (spAdapter != null) {
                        try {
                            spAdapter.postTerminationNotificationSuccess(
                                hostedEntityId,
                                request,
                                response,
                                userID,
                                reqFedTermination,
                                IFSConstants.TERMINATION_SP_HTTP_PROFILE);
                        } catch (Exception e) {
                            // ignore adapter exception
                            FSUtils.debug.error("postTerm.SP/HTTP", e);
                        }
                    }
                }
                response.sendRedirect(redirectURL.toString());
                return true;
            }
        } catch (IOException e) {
            FSUtils.debug.error("FSFedTerminationHandler" +
                FSUtils.bundle.getString(
                    IFSConstants.FEDERATION_REDIRECT_FAILED));
        } catch (FSMsgException e) {
            FSUtils.debug.error(
                "FSFedTerminationHandler::doFederationTermination " +
                FSUtils.bundle.getString(
                    IFSConstants.TERMINATION_FAILED_SEND_REMOTE));
        } catch (SAMLResponderException e) {
            FSUtils.debug.error(
                "FSFedTerminationHandler::doFederationTermination " +
                FSUtils.bundle.getString(
                    IFSConstants.TERMINATION_FAILED_SEND_REMOTE));
        }
        // Always show success page since local termination succeeded
        FSServiceUtils.returnLocallyAfterOperation(
            response, termination_done_url, true,
            IFSConstants.TERMINATION_SUCCESS, IFSConstants.TERMINATION_FAILURE);
        return false;
    }
}
