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

import org.json.JSONObject;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * End-to-end tests for the nested {@code setState} de-serialization sinks that
 * GHSA-573r-mwh6-jw8j reported (CWE-470 / CWE-502). A logical wrapper
 * ({@link OrSubject}/{@link AndSubject}/{@link NotSubject}, {@link OrCondition}/{@link AndCondition}/
 * {@link NotCondition}) passes the outer allowlist because it genuinely is an
 * {@link EntitlementSubject}/{@link EntitlementCondition}; the attack smuggles an arbitrary class
 * name in the wrapper's nested {@code className}. These tests confirm that a nested member whose
 * class is not the expected entitlement type is neither loaded-with-init nor instantiated, while a
 * legitimate nested member still round-trips.
 * <p>
 * {@code singleThreaded} because the tests assert on the shared {@link GadgetProbe} static state.
 */
@Test(singleThreaded = true)
public class LogicalMemberSetStateTest {

    private static final String GADGET = "com.sun.identity.entitlement.NonEntitlementGadget";
    private static final String NO_SUBJECT = "com.sun.identity.entitlement.NoSubject";
    private static final String OR_CONDITION = "com.sun.identity.entitlement.OrCondition";
    private static final String AND_CONDITION = "com.sun.identity.entitlement.AndCondition";
    private static final String NOT_CONDITION = "com.sun.identity.entitlement.NotCondition";
    private static final String AND_SUBJECT = "com.sun.identity.entitlement.AndSubject";
    private static final String OR_SUBJECT = "com.sun.identity.entitlement.OrSubject";
    private static final String NOT_SUBJECT = "com.sun.identity.entitlement.NotSubject";

    @BeforeMethod
    public void setUp() {
        GadgetProbe.reset();
    }

    /**
     * A single nested member, as {@code setState} expects it: the member's own state is itself a
     * JSON <em>string</em>, so it has to be quoted and escaped rather than inlined.
     */
    private static String member(String className, String state) {
        return "{\"className\":\"" + className + "\",\"state\":" + JSONObject.quote(state) + "}";
    }

    private static void assertGadgetNeverTouched() {
        assertThat(GadgetProbe.staticInitialised).isFalse();
        assertThat(GadgetProbe.constructed).isFalse();
    }

    @Test
    public void logicalSubjectSetStateDropsNonSubjectMemberWithoutInstantiatingIt() {
        OrSubject subject = new OrSubject();

        subject.setState("{\"memberESubjects\":[{\"className\":\"" + GADGET + "\",\"state\":\"{}\"}]}");

        assertThat(subject.getESubjects()).isNullOrEmpty();
        assertGadgetNeverTouched();
    }

    @Test
    public void logicalSubjectSetStateAcceptsLegitimateMember() {
        OrSubject subject = new OrSubject();

        subject.setState("{\"memberESubjects\":[{\"className\":\"" + NO_SUBJECT + "\",\"state\":\"{}\"}]}");

        assertThat(subject.getESubjects()).hasSize(1);
        assertThat(subject.getESubjects().iterator().next()).isInstanceOf(NoSubject.class);
    }

    @Test
    public void logicalSubjectSetStateSkipsInvalidMemberButKeepsValidSiblings() {
        OrSubject subject = new OrSubject();

        subject.setState("{\"memberESubjects\":["
                + "{\"className\":\"" + NO_SUBJECT + "\",\"state\":\"{}\"},"
                + "{\"className\":\"" + GADGET + "\",\"state\":\"{}\"},"
                + "{\"className\":\"" + NO_SUBJECT + "\",\"state\":\"{}\"}]}");

        // The rejected middle member must not truncate the valid siblings around it.
        assertThat(subject.getESubjects()).hasSize(2);
        assertGadgetNeverTouched();
    }

    @Test
    public void logicalSubjectSetStatePreservesPSubjectName() {
        OrSubject subject = new OrSubject();

        subject.setState("{\"pSubjectName\":\"admins\",\"memberESubjects\":[]}");

        // Regression: setState previously read jo.optString(pSubjectName) (the null field as the
        // key) instead of the literal "pSubjectName", silently blanking the name.
        assertThat(subject.getPSubjectName()).isEqualTo("admins");
    }

    @Test
    public void notSubjectSetStateDropsNonSubjectMemberWithoutInstantiatingIt() {
        NotSubject subject = new NotSubject();

        subject.setState("{\"memberESubject\":{\"className\":\"" + GADGET + "\",\"state\":\"{}\"}}");

        assertThat(subject.getESubject()).isNull();
        assertGadgetNeverTouched();
    }

    @Test
    public void notSubjectSetStateAcceptsLegitimateMember() {
        NotSubject subject = new NotSubject();

        subject.setState("{\"memberESubject\":{\"className\":\"" + NO_SUBJECT + "\",\"state\":\"{}\"}}");

        assertThat(subject.getESubject()).isInstanceOf(NoSubject.class);
    }

    @Test
    public void logicalConditionSetStateDropsNonConditionMemberWithoutInstantiatingIt() {
        OrCondition condition = new OrCondition();

        condition.setState("{\"memberECondition\":[{\"className\":\"" + GADGET + "\",\"state\":\"{}\"}]}");

        assertThat(condition.getEConditions()).isNullOrEmpty();
        assertGadgetNeverTouched();
    }

    @Test
    public void logicalConditionSetStateAcceptsLegitimateMember() {
        OrCondition condition = new OrCondition();

        condition.setState("{\"memberECondition\":[{\"className\":\"" + OR_CONDITION + "\",\"state\":\"{}\"}]}");

        assertThat(condition.getEConditions()).hasSize(1);
        assertThat(condition.getEConditions().iterator().next()).isInstanceOf(OrCondition.class);
    }

    @Test
    public void logicalConditionSetStateSkipsInvalidMemberButKeepsValidSiblings() {
        OrCondition condition = new OrCondition();

        condition.setState("{\"memberECondition\":["
                + "{\"className\":\"" + OR_CONDITION + "\",\"state\":\"{}\"},"
                + "{\"className\":\"" + GADGET + "\",\"state\":\"{}\"}]}");

        // The rejected member must not truncate the valid sibling declared before it.
        assertThat(condition.getEConditions()).hasSize(1);
        assertGadgetNeverTouched();
    }

    @Test
    public void notConditionSetStateDropsNonConditionMemberWithoutInstantiatingIt() {
        NotCondition condition = new NotCondition();

        condition.setState("{\"memberECondition\":{\"className\":\"" + GADGET + "\",\"state\":\"{}\"}}");

        assertThat(condition.getECondition()).isNull();
        assertGadgetNeverTouched();
    }

    @Test
    public void notConditionSetStateAcceptsLegitimateMember() {
        NotCondition condition = new NotCondition();

        condition.setState("{\"memberECondition\":{\"className\":\"" + OR_CONDITION + "\",\"state\":\"{}\"}}");

        assertThat(condition.getECondition()).isInstanceOf(OrCondition.class);
    }

    @Test
    public void notSubjectSetStatePreservesPSubjectNameWhenMemberIsRejected() {
        NotSubject subject = new NotSubject();

        subject.setState("{\"pSubjectName\":\"admins\",\"memberESubject\":{\"className\":\""
                + GADGET + "\",\"state\":\"{}\"}}");

        // The name is read before the member load, so a refused member does not also blank it.
        assertThat(subject.getPSubjectName()).isEqualTo("admins");
        assertThat(subject.getESubject()).isNull();
        assertGadgetNeverTouched();
    }

    @Test
    public void andSubjectWithRejectedMemberDeniesInsteadOfGranting() throws Exception {
        AndSubject subject = new AndSubject();

        subject.setState("{\"memberESubjects\":[{\"className\":\"" + GADGET + "\",\"state\":\"{}\"}]}");

        // An empty AND member set is satisfied, so dropping the only member would turn a restrictive
        // policy into a grant-to-everyone. The refusal must be remembered and evaluate must deny.
        assertThat(subject.isMemberRejected()).isTrue();
        assertThat(subject.evaluate("/", null, null, "res", null).isSatisfied()).isFalse();
        assertGadgetNeverTouched();
    }

    @Test
    public void andSubjectWithoutRejectedMemberKeepsExistingSemantics() throws Exception {
        AndSubject subject = new AndSubject();

        subject.setState("{\"memberESubjects\":[]}");

        assertThat(subject.isMemberRejected()).isFalse();
        assertThat(subject.evaluate("/", null, null, "res", null).isSatisfied()).isTrue();
    }

    @Test
    public void andConditionWithRejectedMemberFailsInsteadOfSucceeding() throws Exception {
        AndCondition condition = new AndCondition();

        condition.setState("{\"memberECondition\":[{\"className\":\"" + GADGET + "\",\"state\":\"{}\"}]}");

        assertThat(condition.isMemberRejected()).isTrue();
        assertThat(condition.evaluate("/", null, "res", null).isSatisfied()).isFalse();
        assertGadgetNeverTouched();
    }

    @Test
    public void andConditionWithoutRejectedMemberKeepsExistingSemantics() throws Exception {
        AndCondition condition = new AndCondition();

        condition.setState("{\"memberECondition\":[]}");

        assertThat(condition.isMemberRejected()).isFalse();
        assertThat(condition.evaluate("/", null, "res", null).isSatisfied()).isTrue();
    }

    @Test
    public void settingMembersProgrammaticallyClearsTheRejectionFlag() {
        AndSubject subject = new AndSubject();
        subject.setState("{\"memberESubjects\":[{\"className\":\"" + GADGET + "\",\"state\":\"{}\"}]}");
        assertThat(subject.isMemberRejected()).isTrue();

        subject.setESubjects(java.util.Collections.<EntitlementSubject>singleton(new NoSubject()));

        assertThat(subject.isMemberRejected()).isFalse();
    }

    @Test
    public void reapplyingAStateWithoutMembersClearsTheRejectionFlag() {
        AndSubject subject = new AndSubject();
        subject.setState("{\"memberESubjects\":[{\"className\":\"" + GADGET + "\",\"state\":\"{}\"}]}");
        assertThat(subject.isMemberRejected()).isTrue();

        // The state being applied fully redefines the object, so the reset must not sit inside the
        // "are there members?" branch: a prior refusal must not leak into an unrelated state.
        subject.setState("{}");

        assertThat(subject.isMemberRejected()).isFalse();
    }

    @Test
    public void orConditionWithEveryMemberRejectedFailsInsteadOfSucceeding() throws Exception {
        OrCondition condition = new OrCondition();

        condition.setState("{\"memberECondition\":[{\"className\":\"" + GADGET + "\",\"state\":\"{}\"}]}");

        // Unlike OrSubject, which denies on an empty member set, an empty OrCondition is satisfied -
        // so dropping the only member would turn the stored restriction into an unconditional grant.
        assertThat(condition.isMemberRejected()).isTrue();
        assertThat(condition.evaluate("/", null, "res", null).isSatisfied()).isFalse();
        assertGadgetNeverTouched();
    }

    @Test
    public void orConditionWithASurvivingMemberStillEvaluatesTheRemainder() throws Exception {
        OrCondition condition = new OrCondition();

        condition.setState("{\"memberECondition\":["
                + "{\"className\":\"" + OR_CONDITION + "\",\"state\":\"{}\"},"
                + "{\"className\":\"" + GADGET + "\",\"state\":\"{}\"}]}");

        // Only the emptied case is unsafe: dropping a member from a non-empty OR can only make it
        // stricter, so the surviving member (an empty, hence satisfied, OrCondition) still decides.
        assertThat(condition.isMemberRejected()).isTrue();
        assertThat(condition.getEConditions()).hasSize(1);
        assertThat(condition.evaluate("/", null, "res", null).isSatisfied()).isTrue();
        assertGadgetNeverTouched();
    }

    @Test
    public void orConditionWithoutRejectedMemberKeepsExistingSemantics() throws Exception {
        OrCondition condition = new OrCondition();

        condition.setState("{\"memberECondition\":[]}");

        assertThat(condition.isMemberRejected()).isFalse();
        assertThat(condition.evaluate("/", null, "res", null).isSatisfied()).isTrue();
    }

    @Test
    public void orSubjectWithEveryMemberRejectedDenies() throws Exception {
        OrSubject subject = new OrSubject();

        subject.setState("{\"memberESubjects\":[{\"className\":\"" + GADGET + "\",\"state\":\"{}\"}]}");

        // OrSubject needs no explicit guard: an empty member set already denies.
        assertThat(subject.evaluate("/", null, null, "res", null).isSatisfied()).isFalse();
        assertGadgetNeverTouched();
    }

    @Test
    public void andSubjectKeepsDenyingAfterTheStateIsStoredAndReloaded() throws Exception {
        AndSubject subject = new AndSubject();
        subject.setState("{\"memberESubjects\":[{\"className\":\"" + GADGET + "\",\"state\":\"{}\"}]}");

        // What a re-save (ResavePoliciesStep, or any other read-modify-write) would store.
        String stored = subject.getState();
        assertThat(stored).contains(GADGET);

        AndSubject reloaded = new AndSubject();
        reloaded.setState(stored);

        // Without the refused member in the stored state, the reloaded wrapper would be an empty
        // AND - which is satisfied, i.e. a grant to everyone.
        assertThat(reloaded.isMemberRejected()).isTrue();
        assertThat(reloaded.evaluate("/", null, null, "res", null).isSatisfied()).isFalse();
        assertGadgetNeverTouched();
    }

    @Test
    public void andConditionKeepsFailingAfterTheStateIsStoredAndReloaded() throws Exception {
        AndCondition condition = new AndCondition();
        condition.setState("{\"memberECondition\":[{\"className\":\"" + GADGET + "\",\"state\":\"{}\"}]}");

        String stored = condition.getState();
        assertThat(stored).contains(GADGET);

        AndCondition reloaded = new AndCondition();
        reloaded.setState(stored);

        assertThat(reloaded.isMemberRejected()).isTrue();
        assertThat(reloaded.evaluate("/", null, "res", null).isSatisfied()).isFalse();
        assertGadgetNeverTouched();
    }

    @Test
    public void orConditionKeepsFailingAfterTheStateIsStoredAndReloaded() throws Exception {
        OrCondition condition = new OrCondition();
        condition.setState("{\"memberECondition\":[{\"className\":\"" + GADGET + "\",\"state\":\"{}\"}]}");

        OrCondition reloaded = new OrCondition();
        reloaded.setState(condition.getState());

        assertThat(reloaded.isMemberRejected()).isTrue();
        assertThat(reloaded.evaluate("/", null, "res", null).isSatisfied()).isFalse();
        assertGadgetNeverTouched();
    }

    @Test
    public void storedStateKeepsBothTheSurvivingAndTheRefusedMembers() {
        AndSubject subject = new AndSubject();
        subject.setState("{\"memberESubjects\":["
                + "{\"className\":\"" + NO_SUBJECT + "\",\"state\":\"{}\"},"
                + "{\"className\":\"" + GADGET + "\",\"state\":\"{}\"}]}");

        String stored = subject.getState();

        // The refused member is written back verbatim, so re-saving the policy does not quietly
        // rewrite it into a weaker one. Its class is still never loaded initialised nor instantiated.
        assertThat(stored).contains(NO_SUBJECT);
        assertThat(stored).contains(GADGET);

        AndSubject reloaded = new AndSubject();
        reloaded.setState(stored);
        assertThat(reloaded.getESubjects()).hasSize(1);
        assertThat(reloaded.isMemberRejected()).isTrue();
        assertGadgetNeverTouched();
    }

    @Test
    public void notConditionWrappingARejectedAndConditionFailsInsteadOfNegatingIt() throws Exception {
        NotCondition condition = new NotCondition();

        condition.setState("{\"memberECondition\":" + member(AND_CONDITION,
                "{\"memberECondition\":[" + member(GADGET, "{}") + "]}") + "}");

        // The nested AND fails closed because its only member was refused. Negating that failure
        // would turn the wrapper into an unconditional grant - the very restriction the refusal
        // removed. NOT has to fail closed itself instead.
        assertThat(condition.getECondition()).isInstanceOf(AndCondition.class);
        assertThat(((AndCondition) condition.getECondition()).isMemberRejected()).isTrue();
        assertThat(condition.hasRejectedMemberInSubtree()).isTrue();
        assertThat(condition.evaluate("/", null, "res", null).isSatisfied()).isFalse();
        assertGadgetNeverTouched();
    }

    @Test
    public void notConditionFailsWhenTheRefusalIsDeeperInTheSubtree() throws Exception {
        NotCondition condition = new NotCondition();

        // NOT -> OR -> AND(refused): the OR itself is intact, it simply evaluates a member that
        // fails closed, so only a look at the whole subtree catches this one.
        String damagedAnd = "{\"memberECondition\":[" + member(GADGET, "{}") + "]}";
        String orOverDamagedAnd = "{\"memberECondition\":[" + member(AND_CONDITION, damagedAnd) + "]}";
        condition.setState("{\"memberECondition\":" + member(OR_CONDITION, orOverDamagedAnd) + "}");

        assertThat(condition.hasRejectedMemberInSubtree()).isTrue();
        assertThat(condition.evaluate("/", null, "res", null).isSatisfied()).isFalse();
        assertGadgetNeverTouched();
    }

    @Test
    public void notConditionStillNegatesAnUndamagedMember() throws Exception {
        NotCondition condition = new NotCondition();

        // NOT(NOT(empty OR)): the empty OR is satisfied, so the inner NOT is not, so the outer NOT
        // is - the guard must not collapse every NOT into a deny.
        String innerNot = "{\"memberECondition\":" + member(OR_CONDITION, "{}") + "}";
        condition.setState("{\"memberECondition\":" + member(NOT_CONDITION, innerNot) + "}");

        assertThat(condition.hasRejectedMemberInSubtree()).isFalse();
        assertThat(condition.evaluate("/", null, "res", null).isSatisfied()).isTrue();
    }

    @Test
    public void notSubjectWrappingARejectedAndSubjectDeniesInsteadOfNegatingIt() throws Exception {
        NotSubject subject = new NotSubject();

        subject.setState("{\"memberESubject\":" + member(AND_SUBJECT,
                "{\"memberESubjects\":[" + member(GADGET, "{}") + "]}") + "}");

        // Same inversion on the subject side: the nested AND denies, and negating that deny would
        // match everyone the stored policy meant to exclude.
        assertThat(subject.getESubject()).isInstanceOf(AndSubject.class);
        assertThat(((AndSubject) subject.getESubject()).isMemberRejected()).isTrue();
        assertThat(subject.hasRejectedMemberInSubtree()).isTrue();
        assertThat(subject.evaluate("/", null, null, "res", null).isSatisfied()).isFalse();
        assertGadgetNeverTouched();
    }

    @Test
    public void notSubjectStillNegatesAnUndamagedMember() throws Exception {
        NotSubject subject = new NotSubject();

        // An empty OrSubject denies, so the NOT around it must still be satisfied.
        subject.setState("{\"memberESubject\":" + member(OR_SUBJECT, "{}") + "}");

        assertThat(subject.hasRejectedMemberInSubtree()).isFalse();
        assertThat(subject.evaluate("/", null, null, "res", null).isSatisfied()).isTrue();
    }

    @Test
    public void notConditionKeepsFailingAfterTheStateIsStoredAndReloaded() throws Exception {
        NotCondition condition = new NotCondition();
        condition.setState("{\"memberECondition\":" + member(AND_CONDITION,
                "{\"memberECondition\":[" + member(GADGET, "{}") + "]}") + "}");

        // The nested AND writes its refused member back out, so the refusal survives a re-save and
        // the reloaded NOT still fails closed.
        String stored = condition.getState();
        assertThat(stored).contains(GADGET);

        NotCondition reloaded = new NotCondition();
        reloaded.setState(stored);

        assertThat(reloaded.hasRejectedMemberInSubtree()).isTrue();
        assertThat(reloaded.evaluate("/", null, "res", null).isSatisfied()).isFalse();
        assertGadgetNeverTouched();
    }

    @Test
    public void notConditionOverANotConditionWhoseMemberWasRefusedFailsInsteadOfNegatingIt()
            throws Exception {
        NotCondition condition = new NotCondition();

        // NOT(NOT(refused)): the inner NOT fails closed because its own member was refused, and
        // negating that failure hands back the grant the refusal was meant to withhold. Unlike the
        // AND/OR case the inner wrapper keeps no member at all, so nothing but the NOT's own
        // emptiness records the damage.
        String innerNot = "{\"memberECondition\":" + member(GADGET, "{}") + "}";
        condition.setState("{\"memberECondition\":" + member(NOT_CONDITION, innerNot) + "}");

        assertThat(condition.getECondition()).isInstanceOf(NotCondition.class);
        assertThat(((NotCondition) condition.getECondition()).isMemberRejected()).isTrue();
        assertThat(condition.hasRejectedMemberInSubtree()).isTrue();
        assertThat(condition.evaluate("/", null, "res", null).isSatisfied()).isFalse();
        assertGadgetNeverTouched();
    }

    @Test
    public void notSubjectOverANotSubjectWhoseMemberWasRefusedDeniesInsteadOfNegatingIt()
            throws Exception {
        NotSubject subject = new NotSubject();

        String innerNot = "{\"memberESubject\":" + member(GADGET, "{}") + "}";
        subject.setState("{\"memberESubject\":" + member(NOT_SUBJECT, innerNot) + "}");

        assertThat(subject.getESubject()).isInstanceOf(NotSubject.class);
        assertThat(((NotSubject) subject.getESubject()).isMemberRejected()).isTrue();
        assertThat(subject.hasRejectedMemberInSubtree()).isTrue();
        assertThat(subject.evaluate("/", null, null, "res", null).isSatisfied()).isFalse();
        assertGadgetNeverTouched();
    }

    @Test
    public void notConditionFailsWhenARefusedNotSitsUnderAnIntactAnd() throws Exception {
        NotCondition condition = new NotCondition();

        // NOT -> AND -> NOT(refused): the AND is intact and merely evaluates a member that fails
        // closed, so the refusal is only visible by walking the whole subtree.
        String innerNot = "{\"memberECondition\":" + member(GADGET, "{}") + "}";
        String andOverInnerNot = "{\"memberECondition\":[" + member(NOT_CONDITION, innerNot) + "]}";
        condition.setState("{\"memberECondition\":" + member(AND_CONDITION, andOverInnerNot) + "}");

        assertThat(condition.hasRejectedMemberInSubtree()).isTrue();
        assertThat(condition.evaluate("/", null, "res", null).isSatisfied()).isFalse();
        assertGadgetNeverTouched();
    }

    @Test
    public void notConditionOverAMemberlessNotFailsClosed() throws Exception {
        NotCondition condition = new NotCondition();

        // What a policy damaged by an earlier version looks like on disk: the refused member was
        // dropped rather than re-emitted, so the inner NOT comes back with no member at all and no
        // refusal to re-arm. It is still a damaged policy, and negating it is still a grant.
        condition.setState("{\"memberECondition\":" + member(NOT_CONDITION, "{}") + "}");

        assertThat(((NotCondition) condition.getECondition()).getECondition()).isNull();
        assertThat(condition.hasRejectedMemberInSubtree()).isTrue();
        assertThat(condition.evaluate("/", null, "res", null).isSatisfied()).isFalse();
    }

    @Test
    public void notSubjectOverAMemberlessNotDenies() throws Exception {
        NotSubject subject = new NotSubject();

        subject.setState("{\"memberESubject\":" + member(NOT_SUBJECT, "{}") + "}");

        assertThat(((NotSubject) subject.getESubject()).getESubject()).isNull();
        assertThat(subject.hasRejectedMemberInSubtree()).isTrue();
        assertThat(subject.evaluate("/", null, null, "res", null).isSatisfied()).isFalse();
    }

    @Test
    public void notConditionRefusalSurvivesAStoreRoundTrip() throws Exception {
        NotCondition condition = new NotCondition();
        condition.setState("{\"memberECondition\":" + member(GADGET, "{}") + "}");

        // The refused member is written back out, so a re-save neither loses the class name nor
        // rewrites the policy into one that merely looks memberless.
        String stored = condition.getState();
        assertThat(stored).contains(GADGET);

        NotCondition reloaded = new NotCondition();
        reloaded.setState(stored);

        assertThat(reloaded.getECondition()).isNull();
        assertThat(reloaded.isMemberRejected()).isTrue();
        assertThat(reloaded.evaluate("/", null, "res", null).isSatisfied()).isFalse();
        assertGadgetNeverTouched();
    }

    @Test
    public void notSubjectRefusalSurvivesAStoreRoundTrip() throws Exception {
        NotSubject subject = new NotSubject();
        subject.setState("{\"pSubjectName\":\"n\",\"memberESubject\":" + member(GADGET, "{}") + "}");

        String stored = subject.getState();
        assertThat(stored).contains(GADGET);

        NotSubject reloaded = new NotSubject();
        reloaded.setState(stored);

        assertThat(reloaded.getESubject()).isNull();
        assertThat(reloaded.isMemberRejected()).isTrue();
        assertThat(reloaded.getPSubjectName()).isEqualTo("n");
        assertGadgetNeverTouched();
    }

    @Test
    public void andSubjectDeniesWhenAMemberCarriesNoClassName() throws Exception {
        AndSubject subject = new AndSubject();

        // No class name to refuse, but the member is just as unusable. Reading it outside the
        // per-member guard used to throw out of the loop, dropping every member and recording no
        // refusal at all - an empty AndSubject, i.e. a match for everyone.
        subject.setState("{\"memberESubjects\":[{\"state\":\"{}\"}]}");

        assertThat(subject.getESubjects()).isNullOrEmpty();
        assertThat(subject.isMemberRejected()).isTrue();
        assertThat(subject.evaluate("/", null, null, "res", null).isSatisfied()).isFalse();
    }

    @Test
    public void andConditionFailsWhenAMemberCarriesNoState() throws Exception {
        AndCondition condition = new AndCondition();

        condition.setState("{\"memberECondition\":[{\"className\":\"" + OR_CONDITION + "\"}]}");

        assertThat(condition.getEConditions()).isNullOrEmpty();
        assertThat(condition.isMemberRejected()).isTrue();
        assertThat(condition.evaluate("/", null, "res", null).isSatisfied()).isFalse();
    }

    @Test
    public void malformedMemberDoesNotTruncateItsValidSiblings() {
        OrSubject subject = new OrSubject();

        subject.setState("{\"memberESubjects\":["
                + "{\"state\":\"{}\"},"
                + "{\"className\":\"" + NO_SUBJECT + "\",\"state\":\"{}\"}]}");

        assertThat(subject.getESubjects()).hasSize(1);
        assertThat(subject.isMemberRejected()).isTrue();
    }

    @Test
    public void malformedMemberRefusalSurvivesAStoreRoundTrip() {
        AndSubject subject = new AndSubject();
        subject.setState("{\"memberESubjects\":[{\"state\":\"{}\"}]}");

        AndSubject reloaded = new AndSubject();
        reloaded.setState(subject.getState());

        assertThat(reloaded.isMemberRejected()).isTrue();
    }

    @Test
    public void memberThatIsNotEvenAnObjectIsRefusedAndStaysRefusedAfterAReSave() {
        AndSubject subject = new AndSubject();
        subject.setState("{\"memberESubjects\":[\"not-an-object\"]}");

        assertThat(subject.isMemberRejected()).isTrue();

        // Nothing usable to re-emit, so an empty member takes its place - unresolvable in exactly
        // the same way, which is what keeps the reloaded wrapper fail-closed.
        AndSubject reloaded = new AndSubject();
        reloaded.setState(subject.getState());

        assertThat(reloaded.isMemberRejected()).isTrue();
    }

    @Test
    public void notSubjectRefusesAMemberWithoutStateAndKeepsRefusingItAfterAReSave() throws Exception {
        NotSubject subject = new NotSubject();

        // The class name resolves, so the member is constructed before its state is applied; it is
        // the missing "state" key that refuses it. Keeping that half-applied instance in the field
        // would let toJSONObject() write out an intact - and empty, i.e. never satisfied - member in
        // place of the refused declaration, and the reloaded NOT would negate it into a match.
        subject.setState("{\"memberESubject\":{\"className\":\"" + OR_SUBJECT + "\"}}");

        assertThat(subject.getESubject()).isNull();
        assertThat(subject.isMemberRejected()).isTrue();
        assertThat(subject.evaluate("/", null, null, "res", null).isSatisfied()).isFalse();

        NotSubject reloaded = new NotSubject();
        reloaded.setState(subject.getState());

        assertThat(reloaded.getESubject()).isNull();
        assertThat(reloaded.isMemberRejected()).isTrue();
        assertThat(reloaded.evaluate("/", null, null, "res", null).isSatisfied()).isFalse();
    }

    @Test
    public void notConditionRefusesAMemberWithoutStateAndKeepsRefusingItAfterAReSave()
            throws Exception {
        NotCondition condition = new NotCondition();
        condition.setState("{\"memberECondition\":{\"className\":\"" + OR_CONDITION + "\"}}");

        assertThat(condition.getECondition()).isNull();
        assertThat(condition.isMemberRejected()).isTrue();
        assertThat(condition.evaluate("/", null, "res", null).isSatisfied()).isFalse();

        NotCondition reloaded = new NotCondition();
        reloaded.setState(condition.getState());

        assertThat(reloaded.getECondition()).isNull();
        assertThat(reloaded.isMemberRejected()).isTrue();
        assertThat(reloaded.evaluate("/", null, "res", null).isSatisfied()).isFalse();
    }

    @Test
    public void notOverANotWhoseMemberHasNoStateKeepsFailingAfterAReSave() throws Exception {
        NotCondition condition = new NotCondition();

        // NOT(NOT(member without state)): the outer NOT fails closed on the first load because the
        // refusal is visible in the subtree. Should the inner NOT re-save the member it half
        // applied, nothing is left to refuse and the outer NOT negates an ordinary deny into a
        // grant - the enclosing-NOT shape, reached without a single unresolvable class name.
        String innerNot = "{\"memberECondition\":{\"className\":\"" + OR_CONDITION + "\"}}";
        condition.setState("{\"memberECondition\":" + member(NOT_CONDITION, innerNot) + "}");

        assertThat(condition.hasRejectedMemberInSubtree()).isTrue();
        assertThat(condition.evaluate("/", null, "res", null).isSatisfied()).isFalse();

        NotCondition reloaded = new NotCondition();
        reloaded.setState(condition.getState());

        assertThat(reloaded.hasRejectedMemberInSubtree()).isTrue();
        assertThat(reloaded.evaluate("/", null, "res", null).isSatisfied()).isFalse();
    }

    @Test
    public void notSubjectOverANotSubjectWhoseMemberHasNoStateKeepsDenyingAfterAReSave()
            throws Exception {
        NotSubject subject = new NotSubject();

        String innerNot = "{\"memberESubject\":{\"className\":\"" + OR_SUBJECT + "\"}}";
        subject.setState("{\"memberESubject\":" + member(NOT_SUBJECT, innerNot) + "}");

        assertThat(subject.hasRejectedMemberInSubtree()).isTrue();
        assertThat(subject.evaluate("/", null, null, "res", null).isSatisfied()).isFalse();

        NotSubject reloaded = new NotSubject();
        reloaded.setState(subject.getState());

        assertThat(reloaded.hasRejectedMemberInSubtree()).isTrue();
        assertThat(reloaded.evaluate("/", null, null, "res", null).isSatisfied()).isFalse();
    }

    @Test
    public void notSubjectDropsAnEarlierMemberWhenTheNextStateIsRefused() throws Exception {
        NotSubject subject = new NotSubject();
        subject.setState("{\"memberESubject\":" + member(NO_SUBJECT, "{}") + "}");
        assertThat(subject.getESubject()).isInstanceOf(NoSubject.class);

        // Re-applying a state fully redefines the object, so a refused member must not leave the
        // previous one in place: that stale member is what a re-save would store, and the refusal
        // would be gone from the policy altogether.
        subject.setState("{\"memberESubject\":" + member(GADGET, "{}") + "}");

        assertThat(subject.getESubject()).isNull();
        assertThat(subject.isMemberRejected()).isTrue();
        assertThat(subject.getState()).contains(GADGET).doesNotContain(NO_SUBJECT);
        assertGadgetNeverTouched();
    }

    @Test
    public void notConditionDropsAnEarlierMemberWhenTheNextStateIsRefused() throws Exception {
        NotCondition condition = new NotCondition();
        condition.setState("{\"memberECondition\":" + member(OR_CONDITION, "{}") + "}");
        assertThat(condition.getECondition()).isInstanceOf(OrCondition.class);

        condition.setState("{\"memberECondition\":" + member(GADGET, "{}") + "}");

        assertThat(condition.getECondition()).isNull();
        assertThat(condition.isMemberRejected()).isTrue();
        assertThat(condition.getState()).contains(GADGET).doesNotContain(OR_CONDITION);
        assertGadgetNeverTouched();
    }

    @Test
    public void notSubjectStillAcceptsAMemberThatFullyApplies() throws Exception {
        NotSubject subject = new NotSubject();
        subject.setState("{\"memberESubject\":" + member(NO_SUBJECT, "{}") + "}");

        // The member is assigned later than it used to be, so pin down that it is still assigned.
        assertThat(subject.getESubject()).isInstanceOf(NoSubject.class);
        assertThat(subject.isMemberRejected()).isFalse();
        assertThat(subject.hasRejectedMemberInSubtree()).isFalse();
    }

    @Test
    public void storedStateOfAWrapperWithoutRefusalsIsUnchanged() {
        AndSubject subject = new AndSubject();
        subject.setState("{\"memberESubjects\":[{\"className\":\"" + NO_SUBJECT + "\",\"state\":\"{}\"}]}");

        AndSubject reloaded = new AndSubject();
        reloaded.setState(subject.getState());

        // Nothing was refused, so nothing extra is written: the state round-trips as before.
        assertThat(reloaded.isMemberRejected()).isFalse();
        assertThat(reloaded.getESubjects()).hasSize(1);
        assertThat(reloaded.getState()).isEqualTo(subject.getState());
    }
}
