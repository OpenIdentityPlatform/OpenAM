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
 * $Id: COSTemplate.java,v 1.4 2008/06/25 05:41:47 qcheng Exp $
 *
 */

package com.iplanet.ums.cos;

import com.iplanet.services.ldap.Attr;
import com.iplanet.services.ldap.AttrSet;
import com.iplanet.services.ldap.ModSet;
import com.iplanet.ums.CreationTemplate;
import com.iplanet.ums.PersistentObject;
import com.iplanet.ums.UMSException;

/**
 * This class represents a COS Template. A COS Template has attributes and
 * attribute values which will be dynamically added to entries affected by COS
 * definitions.
 * @supported.api
 */
public class COSTemplate extends PersistentObject {

    /**
     * NoArg constructor
     */
    public COSTemplate() {
    }

    /**
     * Constructor with creation template and name parameter.
     * 
     * @param temp
     *            the creation tenplate
     * @param name
     *            the name of this template
     * 
     * @throws UMSException
     *             The exception thrown from the parent class constructor.
     * @see com.iplanet.ums.PersistentObject#PersistentObject (CreationTemplate,
     *      AttrSet)
     * @supported.api
     */
    public COSTemplate(CreationTemplate temp, String name) throws UMSException {
        super(temp,
                new AttrSet(new Attr(COSTemplate.DEFAULT_NAMING_ATTR, name)));
    }

    /**
     * Returns the name of this COS template.
     * 
     * @return The name of this COS template.
     * @supported.api
     */
    public String getName() {
        String attributeValue = null;
        Attr attribute = getAttribute(getNamingAttribute());
        if (attribute != null) {
            attributeValue = attribute.getValue();
        }
        return attributeValue;
    }

    /**
     * Sets the priority for this template. The priority determines which COS
     * template will provide the attribute value if there are competing
     * templates. A priority of "0" is the highest priority.
     * 
     * @param priority Priority for this template.
     * @supported.api
     */
    public void setPriority(int priority) {
        setAttribute(new Attr("cosPriority", new Integer(priority).toString()));
    }

    /**
     * Adds a name/value attribute pair for this template; for example,
     * "postalcode" and "95020".
     * 
     * @param name
     *            the name of the attribute
     * 
     * @param value
     *            the value of the attribute
     * @supported.api
     */
    public void addTemplateAttribute(String name, String value) {
        modify(name, value, ModSet.ADD);
    }

    /**
     * Removes a name/value attribute pair from this template.
     * 
     * @param name
     *            the name of the attribute
     * @supported.api
     */
    public void removeTemplateAttribute(String name) {
        removeAttribute(new Attr(name));
    }

    /**
     * LDAP attribute names that apply to COS LDAP entries; used internally by
     * UMS and COS Manager.
     */
    static final String DEFAULT_NAMING_ATTR = "cn";

    static final String[] ATTRIBUTE_NAMES = { "objectclass",
            DEFAULT_NAMING_ATTR };

}
