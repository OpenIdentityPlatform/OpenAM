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
 * $Id: NameIdentifier.java,v 1.2 2008/06/25 05:47:32 qcheng Exp $
 *
 */

  

package com.sun.identity.saml.assertion;
import org.w3c.dom.Element;
import com.sun.identity.saml.common.SAMLException;
import com.sun.identity.saml.common.SAMLUtilsCommon;
import com.sun.identity.saml.common.SAMLRequesterException;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.shared.xml.XMLUtils;


/**
 * The NameIdentifier element specifies a <code>Subject</code> by a   
 * combination of a name and a security domain governing the name of the
 * <code>Subject</code>.
 * @supported.all.api
 */
public class NameIdentifier {
    static SAMLConstants sc;
    private java.lang.String _nameQualifier = "";
    private java.lang.String _format = "";
    private java.lang.String _name = "";

    /** 
     * Default contructor
     *
     */
    protected NameIdentifier() {}
   
    /**
     * Constructs a <code>NameIdentifer</code> element from an existing XML
     * block.
     *
     * @param nameIdentifierElement A <code>org.w3c.dom.Element</code> 
     *        representing DOM tree for <code>NameIdentifier</code> object
     * @exception SAMLException if it could not process the 
     *        <code>org.w3c.dom.Element</code> properly, implying that there
     *        is an error in the sender or in the element definition.
     */
    public NameIdentifier(org.w3c.dom.Element nameIdentifierElement)  
        throws SAMLException 
    {
        Element elt = (Element) nameIdentifierElement;
        String eltName = elt.getLocalName();
        if (eltName == null)  {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message(
                    "NameIdentifier: local name missing");
            }
            throw new SAMLRequesterException(SAMLUtilsCommon.bundle.getString
                ("nullInput")) ;   
        }
        if (!(eltName.equals("NameIdentifier")))  {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message(
                    "NameIdentifier: invalid root element");
            }
            throw new SAMLRequesterException(SAMLUtilsCommon.bundle.getString(
                "invalidElement")) ;   
        }
        String read = elt.getAttribute("NameQualifier");
        if (read != null) {
            _nameQualifier = read;
        }  // by default if not specified then _nameQualifer is ""
        read = elt.getAttribute("Format");
        // TODO considering the null and "" same both mean no 
        // format .
        if (read != null) {
            _format = read;
        }
        read = XMLUtils.getElementValue(elt);
        if ((read == null) || (read.length() == 0)) {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message("NameIdentifier: null input "
                    + "specified");
            }
            throw new SAMLRequesterException(SAMLUtilsCommon.bundle.getString(
                "nullInput")) ;   
        } else {
           _name = read;
        }
    }   

    /**
     * Constructor
     *@param name - The string representing the name of the Subject
     *@exception SAMLException if the input has an error.
     */
    public NameIdentifier(String name)  
        throws SAMLException 
    {
        if ((name == null) || (name.length() == 0)) {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message(
                    "NameIdentifier: null input specified");
            }
            throw new SAMLRequesterException(SAMLUtilsCommon.bundle.getString(
                "nullInput")) ;   
        }
        _name=name;
    }
   
    /**
     * Constructs a <code>NameQualifier</code> instance.
     *
     * @param name The string representing the name of the Subject
     * @param nameQualifier The security or administrative domain that qualifies
     *        the name of the <code>Subject</code>. This is optional, could be
     *        null or "".
     * @exception SAMLException if the input has an error.
     */
    public NameIdentifier(String name, String nameQualifier)  
        throws SAMLException 
    {
        if ((name == null) || (name.length() == 0)) {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message(
                    "NameIdentifier: null input specified");
            }
            throw new SAMLRequesterException(SAMLUtilsCommon.bundle.getString(
                "nullInput")) ;   
        }
        if (nameQualifier != null ) {
            _nameQualifier = nameQualifier;
        } // else by default its ""
            
        // TODO Treating null same as "" for nameQualifier 
        // as it does not have any -ve impact(??) but its easy in equals()
        // method
        // TODO need I restrict format to the ones defined per the SAML
        // specification. ?
        _name=name;
    }
   
    /**
     * Constructs a <code>NameQualifier</code> instance.
     *
     * @param name The string representing the name of the Subject
     * @param nameQualifier The security or administrative domain that qualifies
     *        the name of the <code>Subject</code>. This is optional could be
     *        null or "".
     * @param format The syntax used to describe the name of the
     *        <code>Subject</code>. This optional, could be null or "".
     * @exception SAMLException if the input has an error.
     */
    public NameIdentifier(String name, String nameQualifier, String format)  
        throws SAMLException 
    {
        if ((name == null) || (name.length() == 0)) {
            if (SAMLUtilsCommon.debug.messageEnabled()) {
                SAMLUtilsCommon.debug.message(
                    "NameIdentifier: null input specified");
            }
            throw new SAMLRequesterException(SAMLUtilsCommon.bundle.getString(
                "nullInput")) ;   
        }
        if (nameQualifier == null ) {
            _nameQualifier="";
        } else {
            _nameQualifier = nameQualifier;
        }
        // TODO Treating null same as "" for securityDomain 
        // as it does not have any -ve impact(??) but its easy in equals()
        // method
        // TODO need I restrict format to the ones defined per the SAML
        // specification. ?
        if (format == null ) {
            _format="";
        } else {
            _format = format;
        }
        _name=name;
    }
   
    /**
     * Returns format.
     * 
     * @return format element. Returns null if there is no format specified.
     */
    public java.lang.String  getFormat() {
        return _format;
    }

   
    /**
     * Sets the format attribute.
     *
     * @param format A String representing the format.
     * @return true if operation succeeds.
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
     * Returns the name.
     *
     * @return A String representing the <code>nameQualifier</code>. 
     * Returns null if there is no <code>nameQualifier</code>.
     */
    public java.lang.String  getNameQualifier() {
        return _nameQualifier;
    }

   
    /**
     * Sets <code>nameQualifier</code> attribute.
     *
     * @param nameQualifier name qualifier.
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
     * @return name
     */
    public java.lang.String getName() {
        return _name;
    }
   
    /**
     * Returns a String representation of the element.
     *
     * @return A string containing the valid XML for this element
     *         By default name space name is prepended to the element name 
     *         example <code>&lt;saml:NameIdentifier&gt;</code>.
     */
    public java.lang.String toString() {
        // call toString() with includeNS true by default and declareNS false
        String xml = this.toString(true, false);
        return xml;
    }

    /**
     * Returns String representation of the <code>&lt;NameIdentifier&gt;</code>
     * element.
     *
     * @param includeNS Determines whether or not the namespace qualifier is 
     *        prepended to the Element when converted.
     * @param declareNS Determines whether or not the namespace is declared 
     *        within the Element.
     * @return A string containing the valid XML for this element
     */
    public java.lang.String  toString(boolean includeNS, boolean declareNS) {
        StringBuffer xml = new StringBuffer(3000);
        String NS="";
        String appendNS="";
        if (declareNS) NS=sc.assertionDeclareStr;
        if (includeNS) appendNS="saml:";
        xml.append("<").append(appendNS).append("NameIdentifier").
            append(NS);
        if ((_nameQualifier != null) && (!(_nameQualifier.length() == 0))) {
            xml.append(" ").append("NameQualifier").append("=\"").
                append(_nameQualifier).append("\"");
        }
        if ((_format != null) && (!(_format.length() == 0))) {
            xml.append(" ").append("Format").append("=\"").append(_format).
            append("\"");
        }
        xml.append(sc.RIGHT_ANGLE).append(_name);
        xml.append(SAMLUtilsCommon.makeEndElementTagXML(
            "NameIdentifier",includeNS));
        return xml.toString();
    }                        

    /**
     * Checks for equality between this object and the
     * <code>NameQualifier</code> passed down as parameter. Checks if Name is
     * equal and if it has <code>NameQualifier</code> and Format defined
     * checks for equality in those too.
     *
     * @param nid <code>NameIdentifier</code> to be checked
     * @return true if the two <code>NameQualifier</code> are equal or not
     */
    public boolean equals(NameIdentifier nid) {
        if (nid != null) {
            String name = nid.getName();
            String nameQualifier = nid.getNameQualifier();
            String format = nid.getFormat();
            // never null as null converted to ""
            if ((name.length() == 0) || (!name.equalsIgnoreCase(_name))) {
                return false;
            }
            if (!nameQualifier.equalsIgnoreCase(_nameQualifier)) {
                return false;
            }
            // TODO checking format for exact match, is that correct ?
            if (!format.equals(_format)) {
                return false;
            }
        } else {
            return false;
        }
        return true;
    }
}

