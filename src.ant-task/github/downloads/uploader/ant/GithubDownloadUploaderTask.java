package github.downloads.uploader.ant;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.egit.github.core.Download;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.DownloadService;

public class GithubDownloadUploaderTask extends Task {
	
	GitHubProjectMojo mojo = new GitHubProjectMojo();

	public boolean isDebug() {
		return mojo.isDebug();
	}

	public boolean isInfo() {
		return mojo.isInfo();
	}

	public void debug(String message) {
		mojo.debug(message);
	}

	public void debug(String message, Throwable throwable) {
		mojo.debug(message, throwable);
	}

	public void info(String message) {
		mojo.info(message);
	}

	public void info(String message, Throwable throwable) {
		mojo.info(message, throwable);
	}

	public GitHubClient createClient(String host, String userName,
			String password, String oauth2Token, String serverId) {
		return mojo.createClient(host, userName, password, oauth2Token,
				serverId);
	}

	public GitHubClient createClient(String hostname) {
		return mojo.createClient(hostname);
	}

	public GitHubClient createClient() {
		return mojo.createClient();
	}

	public boolean configureUsernamePassword(GitHubClient client,
			String userName, String password) {
		return mojo.configureUsernamePassword(client, userName, password);
	}

	public boolean configureOAuth2Token(GitHubClient client, String oauth2Token) {
		return mojo.configureOAuth2Token(client, oauth2Token);
	}

	public boolean configureServerCredentials(GitHubClient client,
			String serverId) {
		return mojo.configureServerCredentials(client, serverId);
	}


	/**
	 * Owner of repository to upload to
	 *
	 * @parameter expression="${github.downloads.repositoryOwner}"
	 */
	private String owner;

	/**
	 * Name of repository to upload to
	 *
	 * @parameter expression="${github.downloads.repositoryName}"
	 */
	private String repository;

	/**
	 * User name for authentication
	 *
	 * @parameter expression="${github.downloads.userName}"
	 *            default-value="${github.global.userName}"
	 */
	private String username;

	/**
	 * User name for authentication
	 *
	 * @parameter expression="${github.downloads.password}"
	 *            default-value="${github.global.password}"
	 */
	private String password;

	/**
	 * Description of download
	 *
	 * @parameter
	 */
	private String description;

	/**
	 * User name for authentication
	 *
	 * @parameter expression="${github.downloads.oauth2Token}"
	 *            default-value="${github.global.oauth2Token}"
	 */
	private String oauth2Token;

	/**
	 * Override existing downloads
	 *
	 * @parameter expression="${github.downloads.override}"
	 */
	private boolean overwrite;

	/**
	 * Show what downloads would be deleted and uploaded but don't actually
	 * alter the current set of repository downloads. Showing what downloads
	 * will be deleted does require still listing the current downloads
	 * available from the repository.
	 *
	 * @parameter expression="${github.downloads.dryRun}"
	 */
	private boolean dryRun;

	/**
	 * Host for API calls
	 *
	 * @parameter expression="${github.downloads.host}"
	 *            default-value="${github.global.host}"
	 */
	private String host = "api.github.com";

	/**
	 * Suffix to append to all uploaded files. The configured suffix will go
	 * before the file extension.
	 *
	 * @parameter expression="${github.downloads.suffix}"
	 */
	private String suffix;

	/**
	 * Id of server to use
	 *
	 * @parameter expression="${github.downloads.server}"
	 *            default-value="${github.global.server}"
	 */
	private String server;

	private File file;
	
	private File[] files;
	
	public void setFiles(File[] files) {
		this.files = files;
	}

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}

	private List<FileSet> filesets = new ArrayList<FileSet>();
	
	public void addFileSet(FileSet fs) {
		filesets.add(fs);
	}

	/**
	 * Get files to create downloads from
	 *
	 * @return non-null but possibly empty list of files
	 */
	protected Collection<File> getFiles() {
		List<File> files = new ArrayList<File>();
		
		if (filesets.size() == 0 && file != null) {
			files.add(file);
			return files;
		}
		
		if (filesets.size() == 0 && this.files != null) {
			for (File f: this.files) files.add(f);
			return files;
		}
		
		for (FileSet fs: filesets) {
			for (Iterator<?> it = fs.iterator(); it.hasNext();) {
				files.add((File) it.next());
			}
		}
		
		return files;
	}

	/**
	 * Get file from artifact
	 *
	 * @param artifact
	 * @return existent artifact file or null
	 */
	protected File getArtifactFile(Artifact artifact) {
		if (artifact == null)
			return null;
		File file = artifact.getFile();
		return file != null && file.isFile() && file.exists() ? file : null;
	}

	/**
	 * Get map of existing downloads with names mapped to download identifiers.
	 *
	 * @param service
	 * @param repository
	 * @return map of existing downloads
	 * @
	 */
	protected Map<String, Integer> getExistingDownloads(
			DownloadService service, RepositoryId repository)
			 {
		try {
			Map<String, Integer> existing = new HashMap<String, Integer>();
			for (Download download : service.getDownloads(repository))
				if (!StringUtils.isEmpty(download.getName()))
					existing.put(download.getName(), download.getId());
			if (isDebug()) {
				final int size = existing.size();
				if (size != 1)
					debug(MessageFormat.format("Listed {0} existing downloads",
							size));
				else
					debug("Listed 1 existing download");
			}
			return existing;
		} catch (IOException e) {
			throw new IllegalStateException("Listing downloads failed: "
					+ getExceptionMessage(e), e);
		}
	}

	private String getExceptionMessage(IOException e) {
		return e != null && e.getMessage() != null ? e.getMessage() : (e != null ? e.toString() : null);
	}

	/**
	 * Deleting existing download with given id and name
	 *
	 * @param repository
	 * @param name
	 * @param id
	 * @param service
	 * @
	 */
	protected void deleteDownload(RepositoryId repository, String name, int id,
			DownloadService service)  {
		try {
			info(MessageFormat.format(
					"Deleting existing download: {0} (id={1})", name,
					Integer.toString(id)));
			if (!dryRun)
				service.deleteDownload(repository, id);
		} catch (IOException e) {
			String prefix = MessageFormat.format(
					"Deleting existing download {0} failed: ", name);
			throw new IllegalStateException(prefix + getExceptionMessage(e), e);
		}
	}

	public void execute() throws BuildException  {
		RepositoryId repository = getRepository(owner, this.repository);

		DownloadService service = new DownloadService(createClient(host, username, password, oauth2Token, server));

		Map<String, Integer> existing;
		if (overwrite) {
			existing = getExistingDownloads(service, repository);
			log("Got existing downloads: " + existing);
		} else {
			existing = Collections.emptyMap();
		}

		Collection<File> files = getFiles();

		if (dryRun)
			info("Dry run mode, downloads will not be deleted or uploaded");

		int fileCount = files.size();
		if (fileCount != 1)
			info(MessageFormat.format("Adding {0} downloads to repository {1}",
					fileCount, repository.generateId()));
		else
			info(MessageFormat.format("Adding 1 download to repository {0}",
					repository.generateId()));

		for (File file : files) {
			String name = file.getName();
			if (!StringUtils.isEmpty(suffix)) {
				final int lastDot = name.lastIndexOf('.');
				if (lastDot != -1)
					name = name.substring(0, lastDot) + suffix
							+ name.substring(lastDot);
				else
					name += suffix;
			}

			final long size = file.length();
			Integer existingId = existing.remove(name);
			if (existingId != null)
				deleteDownload(repository, name, existingId, service);

			Download download = new Download().setName(name).setSize(size);
			if (!StringUtils.isEmpty(description))
				download.setDescription(description);

			if (size != 1)
				info(MessageFormat.format("Adding download: {0} ({1} bytes)",
						name, size));
			else
				info(MessageFormat
						.format("Adding download: {0} (1 byte)", name));

			if (!dryRun)
				try {
					service.createDownload(repository, download, file);
				} catch (IOException e) {
					String prefix = MessageFormat.format(
							"Resource {0} upload failed: ", name);
					throw new IllegalStateException(prefix
							+ getExceptionMessage(e), e);
				}
		}
	}
	
	
	public void testListDownloads() {

	}

	public RepositoryId getRepository(String owner, String name) {
		return mojo.getRepository(owner, name);
	}

	public String getOwner() {
		return owner;
	}

	public void setOwner(String repositoryOwner) {
		this.owner = repositoryOwner;
	}

	public String getRepository() {
		return repository;
	}

	public void setRepository(String repositoryName) {
		this.repository = repositoryName;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String userName) {
		this.username = userName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getOAuth2Token() {
		return oauth2Token;
	}

	public void setOAuth2Token(String oauth2Token) {
		this.oauth2Token = oauth2Token;
	}

	public boolean isOverwrite() {
		return overwrite;
	}

	public void setOverwrite(boolean override) {
		this.overwrite = override;
	}

	public boolean isDryRun() {
		return dryRun;
	}

	public void setDryRun(boolean dryRun) {
		this.dryRun = dryRun;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getSuffix() {
		return suffix;
	}

	public void setSuffix(String suffix) {
		this.suffix = suffix;
	}

	public String getServer() {
		return server;
	}

	public void setServer(String server) {
		this.server = server;
	}
}
