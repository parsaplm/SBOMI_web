package com.parsa.middleware.session;

import com.parsa.middleware.config.ConfigProperties;
import com.teamcenter.schemas.soa._2006_03.exceptions.InvalidCredentialsException;
import com.teamcenter.schemas.soa._2006_03.exceptions.ServiceException;
import com.teamcenter.services.strong.core.DataManagementService;
import com.teamcenter.services.strong.core.SessionService;
import com.teamcenter.services.strong.core._2006_03.Session.LoginResponse;
import com.teamcenter.soa.SoaConstants;
import com.teamcenter.soa.client.Connection;
import com.teamcenter.soa.client.model.ModelObject;
import com.teamcenter.soa.client.model.strong.User;
import com.teamcenter.soa.client.model.strong.WorkspaceObject;
import com.teamcenter.soa.exceptions.CanceledOperationException;
import com.teamcenter.soa.exceptions.NotLoadedException;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;
@Component
public class AppXSession {
	private final Logger logger;

	private final ConfigProperties settings;

	public User getUser() {

		return user;
	}

	private User user;

	/**
	 * Single instance of the Connection object that is shared throughout the
	 * application. This Connection object is needed whenever a Service stub is
	 * instantiated.
	 */
	private final Connection connection;

	/**
	 * The credentialManager is used both by the Session class and the Teamcenter
	 * Services Framework to get user credentials.
	 *
	 */
	private final AppXCredentialManager credentialManager;

	/**
	 * Create an instance of the Session with a connection to the specified server.
	 * <p>
	 * Add implementations of the ExceptionHandler, PartialErrorListener,
	 * ChangeListener, and DeleteListeners.
	 *
	 * @param host     Address of the host to connect to, http://serverName:port/tc
	 * @param settings
	 */
	public AppXSession(ConfigProperties settings) {
		this.settings = settings;
		String host = settings.getUrl();
		// Create an instance of the CredentialManager, this is used
		// by the SOA Framework to get the user's credentials when
		// challenged by the server (session timeout on the web tier).
		credentialManager = new AppXCredentialManager();

		this.logger = Logger.getLogger(AppXSession.class.getName());;
		String protocol = null;
		String envNameTccs = null;
		if (host.startsWith("http")) {
			protocol = SoaConstants.HTTP;
		} else if (host.startsWith("tccs")) {
			protocol = SoaConstants.TCCS;
			host = host.trim();
			final int envNameStart = host.indexOf('/') + 2;
			envNameTccs = host.substring(envNameStart, host.length());
			host = "";
		} else {
			protocol = SoaConstants.HTTP;
		}

		// Create the Connection object, no contact is made with the server
		// until a service request is made
		connection = new Connection(host, credentialManager, SoaConstants.REST, protocol);

		if (protocol == SoaConstants.TCCS) {
			connection.setOption(Connection.TCCS_ENV_NAME, envNameTccs);
		}

		// Add an ExceptionHandler to the Connection, this will handle any
		// InternalServerException, communication errors, XML marshaling errors
		// .etc
		final AppXExceptionHandler exceptionHandler = new AppXExceptionHandler(settings);
		exceptionHandler.setLogger(logger);
		connection.setExceptionHandler(exceptionHandler);

		// While the above ExceptionHandler is required, all of the following
		// Listeners are optional. Client application can add as many or as few
		// Listeners
		// of each type that they want.

		// Add a Partial Error Listener, this will be notified when ever a
		// a service returns partial errors.
		connection.getModelManager().addPartialErrorListener(new AppXPartialErrorListener());

		// Add a Change and Delete Listener, this will be notified when ever a
		// a service returns model objects that have been updated or deleted.
		connection.getModelManager().addModelEventListener(new AppXModelEventListener());

		// Add a Request Listener, this will be notified before and after each
		// service request is sent to the server.
		Connection.addRequestListener(new AppXRequestListener());

	}

	/**
	 * Get the single Connection object for the application
	 *
	 * @return connection
	 */
	public Connection getConnection() {
		return connection;
	}

	/**
	 * Login to the Teamcenter Server
	 *
	 */
	public User login(String name, String password) throws InvalidCredentialsException {
		// Get the service stub
		final SessionService sessionService = SessionService.getService(connection);

		try {
			// TODO: Change to no prompt

			// Prompt for credentials until they are right, or until user
			// cancels
			final String[] credentials = credentialManager.promptForCredentials(name, password);
			while (true) {

				// *****************************
				// Execute the service operation
				// *****************************
				final String sessionDisc = generateSessionDiscriminator();
				System.out.println("SessionDiscriminator:" + sessionDisc);
				final LoginResponse out = sessionService.login(credentials[0], credentials[1], "", "", "", sessionDisc);
				user = out.user;
				return user;

			}
		}
		// User canceled the operation, don't need to tell him again
		catch (final CanceledOperationException e) {
			e.printStackTrace();
		} catch (final RuntimeException e) {
			e.printStackTrace();
		}

		// Exit the application
//		System.exit(0);
		return null;
	}

	/**
	 * Terminate the session with the Teamcenter Server
	 *
	 */
	public void logout() {
		// Get the service stub
		final SessionService sessionService = SessionService.getService(connection);
		try {
			// *****************************
			// Execute the service operation
			// *****************************
			sessionService.logout();
			user = null;
		} catch (final ServiceException e) {
		}
	}

	/**
	 * Print some basic information for a list of objects
	 *
	 * @param objects
	 */
	public void printObjects(ModelObject[] objects) {
		if (objects == null) {
			return;
		}

		final SimpleDateFormat format = new SimpleDateFormat("M/d/yyyy h:mm a", new Locale("en", "US")); // Simple no
																											// time zone

		// Ensure that the referenced User objects that we will use below are loaded
		getUsers(objects);

		System.out.println("Name\t\tOwner\t\tLast Modified\tItem type");
		System.out.println("====\t\t=====\t\t=============\t=============");
		for (int i = 0; i < objects.length; i++) {
			if (!(objects[i] instanceof WorkspaceObject)) {
				continue;
			}

			final WorkspaceObject wo = (WorkspaceObject) objects[i];
			try {
				final String name = wo.get_object_string();

				final User owner = (User) wo.get_owning_user();
				final String ItemType = wo.get_object_type().toString();
				final Calendar lastModified = wo.get_last_mod_date();
				System.out.println(name + "\t" + owner.get_user_name() + "\t" + format.format(lastModified.getTime())
						+ "\t" + ItemType);
			} catch (final NotLoadedException e) {
				// Print out a message, and skip to the next item in the folder
				// Could do a DataManagementService.getProperties call at this point
				System.out.println(e.getMessage());
				System.out.println(
						"The Object Property Policy ($TC_DATA/soa/policies/Default.xml) is not configured with this property.");
			}
		}

	}

	private void getUsers(ModelObject[] objects) {
		if (objects == null) {
			return;
		}

		final DataManagementService dmService = DataManagementService.getService(getConnection());

		final List<User> unKnownUsers = new Vector<>();
		for (int i = 0; i < objects.length; i++) {
			if (!(objects[i] instanceof WorkspaceObject)) {
				continue;
			}

			final WorkspaceObject wo = (WorkspaceObject) objects[i];

			User owner = null;
			try {
				owner = (User) wo.get_owning_user();
				owner.get_user_name();
			} catch (final NotLoadedException e) {
				if (owner != null) {
					unKnownUsers.add(owner);
				}
			}
		}
		final User[] users = unKnownUsers.toArray(new User[unKnownUsers.size()]);
		final String[] attributes = { "user_name" };

		// *****************************
		// Execute the service operation
		// *****************************
		dmService.getProperties(users, attributes);

	}

	public static String generateSessionDiscriminator() {
		// Generate a unique UUID (Universally Unique Identifier)
		final UUID uuid = UUID.randomUUID();

		// Convert the UUID to a string and remove dashes
		final String sessionDiscriminator = uuid.toString().replace("-", "");

		return sessionDiscriminator;
	}

}
