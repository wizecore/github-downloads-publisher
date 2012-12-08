/*
 * Copyright (c) 2011 GitHub Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
package github.downloads.uploader.ant;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;

/**
 * Base GitHub Mojo class to be extended.
 *
 * @author Kevin Sawicki (kevin@github.com)
 */
public class GitHubProjectMojo {
	
	private Logger log = Logger.getLogger(getClass().getName());

	/**
	 * Get formatted exception message for {@link IOException}
	 *
	 * @param e
	 * @return message
	 */
	public static String getExceptionMessage(IOException e) {
		return e.getMessage();
	}

	/**
	 * Is debug logging enabled?
	 *
	 * @return true if enabled, false otherwise
	 */
	public boolean isDebug() {
		return log.isLoggable(Level.FINE);
	}

	/**
	 * Is info logging enabled?
	 *
	 * @return true if enabled, false otherwise
	 */
	public boolean isInfo() {
		return log.isLoggable(Level.INFO);
	}

	/**
	 * Log given message at debug level
	 *
	 * @param message
	 */
	public void debug(String message) {
		log.fine(message);
	}

	/**
	 * Log given message and throwable at debug level
	 *
	 * @param message
	 * @param throwable
	 */
	public void debug(String message, Throwable throwable) {
		log.log(Level.FINE, message, throwable);
	}

	/**
	 * Log given message at info level
	 *
	 * @param message
	 */
	public void info(String message) {
		log.info(message);
	}

	/**
	 * Log given message and throwable at info level
	 *
	 * @param message
	 * @param throwable
	 */
	public void info(String message, Throwable throwable) {
		log.log(Level.INFO, message, throwable);
	}

	/**
	 * Create client
	 *
	 * @param host
	 * @param userName
	 * @param password
	 * @param oauth2Token
	 * @param serverId
	 * @param settings
	 * @param session
	 * @return client
	 * @throws MojoExecutionException
	 */
	public GitHubClient createClient(String host, String userName,
			String password, String oauth2Token, String serverId) {
		GitHubClient client;
		if (!StringUtils.isEmpty(host)) {
			if (isDebug())
				debug("Using custom host: " + host);
			client = createClient(host);
		} else
			client = createClient();

		if (configureUsernamePassword(client, userName, password)
				|| configureOAuth2Token(client, oauth2Token)
				|| configureServerCredentials(client, serverId))
			return client;
		else
			throw new IllegalStateException(
					"No authentication credentials configured");
	}

	/**
	 * Create client
	 * <p>
	 * Subclasses can override to do any custom client configuration
	 *
	 * @param hostname
	 * @return non-null client
	 * @throws MojoExecutionException
	 */
	public GitHubClient createClient(String hostname)
			{
		if (!hostname.contains("://"))
			return new GitHubClient(hostname);
		try {
			URL hostUrl = new URL(hostname);
			return new GitHubClient(hostUrl.getHost(), hostUrl.getPort(),
					hostUrl.getProtocol());
		} catch (MalformedURLException e) {
			throw new IllegalArgumentException("Could not parse host URL "
					+ hostname, e);
		}
	}

	/**
	 * Create client
	 * <p>
	 * Subclasses can override to do any custom client configuration
	 *
	 * @return non-null client
	 */
	public GitHubClient createClient() {
		return new GitHubClient();
	}

	/**
	 * Configure credentials from configured username/password combination
	 *
	 * @param client
	 * @param userName
	 * @param password
	 * @return true if configured, false otherwise
	 */
	public boolean configureUsernamePassword(final GitHubClient client,
			final String userName, final String password) {
		if (StringUtils.isEmpty(userName, password))
			return false;

		if (isDebug())
			debug("Using basic authentication with username: " + userName);
		client.setCredentials(userName, password);
		return true;
	}

	/**
	 * Configure credentials from configured OAuth2 token
	 *
	 * @param client
	 * @param oauth2Token
	 * @return true if configured, false otherwise
	 */
	public boolean configureOAuth2Token(final GitHubClient client,
			final String oauth2Token) {
		if (StringUtils.isEmpty(oauth2Token))
			return false;

		if (isDebug())
			debug("Using OAuth2 access token authentication");
		client.setOAuth2Token(oauth2Token);
		return true;
	}

	/**
	 * Configure client with credentials from given server id
	 *
	 * @param client
	 * @param serverId
	 * @param settings
	 * @param session
	 * @return true if configured, false otherwise
	 * @throws MojoExecutionException
	 */
	public boolean configureServerCredentials(final GitHubClient client, final String serverId) {
		if (StringUtils.isEmpty(serverId))
			return false;

		String serverUsername = null;
		String serverPassword = null;

		if (!StringUtils.isEmpty(serverUsername, serverPassword)) {
			if (isDebug())
				debug("Using basic authentication with username: "
						+ serverUsername);
			client.setCredentials(serverUsername, serverPassword);
			return true;
		}

		// A server password without a username is assumed to be an OAuth2 token
		if (!StringUtils.isEmpty(serverPassword)) {
			if (isDebug())
				debug("Using OAuth2 access token authentication");
			client.setOAuth2Token(serverPassword);
			return true;
		}

		if (isDebug())
			debug(MessageFormat.format(
					"Server ''{0}'' is missing username/password credentials",
					serverId));
		return false;
	}

	/**
	 * Get repository and throw a {@link MojoExecutionException} on failures
	 *
	 * @param project
	 * @param owner
	 * @param name
	 * @return non-null repository id
	 * @throws MojoExecutionException
	 */
	public RepositoryId getRepository(final String owner, final String name) {
		RepositoryId repository = RepositoryUtils.getRepository(owner, name);
		if (repository == null)
			throw new IllegalArgumentException(
					"No GitHub repository (owner and name) configured");
		if (isDebug())
			debug(MessageFormat.format("Using GitHub repository {0}",
					repository.generateId()));
		return repository;
	}
}
