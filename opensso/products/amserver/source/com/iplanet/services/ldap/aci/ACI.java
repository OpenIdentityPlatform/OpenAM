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
 * $Id: ACI.java,v 1.4 2008/06/25 05:41:38 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.services.ldap.aci;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

/**
 * Class that encapsulates directory entry aci Provides a simple programmatic
 * interface to compose, set, query and parse ACI
 * @supported.api
 */
public class ACI {

    public static final String ACI = "aci";

    static final String[] SUPPORTED_ATTR_RULES = { "userdnattr", "groupdnattr",
            "userattr" };

    static final Collection SUPPORTED_ATTR_RULES_COLLECTION = Arrays
            .asList(SUPPORTED_ATTR_RULES);

    static final String TARGET = "target";

    static final String TARGETFILTER = "targetfilter";

    static final String TARGETATTR = "targetattr";

    static final String TARGETATTRFILTERS = "targetattrfilters";

    static final String VERSION = "version";

    static final String ACL = "acl";

    static final String ALLOW = "allow";

    static final String DENY = "deny";

    static final String USERDN = "userdn";

    static final String GROUPDN = "groupdn";

    static final String ROLEDN = "roledn";

    static final String USERDNATTR = "userdnattr";

    static final String GROUPDNATTR = "groupdnattr";

    static final String USERATTR = "userattr";

    static final String AUTHMETHOD = "authmethod";

    static final String IP = "ip";

    static final String DNS = "dns";

    static final String TIMEOFDAY = "timeofday";

    static final String DAYOFWEEK = "dayofweek";

    static final String PRINCIPAL_SET = "principal";

    static final String AUTHMETHOD_SET = "authmethod";

    static final String IP_SET = "IP";

    static final String TOD_SET = "tod";

    static final String DOW_SET = "dow";

    static final String KEYWORD = "keyword";

    static final String OPERATOR = "operator";

    static final String VALUE = "value";

    static final String OPENPARENTH = "(";

    static final String CLOSEPARENTH = ")";

    static final String EXPRESSIONCONNECTOR = "expressionconnector";

    static final String EQ = "=";

    static final String NE = "!=";

    static final String GE = ">=";

    static final String LE = "<=";

    static final String GT = ">";

    static final String LT = "<";

    static final String AND = "and";

    static final String OR = "or";

    static final String OR_PIPE = "||";

    static final String SPACE = " ";

    static final String QUOTE = "\"";

    static final String NEWLINE = "\n";

    static final String SEMICOLON = ";";

    static final String COMMA = ",";

    static final String LDAP_PREFIX = "ldap:///";

    /*
     * List of keywords in the context of ACI: Keywords that control the target
     * set: target targetattr targetfilter targetattrfilters
     * 
     * Keywords that control the permissions: allow deny
     * 
     * Legal values for permissions: read write add delete search compare
     * selfwrite proxy all
     * 
     * Keywords that are allowed in bindrule: userdn with special values: self
     * all anyone groupdn userdnattr groupdnattr userattr ip dns timeofday
     * dayofweek authmethod none simple ssl sasl
     */

    /**
     * No argument constructor
     * @supported.api
     */
    public ACI() {
    }

    /**
     * Constructor
     * 
     * @param name
     *            name of the ACI
     * @supported.api
     */
    public ACI(String name) {
        _name = name;
    }

    /*
     * Constructor.
     * 
     * @param name name of the ACI.
     * @param target the target to which the ACI applies.
     * @param tagetFilter the LDAP filter that controls the set of entries to
     *        which the ACI applies.
     * @param targetAttributes <code>QualfiedCollection</code> of attributes to
     *        which the ACI applies.
     * @param users collection of users for who the ACI applies.
     * @param permissions <code>QualifiedCollection</code> of permissions that
     *        apply to the ACI.
     * @link QualifiedCollection.setExclusive
     * @supported.api
     */
    public ACI(String name, String target, String targetFilter,
            QualifiedCollection targetAttributes, Collection users,
            QualifiedCollection permissions) {
        setName(name);
        setTarget(target);
        setTargetFilter(targetFilter);
        setTargetAttributes(targetAttributes);
        setUsers(users);
        setPermissions(permissions);
    }

    /**
     * Checks whether the object is passed is semantically equal to this object.
     * The objects are considered to be equal if both the objects have the same
     * state, that is their respective instance variables have equal values.
     * 
     * @param object
     *            the object to check for equality
     * @return <code>true</code> if the passed object is equal to this object,
     *         <code>false</code> otherwise
     * @supported.api
     */
    public boolean equals(Object object) {
        boolean objectsEqual = false;
        if (this == object) {
            objectsEqual = true;
        } else if (object != null && object.getClass().equals(getClass())) {
            ACI castObject = (ACI) object;
            if (castObject.getName().equals(getName())
                    && castObject.getTarget().equals(getTarget())
                    && castObject.getTargetFilter().equals(getTargetFilter())
                    && castObject.getTargetAttributes().equals(
                            getTargetAttributes())
                    && castObject.getPermissions().equals(getPermissions())
                    && castObject.getUsers().equals(getUsers())
                    && castObject.getGroups().equals(getGroups())
                    && castObject.getRoles().equals(getRoles())
                    && castObject.getClientHostNames().equals(
                            getClientHostNames())
                    && castObject.getTimesOfDay().equals(getTimesOfDay())
                    && castObject.getDaysOfWeek().equals(getDaysOfWeek())
                    && castObject.getAuthMethods().equals(getAuthMethods())) {
                objectsEqual = true;
            }
        }
        return objectsEqual;
    }

    /**
     * Sets the name of the ACI
     * 
     * @param name
     *            the name of the ACI
     * @supported.api
     */
    public void setName(String name) {
        _name = name;
    }

    /**
     * Gets the name of the ACI
     * 
     * @return the name of the ACI
     * @supported.api
     */
    public String getName() {
        return _name;
    }

    /**
     * Sets the target of the ACI
     * 
     * @param target
     *            the target of the ACI
     * @supported.api
     */
    public void setTarget(String target) {
        _target = target;
    }

    /**
     * Gets the target of the ACI
     * 
     * @return the target of the ACI
     * @supported.api
     */
    public String getTarget() {
        return _target;
    }

    /**
     * Sets the target filter of the ACI
     * 
     * @param targetFilter
     *            the ldap target filter for the ACI
     * @supported.api
     */
    public void setTargetFilter(String targetFilter) {
        _targetFilter = targetFilter;
    }

    /**
     * Gets the target filter for the ACI
     * 
     * @return the target filter that controls the entries to which the ACI
     *         apllies
     * @supported.api
     */
    public String getTargetFilter() {
        return _targetFilter;
    }

    /**
     * Removes the target filter of the ACI
     * @supported.api
     */
    public void removeTargetFilter() {
        _targetFilter = null;
    }

    /**
     * Sets the QualifiedCollection of targetAttributes that apply to the ACI
     * 
     * @param targetAttributes
     *            the QualifiedCollection of target attributes that apply to the
     *            ACI
     * @supported.api
     */
    public void setTargetAttributes(QualifiedCollection targetAttributes) {
        _targetAttributes = targetAttributes;
    }

    /**
     * Geets the QualifiedCollection of targetAttributes that apply to the ACI
     * 
     * @return the QualifiedCollection of target attributes that apply to the
     *         ACI
     * @supported.api
     */

    public QualifiedCollection getTargetAttributes() {
        return _targetAttributes;
    }

    /**
     * Removes the QualifiedCollection of targetAttributes that contol the
     * attributes to which this ACI apllies
     * @supported.api
     */
    public void removeTargetAttributes() {
        _targetAttributes = null;
    }

    /**
     * Sets the QualifiedCollection of permissions that apply to the ACI
     * 
     * @param permissions
     *            the QualifiedCollection of permissions that apply to the ACI
     *           
     * @supported.api
     */
    public void setPermissions(QualifiedCollection permissions) {
        _permissions = permissions;
    }

    /**
     * Geets the QualifiedCollection of permissions that apply to the ACI
     * 
     * @return the QualifiedCollection of permissions that apply to the ACI
     *        
     * @supported.api
     */
    public QualifiedCollection getPermissions() {
        return _permissions;
    }

    /**
     * Sets the collection of users to whom the ACI apllies
     * 
     * @param users
     *            the collection of users to whom the ACI apllies
     *           
     * @supported.api
     */
    public void setUsers(Collection users) {
        _users = users;
    }

    /**
     * Gets the collection of users to whom the ACI apllies
     * 
     * @return the collection of users to whom the ACI apllies
     *        
     * @supported.api
     */
    public Collection getUsers() {
        return _users;
    }

    /**
     * Sets the collection of groups to whom the ACI apllies
     * 
     * @param groups
     *            the collection of groups to whom the ACI apllies
     *           
     * @supported.api
     */
    public void setGroups(Collection groups) {
        _groups = groups;
    }

    /**
     * Gets the collection of groups to whom the ACI apllies
     * 
     * @return the collection of groups to whom the ACI apllies
     *        
     * @supported.api
     */
    public Collection getGroups() {
        return _groups;
    }

    /**
     * Sets the collection of roles to which the ACI applies
     * 
     * @param roles
     *            the collection of roles to which the ACI applies
     *           
     * @supported.api
     */
    public void setRoles(Collection roles) {
        _roles = roles;
    }

    /**
     * Gets the collection of roles to which the ACI applies
     * 
     * @return the collection of roles to which the ACI applies
     *        
     * @supported.api
     */
    public Collection getRoles() {
        return _roles;
    }

    /**
     * Sets the client IPs to which this ACI applies
     * 
     * @param clientIP
     *            collection of client IPs to which this ACI applies
     *           
     * @supported.api
     */
    public void setClientIP(Collection clientIP) {
        _clientIP = clientIP;
    }

    /**
     * Gets the client IPs to which this ACI applies
     * 
     * @return collection of client IPs to which this ACI applies
     *        
     * @supported.api
     */
    public Collection getClientIP() {
        return _clientIP;
    }

    /**
     * Sets the client DNS host names to which this ACI applies
     * 
     * @param clientHostNames
     *            collection of DNS host names to which this ACI applies
     *           
     * @supported.api
     */
    public void setClientHostNames(Collection clientHostNames) {
        _clientHostNames = clientHostNames;
    }

    /**
     * Gets the client DNS host names to which this ACI applies
     * 
     * @return collection of DNS host names to which this ACI applies
     *        
     * @supported.api
     */
    public Collection getClientHostNames() {
        return _clientHostNames;
    }

    /**
     * Sets the times of the day at which this ACI applies
     * 
     * @param timesOfDay
     *            collection of timesOfDay at which this ACI applies
     *           
     * @supported.api
     */
    public void setTimesOfDay(Collection timesOfDay) {
        _timesOfDay = timesOfDay;
    }

    /**
     * Gets the times of the day at which this ACI applies
     * 
     * @return collection of timesOfDay at which this ACI applies
     *        
     * @supported.api
     */
    public Collection getTimesOfDay() {
        return _timesOfDay;
    }

    /**
     * Sets the days of the week on which this ACI applies
     * 
     * @param daysOfWeek
     *            collection of days of week on which this ACI applies
     *           
     * @supported.api
     */
    public void setDaysOfWeek(Collection daysOfWeek) {
        _daysOfWeek = daysOfWeek;
    }

    /**
     * Gets the days of the week on which this ACI applies
     * 
     * @return collection of days of week on which this ACI applies
     *        
     * @supported.api
     */
    public Collection getDaysOfWeek() {
        return _daysOfWeek;
    }

    /**
     * Sets the authorization methods to which this ACI applies
     * 
     * @param authMethods
     *            the collection of authorization methods to which this ACI
     *            applies
     */
    public void setAuthMethods(Collection authMethods) {
        _authMethods = authMethods;
    }

    /**
     * Gets the authorization methods to which this ACI applies
     * 
     * @return collection of authorization methods to which this ACI applies
     *        
     * @supported.api
     */
    public Collection getAuthMethods() {
        return _authMethods;
    }

    /**
     * Sets the value for the given attrRule name
     * 
     * @param attrName
     *            name of the attribute
     * @param values
     *            collections of value for the attr rule
     * @supported.api
     */
    public void setAttrRuleValue(String attrName, Collection values)
            throws ACIComposeException {
        attrName = attrName.toLowerCase();
        if (attrName.equals(USERDNATTR)) {
            setUserDNAttrs(values);
        } else if (attrName.equals(GROUPDNATTR)) {
            setGroupDNAttrs(values);
        } else if (attrName.equals(USERATTR)) {
            setUserAttrs(values);
        } else {
            throw new ACIComposeException("Unsupported attr rule name : "
                    + attrName);
        }
    }

    /**
     * Gets the collections of values for the given attrRuleName
     * 
     * @return collection of attr rule names supported by the ACI API
     *        
     * @supported.api
     */
    public Collection getAttrRuleValue(String attrName) throws ACIException {
        Collection values = null;
        if (attrName.equals(USERDNATTR)) {
            values = getUserDNAttrs();
        } else if (attrName.equals(GROUPDNATTR)) {
            values = getGroupDNAttrs();
        } else if (attrName.equals(USERATTR)) {
            values = getUserAttrs();
        } else {
            throw new ACIException("Unsupported attr rule name : " + attrName);
        }
        return values;
    }

    /**
     * Gets the names of supported attr rule names
     * 
     * @return the collection of attr rule names supported by the ACI API
     *        
     * @supported.api
     */
    public Collection getSupportedAttrRules() {
        return SUPPORTED_ATTR_RULES_COLLECTION;
    }

    /**
     * Sets the target attr filters that controls value based access control
     * 
     * @param targetAttrFilters
     *            string defining a filter for value based access control
     *           
     * @supported.api
     */
    public void setTargetAttrFilters(String targetAttrFilters) {
        _targetAttrFilters = targetAttrFilters;
    }

    /**
     * Gets the target attr filters that controls value based access control
     * 
     * @return string defining a filter for value based access control
     *        
     * @supported.api
     */
    public String getTargetAttrFilters() {
        return _targetAttrFilters;
    }

    /**
     * Gets a string representation of this ACI
     * 
     * @return string representation of this ACI
     * @supported.api
     */
    public String toString() {
        StringBuilder aci = new StringBuilder();
        StringBuffer bindRule = new StringBuffer();
        StringBuffer tempBuffer = new StringBuffer();
        String value = null;

        value = getTarget();
        if (value != null && value.length() != 0) {
            aci.append(SPACE).append(OPENPARENTH).append(TARGET).append(SPACE)
                    .append(EQ).append(SPACE).append(QUOTE).append(value)
                    .append(QUOTE).append(CLOSEPARENTH).append(NEWLINE);
        }

        QualifiedCollection qc = null;
        Iterator iter = null;
        boolean exclusive;
        String operator;
        qc = getTargetAttributes();
        if (qc != null && qc.getCollection() != null
                && !qc.getCollection().isEmpty()) {
            exclusive = qc.isExclusive();
            operator = exclusive ? NE : EQ;
            aci.append(SPACE).append(OPENPARENTH).append(TARGETATTR).append(
                    SPACE).append(operator).append(SPACE);
            iter = qc.getCollection().iterator();
            if (iter.hasNext()) {
                value = (String) iter.next();
                aci.append(QUOTE).append(value);
            }
            while (iter.hasNext()) {
                value = (String) iter.next();
                aci.append(OR_PIPE).append(value);
            }
            aci.append(QUOTE).append(CLOSEPARENTH).append(NEWLINE);
        }

        value = getTargetFilter();
        if (value != null && value.length() != 0) {
            aci.append(SPACE).append(OPENPARENTH).append(TARGETFILTER).append(
                    SPACE).append(EQ).append(SPACE).append(QUOTE).append(value)
                    .append(QUOTE).append(CLOSEPARENTH).append(NEWLINE);
        }

        value = getTargetAttrFilters();
        if (value != null && value.length() != 0) {
            aci.append(SPACE).append(OPENPARENTH).append(TARGETATTRFILTERS)
                    .append(SPACE).append(EQ).append(SPACE).append(QUOTE)
                    .append(value).append(QUOTE).append(CLOSEPARENTH).append(
                            NEWLINE);
        }

        aci.append(SPACE).append(OPENPARENTH).append(VERSION).append(SPACE)
                .append(getVersion()).append(SEMICOLON);
        aci.append(ACL).append(SPACE).append(QUOTE).append(getName()).append(
                QUOTE).append(SEMICOLON);

        qc = getPermissions();
        if (qc != null && qc.getCollection() != null
                && !qc.getCollection().isEmpty()) {
            exclusive = qc.isExclusive();
            String permissionType = exclusive ? DENY : ALLOW;
            aci.append(permissionType).append(OPENPARENTH);
            iter = qc.getCollection().iterator();
            if (iter.hasNext()) {
                value = (String) iter.next();
                aci.append(value);
            }
            while (iter.hasNext()) {
                value = (String) iter.next();
                aci.append(COMMA).append(SPACE).append(value);
            }
            aci.append(CLOSEPARENTH).append(NEWLINE);
        }

        Collection collection = null;
        collection = getUsers();
        if (collection != null && !collection.isEmpty()) {
            iter = collection.iterator();
            if (iter.hasNext()) {
                value = (String) iter.next();
                tempBuffer.append(USERDN).append(EQ).append(QUOTE).append(
                        LDAP_PREFIX).append(value);
            }
            while (iter.hasNext()) {
                value = (String) iter.next();
                tempBuffer.append(SPACE).append(OR_PIPE).append(SPACE).append(
                        LDAP_PREFIX).append(value);
            }
            tempBuffer.append(QUOTE).append(SPACE);
        }
        if (tempBuffer.length() != 0) {
            bindRule.append(tempBuffer);
        }

        tempBuffer.setLength(0);
        collection = getGroups();
        if (collection != null && !collection.isEmpty()) {
            iter = collection.iterator();
            if (iter.hasNext()) {
                value = (String) iter.next();
                tempBuffer.append(GROUPDN).append(EQ).append(QUOTE).append(
                        LDAP_PREFIX).append(value);
            }
            while (iter.hasNext()) {
                value = (String) iter.next();
                tempBuffer.append(SPACE).append(OR_PIPE).append(SPACE).append(
                        LDAP_PREFIX).append(value);
            }
            tempBuffer.append(QUOTE).append(SPACE);
        }
        if (tempBuffer.length() != 0) {
            if (bindRule.length() > 0) {
                bindRule.append(SPACE).append(OR).append(SPACE);
            }
            bindRule.append(tempBuffer);
        }

        tempBuffer.setLength(0);
        collection = getRoles();
        if (collection != null && !collection.isEmpty()) {
            iter = collection.iterator();
            if (iter.hasNext()) {
                value = (String) iter.next();
                tempBuffer.append(ROLEDN).append(EQ).append(QUOTE).append(
                        LDAP_PREFIX).append(value);
            }
            while (iter.hasNext()) {
                value = (String) iter.next();
                tempBuffer.append(SPACE).append(OR_PIPE).append(SPACE).append(
                        LDAP_PREFIX).append(value);
            }
            tempBuffer.append(QUOTE).append(SPACE);
        }
        if (tempBuffer.length() != 0) {
            if (bindRule.length() > 0) {
                bindRule.append(" or ");
            }
            bindRule.append(tempBuffer);
        }

        tempBuffer.setLength(0);
        collection = getUserDNAttrs();
        if (collection != null && !collection.isEmpty()) {
            iter = collection.iterator();
            if (iter.hasNext()) {
                value = (String) iter.next();
                tempBuffer.append(USERDNATTR).append(EQ).append(QUOTE).append(
                        value).append(QUOTE);
            }
            while (iter.hasNext()) {
                value = (String) iter.next();
                tempBuffer.append(SPACE).append(OR).append(SPACE);
                tempBuffer.append(USERDNATTR).append(EQ).append(QUOTE).append(
                        value).append(QUOTE);
            }
        }
        if (tempBuffer.length() != 0) {
            if (bindRule.length() > 0) {
                bindRule.append(SPACE).append(OR).append(SPACE);
            }
            bindRule.append(tempBuffer);
        }

        tempBuffer.setLength(0);
        collection = getGroupDNAttrs();
        if (collection != null && !collection.isEmpty()) {
            iter = collection.iterator();
            if (iter.hasNext()) {
                value = (String) iter.next();
                tempBuffer.append(GROUPDNATTR).append(EQ).append(QUOTE).append(
                        value).append(QUOTE);
            }
            while (iter.hasNext()) {
                value = (String) iter.next();
                tempBuffer.append(SPACE).append(OR).append(SPACE);
                tempBuffer.append(GROUPDNATTR).append(EQ).append(QUOTE).append(
                        value).append(QUOTE);
            }
        }
        if (tempBuffer.length() != 0) {
            if (bindRule.length() > 0) {
                bindRule.append(SPACE).append(OR).append(SPACE);
            }
            bindRule.append(tempBuffer);
        }

        tempBuffer.setLength(0);
        collection = getUserAttrs();
        if (collection != null && !collection.isEmpty()) {
            iter = collection.iterator();
            if (iter.hasNext()) {
                value = (String) iter.next();
                tempBuffer.append(USERATTR).append(EQ).append(QUOTE).append(
                        value).append(QUOTE);
            }
            while (iter.hasNext()) {
                value = (String) iter.next();
                tempBuffer.append(SPACE).append(OR).append(SPACE);
                tempBuffer.append(USERATTR).append(EQ).append(QUOTE).append(
                        value).append(QUOTE);
            }
        }
        if (tempBuffer.length() != 0) {
            if (bindRule.length() > 0) {
                bindRule.append(SPACE).append(OR).append(SPACE);
            }
            bindRule.append(tempBuffer);
        }

        if (bindRule.length() > 0) {
            bindRule.insert(0, SPACE);
            bindRule.insert(1, OPENPARENTH);
            bindRule.append(CLOSEPARENTH).append(NEWLINE);
        }

        tempBuffer.setLength(0);
        collection = getAuthMethods();
        if (collection != null && !collection.isEmpty()) {
            iter = collection.iterator();
            if (iter.hasNext()) {
                value = (String) iter.next();
                tempBuffer.append(AUTHMETHOD).append(EQ).append(QUOTE).append(
                        value).append(QUOTE);
            }
            while (iter.hasNext()) {
                value = (String) iter.next();
                tempBuffer.append(SPACE).append(OR).append(SPACE);
                tempBuffer.append(AUTHMETHOD).append(EQ).append(QUOTE).append(
                        value).append(QUOTE);
            }
        }
        if (tempBuffer.length() != 0) {
            if (bindRule.length() > 0) {
                bindRule.append(SPACE).append(AND).append(SPACE);
            }
            bindRule.append(OPENPARENTH).append(tempBuffer)
                    .append(CLOSEPARENTH);
        }

        StringBuffer ipBuffer = new StringBuffer();
        collection = getClientIP();
        if (collection != null && !collection.isEmpty()) {
            iter = collection.iterator();
            if (iter.hasNext()) {
                value = (String) iter.next();
                ipBuffer.append(IP).append(EQ).append(QUOTE).append(value)
                        .append(QUOTE);
            }
            while (iter.hasNext()) {
                value = (String) iter.next();
                ipBuffer.append(SPACE).append(OR).append(SPACE);
                ipBuffer.append(IP).append(EQ).append(QUOTE).append(value)
                        .append(QUOTE);
            }
        }

        tempBuffer.setLength(0);
        collection = getClientHostNames();
        if (collection != null && !collection.isEmpty()) {
            iter = collection.iterator();
            if (iter.hasNext()) {
                value = (String) iter.next();
                tempBuffer.append(DNS).append(EQ).append(QUOTE).append(value)
                        .append(QUOTE);
            }
            while (iter.hasNext()) {
                value = (String) iter.next();
                tempBuffer.append(SPACE).append(OR).append(SPACE);
                tempBuffer.append(DNS).append(EQ).append(QUOTE).append(value)
                        .append(QUOTE);
            }
        }
        if (ipBuffer.length() != 0) {
            ipBuffer.append(SPACE).append(OR).append(SPACE).append(tempBuffer);
        } else {
            ipBuffer.append(tempBuffer);
        }

        if (ipBuffer.length() != 0) {
            if (bindRule.length() > 0) {
                bindRule.append(NEWLINE).append(SPACE).append(AND);
            }
            bindRule.append(SPACE).append(OPENPARENTH).append(ipBuffer).append(
                    CLOSEPARENTH);
        }

        tempBuffer.setLength(0);
        collection = getDaysOfWeek();
        if (collection != null && !collection.isEmpty()) {
            iter = collection.iterator();
            if (iter.hasNext()) {
                value = (String) iter.next();
                tempBuffer.append(DAYOFWEEK).append(EQ).append(QUOTE).append(
                        value);
            }
            while (iter.hasNext()) {
                value = (String) iter.next();
                tempBuffer.append(COMMA).append(SPACE).append(value);
            }
            tempBuffer.append(QUOTE).append(SPACE);
        }
        if (tempBuffer.length() != 0) {
            if (bindRule.length() > 0) {
                bindRule.append(NEWLINE).append(SPACE).append(AND)
                        .append(SPACE);
            }
            bindRule.append(OPENPARENTH).append(tempBuffer)
                    .append(CLOSEPARENTH);
        }

        tempBuffer.setLength(0);
        collection = getTimesOfDay();
        if (collection != null && !collection.isEmpty()) {
            iter = collection.iterator();
            if (iter.hasNext()) {
                value = (String) iter.next();
                tempBuffer.append(TIMEOFDAY).append(GE).append(QUOTE).append(
                        value).append(QUOTE);
            }
            if (iter.hasNext()) {
                value = (String) iter.next();
                tempBuffer.append(SPACE).append(AND).append(SPACE);
                tempBuffer.append(TIMEOFDAY).append(LE).append(QUOTE).append(
                        value).append(QUOTE);
            }
            while (iter.hasNext()) {
                value = (String) iter.next();
                tempBuffer.append(SPACE).append(OR).append(SPACE);
                tempBuffer.append(TIMEOFDAY).append(GE).append(QUOTE).append(
                        value).append(QUOTE);
                if (iter.hasNext()) {
                    value = (String) iter.next();
                    tempBuffer.append(SPACE).append(AND).append(SPACE);
                    tempBuffer.append(TIMEOFDAY).append(LE).append(QUOTE)
                            .append(value).append(QUOTE);
                }
            }
        }
        if (tempBuffer.length() != 0) {
            if (bindRule.length() > 0) {
                bindRule.append(NEWLINE).append(SPACE).append(AND)
                        .append(SPACE);
            }
            bindRule.append(OPENPARENTH).append(tempBuffer)
                    .append(CLOSEPARENTH);
        }

        if (bindRule.length() != 0) {
            aci.append(bindRule);
        }
        aci.append(SEMICOLON).append(SPACE).append(CLOSEPARENTH);
        return aci.toString().replace('\n', ' ');
    }

    /**
     * Converts aci text to ACI
     * 
     * @param aciText
     *            value of aci attribute, typically read from directoy server
     * @return the converted ACI
     * @supported.api
     */
    public static ACI valueOf(String aciText) throws ACIParseException {
        return ACIParser.parseACI(aciText);
    }

    /**
     * Set the user DN attributes
     * @supported.api
     */
    public void setUserDNAttrs(Collection values) {
        _userDNAttrs = values;
    }

    /**
     * Get the DN attributes.
     * @supported.api
     */
    public Collection getUserDNAttrs() {
        return _userDNAttrs;
    }

    /**
     * Set the group DN attributes.
     * @supported.api
     */
    public void setGroupDNAttrs(Collection values) {
        _groupDNAttrs = values;
    }

    /**
     * Get the group DN attributes.
     * @supported.api
     */
    Collection getGroupDNAttrs() {
        return _groupDNAttrs;
    }

    /**
     * Set the user attributes.
     * @supported.api
     */
    public void setUserAttrs(Collection values) {
        _userAttrs = values;
    }

    /**
     * Get the user Attributes.
     * @supported.api
     */
    public Collection getUserAttrs() {
        return _userAttrs;
    }

    /**
     * Set the ACI text.
     * @supported.api
     */
    public void setACIText(String aciText) {
        _aciText = aciText;
    }

    /**
     * Get the ACI text.
     * @supported.api
     */
    public String getACIText() {
        return _aciText;
    }

    /**
     * Set the Access Control Rule.
     * @supported.api
     */
    public void setACR(ACR acr) {
        setVersion(acr.getVersion());
        setName(acr.getName());
        setPermissions(acr.getPermissions());
        BindRule br = acr.getBindRule();
        setUsers(br.getUsers());
        setGroups(br.getGroups());
        setRoles(br.getRoles());
        setUserDNAttrs(br.getUserDNAttrs());
        setGroupDNAttrs(br.getGroupDNAttrs());
        setUserAttrs(br.getUserAttrs());
        setAuthMethods(br.getAuthMethods());
        setClientIP(br.getClientIP());
        setClientHostNames(br.getClientHostNames());
        setDaysOfWeek(br.getDaysOfWeek());
        setTimesOfDay(br.getTimesOfDay());
    }

    /**
     * Set the version number of the ACI.
     * @supported.api
     */
    public void setVersion(String version) {
        _version = version;
    }

    /**
     * Get the version number.
     * @supported.api
     */
    public String getVersion() {
        return _version;
    }

    private String _target;

    private String _targetFilter;

    private String _targetAttrFilters;

    private QualifiedCollection _targetAttributes;

    private String _name = "Unnamed";

    private String _version = "3.0";

    private QualifiedCollection _permissions;

    private Collection _users;

    private Collection _groups;

    private Collection _roles;

    private Collection _clientIP;

    private Collection _clientHostNames;

    private Collection _timesOfDay;

    private Collection _daysOfWeek;

    private Collection _authMethods;

    private Collection _userDNAttrs;

    private Collection _groupDNAttrs;

    private Collection _userAttrs;

    private String _aciText = "";
}

/**
 * Support class with utility methods used to parse the value of aci attibute
 * read from the directory server to ACI object
 */
class ACIParser {

    static ACI parseACI(String aciText) throws ACIParseException {
        ACI aci = new ACI();
        aci.setACIText(aciText);
        if (aciText == null) {
            throw new ACIParseException("Malformed aci");
        } else {
            aciText = aciText.trim();
            int length = aciText.length();
            if (aciText.length() == 0) {
                throw new ACIParseException("Malformed aci:aci is blank");
            } else if ((aciText.charAt(0) != '(')
                    || (aciText.charAt(length - 1) != ')')) {
                throw new ACIParseException(
                        "Malformed aci: aci not enclosed in parenthesis");
            } else {
                ArrayList topLevelSubExpressions = getSubExpressions(aciText);
                if (topLevelSubExpressions.size() > 5) {
                    throw new ACIParseException(
                            "Malformed aci: more than 5 toplevel " +
                            "subexpressions");
                }
                for (int i = 0; i < topLevelSubExpressions.size(); i++) {
                    String subExpression = (String) topLevelSubExpressions
                            .get(i);
                    if (subExpression.length() < 6) {
                        throw new ACIParseException(
                                "Malformed aci:too short to be valid");
                    }
                    String lcSubExpression = subExpression.substring(1)
                            .toLowerCase().trim();
                    if (lcSubExpression.indexOf(ACI.TARGET) == 0) {
                        ACITargetExpression aciTargetExpression = 
                            ACITargetExpression.valueOf(subExpression);
                        // System.out.println(aciTargetExpression);
                        if (aciTargetExpression.getKeyword().equals(ACI.TARGET))
                        {
                            if (aciTargetExpression.getOperator()
                                    .equals(ACI.EQ)) {
                                aci.setTarget(aciTargetExpression.getValue());
                            } else {
                                throw new ACIParseException(
                                        "Unsupported operator for : "
                                                + ACI.TARGET);
                            }
                        } else if (aciTargetExpression.getKeyword().equals(
                                ACI.TARGETFILTER)) {
                            if (aciTargetExpression.getOperator()
                                    .equals(ACI.EQ)) {
                                aci.setTargetFilter(aciTargetExpression
                                        .getValue());
                            } else {
                                throw new ACIParseException(
                                        "Unsupported operator for : "
                                                + ACI.TARGETFILTER);
                            }
                        } else if (aciTargetExpression.getKeyword().equals(
                                ACI.TARGETATTRFILTERS)) {
                            if (aciTargetExpression.getOperator()
                                    .equals(ACI.EQ)) {
                                aci.setTargetAttrFilters(aciTargetExpression
                                        .getValue());
                            } else {
                                throw new ACIParseException(
                                        "Unsupported operator for : "
                                                + ACI.TARGETATTRFILTERS);
                            }
                        } else if (aciTargetExpression.getKeyword().equals(
                                ACI.TARGETATTR)) {
                            boolean exclusive = false;
                            QualifiedCollection qc = null;
                            Collection collection = null;
                            if (aciTargetExpression.getOperator()
                                    .equals(ACI.EQ)) {
                                exclusive = false;
                                collection = getTokens(aciTargetExpression
                                        .getValue(), ACI.OR_PIPE);
                                qc = new QualifiedCollection(collection,
                                        exclusive);
                                aci.setTargetAttributes(qc);
                            } else if (aciTargetExpression.getOperator()
                                    .equals(ACI.NE)) {
                                exclusive = true;
                                collection = getTokens(aciTargetExpression
                                        .getValue(), ACI.OR_PIPE);
                                qc = new QualifiedCollection(collection,
                                        exclusive);
                                aci.setTargetAttributes(qc);
                            } else {
                                throw new ACIParseException(
                                        "Unsupported operator for : "
                                                + ACI.TARGETATTR);
                            } // check for targetattr complete
                        } else {
                            throw new ACIParseException(
                                    "Unsupported keyword : "
                                            + aciTargetExpression.getKeyword());
                        }// check for target* complete
                    } else if (lcSubExpression.indexOf(ACI.VERSION) == 0) {
                        ACR acr = ACRParser.parse(subExpression);
                        aci.setACR(acr);
                    } else {
                        throw new ACIParseException(
                                "Malformed aci:invalid toplevel subexpression");
                    }
                }
            }
        }
        if (aci.getPermissions() == null) {
            throw new ACIParseException("permissions not defined");
        }
        return aci;
    }

    static ArrayList getSubExpressions(String text) throws ACIParseException {
        ArrayList subExpressions = new ArrayList();
        text = text.trim();
        int length = text.length();
        if (length <= 0) {
            return subExpressions;
        } else if ((text.charAt(0) != '(') || (text.charAt(length - 1) != ')'))
        {
            throw new ACIParseException("Unmatched parenthesis");
        }
        boolean quoted = false;
        int parenthCount = 0;
        int i = 0;
        for (; i < length; i++) {
            if (text.charAt(i) == '\"') {
                quoted = !quoted;
            } else if (!quoted && (text.charAt(i) == '(')) {
                parenthCount++;
            } else if (!quoted && (text.charAt(i) == ')')) {
                parenthCount--;
            }
            if (parenthCount == 0)
                break;
        }
        if (parenthCount != 0) {
            throw new ACIParseException("Unmatched \" or parenthesis ");
        }
        subExpressions.add(text.substring(0, i + 1));
        if ((i + 1) < length) {
            subExpressions.addAll(getSubExpressions(text.substring(i + 1)));
        }
        return subExpressions;
    }

    static Collection getTokens(String text, String separator) {
        int index = 0;
        int startIndex = 0;
        int tokenSize = separator.length();
        ArrayList tokens = new ArrayList();
        while ((index = text.indexOf(separator, startIndex)) != -1) {
            tokens.add(text.substring(startIndex, index).trim());
            startIndex = index + tokenSize;
        }
        tokens.add(text.substring(startIndex));
        return tokens;
    }
}

/**
 * Class to represent the expressions for target, targetattr and targetfilter
 */
class ACITargetExpression {
    private String _keyword;

    private String _operator;

    private String _value;

    ACITargetExpression(String keyword, String operator, String value) {
        _keyword = keyword.toLowerCase();
        _operator = operator;
        _value = value;
    }

    String getKeyword() {
        return _keyword;
    }

    String getOperator() {
        return _operator;
    }

    String getValue() {
        return _value;
    }

    static ACITargetExpression valueOf(String text) throws ACIParseException {
        String keyword = null;
        String operator = null;
        String value = null;
        int opIndex = text.indexOf(ACI.EQ);
        if (opIndex <= 0) {
            throw new ACIParseException("Malformed aci");
        } else if (text.charAt(opIndex - 1) == '!') {
            opIndex--;
            operator = ACI.NE;
        } else {
            operator = ACI.EQ;
        }
        keyword = text.substring(1, opIndex).trim();
        value = text.substring(opIndex + operator.length(), text.length() - 1)
                .trim();
        value = trimSurroundingQuotes(value);
        return new ACITargetExpression(keyword, operator, value);
    }

    /**
     * Returns the string representation of ACITargetExpression
     *
     * @supported.api
     */
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(_keyword).append(":").append(_operator).append(":").append(_value);
        return sb.toString();
    }

    static String trimSurroundingQuotes(String str) {
        if (str != null && str.length() > 1 && (str.charAt(0) == '"')
                && (str.charAt(str.length() - 1) == '"')) {
            str = str.substring(1, str.length() - 1);
        }
        return str;
    }
}

/**
 * Class with utility methods to parse version, aci name and acr of the aci
 */
class ACRParser {
    static ACR parse(String text) throws ACIParseException {
        // get the version
        ACR acr = new ACR();
        if (text.length() < 6) {
            throw new ACIParseException("Malformed aci");
        }
        text = text.substring(1, text.length() - 1).trim();
        if (text.charAt(text.length() - 1) != ';') {
            throw new ACIParseException("Malformed aci");
        }

        String lcText = text.toLowerCase();
        int colonIndex = 0;
        if (lcText.indexOf(ACI.VERSION) == 0) {
            text = text.substring((ACI.VERSION).length()).trim();
            colonIndex = text.indexOf(ACI.SEMICOLON);
            if ((colonIndex < 0) || (colonIndex == (text.length() - 1))) {
                throw new ACIParseException("Malformed aci");
            }
            String version = text.substring(0, colonIndex).trim();
            acr.setVersion(version);
            text = text.substring(colonIndex + 1);
        } else {
            throw new ACIParseException("Malformed aci");
        }

        text = text.trim();
        lcText = text.toLowerCase();
        if ((lcText.indexOf(ACI.ACL) == 0) || (lcText.indexOf(ACI.ACI) == 0)) {
            text = text.substring((ACI.ACL).length()).trim();
            colonIndex = text.indexOf(";");
            if ((colonIndex < 0) || (colonIndex == (text.length() - 1))) {
                throw new ACIParseException("Malformed aci");
            }
            String aclName = text.substring(0, colonIndex).trim();
            int aclNameLength = aclName.length();
            if ((aclNameLength < 3) || aclName.charAt(0) != '"'
                    || aclName.charAt(aclNameLength - 1) != '"') {
                throw new ACIParseException("Malformed aci");
            }
            aclName = aclName.substring(1, aclNameLength - 1);
            acr.setName(aclName);
            text = text.substring(colonIndex + 1);
        } else {
            throw new ACIParseException("Malformed aci");
        }

        boolean allowed = false;
        boolean denied = false;
        String permissionMode = null;
        text = text.trim();
        lcText = text.toLowerCase();
        if (lcText.indexOf(ACI.ALLOW) == 0) {
            allowed = true;
            permissionMode = ACI.ALLOW;
        } else if (lcText.indexOf(ACI.DENY) == 0) {
            denied = true;
            permissionMode = ACI.DENY;
        }

        if (allowed || denied) {
            colonIndex = text.indexOf(ACI.SEMICOLON);
            if ((colonIndex < 0) || (colonIndex != (text.length() - 1))) {
                throw new ACIParseException("Malformed aci");
            }
            text = text.substring(0, colonIndex);
            text = text.substring(permissionMode.length()).trim();
            int parenthIndex = text.indexOf(ACI.CLOSEPARENTH);
            String permissionExpression = text.substring(0, parenthIndex + 1);
            text = text.substring(parenthIndex + 1).trim();
            int peLength = permissionExpression.length();
            if ((peLength < 3) || (permissionExpression.charAt(0) != '(')
                    || (permissionExpression.charAt(peLength - 1) != ')')) {
                throw new ACIParseException(
                        "Malformed aci-invlaid permission expression : "
                                + permissionExpression);
            }
            permissionExpression = permissionExpression.substring(1,
                    peLength - 1);
            Collection permissions = ACIParser.getTokens(permissionExpression,
                    ACI.COMMA);
            QualifiedCollection qc = new QualifiedCollection(permissions,
                    denied);
            acr.setPermissions(qc);
        } else {
            throw new ACIParseException("Malformed aci");
        }

        BindRuleTokenizer tokenizer = new BindRuleTokenizer(text);
        BindRuleBuilder brBuilder = new BindRuleBuilder();
        String token = null;
        while ((token = tokenizer.nextToken()) != null) {
            // System.out.println( "token : " + token);
            brBuilder.addToken(token);
        }
        acr.setBindRule(brBuilder.getBindRule());
        return acr;
    }

}

/**
 * Class to represent the version, aci name and acr of aci
 */
class ACR {
    String _version;

    String _name;

    QualifiedCollection _permissions;

    BindRule _bindRule;

    ACR() {
    }

    void setVersion(String version) {
        _version = version;
    }

    String getVersion() {
        return _version;
    }

    void setName(String name) {
        _name = name;
    }

    String getName() {
        return _name;
    }

    void setPermissions(QualifiedCollection permissions) {
        _permissions = permissions;
    }

    QualifiedCollection getPermissions() {
        return _permissions;
    }

    void setBindRule(BindRule bindRule) {
        _bindRule = bindRule;
    }

    BindRule getBindRule() {
        return _bindRule;
    }
}

class BindRule {

    Collection _users;

    Collection _groups;

    Collection _roles;

    Collection _authMethods;

    Collection _clientIP;

    Collection _clientHostNames;

    Collection _timesOfDay;

    String _todOp;

    Collection _daysOfWeek;

    String _previousTodOperator;

    Collection _userDNAttrs;

    Collection _groupDNAttrs;

    Collection _userAttrs;

    BindRule() {
    }

    void addUsers(String value) throws ACIParseException {
        if (_users == null) {
            _users = new ArrayList();
        }
        Iterator c = ACIParser.getTokens(value, ACI.OR_PIPE).iterator();
        while (c.hasNext()) {
            String str = ((String) c.next()).trim();
            if (str.indexOf(ACI.LDAP_PREFIX) == 0) {
                _users.add(str.substring((ACI.LDAP_PREFIX).length()));
            } else if (str.toLowerCase().indexOf(ACI.LDAP_PREFIX) == 0) {
                _users.add(str.substring((ACI.LDAP_PREFIX).length()));
            } else {
                throw new ACIParseException("Malformed userDN : " + str);
            }
        }
    }

    Collection getUsers() {
        return _users;
    }

    void addGroups(String value) throws ACIParseException {
        if (_groups == null) {
            _groups = new ArrayList();
        }
        Iterator c = ACIParser.getTokens(value, ACI.OR_PIPE).iterator();
        while (c.hasNext()) {
            String str = (String) c.next();
            if (str.indexOf(ACI.LDAP_PREFIX) == 0) {
                _groups.add(str.substring((ACI.LDAP_PREFIX).length()));
            } else if (str.toLowerCase().indexOf(ACI.LDAP_PREFIX) == 0) {
                _groups.add(str.substring((ACI.LDAP_PREFIX).length()));
            } else {
                throw new ACIParseException("Malformed groupDN : " + value);
            }
        }
    }

    Collection getGroups() {
        return _groups;
    }

    void addRoles(String value) throws ACIParseException {
        if (_roles == null) {
            _roles = new ArrayList();
        }
        Iterator c = ACIParser.getTokens(value, ACI.OR_PIPE).iterator();
        while (c.hasNext()) {
            String str = (String) c.next();
            if (str.indexOf(ACI.LDAP_PREFIX) == 0) {
                _roles.add(str.substring((ACI.LDAP_PREFIX).length()));
            } else if (str.toLowerCase().indexOf(ACI.LDAP_PREFIX) == 0) {
                _roles.add(str.substring((ACI.LDAP_PREFIX).length()));
            } else {
                throw new ACIParseException("Malformed roleDN : " + value);
            }
        }
    }

    Collection getRoles() {
        return _roles;
    }

    void addUserDNAttr(String value) {
        if (_userDNAttrs == null) {
            _userDNAttrs = new ArrayList();
        }
        _userDNAttrs.add(value);
    }

    Collection getUserDNAttrs() {
        return _userDNAttrs;
    }

    void addGroupDNAttr(String value) {
        if (_groupDNAttrs == null) {
            _groupDNAttrs = new ArrayList();
        }
        _groupDNAttrs.add(value);
    }

    Collection getGroupDNAttrs() {
        return _groupDNAttrs;
    }

    void addAuthMethod(String value) {
        if (_authMethods == null) {
            _authMethods = new ArrayList();
        }
        _authMethods.add(value);
    }

    Collection getAuthMethods() {
        return _authMethods;
    }

    void addClientIP(String value) {
        if (_clientIP == null) {
            _clientIP = new ArrayList();
        }
        _clientIP.add(value);
    }

    Collection getClientIP() {
        return _clientIP;
    }

    void addClientHostName(String value) {
        if (_clientHostNames == null) {
            _clientHostNames = new ArrayList();
        }
        _clientHostNames.add(value);
    }

    Collection getClientHostNames() {
        return _clientHostNames;
    }

    void addTimeOfDay(String value, String operator) throws ACIParseException {
        if (_timesOfDay == null) {
            _timesOfDay = new ArrayList();
        }
        if (!operator.equals(ACI.GE) && !operator.equals(ACI.LE)) {
            throw new ACIParseException("Illegal operator for timeofday : "
                    + operator);
        }
        if ((_previousTodOperator == null) && (!operator.equals(ACI.GE))) {
            throw new ACIParseException(
                    "Illegal first operator for timeofday : " + operator);
        } else if (operator.equals(_previousTodOperator)) {
            throw new ACIParseException(
                    "Illegal operator sequence for timeofday : " + operator);
        }
        _timesOfDay.add(value);
        _previousTodOperator = operator;
    }

    Collection getTimesOfDay() {
        return _timesOfDay;
    }

    void addDaysOfWeek(String value) {
        if (_daysOfWeek == null) {
            _daysOfWeek = new ArrayList();
        }
        _daysOfWeek.addAll(ACIParser.getTokens(value, ACI.COMMA));
    }

    Collection getDaysOfWeek() {
        return _daysOfWeek;
    }

    void addUserAttr(String value) {
        if (_userAttrs == null) {
            _userAttrs = new ArrayList();
        }
        _userAttrs.add(value);
    }

    Collection getUserAttrs() {
        return _userAttrs;
    }

}

class BindRuleBuilder {
    String _previousToken;

    String _previousTokenType = "";

    String _previousKeyword;

    String _previousKeywordSet;

    String _previousExpressionConnector;

    String _token;

    String _tokenType; // keyword, opearotr, value, expressionconnector,
                        // openparenth, closeparenth

    String _keyword;

    String _keywordSet;

    ArrayList _doneKeywordSets = new ArrayList();

    String _operator;

    String _value;

    String _expressionConnector;

    int _parenthCount;

    int _keywordCount;

    int _previousKeywordCount;

    BindRule br = new BindRule();

    void addToken(String token) throws ACIParseException {
        _tokenType = getTokenType(token);
        if (_previousTokenType.equals(ACI.KEYWORD)
                && !_tokenType.equals(ACI.OPERATOR)) {
            throw new ACIParseException("keyword not followed by operator : "
                    + _previousToken);
        }
        if (_previousTokenType.equals(ACI.OPERATOR)
                && !_tokenType.equals(ACI.VALUE)) {
            throw new ACIParseException("operator not followed by value");
        }
        if (_previousTokenType.equals(ACI.VALUE)
                && !_tokenType.equals(ACI.EXPRESSIONCONNECTOR)
                && !_tokenType.equals(ACI.OPENPARENTH)
                && !_tokenType.equals(ACI.CLOSEPARENTH)) {
            throw new ACIParseException("value not followed by connector : "
                    + _previousToken);
        }
        if (_previousTokenType.equals(ACI.EXPRESSIONCONNECTOR)
                && !_tokenType.equals(ACI.KEYWORD)
                && !_tokenType.equals(ACI.OPENPARENTH)) {
            throw new ACIParseException(
                    "expressionconnector not followed by keyword : " + token);
        }

        if (_tokenType.equals(ACI.OPENPARENTH)) {
            _parenthCount++;
        } else if (_tokenType.equals(ACI.CLOSEPARENTH)) {
            _parenthCount--;
        } else if (_tokenType.equals(ACI.KEYWORD)) {
            // if ( _keywordCount == 0 ) {
            // _keywordCount = 1;
            // }
            _keyword = token.toLowerCase();
            _keywordSet = getKeywordSet(token);
            if (_doneKeywordSets.contains(_keywordSet)) {
                throw new ACIParseException(
                        "keywords from diffrent sets overlap");
            }
            if ((_previousKeywordSet != null)
                    && !_keywordSet.equals(_previousKeywordSet)) {
                _doneKeywordSets.add(_previousKeywordSet);
                _keywordCount = 0; // 1 ;
                if (!_expressionConnector.equals(ACI.AND)) {
                    throw new ACIParseException(
                            "sets of of keywords have to be "
                                    + " connected  by logical and ");
                }
            } else {
                _keywordCount++;
                if ((_keywordCount > 1) && !_keywordSet.equals(ACI.TOD_SET)
                        && (!(ACI.OR).equals(_expressionConnector))) { 
                    // || (_parenthCount < 1) )) {
                    throw new ACIParseException(
                            "keywords from the same set has to "
                                    + " be connected  by logical OR : " 
                                    + token);
                    // + " and enclosed in parenthesis ");
                }
            }
            _previousKeyword = _keyword;
            _previousKeywordSet = _keywordSet;
            _previousKeywordCount = _keywordCount;
        } else if (_tokenType.equals(ACI.OPERATOR)) {
            _operator = token.toLowerCase();
        } else if (_tokenType.equals(ACI.VALUE)) {
            _value = token.substring(1, token.length() - 1);
            if (_keyword == null) {
                throw new ACIParseException("keyword is null");
            }
            addParameter(_keyword, _operator, _value);
            // _operator = null;
            _value = null;
        } else if (_tokenType.equals(ACI.EXPRESSIONCONNECTOR)) {
            _expressionConnector = token.toLowerCase();
            if (_expressionConnector.equals(ACI.AND)
                    && !(ACI.TIMEOFDAY).equals(_keyword)) {
                if (_parenthCount != 0) {
                    throw new ACIParseException(
                            "Can not enclose keywords from "
                                  + " different sets in the same parenthesis");
                }
                if ((_previousKeywordCount > 1)
                        && !(ACI.CLOSEPARENTH).equals(_previousTokenType)) {
                    throw new ACIParseException("preceding set of expressions "
                            + " not enclosed in parenthesis");
                }
            }
        } else if (_tokenType.equals(ACI.OR_PIPE)) {
        } else {
            throw new ACIParseException("Unknown token type for : " + token);
        }
        _previousTokenType = _tokenType;
    }

    String getKeywordSet(String keyword) throws ACIParseException {
        String keywordSet = null;
        if (keyword.equals(ACI.USERDN) || keyword.equals(ACI.USERDNATTR)
                || keyword.equals(ACI.GROUPDN)
                || keyword.equals(ACI.GROUPDNATTR)
                || keyword.equals(ACI.ROLEDN) || keyword.equals(ACI.USERATTR)) {
            keywordSet = ACI.PRINCIPAL_SET;
        } else if (keyword.equals(ACI.IP) || keyword.equals(ACI.DNS)) {
            keywordSet = ACI.IP_SET;
        } else if (keyword.equals(ACI.AUTHMETHOD)) {
            keywordSet = ACI.AUTHMETHOD_SET;
        } else if (keyword.equals(ACI.TIMEOFDAY)) {
            keywordSet = ACI.TOD_SET;
        } else if (keyword.equals(ACI.DAYOFWEEK)) {
            keywordSet = ACI.DOW_SET;
        } else {
            throw new ACIParseException("can not determine keyword set for : "
                    + keyword);
        }
        return keywordSet;
    }

    String getTokenType(String token) throws ACIParseException {
        token = token.toLowerCase();
        String tokenType = null;
        if ((token.charAt(0) == '"')
                && (token.charAt(token.length() - 1) == '"')) {
            tokenType = ACI.VALUE;
        } else if (token.equals(ACI.USERDN) || token.equals(ACI.USERDNATTR)
                || token.equals(ACI.GROUPDN) || token.equals(ACI.GROUPDNATTR)
                || token.equals(ACI.ROLEDN) || token.equals(ACI.USERATTR)
                || token.equals(ACI.IP) || token.equals(ACI.DNS)
                || token.equals(ACI.AUTHMETHOD) || token.equals(ACI.TIMEOFDAY)
                || token.equals(ACI.DAYOFWEEK)) {
            tokenType = ACI.KEYWORD;
        } else if (token.equals(ACI.EQ) || token.equals(ACI.NE)
                || token.equals(ACI.LT) || token.equals(ACI.LE)
                || token.equals(ACI.GT) || token.equals(ACI.GE)) {
            tokenType = ACI.OPERATOR;
        } else if (token.equals(ACI.AND) || token.equals(ACI.OR)) {
            tokenType = ACI.EXPRESSIONCONNECTOR;
        } else if (token.equals(ACI.OPENPARENTH)) {
            tokenType = ACI.OPENPARENTH;
        } else if (token.equals(ACI.CLOSEPARENTH)) {
            tokenType = ACI.CLOSEPARENTH;
        } else if (token.equals(ACI.OR_PIPE)) {
            tokenType = ACI.OR_PIPE;
        } else {
            throw new ACIParseException("tokentype unknown for " + token);
        }
        return tokenType;
    }

    void addParameter(String keyword, String operator, String value)
            throws ACIParseException {
        if (!keyword.equals(ACI.TIMEOFDAY) && !operator.equals(ACI.EQ)) {
            throw new ACIParseException(" keyword " + keyword
                    + " does not allow operator " + operator);
        }
        if (keyword.equals(ACI.TIMEOFDAY) && operator.equals(ACI.GE)
                && _keywordCount > 1 && !(ACI.OR).equals(_expressionConnector))
        {
            throw new ACIParseException(" illegal operator for timeofday : "
                    + operator);
        }
        if (keyword.equals(ACI.TIMEOFDAY) && operator.equals(ACI.LE)
                && !(ACI.AND).equals(_expressionConnector)) {
            throw new ACIParseException(" illegal operator for timeofday : "
                    + operator);
        }
        if (keyword.equals(ACI.USERDN)) {
            br.addUsers(value);
        } else if (keyword.equals(ACI.USERDNATTR)) {
            br.addUserDNAttr(value);
        } else if (keyword.equals(ACI.GROUPDN)) {
            br.addGroups(value);
        } else if (keyword.equals(ACI.GROUPDNATTR)) {
            br.addGroupDNAttr(value);
        } else if (keyword.equals(ACI.ROLEDN)) {
            br.addRoles(value);
        } else if (keyword.equals(ACI.USERATTR)) {
            br.addUserAttr(value);
        } else if (keyword.equals(ACI.AUTHMETHOD)) {
            br.addAuthMethod(value);
        } else if (keyword.equals(ACI.IP)) {
            br.addClientIP(value);
        } else if (keyword.equals(ACI.DNS)) {
            br.addClientHostName(value);
        } else if (keyword.equals(ACI.TIMEOFDAY)) {
            br.addTimeOfDay(value, operator);
        } else if (keyword.equals(ACI.DAYOFWEEK)) {
            br.addDaysOfWeek(value);
        } else {
            throw new ACIParseException("Unknown keyword : " + keyword);
        }
    }

    BindRule getBindRule() {
        return br;
    }

    /**
     * Returns the string representation of the Bind rule.
     * @supported.api
     */
    public String toString() {
        StringBuilder bindRule = new StringBuilder();
        return bindRule.toString();
    }

}

class BindRuleTokenizer {

    String _text;

    int _textLength;

    int _startIndex = 0;

    int _currentIndex;

    BindRuleTokenizer(String text) {
        _text = text;
        _textLength = text.length();
        _currentIndex = 0;
    }

    String nextToken() {
        StringBuilder token = new StringBuilder();
        if (_currentIndex < _textLength) {
            for (; _currentIndex < _textLength; _currentIndex++) {
                char c = _text.charAt(_currentIndex);
                if (c == '\n' || c == '\r' || c == ' ') {
                    if (token.length() == 0) {
                        continue;
                    } else {
                        _currentIndex++;
                        break;
                    }
                } else if (c == '"') {
                    if (token.length() == 0) {
                        token.append(c);
                        while (_currentIndex < (_textLength - 1)) {
                            _currentIndex++;
                            c = _text.charAt(_currentIndex);
                            token.append(c);
                            if (c == '"') {
                                _currentIndex++;
                                break;
                            }
                        }
                    }
                    break;
                } else if (c == '(' || c == ')') {
                    if (token.length() == 0) {
                        token.append(c);
                        _currentIndex++;
                    }
                    break;
                } else if (c == '<' || c == '>' || c == '!') {
                    if (token.length() == 0) {
                        token.append(c);
                        _currentIndex++;
                        if (_currentIndex < (_textLength)
                                && ((c = _text.charAt(_currentIndex)) == '=')) {
                            token.append(c);
                            _currentIndex++;
                        }
                    }
                    break;
                } else if (c == '=') {
                    if (token.length() == 0) {
                        token.append(c);
                        _currentIndex++;
                    }
                    break;
                } else {
                    token.append(c);
                }
            }
        }
        return (token.length() != 0) ? token.toString() : null;
    }
}
