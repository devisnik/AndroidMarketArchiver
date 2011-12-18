package de.devisnik.android.stats;

import java.text.DateFormat;
import java.util.Date;
import java.util.Properties;

public class AppData {

	private static final String SEPARATOR = "|";

	enum Property {
		NAME, VERSION, TIMESTAMP, DOWNLOADS, INSTALLS, RATED_1, RATED_2, RATED_3, RATED_4, RATED_5,
	}

	private final Properties itsProperties = new Properties();

	public AppData() {
		setProperty(Property.TIMESTAMP, System.currentTimeMillis());
	}

	void setProperty(Property property, long value) {
		setProperty(property, Long.toString(value));
	}

	void setProperty(Property property, int value) {
		setProperty(property, Integer.toString(value));
	}

	void setProperty(Property property, String value) {
		itsProperties.setProperty(property.name(), value);
	}

	String getValueAsString(Property property) {
		return itsProperties.getProperty(property.name());
	}

	int getValueAsInt(Property property) {
		return Integer.parseInt(getValueAsString(property));
	}
	
	void setRatings(int rating, String count) {
		setProperty(getRatingProperty(rating), count);
	}

	private Property getRatingProperty(int rating) {
		return Property.valueOf("RATED_" + rating);
	}

	String ratings(int stars) {
		return getValueAsString(getRatingProperty(stars));
	}

	String ratingAverage() {
		int sum = 0;
		int count = 0;
		for (int i = 1; i <= 5; i++) {
			int rated = getValueAsInt(getRatingProperty(i));
			sum += i*rated;
			count += rated;
		}
		return Float.toString((float)sum/ count).replace('.', ',');
	}

	String name() {
		return getValueAsString(Property.NAME);
	}

	String version() {
		return getValueAsString(Property.VERSION);
	}

	String timeStamp() {
		return getValueAsString(Property.TIMESTAMP);
	}

	String downloads() {
		return getValueAsString(Property.DOWNLOADS);
	}

	String installs() {
		return getValueAsString(Property.INSTALLS);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		append(builder, "timeStamp_as_date", timeStampAsDateString());
		for (Property property : Property.values())
			append(builder, property.name(), getValueAsString(property));
		return builder.toString();
	}

	String timeStampAsDateString() {
		return DateFormat.getDateTimeInstance().format(new Date(Long.parseLong(timeStamp())));
	}

	public static AppData fromString(String line) {
		String[] pairs = line.split(SEPARATOR);
		AppData appData = new AppData();
		for (String pair : pairs) {
			String[] propertyPair = pair.split("=");
			try {
				appData.setProperty(Property.valueOf(propertyPair[0]), propertyPair[1]);
			} catch (IllegalArgumentException e) {
			}
		}
		return appData;
	}

	private void append(StringBuilder builder, String key, String value) {
		if (builder.length() > 0)
			builder.append(SEPARATOR);
		builder.append(key + "=" + value);
	}

	@Override
	public int hashCode() {
		int value = 0;
		for (Property property : Property.values())
			value += getValueAsString(property).hashCode();
		return value;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj.getClass() != AppData.class)
			return false;
		AppData other = (AppData) obj;
		for (Property property : Property.values())
			if (!getValueAsString(property).equals(other.getValueAsString(property)))
				return false;
		return true;
	}
}
