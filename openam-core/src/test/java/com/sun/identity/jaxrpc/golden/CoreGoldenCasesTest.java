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
package com.sun.identity.jaxrpc.golden;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.iplanet.am.sdk.remote.DirectoryManagerIF;
import com.sun.identity.entity.EntityObjectIF;
import com.sun.identity.shared.jaxrpc.golden.GoldenCase;
import com.sun.identity.shared.jaxrpc.golden.GoldenCases;
import com.sun.identity.sm.jaxrpc.SMSObjectIF;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class CoreGoldenCasesTest {

    private static final List<Class<?>> CORE_INTERFACES =
            Arrays.asList(SMSObjectIF.class, DirectoryManagerIF.class, EntityObjectIF.class);

    private Locale savedLocale;

    @BeforeClass(alwaysRun = true)
    public void pinLocale() {
        savedLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
        ResourceBundle.clearCache();
    }

    @AfterClass(alwaysRun = true)
    public void restoreLocale() {
        Locale.setDefault(savedLocale);
        ResourceBundle.clearCache();
    }

    @Test
    public void catalogueSizesMatchTheInterfaces() {
        // ok + fault + NoArg + edges
        assertThat(CoreGoldenCases.forInterface(SMSObjectIF.class)).hasSize(20 + 25 + 1 + 37);
        assertThat(CoreGoldenCases.forInterface(DirectoryManagerIF.class)).hasSize(76 + 128 + 1 + 4);
        assertThat(CoreGoldenCases.forInterface(EntityObjectIF.class)).hasSize(5 + 10);
    }

    @Test
    public void idsAreUniquePerInterface() {
        List<String> ids = CoreGoldenCases.forInterface(DirectoryManagerIF.class).stream()
                .map(GoldenCase::id)
                .collect(Collectors.toList());
        assertThat(new HashSet<>(ids)).hasSameSizeAs(ids);
    }

    @Test
    public void everyDeclaredExceptionConstructs() {
        int faults = 0;
        for (Class<?> iface : CORE_INTERFACES) {
            for (GoldenCase c : CoreGoldenCases.forInterface(iface)) {
                if (!c.id().contains("__fault")) {
                    continue;
                }
                faults++;
                assertThat(c.thrown()).as(c.id()).isNotNull();
                assertThat(declaredExceptions(iface, c.operation())).as(c.id())
                        .contains(c.thrown().getClass());
            }
        }
        // 25 + 128 + 10 declared, the 2 NoArg cases and the 2 null-argument edges
        assertThat(faults).isEqualTo(25 + 128 + 10 + 2 + 2);
    }

    @Test
    public void targetReturnsResultOnlyForItsOperation() throws Exception {
        GoldenCase c = byId(SMSObjectIF.class, "getRootSuffix__ok");
        List<Object[]> received = new ArrayList<>();
        SMSObjectIF t = (SMSObjectIF) GoldenCases.target(c, received);
        assertThat(t.getRootSuffix()).isEqualTo("result");
        assertThat(received).hasSize(1);
        assertThat(t.toString()).isNotNull();
        assertThatThrownBy(() -> t.getAMSdkBaseDN()).isInstanceOf(AssertionError.class);
    }

    private static GoldenCase byId(Class<?> iface, String id) {
        return CoreGoldenCases.forInterface(iface).stream()
                .filter(c -> c.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no case " + id));
    }

    private static Set<Class<?>> declaredExceptions(Class<?> iface, String operation) {
        for (Method m : iface.getMethods()) {
            if (m.getName().equals(operation)) {
                Set<Class<?>> declared = new HashSet<>(Arrays.asList(m.getExceptionTypes()));
                declared.remove(RemoteException.class);
                return declared;
            }
        }
        throw new AssertionError("no method " + operation);
    }
}
