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
 * Copyright 2013-2015 ForgeRock AS.
 */

package org.forgerock.openam.idrepo.ldap;

import com.sun.identity.idm.IdRepo;
import com.sun.identity.idm.IdRepoErrorCode;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import java.util.Map;
import java.util.Set;
import static org.fest.assertions.Assertions.*;
import static org.forgerock.openam.ldap.LDAPConstants.*;
import static org.forgerock.openam.utils.CollectionUtils.*;
import static org.testng.Assert.fail;

import org.forgerock.openam.utils.MapHelper;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class DSEERepoTest extends IdRepoTestBase {

    private static final String DSEE_SETTINGS = "/config/dseesettings.properties";
    private static final String DSEE_LDIF = "/ldif/dsee.ldif";
    private static final String ACCOUNTANT = "Accountant";
    private static final String ACCOUNTANT_DN = "cn=Accountant,dc=openam,dc=openidentityplatform,dc=org";
    private static final String MANAGER = "Manager";
    private static final String MANAGER_DN = "cn=Manager,dc=openam,dc=openidentityplatform,dc=org";

    @BeforeClass
    public void setUp() throws Exception {
        idrepo.initialize(MapHelper.readMap(DSEE_SETTINGS));
        idrepo.addListener(null, idRepoListener);
    }

    @Override
    protected String getLDIFPath() {
        return DSEE_LDIF;
    }

    @Test
    public void addingRoleToUserSuccessful() throws Exception {
        Set<String> roleMemberships = idrepo.getMemberships(null, IdType.USER, DEMO, IdType.ROLE);
        assertThat(roleMemberships).isNotNull().isEmpty();
        assertThat(idrepo.getAttributes(null, IdType.USER, DEMO, asSet(ROLE_DN_ATTR))).isEmpty();
        idrepo.modifyMemberShip(null, IdType.ROLE, ACCOUNTANT, asSet(DEMO), IdType.USER, IdRepo.ADDMEMBER);
        Map<String, Set<String>> attrs = idrepo.getAttributes(null, IdType.USER, DEMO, asSet(ROLE_DN_ATTR));
        assertThat(attrs.get(ROLE_DN_ATTR)).isNotNull().containsOnly(ACCOUNTANT_DN);
        roleMemberships = idrepo.getMemberships(null, IdType.USER, DEMO, IdType.ROLE);
        assertThat(roleMemberships).hasSize(1).containsOnly(ACCOUNTANT_DN);
        //retrieving filtered roles should also return managed roles
        roleMemberships = idrepo.getMemberships(null, IdType.USER, DEMO, IdType.FILTEREDROLE);
        assertThat(roleMemberships).hasSize(1).containsOnly(ACCOUNTANT_DN);
    }

    @Test(dependsOnMethods = "addingRoleToUserSuccessful")
    public void removingRoleFromUserSuccessful() throws Exception {
        Set<String> roleMemberships = idrepo.getMemberships(null, IdType.USER, DEMO, IdType.ROLE);
        assertThat(roleMemberships).hasSize(1).containsOnly(ACCOUNTANT_DN);
        idrepo.modifyMemberShip(null, IdType.ROLE, ACCOUNTANT, asSet(DEMO), IdType.USER, IdRepo.REMOVEMEMBER);
        assertThat(idrepo.getAttributes(null, IdType.USER, DEMO, asSet(ROLE_DN_ATTR))).isEmpty();
        roleMemberships = idrepo.getMemberships(null, IdType.USER, DEMO, IdType.ROLE);
        assertThat(roleMemberships).isNotNull().isEmpty();
    }

    @Test(dependsOnMethods = "removingRoleFromUserSuccessful")
    public void retrievingNonExistentFilteredRoleMembershipsDoesNotFail() throws Exception {
        Set<String> filteredRoles = idrepo.getMemberships(null, IdType.USER, DEMO, IdType.FILTEREDROLE);
        assertThat(filteredRoles).isNotNull().isEmpty();
    }

    @Test
    public void retrievingFilteredRoleIsSuccessful() throws Exception {
        Set<String> filteredRoles = idrepo.getMemberships(null, IdType.USER, USER0, IdType.FILTEREDROLE);
        assertThat(filteredRoles).hasSize(1).containsOnly(MANAGER_DN);
    }

    @Test
    public void retrievingFilteredRoleMembershipsIsSuccessful() throws Exception {
        Set<String> members = idrepo.getMembers(null, IdType.FILTEREDROLE, MANAGER, IdType.USER);
        assertThat(members).hasSize(1).containsOnly(USER0_DN);
    }

    @Test
    public void removingFilteredRoleFails() throws Exception {
        try {
            idrepo.modifyMemberShip(null, IdType.FILTEREDROLE, MANAGER, asSet(USER0), IdType.USER,
                    IdRepo.REMOVEMEMBER);
            fail();
        } catch (IdRepoException ire) {
            assertThat(ire).hasMessage(getIdRepoExceptionMessage(IdRepoErrorCode.MEMBERSHIP_CANNOT_BE_MODIFIED,
                    DJLDAPv3Repo.class.getName(), IdType.FILTEREDROLE.getName()));
        }
    }
}
