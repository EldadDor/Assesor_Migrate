package com.idi.assessor.datacar.scanner;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.stereotype.Component;

import com.idi.assessor.datacar.common.FileDataWrapper;

@Component("recursiveTodayOnlyDirectoryScanner")
public class RecursiveTodayOnlyDirectoryScanner extends AbstractRecursiveDirectoryScanner<Object, Object, Object> {

    public RecursiveTodayOnlyDirectoryScanner(String sambaDirectoryPath) {
        super(sambaDirectoryPath);

        CompositeFileListFilter<FileDataWrapper> mainFilter = new CompositeFileListFilter<>();

        // "Today only" filter logic for FileDataWrapper
        FileListFilter<FileDataWrapper> todayOnlyFilter = files -> {
            List<FileDataWrapper> acceptedFiles = new ArrayList<>();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.ENGLISH);
            String todayStr = sdf.format(new Date());

            for (FileDataWrapper fdw : files) {
                String fileDateStr = sdf.format(new Date(fdw.getLastModifiedTimestamp()));
                if (todayStr.equals(fileDateStr)) {
                    acceptedFiles.add(fdw);
                }
            }
            return acceptedFiles;
        };
        mainFilter.addFilter(todayOnlyFilter);

        // You might also want an adapted AcceptOnceFileListFilter<FileDataWrapper> here
        // mainFilter.addFilter(new YourAdaptedAcceptOnceFilterForFileDataWrapper());
        this.setFilter(mainFilter);
    }

    // Default constructor for cases where path might be set via setter or later configuration by Spring
    public RecursiveTodayOnlyDirectoryScanner() {
        super(null); // Or a default path
        System.out.println("RecursiveTodayOnlyDirectoryScanner created with no initial Samba path. Ensure setSambaDirectoryPath is called if used this way.");
        // Initialize filters as well if a default path is meaningful
        CompositeFileListFilter<FileDataWrapper> mainFilter = new CompositeFileListFilter<>();
        FileListFilter<FileDataWrapper> todayOnlyFilter = files -> {
            List<FileDataWrapper> acceptedFiles = new ArrayList<>();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd", Locale.ENGLISH);
            String todayStr = sdf.format(new Date());
            for (FileDataWrapper fdw : files) {
                String fileDateStr = sdf.format(new Date(fdw.getLastModifiedTimestamp()));
                if (todayStr.equals(fileDateStr)) {
                    acceptedFiles.add(fdw);
                }
            }
            return acceptedFiles;
        };
        mainFilter.addFilter(todayOnlyFilter);
        this.setFilter(mainFilter);
    }

    // Ensure no listEligibleFiles(File directory) method is present here, 
    // as it was in the old AbstractDirectoryScanner from Spring and might be causing confusion.
    // The primary scanning method is scanForFiles() inherited from AbstractRecursiveDirectoryScanner.
}
