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
 * $Id: RequestAuthnContext.java,v 1.2 2008/06/25 05:46:47 qcheng Exp $
 * Portions Copyrighted 2014 ForgeRock AS
 */

package com.sun.identity.federation.message.common;

import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.shared.encode.URLEncDec;
import com.sun.identity.shared.xml.XMLUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import javax.servlet.http.HttpServletRequest;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * This class <code>RequestAuthnContext</code> represents the requesting
 * Authentication Context as part of the <code>FSAuthnRequest</code>.
 *
 * @supported.all.api
 * @deprecated since 12.0.0
 */
@Deprecated
public class RequestAuthnContext {

    private List authnContextClassRefList = null;
    private List authnContextStatementRefList = null;
    private String authnContextComparison = null;
    private int minorVersion = 0;
    
    /**
     * Constructor to create <code>RequestAuthnContext</code> object.
     *
     * @param authnContextClassRefList 
     *        Ordered list of AuthnContext Classes Refs.
     * @param authnContextStatementRefList 
     *        Ordered list of AuthnContext Statement Refs.
     *        Note: authnContextClassRefList and authContextStatementRefList
     *        are mutually exclusive lists.
     * @param authnContextComparison AuthnContext Comparison Type.
     *        Possible values are  <code>exact</code>, <code>minimum<code>,
     *        <code>better</code> and <code>maximum</code>.
     */
    public RequestAuthnContext (
        List authnContextClassRefList,
        List authnContextStatementRefList,
        String authnContextComparison) {

        this.authnContextStatementRefList = authnContextStatementRefList;
        this.authnContextClassRefList = authnContextClassRefList;
        this.authnContextComparison = authnContextComparison;
    }

    /**
     * Default constructor.
     */
    public RequestAuthnContext(){}
    
    /**
     * Constructor to create <code>RequestAuthnContext</code> object from
     * Docuemnt Element.
     *
     * @param root the Document Element.
     * @throws FSMsgException on error.
     */
    public RequestAuthnContext(Element root) throws FSMsgException {
        if(root == null) {
            FSUtils.debug.message("AuthnContext.parseXML: null input.");
            throw new FSMsgException("nullInput",null);
        }
        
        String tag = root.getLocalName();
        
        if(tag == null) {
            FSUtils.debug.error("AuthnContext.parseXML: wrong input.");
            throw new FSMsgException("wrongInput",null);
        }
        
        if(!tag.equals("RequestAuthnContext") && !tag.equals("AuthnContext")) {
            FSUtils.debug.error("AuthnContext.parseXML: wrong input.");
            throw new FSMsgException("wrongInput",null);
        }
        
        NodeList nl = root.getChildNodes();
        int length = nl.getLength();
        
        for(int i = 0; i < length; i++) {
            Node child = nl.item(i);
            String childName = child.getLocalName();
            
            if(childName == null) {
                continue;
            }
            
            if(childName.equals("AuthnContextClassRef")) {
                if(authnContextStatementRefList != null) {
                    FSUtils.debug.error("AuthnContext(Element): Should"
                            + "contain either <AuthnContextStatementRef> or "
                            + "<AuthnContextClassRef>");
                    throw new FSMsgException("wrongInput",null);
                }
                
                if(authnContextClassRefList == null ||
                        authnContextClassRefList == Collections.EMPTY_LIST) {
                    authnContextClassRefList = new ArrayList();
                }
                
                authnContextClassRefList.add(
                        XMLUtils.getElementValue((Element) child));
                
            } else if (childName.equals("AuthnContextStatementRef")) {
                
                if(authnContextClassRefList != null) {
                    FSUtils.debug.error("AuthnContext(Element): Should"
                            + "contain either <AuthnContextStatementRef> or "
                            + "<AuthnContextClassRef>");
                    throw new FSMsgException("wrongInput",null);
                }
                
                if(authnContextStatementRefList == null ||
                       authnContextStatementRefList == Collections.EMPTY_LIST) {
                    authnContextStatementRefList = new ArrayList();
                }
                
                authnContextStatementRefList.add(
                        XMLUtils.getElementValue((Element) child));
                
            } else if(childName.equals("AuthnContextComparison")) {
                
                authnContextComparison = XMLUtils.getElementValue(
                        (Element)child);
            }
        }
    }
    
    /**
     * Returns <code>List</code> of <code>AuthnContext</code> Class References.
     *
     * @return <code>List</code> of <code>AuthnContext</code> Class Reference
     *         classes.
     * @see #setAuthnContextClassRefList(List)
     */
    public List getAuthnContextClassRefList() {
        return authnContextClassRefList;
    }
    
    /**
     * Sets a <code>List</code> of  <code>AuthnContext</code> Class References.
     *
     * @param authnContextClassRefList a <code>List</code> of  
     *        <code>AuthnContext</code> Class References.
     * @see #getAuthnContextClassRefList
     */
    public void setAuthnContextClassRefList(
        List authnContextClassRefList) {

        this.authnContextClassRefList = authnContextClassRefList;
    }
    
    /**
     * Returns a <code>List</code> of <code>AuthnContext</code> Statement
     * References.
     *
     * @return a <code>List</code> of <code>AuthnContext</code> Statement
     *         References.
     * @see #setAuthnContextStatementRefList(List)
     */
    public List getAuthnContextStatementRefList() {
        return this.authnContextStatementRefList;
    }

    /**
     * Sets a <code>List</code> of <code>AuthnContext</code> Statement 
     * References.
     *
     * @param authnContextStatementRefList a <code>List</code> of
     *        <code>AuthnContext</code> Statement References.
     * @see #getAuthnContextStatementRefList
     */
    public void setAuthnContextStatementRefList(
                    List authnContextStatementRefList ) {
        this.authnContextStatementRefList = authnContextStatementRefList;
    }

    /**
     * Returns the <code>AuthnContext</code> Comparison type.
     *
     * @return authnContextComparison the <code>AuthnContext</code> Comparison 
     *          type.
     * @see #setAuthnContextComparison(String)
     */
    public String getAuthnContextComparison() {
        return authnContextComparison;
    }

    /**
     * Sets the <code>AuthnContext</code> comparison type.
     *
     * @param authnContextComparison the <code>AuthnContext</code> comparison 
     *        type.
     * @see #getAuthnContextComparison
     */
    public void setAuthnContextComparison(String authnContextComparison) {
        this.authnContextComparison = authnContextComparison;
    }

    /**
     * Returns the <code>MinorVersion</code>.
     *
     * @return the <code>MinorVersion</code>.
     * @see #setMinorVersion(int)
     */
    public int getMinorVersion() {
       return minorVersion;
    }

    /**
     * Sets the <code>MinorVersion</code>.
     *
     * @param minorVersion the <code>MinorVersion</code>.
     * @see #getMinorVersion()
     */
    public void setMinorVersion(int minorVersion) {
        this.minorVersion = minorVersion;
    }

    /**
     * Returns a String representation of the Logout Response.
     *
     * @return a string containing the valid XML for this element
     * @throws FSMsgException if there is an error converting
     *         this object ot a string.
     */
    public String toXMLString() throws FSMsgException {
        return this.toXMLString(true, false);
    }
    
    /**
     * Returns a String representation of this object.
     *
     * @param includeNS : Determines whether or not the namespace qualifier
     *        is prepended to the Element when converted
     * @param declareNS : Determines whether or not the namespace is declared
     *        within the Element.
     * @return a string containing the valid XML for this element
     * @throws FSMsgException if there is an error converting
     *         this object to a string.
     */
    public String toXMLString(boolean includeNS, boolean declareNS) 
                              throws FSMsgException {
      return toXMLString(includeNS, declareNS, false);
    }
   
     /**
     * Returns a String representation of the Logout Response.
     *
     * @param includeNS Determines whether or not the namespace qualifier
     *        is prepended to the Element when converted
     * @param declareNS Determines whether or not the namespace is declared
     *        within the Element.
     * @param includeHeader Determines whether the output include the xml
     *        declaration header.
     * @return a string containing the valid XML for this element
     * @throws FSMsgException if there is an error converting
     *         this object ot a string.
     */
    public String toXMLString(boolean includeNS, boolean declareNS, 
                              boolean includeHeader) throws FSMsgException {

        StringBuffer xml = new StringBuffer(300);

        if (includeHeader) {
            xml.append("<?xml version=\"1.0\" encoding=\"").
                append(SAMLConstants.DEFAULT_ENCODING).append("\" ?>\n");
        }

        String prefixAC = "";
        String prefixLIB = "";
        String uriAC = "";
        String uriLIB = "";

        if (includeNS) {
            prefixLIB = IFSConstants.LIB_PREFIX;
            prefixAC = IFSConstants.AC_PREFIX;
        }

        if (declareNS) {
            if(minorVersion == IFSConstants.FF_12_PROTOCOL_MINOR_VERSION) {
               uriLIB = IFSConstants.LIB_12_NAMESPACE_STRING;
               uriAC = IFSConstants.AC_12_NAMESPACE_STRING;
            } else {
               uriLIB = IFSConstants.LIB_NAMESPACE_STRING;
               uriAC = IFSConstants.AC_NAMESPACE_STRING;
            }
        }
        
        xml.append("<").append(prefixLIB);

        if(minorVersion == IFSConstants.FF_12_PROTOCOL_MINOR_VERSION) {
           xml.append("RequestAuthnContext");
        } else {
           xml.append("AuthnContext");
        }

        xml.append(uriLIB).append(">\n");

        if ((authnContextClassRefList != null) && 
            (authnContextClassRefList != Collections.EMPTY_LIST)) {

            if((authnContextStatementRefList != null) && 
               (authnContextClassRefList != Collections.EMPTY_LIST)) {
               throw new FSMsgException("ExclusiveEntries",null);
            }

            Iterator j = authnContextClassRefList.iterator();
            while (j.hasNext()) {
                xml.append("<").append(prefixLIB).
                    append("AuthnContextClassRef").append(">"); 
                xml.append((String)j.next());
                xml.append("</").append(prefixLIB).
                    append("AuthnContextClassRef").append(">\n"); 
            }
        }
        
        
        if ((authnContextStatementRefList != null) && 
            (authnContextStatementRefList != Collections.EMPTY_LIST)) {

            Iterator j = authnContextStatementRefList.iterator();
            while (j.hasNext()) {
                xml.append("<").append(prefixLIB).
                    append("AuthnContextStatementRef").append(">"); 
                xml.append((String)j.next());
                xml.append("</").append(prefixLIB).
                    append("AuthnContextStatementRef").append(">\n"); 
            }
        }

        if(minorVersion == IFSConstants.FF_12_PROTOCOL_MINOR_VERSION) {
           xml.append("<").append(prefixLIB)
              .append("AuthnContextComparison").append(">")
              .append(authnContextComparison)
              .append("</").append(prefixLIB)
              .append("AuthnContextComparison").append(">\n");
        }
        
        xml.append("</").append(prefixLIB);
        if(minorVersion == IFSConstants.FF_12_PROTOCOL_MINOR_VERSION) {
           xml.append("RequestAuthnContext").append(">\n");
        } else {
           xml.append("AuthnContext").append(">\n");
        }

        return xml.toString();    
    }   
   
    /**
     * Returns <code>RequestAuthnContext</code> object. The
     * object is creating by parsing the <code>HttpServletRequest</code>
     * object.
     *
     * @param request the <code>HttpServletRequest</code> object.
     * @return <code><RequestAuthnContext/code> object.
     * @throws FSMsgException if there is an error
     *         creating <code>RequestAuthnContext</code> object.
     */
    public static RequestAuthnContext parseURLEncodedRequest(
           HttpServletRequest request, int minorVersion)
           throws FSMsgException {
        RequestAuthnContext retAuthnContext = new RequestAuthnContext();
        String strAuthnContextClassRef = 
               request.getParameter("AuthnContextClassRef");

        if(strAuthnContextClassRef != null){
           StringTokenizer st = new StringTokenizer(strAuthnContextClassRef);

            while (st.hasMoreTokens()) {
               if (retAuthnContext.authnContextClassRefList == null) {
                   retAuthnContext.authnContextClassRefList = new ArrayList();
               }                    
               retAuthnContext.authnContextClassRefList.add(st.nextToken());
            }
        }
        
        String strAuthnContextStatementRef = 
               request.getParameter("AuthnContextStatementRef");

        if(strAuthnContextStatementRef != null){
            StringTokenizer st = 
                new StringTokenizer(strAuthnContextStatementRef);

            while (st.hasMoreTokens()) {
               if (retAuthnContext.authnContextStatementRefList == null) {
                   retAuthnContext.authnContextStatementRefList = 
                                   new ArrayList();
               }                    
               retAuthnContext.authnContextStatementRefList.add(st.nextToken());
            }
        }

        String strAuthnContextComparison =
               request.getParameter("AuthnContextComparison");

        if(strAuthnContextComparison != null) {
           retAuthnContext.setAuthnContextComparison(strAuthnContextComparison);
        }

        retAuthnContext.setMinorVersion(minorVersion);
        
        return retAuthnContext;        
    }
    
      
    /**
     * Returns an URL Encoded String.
     *
     * @return a url encoded query string.
     * @throws FSMsgException if there is an error.
     */
    public String toURLEncodedQueryString() throws FSMsgException {

       StringBuffer urlEncodedAuthnReq = new StringBuffer(300);

       if ((authnContextClassRefList != null) && 
           (!authnContextClassRefList.isEmpty())) {

           if((authnContextStatementRefList != null) &&
              (!authnContextStatementRefList.isEmpty())) { 
               throw new FSMsgException("ExclusiveEntries",null);
           }

           StringBuffer strEncodedString = new StringBuffer(100); 
           Iterator j = authnContextClassRefList.iterator();
           strEncodedString.append((String)j.next());

           while(j.hasNext()) {
               strEncodedString.append(" ").append((String)j.next());
           }

           urlEncodedAuthnReq.append("AuthnContextClassRef=").
                    append(URLEncDec.encode(strEncodedString.toString())).
                    append(IFSConstants.AMPERSAND);
        }
       
       if ((authnContextStatementRefList != null) && 
            (!authnContextStatementRefList.isEmpty())) {

            StringBuffer strEncodedString = new StringBuffer(100); 
            Iterator j = authnContextStatementRefList.iterator();

            strEncodedString.append((String)j.next());
            while (j.hasNext()) {
                strEncodedString.append(" ").append((String)j.next());
            }

            urlEncodedAuthnReq.append("AuthnContextClassRef=").
                   append(URLEncDec.encode(strEncodedString.toString())).
                   append(IFSConstants.AMPERSAND);
        }

        if(minorVersion == IFSConstants.FF_12_PROTOCOL_MINOR_VERSION) {
            if(authnContextComparison != null) {
                   urlEncodedAuthnReq.append("AuthnContextComparison=").
                   append(URLEncDec.encode(authnContextComparison)).
                   append(IFSConstants.AMPERSAND);
            }
        }
        return urlEncodedAuthnReq.toString();         
    } 
}
