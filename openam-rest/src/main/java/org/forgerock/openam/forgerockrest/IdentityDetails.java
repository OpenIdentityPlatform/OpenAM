/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.forgerockrest;

import java.util.Map;
import java.util.Set;

/**
 * Models an identity's (user/group/agent) details.
 *
 * @since 13.0.0
 */
class IdentityDetails {

    private String name;
    private String type;
    private String realm;
    private Set<String> roleList;
    private Set<String> groupList;
    private Set<String> memberList;
    private Map<String, Set<String>> attributes;

    String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    String getType() {
        return type;
    }

    void setType(String type) {
        this.type = type;
    }

    String getRealm() {
        return realm;
    }

    void setRealm(String realm) {
        this.realm = realm;
    }

    Set<String> getRoleList() {
        return roleList;
    }

    void setRoleList(Set<String> roleList) {
        this.roleList = roleList;
    }

    Set<String> getGroupList() {
        return groupList;
    }

    void setGroupList(Set<String> groupList) {
        this.groupList = groupList;
    }

    Set<String> getMemberList() {
        return memberList;
    }

    void setMemberList(Set<String> memberList) {
        this.memberList = memberList;
    }

    Map<String, Set<String>> getAttributes() {
        return attributes;
    }

    void setAttributes(Map<String, Set<String>> attributes) {
        this.attributes = attributes;
    }
}
