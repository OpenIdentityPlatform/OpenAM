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
 * Copyright 2013-2016 ForgeRock AS.
 */
package org.forgerock.openam.idrepo.ldap;

import static org.fest.assertions.Assertions.assertThat;
import static org.forgerock.openam.utils.CollectionUtils.asOrderedSet;
import static org.forgerock.openam.utils.CollectionUtils.asSet;
import static org.mockito.Mockito.*;
import static org.testng.Assert.fail;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.sun.identity.idm.IdRepoListener;
import org.forgerock.openam.utils.CrestQuery;
import org.forgerock.openam.utils.MapHelper;
import org.forgerock.opendj.ldap.Filter;
import org.forgerock.opendj.ldap.ResultCode;
import org.mockito.ArgumentCaptor;
import org.powermock.api.mockito.PowerMockito;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.sun.identity.authentication.spi.InvalidPasswordException;
import com.sun.identity.common.CaseInsensitiveHashMap;
import com.sun.identity.common.CaseInsensitiveHashSet;
import com.sun.identity.idm.IdRepo;
import com.sun.identity.idm.IdRepoDuplicateObjectException;
import com.sun.identity.idm.IdRepoErrorCode;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.RepoSearchResults;
import com.sun.identity.sm.SchemaType;

public class GenericRepoTest extends IdRepoTestBase {

    private static final String GENERIC_SETTINGS = "/config/genericsettings.properties";
    private static final String GENERIC_DS_LDIF = "/ldif/generic.ldif";

    @BeforeClass
    public void setUp() throws Exception {
        idrepo.initialize(MapHelper.readMap(GENERIC_SETTINGS));
        idrepo.addListener(null, idRepoListener);
    }

    @Override
    protected String getLDIFPath() {
        return GENERIC_DS_LDIF;
    }

    @Test
    public void authenticationSuccessful() throws Exception {
        assertThat(idrepo.authenticate(getCredentials(DEMO, "changeit"))).isTrue();
    }

    @Test(description = "OPENAM-3666")
    public void authenticationFailsWithInvalidPassword() throws Exception {
        try {
            idrepo.authenticate(getCredentials(DEMO, "invalid"));
            fail();
        } catch (InvalidPasswordException ipe) {
            assertThat(ipe.getTokenId()).isEqualTo(DEMO);
        }
    }

    @Test
    public void authenticationFailsWithInvalidUserName() {
        try {
            idrepo.authenticate(getCredentials("invalid", "changeit"));
            fail();
        } catch (Exception ex) {
            assertThat(ex).isInstanceOf(IdRepoException.class);
        }
    }

    @Test
    public void changePasswordFailsWithInvalidPassword() throws Exception {
        try {
            idrepo.changePassword(null, IdType.USER, DEMO, "userpassword", "invalid", "newpassword");
            fail();
        } catch (Exception ex) {
            assertThat(ex).isInstanceOf(IdRepoException.class)
                    .hasMessage(getIdRepoExceptionMessage(IdRepoErrorCode.LDAP_EXCEPTION_OCCURRED,
                            DJLDAPv3Repo.class.getName(), "49"));
        }
    }

    @Test
    public void changePasswordSuccessful() throws Exception {
        assertThat(idrepo.authenticate(getCredentials(DEMO, "changeit"))).isTrue();
        idrepo.changePassword(null, IdType.USER, DEMO, "userpassword", "changeit", "newpassword");
        try {
            idrepo.authenticate(getCredentials(DEMO, "changeit"));
            fail();
        } catch (InvalidPasswordException ex) {
        }
        assertThat(idrepo.authenticate(getCredentials(DEMO, "newpassword"))).isTrue();
        idrepo.changePassword(null, IdType.USER, DEMO, "userpassword", "newpassword", "changeit");
        try {
            idrepo.authenticate(getCredentials(DEMO, "newpassword"));
            fail();
        } catch (InvalidPasswordException ex) {
        }
        assertThat(idrepo.authenticate(getCredentials(DEMO, "changeit"))).isTrue();
    }

    @Test
    public void getFullyQualifiedNameWorksForExistingUser() throws Exception {
        String fqn = idrepo.getFullyQualifiedName(null, IdType.USER, DEMO);
        assertThat(fqn).isNotNull().isEqualTo("[localhost:50389]/" + DEMO_DN);
    }

    @Test
    public void getFullyQualifiedNameReturnsNullIfUserDoesNotExist() throws Exception {
        assertThat(idrepo.getFullyQualifiedName(null, IdType.USER, "badger")).isNull();
    }

    @Test
    public void isActiveReturnsFalseForNonExistentUser() throws Exception {
        assertThat(idrepo.isActive(null, IdType.USER, "invalid")).isFalse();
    }

    @Test
    public void isActiveReturnsFalseForInactiveUser() throws Exception {
        idrepo.setActiveStatus(null, IdType.USER, DEMO, false);
        assertThat(idrepo.isActive(null, IdType.USER, DEMO)).isFalse();
        idrepo.setActiveStatus(null, IdType.USER, DEMO, true);
        assertThat(idrepo.isActive(null, IdType.USER, DEMO)).isTrue();
    }

    @Test
    public void isExistsReturnsFalseForNonExistentUser() throws Exception {
        assertThat(idrepo.isExists(null, IdType.USER, "invalid")).isFalse();
    }

    @Test
    public void isExistsReturnsTrueForExistingUser() throws Exception {
        assertThat(idrepo.isExists(null, IdType.USER, DEMO)).isTrue();
    }

    @Test
    public void getAttributesDoesNotReturnUndefinedAttributes() throws Exception {
        Map<String, Set<String>> attrs = idrepo.getAttributes(null, IdType.USER, DEMO);
        Set<String> attrNames = new CaseInsensitiveHashSet(attrs.keySet());
        assertThat(attrs).isNotEmpty();
        assertThat(attrNames.contains("l")).isFalse();
        attrs = idrepo.getAttributes(null, IdType.USER, DEMO, asSet("l"));
        assertThat(attrs).isEmpty();
        attrs = idrepo.getAttributes(null, IdType.USER, DEMO, asSet("l", "sn"));
        assertThat(attrs).hasSize(1);
        assertThat(attrs.keySet().contains("sn")).isTrue();
    }

    @Test
    public void getAttributesFailsForNonExistentUser() throws Exception {
        try {
            idrepo.getAttributes(null, IdType.USER, "invalid");
            fail();
        } catch (IdRepoException ire) {
            assertThat(ire).isInstanceOf(IdRepoException.class)
                    .hasMessage(getIdRepoExceptionMessage(IdRepoErrorCode.TYPE_NOT_FOUND, "invalid", "user"));
        }
    }

    @Test
    public void getAttributesReturnsDNIfNoAttributesAreRequested() throws Exception {
        Map<String, Set<String>> attrs = idrepo.getAttributes(null, IdType.USER, DEMO);
        assertThat(attrs.get("dn")).isNotNull().contains(DEMO_DN);
    }

    @Test
    public void getAttributesReturnsDNIfRequested() throws Exception {
        Map<String, Set<String>> attrs = idrepo.getAttributes(null, IdType.USER, DEMO, asSet("dn"));
        assertThat(attrs.keySet()).hasSize(1).contains("dn");
        assertThat(attrs.get("dn")).isNotNull().contains(DEMO_DN);
    }

    @Test
    public void getBinaryAttributesReturnsByteArrays() throws Exception {
        Map<String, byte[][]> binAttrs = idrepo.getBinaryAttributes(null, IdType.USER, DEMO, asSet("sn"));
        assertThat(binAttrs).isNotNull().isNotEmpty();
        assertThat(binAttrs.keySet()).hasSize(1).contains("sn");
        byte[][] values = binAttrs.get("sn");
        assertThat(values.length).isEqualTo(1);
        assertThat(values[0]).isEqualTo("demo".getBytes("UTF-8"));
    }

    @Test
    public void createIgnoresUndefinedAttributes() throws Exception {
        Map<String, Set<String>> attrs = MapHelper.readMap("/config/users/testuser1.properties");
        String dn = idrepo.create(null, IdType.USER, TEST_USER1, attrs);
        assertThat(dn).isEqualTo("uid=testuser1,ou=people,dc=openam,dc=openidentityplatform,dc=org");
        Map<String, Set<String>> createdAttrs =
                new CaseInsensitiveHashMap(idrepo.getAttributes(null, IdType.USER, TEST_USER1));
        assertThat(createdAttrs.keySet().containsAll(asSet("objectclass", "uid", "sn", "cn", "postaladdress")));
        assertThat(createdAttrs.get("sn")).containsOnly(TEST_USER1);
        assertThat(createdAttrs.get("cn")).containsOnly(TEST_USER1);
        assertThat(createdAttrs.get("postaladdress")).containsOnly("true");
    }

    @Test
    public void deleteFailsForNonExistentUser() throws Exception {
        try{
            idrepo.delete(null, IdType.USER, "invalid");
            fail();
        } catch (IdRepoException ire) {
            assertThat(ire).hasMessage(getIdRepoExceptionMessage(IdRepoErrorCode.TYPE_NOT_FOUND, "invalid", IdType.USER.getName()));
        }
    }

    @Test(dependsOnMethods = "createIgnoresUndefinedAttributes")
    public void deleteSuccessful() throws Exception {
        assertThat(idrepo.isExists(null, IdType.USER, TEST_USER1)).isTrue();
        idrepo.delete(null, IdType.USER, TEST_USER1);
        assertThat(idrepo.isExists(null, IdType.USER, TEST_USER1)).isFalse();
    }

    @Test
    public void setAttributesFailsWithEmptyChangeset() throws Exception {
        try {
            idrepo.setAttributes(null, IdType.USER, DEMO, Collections.EMPTY_MAP, true);
            fail();
        } catch (IdRepoException ire) {
            assertThat(ire).hasMessage(getIdRepoExceptionMessage(IdRepoErrorCode.ILLEGAL_ARGUMENTS));
        }
    }

    //Disabled, because this would require a bit more clever schema object in IdRepoTestBase.
    @Test(enabled = false)
    public void setAttributesAddsNecessaryObjectClasses() throws Exception {
        Map<String, Set<String>> attrs = idrepo.getAttributes(null, IdType.USER, DEMO, asSet("objectclass"));
        assertThat(attrs).hasSize(1);
        Set<String> attr = new CaseInsensitiveHashSet(attrs.entrySet().iterator().next().getValue());
        assertThat(attr.contains("pilotPerson")).isFalse();
        Map<String, Set<String>> changes = new HashMap<String, Set<String>>();
        changes.put("otherMailbox", asSet("DemoLand"));
        idrepo.setAttributes(null, IdType.USER, DEMO, changes, true);
        attrs = idrepo.getAttributes(null, IdType.USER, DEMO, asSet("objectclass"));
        assertThat(attrs).hasSize(1);
        attr = new CaseInsensitiveHashSet(attrs.entrySet().iterator().next().getValue());
        assertThat(attr.contains("pilotPerson")).isTrue();
    }

    @Test
    public void settingAttributesIsSuccessful() throws Exception {
        Map<String, Set<String>> attrs = new CaseInsensitiveHashMap(idrepo.getAttributes(null, IdType.USER, DEMO));
        assertThat(attrs.get("sn")).contains(DEMO);
        Map<String, Set<String>> changes = new HashMap<String, Set<String>>();
        changes.put("sn", asSet("testsn"));
        idrepo.setAttributes(null, IdType.USER, DEMO, changes, true);
        attrs = new CaseInsensitiveHashMap(idrepo.getAttributes(null, IdType.USER, DEMO));
        assertThat(attrs.get("sn")).containsOnly(DEMO, "testsn");
        changes.put("sn", asSet(DEMO));
        idrepo.setAttributes(null, IdType.USER, DEMO, changes, false);
        attrs = new CaseInsensitiveHashMap(idrepo.getAttributes(null, IdType.USER, DEMO));
        assertThat(attrs.get("sn")).containsOnly(DEMO);
    }

    @Test
    public void settingBinarryAttributesIsSuccessful() throws Exception {
        Map<String, Set<String>> attrs = new CaseInsensitiveHashMap(idrepo.getAttributes(null, IdType.USER, DEMO));
        assertThat(attrs.get("sn")).contains(DEMO);
        Map<String, byte[][]> changes = new HashMap<String, byte[][]>();
        byte[][] values = new byte[1][];
        values[0] = "testsn".getBytes("UTF-8");
        changes.put("sn", values);
        idrepo.setBinaryAttributes(null, IdType.USER, DEMO, changes, true);
        attrs = new CaseInsensitiveHashMap(idrepo.getAttributes(null, IdType.USER, DEMO));
        assertThat(attrs.get("sn")).containsOnly(DEMO, "testsn");
        values[0] = DEMO.getBytes("UTF-8");
        changes.put("sn", values);
        idrepo.setBinaryAttributes(null, IdType.USER, DEMO, changes, false);
        attrs = new CaseInsensitiveHashMap(idrepo.getAttributes(null, IdType.USER, DEMO));
        assertThat(attrs.get("sn")).containsOnly(DEMO);
    }

    @Test(description = "OPENAM-3237")
    public void settingNonExistentAttributeWithEmptyValueDoesNotFail() throws Exception {
        Map<String, Set<String>> attrs = new CaseInsensitiveHashMap(idrepo.getAttributes(null, IdType.USER, DEMO));
        assertThat(attrs.get("cn")).isNull();
        Map<String, Set<String>> changes = new HashMap<String, Set<String>>();
        changes.put("cn", new HashSet<String>(0));
        idrepo.setAttributes(null, IdType.USER, DEMO, changes, true);
        attrs = new CaseInsensitiveHashMap(idrepo.getAttributes(null, IdType.USER, DEMO));
        assertThat(attrs.get("cn")).isNull();
    }

    @Test
    public void removeAttributesFailWithEmptyChangeset() throws Exception {
        try {
            idrepo.removeAttributes(null, IdType.USER, DEMO, asSet("l"));
            fail();
        } catch (IdRepoException ire) {
            assertThat(ire).hasMessage(getIdRepoExceptionMessage(IdRepoErrorCode.ILLEGAL_ARGUMENTS));
        }
    }

    @Test
    public void removeAttributesSuccessful() throws Exception {
        Map<String, Set<String>> attrs =
                new CaseInsensitiveHashMap(idrepo.getAttributes(null, IdType.USER, DEMO, asSet("givenName")));
        assertThat(attrs.get("givenName")).containsOnly(DEMO);
        idrepo.removeAttributes(null, IdType.USER, DEMO, asSet("givenName"));
        attrs = new CaseInsensitiveHashMap(idrepo.getAttributes(null, IdType.USER, DEMO, asSet("givenName")));
        assertThat(attrs.get("givenName")).isNull();
        Map<String, Set<String>> changes = new HashMap<String, Set<String>>();
        changes.put("givenName", asSet(DEMO));
        idrepo.setAttributes(null, IdType.USER, DEMO, changes, true);
        attrs = new CaseInsensitiveHashMap(idrepo.getAttributes(null, IdType.USER, DEMO, asSet("givenName")));
        assertThat(attrs.get("givenName")).containsOnly(DEMO);
    }

    @Test(description = "OPENAM-3376")
    public void createEntryWithEmptyAttributes() throws Exception {
        Map<String, Set<String>> attrs = MapHelper.readMap("/config/users/testuser1.properties");
        attrs.put("mail", null);
        idrepo.create(null, IdType.USER, TEST_USER1, attrs);
        assertThat(idrepo.isExists(null, IdType.USER, TEST_USER1)).isTrue();
        idrepo.delete(null, IdType.USER, TEST_USER1);
        assertThat(idrepo.isExists(null, IdType.USER, TEST_USER1)).isFalse();
    }

    @Test
    public void searchReturnsEmptyResultsIfNoMatch() throws Exception {
        CrestQuery crestQuery = new CrestQuery("invalid");
        RepoSearchResults results =
                idrepo.search(null, IdType.USER, crestQuery, 0, 0, null, true, IdRepo.AND_MOD, null, true);
        assertThat(results.getErrorCode()).isEqualTo(ResultCode.SUCCESS.intValue());
        assertThat(results.getType()).isEqualTo(IdType.USER);
        assertThat(results.getSearchResults()).isEmpty();
        assertThat(results.getResultAttributes()).isEmpty();
    }

    @Test
    public void searchReturnsMatchesForSearchAttribute() throws Exception {
        CrestQuery crestQuery = new CrestQuery("searchTester*");
        RepoSearchResults results =
                idrepo.search(null, IdType.USER, crestQuery, 0, 0, null, true, IdRepo.AND_MOD, null, true);
        assertThat(results.getErrorCode()).isEqualTo(ResultCode.SUCCESS.intValue());
        assertThat(results.getType()).isEqualTo(IdType.USER);
        assertThat(results.getSearchResults()).hasSize(4).containsOnly("searchTester1", "searchTester2",
                "searchTester3", "searchTester4");
    }

    @Test
    public void searchReturnsRequestedAttributes() throws Exception {
        CrestQuery crestQuery = new CrestQuery("searchTester1");
        RepoSearchResults results =
                idrepo.search(null, IdType.USER, crestQuery, 0, 0, asSet("sn"), false, IdRepo.AND_MOD, null, true);
        assertThat(results.getErrorCode()).isEqualTo(ResultCode.SUCCESS.intValue());
        assertThat(results.getType()).isEqualTo(IdType.USER);
        assertThat(results.getSearchResults()).hasSize(1).containsOnly("searchTester1");
        Map<String, Map<String, Set<String>>> resultAttrs = results.getResultAttributes();
        assertThat(resultAttrs).hasSize(1);
        assertThat(resultAttrs.get("searchTester1")).hasSize(2);
        assertThat(resultAttrs.get("searchTester1").get("sn")).containsOnly("hello");
        assertThat(resultAttrs.get("searchTester1").get("uid")).containsOnly("searchTester1");
    }

    //need to depend on deleteSuccessful, otherwise testuser1 would ruin the day :)
    @Test(dependsOnMethods = "deleteSuccessful")
    public void searchReturnsMatchesForComplexFilters() throws Exception {
        Map<String, Set<String>> avPairs = new HashMap<String, Set<String>>();
        avPairs.put("objectclass", asSet("inetorgperson"));
        avPairs.put("sn", asSet("hellNo"));
        CrestQuery crestQuery = new CrestQuery("*");
        RepoSearchResults results =
                idrepo.search(null, IdType.USER, crestQuery, 0, 0, null, true, IdRepo.AND_MOD, avPairs, true);
        assertThat(results.getErrorCode()).isEqualTo(ResultCode.SUCCESS.intValue());
        assertThat(results.getType()).isEqualTo(IdType.USER);
        assertThat(results.getSearchResults()).containsOnly("searchTester3");
        results = idrepo.search(null, IdType.USER, crestQuery, 0, 0, null, true, IdRepo.OR_MOD, avPairs, true);
        assertThat(results.getErrorCode()).isEqualTo(ResultCode.SUCCESS.intValue());
        assertThat(results.getType()).isEqualTo(IdType.USER);
        assertThat(results.getSearchResults()).containsOnly(DEMO, "searchTester1", "searchTester2", "searchTester3",
                "searchTester4");
    }

    @Test(enabled = false)
    public void searchReturnsEarlyIfMaxResultsReached() throws Exception {
        CrestQuery crestQuery = new CrestQuery("*");
        RepoSearchResults results = idrepo.search(null, IdType.USER, crestQuery, 0, 2, null, true,
                                                                                    IdRepo.AND_MOD, null, true);
        assertThat(results.getErrorCode()).isEqualTo(RepoSearchResults.SIZE_LIMIT_EXCEEDED);
        assertThat(results.getSearchResults()).hasSize(2);
    }

    @Test
    public void groupCreationSuccessful() throws Exception {
        Map<String, Set<String>> attributes = MapHelper.readMap("/config/groups/test1.properties");
        idrepo.create(null, IdType.GROUP, TEST1_GROUP, attributes);
        Map<String, Set<String>> returnedAttrs =
                new CaseInsensitiveHashMap(idrepo.getAttributes(null, IdType.GROUP, TEST1_GROUP));
        assertThat(returnedAttrs.get("uniqueMember")).containsOnly("uid=demo,ou=people,dc=openam,dc=openidentityplatform,dc=org");
        assertThat(idrepo.getMembers(null, IdType.GROUP, TEST1_GROUP, IdType.USER)).containsOnly(DEMO_DN);
        assertThat(idrepo.getMemberships(null, IdType.USER, DEMO, IdType.GROUP)).contains(TEST1_GROUP_DN);
    }

    @Test(dependsOnMethods = "groupCreationSuccessful")
    public void cannotCreateGroupWithSameNameAsExistingGroup() throws Exception {
        Map<String, Set<String>> attributes = MapHelper.readMap("/config/groups/test1.properties");
        try {
            idrepo.create(null, IdType.GROUP, TEST1_GROUP, attributes);
            fail();
        } catch (Exception ex) {
            assertThat(ex).isInstanceOf(IdRepoDuplicateObjectException.class)
                    .hasMessage(getIdRepoExceptionMessage(IdRepoErrorCode.NAME_ALREADY_EXISTS, TEST1_GROUP));
        }
    }

    @Test(dependsOnMethods = "groupCreationSuccessful")
    public void groupMemberRemovalSuccessful() throws Exception {
        assertThat(idrepo.getMembers(null, IdType.GROUP, TEST1_GROUP, IdType.USER)).containsOnly(DEMO_DN);
        idrepo.modifyMemberShip(null, IdType.GROUP, TEST1_GROUP, asSet(DEMO), IdType.USER, IdRepo.REMOVEMEMBER);
        assertThat(idrepo.getMembers(null, IdType.GROUP, TEST1_GROUP, IdType.USER)).isEmpty();
    }

    @Test(dependsOnMethods = "groupMemberRemovalSuccessful")
    public void groupMembershipsAreConsistent() throws Exception {
        assertThat(idrepo.getMembers(null, IdType.GROUP, TEST1_GROUP, IdType.USER)).isEmpty();
        assertThat(idrepo.getMemberships(null, IdType.USER, DEMO, IdType.GROUP)).isEmpty();
        idrepo.modifyMemberShip(null, IdType.GROUP, TEST1_GROUP, asSet(DEMO), IdType.USER, IdRepo.ADDMEMBER);
        assertThat(idrepo.getMembers(null, IdType.GROUP, TEST1_GROUP, IdType.USER)).containsOnly(DEMO_DN);
        assertThat(idrepo.getMemberships(null, IdType.USER, DEMO, IdType.GROUP)).containsOnly(TEST1_GROUP_DN);
        idrepo.modifyMemberShip(null, IdType.GROUP, TEST1_GROUP, asSet(DEMO), IdType.USER, IdRepo.REMOVEMEMBER);
        assertThat(idrepo.getMembers(null, IdType.GROUP, TEST1_GROUP, IdType.USER)).isEmpty();
        assertThat(idrepo.getMemberships(null, IdType.USER, DEMO, IdType.GROUP)).isEmpty();
    }

    @Test(dependsOnMethods = "groupMembershipsAreConsistent")
    public void groupDeletionSuccessful() throws Exception {
        assertThat(idrepo.isExists(null, IdType.GROUP, TEST1_GROUP)).isTrue();
        idrepo.delete(null, IdType.GROUP, TEST1_GROUP);
        assertThat(idrepo.isExists(null, IdType.GROUP, TEST1_GROUP)).isFalse();
    }

    @Test
    public void assignNonUserServiceIsNoop() throws Exception {
        Map<String, Set<String>> attributes = new CaseInsensitiveHashMap(idrepo.getAttributes(null, IdType.USER, DEMO));
        Map<String, Set<String>> changes = new HashMap<String, Set<String>>();
        changes.put("manager", asSet("DemoBoss"));
        idrepo.assignService(null, IdType.USER, DEMO, "myservice", SchemaType.DYNAMIC, changes);
        Set<String> values = idrepo.getAttributes(null, IdType.USER, DEMO, asSet("manager")).get("manager");
        assertThat(values).isNull();
        Map<String, Set<String>> attributesAfter =
                new CaseInsensitiveHashMap(idrepo.getAttributes(null, IdType.USER, DEMO));
        assertThat(attributes).isEqualTo(attributesAfter);
    }

    @Test
    public void assignServiceFailsWithInvalidType() throws Exception {
        try {
            idrepo.assignService(null, IdType.GROUP, DEMO, "myservice", SchemaType.USER,
                    new HashMap<String, Set<String>>());
            fail();
        } catch (IdRepoException ire) {
            assertThat(ire).hasMessage(getIdRepoExceptionMessage(
                    IdRepoErrorCode.SERVICES_NOT_SUPPORTED_FOR_AGENTS_AND_GROUPS, DJLDAPv3Repo.class.getName()));
        }
    }

    @Test
    public void assignServiceToUserIsSuccessful() throws Exception {
        Map<String, Set<String>> changes = new HashMap<String, Set<String>>();
        changes.put("manager", asSet("DemoBoss"));
        changes.put("iplanet-am-session-max-caching-time", asSet("10"));
        idrepo.assignService(null, IdType.USER, DEMO, "iPlanetAMSessionService", SchemaType.USER, changes);
        Set<String> values = idrepo.getAttributes(null, IdType.USER, DEMO, asSet("manager")).get("manager");
        assertThat(values).isNotNull().hasSize(1).containsOnly("DemoBoss");
    }

    @Test
    public void assignServiceSucceedsForRealm() throws Exception {
        Map<String, Set<String>> changes = new HashMap<String, Set<String>>();
        changes.put("iplanet-am-session-max-session-time", asSet("10000"));
        idrepo.assignService(null, IdType.REALM, null, "iPlanetAMSessionService", SchemaType.ORGANIZATION, changes);
        ArgumentCaptor<Map> argument = ArgumentCaptor.forClass(Map.class);
        verify(idRepoListener, times(1)).setServiceAttributes(eq("iPlanetAMSessionService"), argument.capture());
        assertThat(argument.getValue().keySet()).contains("iPlanetAMSessionService");
        assertThat(argument.getValue().get("iPlanetAMSessionService")).isEqualTo(changes);
    }

    @Test
    public void getAssignedServicesReturnsEmptySetIfNoServiceMatches() throws Exception {
        Map<String, Set<String>> lie = new HashMap<String, Set<String>>();
        lie.put("iPlanetAMSessionService", asSet("nonExistent"));
        Set<String> assignedServices = idrepo.getAssignedServices(null, IdType.USER, DEMO, lie);
        assertThat(assignedServices).isNotNull().isEmpty();
    }

    @Test
    public void getAssignedServicesFailsForInvalidType() throws Exception {
        try {
            idrepo.getAssignedServices(null, IdType.GROUP, DEMO, null);
            fail();
        } catch (IdRepoException ire) {
            assertThat(ire).hasMessage(getIdRepoExceptionMessage(
                    IdRepoErrorCode.SERVICES_NOT_SUPPORTED_FOR_AGENTS_AND_GROUPS, DJLDAPv3Repo.class.getName()));
        }
    }

    @Test(dependsOnMethods = "assignServiceToUserIsSuccessful")
    public void getAssignedServicesReturnsServicesAssigned() throws Exception {
        Map<String, Set<String>> lie = new HashMap<String, Set<String>>();
        lie.put("iPlanetAMSessionService", asSet("inetorgperson"));
        Set<String> assignedServices = idrepo.getAssignedServices(null, IdType.USER, DEMO, lie);
        assertThat(assignedServices).containsOnly("iPlanetAMSessionService");
    }

    @Test(dependsOnMethods = "assignServiceSucceedsForRealm")
    public void getAssignedServicesReturnsServicesAssignedToRealm() throws Exception {
        Set<String> assignedServices = idrepo.getAssignedServices(null, IdType.REALM, null, null);
        assertThat(assignedServices).containsOnly("iPlanetAMSessionService");
    }

    @Test(dependsOnMethods = "getAssignedServicesReturnsServicesAssigned")
    public void getServiceAttributesReturnsCorrectValuesForUser() throws Exception {
        Map<String, Set<String>> attrs = idrepo.getServiceAttributes(null, IdType.USER, DEMO, "iPlanetAMSessionService",
                asSet("manager"));
        assertThat(attrs).hasSize(1);
        assertThat(attrs.get("manager")).isNotNull().containsOnly("DemoBoss");
        attrs = idrepo.getServiceAttributes(null, IdType.USER, DEMO, "iPlanetAMSessionService",
                asSet("iplanet-am-session-max-session-time"));
        assertThat(attrs).hasSize(1);
        assertThat(attrs.get("iplanet-am-session-max-session-time")).isNotNull().containsOnly("10000");
        attrs = idrepo.getServiceAttributes(null, IdType.USER, DEMO, "iPlanetAMSessionService",
                asSet("iplanet-am-session-max-session-time", "iplanet-am-session-max-caching-time"));
        assertThat(attrs).hasSize(2);
        assertThat(attrs.get("iplanet-am-session-max-session-time")).isNotNull().containsOnly("10000");
        assertThat(attrs.get("iplanet-am-session-max-caching-time")).isNotNull().containsOnly("10");
    }

    @Test(dependsOnMethods = "getAssignedServicesReturnsServicesAssignedToRealm")
    public void getServiceAttributesReturnsCorrectValuesForRealm() throws Exception {
        Map<String, Set<String>> attrs =
                idrepo.getServiceAttributes(null, IdType.REALM, null, "iPlanetAMSessionService", null);
        assertThat(attrs).hasSize(1);
        assertThat(attrs.get("iplanet-am-session-max-session-time")).isNotNull().containsOnly("10000");
        attrs = idrepo.getServiceAttributes(null, IdType.REALM, null, "invalid", null);
        assertThat(attrs).isEmpty();
        attrs = idrepo.getServiceAttributes(null, IdType.REALM, null, "iPlanetAMSessionService", asSet("invalid"));
        assertThat(attrs).isEmpty();
        attrs = idrepo.getServiceAttributes(null, IdType.REALM, null, "iPlanetAMSessionService",
                asSet("iplanet-am-session-max-session-time"));
        assertThat(attrs).hasSize(1);
        assertThat(attrs.get("iplanet-am-session-max-session-time")).isNotNull().containsOnly("10000");
    }

    @Test(dependsOnMethods = "getServiceAttributesReturnsCorrectValuesForUser")
    public void getBinaryServiceAttributesReturnsCorrectValuesForUser() throws Exception {
        Map<String, byte[][]> attrs =
                idrepo.getBinaryServiceAttributes(null, IdType.USER, DEMO,"iPlanetAMSessionService", asSet("manager"));
        assertThat(attrs).hasSize(1);
        byte[][] values = attrs.get("manager");
        assertThat(values[0]).isEqualTo("DemoBoss".getBytes(Charset.forName("UTF-8")));
        attrs = idrepo.getBinaryServiceAttributes(null, IdType.USER, DEMO, "iPlanetAMSessionService",
                asSet("iplanet-am-session-max-session-time"));
        assertThat(attrs).hasSize(1);
        values = attrs.get("iplanet-am-session-max-session-time");
        assertThat(values[0]).isEqualTo("10000".getBytes(Charset.forName("UTF-8")));
        attrs = idrepo.getBinaryServiceAttributes(null, IdType.USER, DEMO, "iPlanetAMSessionService",
                asSet("iplanet-am-session-max-session-time", "iplanet-am-session-max-caching-time"));
        assertThat(attrs).hasSize(2);
        values = attrs.get("iplanet-am-session-max-session-time");
        assertThat(values[0]).isEqualTo("10000".getBytes(Charset.forName("UTF-8")));
        values = attrs.get("iplanet-am-session-max-caching-time");
        assertThat(values[0]).isEqualTo("10".getBytes(Charset.forName("UTF-8")));
    }

    @Test(dependsOnMethods = "getBinaryServiceAttributesReturnsCorrectValuesForUser")
    public void modifyServiceForUserIsSuccessful() throws Exception {
        Map<String, Set<String>> attrs =
                idrepo.getServiceAttributes(null, IdType.USER, DEMO, "iPlanetAMSessionService", asSet("manager"));
        assertThat(attrs).hasSize(1);
        assertThat(attrs.get("manager")).isNotNull().containsOnly("DemoBoss");
        Map<String, Set<String>> changes = new HashMap<String, Set<String>>();
        changes.put("manager", asSet("NonDemoBoss"));
        idrepo.modifyService(null, IdType.USER, DEMO, "iPlanetAMSessionService", SchemaType.USER, changes);
        attrs = idrepo.getServiceAttributes(null, IdType.USER, DEMO, "iPlanetAMSessionService", asSet("manager"));
        assertThat(attrs).hasSize(1);
        assertThat(attrs.get("manager")).isNotNull().containsOnly("NonDemoBoss");
    }

    @Test(dependsOnMethods = "getAssignedServicesReturnsServicesAssignedToRealm")
    public void modifyServiceForRealmIsSuccessful() throws Exception {
        //verify initial state
        Map<String, Set<String>> attrs =
                idrepo.getServiceAttributes(null, IdType.REALM, null, "iPlanetAMSessionService", null);
        assertThat(attrs).hasSize(1);
        assertThat(attrs.get("iplanet-am-session-max-session-time")).isNotNull().containsOnly("10000");

        //perform changes
        Map<String, Set<String>> changes = new HashMap<String, Set<String>>();
        changes.put("iplanet-am-session-max-caching-time", asSet("15"));
        idrepo.modifyService(null, IdType.REALM, null, "iPlanetAMSessionService", SchemaType.USER, changes);
        ArgumentCaptor<Map> argument = ArgumentCaptor.forClass(Map.class);
        verify(idRepoListener, times(1)).setServiceAttributes(eq("iPlanetAMSessionService"), argument.capture());
        assertThat(argument.getValue().keySet()).contains("iPlanetAMSessionService");
        changes.put("iplanet-am-session-max-session-time", asSet("10000"));
        assertThat(argument.getValue().get("iPlanetAMSessionService")).isEqualTo(changes);

        attrs = idrepo.getServiceAttributes(null, IdType.REALM, null, "iPlanetAMSessionService", null);
        assertThat(attrs).hasSize(2);
        assertThat(attrs.get("iplanet-am-session-max-session-time")).isNotNull().containsOnly("10000");
        assertThat(attrs.get("iplanet-am-session-max-caching-time")).isNotNull().containsOnly("15");
        changes.clear();
        changes.put("iplanet-am-session-max-caching-time", new HashSet<String>());
        idrepo.modifyService(null, IdType.REALM, null, "iPlanetAMSessionService", SchemaType.USER, changes);
        assertThat(attrs).hasSize(2);
        assertThat(attrs.get("iplanet-am-session-max-session-time")).isNotNull().containsOnly("10000");
        assertThat(attrs.get("iplanet-am-session-max-caching-time")).isNotNull().isEmpty();
    }

    @Test(enabled = false)
    public void unassignServiceFromUserIsSuccessful() throws Exception {
        //cannot be enabled just yet as it would require more complicated schema setup
    }

    @Test(dependsOnMethods = "modifyServiceForRealmIsSuccessful")
    public void unassignServiceFromRealmIsSuccessful() throws Exception {
        Set<String> assignedServices = idrepo.getAssignedServices(null, IdType.REALM, null, null);
        assertThat(assignedServices).isNotNull().containsOnly("iPlanetAMSessionService");
        idrepo.unassignService(null, IdType.REALM, null, "iPlanetAMSessionService", null);
        assignedServices = idrepo.getAssignedServices(null, IdType.REALM, null, null);
        assertThat(assignedServices).isNotNull().isEmpty();
    }

    @Test
    public void testConstructFilterWithSpecialCharactersWithoutWildcards() throws Exception {
        Map<String, Set<String>> attrs = new HashMap<String, Set<String>>(1);
        attrs.put("cn", asSet("()\\\0"));
        Filter filter = idrepo.constructFilter(IdRepo.AND_MOD, attrs);
        assertThat(filter.toString()).isEqualTo("(&(cn=\\28\\29\\5C\\00))");
    }

    @Test
    public void testConstructFilterWithMultipleAssertions() throws Exception {
        Map<String, Set<String>> attrs = new HashMap<String, Set<String>>(1);
        attrs.put("cn", asOrderedSet("()\\\0", "hello"));
        Filter filter = idrepo.constructFilter(IdRepo.AND_MOD, attrs);
        assertThat(filter.toString()).isEqualTo("(&(cn=\\28\\29\\5C\\00)(cn=hello))");
    }

    @Test
    public void testConstructFilterWithWildcards() throws Exception {
        Map<String, Set<String>> attrs = new HashMap<String, Set<String>>(1);
        attrs.put("cn", asOrderedSet("()\\\0", "*w(o)rld*"));
        Filter filter = idrepo.constructFilter(IdRepo.OR_MOD, attrs);
        assertThat(filter.toString()).isEqualTo("(|(cn=\\28\\29\\5C\\00)(cn=*w\\28o\\29rld*))");
    }

    @Test
    public void exceptionContainsLDAPErrorCode() throws Exception {
        try {
            idrepo.getAttributes(null, IdType.USER, "badger");
            fail();
        } catch (IdRepoException ire) {
            assertThat(ire.getLDAPErrorCode()).isNotNull().isEqualTo(
                    String.valueOf(ResultCode.CLIENT_SIDE_NO_RESULTS_RETURNED.intValue()));
        }
    }

    @ Test (expectedExceptions = IllegalStateException.class)
    public void shouldThrowExceptionIfListenerAlreadyExists() {
        IdRepoListener newIdRepoListener = PowerMockito.mock(IdRepoListener.class);
        idrepo.addListener(null, newIdRepoListener);
    }

    @Test
    public void removeListenerWithPSearch() {
        assertThat(idrepo.getPsearchMap()).hasSize(1);
        idrepo.removeListener();
        assertThat(idrepo.getPsearchMap()).hasSize(0);
    }

}
