/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: FSAuthDomainsModel.java,v 1.3 2008/06/25 05:49:39 qcheng Exp $
 *
 */

package com.sun.identity.console.federation.model;

import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface FSAuthDomainsModel
    extends AMModel
{
    /**
     * Attribute Name for Authenticaton Domain Description.
     */
    String TF_NAME = "tfName";

    /**
     * Attribute Name for Authenticaton Domain Description.
     */
    String TF_DESCRIPTION = "tfDescription";

    /**
     * Attribute Name for Writer Service URL.
     */
    String TF_IDFF_WRITER_SERVICE_URL = "tfIDFFWriterServiceURL";

    /**
     * Attribute Name for Reader Service URL.
     */
    String TF_IDFF_READER_SERVICE_URL = "tfIDFFReaderServiceURL";    
    
    /**
     * Attribute Name for Writer Service URL.
     */
    String TF_SAML2_WRITER_SERVICE_URL = "tfSAML2WriterServiceURL";

    /**
     * Attribute Name for Reader Service URL.
     */
    String TF_SAML2_READER_SERVICE_URL = "tfSAML2ReaderServiceURL";

    /**
     * Attribute Name for Authenticaton Domain Status.
     */
    String SINGLE_CHOICE_STATUS = "singleChoiceStatus";

    /**
     * Attribute Name for Authenticaton Domain realm.
     */   
    String SINGLE_CHOICE_REALM = "singleChoiceShowMenu";
    
    /**
     * Returns authentication domains 
     *
     * @return authentication domains 
     */
    Set getAuthenticationDomains();

    /**
     * Returns a Set of COT descriptors 
     *
     * @return Set of COT descriptors
     */
    Set getCircleOfTrustDescriptors();        
    
    /**
     * Creates authentication domain.
     *
     * @param attrValues Map of attribute name to set of attribute values.
     * @throws AMConsoleException if authentication domain cannot be created.
     */
    void createAuthenticationDomain(Map attrValues, Set providers)
	throws AMConsoleException;

    /**
     * Deletes authentication domains.
     * @param realm
     * @param cotName Name of circle of trust
     * @throws AMConsoleException if authentication domains cannot be deleted.
     */
    void deleteAuthenticationDomain(String realm, String cotName)
	throws AMConsoleException;

    /**
     * Returns attribute values.
     * @param realm
     * @param name Name of authentication domain.
     * @return attribute values.
     * @throws FSAllianceManagementException if attribute values cannot be 
     *         obtained.
     */
    Map getAttributeValues(String realm, String name)
	throws AMConsoleException;

    /**
     * Set attribute values.
     * @param realm Realm of authentication domain.
     * @param name Name of authentication domain.
     * @param values Map of attribute name to value.
     * @throws AMConsoleException if attribute values cannot be set.
     */
    void setAttributeValues(String realm, String name, Map values)
	throws AMConsoleException;

    /**
     * Returns a map of authentication domain attributes.
     *
     * @return Map of authentication domain attributes.
     */
    Map getDataMap();

    /**
     * Returns a set of provider names.
     * @param realm
     * @return a set of provider names.
     * @throws AMConsoleException if provider names cannot be obtained.
     */
    Set getAllProviderNames(String realm)
	throws AMConsoleException;

    /**
     * Returns a set of provider names under a authentication domain.
     * @param realm
     * @param name Name of authentication domain.
     * @return a set of provider names under a authentication domain.
     * @throws AMConsoleException if provider names cannot be obtained.
     */
    Set getTrustedProviderNames(String realm, String name)
	throws AMConsoleException;

    /**
     * Adds providers.
     * @param realm realm of circle of trust
     * @param cotName Name of circle of trust
     * @param names Names provider to be added.
     * @throws AMConsoleException if provider cannot be added.
     */
    public void addProviders(String realm, String cotName, Collection names)
        throws AMConsoleException;
        
    /**
     * Returns realms that have names matching with a filter.
     *
     * @param base Base realm name for this search. null indicates root
     *        suffix.
     * @param filter Filter string.
     * @return realms that have names matching with a filter.
     * @throws AMConsoleException if search fails.
     */
    Set getRealmNames(String base, String filter)
        throws AMConsoleException;
       
    /**
     * Returns the realm associated with the given circle of trust.
     *
     * @param name  circle of trust name.
     * @return realm where the circle of trust exists.
     * @throws AMConsoleException if search fails.
     */
    String getRealm(String name) throws AMConsoleException;

}
