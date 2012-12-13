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
 * $Id: ICOSDefinition.java,v 1.3 2008/06/25 05:41:47 qcheng Exp $
 *
 */

package com.iplanet.ums.cos;

import com.iplanet.ums.UMSException;

/**
 * This interface provides method signatures which will be implemented for COS
 * definitions. Each of the COS definition classes implement this interface, and
 * provide implementations for these methods.
 * @supported.api
 */
public interface ICOSDefinition {
    /**
     * This method sets the name for the COS definition.
     * 
     * @param name
     *            the name of this COS definition.
     * @supported.api
     */
    public void setName(String name);

    /**
     * This method returns the name of the COS definition.
     * 
     * @return the name of the COS definition.
     * @supported.api
     */
    public String getName();

    /**
     * This method adds a COS attribute to the COS definition.
     * 
     * @param attrName
     *            the name of the attribute.
     * @param qualifier
     *            The qualifier for the attribute. The valid range of values
     *            are: 0 (default), 1 (override), 2 (operational), 3
     *            (merge-schemes).
     * @throws UMSException
     *             if an exception occurs.
     * @supported.api
     */
    public void addCOSAttribute(String attrName, int qualifier)
            throws UMSException;

    /**
     * This method removes a COS attribute from the COS definiton.
     * 
     * @param attrName
     *            The name of the COS attribute to be removed.
     * @supported.api
     */
    public void removeCOSAttribute(String attrName);

    /**
     * This method returns an array of COS attributes.
     * 
     * @return an array of COS attributes.
     */
    public String[] getCOSAttributes();

    //
    // Attribute name strings for COS definitions.
    //
    /**
     * This field represents the default naming attribute for COS definitions.
     *
     * @supported.api
     */
    public static final String DEFAULT_NAMING_ATTR = "cn";

    /**
     * This field represents a keyword used in COS definitions.
     *
     * @supported.api
     */
    public static final String COSTEMPLATEDN = "cosTemplateDn";

    /**
     * This field represents a keyword used in COS definitions.
     *
     * @supported.api
     */
    public static final String COSSPECIFIER = "cosSpecifier";

    /**
     * This field represents a keyword used in COS definitions.
     *
     * @supported.api
     */
    public static final String ICOSSPECIFIER = "cosIndirectSpecifier";

    /**
     * This field represents a keyword used in COS definitions.
     *
     * @supported.api
     */
    public static final String COSATTRIBUTE = "cosAttribute";

    /**
     * This field represents an LDAP search filter used for searching for COS
     * definitions by name.
     * @supported.api
     */
    public static final String COSSUPERDEF_NAME_SEARCH = 
        "(&(objectclass=ldapsubentry)(objectclass=cossuperdefinition)("
            + DEFAULT_NAMING_ATTR + "=";

    /**
     * This field represents an LDAP search filter used for searching for COS
     * definitions.
     * @supported.api
     */
    public static final String COSSUPERDEF_SEARCH = 
        "&(objectclass=ldapsubentry)(objectclass=cossuperdefinition)";

    // for DS 4.x
    /**
     * This field represents a keyword used in Directory Server 4.x COS
     * implementations.
     * @supported.api
     */
    public static final String COSTARGETTREE = "cosTargetTree";

    //
    // cosAttribute qualifiers for COS definitions.
    //
    /**
     * This field represents the minimum value a COS attribute qualifier may
     * have.
     * @supported.api
     */
    public static final int minQualifier = 0;

    /**
     * This field represents the maximum value a COS attribute qualifier may
     * have.
     * @supported.api
     */
    public static final int maxQualifier = 2;

    /**
     * This field represents the numeric qualifier constant for "default".
     *
     * @supported.api
     */
    public static final int DEFAULT = 0;

    /**
     * This field represents the numeric qualifier constant for "override".
     *
     * @supported.api
     */
    public static final int OVERRIDE = 1;

    /**
     * This field represents the numeric qualifier constant for "operational".
     *
     * @supported.api
     */
    public static final int OPERATIONAL = 2;

    /**
     * This field represents the numeric qualifier constant for "merge-schemes".
     *
     * @supported.api
     */
    public static final int MERGE_SCHEMES = 3;

    /**
     * This represents a string array of COS attribute qualifiers. The valid
     * values are "default", "override", "operational", and "merge-schemes".
     *
     * @supported.api
     */
    public static final String[] qualifiers = { "default", "override",
            "operational", "merge-schemes" };

}
