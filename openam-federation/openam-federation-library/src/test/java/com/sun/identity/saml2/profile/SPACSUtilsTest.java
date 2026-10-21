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

package com.sun.identity.saml2.profile;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import com.sun.identity.saml2.assertion.Assertion;
import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.Issuer;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;

/**
 * GHSA-3j2f-qhrc-rjfj: the one-time marker for a bearer assertion used to be written for the
 * HTTP-POST binding alone, at the very end of {@code SPACSUtils.processResponse} and never at all on
 * the assertion consumer service of the SAML2 authentication module. These cover the claim itself -
 * that it happens once, that it is atomic, that it reaches every bearer assertion of the response
 * rather than only the one the session is built from, and that a second holder of any of them is
 * turned away.
 */
public class SPACSUtilsTest {

    /**
     * {@code SPCache.assertionByIDCache} is static and outlives a test method, and the claim throws
     * on a second put - so an id left behind here would fail any later test that reused it. Written
     * from the worker threads of the contention test as well as from the test thread, hence the
     * concurrent set.
     */
    private final Set<String> claimedIDs =
            Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    @AfterMethod
    public void releaseTheClaimedAssertions() {
        for (String assertionID : claimedIDs) {
            SPCache.assertionByIDCache.remove(assertionID);
        }
        claimedIDs.clear();
    }

    /**
     * A response carrying one bearer assertion, which is also the one the session is built from.
     */
    private Map responseOf(String assertionID) throws SAML2Exception {
        return responseWithBearerAssertions(assertionID);
    }

    /**
     * A response as {@code SAML2Utils.verifyResponse} reports it: the assertion the session is
     * built from, plus every bearer assertion it accepted.
     */
    private Map responseWithBearerAssertions(String authnAssertionID,
            String... otherBearerAssertionIDs) throws SAML2Exception {
        Assertion assertion = AssertionFactory.getInstance().createAssertion();
        assertion.setID(authnAssertionID);
        claimedIDs.add(authnAssertionID);

        Map smap = new HashMap();
        smap.put(SAML2Constants.POST_ASSERTION, assertion);
        smap.put(SAML2Constants.NOTONORAFTER, System.currentTimeMillis() + 60000L);

        Map<String, Long> bearerAssertionIDs = new LinkedHashMap<>();
        bearerAssertionIDs.put(authnAssertionID, System.currentTimeMillis() + 60000L);
        for (String assertionID : otherBearerAssertionIDs) {
            bearerAssertionIDs.put(assertionID, System.currentTimeMillis() + 60000L);
            claimedIDs.add(assertionID);
        }
        smap.put(SAML2Constants.BEARER_ASSERTION_IDS, bearerAssertionIDs);
        return smap;
    }

    @Test
    public void theFirstHolderOfAnAssertionConsumesIt() throws Exception {

        SPACSUtils.markAssertionAsConsumed(responseOf("s2-first-holder"));

        assertThat(SPCache.assertionByIDCache.get("s2-first-holder"))
                .isEqualTo(SAML2Constants.ONETIME);
    }

    @Test
    public void aReplayedAssertionIsRejected() throws Exception {

        SPACSUtils.markAssertionAsConsumed(responseOf("s2-replayed"));

        try {
            SPACSUtils.markAssertionAsConsumed(responseOf("s2-replayed"));
            org.testng.Assert.fail("the second submission should have been rejected");
        } catch (SAML2Exception expected) {
            assertThat(expected.getMessage()).isNotEmpty();
        }
    }

    /**
     * The claim has to be atomic. Written as a plain {@code put} it left a window in which two
     * submissions of one assertion both got past the one-time check in {@code verifyResponse} and
     * both went on to mint a session.
     */
    @Test
    public void concurrentHoldersOfOneAssertionLeaveExactlyOneWinner() throws Exception {

        final int racers = 16;
        final String assertionID = "s2-contended";
        final CountDownLatch start = new CountDownLatch(1);
        final AtomicInteger winners = new AtomicInteger();
        final AtomicInteger losers = new AtomicInteger();

        ExecutorService pool = Executors.newFixedThreadPool(racers);
        try {
            Future[] running = new Future[racers];
            for (int i = 0; i < racers; i++) {
                running[i] = pool.submit(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        start.await();
                        try {
                            SPACSUtils.markAssertionAsConsumed(responseOf(assertionID));
                            winners.incrementAndGet();
                        } catch (SAML2Exception rejected) {
                            losers.incrementAndGet();
                        }
                        return null;
                    }
                });
            }
            start.countDown();
            for (Future f : running) {
                f.get(30, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(winners.get()).as("sessions the assertion could mint").isEqualTo(1);
        assertThat(losers.get()).as("submissions turned away").isEqualTo(racers - 1);
    }

    /**
     * GHSA-3j2f-qhrc-rjfj: {@code verifyResponse} checks every bearer assertion of a response
     * against the one-time cache, so every one of them has to be claimed. Claiming only the
     * assertion the session was built from left the rest replayable: an unsolicited response
     * carrying two individually signed assertions could be submitted again with the spent one
     * dropped, and the second assertion would mint a second session.
     */
    @Test
    public void everyBearerAssertionOfTheResponseIsClaimed() throws Exception {

        SPACSUtils.markAssertionAsConsumed(
                responseWithBearerAssertions("s2-first-of-two", "s2-second-of-two"));

        assertThat(SPCache.assertionByIDCache.get("s2-first-of-two"))
                .as("the assertion the session was built from").isEqualTo(SAML2Constants.ONETIME);
        assertThat(SPCache.assertionByIDCache.get("s2-second-of-two"))
                .as("the assertion that merely came along with it").isEqualTo(SAML2Constants.ONETIME);
    }

    /**
     * And the response that carries only the assertion left over from an earlier submission is
     * turned away, rather than being accepted because its first assertion is new.
     */
    @Test
    public void aResponseStrippedDownToAnAlreadyClaimedAssertionIsRejected() throws Exception {

        SPACSUtils.markAssertionAsConsumed(responseWithBearerAssertions("s2-kept", "s2-stripped"));

        try {
            SPACSUtils.markAssertionAsConsumed(responseWithBearerAssertions("s2-stripped"));
            org.testng.Assert.fail("the second submission should have been rejected");
        } catch (SAML2Exception expected) {
            assertThat(expected.getMessage()).isNotEmpty();
        }
    }

    /**
     * And the claim is all or nothing. A response that pairs a fresh assertion with a spent one is
     * refused - and has to leave the fresh one unclaimed, or resubmitting it with the spent one
     * would be a way of spending an assertion the sender never got a session out of.
     *
     * <p>The rollback reaches the assertions this call claimed and stops there. The one that caused
     * the refusal was claimed by an earlier submission, so handing it back as well would un-spend a
     * captured assertion: pair it with any fresh one, take the refusal, and it is replayable again.
     * Both halves are asserted here, because a rollback that goes one assertion too far leaves the
     * fresh one unclaimed just the same.
     */
    @Test
    public void aRefusedResponseSpendsNoneOfItsAssertions() throws Exception {

        SPACSUtils.markAssertionAsConsumed(responseOf("s2-spent-earlier"));

        try {
            SPACSUtils.markAssertionAsConsumed(
                    responseWithBearerAssertions("s2-still-fresh", "s2-spent-earlier"));
            org.testng.Assert.fail("a response carrying a spent assertion should have been rejected");
        } catch (SAML2Exception expected) {
            assertThat(expected.getMessage()).isNotEmpty();
        }

        assertThat(SPCache.assertionByIDCache.get("s2-still-fresh"))
                .as("the assertion claimed before the refusal, handed back").isNull();
        assertThat(SPCache.assertionByIDCache.get("s2-spent-earlier"))
                .as("the assertion that caused the refusal, still spent")
                .isEqualTo(SAML2Constants.ONETIME);

        // and it is still good for a response of its own.
        SPACSUtils.markAssertionAsConsumed(responseOf("s2-still-fresh"));
        assertThat(SPCache.assertionByIDCache.get("s2-still-fresh"))
                .isEqualTo(SAML2Constants.ONETIME);
    }

    /**
     * This method is the only replay defence its callers have, so a map it cannot find an assertion
     * in has to be refused. Returning quietly would switch replay protection off for that request
     * and leave nothing behind to say so.
     */
    @Test
    public void aResponseWithoutAnAssertionIsRefused() throws Exception {

        try {
            SPACSUtils.markAssertionAsConsumed(new HashMap());
            org.testng.Assert.fail("a map with no assertion should have been refused");
        } catch (SAML2Exception expected) {
            assertThat(expected.getMessage()).isNotEmpty();
        }
    }

    /**
     * A map that carries the assertion the session is built from but not the bearer assertions
     * {@code verifyResponse} reports is refused as well. Falling back to that one assertion would
     * leave the rest of the response replayable - the very gap this claim closes - and would do it
     * silently, for a caller that had every reason to think it was protected.
     */
    @Test
    public void aResponseThatReportsNoBearerAssertionIsRefused() throws Exception {

        Map smap = responseOf("s2-unreported");
        smap.remove(SAML2Constants.BEARER_ASSERTION_IDS);

        try {
            SPACSUtils.markAssertionAsConsumed(smap);
            org.testng.Assert.fail("a map reporting no bearer assertion should have been refused");
        } catch (SAML2Exception expected) {
            assertThat(expected.getMessage()).isNotEmpty();
        }

        assertThat(SPCache.assertionByIDCache.get("s2-unreported"))
                .as("nothing was claimed on the way out").isNull();
    }

    /**
     * GHSA-3j2f-qhrc-rjfj: the identity provider that answers an {@code ArtifactResolve} is chosen
     * by the {@code SourceID} inside the artifact, while the identity the response claims is the
     * {@code Issuer} written inside it - which {@code verifyResponse} only checks for membership of
     * the circle of trust. Unsigned, that let any member of the circle of trust answer a resolution
     * with a response issued in the name of any other.
     *
     * <p>A mismatch is refused whichever way the signature decision went, and the row that carries
     * the weight is the one where it is not required: an SP that requires the signature would have
     * the response rejected by the signature check anyway, since the assertions are verified against
     * the certificates of the entity they name. An SP that has relaxed it is the only one that ever
     * reaches this comparison with nothing else standing behind it - so both rows are asserted, or
     * making the comparison conditional on the signature decision would leave the suite green while
     * turning the binding off for exactly the deployment it was written for.
     *
     * <p>A match is accepted whichever way it went, too. Making the signature decision refuse on its
     * own would reject every correctly issued artifact response on an SP that requires the
     * signature, which is the stock configuration.
     */
    @Test
    public void anArtifactResponseIssuedByAnotherEntityIsUnbound() throws Exception {

        assertThat(SPACSUtils.isArtifactResponseUnbound("urn:example:idp", issuer("urn:example:other"),
                true))
                .as("resolved against one identity provider, issued in the name of another").isTrue();
        assertThat(SPACSUtils.isArtifactResponseUnbound("urn:example:idp", issuer("urn:example:other"),
                false))
                .as("issued by another entity, and no assertion signature to bind it either").isTrue();
        assertThat(SPACSUtils.isArtifactResponseUnbound("urn:example:idp", issuer("urn:example:idp"),
                false))
                .as("the entity that answered the resolution is the entity that issued it").isFalse();
        assertThat(SPACSUtils.isArtifactResponseUnbound("urn:example:idp", issuer("urn:example:idp"),
                true))
                .as("the entity that answered is the entity that issued it, on an SP that requires "
                        + "the signature").isFalse();
    }

    /**
     * {@code Response/Issuer} is optional (SAML 2.0 Core 3.2.2) and {@code verifyResponse} falls back
     * to the {@code Issuer} of the assertions, which their signature binds. Its absence therefore
     * leaves the response unbound only where this SP has relaxed that signature - an SP that
     * requires it keeps accepting a response that leaves the element out, as it did before this
     * change, whether that requirement comes from {@code WantAssertionsSigned} or from the
     * deployment not having relaxed the artifact binding.
     */
    @Test
    public void anArtifactResponseWithoutAnIssuerIsUnboundOnlyWhereTheSignatureIsNotRequired()
            throws Exception {

        assertThat(SPACSUtils.isArtifactResponseUnbound("urn:example:idp", null, true))
                .as("the assertion signature binds the issuer this response leaves out").isFalse();
        assertThat(SPACSUtils.isArtifactResponseUnbound("urn:example:idp", null, false))
                .as("no issuer, and no signature required either").isTrue();
    }

    private Issuer issuer(String entityID) throws SAML2Exception {
        Issuer issuer = AssertionFactory.getInstance().createIssuer();
        issuer.setValue(entityID);
        return issuer;
    }
}
