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

import static org.eclipse.egit.github.core.client.IGitHubConstants.HOST_DEFAULT;
import static org.eclipse.egit.github.core.client.IGitHubConstants.SUFFIX_GIT;

import org.eclipse.egit.github.core.RepositoryId;

/**
 * Repository utilities
 *
 * @author Kevin Sawicki (kevin@github.com)
 */
public class RepositoryUtils {

	/**
	 * Extra repository id from given SCM URL
	 *
	 * @param url
	 * @return repository id or null if extraction fails
	 */
	public static RepositoryId extractRepositoryFromScmUrl(String url) {
		if (StringUtils.isEmpty(url))
			return null;
		int ghIndex = url.indexOf(HOST_DEFAULT);
		if (ghIndex == -1 || ghIndex + 1 >= url.length())
			return null;
		if (!url.endsWith(SUFFIX_GIT))
			return null;
		url = url.substring(ghIndex + HOST_DEFAULT.length() + 1, url.length()
				- SUFFIX_GIT.length());
		return RepositoryId.createFromId(url);
	}

	/**
	 * Get repository
	 * 
	 * @param owner
	 * @param name
	 *
	 * @return repository id or null if none configured
	 */
	public static RepositoryId getRepository(final String owner, final String name) {
		if (!StringUtils.isEmpty(owner, name))
			return RepositoryId.create(owner, name);
		else
			throw new IllegalArgumentException("owner and name must be set!");
	}
}
