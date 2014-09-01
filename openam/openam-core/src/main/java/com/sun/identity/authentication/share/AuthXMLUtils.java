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
 * $Id: AuthXMLUtils.java,v 1.10 2009/06/19 20:39:09 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted 2010-2011 ForgeRock AS
 */

package com.sun.identity.authentication.share;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.shared.encode.Base64;
import com.sun.identity.authentication.spi.DSAMECallbackInterface;
import com.sun.identity.authentication.spi.HttpCallback;
import com.sun.identity.authentication.spi.PagePropertiesCallback;
import com.sun.identity.authentication.spi.RedirectCallback;
import com.sun.identity.authentication.spi.X509CertificateCallback;
import com.sun.identity.security.DecodeAction;
import com.sun.identity.security.EncodeAction;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.AccessController;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ChoiceCallback;
import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.callback.LanguageCallback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.TextInputCallback;
import javax.security.auth.callback.TextOutputCallback;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class AuthXMLUtils {
    
    static Debug debug = Debug.getInstance("amAuthXMLUtils");

    /**
     * TODO-JAVADOC
     */
    public static Callback[] getCallbacks(Node callbackNode,boolean noFilter) {
        try {
            return getCallbacks(callbackNode,noFilter,null);
        } catch (Exception e) {
            debug.message("Exception . " ,e);
            return null;
        }
    }
    
    /**
     * TODO-JAVADOC
     */
    public static Callback[] getCallbacks(
        Node callbackNode,
        boolean noFilter,
        Callback[] callbacks) {
        if (callbackNode == null) {
            return (null);
        }
        
        // Get the length attribute and construct the callbacks
        String lenString = XMLUtils.getNodeAttributeValue(
            callbackNode, AuthXMLTags.LENGTH);
        int length = Integer.parseInt(lenString);
        if (debug.messageEnabled()) {
            debug.message("Callbacks length is : " + length);
            if (callbacks != null) {
                for (int k=0;k < callbacks.length;k++) {
                    debug.message("callback is : " + callbacks[k]);
                }
            }
        }
        
        Callback[] answer = new Callback[0];
        ArrayList callbackList = new ArrayList();
        Callback callback = null;
        // Get the child nodes and construct the callbacks
        NodeList childNodes = callbackNode.getChildNodes();
        if (childNodes == null) {
            return null;
        }
        
        // following indexes are to keep track of order of
        // callback in the callback array.
        int nameIndex=0;
        int passIndex=0;
        int ccIndex=0;
        int concIndex=0;
        int tiIndex=0;
        int toIndex=0;
        int lcIndex=0;
        int ppIndex=0;
        int diIndex=0;
        int authCallbackIndex=0;
        
        for (int i = 0; i < childNodes.getLength(); i++) {
            
            Node childNode = childNodes.item(i);
            String childNodeName = childNode.getNodeName();
            if (childNodeName.equals(AuthXMLTags.NAME_CALLBACK)) {
                if (callbacks != null) {
                    nameIndex= getNameCallbackIndex(callbacks,nameIndex);
                    if (nameIndex >= 0){
                        callbackList.add(
                        createNameCallback(childNode,callbacks[nameIndex]));
                    }
                    nameIndex = nameIndex +1;
                } else {
                    callbackList.add(createNameCallback(childNode,null));
                }
            } else if (childNodeName.equals(AuthXMLTags.PASSWORD_CALLBACK)) {
                if (callbacks != null) {
                    passIndex= getPasswordCallbackIndex(callbacks,passIndex);
                    if (passIndex >= 0) {
                        callbackList.add(createPasswordCallback(childNode,
                        callbacks[passIndex]));
                    }
                    passIndex= passIndex+1;
                } else {
                    callbackList.add(createPasswordCallback(childNode,null));
                }
            } else if (childNodeName.equals(AuthXMLTags.CHOICE_CALLBACK)) {
                if (callbacks != null) {
                    ccIndex = getChoiceCallbackIndex(callbacks,ccIndex);
                    if (ccIndex >= 0) {
                        callbackList.add(createChoiceCallback(childNode,
                        callbacks[ccIndex]));
                    }
                    ccIndex = ccIndex + 1;
                } else {
                    callbackList.add(createChoiceCallback(childNode,null));
                }
            } else if (childNodeName.equals(AuthXMLTags.CONFIRMATION_CALLBACK)){
                if (callbacks != null) {
                    concIndex = getConfCallbackIndex(callbacks,concIndex);
                    if (concIndex >= 0) {
                        callbackList.add(createConfirmationCallback(childNode,
                        callbacks[concIndex]));
                    }
                    concIndex = concIndex+1;
                } else {
                    callbackList.add(
                        createConfirmationCallback(childNode,null));
                }
            } else if (childNodeName.equals(AuthXMLTags.TEXT_INPUT_CALLBACK)) {
                if (callbacks != null) {
                    tiIndex = getTextInputIndex(callbacks,tiIndex);
                    if ( tiIndex >= 0) {
                        callbackList.add(createTextInputCallback(childNode,
                        callbacks[tiIndex]));
                    }
                    tiIndex = tiIndex + 1;
                } else {
                    callbackList.add(createTextInputCallback(childNode,null));
                }
            } else if (childNodeName.equals(AuthXMLTags.TEXT_OUTPUT_CALLBACK)) {
                if (callbacks != null) {
                    toIndex = getTextOutputIndex(callbacks,toIndex);
                    if ( toIndex >= 0) {
                        callbackList.add(createTextOutputCallback(childNode,
                        callbacks[toIndex]));
                    }
                    toIndex = toIndex + 1;
                } else {
                    callbackList.add(createTextOutputCallback(childNode,null));
                }
            } else if (
                childNodeName.equals(AuthXMLTags.PAGE_PROPERTIES_CALLBACK)
                    && noFilter
            ) {
                if (callbacks != null) {
                    ppIndex = getPagePropertiesIndex(callbacks,ppIndex);
                    if ( ppIndex >= 0) {
                        callbackList.add(createPagePropertiesCallback(
                        childNode,callbacks[ppIndex]));
                    }
                    ppIndex = ppIndex + 1;
                } else {
                    callbackList.add(createPagePropertiesCallback(childNode,
                    null));
                }
            } else if (childNodeName.equals(AuthXMLTags.LANGUAGE_CALLBACK)) {
                if (callbacks != null) {
                    lcIndex = getLanguageCallbackIndex(callbacks,lcIndex);
                    if (lcIndex >= 0) {
                        callbackList.add(createLanguageCallback(childNode,
                        callbacks[lcIndex]));
                    }
                    lcIndex = lcIndex + 1;
                } else {
                    callbackList.add(createLanguageCallback(childNode,null));
                }
            } else if (childNodeName.equals(AuthXMLTags.SAML_CALLBACK)) {
                AuthenticationCallbackXMLHelper callbackXMLHelper =
                AuthenticationCallbackXMLHelperFactory.getCallbackXMLHelper();
                if (callbackXMLHelper != null) {
                    if (callbacks != null) {
                        authCallbackIndex = 
                            callbackXMLHelper.getAuthenticationCallbackIndex(
                                callbacks,authCallbackIndex);
                        if (authCallbackIndex >= 0) {
                            callbackList.add(
                                callbackXMLHelper.createAuthenticationCallback(
                                    childNode,callbacks[authCallbackIndex]));
                        }
                        authCallbackIndex = authCallbackIndex + 1;
                    } else {
                        callbackList.add(
                            callbackXMLHelper.createAuthenticationCallback(
                                childNode,null));
                    }
                }
            } else if (childNodeName.equals(
                       AuthXMLTags.X509CERTIFICATE_CALLBACK)) {
                if (callbacks != null) {
                    lcIndex =
                        getX509CertificateCallbackIndex(callbacks,lcIndex);
                    if (lcIndex >= 0) {
                        callbackList.add(
                                createX509CertificateCallback(childNode,
                                callbacks[lcIndex]));
                    }
                    lcIndex = lcIndex + 1;
                } else {
                    callbackList.add(
                            createX509CertificateCallback(childNode,null));
                }
            } else if (childNodeName.equals(
                    AuthXMLTags.HTTP_CALLBACK)) {
                if (callbacks != null) {
                    diIndex = getHttpCallbackIndex(callbacks,ppIndex);
                    if (diIndex >= 0) {
                        callbackList.add(createHttpCallback(childNode,
                                callbacks[diIndex]));
                    }
                } else {
                    callbackList.add(createHttpCallback(childNode,null));
                }
            } else if (childNodeName.equals(AuthXMLTags.CUSTOM_CALLBACK)) {
                if (callbacks != null) {
                    diIndex = getCustomCallbackIndex(callbacks,ppIndex);
                    if ( diIndex >= 0) {
                        callbackList.add(createCustomCallback(childNode,
                        callbacks[diIndex]));
                    }
                    diIndex = diIndex + 1;
                } else {
                    callbackList.add(createCustomCallback(childNode,null));
                }
            } else if (childNodeName.equals(AuthXMLTags.REDIRECT_CALLBACK)) {
                 if (callbacks != null) {
                     diIndex = getRedirectCallbackIndex(callbacks,ppIndex);
                     if ( diIndex >= 0) {
                         callbackList.add(createRedirectCallback(childNode,
                         callbacks[diIndex]));
                     }
                     diIndex = diIndex + 1;
                 } else {
                     callbackList.add(createRedirectCallback(childNode,null));
                }
            }
        }
        
        return (Callback[]) callbackList.toArray(answer);
    }

    /**
     * Returns the remote HttpServletRequest object from the embedded serialized
     * content in the XML document
     *
     * @param requestNode The request xml node
     * @return The Http Servlet Request object
     */
    public static HttpServletRequest getRemoteRequest(Node requestNode) {
        if (requestNode == null)
            return null;

        Object obj = null;

        try {
            obj = deserializeToObject(
                    getValueOfChildNode(requestNode, AuthXMLTags.HTTP_SERVLET_REQUEST));
        } catch (IOException ioe) {
            debug.error("Unable to deserialize request object", ioe);
        } catch (ClassNotFoundException cnfe) {
            debug.error("Unable to load class", cnfe);
        }

        return (HttpServletRequest) obj;
    }

    /**
     * Returns the remote HttpServletResponse object from the embedded serialized
     * content in the XML document
     *
     * @param responseNode The response xml node
     * @return The Http Servlet Response object
     */
    public static HttpServletResponse getRemoteResponse(Node responseNode) {
        if (responseNode == null)
            return null;

        Object obj = null;

        try {
            obj = deserializeToObject(
                    getValueOfChildNode(responseNode, AuthXMLTags.HTTP_SERVLET_RESPONSE));
        } catch (IOException ioe) {
            debug.error("Unable to deserialize response object", ioe);
        } catch (ClassNotFoundException cnfe) {
            debug.error("Unable to load class", cnfe);
        }

        return (HttpServletResponse) obj;
    }
    
    /**
     * TODO-JAVADOC
     */
    public static String getXMLForCallbacks(Callback[] callbacks) {
        if (callbacks == null) {
            return ("");
        }
        
        // Construct the xml string
        StringBuilder xmlString = new StringBuilder();
        xmlString.append(AuthXMLTags.CALLBACKS_BEGIN)
            .append(AuthXMLTags.SPACE)
            .append(AuthXMLTags.LENGTH)
            .append(AuthXMLTags.EQUAL)
            .append(AuthXMLTags.QUOTE)
            .append(callbacks.length)
            .append(AuthXMLTags.QUOTE)
            .append(AuthXMLTags.ELEMENT_END);
        
        for (int i = 0; i < callbacks.length; i++) {
            if (callbacks[i] instanceof NameCallback) {
                NameCallback nameCallback = (NameCallback) callbacks[i];
                xmlString.append(getNameCallbackXML(nameCallback));
            } else if (callbacks[i] instanceof PasswordCallback) {
                PasswordCallback passwordCallback =
                    (PasswordCallback) callbacks[i];
                xmlString.append(getPasswordCallbackXML(passwordCallback));
            } else if (callbacks[i] instanceof ChoiceCallback) {
                ChoiceCallback choiceCallback = (ChoiceCallback) callbacks[i];
                xmlString.append(getChoiceCallbackXML(choiceCallback));
            } else if (callbacks[i] instanceof ConfirmationCallback) {
                ConfirmationCallback conCallback =
                    (ConfirmationCallback) callbacks[i];
                xmlString.append(getConfirmationCallbackXML(conCallback));
            } else if (callbacks[i] instanceof TextInputCallback) {
                TextInputCallback textInputCallback =
                    (TextInputCallback) callbacks[i];
                xmlString.append(getTextInputCallbackXML(textInputCallback));
            } else if (callbacks[i] instanceof TextOutputCallback) {
                TextOutputCallback textOutputCallback =
                    (TextOutputCallback)callbacks[i];
                xmlString.append(getTextOutputCallbackXML(textOutputCallback));
            } else if (callbacks[i] instanceof PagePropertiesCallback) {
                PagePropertiesCallback pagePCallback
                =(PagePropertiesCallback) callbacks[i];
                xmlString.append(getPagePropertiesCallbackXML(pagePCallback));
            } else if (callbacks[i] instanceof LanguageCallback) {
                LanguageCallback lc = (LanguageCallback) callbacks[i];
                xmlString.append(getLanguageCallbackXML(lc));
            } else if (callbacks[i] instanceof X509CertificateCallback) {
                X509CertificateCallback xc =
                    (X509CertificateCallback) callbacks[i];
                xmlString.append(getX509CertificateCallbackXML(xc));
            } else if (callbacks[i] instanceof HttpCallback) {
                HttpCallback hc = (HttpCallback)callbacks[i];
                xmlString.append(getHttpCallbackXML(hc));
            } else if (callbacks[i] instanceof DSAMECallbackInterface) {
                DSAMECallbackInterface dsameCallback =
                (DSAMECallbackInterface) callbacks[i];
                xmlString.append(getCustomCallbackXML(dsameCallback));
            } else if (callbacks[i] instanceof RedirectCallback) {
                RedirectCallback redirectCallback =
                         (RedirectCallback) callbacks[i];
                xmlString.append(getRedirectCallbackXML(redirectCallback));
            } else {
                AuthenticationCallbackXMLHelper callbackXMLHelper =
                AuthenticationCallbackXMLHelperFactory.getCallbackXMLHelper();
                if (callbackXMLHelper != null) {
                    xmlString.append(
                        callbackXMLHelper.getAuthenticationCallbackXML(
                            callbacks[i]));
                }
            }            
        }
        
        xmlString.append(AuthXMLTags.CALLBACKS_END);
        return (xmlString.toString());
    }
    
    static NameCallback createNameCallback(Node childNode, Callback callback) {
        String prompt = getPrompt(childNode);
        
        NameCallback nameCallback = null;
        if (callback != null) {
            if (callback instanceof NameCallback) {
                nameCallback = (NameCallback) callback;
            }
        }
        
        if (nameCallback == null) {
            String defaultValue=getDefaultValue(childNode);
            if (defaultValue == null) {
                nameCallback = new NameCallback(prompt);
            } else {
                nameCallback = new NameCallback(prompt,defaultValue);
            }
        }
        
        String value = getValue(childNode);
        if (debug.messageEnabled()) {
            debug.message("Value is : " + value);
        }
        
        if (value != null) {
            nameCallback.setName(value);
        }
        
        return nameCallback;
    }
    
    static PasswordCallback createPasswordCallback(
        Node childNode,
        Callback callback) {
        String prompt = getPrompt(childNode);
        
        boolean echoPassword = false;
        String echoPasswordAttr = XMLUtils.getNodeAttributeValue(
        childNode, AuthXMLTags.ECHO_PASSWORD);
        if ((echoPasswordAttr != null) &&
        echoPasswordAttr.equals("true")) {
            echoPassword = true;
        }
        PasswordCallback passwordCallback = null;
        if (callback != null) {
            if (callback instanceof PasswordCallback) {
                passwordCallback = (PasswordCallback)callback;
            }
        }
        
        if (passwordCallback == null) {
            passwordCallback = new PasswordCallback(prompt,echoPassword);
        }
        
        String value = getValueNoTrim(childNode);
        
        /** if (debug.messageEnabled()) {
         *     debug.message("Value is : " + value);
         *}
         */
        
        if (value != null) {
            passwordCallback.setPassword(value.toCharArray());
        }
        
        return passwordCallback;
    }
    
    static ChoiceCallback createChoiceCallback(
        Node childNode,
        Callback callback) {
        ChoiceCallback choiceCallback = null;
        if (callback != null) {
            if (callback instanceof ChoiceCallback) {
                choiceCallback = (ChoiceCallback)callback;
            }
        }
        
        if (choiceCallback == null) {
            String prompt = getPrompt(childNode);
            boolean multiSelect = false;
            String multiSelectAttr = XMLUtils.getNodeAttributeValue(
            childNode, AuthXMLTags.MULTI_SELECT_ALLOWED);
            if ((multiSelectAttr != null) &&
            multiSelectAttr.equals("true")) {
                multiSelect = true;
            }
            
            String[] choices = null;
            int defaultChoice = 0;
            Node choicesNode =
            XMLUtils.getChildNode(childNode, AuthXMLTags.CHOICE_VALUES);
            NodeList choicesChildNodes = choicesNode.getChildNodes();
            choices = new String[choicesChildNodes.getLength()];
            for (int j = 0; j < choicesChildNodes.getLength(); j++) {
                Node choiceValueNode = choicesChildNodes.item(j);
                String isDefaultAttr = XMLUtils.getNodeAttributeValue(
                choiceValueNode, AuthXMLTags.IS_DEFAULT);
                if ((isDefaultAttr != null) &&
                isDefaultAttr.equals("yes")) {
                    defaultChoice = j;
                }
                String choiceValue = getValue(choiceValueNode);
                choices[j] = choiceValue;
            }
            
            choiceCallback = new ChoiceCallback(prompt, choices,
            defaultChoice, multiSelect);
        }
        
        int[] selectedIndexes;
        Node selectedNode = XMLUtils.getChildNode(childNode,
        AuthXMLTags.SELECTED_VALUES);
        if ( selectedNode != null ) {
            NodeList selectChildNodes = selectedNode.getChildNodes();
            selectedIndexes = new int[selectChildNodes.getLength()];
            for (int j = 0; j < selectChildNodes.getLength(); j++) {
                Node selectValueNode = selectChildNodes.item(j);
                selectedIndexes[j] =
                Integer.parseInt(XMLUtils.getValueOfValueNode(selectValueNode));
            }
            if (choiceCallback.allowMultipleSelections()) {
                choiceCallback.setSelectedIndexes(selectedIndexes);
            } else {
                choiceCallback.setSelectedIndex(selectedIndexes[0]);
            }
        }
        
        return choiceCallback;
    }
    
    static ConfirmationCallback createConfirmationCallback(
        Node childNode,
        Callback callback) {
        ConfirmationCallback conCallback = null;
        if (callback != null) {
            if (callback instanceof ConfirmationCallback) {
                conCallback = (ConfirmationCallback) callback;
            }
        }
        if (conCallback ==  null) {
            String prompt = getPrompt(childNode);
            int messageType = 0;
            String msgType = XMLUtils.getNodeAttributeValue(
            childNode, AuthXMLTags.MESSAGE_TYPE);
            if (msgType.equals("information")) {
                messageType = ConfirmationCallback.INFORMATION;
            } else if (msgType.equals("error")) {
                messageType = ConfirmationCallback.ERROR;
            } else if (msgType.equals("warning")) {
                messageType = ConfirmationCallback.WARNING;
            }
            
            boolean bOptions = false;
            int optionType = 0;
            String optType = XMLUtils.getNodeAttributeValue(
            childNode, AuthXMLTags.OPTION_TYPE);
            if ( optType != null ) {
                if (optType.equals("yes_no")) {
                    optionType = ConfirmationCallback.YES_NO_OPTION;
                } else if (optType.equals("yes_no_cancel")) {
                    optionType = ConfirmationCallback.YES_NO_CANCEL_OPTION;
                } else if (optType.equals("ok_cancel")) {
                    optionType = ConfirmationCallback.OK_CANCEL_OPTION;
                } else if (optType.equals("unspecified")) {
                    optionType = ConfirmationCallback.UNSPECIFIED_OPTION;
                    bOptions = true;
                }
            }
            
            String[] options = null;
            Node optionsNode = XMLUtils.getChildNode(childNode,
            AuthXMLTags.OPTION_VALUES);
            if ( optionsNode != null ) {
                NodeList optionsChildNodes = optionsNode.getChildNodes();
                options = new String[optionsChildNodes.getLength()];
                for (int j = 0; j < optionsChildNodes.getLength(); j++) {
                    Node optionValueNode = optionsChildNodes.item(j);
                    String optionValue = getValue(optionValueNode);
                    options[j] = optionValue;
                }
            }
            
            Node defaultNode = XMLUtils.getChildNode(childNode,
            AuthXMLTags.DEFAULT_OPTION_VALUE);
            String defaultValue = getValue(defaultNode);
            int defaultOption = Integer.parseInt(defaultValue);
            
            if ( prompt != null ) {
                if ( bOptions ) {
                    conCallback = new ConfirmationCallback(prompt, messageType,
                    options, defaultOption);
                } else {
                    conCallback = new ConfirmationCallback(prompt, messageType,
                    optionType,defaultOption);
                }
            } else {
                if ( bOptions ) {
                    conCallback = new ConfirmationCallback(messageType, options,
                    defaultOption);
                } else {
                    conCallback = new ConfirmationCallback(
                        messageType, optionType,
                    defaultOption);
                }
            }
        }
        
        Node selectedNode = XMLUtils.getChildNode(childNode,
        AuthXMLTags.SELECTED_VALUE);
        if ( selectedNode != null ) {
            String selectedValue = getValue(selectedNode);
            int selectedOption = Integer.parseInt(selectedValue);
            conCallback.setSelectedIndex(selectedOption);
        }
        
        return conCallback;
    }
    
    static TextInputCallback createTextInputCallback(
        Node childNode,
        Callback callback) {
        TextInputCallback textInputCallback= null;
        if (callback != null) {
            if (callback instanceof TextInputCallback) {
                textInputCallback = (TextInputCallback)callback;
            }
        }
        
        if (textInputCallback == null) {
            String prompt = getPrompt(childNode);
            String defaultValue=getDefaultValue(childNode);
            if (defaultValue == null) {
                textInputCallback = new TextInputCallback(prompt);
            } else {
                textInputCallback = new TextInputCallback(prompt,defaultValue);
            }
        }
        String value = getValue(childNode);
        if (value != null) {
            textInputCallback.setText(value);
        }
        
        return textInputCallback;
    }
    
    static TextOutputCallback createTextOutputCallback(
        Node childNode,
        Callback callback) {
        TextOutputCallback textOutputCallback= null;
        if (callback != null) {
            if (callback instanceof TextOutputCallback) {
                textOutputCallback = (TextOutputCallback)callback;
            }
        }
        
        if (textOutputCallback == null) {
            String value = getValue(childNode);
            int messageType = 0;
            String msgType = XMLUtils.getNodeAttributeValue(childNode,
            AuthXMLTags.MESSAGE_TYPE);
            if (msgType.equals("information")) {
                messageType = TextOutputCallback.INFORMATION;
            } else if (msgType.equals("error")) {
                messageType = TextOutputCallback.ERROR;
            } else if (msgType.equals("warning")) {
                messageType = TextOutputCallback.WARNING;
            }
            textOutputCallback = new TextOutputCallback(messageType, value);
            
        }
        
        return textOutputCallback;
    }
    
    static PagePropertiesCallback createPagePropertiesCallback(
        Node childNode,
        Callback callback) {
        PagePropertiesCallback pagePropertiesCallback = null;
        if (callback != null) {
            if (callback instanceof PagePropertiesCallback) {
                pagePropertiesCallback = (PagePropertiesCallback)callback;
            }
        }
        
        if (pagePropertiesCallback == null) {
            boolean errState = false;
            String errStateAttr = XMLUtils.getNodeAttributeValue(childNode,
            AuthXMLTags.ERROR_STATE);
            if ((errStateAttr != null) && errStateAttr.equals("true")) {
                errState = true;
            }
            String moduleName = getValueOfChildNode(childNode, "ModuleName");
            String pageState = getValueOfChildNode(childNode, "PageState");
            String header = getValueOfChildNode(childNode, "HeaderValue");
            String image = getValueOfChildNode(childNode, "ImageName");
            int timeOut = Integer.parseInt(
                getValueOfChildNode(childNode, "PageTimeOutValue"));
            String template = getValueOfChildNode(childNode, "TemplateName");
            List<String> required = stringToList(getValueOfChildNode(childNode, "RequiredList"));
            List<String> attributes = stringToList(getValueOfChildNode(childNode, "AttributeList"));
            List<String> infoText = stringToList(getValueOfChildNode(childNode, "InfoTextList"));
            pagePropertiesCallback = new PagePropertiesCallback(moduleName,
            header, image,timeOut, template, errState,
            pageState);
            pagePropertiesCallback.setRequire(required);
            pagePropertiesCallback.setAttribute(attributes);
            pagePropertiesCallback.setInfoText(infoText);
        }
        return pagePropertiesCallback;
    }
    
    static X509CertificateCallback createX509CertificateCallback(
        Node childNode, Callback callback) {
        X509CertificateCallback certCallback = null;
        if (callback != null) {
            if (callback instanceof X509CertificateCallback) {
                certCallback = (X509CertificateCallback)callback;
            }
        }

        if (certCallback == null) {
            certCallback = new X509CertificateCallback(getPrompt(childNode));
        }

        boolean signReq = true;
        String strSignReq = XMLUtils.getNodeAttributeValue(childNode,
        AuthXMLTags.SIGN_REQUIRED);
        if (strSignReq.equals("false")) {
            signReq = false;
        }
        certCallback.setReqSignature(signReq);

        Node pNode =
            XMLUtils.getChildNode(childNode, AuthXMLTags.X509CERTIFICATE);
        if (pNode != null) {
            String certificate = XMLUtils.getValueOfValueNode(pNode);
            if (certificate != null) {
                /*
                 * use the base64 decoder from MimeUtility instead of
                 * writing our own
                 */
                byte certbytes [] = Base64.decode(certificate);
                ByteArrayInputStream carray = 
                    new ByteArrayInputStream(certbytes);

                try {
                    CertificateFactory cf =
                        CertificateFactory.getInstance("X.509");
                    X509Certificate userCert =
                        (X509Certificate) cf.generateCertificate(carray);
                    certCallback.setCertificate(userCert);
                } catch (CertificateException e) {
                    debug.error("createX509CertificateCallback : ", e);
                }
            }
        }

        return certCallback;
    }

    static RedirectCallback createRedirectCallback(Node childNode,
            Callback callback) {      

        RedirectCallback redirectCallback = null;

        if (callback != null) {
            if (callback instanceof RedirectCallback) {
                redirectCallback = (RedirectCallback) callback;
            }
        }

        if (redirectCallback == null) {
            String redirectURL = getRedirectURL(childNode);
            String redirectMethod = XMLUtils.getNodeAttributeValue(
                    childNode, AuthXMLTags.REDIRECT_METHOD);

            Map redirectData = getRedirectData(childNode);
            String statusParam = getRedirectStatusParam(childNode);
            String redirectBackUrlCookie = getRedirectBackUrlCookie(childNode);

            if (debug.messageEnabled()) {
                debug.message("Created Redirect Callback: redirectURL=" +
                        redirectURL +
                        " redirectMethod=" + redirectMethod + 
                        " redirectData=" + redirectData +
                        " statusParam=" + statusParam +
                        " redirectBackUrlCookie = " + redirectBackUrlCookie);
            }

            redirectCallback = new RedirectCallback(redirectURL,redirectData,
                        redirectMethod, statusParam, redirectBackUrlCookie);
        }

        String redirectStatus = getRedirectStatus(childNode);
        if (redirectStatus != null && redirectStatus.length() > 0) {
            redirectCallback.setStatus(redirectStatus);
        }
        return redirectCallback;
    }

    static HttpCallback createHttpCallback(Node childNode, Callback callback) {
        HttpCallback hc = null;
        if (callback != null && (callback instanceof HttpCallback)) {
            hc = (HttpCallback)callback;
        }

        if (hc == null) {
            String authRHeader = getValueOfChildNode(childNode,
                    AuthXMLTags.HTTP_HEADER);
            String negoHeader = getValueOfChildNode(childNode,
                    AuthXMLTags.HTTP_NEGO);
            String errorCode = getValueOfChildNode(childNode,
                    AuthXMLTags.HTTP_CODE);
            hc = new HttpCallback(authRHeader, negoHeader, errorCode);
        }

        String tokenValue = getValueOfChildNode(childNode,
                AuthXMLTags.HTTP_TOKEN);
        if (tokenValue != null && tokenValue.length() > 0) {
            hc.setAuthorization(tokenValue);
        }
        return hc;
    }

    protected static String getRedirectURL(Node node) {
        Node pNode = XMLUtils.getChildNode(node, AuthXMLTags.REDIRECT_URL);
        if (pNode != null) {
            return (XMLUtils.getValueOfValueNode(pNode));
        }
        return (null);
    }

    protected static Map getRedirectData(Node node) {
        Map nameValuePairs = new HashMap();
        Set redirectDataNodes =
            XMLUtils.getChildNodes(node, AuthXMLTags.REDIRECT_DATA);
        Iterator dataNodesIterator = redirectDataNodes.iterator();
        
        while (dataNodesIterator.hasNext()) {
            Node dataNode = (Node) dataNodesIterator.next();
            String dataName = getDataName(dataNode);
            String dataValue = getDataValue(dataNode);
            nameValuePairs.put(dataName, dataValue);
        }

        return nameValuePairs;
    }

    protected static String getDataName(Node node) {
        Node pNode = XMLUtils.getChildNode(node, AuthXMLTags.REDIRECT_NAME);
        if (pNode != null) {
            return (XMLUtils.getValueOfValueNode(pNode));
        }
        return (null);
    }

    protected static String getDataValue(Node node) {
        Node pNode = XMLUtils.getChildNode(node, AuthXMLTags.REDIRECT_VALUE);
        if (pNode != null) {
            return (XMLUtils.getValueOfValueNode(pNode));
        }
        return (null);
    }

    protected static String getRedirectStatus(Node node) {
        Node pNode = XMLUtils.getChildNode(node,
                AuthXMLTags.REDIRECT_STATUS);
        if (pNode != null) {
            return (XMLUtils.getValueOfValueNode(pNode));
        }
        return (null);
    }

    protected static String getRedirectStatusParam(Node node) {
        Node pNode = XMLUtils.getChildNode(node,
                AuthXMLTags.REDIRECT_STATUS_PARAM);
        if (pNode != null) {
            return (XMLUtils.getValueOfValueNode(pNode));
        }
        return (null);
    }

    protected static String getRedirectBackUrlCookie(Node node) {
        Node pNode = XMLUtils.getChildNode(node,
                AuthXMLTags.REDIRECT_BACK_URL_COOKIE);
        if (pNode != null) {
            return (XMLUtils.getValueOfValueNode(pNode));
        }
        return (null);
    }

    static String getNameCallbackXML(NameCallback nameCallback) {
        StringBuilder xmlString = new StringBuilder();
        xmlString.append(AuthXMLTags.NAME_CALLBACK_BEGIN)
            .append(AuthXMLTags.PROMPT_BEGIN)
            .append(XMLUtils.escapeSpecialCharacters(nameCallback.getPrompt()))
            .append(AuthXMLTags.PROMPT_END);
        
        String defaultName = nameCallback.getDefaultName();
        if (defaultName != null) {
            xmlString.append(AuthXMLTags.DEFAULT_VALUE_BEGIN)
                .append(AuthXMLTags.VALUE_BEGIN)
                .append(XMLUtils.escapeSpecialCharacters(defaultName))
                .append(AuthXMLTags.VALUE_END)
                .append(AuthXMLTags.DEFAULT_VALUE_END);
        }
        
        String name = nameCallback.getName();
        if (name != null) {
            xmlString.append(AuthXMLTags.VALUE_BEGIN)
                .append(XMLUtils.escapeSpecialCharacters(name))
                .append(AuthXMLTags.VALUE_END);
        }
        
        xmlString.append(AuthXMLTags.NAME_CALLBACK_END);
        return xmlString.toString();
    }
    
    static String getPasswordCallbackXML(PasswordCallback passwordCallback) {
        StringBuilder xmlString = new StringBuilder();
        xmlString.append(AuthXMLTags.PASSWORD_CALLBACK_BEGIN)
            .append(AuthXMLTags.SPACE)
            .append(AuthXMLTags.ECHO_PASSWORD)
            .append(AuthXMLTags.EQUAL)
            .append(AuthXMLTags.QUOTE);
        
        if ( passwordCallback.isEchoOn() ) {
            xmlString.append("true");
        } else {
            xmlString.append("false");
        }
        
        xmlString.append(AuthXMLTags.QUOTE)
            .append(AuthXMLTags.ELEMENT_END)
            .append(AuthXMLTags.PROMPT_BEGIN)
            .append(XMLUtils.escapeSpecialCharacters(
            passwordCallback.getPrompt()))
            .append(AuthXMLTags.PROMPT_END);
        
        if (passwordCallback.getPassword() != null) {
            xmlString.append(AuthXMLTags.VALUE_BEGIN)
                .append((XMLUtils.escapeSpecialCharacters(
            new String(passwordCallback.getPassword()))).toCharArray())
                .append(AuthXMLTags.VALUE_END);
        }
        
        xmlString.append(AuthXMLTags.PASSWORD_CALLBACK_END);
        return xmlString.toString();
    }
    
    static String getChoiceCallbackXML(ChoiceCallback choiceCallback) {
        StringBuilder xmlString = new StringBuilder();
        
        xmlString.append(AuthXMLTags.CHOICE_CALLBACK_BEGIN)
            .append(AuthXMLTags.SPACE)
            .append(AuthXMLTags.MULTI_SELECT_ALLOWED)
            .append(AuthXMLTags.EQUAL)
            .append(AuthXMLTags.QUOTE);
        
        if (choiceCallback.allowMultipleSelections() ) {
            xmlString.append("true");
        } else {
            xmlString.append("false");
        }
        
        xmlString.append(AuthXMLTags.QUOTE)
            .append(AuthXMLTags.ELEMENT_END)
            .append(AuthXMLTags.PROMPT_BEGIN)
            .append(XMLUtils.escapeSpecialCharacters(
            choiceCallback.getPrompt()))
            .append(AuthXMLTags.PROMPT_END);
        
        String[] choices = choiceCallback.getChoices();
        int checked = choiceCallback.getDefaultChoice();
        if (choices != null ) {
            xmlString.append(AuthXMLTags.CHOICE_VALUES_BEGIN);
            for (int j=0; j<choices.length; j++) {
                xmlString.append(AuthXMLTags.CHOICE_VALUE_BEGIN);
                if ( j == checked ) {
                    xmlString.append(AuthXMLTags.SPACE)
                        .append(AuthXMLTags.IS_DEFAULT)
                        .append(AuthXMLTags.EQUAL)
                        .append(AuthXMLTags.QUOTE)
                        .append("yes")
                        .append(AuthXMLTags.QUOTE);
                }
                xmlString.append(AuthXMLTags.ELEMENT_END)
                    .append(AuthXMLTags.VALUE_BEGIN)
                    .append(XMLUtils.escapeSpecialCharacters(choices[j]))
                    .append(AuthXMLTags.VALUE_END)
                    .append(AuthXMLTags.CHOICE_VALUE_END);
            }
            xmlString.append(AuthXMLTags.CHOICE_VALUES_END);
        }
        
        int[] selectIndexes = choiceCallback.getSelectedIndexes();
        if ( selectIndexes != null ) {
            xmlString.append(AuthXMLTags.SELECTED_VALUES_BEGIN);
            for (int j=0; j<selectIndexes.length; j++) {
                xmlString.append(AuthXMLTags.VALUE_BEGIN)
                    .append(Integer.toString(selectIndexes[j]))
                    .append(AuthXMLTags.VALUE_END);
            }
            xmlString.append(AuthXMLTags.SELECTED_VALUES_END);
        }
        
        xmlString.append(AuthXMLTags.CHOICE_CALLBACK_END);
        return xmlString.toString();
    }
    
    static String getConfirmationCallbackXML(ConfirmationCallback conCallback) {
        StringBuilder xmlString = new StringBuilder();
        
        xmlString.append(AuthXMLTags.CONFIRMATION_CALLBACK_BEGIN)
            .append(AuthXMLTags.SPACE)
            .append(AuthXMLTags.MESSAGE_TYPE)
            .append(AuthXMLTags.EQUAL)
            .append(AuthXMLTags.QUOTE);
        
        switch(conCallback.getMessageType()) {
            case ConfirmationCallback.INFORMATION:
                xmlString.append("information");
                break;
            case ConfirmationCallback.ERROR:
                xmlString.append("error");
                break;
            case ConfirmationCallback.WARNING:
                xmlString.append("warning");
        }
        xmlString.append(AuthXMLTags.QUOTE);
        
        xmlString.append(AuthXMLTags.SPACE)
            .append(AuthXMLTags.OPTION_TYPE)
            .append(AuthXMLTags.EQUAL)
            .append(AuthXMLTags.QUOTE);
        
        switch(conCallback.getOptionType()) {
            case ConfirmationCallback.YES_NO_OPTION:
                xmlString.append("yes_no");
                break;
            case ConfirmationCallback.YES_NO_CANCEL_OPTION:
                xmlString.append("yes_no_cancel");
                break;
            case ConfirmationCallback.OK_CANCEL_OPTION:
                xmlString.append("ok_cancel");
                break;
            case ConfirmationCallback.UNSPECIFIED_OPTION:
                xmlString.append("unspecified");
        }
        xmlString.append(AuthXMLTags.QUOTE)
            .append(AuthXMLTags.ELEMENT_END);
        
        if (conCallback.getPrompt() != null ) {
            xmlString.append(AuthXMLTags.PROMPT_BEGIN)
                .append(XMLUtils.escapeSpecialCharacters(
                conCallback.getPrompt()))
                .append(AuthXMLTags.PROMPT_END);
        }
        
        String[] options = conCallback.getOptions();
        if ( options != null ) {
            xmlString.append(AuthXMLTags.OPTION_VALUES_BEGIN);
            for (int j=0; j<options.length; j++) {
                xmlString.append(AuthXMLTags.OPTION_VALUE_BEGIN)
                    .append(AuthXMLTags.VALUE_BEGIN)
                    .append(XMLUtils.escapeSpecialCharacters(options[j]))
                    .append(AuthXMLTags.VALUE_END)
                    .append(AuthXMLTags.OPTION_VALUE_END);
            }
            xmlString.append(AuthXMLTags.OPTION_VALUES_END);
        }
        
        int defaultOption = conCallback.getDefaultOption();
        xmlString.append(AuthXMLTags.DEFAULT_OPTION_VALUE_BEGIN)
            .append(AuthXMLTags.VALUE_BEGIN)
            .append(Integer.toString(defaultOption))
            .append(AuthXMLTags.VALUE_END)
            .append(AuthXMLTags.DEFAULT_OPTION_VALUE_END);
        
        int selectedValue = conCallback.getSelectedIndex();
        xmlString.append(AuthXMLTags.SELECTED_VALUE_BEGIN)
            .append(AuthXMLTags.VALUE_BEGIN)
            .append(Integer.toString(selectedValue))
            .append(AuthXMLTags.VALUE_END)
            .append(AuthXMLTags.SELECTED_VALUE_END);
        
        xmlString.append(AuthXMLTags.CONFIRMATION_CALLBACK_END);
        return xmlString.toString();
    }
    
    static String getTextInputCallbackXML(TextInputCallback textInputCallback) {
        StringBuilder xmlString  = new StringBuilder();
        
        xmlString.append(AuthXMLTags.TEXTINPUT_CALLBACK_BEGIN)
            .append(AuthXMLTags.PROMPT_BEGIN)
            .append(XMLUtils.escapeSpecialCharacters(
            textInputCallback.getPrompt()))
            .append(AuthXMLTags.PROMPT_END);
        
        String defaultText = textInputCallback.getDefaultText();
        if (defaultText != null) {
            xmlString.append(AuthXMLTags.DEFAULT_VALUE_BEGIN)
                .append(AuthXMLTags.VALUE_BEGIN)
                .append(XMLUtils.escapeSpecialCharacters(defaultText))
                .append(AuthXMLTags.VALUE_END)
                .append(AuthXMLTags.DEFAULT_VALUE_END);
        }
        
        String setText = textInputCallback.getText();
        if (setText != null) {
            xmlString.append(AuthXMLTags.VALUE_BEGIN)
                .append(XMLUtils.escapeSpecialCharacters(setText))
                .append(AuthXMLTags.VALUE_END);
        }
        
        xmlString.append(AuthXMLTags.TEXTINPUT_CALLBACK_END);
        return xmlString.toString();
    }
    
    static String getTextOutputCallbackXML(
        TextOutputCallback textOutputCallback) {
        StringBuilder xmlString = new StringBuilder();
        
        xmlString.append(AuthXMLTags.TEXTOUTPUT_CALLBACK_BEGIN)
            .append(AuthXMLTags.SPACE)
            .append(AuthXMLTags.MESSAGE_TYPE)
            .append(AuthXMLTags.EQUAL)
            .append(AuthXMLTags.QUOTE);
        
        switch(textOutputCallback.getMessageType()) {
            case TextOutputCallback.INFORMATION:
                xmlString.append("information");
                break;
            case TextOutputCallback.ERROR:
                xmlString.append("error");
                break;
            case TextOutputCallback.WARNING:
                xmlString.append("warning");
        }
        xmlString.append(AuthXMLTags.QUOTE)
            .append(AuthXMLTags.ELEMENT_END)
            .append(AuthXMLTags.VALUE_BEGIN)
            .append(textOutputCallback.getMessage())
            .append(AuthXMLTags.VALUE_END);
        
        xmlString.append(AuthXMLTags.TEXTOUTPUT_CALLBACK_END);
        return xmlString.toString();
    }
    
    static String getPagePropertiesCallbackXML(
        PagePropertiesCallback pagePCallback) {
        StringBuilder xmlString = new StringBuilder();
        
        xmlString.append(AuthXMLTags.PAGEP_CALLBACK_BEGIN)
            .append(AuthXMLTags.SPACE)
            .append(AuthXMLTags.ERROR_STATE)
            .append(AuthXMLTags.EQUAL)
            .append(AuthXMLTags.QUOTE);
        
        if ( pagePCallback.getErrorState() ) {
            xmlString.append("true");
        } else {
            xmlString.append("false");
        }
        
        xmlString.append(AuthXMLTags.QUOTE)
            .append(AuthXMLTags.ELEMENT_END)
            .append(AuthXMLTags.MODULE_NAME_BEGIN)
            .append(pagePCallback.getModuleName())
            .append(AuthXMLTags.MODULE_NAME_END)
            .append(AuthXMLTags.HEADER_VALUE_BEGIN)
            .append(XMLUtils.escapeSpecialCharacters(pagePCallback.getHeader()))
            .append(AuthXMLTags.HEADER_VALUE_END)
            .append(AuthXMLTags.IMAGE_NAME_BEGIN)
            .append(pagePCallback.getImage())
            .append(AuthXMLTags.IMAGE_NAME_END)
            .append(AuthXMLTags.PAGE_TIMEOUT_BEGIN)
            .append(Integer.toString(pagePCallback.getTimeOutValue()))
            .append(AuthXMLTags.PAGE_TIMEOUT_END)
            .append(AuthXMLTags.TEMPLATE_NAME_BEGIN)
            .append(pagePCallback.getTemplateName())
            .append(AuthXMLTags.TEMPLATE_NAME_END)
            .append(AuthXMLTags.PAGE_STATE_BEGIN)
            .append(pagePCallback.getPageState())
            .append(AuthXMLTags.PAGE_STATE_END)
            .append(AuthXMLTags.ATTRIBUTE_LIST_BEGIN)
            .append(listToString(pagePCallback.getAttribute()))
            .append(AuthXMLTags.ATTRIBUTE_LIST_END)
            .append(AuthXMLTags.REQUIRED_LIST_BEGIN)
            .append(listToString(pagePCallback.getRequire()))
            .append(AuthXMLTags.REQUIRED_LIST_END)
            .append(AuthXMLTags.INFOTEXT_LIST_BEGIN)
            .append(listToString(pagePCallback.getInfoText()))
            .append(AuthXMLTags.INFOTEXT_LIST_END);
        
        xmlString.append(AuthXMLTags.PAGEP_CALLBACK_END);
        return xmlString.toString();
    }    
    
    static String listToString(List<String> list) {
        StringBuilder buffer = new StringBuilder();
        Iterator<String> it = list.iterator();
        
        while (it.hasNext()) {
            buffer.append(it.next());
            
            if (it.hasNext()) {
                buffer.append(",");
            }
        }
                
        return buffer.toString();
    }
    
    static List<String> stringToList(String text) {
        int from = 0;
        int idx;
        List<String> ret = new ArrayList<String>();
 
        while ((idx = text.indexOf(',', from)) != -1) {
            ret.add(text.substring(from, idx));
            from = idx + 1;
        }
        
        ret.add(text.substring(from));
        return ret;
    }

    static String getX509CertificateCallbackXML(
        X509CertificateCallback certCallback) {
        StringBuilder xmlString = new StringBuilder();

        xmlString.append(AuthXMLTags.CERT_CALLBACK_BEGIN)
        .append(AuthXMLTags.SPACE)
        .append(AuthXMLTags.SIGN_REQUIRED)
        .append(AuthXMLTags.EQUAL)
        .append(AuthXMLTags.QUOTE)
        .append(certCallback.getReqSignature())
        .append(AuthXMLTags.QUOTE)
        .append(AuthXMLTags.ELEMENT_END);

        xmlString.append(AuthXMLTags.PROMPT_BEGIN)
        .append(XMLUtils.escapeSpecialCharacters(certCallback.getPrompt()))
        .append(AuthXMLTags.PROMPT_END);

        X509Certificate cert = certCallback.getCertificate();
        if (cert != null) {
            try {
                xmlString.append(AuthXMLTags.X509CERTIFICATE_BEGIN)
                .append(Base64.encode(cert.getEncoded()))
                .append(AuthXMLTags.X509CERTIFICATE_END);
            } catch (CertificateEncodingException e) {
                debug.error("getX509CertificateCallbackXML : ", e);
            }
        }

        xmlString.append(AuthXMLTags.CERT_CALLBACK_END);

        return xmlString.toString();
    }

    static String getHttpCallbackXML(HttpCallback hc) {
        StringBuilder xmlString = new StringBuilder();
        xmlString.append(AuthXMLTags.HTTP_CALLBACK_BEGIN).
                append(AuthXMLTags.HTTP_HEADER_BEGIN).
                append(hc.getAuthorizationHeader()).
                append(AuthXMLTags.HTTP_HEADER_END). 
                append(AuthXMLTags.HTTP_NEGO_BEGIN). 
                append(hc.getNegotiationHeaderName()).
                append(":").
                append(hc.getNegotiationHeaderValue()).
                append(AuthXMLTags.HTTP_NEGO_END). 
                append(AuthXMLTags.HTTP_CODE_BEGIN). 
                append(hc.getNegotiationCode()).
                append(AuthXMLTags.HTTP_CODE_END);

        String tokenValue = hc.getAuthorization();
        if (tokenValue != null && tokenValue.length() > 0) {
            xmlString.append(AuthXMLTags.HTTP_TOKEN_BEGIN).
                    append(tokenValue).
                    append(AuthXMLTags.HTTP_TOKEN_END);
        }
        xmlString.append(AuthXMLTags.HTTP_CALLBACK_END);
        return xmlString.toString();
    }

    static String getRedirectCallbackXML(
        RedirectCallback redirectCallback) {
        StringBuilder xmlString = new StringBuilder();
        String redirectMethod = redirectCallback.getMethod();

        // <RedirectCallback>
        xmlString.append(AuthXMLTags.REDIRECT_CALLBACK_BEGIN);

        if (redirectMethod != null) {
            xmlString.append(AuthXMLTags.SPACE)
                    .append(AuthXMLTags.REDIRECT_METHOD)
                    .append(AuthXMLTags.EQUAL)
                    .append(AuthXMLTags.QUOTE)
                    .append(redirectMethod)
                    .append(AuthXMLTags.QUOTE)
                    .append(AuthXMLTags.ELEMENT_END);
        } else {
            xmlString.append(AuthXMLTags.ELEMENT_END);
        }

        // <RedirectUrl>
        String redirectUrl = (redirectCallback.getRedirectUrl() != null) ?
                redirectCallback.getRedirectUrl() : "";

        xmlString.append(AuthXMLTags.REDIRECT_URL_BEGIN)
                .append(XMLUtils.escapeSpecialCharacters(redirectUrl))
                .append(AuthXMLTags.REDIRECT_URL_END);

        // <RedirectData>
        Map redirectData = redirectCallback.getRedirectData();
        if (redirectData != null) {
            Iterator nameSet = redirectData.keySet().iterator();      
            while (nameSet.hasNext()) {
                String name = (String) nameSet.next();
                String value = (String) redirectData.get(name);
                name = (name != null) ? name : "";
                value = (value != null) ? value : "";
                xmlString.append(AuthXMLTags.REDIRECT_DATA_BEGIN)
                    .append(AuthXMLTags.REDIRECT_NAME_BEGIN)
                    .append(name)
                    .append(AuthXMLTags.REDIRECT_NAME_END)
                    .append(AuthXMLTags.REDIRECT_VALUE_BEGIN)
                    .append(value)
                    .append(AuthXMLTags.REDIRECT_VALUE_END)
                    .append(AuthXMLTags.REDIRECT_DATA_END);
            }
        }

        // <RedirectStatusParam>
        String redirectParam = redirectCallback.getStatusParameter();

        if (redirectParam != null) {
            xmlString.append(AuthXMLTags.REDIRECT_STATUS_PARAM_BEGIN)
                    .append(redirectParam)
                    .append(AuthXMLTags.REDIRECT_STATUS_PARAM_END);
        }

        // <RedirectBackUrlCookie>
        String redirectBackUrlCookie = redirectCallback.getRedirectBackUrlCookieName();
        if (redirectBackUrlCookie != null) {
            xmlString.append(AuthXMLTags.REDIRECT_BACK_URL_COOKIE_BEGIN)
                    .append(redirectBackUrlCookie)
                    .append(AuthXMLTags.REDIRECT_BACK_URL_COOKIE_END);
        }

        // <Status>
        String redirectStatus = redirectCallback.getStatus();
        if (redirectStatus != null && redirectStatus.length() > 0) {
            xmlString.append(AuthXMLTags.REDIRECT_STATUS_BEGIN)
                    .append(redirectStatus)
                    .append(AuthXMLTags.REDIRECT_STATUS_END);
        }

        xmlString.append(AuthXMLTags.REDIRECT_CALLBACK_END);
        if (debug.messageEnabled()) {
            debug.message("created callback " + xmlString.toString());
        }

        return xmlString.toString();
    }

    protected static String getPrompt(Node node) {
        Node pNode = XMLUtils.getChildNode(node, AuthXMLTags.PROMPT);
        if (pNode != null) {
            return (XMLUtils.getValueOfValueNode(pNode));
        }
        return (null);
    }
    
    protected static String getValue(Node node) {
        Node pNode = XMLUtils.getChildNode(node, AuthXMLTags.VALUE);
        if (pNode != null) {
            return (XMLUtils.getValueOfValueNode(pNode));
        }
        return (null);
    }

    protected static String getValueNoTrim(Node node) {
        Node pNode = XMLUtils.getChildNode(node, AuthXMLTags.VALUE);
        if (pNode != null) {
            return (XMLUtils.getValueOfValueNodeNoTrim(pNode));
        }
        return (null);
    }

    protected static String getValueOfChildNode(Node node, String childNode) {
        Node pNode = XMLUtils.getChildNode(node, childNode);
        if (pNode != null) {
            return (XMLUtils.getValueOfValueNode(pNode));
        }
        return (null);
    }

 
    static DSAMECallbackInterface createCustomCallback(
        Node childNode,
        Callback customCallback) {
        debug.message("in create custom callback");
        String className = XMLUtils.getNodeAttributeValue(
            childNode,AuthXMLTags.ATTRIBUTE_CLASS_NAME);
        try {
            DSAMECallbackInterface callback = null;
            if (customCallback != null) {
                if (customCallback instanceof DSAMECallbackInterface) {
                    callback = (DSAMECallbackInterface)customCallback;
                    if (callback != null) {
                        String clName = callback.getClass().getName();
                        if (debug.messageEnabled()) {
                            debug.message("Class Name is : " + clName);
                        }
                        if ((clName == null) ||  (!clName.equals(className))) {
                            callback = null;
                        }
                    }
                }
            }
            
            if (callback == null) {
                if ((className != null) && (className.length() != 0)) {
                    Class xmlClass = Class.forName(className);
                    callback = (DSAMECallbackInterface) xmlClass.newInstance();
                }
            }
            
            HashMap map = new HashMap();
            String value = null;
            String attributeName  = null;
            HashSet valueSet = null;
            // get the values
            NodeList childNodesList = childNode.getChildNodes();
            for (int i = 0; i < childNodesList.getLength(); i++) {
                Node cNode = childNodesList.item(i);
                NodeList childNodeIList = cNode.getChildNodes();
                for (int j=0;j < childNodeIList.getLength(); j++) {
                    Node childNodeI = childNodeIList.item(j);
                    String localName = childNodeI.getLocalName();
                    if (debug.messageEnabled()) {
                        debug.message("child node local name : "
                        + localName);
                    }
                    if (localName.equals(AuthXMLTags.ATTRIBUTE)) {
                        attributeName =
                        XMLUtils.getNodeAttributeValue(
                            childNodeI,AuthXMLTags.ATTRIBUTE_NAME);
                    }
                    if (childNodeI.getLocalName().equals(AuthXMLTags.VALUE)) {
                        valueSet = new HashSet();
                        value = XMLUtils.getValueOfValueNode(childNodeI);
                        valueSet.add(value);
                    }
                }
                map.put(attributeName,valueSet);
            }
            
            if (debug.messageEnabled()) {
                debug.message("MAP is : " + map);
            }
            callback.setConfig(map);
            return callback;
        } catch (Exception e) {
            debug.message("Error creating callback " , e);
            return null;
        }
        
    }
    
    
    static String getCustomCallbackXML(DSAMECallbackInterface dsameCallback) {
        try {
            StringBuilder xmlString  = new StringBuilder();
            String className = dsameCallback.getClass().getName();
            
            if (debug.messageEnabled()) {
                debug.message("Custom Callback Class name : " + className);
            }
            
            xmlString.append(AuthXMLTags.CUSTOM_CALLBACK_BEGIN)
                .append(AuthXMLTags.SPACE)
                .append(AuthXMLTags.ATTRIBUTE_CLASS_NAME)
                .append(AuthXMLTags.EQUAL)
                .append(AuthXMLTags.QUOTE)
                .append(className)
                .append(AuthXMLTags.QUOTE)
                .append(AuthXMLTags.ELEMENT_END);
            
            // get the map from the custom callabck and loop trhu to
            // to add attribute and value
            
            Map map = dsameCallback.getConfig();
            if (debug.messageEnabled()) {
                debug.message("map is : " + map);
            }
            Set keysSet = map.keySet();
            
            Iterator keys = keysSet.iterator();
            if (debug.messageEnabled()) {
                debug.message("keyset is : " + keysSet);
            }
            while (keys.hasNext()) {
                xmlString.append(AuthXMLTags.ATTRIBUTE_VALUE_PAIR_BEGIN);
                String keyName =  (String) keys.next();
                if (debug.messageEnabled()) {
                    debug.message("KEY IS : " + keyName);
                }
                xmlString.append(AuthXMLTags.ATTRIBUTE_BEGIN)
                    .append(AuthXMLTags.SPACE)
                    .append(AuthXMLTags.ATTRIBUTE_NAME)
                    .append(AuthXMLTags.EQUAL)
                    .append(AuthXMLTags.QUOTE)
                    .append(keyName)
                    .append(AuthXMLTags.QUOTE)
                    .append(AuthXMLTags.ELEMENT_END)
                    .append(AuthXMLTags.ATTRIBUTE_END);
                Set valueSet = (Set) map.get(keyName);
                if (debug.messageEnabled()) {
                    debug.message("Value Set : " + valueSet);
                }
                Iterator vIterator = valueSet.iterator();
                while (vIterator.hasNext()) {
                    String value = (String) vIterator.next();
                    xmlString.append(AuthXMLTags.VALUE_BEGIN)
                        .append(value)
                        .append(AuthXMLTags.VALUE_END);
                }
                xmlString.append(AuthXMLTags.ATTRIBUTE_VALUE_PAIR_END);
            }
            
            xmlString.append(AuthXMLTags.CUSTOM_CALLBACK_END);
            return xmlString.toString();
            
        } catch (Exception e) {
            debug.message("Error creating customCallback :" ,e);
            return null;
        }
    }
    
    /**
     * Serialize the subject.
     *
     * @param subject Subject to be serialized.
     * @return serialized subject.
     */
    public static String getSerializedSubject(Subject subject) {
        byte[] sSerialized = null ;
        
        String encodedString = null;
        ByteArrayOutputStream byteOut;
        ObjectOutputStream objOutStream ;
        try{
            byteOut = new ByteArrayOutputStream();
            objOutStream = new ObjectOutputStream(byteOut);
            
            //convert object to byte using streams
            objOutStream.writeObject(subject);
            
            sSerialized = byteOut.toByteArray();
            
            // base 64 encoding & encrypt
            encodedString =  (String) AccessController.doPrivileged(
            new EncodeAction(Base64.encode(sSerialized).trim()));
            
            if (debug.messageEnabled()) {
                debug.message("encoded Subject is : " + encodedString);
            }
        }catch (Exception e){
            debug.message("Exception  : " , e);
        }
        return encodedString;
    }
    
    /**
     * Deserializes Subject.
     *
     * @param subjectSerialized Serialized Subject.
     * @throws Exception
     */
    public static Subject getDeSerializedSubject(String subjectSerialized)
        throws Exception {
        
        // decrypt and then decode
        String decStr = (String) AccessController.doPrivileged(
        new DecodeAction(subjectSerialized));
        byte[] sSerialized = Base64.decode(decStr);
        
        if (sSerialized == null) return null;
        byte byteDecrypted[];
        ByteArrayInputStream byteIn;
        ObjectInputStream objInStream = null;
        Object tempObject = null;
        try{
            byteDecrypted = sSerialized;
            //convert byte to object using streams
            byteIn = new ByteArrayInputStream(byteDecrypted);
            objInStream  = new ObjectInputStream(byteIn);
            tempObject = objInStream.readObject();
        }catch(Exception e){
            debug.message("Exception Message in decrypt: " , e);
        }
        if (tempObject == null) return null;
        
        Subject subjectObj = (Subject) tempObject;
        
        if (debug.messageEnabled()) {
            debug.message("returning temp" + subjectObj);
        }
        return subjectObj;
    }

    /**
     * Serialize an object to a string
     *
     * @param object The object to be serialized
     * @return Base64 encoded string representation of the object
     * @throws IOException If the object is not serializable
     */
    public static String serializeToString(Object object)
    throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(object);
        oos.close();

        return Base64.encode(baos.toByteArray());
    }

    /**
     * Deserialize a string back into the original object
     *
     * @param encObj The Base64 encoded string representation of the object
     * @return The deserialized object
     * @throws IOException If the object cannot be deserialized
     * @throws ClassNotFoundException If the class representing the object
     * cannot be found
     */
    public static Object deserializeToObject(String encObj)
    throws IOException, ClassNotFoundException {
        Object obj = null;

        if (encObj != null && encObj.length() > 0) {
            ByteArrayInputStream bais = new ByteArrayInputStream(Base64.decode(encObj));
            ObjectInputStream oos = new ObjectInputStream(bais);
            obj = oos.readObject();
            oos.close();
        }

        return obj;
    }
    
    static int getNameCallbackIndex(Callback[] callbacks,int startIndex) {
        int i=0;
        for (i = startIndex;i < callbacks.length;i++) {
            if (callbacks[i] instanceof NameCallback) {
                return i;
            }
        }
        return -1;
    }
    
    static int getPasswordCallbackIndex(Callback[] callbacks,int startIndex) {
        int i=0;
        for (i = startIndex;i < callbacks.length;i++) {
            if (callbacks[i] instanceof PasswordCallback) {
                return i;
            }
        }
        return -1;
    }
    
    static int getConfCallbackIndex(Callback[] callbacks,int startIndex) {
        int i=0;
        for (i = startIndex;i < callbacks.length;i++) {
            if (callbacks[i] instanceof ConfirmationCallback) {
                return i;
            }
        }
        return -1;
    }
    
    static int getChoiceCallbackIndex(Callback[] callbacks,int startIndex) {
        int i=0;
        for (i = startIndex;i < callbacks.length;i++) {
            if (callbacks[i] instanceof ChoiceCallback) {
                return i;
            }
        }
        return -1;
    }
    
    static int getTextInputIndex(Callback[] callbacks,int startIndex) {
        int i=0;
        for (i = startIndex;i < callbacks.length;i++) {
            if (callbacks[i] instanceof TextInputCallback) {
                return i;
            }
        }
        return -1;
    }
    
    static int getTextOutputIndex(Callback[] callbacks,int startIndex) {
        int i=0;
        for (i = startIndex;i < callbacks.length;i++) {
            if (callbacks[i] instanceof TextOutputCallback) {
                return i;
            }
        }
        return -1;
    }
    
    static int  getPagePropertiesIndex(Callback[] callbacks,int startIndex) {
        int i=0;
        for (i = startIndex;i < callbacks.length;i++) {
            if (callbacks[i] instanceof PagePropertiesCallback) {
                return i;
            }
        }
        return -1;
    }
    
    static int getCustomCallbackIndex(Callback[] callbacks, int startIndex) {
        int i=0;
        for (i = startIndex;i < callbacks.length;i++) {
            if (callbacks[i] instanceof DSAMECallbackInterface) {
                return i;
            }
        }
        return -1;
    }
    
    static int getLanguageCallbackIndex(Callback[] callbacks, int startIndex) {
        int i=0;
        for (i = startIndex;i < callbacks.length;i++) {
            if (callbacks[i] instanceof LanguageCallback) {
                return i;
            }
        }
        return -1;
    }    

    static int getX509CertificateCallbackIndex(
        Callback[] callbacks, int startIndex) {
        int i=0;
        for (i = startIndex;i < callbacks.length;i++) {
            if (callbacks[i] instanceof X509CertificateCallback) {
                return i;
            }
        }
        return -1;
    }

    static int getHttpCallbackIndex(Callback[] callbacks, int startIndex) {
        int i=0;
        for (i = startIndex; i < callbacks.length; i++) {
            if (callbacks[i] instanceof HttpCallback) {
                return i;
            }
        }
        return -1;
    }
    
    static int getRedirectCallbackIndex(Callback[] callbacks,int startIndex) {
        int i = 0;
        for (i = startIndex; i < callbacks.length; i++) {
            if (callbacks[i] instanceof RedirectCallback) {
                return i;
            }
        }
        return -1;
    }

    static LanguageCallback createLanguageCallback(
        Node childNode,
        Callback callback) {
        LanguageCallback languageCallback = null;
        if (callback != null) {
            if (callback instanceof LanguageCallback) {
                languageCallback= (LanguageCallback) callback;
            }
        }
        
        if (languageCallback == null) {
            languageCallback= new LanguageCallback();
        }
        
        Node localeNode= XMLUtils.getChildNode(childNode,"Locale");
        
        String language =
        XMLUtils.getNodeAttributeValue(localeNode, AuthXMLTags.ATTRIBUTE_LANG);
        String country = XMLUtils.getNodeAttributeValue(
            localeNode, AuthXMLTags.ATTRIBUTE_COUNTRY);
        String variant = XMLUtils.getNodeAttributeValue(
            localeNode, AuthXMLTags.ATTRIBUTE_VARIANT);
        
        if (debug.messageEnabled()) {
            debug.message("Language is " + language);
            debug.message("Country is " + country);
            debug.message("Variant is " + variant);
        }
        
        if ((language != null)  && (country !=null)) {
            java.util.Locale locale = null;
            if (variant != null) {
                locale =  new java.util.Locale(language,country,variant);
            } else {
                locale = new java.util.Locale(language,country);
            }
            languageCallback.setLocale(locale);
        }
        
        return languageCallback;
    }
    
    static String getLanguageCallbackXML(LanguageCallback languageCallback) {
        StringBuilder xmlString = new StringBuilder();
        xmlString.append(AuthXMLTags.LANGUAGE_CALLBACK_BEGIN)
            .append(AuthXMLTags.LOCALE_BEGIN);
        
        java.util.Locale locale = languageCallback.getLocale();
        
        if (locale != null) {
            String language = locale.getLanguage();
            if ((language != null) && (language.length() != 0)) {
                xmlString.append(AuthXMLTags.SPACE)
                    .append(AuthXMLTags.ATTRIBUTE_LANG)
                    .append(AuthXMLTags.EQUAL)
                    .append(AuthXMLTags.QUOTE)
                    .append(language)
                    .append(AuthXMLTags.QUOTE);
            }
            
            String country = locale.getCountry();
            if ((country != null) && (country.length() != 0)) {
                xmlString.append(AuthXMLTags.SPACE)
                    .append(AuthXMLTags.ATTRIBUTE_COUNTRY)
                    .append(AuthXMLTags.EQUAL)
                    .append(AuthXMLTags.QUOTE)
                    .append(country)
                    .append(AuthXMLTags.QUOTE);
            }
            
            String variant = locale.getVariant();
            if ((variant!= null) && (variant.length() != 0)) {
                xmlString.append(AuthXMLTags.SPACE)
                    .append(AuthXMLTags.ATTRIBUTE_VARIANT)
                    .append(AuthXMLTags.EQUAL)
                    .append(AuthXMLTags.QUOTE)
                    .append(variant)
                    .append(AuthXMLTags.QUOTE);
            }
            
        }
        xmlString.append(AuthXMLTags.ELEMENT_END);
        xmlString.append(AuthXMLTags.LOCALE_END);
        
        
        xmlString.append(AuthXMLTags.LANGUAGE_CALLBACK_END);
        
        if (debug.messageEnabled()) {
            debug.message("LANGUAGE CALLBACK xmlString : "
            + xmlString.toString());
        }
        
        return xmlString.toString();
    }
    
    /**
     * returns the value of DefaultValue Node
     */
    private static String getDefaultValue(Node childNode) {
        Node defaultNode = XMLUtils.getChildNode(childNode,
        AuthXMLTags.DEFAULT_VALUE);
        String defaultValue=null;
        if (defaultNode != null) {
            defaultValue = getValue(defaultNode);
        }
        
        return defaultValue;
    }
}
