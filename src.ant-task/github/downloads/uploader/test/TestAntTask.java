package github.downloads.uploader.test;

import github.downloads.uploader.ant.GithubDownloadUploaderTask;

import java.io.File;

import junit.framework.TestCase;

public class TestAntTask extends TestCase {

	public void testUpload() {
		GithubDownloadUploaderTask t = new GithubDownloadUploaderTask();
		t.setDryRun(false);
		t.setOverwrite(true);
		t.setFile(new File("lib/gson-2.2.2.jar"));
		t.setDescription("Hello world!");
		t.setOwner("testowner");
		t.setRepository("testrepo");
		t.setUsername("testuser");
		t.setPassword("testpass");
		t.execute();
	}
}
