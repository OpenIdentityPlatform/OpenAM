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
 * $Id: Protect.java,v 1.3 2008/06/25 05:42:38 qcheng Exp $
 *
 */
package com.sun.identity.config.resource;

import com.sun.identity.config.util.AjaxPage;
import com.sun.identity.config.pojos.UrlPattern;
import com.sun.identity.config.pojos.condition.*;

import java.util.List;

import net.sf.click.control.ActionLink;

/**
 * @author Les Hazlewood
 */
public class Protect extends AjaxPage {
    public List urlPatterns;
    public ActionLink addUrlPatternLink = new ActionLink("addUrlPattern", this, "addUrlPattern");
    public ActionLink removeUrlPatternLink = new ActionLink("removeUrlPattern", this, "removeUrlPattern");
    public ActionLink addConditionLink = new ActionLink("addCondition", this, "addCondition");
    public ActionLink removeConditionLink = new ActionLink("removeCondition", this, "removeCondition");

    public boolean removeUrlPattern() {
        String[] patterns = toString("urlPattern").split(",");
        UrlPattern[] urlPatterns = new UrlPattern[patterns.length];
        for(int i = 0; i < patterns.length; i++) {
            UrlPattern urlPattern = new UrlPattern();
            urlPattern.setId(Integer.valueOf(patterns[i]));
            urlPatterns[i] = urlPattern;
        }
        getConfigurator().removeUrlPatterns(urlPatterns);
        return false;
    }

    public boolean addUrlPattern() {
        UrlPattern urlPattern = new UrlPattern();
        urlPattern.setPattern(toString("newUrlPattern"));
        getConfigurator().addUrlPattern(urlPattern);
        return false;
    }

    public boolean removeCondition() {
        String[] conds = toString("condition").split(",");
        Condition[] conditions = new Condition[conds.length];
        for(int i = 0; i < conds.length; i++) {
            Condition condition = new Condition();
            condition.setId(Integer.valueOf(conds[i]));
            conditions[i] = condition;
        }
        getConfigurator().removeConditions(conditions);
        return false;
    }

    public boolean addCondition() {
        String[] conditionTypes = toString("conditionType").split(",");
        for(int i = 0; i < conditionTypes.length; i++) {
            Condition condition = resolveCondition(conditionTypes[i]);
            if (condition != null) {
                getConfigurator().addCondition(condition);
            }
        }
        return false;
    }

    private Condition resolveCondition(String conditionType) {
        Condition condition = null;
        if (conditionType.equals("authByModuleChain")) {
            condition = new AuthenticationByModuleChainCondition();
            condition.setName("Authentication By Module Chain");
        } else if (conditionType.equals("authByType")) {
            condition = new AuthenticationByTypeCondition();
            condition.setName("Authentication By Type");
        } else if (conditionType.equals("minAuthLevel")) {
            condition = new MinimumAuthenticationLevelCondition();
            condition.setName("Minimum Authentication Level");
        } else if (conditionType.equals("maxAuthLEvel")) {
            condition = new MaximumAuthenticationLevelCondition();
            condition.setName("Maximum Authentication Level");
        } else if (conditionType.equals("authToRealm")) {
            condition = new AuthenticationToRealmCondition();
            condition.setName("Authentication To Realm");
        } else if (conditionType.equals("currentSessionProperties")) {
            condition = new CurrentSessionPropertiesCondition();
            condition.setName("Current Session Properties");
        } else if (conditionType.equals("identityMembership")) {
            condition = new IdentityMembershipCondition();
            condition.setName("Identity Membership");
        } else if (conditionType.equals("ipAddress")) {
            condition = new IPAddressCondition();
            IPAddressCondition ipc = (IPAddressCondition) condition;
            ipc.setName(toString("name"));
            ipc.setIpAddressFrom(toString("fromIP1") + "." + toString("fromIP2") + "." + toString("fromIP3") + "." + toString("fromIP4"));
            ipc.setIpAddressTo(toString("toIP1") + "." + toString("toIP2") + "." + toString("toIP3") + "." + toString("toIP4"));
            ipc.setDnsName(toString("dnsName"));
            ipc.setDynamic(Boolean.valueOf("dynamic".equals(toString("parameterType"))));
        } else if (conditionType.equals("ldapFilter")) {
            condition = new LDAPFilterCondition();
            condition.setName("LDAP Filter");
        } else if (conditionType.equals("time")) {
            condition = new TimeCondition();
            condition.setName("Time");
        } else if (conditionType.equals("dnsName")) {
            condition = new DNSNameCondition();
            condition.setName("DNS Name");
        } else if (conditionType.equals("activeSessionTime")) {
            condition = new ActiveSessionTimeCondition();
            condition.setName("Active Session Time");
        }
        return condition;
    }
}
