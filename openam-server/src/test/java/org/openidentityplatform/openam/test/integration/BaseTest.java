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
 * Copyright 2025-2026 3A Systems LLC.
 */

package org.openidentityplatform.openam.test.integration;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.io.FileHandler;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.ITestResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public abstract class BaseTest {
    WebDriver driver;
    WebDriverWait wait;

    final static String AM_PASSWORD = "AMp@ssw0rd";
    final static String PA_PASSWORD = "PAp@ssw0rd";

    @BeforeClass
    public void webdriverSetup() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*","--headless", "--disable-dev-shm-usage", "--no-sandbox",
                "--verbose", "--window-size=1920,1080", "--guest");
//        options.addArguments("--remote-allow-origins=*", "--verbose", "--guest");
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    }

    @AfterClass
    public void webDriverTearDown() {
        if(driver != null) {
            driver.quit();
        }
    }

    /**
     * Remove any OpenAM configuration left over from a previous instance so the next OpenAM boots
     * unconfigured. Invoked by {@link CargoBaseTest} right before the per-test container is started.
     */
    protected void cleanConfig() throws IOException {
        String testConfigPath = System.getProperty("test.config.path");
        Path testConfig = Paths.get(testConfigPath);
        if(testConfig.toFile().exists()) {
            System.out.println("delete existing config directory");
            FileUtils.deleteDirectory(testConfig.toFile());
        }
    }

    /**
     * Dump the OpenAM install.log after every test method (not just on failure), so each run leaves
     * a record of how the per-test OpenAM instance was configured. Runs before the next test's
     * {@link CargoBaseTest#startOpenAM()} wipes the config directory via {@code cleanConfig()}.
     */
    @AfterMethod(alwaysRun = true)
    public void dumpInstallLog() {
        printInstallLogFile();
    }

    //@AfterMethod //uncomment to debug
    public void tearDown(ITestResult result) throws IOException {
        if (result.getStatus() == ITestResult.FAILURE) {
            WebElement element = driver.findElement(By.tagName("html"));
            File source = element.getScreenshotAs(OutputType.FILE);
            FileHandler.copy(source, new File("/tmp/element_screenshot.png"));
        }
    }

    protected void printInstallLogFile() {
        String testConfigPath = System.getProperty("test.config.path");
        Path installLog = Paths.get(testConfigPath, "install.log");
        if(installLog.toFile().exists()) {
            System.err.println("install.log file:");
            try (BufferedReader reader = new BufferedReader(new FileReader(installLog.toFile()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.err.println(line);
                }
            } catch (IOException e) {
                System.err.println("Error reading file: " + e.getMessage());
            }
        }

    }

    /**
     * Dump a full thread dump of the running OpenAM (Tomcat) JVM to stderr.
     *
     * <p>Used by the setup tests when the install does not complete in time. The browser-side
     * {@link org.openqa.selenium.TimeoutException} and {@code install.log} only tell us <em>that</em>
     * the install stalled, not <em>where</em>. A thread dump of the server JVM pins the exact frame
     * the install thread is blocked in (e.g. an unbounded LDAP connect while creating the base
     * entry) - the one piece of evidence that is otherwise impossible to obtain on a CI runner where
     * the hang does not reproduce locally.
     *
     * <p>The OpenAM instance runs in a forked Tomcat JVM (see {@link CargoBaseTest}); we locate it
     * with {@code jps} (main class {@code org.apache.catalina.startup.Bootstrap}) and dump it with
     * {@code jstack}, both taken from the JDK running these tests. Best-effort: any failure is logged
     * and swallowed so it never masks the original test failure.
     */
    protected void dumpOpenAmThreadDump() {
        try {
            final String binDir = System.getProperty("java.home") + File.separator + "bin" + File.separator;
            final List<String> pids = new ArrayList<>();
            final Process jps = new ProcessBuilder(binDir + "jps", "-l").redirectErrorStream(true).start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(jps.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    if (line.contains("catalina.startup.Bootstrap")) {
                        pids.add(line.trim().split("\\s+")[0]);
                    }
                }
            }
            jps.waitFor();
            if (pids.isEmpty()) {
                System.err.println("thread dump: no OpenAM (catalina) JVM found via jps");
                return;
            }
            for (String pid : pids) {
                System.err.println("=== OpenAM JVM thread dump (pid " + pid + ") ===");
                final Process jstack = new ProcessBuilder(binDir + "jstack", "-l", pid)
                        .redirectErrorStream(true).start();
                try (BufferedReader r = new BufferedReader(new InputStreamReader(jstack.getInputStream()))) {
                    String line;
                    while ((line = r.readLine()) != null) {
                        System.err.println(line);
                    }
                }
                jstack.waitFor();
                System.err.println("=== end thread dump (pid " + pid + ") ===");
            }
        } catch (Exception e) {
            System.err.println("thread dump capture failed: " + e);
        }
    }

    /**
     * Dump the top-level wizard page state when setup does not reach #confComplete. The wizard shows
     * "proceed to console" (#confComplete) only when the createConfig XHR returns "true"; on a
     * server-side error it instead reveals #returnToConfig / #setupMessage. Logging which of those is
     * present (plus the full page source) tells us whether the install actually failed - information
     * the install.log and a server thread dump cannot give, because the failure surfaces only in the
     * browser. Must be called while on the top-level document (before switching into progressIframe).
     */
    protected void dumpPageState() {
        try {
            System.err.println("=== wizard page state ===");
            System.err.println("current url: " + driver.getCurrentUrl());
            for (String id : new String[] {"confComplete", "returnToConfig", "setupMessage", "inProgress"}) {
                try {
                    java.util.List<WebElement> els = driver.findElements(By.id(id));
                    if (els.isEmpty()) {
                        System.err.println(id + ": (absent)");
                    } else {
                        WebElement el = els.get(0);
                        System.err.println(id + ": displayed=" + el.isDisplayed()
                                + " text=[" + el.getText().replace("\n", " ").trim() + "]");
                    }
                } catch (Exception ex) {
                    System.err.println(id + ": <error " + ex + ">");
                }
            }
            System.err.println("=== page source ===");
            System.err.println(driver.getPageSource());
            System.err.println("=== end page source ===");
        } catch (Exception e) {
            System.err.println("page state capture failed: " + e);
        }
    }

    WebElement waitForElement(By by) {
        return wait.until(ExpectedConditions.presenceOfElementLocated(by));
    }

    WebElement waitForElementVisible(By by) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(by));
    }

    public static ExpectedCondition<WebElement> visibilityOfAnyElement(final By locator) {
        return new ExpectedCondition<WebElement>() {

            @Override
            public WebElement apply(WebDriver webDriver) {
                List<WebElement> elements = webDriver.findElements(locator);
                return elements.stream().filter(WebElement::isDisplayed).findFirst().orElse(null);
            }

            @Override
            public String toString() {
                return "visibility of any element " + locator;
            }
        };
    }

    public boolean isTextPresent(String str) {
        Exception lastException = new Exception();
        for(int i = 0; i < 3; i++) {
            try {
                WebElement bodyElement  = new WebDriverWait(driver,Duration.ofSeconds(20)).until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
                return bodyElement.getText().toLowerCase().contains(str.toLowerCase());
            } catch (StaleElementReferenceException e) {
                lastException = e;
            }
        }
        throw new RuntimeException(lastException);
    }
}
