package com.parsa.middleware.session;

import com.parsa.middleware.config.ConfigProperties;
import com.parsa.middleware.processing.Utility;
import com.teamcenter.schemas.soa._2006_03.exceptions.ConnectionException;
import com.teamcenter.schemas.soa._2006_03.exceptions.InternalServerException;
import com.teamcenter.schemas.soa._2006_03.exceptions.ProtocolException;
import com.teamcenter.soa.client.ExceptionHandler;
import com.teamcenter.soa.exceptions.CanceledOperationException;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;


/**
 * Implementation of the ExceptionHandler. For ConnectionExceptions (server
 * temporarily down .etc) prompts the user to retry the last request. For other
 * exceptions convert to a RunTime exception.
 */
@Component
public class AppXExceptionHandler implements ExceptionHandler {
	private int retryCount = 0;
	private Logger logger;
	//private final Settings settings = readSettings();

	private final ConfigProperties settings;

	public AppXExceptionHandler(ConfigProperties settings) {
		this.settings = settings;
	}

	public void setLogger(Logger newLogger) {
		logger = newLogger;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.teamcenter.soa.client.ExceptionHandler#handleException(com.teamcenter.
	 * schemas.soa._2006_03.exceptions.InternalServerException)
	 */
	@Override
	public void handleException(InternalServerException ise) {
		// TODO: print exception to log

		if (retryCount >= Utility.convertToInt(settings.getTcMaxRetries(), 3)) {
			retryCount = 0;
			String exceptionMessage = "";
			if (ise instanceof ConnectionException) {
				// ConnectionException are typically due to a network error (server
				// down .etc) and can be recovered from (the last request can be sent again,
				// after the problem is corrected).

				exceptionMessage = "The server returned a connection error.";
			} else if (ise instanceof ProtocolException) {
				// ProtocolException are typically due to programming errors
				// (content of HTTP
				// request is incorrect). These are generally can not be
				// recovered from.

				exceptionMessage = "The server returned a protocol error";
			} else {
				exceptionMessage = "The server returned an internal server error.";
			}

			throw new RuntimeException(
					"Maximum amount of retries exceeded. " + exceptionMessage + "\n" + ise.getMessage());
		}

		if (retryCount < Utility.convertToInt(settings.getTcMaxRetries(), 3)) {
			retryCount++;

			try {
				// If the operation is successful, return or break out of the loop
				Thread.sleep(Utility.convertToInt(settings.getTcRetryDelay(), 60) * 1000);
				logger.severe(
						String.format("Trying to reconnect for the %d. time.\n %s", retryCount, ise.getMessage()));
				return;

			} catch (final InterruptedException ex) {
				logger.severe(ex.getMessage());
				// Handle interruption exception if needed
				ex.printStackTrace();
			} catch (final Exception e) {
				logger.severe(e.getMessage());
				// Handle ServiceException, which can include network-related exceptions
				// Handle and report the exception as needed
				e.printStackTrace();

				// Wait for a certain period of time before retrying
				try {
					Thread.sleep(Utility.convertToInt(settings.getTcRetryDelay(), 60) * 1000);
				} catch (final InterruptedException ex) {
					// Handle interruption exception if needed
					ex.printStackTrace();
				}
			}
		}

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.teamcenter.soa.client.ExceptionHandler#handleException(com.teamcenter.soa
	 * .exceptions.CanceledOperationException)
	 */
	@Override
	public void handleException(CanceledOperationException coe) {
		System.out.println("");
		System.out.println("*****");
		System.out.println(
				"Exception caught in com.teamcenter.clientx.AppXExceptionHandler.handleException(CanceledOperationException).");

		// Expecting this from the login tests with bad credentials, and the
		// AnyUserCredentials class not
		// prompting for different credentials
		throw new RuntimeException(coe);
	}

}
