/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: AuthXMLRequestParser.java,v 1.12 2009/11/02 07:20:23 222713 Exp $
 *
 */

/*
 * Portions Copyrighted 2010-2012 ForgeRock Inc
 */

package com.sun.identity.authentication.server;

import java.io.ByteArrayInputStream;

import javax.security.auth.callback.Callback;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.authentication.AuthContext;
import com.sun.identity.authentication.service.AuthException;
import com.sun.identity.authentication.service.AuthUtils;
import com.sun.identity.authentication.service.LoginState;
import com.sun.identity.authentication.share.AuthXMLTags;
import com.sun.identity.authentication.share.AuthXMLUtils;
import com.sun.identity.authentication.util.ISAuthConstants;
import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.NodeList;

/**
 * <code>AuthXMLRequestParser</code> parses the XML data received from the
 * client.
 */
public class AuthXMLRequestParser {
    static Debug debug = Debug.getInstance("amXMLHandler");
    Document xmlDocument = null;
    AuthXMLRequest authXMLRequest = null;
    HttpServletRequest servletReq;

    /**
     * Create <code>AuthXMLRequestParser</code> object 
     * @param  xmlString reprsents request
     * @param req <code>HttpServletRequest</code> contains the request.
     */
    public AuthXMLRequestParser(String xmlString, HttpServletRequest req) {
        servletReq = req;
        try  {
           xmlDocument = XMLUtils.getXMLDocument(new ByteArrayInputStream(
                xmlString.toString().getBytes("UTF-8")));

           if (debug.messageEnabled()) {
               debug.message("AuthXMLRequestParser: in the constructor");
           }
       } catch (Exception e) {
            debug.message("AuthXMLRequest Parser error : " , e);
       }
    }


    /**
     * Parses the authentication request xml document. 
     * 
     * @return a AuthXMLRequest object.
     * @throws AuthException if it fails to parse the xml.
     */
    public AuthXMLRequest parseXML() throws AuthException {
        try {
            debug.message("entering parseXML"); 
            if (xmlDocument == null) {
                return null;
            }

            authXMLRequest = new AuthXMLRequest();
        
            // get the document root
            Element docElem = xmlDocument.getDocumentElement();
            // get the attributes for the root element

            if (docElem != null) {
                String temp = docElem.getAttribute("version");
                if (debug.messageEnabled()) {
                    debug.message("Request Version is.. : " + temp);
                }
                if (temp != null) {
                    authXMLRequest.setRequestVersion(temp);
                }

                Node requestNode = XMLUtils.getChildNode(
                    (Node)docElem,"Request");
                String authIdentifier = null;
                if (requestNode != null) {
                    authIdentifier = 
                        parseNodeAttributes(requestNode,"authIdentifier");
                 
                    if (debug.messageEnabled()) {
                        debug.message("AuthIdentifier is : " + authIdentifier);
                    }
                    authXMLRequest.setAuthIdentifier(authIdentifier);
                }

	        Node appSSOTokenNode = XMLUtils.getChildNode((Node) requestNode,"AppSSOToken");
                if (appSSOTokenNode != null) {
                    debug.message("Got the SSO Token node: ");
                    String appSSOTokenID = XMLUtils.getValueOfValueNode(appSSOTokenNode);
                    if (appSSOTokenID != null) {
                        if (debug.messageEnabled()) {
                            debug.message("Got the Session Id: "+appSSOTokenID);
                        }
                        authXMLRequest.setAppSSOTokenID(appSSOTokenID);
                    }
                }

                // get the Nodes for the Request Element

                // get new auth context node 
                Node newAuthContextNode = XMLUtils.getChildNode(
                    requestNode,"NewAuthContext");
                if (newAuthContextNode != null) {
                    String orgName =
                        parseNodeAttributes(newAuthContextNode,"orgName");
                    authXMLRequest.setOrgName(orgName);
                    authXMLRequest.setRequestType(
                        AuthXMLRequest.NewAuthContext);
                    AuthContextLocal authContext = 
                        AuthUtils.getAuthContext(orgName,authIdentifier,false,
                            servletReq, null, null);
                    authXMLRequest.setAuthContext(authContext);
                }
        
                // get query node 

                Node queryInfoNode = 
                    XMLUtils.getChildNode(requestNode,"QueryInformation");
                if (queryInfoNode != null) {
                    String queryType = parseNodeAttributes(
                        queryInfoNode,"requestedInformation");
                    authXMLRequest.setRequestInformation(queryType);
                    authXMLRequest.setRequestType(
                        AuthXMLRequest.QueryInformation);
                    String orgName = parseNodeAttributes(
                        queryInfoNode, "orgName");
                    AuthContextLocal authContext = null;

                    if (orgName != null) {
                        authContext = AuthUtils.getAuthContext(
                            orgName, servletReq);
                    } else {
                        authContext = AuthUtils.getAuthContext(
                            null, authIdentifier,false);
                    }
                    authXMLRequest.setAuthContext(authContext);
                }

                // get login node 
                Node loginNode = XMLUtils.getChildNode(requestNode,"Login");
                if (loginNode != null) {
                    debug.message("found login node !!");
                    String orgName =
                        parseNodeAttributes(loginNode,"orgName");

                    //Let's set the request type to Login by default
                    authXMLRequest.setRequestType(AuthXMLRequest.Login);
                    //this method can change the default requesttype to
                    //LoginIndex type if indexname/indextype was supplied in the
                    //request
                    parseLoginNodeElements(loginNode, authXMLRequest);
                    AuthContext.IndexType indexType = authXMLRequest.getIndexType();
                    String indexTypeParam = convertIndexType(indexType);
                    String indexName = authXMLRequest.getIndexName();
                    if (indexType == AuthContext.IndexType.COMPOSITE_ADVICE) {
                        //realm name from policy advice has precedence over
                        //the orgName attribute
                        orgName = AuthUtils.getRealmFromPolicyAdvice(indexName);
                    }

                    AuthContextLocal authContext = null;
                    if (orgName != null) {
                        authXMLRequest.setOrgName(orgName);
                    }
                    String hostName = 
                        parseNodeAttributes(loginNode,"hostName");
                    if (hostName != null) {
                        authXMLRequest.setHostName(hostName);
                    }
                    String localeAttr =
                        parseNodeAttributes(loginNode,AuthXMLTags.LOCALE);
                    if (localeAttr != null) {
                        authXMLRequest.setLocale(localeAttr);
                    }
                    String forceAuth = 
                        parseNodeAttributes(loginNode,"forceAuth");
                    if (forceAuth != null) {
                        authXMLRequest.setForceAuth(forceAuth);
                        if (debug.messageEnabled()) {
                            debug.message("AuthXMLRequestParser.parseXML: "
                            + "Got the force auth flag: "+forceAuth);
                        }
                    }
                    boolean forceAuthBool = Boolean.parseBoolean(forceAuth);  
                    authContext =
                        AuthUtils.getAuthContext(orgName,authIdentifier,false,
                            servletReq, indexTypeParam,
                            authXMLRequest,forceAuthBool);
                    authXMLRequest.setAuthContext(authContext);
                    if (localeAttr != null) {
                        LoginState loginState = authContext.getLoginState();
                        loginState.setRemoteLocale(localeAttr);
                    }

                    HttpServletRequest clientRequest =
                        AuthXMLUtils.getRemoteRequest(XMLUtils.getChildNode(requestNode,AuthXMLTags.REMOTE_REQUEST_RESPONSE));
                    HttpServletResponse clientResponse =
                        AuthXMLUtils.getRemoteResponse(XMLUtils.getChildNode(requestNode,AuthXMLTags.REMOTE_REQUEST_RESPONSE));
                    authXMLRequest.setClientRequest(clientRequest);
                    authXMLRequest.setClientResponse(clientResponse);
                }

                // get submit requirements node
                Node submitReqNode = XMLUtils.getChildNode(
                    requestNode, "SubmitRequirements");
                if (submitReqNode != null) {
                    authXMLRequest.setRequestType(
                        AuthXMLRequest.SubmitRequirements);
                    AuthContextLocal authContext = AuthUtils.getAuthContext(
                        servletReq, authIdentifier);
                    authXMLRequest.setAuthContext(authContext);
                    Callback[] callbacks = AuthUtils.getRecdCallback(
                        authContext);
                    parseSubmitReqElements(
                        submitReqNode, authXMLRequest, callbacks);
		    String localeStr = authXMLRequest.getLocale();
                    LoginState loginState = authContext.getLoginState();
                    loginState.setRemoteLocale(localeStr);

                    HttpServletRequest clientRequest =
                        AuthXMLUtils.getRemoteRequest(XMLUtils.getChildNode(requestNode,AuthXMLTags.REMOTE_REQUEST_RESPONSE));
                    HttpServletResponse clientResponse =
                        AuthXMLUtils.getRemoteResponse(XMLUtils.getChildNode(requestNode,AuthXMLTags.REMOTE_REQUEST_RESPONSE));
                    authXMLRequest.setClientRequest(clientRequest);
                    authXMLRequest.setClientResponse(clientResponse);
                }

                // get  logout node
                Node logoutNode = XMLUtils.getChildNode(requestNode,"Logout");
                if (logoutNode != null) {
                    authXMLRequest.setRequestType(AuthXMLRequest.Logout);
                }

                // get abort node
                Node abortNode = XMLUtils.getChildNode(requestNode,"Abort");
                if (abortNode!= null) {
                    authXMLRequest.setRequestType(AuthXMLRequest.Abort);
                    AuthContextLocal authContext =
                        AuthUtils.getAuthContext(null,authIdentifier,true);
                    authXMLRequest.setAuthContext(authContext);
                }
            }
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            debug.message("Error in parseXML: : " , e);
        }

        return authXMLRequest;
    }
     /* Converts IndexType param to query patameter String */
     private String convertIndexType(AuthContext.IndexType index) {
         String indexTypeParam = null;
         if (index == AuthContext.IndexType.SERVICE) {
             indexTypeParam = "service";
         } else if (index == AuthContext.IndexType.LEVEL) {
             indexTypeParam = "authlevel";
         } else if (index == AuthContext.IndexType.ROLE) {
             indexTypeParam = "role";
         } else if (index == AuthContext.IndexType.
             MODULE_INSTANCE) {
             indexTypeParam = "module";
         } else if (index == AuthContext.IndexType.USER) {
             indexTypeParam = "user";
         } else if (index == AuthContext.IndexType.
             COMPOSITE_ADVICE) {
             indexTypeParam = "sunamcompositeadvice";
         } else if (index == AuthContext.IndexType.RESOURCE) {
             indexTypeParam = ISAuthConstants.IP_RESOURCE_ENV_PARAM;
         }
         return indexTypeParam;
     }

    /* get the attribute value for a node */
    private String parseNodeAttributes(Node requestNode,String attrName) {
        try {
            if (requestNode == null) {
                return null;
            }

            String attrValue = 
                XMLUtils.getNodeAttributeValue(requestNode,attrName);

            if (debug.messageEnabled()) {
                debug.message("Attr Value is : " + attrValue);
            }

            return attrValue;
        } catch (Exception e) {
            if (debug.messageEnabled()) {
                debug.message("Error getting " + attrName);
                debug.message("Exception " ,e);
            }
            return null;
        }
    }

    /* parse the login node elements */
    private void parseLoginNodeElements(
        Node loginNode,
        AuthXMLRequest authXMLRequest) {
        authXMLRequest.setRequestType(AuthXMLRequest.Login);
        // get the  the Login Nodes and their values.

        /* get the indexType , indexName */

        Node indexTypeNamePair =
            XMLUtils.getChildNode(loginNode, AuthXMLTags.INDEX_TYPE_PAIR);

        if (indexTypeNamePair != null) {
            String indexType =
                parseNodeAttributes(indexTypeNamePair, AuthXMLTags.INDEX_TYPE);
            if (debug.messageEnabled()) {
                debug.message("indexType is .. : " + indexType);
            }
            authXMLRequest.setIndexType(indexType);
            Node indexNameNode = XMLUtils.getChildNode(
                indexTypeNamePair, AuthXMLTags.INDEX_NAME);

            if (indexNameNode != null) {
                String indexName =
                    XMLUtils.getValueOfValueNode(indexNameNode);
                if (debug.messageEnabled()) {
                    debug.message("indexName is .. : " + indexName);
                }
                authXMLRequest.setIndexName(indexName);
            }
            authXMLRequest.setRequestType(AuthXMLRequest.LoginIndex);
        } 

        Node localeNode = XMLUtils.getChildNode(loginNode, AuthXMLTags.LOCALE);
        if (localeNode != null) {
            authXMLRequest.setLocale(XMLUtils.getValueOfValueNode(localeNode));
        }

        // get the default values for callbacks if any.
        Node paramsNode = XMLUtils.getChildNode(loginNode,AuthXMLTags.PARAMS);
        if (paramsNode != null) {
            authXMLRequest.setParams(XMLUtils.getValueOfValueNode(paramsNode));
        }
        
        // get the values for environment if any
        Node envNode = XMLUtils.getChildNode(loginNode, 
            AuthXMLTags.ENVIRONMENT);
        if (envNode != null) {
            NodeList cList = envNode.getChildNodes();
            List values = new ArrayList();
            int len = cList.getLength();
            for (int i = 0; i < len; i++) {
                Node node = cList.item(i);
                if (node.getNodeName().equals(AuthXMLTags.ENV_VALUE)) {
                    values.add(XMLUtils.getValueOfValueNode(node));
                }
            }
            if (!values.isEmpty()) {
                authXMLRequest.setEnvironment(values);
            }
        }
    }

    /* parse submit requirements node */
    void parseSubmitReqElements(
        Node submitReqNode,
        AuthXMLRequest authXMLRequest,
        Callback[] recdCallbacks) {
        Node callbacksNode = XMLUtils.getChildNode(submitReqNode,"Callbacks");
        Callback[] submittedCallbacks = 
                AuthXMLUtils.getCallbacks(callbacksNode,true,recdCallbacks);
        authXMLRequest.setSubmittedCallbacks(submittedCallbacks);

        Node localeNode =
            XMLUtils.getChildNode(submitReqNode, "Locale");
        if (localeNode != null) {
            String localeValue =
                XMLUtils.getValueOfValueNode(localeNode);
            if (debug.messageEnabled()) {
               debug.message("locale is .. : " + localeValue);
            }
            authXMLRequest.setLocale(localeValue);
        }
        return;
    }
}
