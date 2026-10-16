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
 * Copyright 2026 3A Systems, LLC.
 */

package com.sun.identity.entitlement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.json.JSONObject;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sun.identity.entitlement.opensso.OpenSSOPrivilege;

/**
 * Tests the reflective {@code className} handling on the {@link Privilege} DataStore round-trip
 * (GHSA-573r-mwh6-jw8j): the top-level privilege class itself, and the nested subject, condition and
 * resource-attribute members that {@link Privilege#getInstance(JSONObject)} rebuilds.
 * <p>
 * The privilege site is the one place where the resolver's expected type is an <em>abstract</em>
 * class ({@link Privilege}) rather than an interface, so the instantiability check is exercised
 * against a class hierarchy here.
 * <p>
 * {@code singleThreaded} because the tests assert on the shared {@link GadgetProbe} static state.
 */
@Test(singleThreaded = true)
public class PrivilegeReflectionTest {

    private static final String GADGET = "com.sun.identity.entitlement.NonEntitlementGadget";
    private static final String PRIVILEGE_CLASS = "com.sun.identity.entitlement.opensso.OpenSSOPrivilege";

    @BeforeMethod
    public void setUp() {
        GadgetProbe.reset();
    }

    private static JSONObject privilegeJson(String className) throws Exception {
        JSONObject jo = new JSONObject();
        jo.put("className", className);
        jo.put("name", "testPrivilege");
        jo.put("active", "true");
        return jo;
    }

    private static JSONObject member(String className, String state) throws Exception {
        JSONObject member = new JSONObject();
        member.put("className", className);
        member.put("state", state);
        return member;
    }

    @Test
    public void concretePrivilegeSubclassStillRoundTrips() throws Exception {
        Privilege privilege = Privilege.getInstance(privilegeJson(PRIVILEGE_CLASS));

        assertThat(privilege).isInstanceOf(OpenSSOPrivilege.class);
        assertThat(privilege.getName()).isEqualTo("testPrivilege");
    }

    @Test
    public void rejectsGadgetPrivilegeClassWithoutConstructingIt() throws Exception {
        Privilege privilege = Privilege.getInstance(privilegeJson(GADGET));

        assertThat(privilege).isNull();
        assertThat(GadgetProbe.staticInitialised).isFalse();
        assertThat(GadgetProbe.constructed).isFalse();
    }

    @Test
    public void rejectsAbstractPrivilegeClass() throws Exception {
        // Privilege itself is abstract: the expected type must not be instantiable as its own name.
        assertThat(Privilege.getInstance(privilegeJson("com.sun.identity.entitlement.Privilege")))
                .isNull();
    }

    @Test
    public void legitimateNestedMembersStillRoundTrip() throws Exception {
        JSONObject jo = privilegeJson(PRIVILEGE_CLASS);
        jo.put("eSubject", member("com.sun.identity.entitlement.NoSubject", "{}"));
        jo.put("eCondition", member("com.sun.identity.entitlement.OrCondition", "{}"));

        Privilege privilege = Privilege.getInstance(jo);

        assertThat(privilege).isNotNull();
        assertThat(privilege.getSubject()).isInstanceOf(NoSubject.class);
        assertThat(privilege.getCondition()).isInstanceOf(OrCondition.class);
    }

    @Test
    public void rejectsGadgetNestedSubjectWithoutConstructingIt() throws Exception {
        JSONObject jo = privilegeJson(PRIVILEGE_CLASS);
        jo.put("eSubject", member(GADGET, "{}"));

        Privilege privilege = Privilege.getInstance(jo);

        assertThat(privilege).isNotNull();
        assertThat(privilege.getSubject()).isNull();
        // A null subject matches everyone, so the refusal has to be recorded or the privilege comes
        // back applying to exactly the subjects its declaration was meant to keep out.
        assertThat(privilege.isSubjectUnresolved()).isTrue();
        assertThat(GadgetProbe.staticInitialised).isFalse();
        assertThat(GadgetProbe.constructed).isFalse();
    }

    @Test
    public void privilegeWithAnUnresolvableSubjectDeniesInsteadOfMatchingEveryone() throws Exception {
        JSONObject jo = privilegeJson(PRIVILEGE_CLASS);
        jo.put("eSubject", member(GADGET, "{}"));

        Privilege privilege = Privilege.getInstance(jo);

        assertThat(privilege.doesSubjectMatch(null, "/", null, "res", null).isSatisfied()).isFalse();
        assertThat(GadgetProbe.staticInitialised).isFalse();
        assertThat(GadgetProbe.constructed).isFalse();
    }

    @Test
    public void privilegeWithAnUnresolvableConditionFailsInsteadOfBeingSatisfied() throws Exception {
        JSONObject jo = privilegeJson(PRIVILEGE_CLASS);
        jo.put("eCondition", member(GADGET, "{}"));

        Privilege privilege = Privilege.getInstance(jo);

        assertThat(privilege.doesConditionMatch("/", null, "res", null).isSatisfied()).isFalse();
        assertThat(GadgetProbe.staticInitialised).isFalse();
        assertThat(GadgetProbe.constructed).isFalse();
    }

    @Test
    public void unresolvableSubjectSurvivesAStoreRoundTrip() throws Exception {
        JSONObject jo = privilegeJson(PRIVILEGE_CLASS);
        jo.put("eSubject", member(GADGET, "{}"));

        // The declaration is written back out, so a re-save neither loses it nor rewrites the
        // privilege into one that never had a subject; the next load refuses it again.
        String stored = Privilege.getInstance(jo).toJSONObject().toString();
        assertThat(stored).contains(GADGET);

        Privilege reloaded = Privilege.getInstance(new JSONObject(stored));

        assertThat(reloaded.getSubject()).isNull();
        assertThat(reloaded.isSubjectUnresolved()).isTrue();
        assertThat(reloaded.doesSubjectMatch(null, "/", null, "res", null).isSatisfied()).isFalse();
        assertThat(GadgetProbe.staticInitialised).isFalse();
        assertThat(GadgetProbe.constructed).isFalse();
    }

    @Test
    public void unresolvableConditionSurvivesAStoreRoundTrip() throws Exception {
        JSONObject jo = privilegeJson(PRIVILEGE_CLASS);
        jo.put("eCondition", member(GADGET, "{}"));

        String stored = Privilege.getInstance(jo).toJSONObject().toString();
        assertThat(stored).contains(GADGET);

        Privilege reloaded = Privilege.getInstance(new JSONObject(stored));

        assertThat(reloaded.getCondition()).isNull();
        assertThat(reloaded.isConditionUnresolved()).isTrue();
        assertThat(reloaded.doesConditionMatch("/", null, "res", null).isSatisfied()).isFalse();
        assertThat(GadgetProbe.staticInitialised).isFalse();
        assertThat(GadgetProbe.constructed).isFalse();
    }

    @Test
    public void privilegeDeclaringNoSubjectOrConditionIsUnaffected() throws Exception {
        Privilege privilege = Privilege.getInstance(privilegeJson(PRIVILEGE_CLASS));

        // Nothing was declared, so nothing was refused: the guard must not turn every privilege
        // without an explicit subject or condition into a deny.
        assertThat(privilege.isSubjectUnresolved()).isFalse();
        assertThat(privilege.isConditionUnresolved()).isFalse();
        assertThat(privilege.getSubject()).isInstanceOf(NoSubject.class);
        assertThat(privilege.doesConditionMatch("/", null, "res", null).isSatisfied()).isTrue();
        assertThat(privilege.toJSONObject().has("eCondition")).isFalse();
    }

    @Test
    public void resolvableSubjectClearsNothingOnReSave() throws Exception {
        JSONObject jo = privilegeJson(PRIVILEGE_CLASS);
        jo.put("eSubject", member("com.sun.identity.entitlement.NoSubject", "{}"));

        Privilege privilege = Privilege.getInstance(jo);
        Privilege reloaded = Privilege.getInstance(new JSONObject(privilege.toJSONObject().toString()));

        assertThat(reloaded.isSubjectUnresolved()).isFalse();
        assertThat(reloaded.getSubject()).isInstanceOf(NoSubject.class);
    }

    @Test
    public void rejectsGadgetNestedConditionWithoutConstructingIt() throws Exception {
        JSONObject jo = privilegeJson(PRIVILEGE_CLASS);
        jo.put("eCondition", member(GADGET, "{}"));

        Privilege privilege = Privilege.getInstance(jo);

        assertThat(privilege).isNotNull();
        assertThat(privilege.getCondition()).isNull();
        // A null condition is unconditionally satisfied, so the same applies on this side.
        assertThat(privilege.isConditionUnresolved()).isTrue();
        assertThat(GadgetProbe.staticInitialised).isFalse();
        assertThat(GadgetProbe.constructed).isFalse();
    }

    @Test
    public void getNewInstanceRefusesASubjectWhoseClassIsNotAnEntitlementSubject() throws Exception {
        JSONObject jo = privilegeJson(PRIVILEGE_CLASS);
        jo.put("eSubject", member(GADGET, "{}"));

        // This is the write path behind POST/PUT /ws/1/entitlement/privilege: nothing is stored yet,
        // so there is no refusal to record and deny on - a subject that came back null would simply
        // be stored as a privilege that never had one, i.e. one that matches everyone.
        assertThatThrownBy(() -> Privilege.getNewInstance(jo))
                .isInstanceOf(EntitlementException.class)
                .satisfies(e -> assertThat(((EntitlementException) e).getErrorCode())
                        .isEqualTo(EntitlementException.POLICY_CLASS_CAST_EXCEPTION));
        assertThat(GadgetProbe.staticInitialised).isFalse();
        assertThat(GadgetProbe.constructed).isFalse();
    }

    @Test
    public void getNewInstanceRefusesAConditionWhoseClassIsNotAnEntitlementCondition()
            throws Exception {
        JSONObject jo = privilegeJson(PRIVILEGE_CLASS);
        jo.put("eCondition", member(GADGET, "{}"));

        assertThatThrownBy(() -> Privilege.getNewInstance(jo))
                .isInstanceOf(EntitlementException.class)
                .satisfies(e -> assertThat(((EntitlementException) e).getErrorCode())
                        .isEqualTo(EntitlementException.POLICY_CLASS_CAST_EXCEPTION));
        assertThat(GadgetProbe.staticInitialised).isFalse();
        assertThat(GadgetProbe.constructed).isFalse();
    }

    @Test
    public void getNewInstanceRefusesASubjectClassThatDoesNotExist() throws Exception {
        JSONObject jo = privilegeJson(PRIVILEGE_CLASS);
        jo.put("eSubject", member("com.example.NoSuchSubject", "{}"));

        assertThatThrownBy(() -> Privilege.getNewInstance(jo))
                .isInstanceOf(EntitlementException.class)
                .satisfies(e -> assertThat(((EntitlementException) e).getErrorCode())
                        .isEqualTo(EntitlementException.UNKNOWN_POLICY_CLASS));
    }

    @Test
    public void getNewInstanceStillAcceptsLegitimateMembers() throws Exception {
        JSONObject jo = privilegeJson(PRIVILEGE_CLASS);
        jo.put("eSubject", member("com.sun.identity.entitlement.NoSubject", "{}"));
        // A NOT over an empty OR: getNewInstance validates the condition it built, and the logical
        // wrappers refuse an empty member set, so the condition has to be one that validates.
        jo.put("eCondition", member("com.sun.identity.entitlement.NotCondition",
                "{\"memberECondition\":{\"className\":\"com.sun.identity.entitlement.OrCondition\","
                        + "\"state\":\"{}\"}}"));

        Privilege privilege = Privilege.getNewInstance(jo);

        assertThat(privilege.getSubject()).isInstanceOf(NoSubject.class);
        assertThat(privilege.getCondition()).isInstanceOf(NotCondition.class);
    }

    @Test
    public void getNewInstanceIsUnaffectedWhenNoSubjectOrConditionIsDeclared() throws Exception {
        Privilege privilege = Privilege.getNewInstance(privilegeJson(PRIVILEGE_CLASS));

        // Nothing was declared, so nothing was refused: the guard must not turn every privilege
        // written without an explicit subject or condition into a failed write.
        assertThat(privilege.getSubject()).isInstanceOf(NoSubject.class);
        assertThat(privilege.getCondition()).isNull();
    }

    @Test
    public void rejectsGadgetResourceAttributeWithoutConstructingIt() throws Exception {
        JSONObject jo = privilegeJson(PRIVILEGE_CLASS);
        jo.put("eResourceAttributes", new org.json.JSONArray()
                .put(member("com.sun.identity.entitlement.StaticAttributes", "{}"))
                .put(member(GADGET, "{}")));

        Privilege privilege = Privilege.getInstance(jo);

        assertThat(privilege).isNotNull();
        // The valid sibling survives; only the refused member is dropped.
        assertThat(privilege.getResourceAttributes()).hasSize(1);
        assertThat(privilege.getResourceAttributes().iterator().next())
                .isInstanceOf(StaticAttributes.class);
        assertThat(GadgetProbe.staticInitialised).isFalse();
        assertThat(GadgetProbe.constructed).isFalse();
    }
}
