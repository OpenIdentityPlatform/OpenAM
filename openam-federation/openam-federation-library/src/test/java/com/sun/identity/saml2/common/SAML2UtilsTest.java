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
 * Copyright 2014-2015 ForgeRock AS.
 * Portions Copyrighted 2025-2026 3A Systems, LLC.
 */

package com.sun.identity.saml2.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sun.identity.saml2.jaxb.entityconfig.AttributeType;
import com.sun.identity.saml2.jaxb.entityconfig.SPSSOConfigElement;
import com.sun.identity.saml2.jaxb.metadata.AssertionConsumerServiceElement;
import com.sun.identity.saml2.jaxb.metadata.SPSSODescriptorElement;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.shared.configuration.ISystemProperties;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.shared.encode.URLEncDec;
import org.apache.commons.lang3.RandomStringUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.BooleanSupplier;

public class SAML2UtilsTest {

    private static final String REALM = "/";
    private static final String SP_ENTITY_ID = "urn:example:sp";

    /**
     * {@code SAML2Utils} resolves the metadata of the hosted SP through a private static
     * {@code SAML2MetaManager}, built in a static initialiser from the running configuration. The
     * decision under test asks it which bindings the SP publishes, so the tests put one of their
     * own in its place - and put back the exact reference they found, because the field is static
     * and every other SAML2 test in the surefire fork shares it.
     */
    private void useMetaManager(SAML2MetaManager metaManager) throws Exception {
        metaManagerField().set(null, metaManager);
    }

    private static Field metaManagerField() throws Exception {
        Field field = SAML2Utils.class.getDeclaredField("saml2MetaManager");
        field.setAccessible(true);
        return field;
    }

    private Object realMetaManager;

    @BeforeMethod
    public void rememberTheRealMetaManager() throws Exception {
        realMetaManager = metaManagerField().get(null);
    }

    @AfterMethod
    public void restoreTheRealMetaManager() throws Exception {
        metaManagerField().set(null, realMetaManager);
    }

    /**
     * The deployment-wide fallback is read through {@code SystemPropertiesManager}, which resolves
     * an {@code ISystemProperties} implementation once into a private static field, out of whatever
     * configuration the surefire fork happens to have. The test that covers that read installs one
     * of its own there - and, like the meta manager above, puts back the exact reference it found.
     */
    private void useSystemProperties(ISystemProperties properties) throws Exception {
        systemPropertiesField().set(null, properties);
    }

    private static Field systemPropertiesField() throws Exception {
        Field field = SystemPropertiesManager.class.getDeclaredField("systemProperties");
        field.setAccessible(true);
        return field;
    }

    private Object realSystemProperties;

    @BeforeMethod
    public void rememberTheRealSystemProperties() throws Exception {
        realSystemProperties = systemPropertiesField().get(null);
    }

    @AfterMethod
    public void restoreTheRealSystemProperties() throws Exception {
        systemPropertiesField().set(null, realSystemProperties);
    }

    /**
     * A deployment whose {@code openam.saml.sp.artifact.allow.unsigned.assertions} holds the given
     * value, and which answers every other key the way the fork's real configuration does.
     *
     * <p>The delegation is not decoration. The field this goes into is JVM-global, and a stub that
     * answered {@code null} to everything would be read by the static initialiser of any class
     * first loaded while it is installed - {@code SAML2Utils} itself reads the server protocol,
     * host, port and cookie name that way, {@code FMSigProvider} its canonicalisation and signature
     * algorithms - and such a class caches that {@code null} for the rest of the surefire fork,
     * long after this test has put the real properties back. Overriding the one key under test
     * leaves every other reader seeing what it would have seen.
     *
     * <p>There is nothing to delegate to when the fork found no provider class to instantiate, and
     * nothing is lost by it either: {@code SystemPropertiesManager.get} answers {@code null} to
     * every key while that field is unset, so a mock that does the same is what those readers would
     * have got anyway.
     */
    private ISystemProperties deploymentWideAllowSetTo(String value) {
        ISystemProperties properties = realSystemProperties == null
                ? mock(ISystemProperties.class)
                : mock(ISystemProperties.class, delegatesTo(realSystemProperties));
        // doReturn rather than when(...).thenReturn: the latter would call through to the delegate
        // while recording the stub.
        doReturn(value).when(properties)
                .get(SAML2Constants.SP_ALLOW_UNSIGNED_ARTIFACT_ASSERTIONS_PROPERTY);
        return properties;
    }

    /**
     * A hosted SP whose assertion consumer service publishes the given bindings.
     */
    private SAML2MetaManager spPublishing(String... acsBindings) throws Exception {
        List<AssertionConsumerServiceElement> services = new ArrayList<>();
        for (String binding : acsBindings) {
            AssertionConsumerServiceElement acs = mock(AssertionConsumerServiceElement.class);
            when(acs.getBinding()).thenReturn(binding);
            services.add(acs);
        }
        SPSSODescriptorElement descriptor = mock(SPSSODescriptorElement.class);
        when(descriptor.getAssertionConsumerService()).thenReturn(services);

        SAML2MetaManager metaManager = mock(SAML2MetaManager.class);
        when(metaManager.getSPSSODescriptor(REALM, SP_ENTITY_ID)).thenReturn(descriptor);
        return metaManager;
    }

    /**
     * A meta manager that finds no service provider under that entity id, as it does for an entity
     * of another realm or one with no service provider role.
     */
    private SAML2MetaManager noSuchSp() throws Exception {
        SAML2MetaManager metaManager = mock(SAML2MetaManager.class);
        when(metaManager.getSPSSODescriptor(anyString(), anyString())).thenReturn(null);
        return metaManager;
    }

    /**
     * Extended metadata of a hosted SP holding the given attributes, as name, value pairs.
     */
    private SPSSOConfigElement spConfig(String... nameThenValue) {
        List<AttributeType> attributes = new ArrayList<>();
        for (int i = 0; i < nameThenValue.length; i += 2) {
            AttributeType attribute = mock(AttributeType.class);
            when(attribute.getName()).thenReturn(nameThenValue[i]);
            when(attribute.getValue()).thenReturn(Arrays.asList(nameThenValue[i + 1]));
            attributes.add(attribute);
        }
        SPSSOConfigElement config = mock(SPSSOConfigElement.class);
        when(config.getAttribute()).thenReturn(attributes);
        return config;
    }

    /**
     * The extended metadata of an SP that has asked for the relaxation and verifies the
     * {@code ArtifactResponse}, which is the only combination that can reach it.
     */
    private SPSSOConfigElement relaxingSpConfig() {
        return spConfig(
                SAML2Constants.SP_ALLOW_UNSIGNED_ARTIFACT_ASSERTIONS, "true",
                SAML2Constants.WANT_ARTIFACT_RESPONSE_SIGNED, "true");
    }

    @Test
    public void encodeDecodeTest() {

        // the max length of each random String to encode
        int maxStringLength = 300;
        // the number of encode/decode iterations we want to test
        int randomStringsCount = 3000;
        Random R = new Random();

        int i = 0;
        while (i < randomStringsCount) {
            int size = R.nextInt(maxStringLength);
            // We don't want any 0 length arrays
            while (size == 0) {
                size = R.nextInt(maxStringLength);
            }
            i++;
            String randomString = RandomStringUtils.randomAlphanumeric(size);
            String encoded = SAML2Utils.encodeForRedirect(randomString);
            String decoded = SAML2Utils.decodeFromRedirect(URLEncDec.decode(encoded));
            assertThat(decoded).isEqualTo(randomString);
        }
    }

    @Test
    public void getMappedAttributesTest() {

        List<String> mappings = new ArrayList<>(6);

        mappings.add("invalid entry");
        mappings.add("name1=value");
        mappings.add("name2=\"static value\"");
        mappings.add("name3=\"static cn=value\"");
        mappings.add("urn:oasis:names:tc:SAML:2.0:attrname-format:uri|urn:mace:dir:attribute-def:name4=value");
        mappings.add("urn:oasis:names:tc:SAML:2.0:attrname-format:uri|name5=\"static value\"");

        Map<String, String> mappedAttributes = SAML2Utils.getMappedAttributes(mappings);

        assertThat(mappedAttributes).isNotNull().hasSize(5);
        assertThat(mappedAttributes).containsEntry("name1", "value");
        assertThat(mappedAttributes).containsEntry("name2", "\"static value\"");
        assertThat(mappedAttributes).containsEntry("name3", "\"static cn=value\"");
        assertThat(mappedAttributes).containsEntry("urn:oasis:names:tc:SAML:2.0:attrname-format:uri|urn:mace:dir:attribute-def:name4", "value");
        assertThat(mappedAttributes).containsEntry("urn:oasis:names:tc:SAML:2.0:attrname-format:uri|name5", "\"static value\"");
    }

    /**
     * GHSA-3j2f-qhrc-rjfj: the signature requirement used to apply to HTTP-POST alone, leaving an
     * assertion delivered over ECP/PAOS - encrypted, so that SPACSUtils never saw it in cleartext -
     * with nothing but the SP metadata flag in front of it. It now covers every binding.
     */
    @Test
    public void assertionSignatureIsRequiredOnEveryBindingByDefault() {

        String[] bindings = {
            SAML2Constants.HTTP_POST,
            SAML2Constants.PAOS,
            SAML2Constants.HTTP_ARTIFACT,
            SAML2Constants.SOAP,
            SAML2Constants.HTTP_REDIRECT
        };

        for (String binding : bindings) {
            assertThat(SAML2Utils.isAssertionSignatureRequired(false, binding, false))
                    .as("signature required on %s with WantAssertionsSigned=false", binding)
                    .isTrue();
        }
    }

    /**
     * The relaxation reaches HTTP-Artifact and nothing else. Letting it reach ECP/PAOS would restore
     * GHSA-3j2f-qhrc-rjfj in full: the hosted SP template ships {@code WantAssertionsSigned="false"}
     * and publishes a PAOS assertion consumer service, so an unsigned response with an encrypted
     * assertion posted to it would be accepted again.
     */
    @Test
    public void unsignedAssertionsAreOnlyEverAcceptedOverArtifact() {

        assertThat(SAML2Utils.isAssertionSignatureRequired(false, SAML2Constants.HTTP_ARTIFACT, true))
                .as("HTTP-Artifact, the one binding the deployment may relax").isFalse();

        assertThat(SAML2Utils.isAssertionSignatureRequired(false, SAML2Constants.PAOS, true))
                .as("ECP/PAOS carries the response as front-channel input and is never relaxed")
                .isTrue();
        assertThat(SAML2Utils.isAssertionSignatureRequired(false, SAML2Constants.HTTP_POST, true))
                .as("HTTP-POST is mandated by the SAML profile and is never relaxed").isTrue();
    }

    @Test
    public void wantAssertionsSignedSurvivesTheRelaxation() {

        assertThat(SAML2Utils.isAssertionSignatureRequired(true, SAML2Constants.HTTP_ARTIFACT, true))
                .as("SP metadata asks for signed assertions").isTrue();
        assertThat(SAML2Utils.isAssertionSignatureRequired(true, SAML2Constants.PAOS, true))
                .as("SP metadata asks for signed assertions").isTrue();
    }

    /**
     * A binding the decision does not recognise has to fail closed, {@code null} included - the
     * caller of {@code verifyResponse} decides what to pass, and an unrecognised value must not
     * inherit the one relaxation there is.
     */
    @Test
    public void unrecognisedBindingFailsClosed() {

        assertThat(SAML2Utils.isAssertionSignatureRequired(false, null, true))
                .as("a null binding is not HTTP-Artifact").isTrue();
        assertThat(SAML2Utils.isAssertionSignatureRequired(false, "urn:example:binding:made-up", true))
                .as("an unknown binding is not HTTP-Artifact").isTrue();
        assertThat(SAML2Utils.isAssertionSignatureRequired(false, SAML2Constants.SOAP, true))
                .as("SOAP is not the artifact ACS binding").isTrue();
    }

    /**
     * The per-SP attribute decides for the SP that sets it; an SP that leaves it unset follows the
     * deployment-wide property.
     */
    @Test
    public void perSpAttributeOverridesTheDeploymentWideDefault() {

        assertThat(SAML2Utils.isUnsignedArtifactAssertionAllowed("true", false))
                .as("this SP accepts unsigned artifact assertions, the deployment does not").isTrue();
        assertThat(SAML2Utils.isUnsignedArtifactAssertionAllowed("false", true))
                .as("this SP requires signed assertions, the deployment is lenient").isFalse();
    }

    @Test
    public void unsetPerSpAttributeFallsBackToTheDeploymentWideDefault() {

        for (String unset : new String[] { null, "", "   " }) {
            assertThat(SAML2Utils.isUnsignedArtifactAssertionAllowed(unset, false))
                    .as("unset attribute [%s], strict deployment", unset).isFalse();
            assertThat(SAML2Utils.isUnsignedArtifactAssertionAllowed(unset, true))
                    .as("unset attribute [%s], lenient deployment", unset).isTrue();
        }
    }

    /**
     * Anything that is not {@code true} leaves the signature required. An attribute holding a typo
     * must not be read as an opt-out.
     *
     * <p>{@code TRUE} among them: {@code wantArtifactResponseSigned}, which gates this one, has to
     * be compared exactly as {@code SPACSUtils.getResponseFromArtifact} compares it when it decides
     * whether to verify the {@code ArtifactResponse} at all. A spelling that relaxed the signature
     * requirement without triggering that verification would be the worst of both.
     */
    @Test
    public void onlyTrueRelaxesTheRequirement() {

        assertThat(SAML2Utils.isUnsignedArtifactAssertionAllowed(" true ", false))
                .as("surrounding whitespace is not a typo").isTrue();

        for (String notTrue : new String[] { "TRUE", "True", "yes", "1", "on", "tru", "false" }) {
            assertThat(SAML2Utils.isUnsignedArtifactAssertionAllowed(notTrue, true))
                    .as("[%s] is not true, so the SP requires the signature", notTrue).isFalse();
        }
    }

    /**
     * {@code SPACSUtils.getResponseFromPost} labels a response HTTP-Artifact whenever the request
     * carries a {@code SAMLart} parameter, and that endpoint is gated on the HTTP-POST binding. The
     * binding on its own is therefore not evidence that this SP does artifact at all, so the
     * relaxation is refused unless the assertion consumer service publishes HTTP-Artifact.
     */
    @Test
    public void relaxationIsRefusedWhenTheSpPublishesNoArtifactEndpoint() throws Exception {

        useMetaManager(spPublishing(SAML2Constants.HTTP_POST));

        assertThat(SAML2Utils.isUnsignedArtifactAssertionAllowed(relaxingSpConfig(), REALM, SP_ENTITY_ID))
                .as("an SP that publishes HTTP-POST alone gets no artifact relaxation").isFalse();
    }

    /**
     * The counterpart of the test above: the same attributes, on an SP that does publish the
     * binding, do reach the relaxation. Without this one the test above would pass just as well if
     * the relaxation were unreachable for every SP there is.
     */
    @Test
    public void relaxationReachesAnSpThatPublishesArtifact() throws Exception {

        useMetaManager(spPublishing(SAML2Constants.HTTP_POST, SAML2Constants.HTTP_ARTIFACT));

        assertThat(SAML2Utils.isUnsignedArtifactAssertionAllowed(relaxingSpConfig(), REALM, SP_ENTITY_ID))
                .as("the SP publishes HTTP-Artifact and asked for the relaxation").isTrue();
    }

    /**
     * An entity of another realm, or one with no service provider role, publishes nothing - and has
     * to fail closed rather than be dereferenced.
     */
    @Test
    public void relaxationIsRefusedForAnEntityWithNoServiceProviderMetadata() throws Exception {

        useMetaManager(noSuchSp());

        assertThat(SAML2Utils.isUnsignedArtifactAssertionAllowed(relaxingSpConfig(), REALM, SP_ENTITY_ID))
                .as("no metadata, no relaxation").isFalse();
    }

    /**
     * GHSA-3j2f-qhrc-rjfj: the back channel is worth something only where the {@code
     * ArtifactResponse} is verified, which {@code SPACSUtils.getResponseFromArtifact} does when
     * {@code wantArtifactResponseSigned} is {@code true}. Unverified, nothing ties the response to
     * the identity provider the artifact was resolved against, so the assertion signature cannot be
     * dropped on top of it.
     */
    @Test
    public void relaxationIsRefusedWhenTheArtifactResponseIsNotVerified() throws Exception {

        useMetaManager(spPublishing(SAML2Constants.HTTP_ARTIFACT));

        assertThat(SAML2Utils.isUnsignedArtifactAssertionAllowed(
                spConfig(SAML2Constants.SP_ALLOW_UNSIGNED_ARTIFACT_ASSERTIONS, "true"),
                REALM, SP_ENTITY_ID))
                .as("the ArtifactResponse is not verified at all").isFalse();

        assertThat(SAML2Utils.isUnsignedArtifactAssertionAllowed(
                spConfig(SAML2Constants.SP_ALLOW_UNSIGNED_ARTIFACT_ASSERTIONS, "true",
                        SAML2Constants.WANT_ARTIFACT_RESPONSE_SIGNED, ""),
                REALM, SP_ENTITY_ID))
                .as("an empty wantArtifactResponseSigned verifies nothing").isFalse();

        assertThat(SAML2Utils.isUnsignedArtifactAssertionAllowed(
                spConfig(SAML2Constants.SP_ALLOW_UNSIGNED_ARTIFACT_ASSERTIONS, "true",
                        SAML2Constants.WANT_ARTIFACT_RESPONSE_SIGNED, "TRUE"),
                REALM, SP_ENTITY_ID))
                .as("SPACSUtils compares that value exactly, so anything else keeps the signature")
                .isFalse();
    }

    /**
     * An SP with no extended metadata at all cannot have asked for the relaxation, and cannot be
     * verifying the {@code ArtifactResponse} either.
     */
    @Test
    public void relaxationIsRefusedForAnSpWithNoExtendedMetadata() throws Exception {

        useMetaManager(spPublishing(SAML2Constants.HTTP_ARTIFACT));

        assertThat(SAML2Utils.isUnsignedArtifactAssertionAllowed(null, REALM, SP_ENTITY_ID))
                .as("no extended metadata, no relaxation").isFalse();
    }

    /**
     * The deployment-wide fallback is read with the rule the per-SP attribute is read with: the
     * literal {@code "true"}, and nothing else, relaxes anything. It used to go through
     * {@code SystemPropertiesManager.getAsBoolean}, which is case-insensitive - so a {@code TRUE}
     * there would have dropped the assertion signature requirement while
     * {@code wantArtifactResponseSigned}, which {@code SPACSUtils.getResponseFromArtifact} compares
     * byte for byte, left the {@code ArtifactResponse} unverified. That pair is what the gate above
     * exists to prevent.
     *
     * <p>This drives the overload production calls rather than the one that takes the answer
     * already parsed, so it pins the read itself and not only the decision it feeds.
     */
    @Test
    public void theDeploymentWidePropertyIsReadAsStrictlyAsThePerSpAttribute() throws Exception {

        useMetaManager(spPublishing(SAML2Constants.HTTP_ARTIFACT));
        // verifies the ArtifactResponse, and leaves the per-SP attribute unset: the only shape in
        // which the deployment-wide property decides anything.
        SPSSOConfigElement inheritingSp =
                spConfig(SAML2Constants.WANT_ARTIFACT_RESPONSE_SIGNED, "true");

        useSystemProperties(deploymentWideAllowSetTo("true"));
        assertThat(SAML2Utils.isUnsignedArtifactAssertionAllowed(inheritingSp, REALM, SP_ENTITY_ID))
                .as("the SP inherits, and this deployment relaxes the artifact binding").isTrue();

        for (String notTrue : new String[] { "TRUE", "True", "yes", "1", "on", null }) {
            useSystemProperties(deploymentWideAllowSetTo(notTrue));
            assertThat(SAML2Utils.isUnsignedArtifactAssertionAllowed(inheritingSp, REALM, SP_ENTITY_ID))
                    .as("[%s] is not true, so the deployment relaxes nothing", notTrue).isFalse();
        }
    }

    /**
     * The overload {@code verifyResponse} actually calls, rather than the decision it delegates to.
     * The fast path in front of that delegation - anything that is not an artifact response, and
     * any SP that asked for signed assertions - has to agree with it on every binding.
     */
    @Test
    public void theDecisionVerifyResponseCallsRelaxesArtifactAndNothingElse() throws Exception {

        useMetaManager(spPublishing(SAML2Constants.HTTP_POST, SAML2Constants.HTTP_ARTIFACT,
                SAML2Constants.PAOS));
        SPSSOConfigElement spConfig = relaxingSpConfig();

        assertThat(SAML2Utils.isAssertionSignatureRequired(false, SAML2Constants.HTTP_ARTIFACT,
                spConfig, REALM, SP_ENTITY_ID))
                .as("HTTP-Artifact, relaxed by this SP").isFalse();

        String[] neverRelaxed = {
            SAML2Constants.HTTP_POST,
            SAML2Constants.PAOS,
            SAML2Constants.SOAP,
            SAML2Constants.HTTP_REDIRECT,
            "urn:example:binding:made-up",
            null
        };
        for (String binding : neverRelaxed) {
            assertThat(SAML2Utils.isAssertionSignatureRequired(false, binding, spConfig, REALM,
                    SP_ENTITY_ID))
                    .as("signature required on %s even for an SP that relaxed artifact", binding)
                    .isTrue();
        }

        assertThat(SAML2Utils.isAssertionSignatureRequired(true, SAML2Constants.HTTP_ARTIFACT,
                spConfig, REALM, SP_ENTITY_ID))
                .as("WantAssertionsSigned survives the relaxation here too").isTrue();
    }

    /**
     * And the same entry point, for an SP that has asked for nothing.
     */
    @Test
    public void theDecisionVerifyResponseCallsRequiresTheSignatureByDefault() throws Exception {

        useMetaManager(spPublishing(SAML2Constants.HTTP_POST, SAML2Constants.HTTP_ARTIFACT));

        assertThat(SAML2Utils.isAssertionSignatureRequired(false, SAML2Constants.HTTP_ARTIFACT,
                spConfig(), REALM, SP_ENTITY_ID))
                .as("an SP that asked for nothing keeps the signature requirement").isTrue();
    }

    /**
     * The default configuration of an OpenAM SP - no attribute, no property - requires the signature
     * on every binding.
     */
    @Test
    public void defaultConfigurationRequiresTheSignatureEverywhere() {

        boolean allowed = SAML2Utils.isUnsignedArtifactAssertionAllowed(null, false);

        String[] bindings = {
            SAML2Constants.HTTP_POST,
            SAML2Constants.PAOS,
            SAML2Constants.HTTP_ARTIFACT
        };

        for (String binding : bindings) {
            assertThat(SAML2Utils.isAssertionSignatureRequired(false, binding, allowed))
                    .as("signature required on %s out of the box", binding).isTrue();
        }
    }

    /**
     * SAML 2.0 Core 5.3: an assertion inherits the signature of the enclosing element, so a
     * signature on either the response or every assertion satisfies the requirement.
     */
    @Test
    public void eitherSignatureSatisfiesTheRequirement() {

        assertThat(SAML2Utils.isMissingRequiredSignature(true, false, () -> true))
                .as("the response carries the signature the assertions inherit").isFalse();
        assertThat(SAML2Utils.isMissingRequiredSignature(false, true, () -> true))
                .as("every assertion carries one of its own").isFalse();
        assertThat(SAML2Utils.isMissingRequiredSignature(false, false, () -> true))
                .as("nothing is signed and this deployment requires a signature").isTrue();
        assertThat(SAML2Utils.isMissingRequiredSignature(false, false, () -> false))
                .as("nothing is signed and this deployment has relaxed the requirement").isFalse();
    }

    /**
     * And the requirement is resolved only where it can still change the answer: it goes to the SP's
     * extended metadata and to the system properties, on a path every SAML2 response takes.
     */
    @Test
    public void theRequirementIsNotResolvedForAResponseThatCarriesASignature() {

        BooleanSupplier neverAsked = () -> {
            throw new AssertionError("the signature requirement was resolved for a signed response");
        };

        assertThat(SAML2Utils.isMissingRequiredSignature(true, false, neverAsked)).isFalse();
        assertThat(SAML2Utils.isMissingRequiredSignature(false, true, neverAsked)).isFalse();
        assertThat(SAML2Utils.isMissingRequiredSignature(true, true, neverAsked)).isFalse();
    }

    /**
     * GHSA-3j2f-qhrc-rjfj: the reachable variant. The ECP profile requires every assertion to be
     * signed in its own right, so the signature inheritance above does not apply there - and the
     * assertion that arrives encrypted is opaque to {@code SPACSUtils.getResponseFromPostECP},
     * which is where the cleartext ones are checked. This is the rule that covers it, applied to the
     * list {@code verifyResponse} decrypted them into.
     */
    @Test
    public void anUnsignedEcpAssertionIsRefusedEvenInsideASignedResponse() {

        assertThat(SAML2Utils.isEcpAssertionSignatureMissing(SAML2Constants.PAOS, false))
                .as("an assertion delivered over ECP/PAOS carries no signature of its own").isTrue();
        assertThat(SAML2Utils.isEcpAssertionSignatureMissing(SAML2Constants.PAOS, true))
                .as("every ECP assertion is signed").isFalse();
    }

    /**
     * The ECP rule is the ECP binding's alone: every other binding is left to the requirement above,
     * which a signature on the enclosing response satisfies.
     */
    @Test
    public void theEcpRuleAppliesToNoOtherBinding() {

        String[] otherBindings = {
            SAML2Constants.HTTP_POST,
            SAML2Constants.HTTP_ARTIFACT,
            SAML2Constants.SOAP,
            SAML2Constants.HTTP_REDIRECT,
            "urn:example:binding:made-up",
            null
        };

        for (String binding : otherBindings) {
            assertThat(SAML2Utils.isEcpAssertionSignatureMissing(binding, false))
                    .as("%s is not ECP/PAOS", binding).isFalse();
        }
    }

    /**
     * The one-time marker of a bearer assertion has to last as long as the assertion is accepted,
     * which is its {@code NotOnOrAfter} plus {@code assertionTimeSkew}. A marker that expired at the
     * {@code NotOnOrAfter} alone would leave the assertion replayable for the length of the skew
     * once the SAML2 token repository had dropped it.
     */
    @Test
    public void theOneTimeMarkerLastsAsLongAsTheAssertionIsAccepted() {

        Date notOnOrAfter = new Date(1_000_000L);

        assertThat(SAML2Utils.bearerAcceptedUntil(notOnOrAfter, 300))
                .as("NotOnOrAfter plus the skew, in milliseconds").isEqualTo(1_300_000L);
        assertThat(SAML2Utils.bearerAcceptedUntil(notOnOrAfter, 0))
                .as("no skew configured").isEqualTo(1_000_000L);
        assertThat(SAML2Utils.bearerAcceptedUntil(null, 300))
                .as("no NotOnOrAfter to start from").isNull();
    }
}
