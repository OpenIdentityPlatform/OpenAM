package org.openidentityplatform.openam.test.integration;

import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.List;

import static org.testng.Assert.assertTrue;

public class IT_Setup {

    WebDriver driver;
    WebDriverWait wait;

    final static String OPENAM_URL = "http://openam.local:8207/openam";
    final static String AM_PASSWORD = "AMp@ssw0rd";
    final static String PA_PASSWORD = "PAp@ssw0rd";

    @BeforeTest
    public void webdriverSetup() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*","--headless", "--disable-dev-shm-usage", "--no-sandbox", "--verbose");
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @Test
    public void testSetup() {
        driver.get(OPENAM_URL);

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
        WebElement proceedToConsole = waitComplete.until(visibilityOfAnyElement(By.cssSelector("#confComplete a")));
        proceedToConsole.click();

        waitForElement(By.id("IDToken1")).sendKeys("amadmin");
        waitForElement(By.id("IDToken2")).sendKeys(AM_PASSWORD);
        waitForElement(By.name("Login.Submit")).click();

        waitForElementVisible(By.className("Tab1Div"));
        waitForElement(By.name("Home.mhCommon.LogOutHREF")).click();
        Alert alert = driver.switchTo().alert();
        alert.accept(); // for OK
        assertTrue(isTextPresent("You are logged out."));

        //check XUI
        driver.get(OPENAM_URL.concat("/XUI"));
        waitForElement(By.id("idToken1")).sendKeys("amadmin");
        waitForElement(By.id("idToken2")).sendKeys(AM_PASSWORD);
        waitForElement(By.id("loginButton_0")).click();

        //check console
        waitForElementVisible(By.id("mainNavBar"));
    }

    @AfterTest
    public void webDriverTearDown() {
        if(driver != null) {
            driver.quit();
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
