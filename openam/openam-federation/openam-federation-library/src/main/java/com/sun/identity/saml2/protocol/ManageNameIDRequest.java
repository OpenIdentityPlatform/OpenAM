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
 * $Id: ManageNameIDRequest.java,v 1.2 2008/06/25 05:47:57 qcheng Exp $
 *
 */


package com.sun.identity.saml2.protocol;

import com.sun.identity.saml2.protocol.RequestAbstract;
import com.sun.identity.saml2.protocol.NewEncryptedID;
import com.sun.identity.saml2.assertion.EncryptedID;
import com.sun.identity.saml2.assertion.NameID;
import com.sun.identity.saml2.common.SAML2Exception;

/**
 * This class represents the ManageNameIDRequestType complex type.
 * <p>The following schema fragment specifies the expected 	
 * content contained within this java content object. 	
 * <p>
 * <pre>
 * &lt;complexType name="ManageNameIDRequestType">
 *   &lt;complexContent>
 *     &lt;extension base="{urn:oasis:names:tc:SAML:2.0:protocol}RequestAbstractType">
 *       &lt;sequence>
 *         &lt;choice>
 *           &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}NameID"/>
 *           &lt;element ref="{urn:oasis:names:tc:SAML:2.0:assertion}EncryptedID"/>
 *         &lt;/choice>
 *         &lt;choice>
 *           &lt;element ref="{urn:oasis:names:tc:SAML:2.0:protocol}NewID"/>
 *           &lt;element ref="{urn:oasis:names:tc:SAML:2.0:protocol}NewEncryptedID"/>
 *           &lt;element ref="{urn:oasis:names:tc:SAML:2.0:protocol}Terminate"/>
 *         &lt;/choice>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * @supported.all.api
 */
public interface ManageNameIDRequest extends RequestAbstract {
    /**
     * Returns the value of the <code>newEncryptedID</code> property.
     * 
     * @return the value of the <code>newEncryptedID</code> property.
     */
    NewEncryptedID getNewEncryptedID();

    /**
     * Sets the value of the <code>newEncryptedID</code> property.
     * 
     * @param value the value of the <code>newEncryptedID</code> property.
     * @throws SAML2Exception if <code>Object</code> is immutable.
     */
    void setNewEncryptedID(NewEncryptedID value)
    throws SAML2Exception;

    /**
     * Returns the value of the <code>encryptedID</code> property.
     * 
     * @return the value of the <code>encryptedID</code> property.
     */
    EncryptedID getEncryptedID();

    /**
     * Sets the value of the <code>encryptedID</code> property.
     * 
     * @param value the value of the <code>encryptedID</code> property.
     * @throws SAML2Exception if <code>Object</code> is immutable.
     */
    void setEncryptedID(EncryptedID value)
    throws SAML2Exception;

    /**
     * Returns the value of the <code>NewID</code> property.
     * 
     * @return the value of the <code>NewID</code> property.
     */
    NewID getNewID();

    /**
     * Sets the value of the <code>NewID</code> property.
     * 
     * @param value the value of the <code>NewID</code> property.
     * @throws SAML2Exception if <code>Object</code> is immutable.
     */
    void setNewID(NewID value)
    throws SAML2Exception;

    /**
     * Returns the value of the <code>nameID</code> property.
     * 
     * @return the value of the <code>nameID</code> property.
     */
    NameID getNameID();

    /**
     * Sets the value of the <code>nameID</code> property.
     * 
     * @param value the value of the <code>nameID</code> property.
     * @throws SAML2Exception if <code>Object</code> is immutable.
     */
    void setNameID(NameID value)
    throws SAML2Exception;

    /**
     * Returns true if this is a terminating request.
     *
     * @return true if this is a terminating request.
     */ 
    boolean getTerminate();

    /**
     * Set this request as terminating request.
     *
     * @param terminate true to set this request as terminating request.
     * @throws SAML2Exception if this object is immutable.
     */
    void setTerminate(boolean terminate)
        throws SAML2Exception;
}
