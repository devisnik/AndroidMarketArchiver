package de.devisnik.android.stats;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverBackedSelenium;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.thoughtworks.selenium.DefaultSelenium;

import de.devisnik.android.stats.AppData.Property;

public class MarketAppDataGrabber {

	static class Params {
		@Parameter(names = "-login", description = "google login", required = true)
		private String login;

		@Parameter(names = "-pass", description = "google password", required = true)
		private String password;

		@Parameter(names = "-verbose", description = "show current action", required = false)
		private boolean verbose;

		@Parameter(names = "-apps", description = "number of apps", required = false)
		private int apps = 1;
		
		@Parameter(names = "-maxRetry", description = "max retries if load fails", required = false)	
		private int maxRetry = 3;
		
		@Parameter(names = "-maxWait", description = "max load waiting in seconds", required = false)		
		private int maxWait = 30;
	}

	private static final String VALID_ROWS_XPATH = "//div[@class='listingTable']/child::div[@style='']/div[@class='listingRow']";
	private static DefaultSelenium selenium;

	public static void main(String[] args) throws Exception {
		LogManager.getLogManager().reset();
		Logger globalLogger = Logger.getLogger(java.util.logging.Logger.GLOBAL_LOGGER_NAME);
		globalLogger.setLevel(java.util.logging.Level.OFF);
		startServerAndClient();
		try {
			Params params = new Params();
			JCommander commander = new JCommander(params);
			try {
				commander.parse(args);
				new MarketAppDataGrabber(params).grab();

			} catch (ParameterException e) {
				e.printStackTrace();
				commander.usage();
			}
		} finally {
			stopClientAndServer();
		}
	}

	private Params mParams;
	private static WebDriver mDriver;

	public MarketAppDataGrabber(Params params) {
		mParams = params;
	}

	private static void stopClientAndServer() {
		selenium.stop();
	}

	private static void startServerAndClient() throws Exception {
		// mDriver = new FirefoxDriver();
		mDriver = new HtmlUnitDriver(DesiredCapabilities.firefox());
		((HtmlUnitDriver) mDriver).setJavascriptEnabled(true);
		selenium = new WebDriverBackedSelenium(mDriver, "http://play.google.com");
	}

	private void grab() throws InterruptedException, IOException {
		signIn();
		waitForAppTableLoaded();
		Number count = selenium.getXpathCount(VALID_ROWS_XPATH);
		log("found " + count + " App reports to grab.");
		int retryCount = 0;
		for (int index = 1; index <= count.intValue(); index++)
			try {
				printoutRatings(VALID_ROWS_XPATH + "/../div[" + index + "]");
				retryCount = 0;
			} catch (Exception e) {
				log("Error while reading stats: " + e.getMessage());
				if (retryCount >= mParams.maxRetry)
					continue;
				retryCount++;
				index--; // try again
			}
	}

	private void waitForAppTableLoaded() throws InterruptedException {
		log("start waiting for apps table loaded");
		waitForElementPresent(VALID_ROWS_XPATH);
		// extra wait to ensure all rows are present
		try {
			waitForElementPresent(VALID_ROWS_XPATH + "/../div[" + mParams.apps + "]");
		} catch (TimeoutException e) {
		}
		log("app table fully loaded");
	}

	/**
	 * signIn.
	 * 
	 * @throws UnsupportedEncodingException
	 */
	private void signIn() throws UnsupportedEncodingException {
		log("signing in!");
		selenium.open("https://www.google.com/accounts/ServiceLogin?service=androiddeveloper&passive=true&nui=1&continue=http://play.google.com/apps/publish/Home");
		selenium.type("Email", mParams.login);
		selenium.type("Passwd", mParams.password);
		selenium.click("signIn");
		selenium.waitForPageToLoad("120000");
		log("done signing in!");
	}

	/**
	 * printoutRatings.
	 */
	private void printoutRatings(String rootTag) throws InterruptedException, IOException {
		log("opening /apps/publish/Home");
		selenium.open("/apps/publish/Home");

		String commentsLink = rootTag + "/table/tbody/tr[2]/td/div/a";
		waitForElementPresent(commentsLink);

		AppData appData = new AppData();
		readBasicAppData(rootTag, appData);

		selenium.click(commentsLink);

		String ratingsBodyTag = "//div[@id='app']/div/div/div[2]/div/div[2]/div[2]/div/div/div[3]/table/tbody";
		waitForElementPresent(ratingsBodyTag);

		readRatingsData(ratingsBodyTag, appData);

		System.out.println(appData);
	}

	private void readRatingsData(String ratingsBodyTag, AppData appData) {
		log("start reading ratings data at " + ratingsBodyTag);
		for (int rating = 1; rating <= 5; rating++) {
			String ratingCountText = selenium.getText(ratingsBodyTag + "/tr[" + (6 - rating) + "]/td[3]");
			appData.setRatings(rating, ratingCountText);
		}
		log("done reading ratings data at " + ratingsBodyTag);
	}

	private void readBasicAppData(String rootTag, AppData appData) {
		log("start reading basic data at " + rootTag);
		String downloadsTag = rootTag + "/div[2]/div[1]/span";
		String activeInstallsTag = rootTag + "/div[2]/div[2]/span[1]";
		String nameTag = rootTag + "/div[1]/a";
		String versionTag = rootTag + "/div[1]/span[2]";
		appData.setProperty(Property.NAME, selenium.getText(nameTag));
		appData.setProperty(Property.VERSION, selenium.getText(versionTag));
		appData.setProperty(Property.DOWNLOADS, readUntilFirstSpace(selenium.getText(downloadsTag)));
		appData.setProperty(Property.INSTALLS, readUntilFirstSpace(selenium.getText(activeInstallsTag)));
		log("done reading basic data at " + rootTag);
	}

	private String readUntilFirstSpace(String input) {
		return input.split(" ")[0];
	}

	private void waitForElementPresent(String elementXPath) throws InterruptedException {
		WebDriverWait wait = new WebDriverWait(mDriver, mParams.maxWait);
		wait.until(visibilityOfElementLocated(By.xpath(elementXPath)));
	}

	private ExpectedCondition<WebElement> visibilityOfElementLocated(final By by) {
		return new ExpectedCondition<WebElement>() {
			public WebElement apply(WebDriver driver) {
				WebElement element = driver.findElement(by);
				return element.isDisplayed() ? element : null;
			}
		};
	}

	private void log(String message) {
		if (mParams.verbose)
			System.out.println(message);
	}
}
