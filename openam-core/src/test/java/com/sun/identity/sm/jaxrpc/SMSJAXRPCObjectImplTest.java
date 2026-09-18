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
package com.sun.identity.sm.jaxrpc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.iplanet.ums.IUMSConstants;
import com.sun.identity.sm.RequiredValueValidator;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceAttributeValidator;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * Tests the JAXRPC {@code validateServiceAttributes} endpoint, which used to ignore the token
 * it is handed and to instantiate any class named by the caller before checking its type
 * (GHSA-wxmx-q96f-w4gw).
 */
public class SMSJAXRPCObjectImplTest {

    private static final String TOKEN_ID = "AQIC5wM2LY4Sfcw";

    private SSOTokenManager tokenManager;

    private SSOTokenManager previousTokenManager;
    private boolean previouslyInitialized;
    private SSOException previousInitializationError;

    /**
     * Drives the public endpoint with the servant already "initialized", so that the token
     * check is exercised where it actually sits rather than only where it is defined. The
     * servant's statics are restored afterwards rather than blanked, so that a test class
     * scheduled before this one in the same JVM does not have its servant taken away.
     */
    @BeforeMethod
    public void stubTheServant() throws Exception {
        previousTokenManager = SMSJAXRPCObjectImpl.tokenMgr;
        previouslyInitialized = SMSJAXRPCObjectImpl.initialized;
        previousInitializationError = SMSJAXRPCObjectImpl.initializationError;

        tokenManager = mock(SSOTokenManager.class);
        when(tokenManager.createSSOToken(TOKEN_ID)).thenReturn(mock(SSOToken.class));
        SMSJAXRPCObjectImpl.tokenMgr = tokenManager;
        SMSJAXRPCObjectImpl.initialized = true;
        SMSJAXRPCObjectImpl.initializationError = null;
    }

    @AfterMethod
    public void unstubTheServant() {
        SMSJAXRPCObjectImpl.tokenMgr = previousTokenManager;
        SMSJAXRPCObjectImpl.initialized = previouslyInitialized;
        SMSJAXRPCObjectImpl.initializationError = previousInitializationError;
    }

    // ------------------------------------------------------------------ the caller's token

    /**
     * The endpoint used to run validators for a caller that presented no usable session at
     * all - {@code /jaxrpc/*} has no container {@code <security-constraint>} in front of it.
     */
    @Test
    public void endpointRefusesAnUnusableToken() throws Exception {
        doThrow(new SSOException("session expired"))
                .when(tokenManager).validateToken(any(SSOToken.class));
        assertFalse(StaticInitializerMarker.refusedCallerExecuted);

        try {
            new SMSJAXRPCObjectImpl().validateServiceAttributes(TOKEN_ID,
                    RefusedCallerValidator.class.getName(),
                    Collections.singleton("aValue"));
            fail("a caller without a usable session must not be able to run validators");
        } catch (SMSException expected) {
            // SMSException(Throwable, errorCode) remaps an SSOException cause onto
            // sms-AUTHENTICATION_ERROR, exactly as SMSEntry.validateToken() does. What
            // matters here is that an error code is set at all: it is the only field that
            // survives the JAXRPC round trip and lets the caller see anything.
            assertEquals(expected.getErrorCode(), IUMSConstants.SMS_AUTHENTICATION_ERROR);
            assertEquals(expected.getResourceBundleName(), IUMSConstants.UMS_BUNDLE_NAME);
        }
        assertFalse(StaticInitializerMarker.refusedCallerExecuted,
                "a refused caller must not get a class loaded, let alone initialized");
    }

    @Test
    public void endpointRefusesAnUnknownToken() throws Exception {
        when(tokenManager.createSSOToken(TOKEN_ID))
                .thenThrow(new SSOException("no such session"));

        try {
            new SMSJAXRPCObjectImpl().validateServiceAttributes(TOKEN_ID,
                    RequiredValueValidator.class.getName(),
                    Collections.singleton("aValue"));
            fail("an unknown token must not be able to run validators");
        } catch (SMSException expected) {
            // Not an SSOException: RemoteServiceAttributeValidator turns that into a "false"
            // answer, which reaches the administrator as invalid attribute values instead of
            // as a rejected session.
            assertEquals(expected.getErrorCode(), IUMSConstants.SMS_AUTHENTICATION_ERROR);
        }
    }

    @Test
    public void endpointReportsABootstrapFailureAsSuch() throws Exception {
        SMSJAXRPCObjectImpl.initializationError = new SSOException("no token manager");

        try {
            new SMSJAXRPCObjectImpl().validateServiceAttributes(TOKEN_ID,
                    RequiredValueValidator.class.getName(),
                    Collections.singleton("aValue"));
            fail("a servant that failed to bootstrap must not answer");
        } catch (SMSException expected) {
            // Not an SSOException: RemoteServiceAttributeValidator turns that into a "false"
            // answer, which reaches the administrator as invalid attribute values or as
            // unknown property names. And not the token error code either - a server that
            // failed to start must not send every caller chasing a token problem.
            assertEquals(expected.getErrorCode(), IUMSConstants.SMS_SERVER_DOWN);
            assertEquals(expected.getResourceBundleName(), IUMSConstants.UMS_BUNDLE_NAME);
        }
    }

    /**
     * The counterpart of the refusals: a caller holding a usable session is served, so the
     * token check cannot be satisfied by refusing everyone.
     */
    @Test
    public void endpointServesAnAuthenticatedCaller() throws Exception {
        assertTrue(new SMSJAXRPCObjectImpl().validateServiceAttributes(TOKEN_ID,
                RequiredValueValidator.class.getName(),
                Collections.singleton("aValue")));
        assertFalse(new SMSJAXRPCObjectImpl().validateServiceAttributes(TOKEN_ID,
                RequiredValueValidator.class.getName(),
                Collections.<String>emptySet()));
    }

    /**
     * A missing {@code <Set_3>} element decodes to {@code null}; the validators are not
     * written for that, and an absent value is the same as no value at all.
     */
    @Test
    public void endpointTreatsMissingValuesAsAnEmptySet() throws Exception {
        assertFalse(new SMSJAXRPCObjectImpl().validateServiceAttributes(TOKEN_ID,
                RequiredValueValidator.class.getName(), null));
    }

    // ------------------------------------------------------------ validator class handling

    /**
     * Holds the markers written by the side effecting classes below. Kept in a separate class
     * so that reading a marker does not initialize the class that writes it. Each marker is
     * set at most once per JVM, which is all a class initializer ever does.
     */
    public static class StaticInitializerMarker {
        static volatile boolean impostorExecuted = false;
        static volatile boolean endpointImpostorExecuted = false;
        static volatile boolean validatorExecuted = false;
        static volatile boolean endpointValidatorExecuted = false;
        static volatile boolean refusedCallerExecuted = false;
        static volatile boolean sizeRefusedExecuted = false;
    }

    /**
     * A class that is not a validator and whose static initializer has an observable side
     * effect - the kind of class an attacker would name to get code running.
     */
    public static class SideEffectingValidatorImpostor {
        static {
            StaticInitializerMarker.impostorExecuted = true;
        }
    }

    /**
     * The same thing for the public endpoint. Kept apart from
     * {@link SideEffectingValidatorImpostor} so that a class initialized through one of the
     * two paths is not reported as both.
     */
    public static class SideEffectingEndpointImpostor {
        static {
            StaticInitializerMarker.endpointImpostorExecuted = true;
        }
    }

    /**
     * A genuine validator that cannot be constructed. Resolving it is fine, instantiating it
     * fails with an <code>InvocationTargetException</code> - the case
     * <code>Class.newInstance()</code> used to propagate raw and undeclared.
     */
    public static class ThrowingConstructorValidator implements ServiceAttributeValidator {
        public ThrowingConstructorValidator() {
            throw new IllegalStateException("this validator cannot be built");
        }

        public boolean validate(Set<String> values) {
            return true;
        }
    }

    /**
     * A genuine validator with no accessible constructor: <code>IllegalAccessException</code>.
     */
    public static class PrivateConstructorValidator implements ServiceAttributeValidator {
        private PrivateConstructorValidator() {
        }

        public boolean validate(Set<String> values) {
            return true;
        }
    }

    /**
     * A genuine validator that cannot be instantiated at all:
     * <code>InstantiationException</code>.
     */
    public abstract static class AbstractValidator implements ServiceAttributeValidator {
        public boolean validate(Set<String> values) {
            return true;
        }
    }

    /**
     * A genuine validator whose class initializer fails. It resolves, so it passes the type
     * check, and only blows up when it is instantiated - as an
     * <code>ExceptionInInitializerError</code> the first time and a
     * <code>NoClassDefFoundError</code> afterwards, both of them <code>LinkageError</code>s
     * rather than exceptions.
     */
    public static class FailingInitializerValidator implements ServiceAttributeValidator {
        static {
            if (true) {
                throw new IllegalStateException("this validator cannot be initialized");
            }
        }

        public boolean validate(Set<String> values) {
            return true;
        }
    }

    /**
     * The positive control for {@link #doesNotRunTheStaticInitializerOfARejectedClass}: a
     * genuine validator built the same way, whose marker <em>is</em> expected to be set.
     */
    public static class SideEffectingValidator implements ServiceAttributeValidator {
        static {
            StaticInitializerMarker.validatorExecuted = true;
        }

        public boolean validate(Set<String> values) {
            return true;
        }
    }

    /**
     * The positive control for {@link #endpointDoesNotInitializeARejectedClass}, on the same
     * entry point: a genuine validator whose marker <em>is</em> expected to be set once the
     * endpoint has accepted it. {@link SideEffectingEndpointImpostor} can never be accepted,
     * so on its own its {@code assertFalse} would also hold if the marker stopped working.
     */
    public static class SideEffectingEndpointValidator implements ServiceAttributeValidator {
        static {
            StaticInitializerMarker.endpointValidatorExecuted = true;
        }

        public boolean validate(Set<String> values) {
            return true;
        }
    }

    /**
     * A validator that resolves, constructs and only then fails, the way
     * {@code com.sun.identity.policy.ResourceComparatorValidator} does on a token with no
     * {@code '='} in it: it indexes the separator without checking that it was there. Nothing
     * about that class is hostile - it is accepted by design - so the failure has to be caught
     * where the validator is run, not where it is loaded.
     */
    public static class ThrowingValidator implements ServiceAttributeValidator {
        public boolean validate(Set<String> values) {
            String token = "abc";
            return token.substring(0, token.indexOf("=")).isEmpty();
        }
    }

    /**
     * The same thing one level down: a validator that reaches a class that is not on the
     * classpath, which arrives as a <code>LinkageError</code> rather than as an exception.
     */
    public static class LinkageErrorValidator implements ServiceAttributeValidator {
        public boolean validate(Set<String> values) {
            throw new NoClassDefFoundError("com/example/AMissingDependency");
        }
    }

    /**
     * A validator named by a refused caller. Its static initializer must never run: the token
     * check comes before the class is touched at all.
     */
    public static class RefusedCallerValidator implements ServiceAttributeValidator {
        static {
            StaticInitializerMarker.refusedCallerExecuted = true;
        }

        public boolean validate(Set<String> values) {
            return true;
        }
    }

    @Test
    public void instantiatesAServiceAttributeValidator() throws Exception {
        ServiceAttributeValidator validator = SMSJAXRPCObjectImpl.loadValidator(
                RequiredValueValidator.class.getName());

        assertTrue(validator instanceof RequiredValueValidator);
        assertTrue(validator.validate(Collections.singleton("aValue")));
        assertFalse(validator.validate(Collections.<String>emptySet()));
    }

    @Test
    public void doesNotRunTheStaticInitializerOfARejectedClass() {
        assertFalse(StaticInitializerMarker.impostorExecuted);

        Throwable thrown = null;
        try {
            SMSJAXRPCObjectImpl.loadValidator(SideEffectingValidatorImpostor.class.getName());
        } catch (Throwable t) {
            thrown = t;
        }

        // Checked before the rejection itself, so that a class that got initialized is
        // reported as such even when it also escaped with the wrong exception.
        assertFalse(StaticInitializerMarker.impostorExecuted,
                "the class must be resolved without being initialized");
        assertTrue(thrown instanceof SMSException,
                "expected the class to be rejected with an SMSException, got " + thrown);
    }

    /**
     * Positive control for the test above: proves the marker mechanism actually observes a
     * class initializer, so that a rename or a refactoring cannot make that test pass
     * vacuously.
     */
    @Test
    public void runsTheStaticInitializerOfAnAcceptedValidator() throws Exception {
        assertFalse(StaticInitializerMarker.validatorExecuted);

        ServiceAttributeValidator validator = SMSJAXRPCObjectImpl.loadValidator(
                SideEffectingValidator.class.getName());

        assertTrue(validator instanceof SideEffectingValidator);
        assertTrue(StaticInitializerMarker.validatorExecuted,
                "an accepted validator is initialized, so the marker must be observable");
    }

    @DataProvider(name = "refusedClassNames")
    public Object[][] refusedClassNames() {
        return new Object[][] {
            { null,                                      "a null class name" },
            { "",                                        "an empty class name" },
            { "   ",                                     "a blank class name" },
            { "com.example.NoSuchValidator",             "an unknown class" },
            { Object.class.getName(),                    "a class that is not a validator" },
            { ServiceAttributeValidator.class.getName(), "the validator interface itself" },
            { ThrowingConstructorValidator.class.getName(),
                                                         "a throwing constructor" },
            { PrivateConstructorValidator.class.getName(),
                                                         "an inaccessible constructor" },
            { AbstractValidator.class.getName(),         "an abstract validator" },
            { FailingInitializerValidator.class.getName(),
                                                         "a failing class initializer" },
        };
    }

    @Test(dataProvider = "refusedClassNames")
    public void refusesTheClassName(String className, String description) {
        try {
            SMSJAXRPCObjectImpl.loadValidator(className);
            fail(description + " must be rejected");
        } catch (SMSException expected) {
            // expected
        }
    }

    /**
     * The whole refusal, asserted in one place and from both entry points. Checking only the
     * error code at the endpoint would let a re-wrap there - the caller-facing tidy-up that
     * keeps the code and drops the rest - pass while refusals stop being indistinguishable.
     *
     * @param refusal the exception the refusal left as.
     * @param className the class name that was refused.
     * @param description what was refused, for the failure message.
     */
    private static void assertRefusal(SMSException refusal, String className,
            String description) {
        assertEquals(refusal.getErrorCode(),
                IUMSConstants.SMS_VALIDATOR_CANNOT_INSTANTIATE_CLASS,
                description + " must be reported like every other refusal");
        assertEquals(refusal.getResourceBundleName(), IUMSConstants.UMS_BUNDLE_NAME,
                description + " must be reported from the same bundle");
        // The bundle text carries a {0}; without the argument the message reaches the
        // administrator, and the server log, with the placeholder unsubstituted. The name
        // is the caller's, so it arrives rendered - see refusesAForgedClassNameSafely.
        assertEquals(refusal.getMessageArgs(),
                new Object[] { SMSJAXRPCObjectImpl.forLog(className) },
                "the refused class name must be the message argument");
    }

    /**
     * The message argument is serialized to the client, where {@code ssoadm} prints it and
     * writes it to a log of its own, so the caller supplied name is rendered before it is put
     * there - sanitizing only the server's copy would move a forged line to the client rather
     * than prevent it.
     */
    @Test
    public void refusesAForgedClassNameSafely() throws Exception {
        String forged = "com.example.Validator\r\nERROR: a fabricated log entry"
                + "\u2028ERROR: and another";

        try {
            new SMSJAXRPCObjectImpl().validateServiceAttributes(TOKEN_ID, forged,
                    Collections.singleton("aValue"));
            fail("a forged class name must be rejected");
        } catch (SMSException expected) {
            Object[] args = expected.getMessageArgs();
            assertEquals(args.length, 1);
            String reported = (String) args[0];
            assertFalse(reported.contains("\r") || reported.contains("\n")
                            || reported.contains("\u2028"),
                    "no line terminator may survive into the reported name: " + reported);
            assertTrue(reported.startsWith("com.example.Validator"),
                    "the name still has to be recognisable: " + reported);
        }
    }

    /**
     * And the length cap, for the same reason: the name is the caller's, and it is written to
     * two logs before anything has accepted it.
     */
    @Test
    public void capsTheLengthOfAReportedClassName() throws Exception {
        StringBuilder longName = new StringBuilder("com.example.");
        while (longName.length() < 5000) {
            longName.append('x');
        }

        try {
            new SMSJAXRPCObjectImpl().validateServiceAttributes(TOKEN_ID, longName.toString(),
                    Collections.singleton("aValue"));
            fail("an over long class name must be rejected");
        } catch (SMSException expected) {
            String reported = (String) expected.getMessageArgs()[0];
            assertTrue(reported.length() <= 203,
                    "the reported name must be capped, got " + reported.length() + " chars");
        }
    }

    /**
     * Every rejection reports the same error code, so that the endpoint does not tell the
     * caller whether a given class is present on the classpath. The error code, rather than
     * the message, is what survives the JAXRPC round trip.
     */
    @Test(dataProvider = "refusedClassNames")
    public void reportsEveryRejectionTheSameWay(String className, String description) {
        try {
            SMSJAXRPCObjectImpl.loadValidator(className);
            fail(description + " must be rejected");
        } catch (SMSException expected) {
            assertRefusal(expected, className, description);
        }
    }

    // ------------------------------------------- validator class handling, at the endpoint

    /**
     * The regression test for GHSA-wxmx-q96f-w4gw, driven through the public method rather
     * than through {@code loadValidator}: restoring the pre-fix body of
     * {@code validateServiceAttributes} leaves the extracted helper in place, correct and
     * unused, so the advisory is only closed where the endpoint is the one asserted on.
     */
    @Test
    public void endpointDoesNotInitializeARejectedClass() throws Exception {
        assertFalse(StaticInitializerMarker.endpointImpostorExecuted);

        try {
            new SMSJAXRPCObjectImpl().validateServiceAttributes(TOKEN_ID,
                    SideEffectingEndpointImpostor.class.getName(),
                    Collections.singleton("aValue"));
            fail("a class that is not a validator must be rejected by the endpoint");
        } catch (SMSException expected) {
            assertRefusal(expected, SideEffectingEndpointImpostor.class.getName(),
                    "a class that is not a validator");
        }

        assertFalse(StaticInitializerMarker.endpointImpostorExecuted,
                "the endpoint must not initialize a class it rejects");
    }

    /**
     * The positive control for the assertion above, which on its own would also hold if the
     * marker stopped being written: {@link SideEffectingEndpointImpostor} is not a validator,
     * so nothing can ever accept it and its marker can never be set. This one is accepted, so
     * its marker has to be set, through the endpoint and not only through
     * {@code loadValidator}.
     */
    @Test
    public void endpointRunsTheStaticInitializerOfAnAcceptedValidator() throws Exception {
        assertFalse(StaticInitializerMarker.endpointValidatorExecuted);

        assertTrue(new SMSJAXRPCObjectImpl().validateServiceAttributes(TOKEN_ID,
                SideEffectingEndpointValidator.class.getName(),
                Collections.singleton("aValue")));

        assertTrue(StaticInitializerMarker.endpointValidatorExecuted,
                "an accepted validator is initialized, so the marker must be observable "
                        + "through the endpoint too");
    }

    /**
     * And the whole refusal set through the endpoint, so that the wiring between the two is
     * pinned for every reason a class can be refused, not only for the impostor above.
     */
    @Test(dataProvider = "refusedClassNames")
    public void endpointRefusesTheClassName(String className, String description)
            throws Exception {
        try {
            new SMSJAXRPCObjectImpl().validateServiceAttributes(TOKEN_ID, className,
                    Collections.singleton("aValue"));
            fail(description + " must be rejected by the endpoint");
        } catch (SMSException expected) {
            assertRefusal(expected, className, description);
        }
    }

    // ------------------------------------------------------- the size of what it is handed

    /**
     * A validator whose marker says whether the values ever reached one. Nothing about it is
     * hostile: the point is that an oversized request is refused before any validator runs,
     * so no validator gets to spend the CPU.
     */
    public static class SizeRefusedValidator implements ServiceAttributeValidator {
        static {
            StaticInitializerMarker.sizeRefusedExecuted = true;
        }

        public boolean validate(Set<String> values) {
            return true;
        }
    }

    private static Set<String> values(int count, int length) {
        Set<String> values = new HashSet<String>(count * 2);
        for (int i = 0; i < count; i++) {
            String index = Integer.toString(i);
            values.add(index + "x".repeat(Math.max(0, length - index.length())));
        }
        return values;
    }

    /**
     * The caller names the validator and supplies the values, and the validators are not
     * written for input of a size they never see from a service schema:
     * {@code ResourceComparatorValidator} used to cost O(n<sup>3</sup>) in the length of one
     * value and the shared map key pattern O(n<sup>2</sup>). Both are bounded now, but the
     * endpoint is what lets any authenticated caller pick a validator and choose its input,
     * so the size of the input is bounded here as well - for the validators that exist today
     * and for the ones added later.
     */
    @DataProvider(name = "oversizedValues")
    public Object[][] oversizedValues() {
        return new Object[][] {
            { 1, SMSJAXRPCObjectImpl.MAX_VALUE_LENGTH + 1,
                                                    "a value over the length limit" },
            { SMSJAXRPCObjectImpl.MAX_VALUES + 1, 1,
                                                    "more values than the limit" },
            { SMSJAXRPCObjectImpl.MAX_TOTAL_LENGTH / SMSJAXRPCObjectImpl.MAX_VALUE_LENGTH + 1,
                    SMSJAXRPCObjectImpl.MAX_VALUE_LENGTH,
                                                    "values over the total limit" },
        };
    }

    @Test(dataProvider = "oversizedValues")
    public void endpointRefusesOversizedValues(int count, int length, String description)
            throws Exception {
        try {
            new SMSJAXRPCObjectImpl().validateServiceAttributes(TOKEN_ID,
                    RequiredValueValidator.class.getName(), values(count, length));
            fail(description + " must be refused");
        } catch (SMSException expected) {
            assertEquals(expected.getErrorCode(),
                    IUMSConstants.SMS_VALIDATOR_VALUES_TOO_LARGE,
                    description + " must be reported as an oversized request");
            assertEquals(expected.getResourceBundleName(), IUMSConstants.UMS_BUNDLE_NAME);
            // A code with no entry in the bundle renders as the code itself -
            // Locale.getString() answers the key it was given and only says so at
            // message level - so the administrator would be shown the raw key.
            assertNotEquals(expected.getL10NMessage(Locale.ENGLISH),
                    IUMSConstants.SMS_VALIDATOR_VALUES_TOO_LARGE,
                    "the error code has to have a message in the bundle");
        }
    }

    /**
     * The bound is checked before the class name is, so that the answer does not depend on
     * the class at all - otherwise an oversized request would tell the caller whether the
     * class it named is a validator that is present on the classpath, which is the oracle the
     * uniform refusal exists to close.
     */
    @Test
    public void endpointRefusesOversizedValuesWhateverTheClassIs() throws Exception {
        assertFalse(StaticInitializerMarker.sizeRefusedExecuted);
        Set<String> tooLong = values(1, SMSJAXRPCObjectImpl.MAX_VALUE_LENGTH + 1);

        for (String className : new String[] { SizeRefusedValidator.class.getName(),
                "com.example.NoSuchValidator", Object.class.getName(), null }) {
            try {
                new SMSJAXRPCObjectImpl().validateServiceAttributes(TOKEN_ID, className,
                        tooLong);
                fail("an oversized request must be refused, whatever the class is");
            } catch (SMSException expected) {
                assertEquals(expected.getErrorCode(),
                        IUMSConstants.SMS_VALIDATOR_VALUES_TOO_LARGE,
                        "the refusal must not depend on the class named");
            }
        }

        assertFalse(StaticInitializerMarker.sizeRefusedExecuted,
                "an oversized request must not get a validator loaded, let alone run");
    }

    /**
     * The counterpart: a request at the limits is served, so the bound cannot be satisfied by
     * refusing everything. The limits are far above anything a service schema declares - the
     * largest value with a validator on it in the shipped schemas is a 256 bit key in base64,
     * 44 characters.
     */
    @Test
    public void endpointServesARequestAtTheLimits() throws Exception {
        assertTrue(new SMSJAXRPCObjectImpl().validateServiceAttributes(TOKEN_ID,
                RequiredValueValidator.class.getName(),
                values(1, SMSJAXRPCObjectImpl.MAX_VALUE_LENGTH)));
        assertTrue(new SMSJAXRPCObjectImpl().validateServiceAttributes(TOKEN_ID,
                RequiredValueValidator.class.getName(),
                values(SMSJAXRPCObjectImpl.MAX_VALUES, 8)));
    }

    @DataProvider(name = "failingValidators")
    public Object[][] failingValidators() {
        return new Object[][] {
            { ThrowingValidator.class.getName(),    "a validator that throws" },
            { LinkageErrorValidator.class.getName(),
                                                    "a validator with a missing dependency" },
        };
    }

    /**
     * A validator that is accepted and then fails on the values it was given leaves as the
     * same <code>SMSException</code> as every refused class. Unwrapped, it would leave the
     * servant as a raw SOAP fault carrying a stack trace - which is what the uniform refusal
     * exists to avoid, and which a caller can reach with any usable session by naming a
     * schema-declared validator and a value it was not written for.
     */
    @Test(dataProvider = "failingValidators")
    public void endpointReportsAValidatorFailureLikeEveryOtherRefusal(String className,
            String description) throws Exception {
        try {
            new SMSJAXRPCObjectImpl().validateServiceAttributes(TOKEN_ID, className,
                    Collections.singleton("aValue"));
            fail(description + " must not answer");
        } catch (SMSException expected) {
            assertRefusal(expected, className, description);
        }
    }
}
