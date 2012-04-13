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
 * $Id: ComplianceServicesImpl.java,v 1.10 2009/11/20 23:52:51 ww203982 Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.am.sdk.ldap;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.sun.identity.shared.ldap.util.DN;
import com.sun.identity.shared.ldap.util.RDN;

import com.iplanet.services.ldap.Attr;
import com.iplanet.services.ldap.AttrSet;
import com.iplanet.services.ldap.ModSet;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.ums.AssignableDynamicGroup;
import com.iplanet.ums.EntryNotFoundException;
import com.iplanet.ums.Guid;
import com.iplanet.ums.ManagedRole;
import com.iplanet.ums.PersistentObject;
import com.iplanet.ums.UMSException;
import com.iplanet.ums.UMSObject;

import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;

import com.iplanet.am.sdk.AMConstants;
import com.iplanet.am.sdk.AMEntryExistsException;
import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMObject;
import com.iplanet.am.sdk.AMSDKBundle;
import com.iplanet.am.sdk.AMStoreConnection;
import com.iplanet.am.sdk.common.IComplianceServices;
import com.sun.identity.shared.debug.Debug;

/**
 * This class <code>Compliance</code> contains the functionality to support
 * iPlanet Compliant DIT. The methods of this class will be used by other
 * classes in <code>com.iplanet.am.sdk package</code>.
 * <p>
 * 
 * In order to determine if iPlanet Compliance mode is required or not, the
 * parameter <code>com.iplanet.am.compliance</code> will be verified. A value
 * of <code>true</code> for this parameter, means iPlanet Compliance mode.
 * <p>
 * 
 * NOTE: An explicit check must be performed using Compliance.
 * isIplanetCompliant() method before calling any other methods in this class.
 */
public class ComplianceServicesImpl implements AMConstants, IComplianceServices 
{
    // Map to keep role->group name mapping
    static private Map roleToGroupMap = new HashMap();

    static private Map groupToRoleMap = new HashMap();

    static private Map deletedOrg = new HashMap();

    static private String USER_STATUS_ATTRIBUTE = "inetuserstatus";

    static private String GROUP_STATUS_ATTRIBUTE = "inetgroupstatus";

    static private String ORG_STATUS_ATTRIBUTE = "inetdomainstatus";

    static private String RESOURCE_STATUS_ATTRIBUTE = "icsstatus";

    static private String DEFAULT_DELETED_ORG_FILTER = 
        "(&(sunPreferredDomain=%V)(inetDomainStatus=deleted)"
            + "(objectclass=sunManagedOrganization))";

    static private String DEFAULT_DELETED_GROUP_FILTER = 
        "(&(inetgroupstatus=deleted)(objectclass=iplanet-am-managed-group))";

    static private String DEFAULT_DELETED_USER_FILTER = 
        "(&(inetUserStatus=deleted)(objectclass=inetorgperson))";

    static private String DEFAULT_DELETED_OBJECT_FILTER = 
        "(|(objectclass=*)(objectclass=ldapsubentry))";

    static private String DEFAULT_DELETED_RESOURCE_FILTER = 
        "(&(objectclass=icsCalendarResource)(icsStatus = deleted))";

    static private String rootSuffix;

    static protected ServiceSchema gsc = null;

    static Debug debug = CommonUtils.debug;

    static SSOToken internalToken = CommonUtils.getInternalToken();

    static {
        rootSuffix = AMStoreConnection.getAMSdkBaseDN();
        if (rootSuffix == null || rootSuffix == "") {
            debug.error("com.iplanet.am.rootsuffix property value "
                    + "should not be null");
        }
    }

    public ComplianceServicesImpl() {
    }

    /**
     * Method to addAttributes to an entry
     */
    private void addAttributesToEntry(SSOToken token, String dn, 
            AttrSet attrSet) throws UMSException {
        PersistentObject po = UMSObject.getObjectHandle(token, new Guid(dn));
        int size = attrSet.size();
        for (int i = 0; i < size; i++) {
            Attr attr = attrSet.elementAt(i);
            po.modify(attr, ModSet.ADD);
        }
        po.save();
    }

    /**
     * Method to remove attributes from an entry
     */
    private void removeAttributesFromEntry(SSOToken token, String dn,
            AttrSet attrSet) throws UMSException {
        PersistentObject po = UMSObject.getObjectHandle(token, new Guid(dn));
        int size = attrSet.size();
        for (int i = 0; i < size; i++) {
            Attr attr = attrSet.elementAt(i);
            po.modify(attr, ModSet.DELETE);
        }
        po.save();
    }

    /**
     * Method which returns a group name corresponding to role DN. Returns null,
     * if no mapping found.
     */
    private String getGroupFromRoleDN(DN dn) {
        // Obtain the role name from the roleDN
        // Check if top level admin-role
        String groupName = (String) roleToGroupMap.get(dn.toString());
        if (groupName == null) { // If not, a org level admin-role
            String roleName = ((RDN) 
                    dn.getRDNs().get(0)).getValues()[0];
            groupName = (String) roleToGroupMap.get(roleName);
            if (debug.messageEnabled()) {
                debug.message("Compliance.getGroupRoleFromDN():"
                        + "Role Name: " + roleName + " Group Name: "
                        + groupName);
            }
        }
        return groupName;
    }

    /**
     * Method which returns a role name corresponding to group DN. Returns null,
     * if no mapping found.
     */
    private String getRoleFromGroupDN(DN dn) {
        // Obtain the role name from the roleDN
        // Check if top level admin-role
        String groupName = dn.explodeDN(true)[0];
        String roleName = (String) groupToRoleMap.get(groupName);
        if (debug.messageEnabled()) {
            debug.message("Compliance.getRoleFromGroupDN: "
                    + "Obtained group to role mapping: " + groupName + " ::"
                    + roleName);
        }
        if (roleName == null) { // If not, a org level admin-role
            if (debug.messageEnabled()) {
                debug.message("Compliance.getRoleFromGroupDN " + "Group: "
                        + dn.toString() + "is not an admin group");
            }
            return null;
        }
        if (debug.messageEnabled()) {
            debug.message("Compliance.getRoleFromGroupDN:" + "Role Name: "
                    + roleName + " Group Name: " + groupName);
        }
        return roleName;
    }

    /**
     * Method which verifies if the <code>roleDN</code> corresponds to an
     * admin role. If true the <code>memberOf</code> and
     * <code>adminRole</code> attributes of each member/user are set to the
     * corresponding administration <code>groupDN</code> and administration
     * <code>groupRDN</code> respectively. Each of the members/users are also
     * added to the corresponding admin group.
     * 
     * @param token
     *            single sign on token.
     * @param membersGuid
     *            Guid array of members to be operated on.
     * @param roleDN
     *            distinguished name of the role.
     * 
     * @exception AMException
     *                if unsuccessful in adding the members to the corresponding
     *                admin group. As a result of which the memberOf and
     *                adminRole attributes are also not updated.
     */
    protected void verifyAndLinkRoleToGroup(SSOToken token, Guid[] membersGuid,
            String roleDN) throws AMException {

        // Obtain the group corresponding to roleDN
        DN dn = new DN(roleDN);
        String groupName = getGroupFromRoleDN(dn);
        if (groupName != null) { // roleDN corresponds to an admin role
            String orgDN = dn.getParent().toString();
            String groupDN = NamingAttributeManager
                    .getNamingAttribute(AMObject.GROUP)
                    + "=" + groupName + ",ou=Groups," + orgDN;
            String groupRDN = NamingAttributeManager
                    .getNamingAttribute(AMObject.GROUP)
                    + "=" + groupName;
            try {
                // Add the members to corresponding group.
                AssignableDynamicGroup group = (AssignableDynamicGroup) 
                    UMSObject.getObject(token, new Guid(groupDN));
                group.addMembers(membersGuid);

                Attr attrs[] = new Attr[1];
                attrs[0] = new Attr("adminrole", groupRDN);
                AttrSet attrSet = new AttrSet(attrs);
                int numMembers = membersGuid.length;
                for (int i = 0; i < numMembers; i++) {
                    addAttributesToEntry(token, membersGuid[i].getDn(), 
                            attrSet);
                }
            } catch (EntryNotFoundException ex) {
                debug.error("Compliance.verifyAndLinkRoleToGroup: "
                        + "Admin groups are missing");
            } catch (UMSException ue) {
                debug.error("Compliance." + "verifyAndLinkRoleToGroup(): ", ue);
                throw new AMException(AMSDKBundle.getString("771"), "771");
            }
        }
    }

    /**
     * Verifies if the <code>roleDN</code> corresponds to an admin role. If
     * true the <code>memberOf</code> and <code>adminRole</code> attributes
     * of each member/user are set to null. Each of the members/users are also
     * removed to the corresponding admin group.
     * 
     * @param token
     *            single sign on token.
     * @param members
     *            Set of member distinguished name to be operated.
     * @param roleDN
     *            distinguished name of the role.
     * @exception AMException
     *                if unsuccessful in removing the members from the
     *                corresponding administrative groups and updating the
     *                <code>memberOf</code> and <code>adminRole</code>
     *                attribute values to null.
     */
    protected void verifyAndUnLinkRoleToGroup(SSOToken token, Set members,
            String roleDN) throws AMException {
        // Obtain the group corresponding to roleDN
        DN dn = new DN(roleDN);
        String groupName = getGroupFromRoleDN(dn);
        if (groupName != null) {
            String orgDN = dn.getParent().toString();
            String groupDN = NamingAttributeManager
                    .getNamingAttribute(AMObject.GROUP)
                    + "=" + groupName + ",ou=Groups," + orgDN;
            String groupRDN = NamingAttributeManager
                    .getNamingAttribute(AMObject.GROUP)
                    + "=" + groupName;
            // Delete the attributes memberOf & adminRole attribute values'
            // corresponding to this groupDN.
            Attr attrs[] = new Attr[1];
            attrs[0] = new Attr("adminrole", groupRDN);
            AttrSet attrSet = new AttrSet(attrs);
            Iterator itr = members.iterator();
            try {
                AssignableDynamicGroup group = (AssignableDynamicGroup) 
                    UMSObject.getObject(token, new Guid(groupDN));
                while (itr.hasNext()) {
                    String memberDN = (String) itr.next();
                    removeAttributesFromEntry(token, memberDN, attrSet);
                    group.removeMember(new Guid(memberDN));
                }
            } catch (EntryNotFoundException ex) {
                debug.error("Compliance.verifyAndUnLinkRoleToGroup: "
                        + "Admin groups are missing");
            } catch (UMSException ue) {
                debug.error("Compliance." + "verifyAndUnLinkRoleToGroup(): ",
                        ue);
                throw new AMException(AMSDKBundle.getString("772"), "772");
            }
        }
    }

    /**
     * Method which verifies if the <code>groupDN</code> corresponds to an
     * administrative role. If true then the members listed in 
     * <Code>membersGuid</Code> are added to the admin role.
     * 
     * @param token
     *            SSO Token
     * @param membersGuid
     *            Guid array of members to be operated on
     * @param groupDN
     *            DN of the role
     * 
     * @exception AMException
     *                if unsuccessful in adding the members to the corresponding
     *                admin group. As a result of which the memberOf and
     *                adminRole attributes are also not updated.
     */
    protected void verifyAndLinkGroupToRole(SSOToken token, Guid[] membersGuid,
            String groupDN) throws AMException {

        // Obtain the role corresponding to groupDN
        DN dn = new DN(groupDN);
        String roleName = getRoleFromGroupDN(dn);
        if (roleName != null) { // roleDN corresponds to an admin role
            String orgDN = dn.getParent().getParent().toString();
            String roleDN = NamingAttributeManager
                    .getNamingAttribute(AMObject.ROLE)
                    + "=" + roleName + "," + orgDN;
            if (debug.messageEnabled()) {
                debug.message("Compliance.verifyAndLinkGroupToRole"
                        + " Linking group: " + groupDN + " to role :" + roleDN);
            }
            try {
                // Add the members to corresponding group.
                ManagedRole role = (ManagedRole) UMSObject.getObject(token,
                        new Guid(roleDN));
                role.addMembers(membersGuid);
            } catch (EntryNotFoundException ex) {
                debug.error("Compliance.verifyAndLinkGroupToRole: Admin "
                        + "groups are missing");
            } catch (UMSException ue) {
                debug.error("Compliance.verifyAndLinkGroupToRole():", ue);
                Object args[] = { roleDN };
                throw new AMException(AMSDKBundle.getString("972", args),
                        "771", args);
            }
        }
    }

    /**
     * Method which verifies if the groupDN corresponds to an admin role. If
     * true then the <Code> members </Code> are removed from the admin role.
     * 
     * @param token Single Sign On Token.
     * @param members Set of member DNs to be operated.
     * @param groupDN Distinguished Name of the group.
     * @throws AMException if unsuccessful in removing the members from the
     *         corresponding admin groups and updating the <code>memberOf</code>
     *         and <code>adminRole</code> attribute values to null.
     */
    protected void verifyAndUnLinkGroupToRole(SSOToken token, Set members,
            String groupDN) throws AMException {

        // Obtain the group corresponding to roleDN
        DN dn = new DN(groupDN);
        String roleName = getRoleFromGroupDN(dn);
        if (roleName != null) {
            String orgDN = dn.getParent().getParent().toString();
            String roleDN = NamingAttributeManager
                    .getNamingAttribute(AMObject.ROLE)
                    + "=" + roleName + "," + orgDN;
            if (debug.messageEnabled()) {
                debug.message("Compliance.verifyAndUnlinkGroupToRole(): "
                        + "Unlinking group: " + groupDN + " to role :"
                        + roleDN);
            }
            // Remove the members from the admin role
            Iterator itr = members.iterator();
            try {
                ManagedRole role = (ManagedRole) UMSObject.getObject(token,
                        new Guid(roleDN));
                while (itr.hasNext()) {
                    String memberDN = (String) itr.next();
                    role.removeMember(new Guid(memberDN));
                }
            } catch (EntryNotFoundException ex) {
                debug.error("Compliance.verifyAndUnLinkGroupToRole: Admin "
                        + "groups are missing");
            } catch (UMSException ue) {
                debug.error("Compliance.verifyAndUnLinkGroupToRole(): ", ue);
                Object args[] = { roleDN };
                throw new AMException(AMSDKBundle.getString("972", args),
                        "772", args);
            }
        }
    }

    /**
     * Method which checks the attribute set for the presence of
     * "inetuserstatus" attribute. If the attribute exists and has a value of
     * "deleted", the method returns true, if not it returns false.
     * <p>
     * 
     * @param attrSet
     *            The attrSet to be verified
     * 
     * @exception AMException
     *                the attrSet has inetuserstatus attribute and the value of
     *                which is "deleted"
     */
    protected void verifyAttributes(AttrSet attrSet) throws AMException {
        String userStatus = attrSet.getValue(USER_STATUS_ATTRIBUTE);
        if (userStatus != null && userStatus.equalsIgnoreCase("deleted")) {
            debug.warning("Compliance.verifyAttributes(): "
                    + USER_STATUS_ATTRIBUTE + ": " + userStatus);
            throw new AMException(AMSDKBundle.getString("327"), "327");
        }
    }

    /**
     * Method which adds additional compliance required attributes to the
     * existing list of attribute names and then fetches the attribute set from
     * LDAP. The compliance attributes are verified for "inetuserstatus"
     * attribute.
     * <p>
     * 
     * @param po a PersistentObject of the entry.
     * @param attributeNames Array of attribute names.
     * @throws AMException if the fetched attribute names has inetuserstatus
     *         attribute and the value of which is "deleted" or if unable to
     *         fetch the attribute set.
     */
    protected AttrSet verifyAndGetAttributes(PersistentObject po,
            String[] attributeNames) throws AMException {
        // The only thing to verify for compliance is "deleted user". Hence,
        // fetch additional attribute "inetuserstatus" along with the given
        // attributes
        boolean found = false;

        // Check if "intetuserstatus" attribute already exists in request
        int i = 0;
        int numAttrs = attributeNames.length;
        String fetchAttributes[] = new String[numAttrs + 1];
        for (; i < numAttrs; i++) {
            if (attributeNames[i].equalsIgnoreCase(USER_STATUS_ATTRIBUTE)) {
                found = true;
                break;
            } else {
                fetchAttributes[i] = attributeNames[i];
            }
        }

        if (!found) // Add "inetuserstatus" attribute
            fetchAttributes[i] = USER_STATUS_ATTRIBUTE;
        else
            // use the original list of attr names
            fetchAttributes = attributeNames;

        // Fetch the attribute,value pairs
        AttrSet retAttrSet;
        try {
            retAttrSet = po.getAttributes(fetchAttributes);
        } catch (UMSException ue) {
            debug.error("Compliance.verifyAndGetAttributes(): ", ue);
            throw new AMException(AMSDKBundle.getString("330"), "330");
        }

        // Verify for deleted user
        verifyAttributes(retAttrSet);
        if (!found) {
            retAttrSet.remove(USER_STATUS_ATTRIBUTE);
        }
        return retAttrSet;
    }

    /**
     * Method which checks if the entry corresponding to userDN represents a
     * deleted user entry (entry with inetuserstatus:deleted)
     * 
     * @param token
     *            a SSOToken object
     * @param userDN
     *            a String representing a user DN
     * 
     * @exception AMEntryExistsException
     *                if the userDN corresponds to a deleted user
     */
    protected void checkIfDeletedUser(SSOToken token, String userDN)
            throws AMEntryExistsException {

        String userAttribute[] = { USER_STATUS_ATTRIBUTE };
        Attr attr;
        try {
            PersistentObject po = UMSObject.getObject(token, new Guid(userDN),
                    userAttribute);
            attr = po.getAttribute(USER_STATUS_ATTRIBUTE);
        } catch (UMSException ue) {
            if (debug.messageEnabled())
                debug.message("Compliance.checkIfDeletedUser(): ", ue);
            return;
        }
        if (attr != null) {
            String attrValue = attr.getValue();
            if (attrValue != null && attrValue.equalsIgnoreCase("deleted")) {
                debug.warning("Compliance.checkIfDeletedUser(): "
                        + "deleted user entry: " + userDN);
                throw new AMEntryExistsException(AMSDKBundle.getString("329"),
                        "329");
            }
        }
    }

    /**
     * Method which checks if the entry corresponding to orgDN represents a
     * deleted organization entry (entry with inetdomainstatus:deleted).
     * 
     * @param token
     *            a SSOToken object.
     * @param orgDN
     *            a String representing an organization DN.
     * 
     * @exception AMEntryExistsException
     *                if the orgDN corresponds to a deleted organization.
     */
    protected void checkIfDeletedOrg(SSOToken token, String orgDN)
            throws AMEntryExistsException {

        Attr attr;
        try {
            PersistentObject po = UMSObject.getObject(token, new Guid(orgDN));
            attr = po.getAttribute(ORG_STATUS_ATTRIBUTE);
        } catch (UMSException ue) {
            if (debug.messageEnabled())
                debug.message("Compliance.checkIfDeletedOrg(): ", ue);
            return;
        }
        if (((attr != null) && (attr.size() != 0)) && attr.contains("deleted")) 
        {
            // Org is deleted
            debug.warning("Compliance.checkIfDeletedOrg(): "
                    + "deleted org entry: " + orgDN);
            throw new AMEntryExistsException(AMSDKBundle.getString("361"),
                    "361");
        }
    }

    /**
     * Method which checks all the parent organizations of this entry till the
     * base DN, and returns true if any one of them is deleted.
     * 
     * @param token Single Sign On token of user.
     * @param dn Distinguished name of the object.
     * @param profileType the profile type of the object whose ancestor is
     *        being checked.
     * @throws AMException if there are errors from data layer.
     */
    public boolean isAncestorOrgDeleted(SSOToken token, String dn,
            int profileType) throws AMException {
        if (debug.messageEnabled()) {
            debug.message("Compliance.isAncestorOrgDeleted-> "
                    + " checking from... " + dn);
        }
        String tdn = (new DN(dn)).toRFCString().toLowerCase();
        if ((profileType == AMObject.ORGANIZATION)
                && deletedOrg.containsKey(tdn)) {
            if (((Boolean) deletedOrg.get(tdn)).booleanValue()) {
                return true;
            } // else continue
        }
        if (profileType != AMObject.ORGANIZATION) {
            tdn = DirectoryServicesFactory.getInstance().getOrganizationDN(
                    internalToken, dn);
        }
        while (!tdn.equalsIgnoreCase(rootSuffix)) {
            // Check to see if ancestor is in the cache deleted cache.
            if (debug.messageEnabled()) {
                debug.message("Compliance.isAncestorOrgDeleted-> "
                        + "Checking for deleted status of " + tdn);
            }
            if (deletedOrg.containsKey(tdn)) {
                return ((Boolean) deletedOrg.get(tdn)).booleanValue();
            }
            try {
                PersistentObject po = UMSObject.getObject(internalToken,
                        new Guid(tdn));
                Attr attr = po.getAttribute(ORG_STATUS_ATTRIBUTE);
                if (debug.messageEnabled() && (attr != null)) {
                    debug.message("Compliance.isAncestorOrgDeleted-> "
                            + ORG_STATUS_ATTRIBUTE + "=" + attr.toString());
                }
                if (((attr != null) && (attr.size() != 0))
                        && attr.contains("deleted")) {
                    // Org is deleted
                    if (debug.messageEnabled()) {
                        debug.message("isAncestorOrgDeleted: caching org: "
                                + tdn + " as deleted");
                    }
                    synchronized (deletedOrg) {
                        deletedOrg.put(tdn, Boolean.TRUE);
                    }
                    // we have encountered at least one ancestor
                    // who is deleted so return true.
                    return true;
                } else {
                    if (debug.messageEnabled()) {
                        debug.message("isAncestorOrgDeleted: caching org: "
                                + tdn + " as active");
                    }
                    synchronized (deletedOrg) {
                        deletedOrg.put(tdn, Boolean.FALSE);
                    }
                }
            } catch (UMSException umse) {
                debug.error("Compliance.isAncestorOrgDeleted-> "
                        + "UMSException", umse);
                return false;
            }
            // continue till we reach the rootSuffix. any one of
            // the ancestors could still be marked deleted.
            tdn = DirectoryServicesFactory.getInstance().getOrganizationDN(
                    token, dn);
        }
        // reached the rootsuffix. This will should never be marked deleted
        return false;
    }

    /**
     * Method to clean up the deletedOrg cache, when an event notification
     * occurs from the directory
     * 
     * @param orgDN
     *            DN of organization that has been modified
     */
    public void cleanDeletedOrgCache(String orgDN) {
        String tdn = orgDN;
        while (!tdn.equalsIgnoreCase(rootSuffix)) {
            // check to see if this dn is in the deletedOrg cache.
            // delete this entry if it is
            if (deletedOrg.containsKey(tdn)) {
                synchronized (deletedOrg) {
                    deletedOrg.remove(tdn);
                }
            }
            // Get the parent DN..
            tdn = (new DN(tdn)).getParent().toRFCString().toLowerCase();
        }
    }

    /**
     * Method which checks if the entry corresponding to DN represents a user
     * entry. If so, it sets the inetuserstatus attribute of the user to
     * deleted. Otherwise, it simply deletes the entry corresponding to the DN
     * 
     * @param token
     *            a SSOToken object
     * @param profileDN
     *            a String representing a DN
     * 
     * @exception AMException
     *                if an error is encountered while setting the
     *                intetuserstatus attribute or if an error was encountered
     *                while performing a delete.
     */
    public void verifyAndDeleteObject(SSOToken token, String profileDN)
            throws AMException {
        try {
            EmailNotificationHelper mailer = null;
            Map attributes = null;
            Guid guid = new Guid(profileDN);
            PersistentObject po = UMSObject.getObject(token, guid);
            if (po instanceof com.iplanet.ums.User) {
                Attr attr = new Attr(USER_STATUS_ATTRIBUTE, "deleted");
                if (debug.messageEnabled()) {
                    debug.message("Compliance:verifyAndDeleteObject: "
                            + "Soft-delete mode, setting inetuserstatus "
                            + "to deleted. " + "profileDN=" + profileDN);
                }
                po.modify(attr, ModSet.REPLACE);
                po.save();
                mailer = new EmailNotificationHelper(profileDN);
                if (mailer != null) {
                    mailer.setUserDeleteNotificationList();
                    attributes = DirectoryServicesFactory.getInstance()
                            .getAttributes(token, profileDN, AMObject.USER);
                    if (mailer.isPresentUserDeleteNotificationList()) {
                        mailer.sendUserDeleteNotification(attributes);
                    }
                }
                return;
            }
            if (po instanceof com.iplanet.ums.Resource) {
                Attr attr = new Attr(RESOURCE_STATUS_ATTRIBUTE, "deleted");
                if (debug.messageEnabled()) {
                    debug.message("Compliance:verifyAndDeleteObject: "
                            + "Soft-delete mode, setting icsstatus "
                            + "to deleted");
                }
                po.modify(attr, ModSet.REPLACE);
                po.save();
                return;
            }
            if (po instanceof com.iplanet.ums.StaticGroup
                    || po instanceof com.iplanet.ums.AssignableDynamicGroup
                    || po instanceof com.iplanet.ums.DynamicGroup) {
                Attr attr = new Attr(GROUP_STATUS_ATTRIBUTE, "deleted");
                if (debug.messageEnabled()) {
                    debug.message("Compliance:verifyAndDeleteObject: "
                            + "Soft-delete mode, setting inetgroupstatus "
                            + "to deleted");
                }
                po.modify(attr, ModSet.REPLACE);
                po.save();
                return;
            }
            if (po instanceof com.iplanet.ums.Organization) {
                if (debug.messageEnabled()) {
                    debug.message("Compliance:verifyAndDeleteObject: "
                            + "Soft-delete mode, setting inetdomainstatus "
                            + "to deleted");
                }
                Attr attr = new Attr(ORG_STATUS_ATTRIBUTE, "deleted");
                po.modify(attr, ModSet.REPLACE);
                po.save();
                DCTreeServicesImpl dcTreeImpl = (DCTreeServicesImpl) 
                    DirectoryServicesFactory.getInstance()
                        .getDCTreeServicesImpl();
                if (dcTreeImpl.isRequired()) {
                    dcTreeImpl.updateDomainStatus(token, profileDN, "deleted");
                }
            } else {
                UMSObject.removeObject(token, guid);
            }

        } catch (UMSException ue) {
            debug.error("Compliance.deleteObject(): ", ue);
            throw new AMException(AMSDKBundle.getString("773"), "773");
        } catch (SSOException se) {
            debug.error("Compliance.deleteObject(): ", se);
            throw new AMException(AMSDKBundle.getString("773"), "773");
        }
    }

    /**
     * Method which checks if Admin Groups need to be created for an
     * organization.
     * 
     * @param orgDN
     *            organization dn
     * @return true if Admin Groups need to be created
     * @exception AMException
     *                if an error is encountered
     */
    public static boolean isAdminGroupsEnabled(String orgDN) throws AMException 
    {
        if (!isUnderRootSuffix(orgDN)) {
            return false;
        }

        try {
            if (gsc == null) {
                ServiceSchemaManager scm = new ServiceSchemaManager(
                        ADMINISTRATION_SERVICE, internalToken);
                gsc = scm.getGlobalSchema();
            }
            Map attrMap = gsc.getReadOnlyAttributeDefaults();
            Set values = (Set) attrMap.get(ADMIN_GROUPS_ENABLED_ATTR);
            boolean enabled = false;
            if (values == null || values.isEmpty()) {
                enabled = false;
            } else {
                String val = (String) values.iterator().next();
                enabled = (val.equalsIgnoreCase("true"));
            }

            if (debug.messageEnabled()) {
                debug.message("Compliance.isAdminGroupsEnabled = " + enabled);
            }
            return enabled;
        } catch (SMSException ex) {
            debug.error(AMSDKBundle.getString("357"), ex);
            throw new AMException(AMSDKBundle.getString("357"), "357");
        } catch (SSOException ex) {
            debug.error(AMSDKBundle.getString("357"), ex);
            throw new AMException(AMSDKBundle.getString("357"), "357");
        }
    }

    /**
     * Method which checks if the object is directly under root suffix
     * 
     * @param objDN
     *            object dn
     * @return true if the object is directly under root suffix
     */
    protected static boolean isUnderRootSuffix(String objDN) {
        if (objDN == null || objDN.length() == 0) {
            // Will be null only in special cases during search filter
            // construction (AMSearchFilterMaanager.getSearchFilter())
            return true;
        }

        DN rootDN = new DN(rootSuffix);
        DN objectDN = new DN(objDN);
        if (rootDN.equals(objectDN) || rootDN.equals(objectDN.getParent())) {
            return true;
        }
        return false;
    }

    /**
     * Method which creates Admin Groups for an organization.
     * 
     * @param token
     *            a SSOToken object
     * @param org
     *            an organization object
     * @exception AMException
     *                if an error is encountered
     */
    protected void createAdminGroups(SSOToken token, PersistentObject org)
            throws AMException, SSOException {
        String gcDN = NamingAttributeManager
                .getNamingAttribute(AMObject.GROUP_CONTAINER)
                + "=groups," + org.getDN();

        AttrSet attrSet = new AttrSet();
        Attr attr = new Attr("objectclass", INET_ADMIN_OBJECT_CLASS);
        attrSet.add(attr);
        attr = new Attr(ADMIN_ROLE_ATTR, DOMAIN_ADMINISTRATORS);
        attrSet.add(attr);
        Map attributes = CommonUtils.attrSetToMap(attrSet);
        DirectoryServicesFactory.getInstance().createEntry(token,
                DOMAIN_ADMINISTRATORS, AMObject.ASSIGNABLE_DYNAMIC_GROUP, gcDN,
                attributes);

        attrSet = new AttrSet();
        attr = new Attr("objectclass", INET_ADMIN_OBJECT_CLASS);
        attrSet.add(attr);
        attr = new Attr(ADMIN_ROLE_ATTR, DOMAIN_ADMINISTRATORS);
        attrSet.add(attr);
        attributes = CommonUtils.attrSetToMap(attrSet);
        DirectoryServicesFactory.getInstance().createEntry(token,
                DOMAIN_HELP_DESK_ADMINISTRATORS,
                AMObject.ASSIGNABLE_DYNAMIC_GROUP, gcDN, attributes);
    }

    /**
     * Method which checks if Compliance User Deletion is enabled
     * 
     * @return true if Compliance User Deletion is enabled
     * @exception AMException
     *                if an error is encountered
     */
    public static boolean isComplianceUserDeletionEnabled() throws AMException {
        try {
            if (gsc == null) {
                ServiceSchemaManager scm = new ServiceSchemaManager(
                        ADMINISTRATION_SERVICE, internalToken);
                gsc = scm.getGlobalSchema();
            }
            Map attrMap = gsc.getReadOnlyAttributeDefaults();
            Set values = (Set) attrMap.get(COMPLIANCE_USER_DELETION_ATTR);
            boolean enabled = false;
            if (values == null || values.isEmpty()) {
                enabled = false;
            } else {
                String val = (String) values.iterator().next();
                enabled = (val.equalsIgnoreCase("true"));
            }

            if (debug.messageEnabled()) {
                debug.message("Compliance.isComplianceUserDeletionEnabled = "
                        + enabled);
            }
            return enabled;
        } catch (SMSException ex) {
            debug.error(AMSDKBundle.getString("359"), ex);
            throw new AMException(AMSDKBundle.getString("359"), "359");
        } catch (SSOException ex) {
            debug.error(AMSDKBundle.getString("359"), ex);
            throw new AMException(AMSDKBundle.getString("359"), "359");
        }
    }

    /**
     * Protected method to get the search filter to be used for searching for
     * deleted objects.
     * 
     */
    public String getDeletedObjectFilter(int objectType) throws AMException,
            SSOException {
        Set values = new HashSet();
        try {
            if (gsc == null) {
                ServiceSchemaManager scm = new ServiceSchemaManager(
                        ADMINISTRATION_SERVICE, internalToken);
                gsc = scm.getGlobalSchema();
            }
            Map attrMap = gsc.getAttributeDefaults();
            if (attrMap != null)
                values = (Set) attrMap.get(COMPLIANCE_SPECIAL_FILTER_ATTR);
            if (debug.messageEnabled()) {
                debug.message("Compliance.getDeletedObjectSearchFilter = "
                        + values.toString());
            }
        } catch (SMSException ex) {
            debug.error(AMSDKBundle.getString("359"), ex);
            throw new AMException(AMSDKBundle.getString("359"), "359");
        } catch (SSOException ex) {
            debug.error(AMSDKBundle.getString("359"), ex);
            throw new AMException(AMSDKBundle.getString("359"), "359");
        }
        String org_filter = null;
        String group_filter = null;
        String user_filter = null;
        String def_filter = null;
        String res_filter = null;
        Iterator iter = values.iterator();
        while (iter.hasNext()) {
            String thisFilter = (String) iter.next();
            if (thisFilter.startsWith("Organization=")) {
                org_filter = thisFilter.substring(13);
            } else if (thisFilter.startsWith("Group=")) {
                group_filter = thisFilter.substring(6);
            } else if (thisFilter.startsWith("User=")) {
                user_filter = thisFilter.substring(5);
            } else if (thisFilter.startsWith("Misc=")) {
                def_filter = thisFilter.substring(5);
            } else if (thisFilter.startsWith("Resource=")) {
                res_filter = thisFilter.substring(9);
            }
        }
        org_filter = (org_filter == null) ? DEFAULT_DELETED_ORG_FILTER
                : org_filter;
        group_filter = (group_filter == null) ? DEFAULT_DELETED_GROUP_FILTER
                : group_filter;
        user_filter = (user_filter == null) ? DEFAULT_DELETED_USER_FILTER
                : user_filter;
        def_filter = (def_filter == null) ? DEFAULT_DELETED_OBJECT_FILTER
                : def_filter;
        res_filter = (res_filter == null) ? DEFAULT_DELETED_RESOURCE_FILTER
                : res_filter;
        switch (objectType) {
        case AMObject.ORGANIZATION:
            return (org_filter);
        case AMObject.USER:
            return (user_filter);
        case AMObject.ASSIGNABLE_DYNAMIC_GROUP:
        case AMObject.DYNAMIC_GROUP:
        case AMObject.STATIC_GROUP:
        case AMObject.GROUP:
            return (group_filter);
        case AMObject.RESOURCE:
            return (res_filter);
        default:
            return ("(|" + org_filter + group_filter + user_filter + def_filter
                    + res_filter + ")");
        }
    }
}
