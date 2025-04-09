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
 * Copyright 2022-2025 3A Systems LLC.
 */

package org.openidentityplatform.openam.test.integration;

import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.Test;

import java.time.Duration;

import static org.testng.Assert.assertTrue;

public class IT_Setup extends BaseTest {

    @Test
    public void testSetup() {



        final String openamUrl = "http://openam.local:8207/openam";

        driver.get(openamUrl);

        //wait for setup page is loaded
        waitForElement(By.id("pushConfigDialog_c"));
        WebElement createDefaultLink = driver.findElement(By.id("DemoConfiguration"));
        createDefaultLink.click();

        //wait for licence check
        waitForElementVisible(By.id("defaultSummary_c"));
        WebElement acceptCheck = driver.findElement(By.id("accept-check"));
        Actions actions = new Actions(driver);
        actions.moveToElement(acceptCheck).perform();
        acceptCheck.click();

        driver.findElement(By.id("acceptLicenseButton")).click();

        waitForElement(By.id("defaultAdminPassword")).sendKeys(AM_PASSWORD);
        waitForElement(By.id("defaultAdminConfirm")).sendKeys(AM_PASSWORD);

        waitForElement(By.id("defaultAgentPassword")).sendKeys(PA_PASSWORD);
        waitForElement(By.id("defaultAgentConfirm")).sendKeys(PA_PASSWORD);

        wait.until(ExpectedConditions.elementToBeClickable(By.id("createDefaultConfig"))).click();

        //wait for setup complete
        WebDriverWait waitComplete = new WebDriverWait(driver, Duration.ofSeconds(300));
        try {
            WebElement proceedToConsole = waitComplete.until(visibilityOfAnyElement(By.cssSelector("#confComplete a")));
            proceedToConsole.click();
        } catch (TimeoutException e) {
            System.err.println("error occurred during install: " + e);
            WebElement progressIframe = waitForElement(By.id("progressIframe"));
            driver.switchTo().frame(progressIframe);
            System.err.println("output messages: " + waitForElement(By.id("progressP")).getText());
            printInstallLogFile();
            throw e;
        } finally {
            driver.switchTo().defaultContent();
        }

        waitForElement(By.id("IDToken1")).sendKeys("amadmin");
        waitForElement(By.id("IDToken2")).sendKeys(AM_PASSWORD);
        waitForElement(By.name("Login.Submit")).click();

        waitForElementVisible(By.className("Tab1Div"));
        waitForElement(By.name("Home.mhCommon.LogOutHREF")).click();
        Alert alert = driver.switchTo().alert();
        alert.accept(); // for OK
        assertTrue(isTextPresent("You are logged out."));

        //check XUI
        driver.get(openamUrl.concat("/XUI"));
        waitForElement(By.id("idToken1")).sendKeys("amadmin");
        waitForElement(By.id("idToken2")).sendKeys(AM_PASSWORD);
        waitForElement(By.id("loginButton_0")).click();

        //check console
        waitForElementVisible(By.id("mainNavBar"));
    }


}
