package com.idi.assessor.datacar.scanner;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.file.FileLocker;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.util.Assert;

import com.google.common.collect.Lists;
import com.idi.assessor.datacar.common.FileDataWrapper;
import com.idi.assessor.datacar.common.IfsLifeCycleAware;
import com.idi.assessor.datacar.common.IfsLifeCycleAwareJob;
import com.idi.assessor.datacar.util.SambaFileUtil;

public abstract class AbstractRecursiveDirectoryScanner<K, V, Scanner> extends IfsLifeCycleAwareJob {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected FileListFilter<FileDataWrapper> filter;

    @Autowired(required = false)
    protected FileLocker locker;

    @Autowired
    protected SambaFileUtil sambaFileUtil;

    protected IfsLifeCycleAware ifsLifeCycleAware;
    protected String sambaDirectoryPath;

    public AbstractRecursiveDirectoryScanner(String sambaDirectoryPath) {
        Assert.notNull(sambaDirectoryPath, "Samba directory path must not be null");
        this.sambaDirectoryPath = sambaDirectoryPath;
    }

    /**
     * Primary method to list files as FileDataWrapper objects from the
     * configured Samba path. This should be used by a custom MessageSource or
     * poller.
     */
    public List<FileDataWrapper> scanForFiles() {
        Assert.notNull(sambaFileUtil, "SambaFileUtil must be configured and autowired.");
        List<FileDataWrapper> files = sambaFileUtil.listFiles(this.sambaDirectoryPath);

        if (files == null || files.isEmpty()) {
            return noEligibleFileWrappersFound();
        }

        if (filter != null) {
            // FileListFilter.filterFiles expects an array
            FileDataWrapper[] fileArray = files.toArray(new FileDataWrapper[0]);
            List<FileDataWrapper> filteredList = filter.filterFiles(fileArray);
            return filteredList;
        } else {
            return files;
        }
    }

    protected List<FileDataWrapper> noEligibleFileWrappersFound() {
        return Lists.newArrayListWithExpectedSize(0);
    }

    public void setSambaDirectoryPath(String sambaDirectoryPath) {
        this.sambaDirectoryPath = sambaDirectoryPath;
    }

    // Custom setFilter for FileDataWrapper
    public void setFilter(FileListFilter<FileDataWrapper> filter) {
        this.filter = filter;
    }

    // setLocker and tryClaim are kept but are no longer overrides. 
    // Their interaction with FileDataWrapper and SambaFileUtil needs careful consideration.
    public void setLocker(FileLocker locker) {
        this.locker = locker;
    }

    /**
     * Tries to claim a "file" represented by FileDataWrapper. The actual
     * mechanism would involve SambaFileUtil (e.g., rename, flag) or an external
     * store. The java.io.File argument is problematic here. This method
     * signature needs to change if it's to be used meaningfully with
     * FileDataWrapper. For now, this is a conceptual placeholder based on the
     * old signature.
     */
    public boolean tryClaim(FileDataWrapper fileDataWrapper) {
        // This is highly conceptual. Actual claiming on Samba would be different.
        log.info("Attempting to claim (conceptually) Samba file: " + fileDataWrapper.getPath());
        if (locker == null) {
            log.warn("FileLocker (for local files) is not configured. Samba claim for path '"
                    + fileDataWrapper.getPath() + "' is optimistic.");
            return true;
        }
        // How a Spring FileLocker (designed for java.io.File) interacts here is undefined.
        // You'd likely need a custom locking strategy for Samba resources.
        // For example, SambaFileUtil could have a method: sambaFileUtil.tryLockResource(fileDataWrapper.getPath())
        log.warn("Standard FileLocker.tryClaim(File) is not directly applicable to FileDataWrapper. "
                + "Claim for path '" + fileDataWrapper.getPath() + "' is optimistic.");
        return true;
    }
}
