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
 * $Id: AMModuleProperties.java,v 1.8 2009/05/02 22:11:28 kevinserwin Exp $
 *
 */

/*
 * Portions Copyrighted 2011 ForgeRock AS
 */

package com.sun.identity.authentication.spi;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ChoiceCallback;
import javax.security.auth.callback.ConfirmationCallback;
import javax.security.auth.callback.LanguageCallback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.TextInputCallback;
import javax.security.auth.callback.TextOutputCallback;
import javax.servlet.ServletContext;
import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.authentication.service.AuthD;
import com.sun.identity.authentication.share.AuthXMLTags;
import com.sun.identity.shared.xml.XMLUtils;

class AMModuleProperties {
    private String moduleName;
    private String order;
    private int p = 0; // callback of the specific pages
    private List page = new ArrayList();
    private List attribute; // Attribute for each page
    private List require; // isRequire for each page
    private List<String> infoText;
    private Hashtable rtable = new Hashtable();
    private Callback[] callbacks = null;
    private static Map moduleProps = new HashMap();
    private static final String amAuth = "amAuth";
    private static Debug debug = Debug.getInstance(amAuth);

    AMModuleProperties(
        String fileName,
        ServletContext servletContext
    ) throws AuthLoginException { 
        InputStream in = null;
        try {
            DocumentBuilder builder = XMLUtils.getSafeDocumentBuilder(false);
            if (servletContext != null) {
                in = servletContext.getResourceAsStream(fileName);
            }
            if (in == null) {
                in = Thread.currentThread().getContextClassLoader()
                    .getResourceAsStream(fileName.substring(1));
                // remove leading '/' from fileName
            }
            Document doc = builder.parse(in);
            in.close();
            walk(doc);
            for(int i=1; i<=rtable.size(); i++) {
                Callback[] cb = (Callback[]) rtable.get(Integer.toString(i));
                page.add(cb);
            }
        } catch(Exception e) {
            debug.error("AMModuleProperties, parse file :" +fileName);
            debug.error("AMModuleProperties, parser error :" , e);
            throw new AuthLoginException(amAuth, "getModulePropertiesError", 
                null);
        } finally {
            try {
                in.close();
            } catch (Exception ee) {
            }
        }
    }
   
    /**
     * Return the list of page
     * @return <code>List</code> of page
     */
    private List getCallbacks() {
        return page;
    } 

    /**
     * Return the list of authentication module property
     * @param fileName that has module properties
     * @return <code>List</code> of module property
     * @exception AuthLoginException if fails to get properties from the file.
     */
    public static List getModuleProperties(String fileName)
            throws AuthLoginException {
        List list = (List) moduleProps.get(fileName);
        List pageAttr;
        if (list != null) {
           return list;
        }

        ServletContext servletContext = 
                        AuthD.getAuth().getServletContext();
        InputStream resStream = null;
        try {
            if (servletContext != null) {
                resStream = servletContext.getResourceAsStream(fileName);
            };
            if (resStream == null) {
                resStream = Thread.currentThread().getContextClassLoader()
		  .getResourceAsStream(fileName.substring(1));
                // remove leading '/' from fileName
            } 
            // file might be empty for modules like Cert,Anonymous etc.
            if (resStream !=null && resStream.read() == -1) {
                if (debug.messageEnabled()) {
                    debug.message(fileName + " is empty");
                }
                list = new ArrayList();
                synchronized(moduleProps) {
                    moduleProps.put(fileName, list);
                }
                return list;
            }
        } catch (Exception e) {
            debug.message("getModuleProperties: Error: " ,e);
        } finally {
            try {
                resStream.close();
            } catch (Exception ee) {
                debug.message("Error closing input stream");
            }
        }

        AMModuleProperties prop = new AMModuleProperties(fileName,
                                            servletContext);
        list = prop.getCallbacks();
        if (list != null && !list.isEmpty()) {
           synchronized(moduleProps) {
                moduleProps.put(fileName, list);
           }
        }
        return list;
    }
    
    //walk the DOM tree and create the callback array
    private void walk(Node node)
    {
        int type = node.getNodeType();
        String nodeName = node.getNodeName();
        debug.message("Callback type: " + nodeName);
        String tmp;
        Node sub;
        switch(type)
        {
            case Node.ELEMENT_NODE:
            {
                if (nodeName.equals("ModuleProperties")) {
                    moduleName = getAttribute(node, "moduleName");
                } else if (nodeName.equals("Callbacks")) {
                    p = 0;
                    String timeout = getAttribute(node, "timeout");
                    String template = getAttribute(node, "template");
                    if(template == null){
                    	template = "";
                    }
                    String image = getAttribute(node, "image");
                    if(image == null){
                    	image = "";
                    }
                    String header = getAttribute(node, "header");
                    boolean error = Boolean.valueOf(
                        getAttribute(node, "error")).booleanValue();
                    int size = Integer.parseInt(getAttribute(node, "length"))
                        + 1;
                    int t=0;
                    if (timeout!=null) {
                        t = Integer.parseInt(timeout);
                    } 
                    order = getAttribute(node, "order");
                    
                    callbacks = new Callback[size];
                    callbacks[p] = new PagePropertiesCallback(moduleName, 
                        header, image, t, template, error, order);
                    p++;

                    attribute = new ArrayList();
                    require = new ArrayList();
                    infoText = new ArrayList<String>();
                } else if (nodeName.equals("NameCallback")) {
                        sub = node.getFirstChild();
                        sub = sub.getNextSibling();
                        String prompt = sub.getFirstChild().getNodeValue();

                        String dftName = null;
                        sub = sub.getNextSibling().getNextSibling();
                        if (sub != null) {
                            sub = sub.getFirstChild();
                            dftName = sub.getNodeValue();
                            callbacks[p] = new NameCallback(prompt, dftName);
                        } else {
                            callbacks[p] = new NameCallback(prompt);
                        }

                        tmp = getAttribute(node, "isRequired");
                        if (tmp != null) {
                            if (tmp.equals("true")) {
                                require.add("true");
                            } else {
                                require.add("");
                            }
                        } else {
                            require.add("");
                        }
                        tmp = getAttribute(node, "attribute");
                        if (tmp!=null) {
                            attribute.add(tmp);
                        } else {
                            attribute.add("");
                        }
                        tmp = getAttribute(node, "infoText");
                        if (tmp!=null) {
                            infoText.add(tmp);
                        } else {
                            infoText.add("");
                        }
                        p++;
                } else if (nodeName.equals("PasswordCallback")) {
                        String echo = getAttribute(node, "echoPassword");
                        sub = node.getFirstChild();
                        sub = sub.getNextSibling().getFirstChild();
                        String prompt = sub.getNodeValue();
                        boolean b = Boolean.valueOf(echo).booleanValue();
                        callbacks[p] = new PasswordCallback(prompt, b);
                        tmp = getAttribute(node, "isRequired");
                        if (tmp!=null) {
                            if (tmp.equals("true")) {
                                require.add("true");
                            } else {
                                require.add("");
                            }
                        } else {
                            require.add("");
                        }
                        tmp = getAttribute(node, "attribute");
                        if (tmp!=null) {
                            attribute.add(tmp);
                        } else {
                            attribute.add("");
                        }
                        tmp = getAttribute(node, "infoText");
                        if (tmp!=null) {
                            infoText.add(tmp);
                        } else {
                            infoText.add("");
                        }
                        p++;
                } else if (nodeName.equals("ChoiceCallback")) {
                        String multiple = getAttribute(
                            node, "multipleSelectionsAllowed");
                        String prompt=null;
                        String[] choices=null;
                        int defaultChoice=0;
                        boolean mch = Boolean.valueOf(multiple).booleanValue();

                        for (sub=node.getFirstChild(); sub!=null;
                            sub=sub.getNextSibling()
                        ) {
                            if (sub.getNodeName().equals("Prompt")) {
                                prompt = sub.getFirstChild().getNodeValue();
                            } else if (sub.getNodeName().equals("ChoiceValues")
                            ) {
                                int len = 0;
                                Node ss = sub.getFirstChild().getNextSibling();
                                for (Node count=ss; count!=null;
                                    count=count.getNextSibling()
                                        .getNextSibling()
                                ) {
                                    len++;
                                }
                                choices = new String[len];
                                for(int i=0; i<len; i++) {
                                    choices[i] = ss.getFirstChild().
                                        getNextSibling().getFirstChild().
                                            getNodeValue();
                                    if (Boolean.valueOf(getAttribute(ss,
                                        "isDefault")).booleanValue()
                                    ) {
                                        defaultChoice = i;
                                    }
                                    ss = ss.getNextSibling().getNextSibling();
                                }
                                break;
                            }
                        }
                        callbacks[p] = new ChoiceCallback(
                            prompt, choices, defaultChoice, mch);
                        tmp = getAttribute(node, "isRequired");
                        if (tmp!=null) {
                            if (tmp.equals("true")) {
                                require.add("true");
                            } else {
                            require.add("");
                            }
                        } else {
                            require.add("");
                        }
                        tmp = getAttribute(node, "attribute");
                        if (tmp!=null) {
                            attribute.add(tmp);
                        } else {
                            attribute.add("");
                        }
                        tmp = getAttribute(node, "infoText");
                        if (tmp!=null) {
                            infoText.add(tmp);
                        } else {
                            infoText.add("");
                        }
                        p++;
                } else if (nodeName.equals("ConfirmationCallback")) {
                        int messageType = ConfirmationCallback.INFORMATION;
                        int defaultOption = 0;
                        String[] options = null;

                        for (sub=node.getFirstChild(); sub!=null;
                            sub=sub.getNextSibling()
                        ) {
                            if (sub.getNodeName().equals("OptionValues")) {
                                int len = 0;
                                Node ss = sub.getFirstChild().getNextSibling();
                                for (Node count=ss; count!=null;
                                    count=count.getNextSibling()
                                        .getNextSibling()
                                ) {
                                    len++;
                                }
                                options = new String[len];
                                for(int i=0; i<len; i++) {
                                    options[i] = ss.getFirstChild()
                                        .getNextSibling().getFirstChild().
                                            getNodeValue();
                                    ss = ss.getNextSibling().getNextSibling();
                                }
                                break;
                            }
                        }
                        callbacks[p] = new ConfirmationCallback(
                            messageType, options, defaultOption);
                        p++;
                } else if (nodeName.equals("TextInputCallback")) {
                        sub = node.getFirstChild();
                        sub = sub.getNextSibling().getFirstChild();
                        String prompt = sub.getNodeValue();
                        callbacks[p] = new TextInputCallback(prompt);
                        p++;
                } else if (nodeName.equals("TextOutputCallback")) {
                        int messageType = TextOutputCallback.ERROR;
                        String s = getAttribute(node, "messageType");
                        if (s.equals("error")) {
                            messageType = TextOutputCallback.ERROR;
                        } else if (s.equals("information")) {
                            messageType = TextOutputCallback.INFORMATION;
                        } else if (s.equals("warning")) {
                            messageType = TextOutputCallback.WARNING;
                        }
                        sub = node.getFirstChild();
                        sub = sub.getNextSibling().getFirstChild();
                        String value = sub.getNodeValue();
                        callbacks[p] = new TextOutputCallback(
                            messageType, value);
                        p++;
                } else if (nodeName.equals("LanguageCallback")) {
                        for (sub=node.getFirstChild(); sub!=null;
                            sub=sub.getNextSibling()
                        ) {
                            if (sub.getNodeName().equals("ChoiceValue")) {
                                String isdefault = getAttribute(
                                    sub, "isDefault");
                            }
                        }
                        callbacks[p] = new LanguageCallback();
                        p++;
		} else if (nodeName.equals(AuthXMLTags.HTTP_CALLBACK)) {
			String header = null;
			String negotiation = null;
			String code = null;
			sub = node.getFirstChild();
			for (; sub!=null; sub=sub.getNextSibling()) {
			    String tmpStr = sub.getNodeName();
			    if (tmpStr.equals(AuthXMLTags.HTTP_HEADER)) {
				header = sub.getFirstChild().getNodeValue();
			    } else if (tmpStr.equals(AuthXMLTags.HTTP_NEGO)) {
				negotiation= sub.getFirstChild().getNodeValue();
			    } else if (tmpStr.equals(AuthXMLTags.HTTP_CODE)) {
				code = sub.getFirstChild().getNodeValue();
			    }
			}
			callbacks[p]= new HttpCallback(header,negotiation,code);
			p++;
		} else if (nodeName.equals(AuthXMLTags.REDIRECT_CALLBACK)) {
			String redirectUrl = null;
                        String statusParameter = null;
                        String redirectBackUrlCookie = null;
			Map redirectData = new HashMap();
			String method = 
                            getAttribute(node, AuthXMLTags.REDIRECT_METHOD);
			sub = node.getFirstChild();
			for (; sub!=null; sub=sub.getNextSibling()) {
			    String tmpStr = sub.getNodeName();                            
			    if (tmpStr.equals(AuthXMLTags.REDIRECT_URL)) {
				redirectUrl = 
                                    sub.getFirstChild().getNodeValue();                                
			    } else if (tmpStr.equals(
                                        AuthXMLTags.REDIRECT_STATUS_PARAM)) {
				statusParameter = 
                                    sub.getFirstChild().getNodeValue();                                
			    } else if (tmpStr.equals(
                                        AuthXMLTags.REDIRECT_BACK_URL_COOKIE)) {
				redirectBackUrlCookie = 
                                    sub.getFirstChild().getNodeValue();                                
			    } else if (tmpStr.equals(
                                        AuthXMLTags.REDIRECT_DATA)) {
                                String name = null;
                                String value = null;                                
				Node ss = sub.getFirstChild().getNextSibling();                         
                                String tmpStrName = ss.getNodeName();                                
                                if (tmpStrName.equals("Name")) {
                                    name = ss.getFirstChild().getNodeValue();                                    
                                }                                 
                                ss = ss.getNextSibling().getNextSibling();
                                String tmpStrValue = ss.getNodeName();                                
                                if (tmpStrValue.equals("Value")) {
                                    value = ss.getFirstChild().getNodeValue();                                    
                                }                                
                                redirectData.put(name,value);                                
			    }
			}
                        if (debug.messageEnabled()) {
                            debug.message("redirectUrl : " + redirectUrl);
                            debug.message("statusParameter : " 
                                + statusParameter);
                            debug.message("redirectBackUrlCookie : " 
                                + redirectBackUrlCookie);
                            debug.message("redirectData : " + redirectData);
                            debug.message("method : " + method);
                        }
			callbacks[p]= 
                            new RedirectCallback(redirectUrl,redirectData,
                                method);			
			p++;
		}

                break;
            }//end of element
            default:
                break;

        }//end of switch
        
              
        //recurse

        for (Node child = node.getFirstChild(); child != null;
            child = child.getNextSibling()
        ) {
            walk(child);
        }
       
        //without this the ending tags will miss
        if ( type == Node.ELEMENT_NODE )
        {
            if (nodeName.equals("Callbacks")) {
                ((PagePropertiesCallback) callbacks[0]).setAttribute(attribute);
                ((PagePropertiesCallback) callbacks[0]).setRequire(require);
                ((PagePropertiesCallback) callbacks[0]).setInfoText(infoText);
                rtable.put(order, callbacks);
                //attrtable.put(order, subAttr);
                //requiretable.put(order, subRequire);
            }
        }
        
    }//end of walk

    private String getAttribute(Node node, String name) {
        NamedNodeMap nnm = node.getAttributes();
        if(nnm != null ) {
            int len = nnm.getLength() ;
            Attr attr;
            for ( int i = 0; i < len; i++ ) {
                attr = (Attr)nnm.item(i);
                if (attr.getNodeName().equals(name)) {
                    return attr.getNodeValue();
                }
            }
        }
        return null;
    }
}
