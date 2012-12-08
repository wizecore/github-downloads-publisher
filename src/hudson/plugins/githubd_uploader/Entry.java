package hudson.plugins.githubd_uploader;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.model.Descriptor;
import hudson.model.Describable;
import hudson.model.Hudson;
import hudson.Extension;

/**
 * Instruction of how to upload one file.
 *
 * @author Kohsuke Kawaguchi
 * @author huksley
 */
public final class Entry implements Describable<Entry> {
    /**
     * Destination owner.
     */
    public final String owner;
    
    /**
     * Destination repository.
     */
    public final String repository;
    
    /**
     * Download description.
     */
    public final String description;
    
    /**
     * File name relative to the workspace root to upload.
     * <p>
     * May contain macro, wildcard.
     */
    public final String sourceFile;

    @DataBoundConstructor
    public Entry(String owner, String repository, String description, String sourceFile) {
        this.owner = owner;
        this.repository = repository;
        this.description = description;
        this.sourceFile = sourceFile;
    }


    // use Descriptor just to make form binding work
    public Descriptor<Entry> getDescriptor() {
        return Hudson.getInstance().getDescriptorByType(EntryDescriptor.class);
    }

    @Extension
    public static final class EntryDescriptor extends Descriptor<Entry> {
        public String getDisplayName() {
            return "";
        }
    }
}
