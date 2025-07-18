package com.idi.assessor.datacar.scanner;

import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.stereotype.Component;

import com.idi.assessor.datacar.common.FileDataWrapper;

@Component("dataCarRecursiveDirectoryScanner")
// Extending the new custom abstract class. Using Object for unspecified generics K, V, Scanner.
public class DataCarRecursiveDirectoryScanner extends AbstractRecursiveDirectoryScanner<Object, Object, Object> {

    // Constructor now takes the Samba directory path
    public DataCarRecursiveDirectoryScanner(String sambaDirectoryPath) {
        super(sambaDirectoryPath);

        CompositeFileListFilter<FileDataWrapper> fileFilter = new CompositeFileListFilter<>();

        FileListFilter<FileDataWrapper> exampleCustomFilter = files -> {
            java.util.List<FileDataWrapper> accepted = new java.util.ArrayList<>();
            for (FileDataWrapper fdw : files) {
                if (!fdw.getName().startsWith(".")) {
                    accepted.add(fdw);
                }
            }
            return accepted;
        };
        fileFilter.addFilter(exampleCustomFilter);
        this.setFilter(fileFilter);
    }

    // Default constructor for cases where path might be set via setter or later configuration by Spring
    // The DSL config currently uses the constructor with path, so this might not be strictly needed for that usage.
    public DataCarRecursiveDirectoryScanner() {
        super(null); // Or a default path, or throw exception if path is mandatory for this specific scanner
        // Initialize filters as well if a default path is meaningful
        System.out.println("DataCarRecursiveDirectoryScanner created with no initial Samba path. Ensure setSambaDirectoryPath is called if used this way.");
        CompositeFileListFilter<FileDataWrapper> fileFilter = new CompositeFileListFilter<>();
        FileListFilter<FileDataWrapper> exampleCustomFilter = files -> {
            java.util.List<FileDataWrapper> accepted = new java.util.ArrayList<>();
            for (FileDataWrapper fdw : files) {
                if (!fdw.getName().startsWith(".")) {
                    accepted.add(fdw);
                }
            }
            return accepted;
        };
        fileFilter.addFilter(exampleCustomFilter);
        this.setFilter(fileFilter);
    }
}
