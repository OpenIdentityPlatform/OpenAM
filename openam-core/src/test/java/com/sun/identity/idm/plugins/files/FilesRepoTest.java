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
package com.sun.identity.idm.plugins.files;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;

import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class FilesRepoTest {

    private final FilesRepo repo = new FilesRepo();

    @Test
    public void constructFileKeepsOrdinaryNamesUnderTheTypeDirectory() throws Exception {
        File file = repo.constructFile("/var/openam/idRepo", IdType.USER, "demo.user-1");

        assertThat(file).isEqualTo(new File(new File("/var/openam/idRepo", "user"), "demo.user-1"));
    }

    @DataProvider
    public Object[][] namesEscapingTheRepository() {
        return new Object[][] {
            {"../../../etc/passwd"},
            {".."},
            {"."},
            {"x/../../y"},
            {"sub/dir"},
            {"..\\..\\x"},
            {"evil\0"},
            {"evil\r\nforged"},          // CR/LF: log-line forging and never a valid name
            {"tab\tname"},
            {""},
            {null},
        };
    }

    @Test(dataProvider = "namesEscapingTheRepository")
    public void constructFileRejectsNamesThatLeaveTheTypeDirectory(String name) {
        assertThatThrownBy(() -> repo.constructFile("/var/openam/idRepo", IdType.USER, name))
                .isInstanceOf(IdRepoException.class);
    }

    @Test
    public void fileFilterTreatsRegexMetaCharactersLiterally() {
        FilesRepo.FileRepoFileFilter filter = repo.new FileRepoFileFilter("user.1");

        assertThat(filter.accept(null, "user.1")).isTrue();
        assertThat(filter.accept(null, "userx1")).isFalse();
    }

    @Test
    public void fileFilterStillExpandsWildcards() {
        FilesRepo.FileRepoFileFilter filter = repo.new FileRepoFileFilter("us*r.1");

        assertThat(filter.accept(null, "user.1")).isTrue();
        assertThat(filter.accept(null, "USER.1")).isTrue();
        assertThat(filter.accept(null, "uSomeThingr.1")).isTrue();
        assertThat(filter.accept(null, "userx1")).isFalse();
        assertThat(repo.new FileRepoFileFilter("*").accept(null, "anything")).isTrue();
    }

    @Test
    public void fileFilterFoldsCaseBeyondAscii() {
        FilesRepo.FileRepoFileFilter filter = repo.new FileRepoFileFilter("ЖОР*");

        assertThat(filter.accept(null, "жора")).isTrue();
        assertThat(filter.accept(null, "Жора")).isTrue();
        assertThat(filter.accept(null, "жук")).isFalse();
    }

    @Test
    public void fileFilterDoesNotInterpretInjectedRegex() {
        // "(" would be a regex syntax error today; it must be a plain character.
        FilesRepo.FileRepoFileFilter filter = repo.new FileRepoFileFilter("a(b");

        assertThat(filter.accept(null, "a(b")).isTrue();
        assertThat(filter.accept(null, "ab")).isFalse();
    }
}
