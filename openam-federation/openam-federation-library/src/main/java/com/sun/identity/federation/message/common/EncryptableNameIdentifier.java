/*
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
 * $Id: EncryptableNameIdentifier.java,v 1.4 2008/06/25 05:46:46 qcheng Exp $
 *
 * Portions Copyrighted 2014-2016 ForgeRock AS.
 */

package com.sun.identity.federation.message.common;

import static org.forgerock.openam.utils.Time.*;

import org.w3c.dom.Element;
import java.util.Date;

import com.sun.identity.federation.common.FSException;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.common.FSUtils;

import com.sun.identity.saml.assertion.NameIdentifier;
import com.sun.identity.shared.DateUtils;
import com.sun.identity.shared.xml.XMLUtils;

/**
 * This class contains methods for encrypting the  <code>NameIdentifier</code> 
 * object.
 *
 * @supported.all.api
 * @deprecated since 12.0.0
 */
@Deprecated
public class EncryptableNameIdentifier {

    private java.lang.String _nameQualifier = "";
    private java.lang.String _name = "";
    private java.lang.String _nonce = "";
    private java.lang.String _format = "";
    private java.util.Date _issueInstant = null;


    /**
     * Default Constructor.
     */
    protected EncryptableNameIdentifier() {}

    /**
     * Constructor creates <code>EncryptableNameIdentifier</code> object.
     *
     * @param ni the <code>NameIdentifier</code> object to be encrypted.
     * @throws FSException if there is an error.
     */
    public EncryptableNameIdentifier(NameIdentifier ni) throws FSException {
        if(ni == null) {
           throw new FSException("nullInput", null) ;   
        }
        _nameQualifier = ni.getNameQualifier();
        _name = ni.getName();
        if(_nameQualifier == null || _name == null) {
           throw new FSException("nullInput", null) ;   
        }
        _format = ni.getFormat();
        if(_format == null) {
           throw new FSException("notValidFormat", null) ;   
        }
        _nonce = FSUtils.generateID();
        _issueInstant = newDate();
    }

    /**
     * Consturctor creates <code>EncryptableNameIdentifier</code> object.
     *
     * @param name 
     * @param nameQualifier
     * @param format
     * @param issueInstant the Issue Instant
     * @param nonce
     * @throws FSException if there is an error.
     */
    public EncryptableNameIdentifier(String name,String nameQualifier,
                                     String format,Date issueInstant,
                                     String nonce ) throws FSException {

        if(name == null || nameQualifier == null || issueInstant == null ||
            format == null || nonce == null) {
           throw new FSException("nullInput", null) ;   
        }
        _name = name;
        _nameQualifier = nameQualifier;
        _format = format;
        _nonce = nonce;
        _issueInstant = issueInstant;
    }


   
    /**
     * Constructs a <code>EncryptedNameIdentifer</code> element from 
     * the Document Element.
     *
     * @param nameIdentifier a <code>org.w3c.dom.Element</code> 
     *        representing DOM tree for <code>EncryptableNameIdentifier</code>
     *        object
     * @throws FSException if it could not process the 
     *            <code>org.w3c.dom.Element</code> properly, implying that there
     *            is an error in the sender or in the element definition.
     */
    public EncryptableNameIdentifier(org.w3c.dom.Element nameIdentifier)  
        throws FSException {
        Element elt = (Element) nameIdentifier;
        String eltName = elt.getLocalName();
        if (eltName == null)  {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("EncryptableNameIdentifier: local" +
                " name missing");
            }
            throw new FSException("nullInput", null) ;   
        }
        if (!(eltName.equals("EncryptableNameIdentifier")))  {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("EncryptableNameIdentifier: invalid"+
                " root element");
            }
            throw new FSException("invalidElement", null) ;   
        }
        String read = elt.getAttribute("NameQualifier");
        if (read != null) {
            _nameQualifier = read;
        }
        read = elt.getAttribute("Format");
        if (read != null) {
            _format = read;
        }

        read = elt.getAttribute("Nonce");
        if (read != null) {
            _nonce = read;
        }

        read = elt.getAttribute("IssueInstant");
        if(read != null) {
           try {
               _issueInstant = DateUtils.stringToDate(read);
           } catch (java.text.ParseException pe) {
               if (FSUtils.debug.messageEnabled()) {
                   FSUtils.debug.message("EncryptableNameIdentifier: "+
                   "Could not parse issue instant", pe);
               }
               throw new FSException("wrongInput", null) ;   
           }
        }
        read = XMLUtils.getElementValue(elt);
        if ((read == null) || (read.length() == 0)) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("EncryptableNameIdentifier: null"+
                " input specified");
            }
            throw new FSException("nullInput", null) ;   
        } else {
           _name = read;
        }
    }   

    /**
     * Returns value of the <code>Format</code> attribute.
     * 
     * @return value of the <code>Format</code> attribute.
     */
    public java.lang.String getFormat() {
        return _format;
    }

   
    /**
     * Sets the <code>Format</code> attribute.
     *
     * @param format the value of the <code>Format</code> attribute.
     * @return true if the operation succeeds.
     */
    public boolean setFormat(java.lang.String  format ) {
        // TODO do I need to restrict the format to those defined 
        // by SAML specification ?
        if ((format == null) || (format.length() == 0))  {
            return false;
        }
        _format = format;
        return true;
    }
   
    /**
     * Returns the <code>NameQualifier</code> attribute.
     *
     * @return the <code>nameQualifier</code>. 
     */
    public java.lang.String  getNameQualifier() {
        return _nameQualifier;
    }

   
    /**
     * Sets <code>nameQualifier</code> attribute.
     *
     * @param nameQualifier the  <code>nameQualifier</code> attribute.
     * @return true if operation succeeds.
     */
    public boolean setNameQualifier(java.lang.String  nameQualifier ) {
        if ((nameQualifier == null) || (nameQualifier.length() == 0))  {
            return false;
        }
        _nameQualifier=nameQualifier;
        return true;
    }

    /**
     * Sets the name attribute.
     *
     * @param name name of the <code>nameQualifier</code>.
     * @return true if operation succeeds.
     */
    protected boolean setName(java.lang.String  name ) {
        if ((name == null) || (name.length() == 0))  {
            return false;
        }
        _name = name;
        return true;
    }

    /**
     * Returns the name from <code>NameQualifier</code>.
     *
     * @return the name from <code>NameQualifier</code>.
     */
    public java.lang.String getName() {
        return _name;
    }

    /**
     * Retunrs the nounce.
     *
     * @return the nounce.
     */
    public java.lang.String getNonce() {
        return _nonce;
    }

    /**
     * Returns the Issue Instant.
     *
     * @return the Issue Instant.
     */
    public java.util.Date getIssueInstant() {
        return _issueInstant;
    }
   
    /**
     * Returns a String representation of the element.
     *
     * @return A string containing the valid XML for this element
     *         By default name space name is prepended to the element name 
     *         example <code>&lt;saml:EncryptableNameIdentifier&gt;</code>.
     */
    public java.lang.String toString() {
        // call toString() with includeNS true by default and declareNS false
         String xml = this.toString(true, false);
        return xml;
    }

    /**
     * Returns String representation of the 
     * <code>&lt;EncryptableNameIdentifier&gt;</code> element.
     *
     * @param includeNS Determines whether or not the namespace qualifier is 
     *        prepended to the Element when converted.
     * @param declareNS Determines whether or not the namespace is declared 
     * within the Element.
     * @return A string containing the valid XML for this element
     */
    public java.lang.String  toString(boolean includeNS, boolean declareNS) {
        StringBuffer xml = new StringBuffer(3000);
        String NS="";
        String appendNS="";
        if (declareNS) {
            NS=IFSConstants.LIB_12_NAMESPACE_STRING;
        }
        if (includeNS) {
            appendNS=IFSConstants.LIB_PREFIX;
        }

        String dateStr = null;
        if(_issueInstant != null) {
           dateStr = DateUtils.toUTCDateFormat(_issueInstant);
        }

        xml.append("<").append(appendNS).append("EncryptableNameIdentifier").
             append(NS);
        if ((_nameQualifier != null) && (!(_nameQualifier.length() == 0))) {
            xml.append(" ").append("NameQualifier").append("=\"").
                append(_nameQualifier).append("\"");
        }
        if ((_format != null) && (!(_format.length() == 0))) {
            xml.append(" ").append("Format").append("=\"").append(_format).
            append("\"");
        }
        if ((_nonce != null) && (!(_nonce.length() == 0))) {
            xml.append(" ").append("Nonce").append("=\"").append(_nonce).
            append("\"");
        }
        if ((_issueInstant != null) && (dateStr.length() != 0)) {
            xml.append(" ").append("IssueInstant").append("=\"").
            append(dateStr).append("\"");
        }
        xml.append(">").append(_name);
        xml.append("</").append(appendNS).append("EncryptableNameIdentifier").
        append(">");
           return xml.toString();
    }                        
}
