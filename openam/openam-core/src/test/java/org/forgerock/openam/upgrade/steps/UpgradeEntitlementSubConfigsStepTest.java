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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openam.upgrade.steps;

import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.Application;
import com.sun.identity.entitlement.ApplicationType;
import com.sun.identity.entitlement.DenyOverride;
import com.sun.identity.entitlement.EntitlementConfiguration;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.ResourceMatch;
import com.sun.identity.entitlement.ResourceSaveIndexes;
import com.sun.identity.entitlement.ResourceSearchIndexes;
import com.sun.identity.entitlement.interfaces.ISaveIndex;
import com.sun.identity.entitlement.interfaces.ISearchIndex;
import com.sun.identity.entitlement.interfaces.ResourceName;
import com.sun.identity.shared.xml.XMLUtils;
import org.forgerock.openam.upgrade.UpgradeException;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.mockito.ArgumentMatcher;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.w3c.dom.Document;

import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit test to exercise the behaviour of {@link UpgradeEntitlementSubConfigsStep}.
 */
public class UpgradeEntitlementSubConfigsStepTest {

    private static final String DETAILED_REPORT = "Entitlement Application Types and Applications Report-" +
            "-------------------------------------------------------%ENTITLEMENT_DATA%-";
    private static final Map<String, Boolean> TYPE_ACTIONS = new HashMap<String, Boolean>();

    private static final String DEFAULT_COMBINER = "DenyOverride";

    private UpgradeStep upgradeStep;

    private EntitlementConfiguration entitlementService;
    private PrivilegedAction<SSOToken> adminTokenAction;
    private ConnectionFactory connectionFactory;

    private Set<ApplicationType> mockTypes;
    private Set<Application> mockApplications;
    private ApplicationType type1;
    private Application app;

    static {
        TYPE_ACTIONS.put("CREATE", Boolean.TRUE);
        TYPE_ACTIONS.put("READ", Boolean.TRUE);
        TYPE_ACTIONS.put("UPDATE", Boolean.TRUE);
        TYPE_ACTIONS.put("DELETE", Boolean.TRUE);
        TYPE_ACTIONS.put("PATCH", Boolean.TRUE);
        TYPE_ACTIONS.put("ACTION", Boolean.TRUE);
        TYPE_ACTIONS.put("QUERY", Boolean.TRUE);
    }

    @BeforeMethod
    public void setUp() throws IllegalAccessException, InstantiationException {
        mockTypes = new HashSet<ApplicationType>(3);
        final ApplicationType type1 = newType("type1");
        final ApplicationType type2 = newType("type2");
        final ApplicationType type3 = newType("type3");
        mockTypes.addAll(Arrays.asList(type1, type2, type3));
        this.type1 = type1;

        mockApplications = new HashSet<Application>(3);
        final Application application1 = newApplication("application1", type1);
        final Application application2 = newApplication("application2", type1);
        final Application application3 = newApplication("application3", type1);
        mockApplications.addAll(Arrays.asList(application1, application2, application3));

        entitlementService = mock(EntitlementConfiguration.class);
        adminTokenAction = mock(PrivilegedAction.class);
        connectionFactory = mock(ConnectionFactory.class);
        upgradeStep = new SafeUpgradeEntitlementSubConfigsStep(
                entitlementService, adminTokenAction, connectionFactory);

        final HashSet<String> conditions = new HashSet<String>();
        conditions.add("condition.entry.1");
        conditions.add("condition.entry.2");

        final HashSet<String> subjects = new HashSet<String>();
        subjects.add("subject.entry.1");
        subjects.add("subject.entry.2");

        final HashSet<String> resources = new HashSet<String>();
        resources.add("http://*");
        resources.add("https://*");

        app = newApplication("application4", type1);
        app.setConditions(conditions);
        app.setSubjects(subjects);
        app.setResources(resources);
        app.setEntitlementCombinerName(DEFAULT_COMBINER);
    }

    @Test
    public void noNewTypesOrApplications() throws UpgradeException, InstantiationException, IllegalAccessException {
        // Both application type 4 and app (application 4) exist, no need to add either.
        mockTypes.add(newType("type4"));
        mockApplications.add(app);

        when(entitlementService.getApplicationTypes()).thenReturn(mockTypes);
        when(entitlementService.getApplications()).thenReturn(mockApplications);

        upgradeStep.initialize();

        assertThat(upgradeStep.isApplicable()).isFalse();
        assertThat(upgradeStep.getShortReport("-")).isEqualTo("");
        assertThat(upgradeStep.getDetailedReport("-")).isEqualTo(
                DETAILED_REPORT.replace("%ENTITLEMENT_DATA%", ""));

        upgradeStep.perform();

        verify(entitlementService, atMost(2)).getApplicationTypes();
        verify(entitlementService, times(2)).getApplications();
        verifyNoMoreInteractions(entitlementService, adminTokenAction, connectionFactory);
    }

    @Test
    public void newTypeNoNewApplications() throws UpgradeException, EntitlementException {
        // Application 4 already exists, no need to add.
        mockApplications.add(app);

        when(entitlementService.getApplicationTypes()).thenReturn(Collections.<ApplicationType>emptySet());
        when(entitlementService.getApplications()).thenReturn(mockApplications);

        upgradeStep.initialize();

        assertThat(upgradeStep.isApplicable()).isTrue();
        assertThat(upgradeStep.getShortReport("-")).isEqualTo("New entitlement application types-");
        assertThat(upgradeStep.getDetailedReport("-")).isEqualTo(
                DETAILED_REPORT.replace("%ENTITLEMENT_DATA%", "New entitlement application types:-type4-"));

        upgradeStep.perform();

        verify(entitlementService, atMost(2)).getApplicationTypes();
        verify(entitlementService, atMost(5)).getApplications();
        verify(entitlementService).storeApplicationType(argThat(new TypeMatch()));
        verifyNoMoreInteractions(entitlementService, adminTokenAction, connectionFactory);
    }

    @Test
    public void noNewTypesNewApplication()
            throws UpgradeException, EntitlementException, InstantiationException, IllegalAccessException {
        // Application type 4 already exists, no need to add.
        mockTypes.add(newType("type4"));

        when(entitlementService.getApplicationTypes()).thenReturn(mockTypes);
        when(entitlementService.getApplications()).thenReturn(Collections.<Application>emptySet());

        upgradeStep.initialize();

        assertThat(upgradeStep.isApplicable()).isTrue();
        assertThat(upgradeStep.getShortReport("-")).isEqualTo("New entitlement applications-");
        assertThat(upgradeStep.getDetailedReport("-")).isEqualTo(
                DETAILED_REPORT.replace("%ENTITLEMENT_DATA%", "New entitlement applications:-application4-"));

        when(entitlementService.getApplications()).thenReturn(Collections.singleton(app));
        when(entitlementService.getApplicationTypes()).thenReturn(Collections.singleton(type1));
        upgradeStep.perform();

        verify(entitlementService, atLeastOnce()).storeApplication(argThat(new ApplicationMatch()));
    }

    @Test
    public void newTypeNewApplication() throws UpgradeException, EntitlementException {
        when(entitlementService.getApplicationTypes()).thenReturn(Collections.<ApplicationType>emptySet());
        when(entitlementService.getApplications()).thenReturn(Collections.<Application>emptySet());

        upgradeStep.initialize();

        assertThat(upgradeStep.isApplicable()).isTrue();
        assertThat(upgradeStep.getShortReport("-")).isEqualTo("New entitlement application types-" +
                "New entitlement applications-");
        assertThat(upgradeStep.getDetailedReport("-")).isEqualTo(
                DETAILED_REPORT.replace("%ENTITLEMENT_DATA%",
                        "New entitlement application types:-type4-New entitlement applications:-application4-"));

        when(entitlementService.getApplicationTypes()).thenReturn(Collections.singleton(type1));
        when(entitlementService.getApplications()).thenReturn(Collections.singleton(app));

        upgradeStep.perform();

        verify(entitlementService).storeApplicationType(argThat(new TypeMatch()));
        verify(entitlementService, atLeastOnce()).storeApplication(argThat(new ApplicationMatch()));
    }

    @Test
    public void modifiedApplicationType() throws Exception {
        //application type type4 does not have the UPDATE action, so it needs to be upgraded
        ApplicationType type = newType("type4");
        type.getActions().remove("UPDATE");
        mockTypes.add(type);
        mockApplications.add(app);

        when(entitlementService.getApplicationTypes()).thenReturn(mockTypes);
        when(entitlementService.getApplications()).thenReturn(mockApplications);

        upgradeStep.initialize();

        assertThat(upgradeStep.isApplicable()).isTrue();

        upgradeStep.perform();

        verify(entitlementService, atMost(3)).getApplicationTypes();
        verify(entitlementService, atMost(5)).getApplications();
        verify(entitlementService).storeApplicationType(argThat(new TypeMatch()));
    }

    @Test
    public void shouldUpdateApplicationResourcePatterns() throws Exception {
        // Given
        ApplicationType appType = newType("type4");
        Application app = newApplication("application4", appType);
        final Set<String> oldResources = CollectionUtils.asSet("one", "two", "three");
        final Set<String> newResources = CollectionUtils.asSet("http://*", "https://*"); // test-entitlement.xml
        app.setResources(oldResources);

        when(entitlementService.getApplicationTypes()).thenReturn(Collections.singleton(appType));
        when(entitlementService.getApplications()).thenReturn(Collections.singleton(app));

        // When
        upgradeStep.initialize();
        upgradeStep.perform();

        // Then
        assertThat(app.getResources()).isEqualTo(newResources);
        verify(entitlementService, atLeastOnce()).storeApplication(app);
    }

    @Test
    public void newApplicationDescriptionNoNewTypesOrApplications() throws UpgradeException, InstantiationException,
            IllegalAccessException, EntitlementException {
        // Both application type 4 and app (application 4) exist, no need to add either.
        mockTypes.add(newType("type4"));
        app.setDescription("New description.");
        mockApplications.add(app);

        when(entitlementService.getApplicationTypes()).thenReturn(mockTypes);
        when(entitlementService.getApplications()).thenReturn(mockApplications);

        upgradeStep.initialize();

        assertThat(upgradeStep.isApplicable()).isTrue();
        assertThat(upgradeStep.getShortReport("-")).isEqualTo("");
        assertThat(upgradeStep.getDetailedReport("-")).isEqualTo(
                DETAILED_REPORT.replace("%ENTITLEMENT_DATA%", ""));

        upgradeStep.perform();

        verify(entitlementService, atMost(2)).getApplicationTypes();
        verify(entitlementService, times(3)).getApplications();
        verify(entitlementService, atLeastOnce()).storeApplication(argThat(new ApplicationMatch()));
        verifyNoMoreInteractions(entitlementService, adminTokenAction, connectionFactory);
    }

    // Used to match the application as defined in the test xml.
    private static final class ApplicationMatch extends ArgumentMatcher<Application> {

        @Override
        public boolean matches(Object argument) {
            boolean matches = true;
            final Application application = (Application)argument;
            matches &= "application4".equals(application.getName());
            matches &= "type1".equals(application.getApplicationType().getName());
            matches &= collectionMatch(
                    Arrays.asList("http://*", "https://*"), application.getResources());
            matches &= collectionMatch(
                    Arrays.asList("subject.entry.1", "subject.entry.2"), application.getSubjects());
            matches &= collectionMatch(
                    Arrays.asList("condition.entry.1", "condition.entry.2"), application.getConditions());
            matches &= application.getEntitlementCombiner() instanceof DenyOverride;
            return matches;
        }

    }

    // Used to match an application type as defined in the test xml.
    private static final class TypeMatch extends ArgumentMatcher<ApplicationType> {

        @Override
        public boolean matches(Object argument) {
            boolean matches = true;
            final ApplicationType type = (ApplicationType)argument;
            matches &= "type4".equals(type.getName());
            matches &= TYPE_ACTIONS.equals(type.getActions());
            matches &= type.getSearchIndex() instanceof DumbSearchIndex;
            matches &= type.getSaveIndex() instanceof DumbSaveIndex;
            matches &= type.getResourceComparator() instanceof DumbResourceName;
            return matches;
        }

    }

    /**
     * Creates a new application type.
     *
     * @param name
     *         application type name
     *
     * @return new application type instance
     *
     * @throws IllegalAccessException
     *         should an error occur when creating the new type
     * @throws InstantiationException
     *         should an error occur when creating the new type
     */
    private static ApplicationType newType(final String name) throws IllegalAccessException, InstantiationException {
        return new ApplicationType(name, new HashMap<String, Boolean>(TYPE_ACTIONS), DumbSearchIndex.class,
                DumbSaveIndex.class, DumbResourceName.class);
    }

    /**
     * Creates a new application.
     *
     * @param name
     *         application name
     * @param type
     *         application type
     *
     * @return new application instance
     */
    private static Application newApplication(final String name, final ApplicationType type) {
        return new Application("/", name, type);
    }

    /**
     * Checks whether two collections are equal.
     *
     * @param expected
     *         expected collection values
     * @param actual
     *         actual collection values
     * @param <T>
     *         the type of the value held within the collections
     * @param <C>
     *         the type of the collections
     *
     * @return whether both collections match
     */
    private static <T, C extends Collection<T>> boolean collectionMatch(final C expected, final C actual) {
        if (expected == null && actual == null) {
            return true;
        }

        if (expected == null || actual == null) {
            return false;
        }

        if (expected.size() != actual.size()) {
            return false;
        }

        for (T value : expected) {
            if (!actual.contains(value)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Test class with the {@link org.forgerock.openam.upgrade.steps.UpgradeEntitlementSubConfigsStep#getEntitlementXML()}
     * method mocked out, so to retrieve the test xml instead.
     */
    private static final class SafeUpgradeEntitlementSubConfigsStep extends UpgradeEntitlementSubConfigsStep {

        public SafeUpgradeEntitlementSubConfigsStep(final EntitlementConfiguration entitlementService,
                                                    final PrivilegedAction<SSOToken> adminTokenAction,
                                                    final ConnectionFactory connectionFactory) {
            super(entitlementService, adminTokenAction, connectionFactory);
        }

        @Override
        protected Document getEntitlementXML() throws UpgradeException {
            try {
                return XMLUtils.getXMLDocument(ClassLoader.getSystemResourceAsStream("test-entitlement.xml"));
            } catch (Exception e) {
                throw new UpgradeException(e);
            }
        }

    }

    // A basic implementation of {@link ISearchIndex}.
    public static final class DumbSearchIndex implements ISearchIndex {

        @Override
        public ResourceSearchIndexes getIndexes(String resource, String realm) throws EntitlementException {
            throw new UnsupportedOperationException();
        }

    }

    // A basic implementation of {@link ISaveIndex}.
    public static final class DumbSaveIndex implements ISaveIndex {

        @Override
        public ResourceSaveIndexes getIndexes(String resource) {
            throw new UnsupportedOperationException();
        }

    }

    // A basic implementation of {@link ResourceName}.
    public static final class DumbResourceName implements ResourceName {

        @Override
        public Set getServiceTypeNames() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void initialize(Map configParams) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ResourceMatch compare(String origRes, String compRes, boolean wildcardCompare) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String append(String superResource, String subResource) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String getSubResource(String res, String superRes) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String canonicalize(String res) throws EntitlementException {
            throw new UnsupportedOperationException();
        }

        public String[] split(String res) {
            throw new UnsupportedOperationException("Not implemented");
        }

    }


}
