package com.parsa.middleware.logger;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.logging.*;

/**
 * Logs all entries and saves the logs at a specific location.
 * 
 * @author(name = "Lukas Keul", company = "Parsa PLM GmbH")
 *
 */
public class ImportLogger {
	private static FileHandler sbomiFileHandler;
	private static FileHandler bomiFileHandler;

//	public static void setup(String filepath) throws IOException {
//		createSBOMILogger(filepath);
//		createBOMILogger(filepath);
//	}

	/**
	 * 
	 */
	public static void createSBOMILogger(String filepath) {
		// get the global logger to configure it
		final Logger sbomLogger = Logger.getLogger("SBOMILogger");
		sbomLogger.setUseParentHandlers(false);

		// Create the directory if it doesn't exist
		final File directory = new File(filepath + File.separator + "sbomi");
		if (!directory.exists()) {
			directory.mkdirs();
		}
		System.out.println(directory.getPath());

		try {
			sbomiFileHandler = new FileHandler(directory + File.separator + "SBOMImporterLog_" + getTimeStampDate()
					+ "_" + getTimeStampTime() + ".log", true);
		} catch (final SecurityException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		}

		// create a TXT formatter
		sbomiFileHandler.setFormatter(new SimpleFormatter() {
			private static final String format = "[%1$tF %1$tT] [%2$-7s] %3$s %n";

			@Override
			public synchronized String format(LogRecord lr) {
				return String.format(format, new Date(lr.getMillis()), lr.getLevel().getLocalizedName(),
						lr.getSourceClassName() + "." + lr.getSourceMethodName() + "()\n\t" + lr.getMessage());
			}
		});
		sbomLogger.addHandler(sbomiFileHandler);

		sbomLogger.info("Initiating the serverside BOM importer.");
		sbomLogger.info(String.format("Saving the log files in %s.", directory.getAbsolutePath()));
	}

	/**
	 * 
	 * @param filepath
	 */
	public static Logger createBOMILogger(String filepath, long taskID, String drawingNo) {
		// get the global logger to configure it
		final Logger bomLogger = Logger.getLogger("BOMILogger_" + taskID);

		// Create the directory if it doesn't exist
		final File directory = new File(filepath + File.separator + "bomi" + File.separator + getTimeStampDate());
		if (!directory.exists()) {
			directory.mkdirs();
		}

		try {
			bomiFileHandler = new FileHandler(
					directory + File.separator + "BOMImporterLog_" +drawingNo + "_"+taskID + "_" + getTimeStampTime() + ".log", true);
		} catch (final SecurityException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		}

		// create a TXT formatter
		bomiFileHandler.setFormatter(new SimpleFormatter() {
			private static final String format = "[%1$tF %1$tT] [%2$-7s] %3$s %n";

			@Override
			public synchronized String format(LogRecord lr) {
				return String.format(format, new Date(lr.getMillis()), lr.getLevel().getLocalizedName(),
						lr.getSourceClassName() + "." + lr.getSourceMethodName() + "()\n\t" + lr.getMessage());
			}
		});
		bomLogger.addHandler(bomiFileHandler);

		return bomLogger;
	}

	/**
	 * 
	 * @return the time stamp in the format dd-MM-uuuu (d = day, M = month, u =
	 *         year)
	 */
	private static String getTimeStampDate() {
		final Instant instant = Instant.ofEpochMilli(System.currentTimeMillis());

		final ZoneId zoneID = ZoneId.of("Europe/Berlin");
		final ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(instant, zoneID);

		final String timeStamp = zonedDateTime.format(DateTimeFormatter.ofPattern("uuuu-MM-dd"));
		return timeStamp;
	}

	/**
	 * 
	 * @return the time stamp in the format hh-mm-ss (m = minute, h = hour)
	 */
	private static String getTimeStampTime() {
		final Instant instant = Instant.now();

		final ZoneId zoneID = ZoneId.of("Europe/Berlin");
		final ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(instant, zoneID);

		final String timeStamp = zonedDateTime.format(DateTimeFormatter.ofPattern("HH-mm-ss"));
		return timeStamp;
	}

	public static void closeSBOMIFileHandler() {
		sbomiFileHandler.close();
	}

	public static void closeLoggerHandlers(Logger logger) {
		for (final Handler handler : logger.getHandlers()) {
			handler.close();
		}
	}

	public static void closeBOMIFileHandler() {
		bomiFileHandler.close();
	}
}
