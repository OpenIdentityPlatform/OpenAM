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
 * Copyright 2016 ForgeRock AS.
 */

package org.forgerock.openam.upgrade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.setup.TestSetupHelper.deleteDirectory;
import static org.forgerock.openam.setup.TestSetupHelper.extractZip;
import static org.mockito.BDDMockito.given;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.setup.ZipUtils;
import org.mockito.Mock;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class EmbeddedOpenDJBackupManagerTest {

    private final Random random = new Random();

    @Mock
    private Debug logger;
    @Mock
    private OpenDJUpgrader upgrader;

    private ZipUtils zipUtils;
    private File baseDirectoryZipExtractPath;
    private String baseDirectory;

    @BeforeMethod
    public void setup() {
        initMocks(this);
        zipUtils = new ZipUtils(logger);
        baseDirectoryZipExtractPath = new File("/tmp/base-directory-" + random.nextInt());
        baseDirectory = baseDirectoryZipExtractPath.getAbsolutePath() + File.separator + "base-directory";
    }

    @AfterMethod
    public void tearDown() throws Exception {
        deleteDirectory(baseDirectoryZipExtractPath);
    }

    @Test
    public void whenInUpgradeRequiredStateItShouldBackupOpenDjDirectory() throws Exception {
        EmbeddedOpenDJBackupManager openDJBackupManager = setupInstallOpenDJRequiringUpgrade();
        openDJBackupManager.createOpenDJBackup();
        verifyBackupZipFileCreated();
        verifyContentsOfBackupZip();
    }

    @Test
    public void whenInUpgradeRequiredStateItShouldCreateUpgradeDirectory() throws Exception {
        EmbeddedOpenDJBackupManager openDJBackupManager = setupInstallOpenDJRequiringUpgrade();
        openDJBackupManager.createOpenDJBackup();
        assertThat(new File(baseDirectory, "upgrade").exists()).isTrue();
    }

    private EmbeddedOpenDJBackupManager setupInstallOpenDJRequiringUpgrade() throws Exception {
        return setupInstallOpenDJRequiringUpgrade(true);
    }

    private EmbeddedOpenDJBackupManager setupInstallOpenDJRequiringUpgrade(boolean isUpgradeRequired) throws Exception {
        createBaseDirectory("installed-opendj-base-directory");
        given(upgrader.isUpgradeRequired()).willReturn(isUpgradeRequired);
        return new EmbeddedOpenDJBackupManager(logger, zipUtils, baseDirectory);
    }

    private void verifyBackupZipFileCreated() {
        Pattern pattern = Pattern.compile("^opendj\\.backup\\.\\d*\\.zip$");
        File backupsDirectory = new File(baseDirectory, "backups");
        assertThat(backupsDirectory.exists()).describedAs("backups directory was not created").isTrue();
        assertThat(backupsDirectory.listFiles()).describedAs("backups directory is empty").hasSize(1);
        String backupZipFile = backupsDirectory.list()[0];
        Matcher matcher = pattern.matcher(backupZipFile);
        assertThat(matcher.matches()).describedAs("backup zip does not match expected pattern").isTrue();
    }

    private void verifyContentsOfBackupZip() throws IOException {
        File backupsDirectory = new File(baseDirectory, "backups");
        String backupZipFile = backupsDirectory.list()[0];
        File backupZipExtractDirectory = new File(baseDirectoryZipExtractPath, "test-extract");
        extractZip(new File(backupsDirectory, backupZipFile), backupZipExtractDirectory.toPath());

        assertThat(new File(backupZipExtractDirectory, "opends/bat/status.bat").exists())
                .describedAs("backup zip is not correct").isTrue();
        assertThat(new File(backupZipExtractDirectory, "opends/bin/status").exists())
                .describedAs("backup zip is not correct").isTrue();
        assertThat(new File(backupZipExtractDirectory, "opends/config/buildinfo").exists())
                .describedAs("backup zip is not correct").isTrue();
        assertThat(new File(backupZipExtractDirectory, "opends/db/userRoot/je.config.csv").exists())
                .describedAs("backup zip is not correct").isTrue();
        assertThat(new File(backupZipExtractDirectory, "opends/ldif/openam_suffix.ldif").exists())
                .describedAs("backup zip is not correct").isTrue();
        assertThat(new File(backupZipExtractDirectory, "opends/logs/errors").exists())
                .describedAs("backup zip is not correct").isTrue();
        assertThat(new File(backupZipExtractDirectory, "opends/template/config/buildinfo").exists())
                .describedAs("backup zip is not correct").isTrue();
    }

    private void createBaseDirectory(String baseDirectoryTemplateName) throws IOException {
        ClassLoader classLoader = getClass().getClassLoader();
        File baseDirectoryZipFile = new File(classLoader.getResource(
                "setup" + File.separator + baseDirectoryTemplateName + ".zip").getFile());
        extractZip(baseDirectoryZipFile, baseDirectoryZipExtractPath.toPath());
    }
}
