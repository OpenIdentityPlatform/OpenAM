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
 * Copyright 2025 3A Systems LLC.
 */

package org.openidentityplatform.openam.test.integration;

import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;

public abstract class BaseTest {
    WebDriver driver;
    WebDriverWait wait;

    final static String AM_PASSWORD = "AMp@ssw0rd";
    final static String PA_PASSWORD = "PAp@ssw0rd";

    @BeforeClass
    public void webdriverSetup() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*","--headless", "--disable-dev-shm-usage", "--no-sandbox", "--verbose");
        //options.addArguments("--remote-allow-origins=*", "--verbose");
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @AfterClass
    public void webDriverTearDown() {
        if(driver != null) {
            driver.quit();
        }
    }

    @BeforeMethod
    public void cleanup() throws IOException {
        String testConfigPath = System.getProperty("test.config.path");
        Path testConfig = Paths.get(testConfigPath);
        if(testConfig.toFile().exists()) {
            System.out.println("delete existing config directory");
            FileUtils.deleteDirectory(testConfig.toFile());
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
