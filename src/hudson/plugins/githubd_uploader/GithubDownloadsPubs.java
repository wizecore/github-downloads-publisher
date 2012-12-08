package hudson.plugins.githubd_uploader;

import github.downloads.uploader.ant.GithubDownloadUploaderTask;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.kohsuke.stapler.DataBoundConstructor;

/**
 * {@link Publisher} that uploads files to java.net documents and files section.
 *
 * @author Kohsuke Kawaguchi
 */
public class GithubDownloadsPubs extends Recorder {

	private final String username;
	private final String password;
    private final List<Entry> entries;

    @DataBoundConstructor
    public GithubDownloadsPubs(String username, String password, List<Entry> entries) {
        this.username = username;
		this.password = password;
		this.entries = entries;
    }

    public List<Entry> getEntries() {
        return entries;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws InterruptedException {
        if(build.getResult()== Result.FAILURE) {
            // build failed. don't post
            return true;
        }

        try {
            EnvVars envVars = build.getEnvironment(listener);

            for (Entry e : entries) {
                if(e.sourceFile.trim().length()==0) {
                    listener.getLogger().println("Configuration error: no file is specified for upload");
                    build.setResult(Result.FAILURE);
                    return true;
                }

                listener.getLogger().println("Uploading "+e.sourceFile+" to " + "https://github.com/" + e.owner + "/" + e.repository + "/downloads");

                String expanded = Util.replaceMacro(e.sourceFile, envVars);
                FilePath[] src = build.getWorkspace().list(expanded);
                if (src.length == 0) {
                    throw new IOException("No such file exists: "+ expanded);
                }

                GithubDownloadUploaderTask t = new GithubDownloadUploaderTask();
        		t.setDryRun(false);
        		t.setOverwrite(true);
        		t.setDescription(e.description);
        		t.setOwner(e.owner);
        		t.setRepository(e.repository);
        		t.setUsername(username);
        		t.setPassword(password);                
        		File[] ff = new File[src.length];
        		for (int i = 0; i < ff.length; i++) {
        			FilePath s = src[i];
                	ff[i] = new File(s.getRemote());
                }
        		t.setFiles(ff);
        		t.execute();
            }
        } catch (IOException e) {
            e.printStackTrace(listener.error("Failed to upload files"));
            build.setResult(Result.FAILURE);
        }

        return true;
    }

    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {
        public String getDisplayName() {
            return "Github publisher to downloads section";
        }

        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }
    }
}
