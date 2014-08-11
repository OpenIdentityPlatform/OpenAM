/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: PrefixResourceName.java,v 1.1 2009/11/24 21:42:35 madan_ranganath Exp $
 *
 * Portions Copyrighted 2014 ForgeRock AS
 */
package com.sun.identity.shared.whitelist;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.shared.resourcename.BasePrefixResourceName;

import java.net.MalformedURLException;
import java.util.Map;

import static com.sun.identity.shared.whitelist.ResourceMatch.*;

/**
 * This is a plugin impelmentation of the <code>ResourceName</code> interface
 * it provides methods to do resource comparisons and resource
 * handling based on prefix based string match going left to right.
 * This kind of pattern matching would be used by URL kind of resources.
 */

public class PrefixResourceName extends BasePrefixResourceName<ResourceMatch, MalformedURLException>
        implements ResourceName {

    /**
     * empty no argument constructor.
     */
    public PrefixResourceName() {
        super(Debug.getInstance("patternMatching"), EXACT_MATCH, NO_MATCH, SUB_RESOURCE_MATCH, SUPER_RESOURCE_MATCH,
                WILDCARD_MATCH);
    }

    /**
     * Initializes the resource name with configuration information,
     * usally set by the administrators. The main configration information
     * retrived is mainly like wild card pattern used, one level wild card
     * pattern used, case sensitivity etc.
     *
     * @param configParams configuration parameters as a map.
     * The keys of the map are the configuration paramaters. 
     * Each key is corresponding to one <code>String</code> value
     * which specifies the configuration paramater value.
     */
    public void initialize(Map configParams) {
        String delimiterConfig = (String)configParams.get(
                        ResourceMatch.RESOURCE_COMPARATOR_DELIMITER);

        if (delimiterConfig != null) {
            this.delimiter = delimiterConfig;
        }

        String caseConfig = (String)configParams.get(
                        ResourceMatch.RESOURCE_COMPARATOR_CASE_SENSITIVE);
        if (caseConfig != null) {
            if (caseConfig.equals("true")) {
                this.caseSensitive = true;
            }
            else if (caseConfig.equals("false")) {
                this.caseSensitive = false;
            } else {
                this.caseSensitive = true;
                }
        }
        String wildcardConfig = (String)configParams.get(
                ResourceMatch.RESOURCE_COMPARATOR_WILDCARD);
        if (wildcardConfig != null) {
            this.wildcard = wildcardConfig;
        }
        String oneLevelWildcardConfig = (String)configParams.get(
                ResourceMatch.RESOURCE_COMPARATOR_ONE_LEVEL_WILDCARD);
        if (oneLevelWildcardConfig != null) {
            this.oneLevelWildcard = oneLevelWildcardConfig;
        }
        if (debug.messageEnabled()) {
            debug.message("PrefixResourceName:initialize():"+
            " delimiter = " + delimiter + 
            " wildcard = " + wildcard +
            " oneLevelWildcard = " + oneLevelWildcard +
            " case = " + caseConfig);
        }
        oneLevelWildcardLength = oneLevelWildcard.length();
        wildcardLength = wildcard.length();
        if (oneLevelWildcard.indexOf(wildcard) != -1) {
            wildcardEmbedded = true;
        } else {
            wildcardEmbedded = false;
        }
        if (wildcard.indexOf(oneLevelWildcard) != -1) {
            oneLevelWildcardEmbedded = true;
        } else {
            oneLevelWildcardEmbedded = false;
        }
        if (debug.messageEnabled()) {
            debug.message("wildcardEmbedded,oneLevelWildcardEmbedded"+
                wildcardEmbedded+","+oneLevelWildcardEmbedded);
        }
        return;
     }

    @Override
    protected MalformedURLException constructResourceInvalidException(Object[] args) {
        return new MalformedURLException("Invalid Resource specified");
    }

}
