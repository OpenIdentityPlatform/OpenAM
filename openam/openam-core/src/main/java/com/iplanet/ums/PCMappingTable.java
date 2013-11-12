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
 * $Id: PCMappingTable.java,v 1.3 2009/01/28 05:34:50 ww203982 Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.ums;

import java.util.Enumeration;

import com.sun.identity.shared.ldap.LDAPDN;

import com.iplanet.services.ldap.Attr;
import com.iplanet.services.ldap.AttrSet;

/**
 * Package class to manage the mapping table between user and people container
 * of an organization. Right now, we store the mapping table as an entry right
 * under the organization.
 */
class PCMappingTable implements java.io.Serializable {

    protected PCMappingTable(java.security.Principal principal, 
            Guid mappingGuid) throws UMSException {
        _principal = principal;
        _mappingGuid = mappingGuid;
    }

    /**
     * Gets the PCMappingTable associated with the given organization
     * 
     * @param org
     *            organization to be managed
     * @return PCMappingTable associated with the given organization
     * @exception UMSException
     *                Failure
     */
    public static PCMappingTable getPCMappingTable(Organization org)
            throws UMSException {

        PersistentObject po = null;
        SearchResults results = org.getChildren("ou=" + MAPPINGTABLE_ENTRYNAME,
                null);
        if (results.hasMoreElements()) {
            po = results.next();
        } else {
            // Create an entry to store the mapping table if it is
            // not already existed.
            // Right now, we store the mapping table as an entry right under
            // the organization.
            po = new PersistentObject();

            po.setAttribute(new Attr("objectclass", new String[] { "top",
                    "extensibleobject" }));
            po.setAttribute(new Attr("ou", MAPPINGTABLE_ENTRYNAME));

            // po.save( org.getPrincipal(), "ou", org.getGuid() );
            Guid guid = new Guid("ou=" + MAPPINGTABLE_ENTRYNAME + ","
                    + org.getGuid().getDn());
            po.setGuid(guid);
            org.addChild(po);
        }
        results.abandon();

        PCMappingTable mt = new PCMappingTable(
                                org.getPrincipal(), po.getGuid());
        return mt;
    }

    /**
     * Gets People Container associated with the user
     * 
     * @param user
     *            user object to look up
     * @return guid identifying People Container associated with the user, null
     *         if no match found and default has not been set
     * @exception UMSException
     *                Failure
     */
    public String getPeopleContainer(User user) throws UMSException {
        PersistentObject po = UMSObject.getObject(_principal, _mappingGuid);
        AttrSet attrSet = po.getAttrSet();
        String defaultPC = getDefault(attrSet);

        for (int j = 0; j < ATTRNAMESTOSKIP.length; j++) {
            attrSet.remove(ATTRNAMESTOSKIP[j]);
        }

        Enumeration e1 = attrSet.getAttributes();
        while (e1.hasMoreElements()) {
            Attr attr = (Attr) e1.nextElement();
            String guid = attr.getName();
            String[] filters = attr.getStringValues();

            for (int j = 0; j < filters.length; j++) {
                AttrSet filterAttrSet = getAttrSetFromFilter(filters[j]);

                // loop through filterAttrSet and compare each one to the
                // user's AttrSet
                Enumeration e2 = filterAttrSet.getAttributes();
                while (e2.hasMoreElements()) {
                    Attr filterAttr = (Attr) e2.nextElement();
                    Attr userAttr = user.getAttribute(filterAttr.getName());
                    if (userAttr != null) {
                        String[] filterAttrValues = filterAttr
                                .getStringValues();
                        for (int i = 0; i < filterAttrValues.length; i++) {
                            if (userAttr.contains(filterAttrValues[i])) {
                                return guid;
                            }
                        }
                    }
                }
            }
        }
        return defaultPC;
    }

    /**
     * Adds rule for determining which People Container the user is supposed to
     * be in.
     * 
     * @param filter
     *            filter representation of the rule
     * @param guid
     *            guid of the People Container which the rule is applied to
     * @exception UMSException
     *                Failure
     */
    public void addRule(Guid guid, String filter) throws UMSException {
        DataLayer.getInstance().addAttributeValue(_principal, _mappingGuid,
                LDAPDN.normalize(guid.getDn()), filter);
    }

    /**
     * Removes the rule with the given filter string applying to the given
     * People Container guid.
     * 
     * @param filter
     *            filter string of the rule to be removed
     * @param guid
     *            guid of which the rule applies to
     * @exception UMSException
     *                Failure
     */
    public void removeRule(Guid guid, String filter) throws UMSException {
        DataLayer.getInstance().removeAttributeValue(_principal, _mappingGuid,
                LDAPDN.normalize(guid.getDn()), filter);
    }

    /**
     * Sets the default People Container.
     * 
     * @param guid
     *            guid of the default People Container
     * @exception UMSException
     *                Failure
     */
    public void setDefault(Guid guid) throws UMSException {
        DataLayer.getInstance().addAttributeValue(_principal, _mappingGuid,
                DEFAULT_PC_ATTRNAME, guid.getDn());
    }

    /**
     * Gets the default People Container from the given attribute set.
     * 
     * @param attrSet
     *            attribute set to get from
     * @return guid identifing the default People Container from the given
     *         attribute set.
     */
    private String getDefault(AttrSet attrSet) {
        Attr attr = attrSet.getAttribute(DEFAULT_PC_ATTRNAME);
        if (attr != null) {
            return attr.getValue();
        }
        return null;
    }

    /**
     * Gets the attribute set correspondent to the filter string. Only accepts
     * filter string with the following format:
     * 
     * <PRE>
     * 
     * &ltfilter&gt ::= &ltand&gt | &ltitem&gt &ltand&gt ::= '(' '&'
     * &ltitemlist&gt ')' &ltitemlist&gt ::= &ltitem&gt | &ltitem&gt
     * &ltitemlist&gt &ltitem&gt ::= '(' &ltattr&gt '=' &ltvalue&gt ')'
     * 
     * </PRE>
     * 
     * @param filter
     *            filter string to parse
     * @return the attribute set correspondent to the filter string
     */
    private AttrSet getAttrSetFromFilter(String filter) {
        AttrSet attrSet = new AttrSet();
        String f = filter;
        f.trim();
        if (f.startsWith("(") && f.endsWith(")")) {
            f = f.substring(1, f.length() - 1);
        }

        if (f.startsWith("|") || f.startsWith("!")) {
            // TODO: should throw an exception: invalid pc filter
            return null;
        }
        if (f.startsWith("&")) {
            int level = 0;
            int start = 0;
            int end = 0;

            for (int i = 0; i < f.length(); i++) {
                if (f.charAt(i) == '(') {
                    if (level == 0) {
                        start = i;
                    }
                    level++;
                }
                if (f.charAt(i) == ')') {
                    level--;
                    if (level == 0) {
                        end = i;
                        String subf = f.substring(start, end + 1);
                        if (subf.startsWith("(") && subf.endsWith(")")) {
                            subf = subf.substring(1, subf.length() - 1);
                        }

                        int idx = subf.indexOf('=');
                        if (idx == -1) {
                            return null;
                        }
                        String type = subf.substring(0, idx).trim();
                        String value = subf.substring(idx + 1).trim();
                        attrSet.add(new Attr(type, value));
                    }
                }
            }

        } else {
            int idx = f.indexOf('=');
            if (idx == -1) {
                return null;
            }
            String type = f.substring(0, idx).trim();
            String value = f.substring(idx + 1).trim();
            attrSet.add(new Attr(type, value));
        }

        return attrSet;
    }

    private java.security.Principal _principal;

    private Guid _mappingGuid;

    private static final String[] ATTRNAMESTOSKIP = { "ou", "objectclass",
            "default" };

    private static final String MAPPINGTABLE_ENTRYNAME = "pcmappingtable";

    private static final String DEFAULT_PC_ATTRNAME = "default";
}
