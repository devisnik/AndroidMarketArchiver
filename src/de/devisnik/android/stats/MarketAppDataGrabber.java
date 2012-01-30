package de.devisnik.android.stats;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.openqa.selenium.WebDriverBackedSelenium;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.remote.DesiredCapabilities;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.thoughtworks.selenium.DefaultSelenium;

import de.devisnik.android.stats.AppData.Property;

public class MarketAppDataGrabber {

	static class Params {
		@Parameter(names = "-login", description = "google login", required=true)
		private String mLogin;
		
		@Parameter(names = "-pass", description = "google password", required=true)
		private String mPassword;		
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

	public MarketAppDataGrabber(Params params) {
		mParams = params;
	}

	private static void stopClientAndServer() {
		selenium.stop();
	}

	private static void startServerAndClient() throws Exception {
		HtmlUnitDriver driver = new HtmlUnitDriver(DesiredCapabilities.firefox());
		driver.setJavascriptEnabled(true);
		selenium = new WebDriverBackedSelenium(driver, "http://market.android.com");
	}

	private void grab() throws InterruptedException, IOException {
		signIn();
		waitForElementPresent(VALID_ROWS_XPATH, 30000);
		// extra wait to ensure all rows are present
		Thread.sleep(2000);
		Number count = selenium.getXpathCount(VALID_ROWS_XPATH);
		for (int index = 1; index <= count.intValue(); index++)
			printoutRatings(VALID_ROWS_XPATH + "/../div[" + index + "]");
	}

	/**
	 * signIn.
	 * 
	 * @throws UnsupportedEncodingException
	 */
	private void signIn() throws UnsupportedEncodingException {
		selenium.open("https://www.google.com/accounts/ServiceLogin?service=androiddeveloper&passive=true&nui=1&continue=http://market.android.com/publish/Home");
		selenium.type("Email", mParams.mLogin);
		selenium.type("Passwd", mParams.mPassword);
		selenium.click("signIn");
		selenium.waitForPageToLoad("120000");
	}

	/**
	 * printoutRatings.
	 */
	private void printoutRatings(String rootTag) throws InterruptedException, IOException {
		selenium.open("/publish/Home");

		String commentsLink = rootTag + "/table/tbody/tr[2]/td/div/a";
		waitForElementPresent(commentsLink, 60);

		AppData appData = new AppData();
		readBasicAppData(rootTag, appData);

		selenium.click(commentsLink);

		String ratingsBodyTag = "//div[@id='app']/div/div/div[2]/div/div[2]/div[2]/div/div/div[3]/table/tbody";
		waitForElementPresent(ratingsBodyTag, 30);

		readRatingsData(ratingsBodyTag, appData);

		System.out.println(appData);
	}

	private void readRatingsData(String ratingsBodyTag, AppData appData) {
		for (int rating = 1; rating <= 5; rating++) {
			String ratingCountText = selenium.getText(ratingsBodyTag + "/tr[" + (6 - rating) + "]/td[3]");
			appData.setRatings(rating, ratingCountText);
		}
	}

	private void readBasicAppData(String rootTag, AppData appData) {
		String downloadsTag = rootTag + "/div[2]/div[1]/span";
		String activeInstallsTag = rootTag + "/div[2]/div[2]/span[1]";
		String nameTag = rootTag + "/div[1]/a";
		String versionTag = rootTag + "/div[1]/span[2]";
		appData.setProperty(Property.NAME, selenium.getText(nameTag));
		appData.setProperty(Property.VERSION, selenium.getText(versionTag));
		appData.setProperty(Property.DOWNLOADS, selenium.getText(downloadsTag));
		appData.setProperty(Property.INSTALLS, selenium.getText(activeInstallsTag));
	}

	private void waitForElementPresent(String elementXPath, int maxWaitInSeconds) throws InterruptedException {
		for (int second = 0;; second++) {
			if (second >= maxWaitInSeconds)
				throw new RuntimeException("timeout");
			try {
				if (selenium.isElementPresent(elementXPath))
					break;
			} catch (Exception e) {
			}
			Thread.sleep(1000);
		}
	}
}
