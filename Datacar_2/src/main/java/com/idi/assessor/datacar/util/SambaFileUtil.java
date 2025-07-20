package com.idi.assessor.datacar.util;

import java.util.Collections;
import java.util.List;

import com.idi.assessor.datacar.exception.IDIApplicativeException;
import org.springframework.stereotype.Component;

import com.idi.assessor.datacar.common.FileDataWrapper;


//import com.idi.plugin.factory.logger.watch.IDIStopWatch;
import jcifs.CIFSContext;
import jcifs.SmbConstants;
import jcifs.smb.DosFileFilter;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFilenameFilter;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public final class SambaFileUtil implements ApplicationContextAware {

	private final String SMB_URL_PREFIX = "smb://";
	private ApplicationContext context;

	private CIFSContext getCIFSContext() {
		return context.getBean(CIFSContext.class);
	}

	private SambaFileUtil() {
	}

	public byte[] readFile(String filepath) {
		StopWatch stopWatch = new StopWatch(filepath);
		stopWatch.start();
		String fileSmbUrl = getSmbUrlFromUnc(filepath);
		InputStream inputStream = null;
		try {
			inputStream = getFileInputStream(filepath);
			return IOUtils.toByteArray(inputStream);
		} catch (Exception e) {
			throw new IDIApplicativeException("Samba error=" + e.getMessage(), e);
		} finally {
//			IDIStopWatch.endDebugWatch(filepath, String.format("[smb] [readFile] filepath =%s", filepath));
			IOUtils.closeQuietly(inputStream);
		}
	}

	public String readFileToString(String filePath, String encoding) {
//		IDIStopWatch.startDebugWatch(filePath);

		String fileSmbUrl = getSmbUrlFromUnc(filePath);
		InputStream inputStream = null;
		try {
			inputStream = getFileInputStream(filePath);
			return IOUtils.toString(inputStream, encoding);
		} catch (IOException e) {
			throw new IDIApplicativeException("Failed reading file at URL: " + fileSmbUrl, e);
		} finally {
//			IDIStopWatch.endDebugWatch(filePath, String.format("[smb] [readFileToString] filePath=%s encoding=%s", filePath, encoding));
			IOUtils.closeQuietly(inputStream);
		}
	}

	public byte[] readFileAsByteArray(String filePath) {
//		IDIStopWatch.startDebugWatch(filePath);

		String fileSmbUrl = getSmbUrlFromUnc(filePath);
		InputStream inputStream = null;
		try {
			inputStream = getFileInputStream(filePath);
			return IOUtils.toByteArray(inputStream);
		} catch (IOException e) {
			throw new IDIApplicativeException("Failed reading file at URL: " + fileSmbUrl, e);
		} finally {
//			IDIStopWatch.endDebugWatch(filePath, String.format("[smb] [readFileAsByteArray] filePath=%s", filePath));
			IOUtils.closeQuietly(inputStream);
		}
	}

	public String readFileToString(String filePath) {
		return readFileToString(filePath, "UTF-8");
	}

	public InputStream readFileAsInputStream(String filePath) {
		return getFileInputStream(filePath);
	}

	private InputStream getFileInputStream(String filePath) {
		String fileSmbUrl = getSmbUrlFromUnc(filePath);
		try {
			SmbFile smbFile = new SmbFile(fileSmbUrl, getCIFSContext());
			return smbFile.getInputStream();
		} catch (IOException e) {
			throw new IDIApplicativeException("Failed reading file at URL=" + fileSmbUrl, e);

		}
	}

	/**
	 * !!! Dont forget to call IOUtils.closeQuietly(outputStream); after writing to the stream. !!!!
	 *
	 * @throws IOException
	 */
	public OutputStream getOutputStream(String to) throws IOException {
		String toSmbUrl = getSmbUrlFromUnc(to);
		SmbFile smbFileTo = createSmbFile(toSmbUrl);
		return smbFileTo.getOutputStream();
	}


	public void saveStringAsFile(String content, String to, String encoding) {
//		IDIStopWatch.startDebugWatch(to);

		String toSmbUrl = getSmbUrlFromUnc(to);
		OutputStreamWriter writer = null;
		try {
			SmbFile smbFileTo = createSmbFile(toSmbUrl);
			writer = new OutputStreamWriter(smbFileTo.getOutputStream(), encoding);
			writer.write(content);
		} catch (Exception e) {
			throw new IDIApplicativeException("Samba error=" + toSmbUrl, e);
		} finally {
//			IDIStopWatch.endDebugWatch(to, String.format("[smb] [saveStringAsFile] to=%s encoding=%s", to, encoding));
			IOUtils.closeQuietly(writer);
		}
	}

	public void saveStringAsFile(String content, String to) {
		saveStringAsFile(content, to, "UTF-8");
	}

	public void saveStringAsHtml(String content, String destDirectory) {
		OutputStreamWriter writer = null;
		try {
			SmbFile smbDestFile = createNewSmbFile(destDirectory, "body.html");
			writer = new OutputStreamWriter(smbDestFile.getOutputStream(), "UTF-8");
			writer.write(content);
		} catch (Exception e) {
			throw new IDIApplicativeException("Samba error=" + e.getMessage(), e);
		} finally {
			IOUtils.closeQuietly(writer);
		}
	}

	public SmbFile createNewSmbFile(String destDirectory, String fileName) throws MalformedURLException, SmbException {
		String toSmbUrl = getSmbUrlFromUnc(destDirectory);
		SmbFile smbDirFileTo = new SmbFile(toSmbUrl, getCIFSContext());
		if (!smbDirFileTo.exists()) {
			smbDirFileTo.mkdirs();
		}
		SmbFile smbDestFile = new SmbFile(smbDirFileTo.getPath() + "/" + fileName, getCIFSContext());
		if (!smbDestFile.isFile()) {
			smbDestFile.createNewFile();
		}
		return smbDestFile;
	}

	public void copyFile(String from, String to) {
//		IDIStopWatch.startDebugWatch(to);
		String fromSmbUrl = getSmbUrlFromUnc(from);
		String toSmbUrl = getSmbUrlFromUnc(to);
		try {
			SmbFile smbFileFrom = new SmbFile(fromSmbUrl, getCIFSContext());
			SmbFile smbFileTo = createSmbFile(toSmbUrl);
			smbFileFrom.copyTo(smbFileTo);
			LocalDateTime localDateTime = LocalDateTime.now();
			ZonedDateTime zdt = ZonedDateTime.of(localDateTime, ZoneId.systemDefault());
			long date = zdt.toInstant().toEpochMilli();
			smbFileTo.setLastModified(date);
		} catch (Exception e) {
			throw new IDIApplicativeException("Samba error=" + toSmbUrl, e);
		} finally {
//			IDIStopWatch.endDebugWatch(to, String.format("[smb] [copyFile] from =%s to=%s", from, to));
		}
	}

	public void moveFolder(String from, String to) {
//		IDIStopWatch.startDebugWatch(to);
		copyFolder(from, to);
		String fromSmbUrl = getSmbUrlFromUnc(from);
		try {
			SmbFile smbFileFrom = new SmbFile(fromSmbUrl, getCIFSContext());
			smbFileFrom.delete();
		} catch (Exception e) {
			throw new IDIApplicativeException("Samba error=" + e.getMessage(), e);
		} finally {
//			IDIStopWatch.endDebugWatch(to, String.format("[smb] [moveFolder] from =%s to=%s", from, to));
		}
	}

	public void copyFolder(String from, String to) {
//		IDIStopWatch.startDebugWatch(to);
		String fromSmbUrl = getSmbUrlFromUnc(from);
		String toSmbUrl = getSmbUrlFromUnc(to);
		try {
			SmbFile smbFileFrom = new SmbFile(fromSmbUrl, getCIFSContext());
			SmbFile smbFileTo = new SmbFile(toSmbUrl, getCIFSContext());
			smbFileFrom.copyTo(smbFileTo);
			smbFileTo.setLastModified(LocalDateTime.MAX.getNano());
		} catch (Exception e) {
			throw new IDIApplicativeException("Samba error=" + toSmbUrl, e);
		} finally {
//			IDIStopWatch.endDebugWatch(to, String.format("[smb] [copyFolder] from =%s to=%s", from, to));
		}
	}

	public void moveFile(String from, String to) {
//		IDIStopWatch.startDebugWatch(to);
		copyFile(from, to);
		String fromSmbUrl = getSmbUrlFromUnc(from);
		try {
			SmbFile smbFileFrom = new SmbFile(fromSmbUrl, getCIFSContext());
			smbFileFrom.delete();
		} catch (Exception e) {
			throw new IDIApplicativeException("Samba error=" + e.getMessage(), e);
		} finally {
//			IDIStopWatch.endDebugWatch(to, String.format("[smb] [moveFile] from =%s to=%s", from, to));
		}
	}

	public boolean deleteFile(String srcFile) {
//		IDIStopWatch.startDebugWatch(srcFile);
		String fromSmbUrl = getSmbUrlFromUnc(srcFile);
		try {
			SmbFile smbFileFrom = new SmbFile(fromSmbUrl, getCIFSContext());
			smbFileFrom.delete();
			return smbFileFrom.exists();
		} catch (Exception e) {
			throw new IDIApplicativeException("Samba error=" + e.getMessage(), e);
		} finally {
//			IDIStopWatch.endDebugWatch(srcFile, String.format("[smb] [deleteFile] srcFile =%s", srcFile));
		}
	}

	public String getSmbUrlFromUnc(String path) {
		String smbUrl = StringUtils.trimLeadingCharacter(path, '\\');
		smbUrl = StringUtils.trimLeadingCharacter(smbUrl, '/');
		smbUrl = smbUrl.replace('\\', '/');
		smbUrl = SMB_URL_PREFIX + smbUrl;
		return smbUrl;
	}

	private String getDirPath(String smbUrl) {
		if (smbUrl == null) {
			throw new IllegalArgumentException("Filename == null");
		}
		int pos = smbUrl.lastIndexOf('/');
		if (pos > 0 && pos < smbUrl.length() - 1) {
			return smbUrl.substring(0, pos + 1);
		}
		throw new IllegalArgumentException("Samba url is not a valid file representation: " + smbUrl);
	}

	public boolean isFileExists(String fileUrl) {
//		IDIStopWatch.startDebugWatch(fileUrl);

		String toSmbUrl = getSmbUrlFromUnc(fileUrl);
		try {
			SmbFile smbFile = new SmbFile(toSmbUrl, getCIFSContext());
			return smbFile.exists();
		} catch (Exception e) {
			throw new IDIApplicativeException("Samba error=" + e.getMessage(), e);
		} finally {
//			IDIStopWatch.endDebugWatch(fileUrl, String.format("[smb] [isFileExists] filePath =%s", fileUrl));
		}
	}

	public String[] list(String directoryUrl) {
		return list(directoryUrl, null);
	}

	public String[] list(String directoryUrl, SmbFilenameFilter fileFilter) {
		final String id = "list" + directoryUrl;
//		IDIStopWatch.startDebugWatch(id);
		directoryUrl = getSmbUrlFromUnc(directoryUrl);
		try {
			SmbFile directory = new SmbFile(directoryUrl, getCIFSContext());
			return directory.list(fileFilter);
		} catch (Exception e) {
			throw new IDIApplicativeException("Samba error=" + e.getMessage(), e);
		} finally {
//			IDIStopWatch.endDebugWatch(id, String.format("[smb] [list] dirPath =%s", directoryUrl));
		}
	}

	public List<FileDataWrapper> listFiles(String directoryUrl) {
		final String id = "listFilesAsWrapper" + directoryUrl;
		directoryUrl = getSmbUrlFromUnc(directoryUrl);

		try {
			SmbFile directory = new SmbFile(directoryUrl, getCIFSContext());
			if (!directory.exists() || !directory.isDirectory()) {
				return Collections.emptyList();
			}

			SmbFile[] smbFiles = directory.listFiles(new DosFileFilter("*",
					SmbConstants.ATTR_COMPRESSED + SmbConstants.ATTR_ARCHIVE));

			if (smbFiles == null || smbFiles.length == 0) {
				return Collections.emptyList();
			}

			return Arrays.stream(smbFiles)
					.filter(smbFile -> {
						try {
							return smbFile.isFile();
						} catch (SmbException e) {
							// Log warning but continue processing other files
							return false;
						}
					})
					.map(smbFile -> {
						try {
							String fullPath = smbFile.getPath();
							// Convert SMB path back to UNC path for consistency
							String uncPath = fullPath.substring(6); // Remove "smb://" prefix
							uncPath = uncPath.replace('/', '\\');
							if (!uncPath.startsWith("\\")) {
								uncPath = "\\" + uncPath;
							}

							String fileName = smbFile.getName();
							long size = smbFile.length();
							long lastModified = smbFile.getLastModified();

							// Create FileDataWrapper without content (lazy loading)
							return new FileDataWrapper(uncPath, fileName, size, lastModified);
						} catch (SmbException e) {
							throw new IDIApplicativeException("Failed to read file metadata for: " + smbFile.getPath(), e);
						}
					})
					.collect(Collectors.toList());

		} catch (Exception e) {
			throw new IDIApplicativeException("Samba error while listing files as FileDataWrapper: " + e.getMessage(), e);
		}
	}

	public List<String> listFileNames(String directoryUrl) {
		final String id = "list" + directoryUrl;
		directoryUrl = getSmbUrlFromUnc(directoryUrl);
		try {
			SmbFile directory = new SmbFile(directoryUrl, getCIFSContext());
			return Arrays.stream(directory.listFiles(new DosFileFilter("*", SmbConstants.ATTR_COMPRESSED + SmbConstants.ATTR_ARCHIVE))).map(SmbFile::getName).collect(Collectors.toList());
		} catch (Exception e) {
			throw new IDIApplicativeException("Samba error=" + e.getMessage(), e);
		}
	}


	public List<String> listDirs(String directoryUrl) {
		final String id = "list" + directoryUrl;
//		IDIStopWatch.startDebugWatch(id);
		directoryUrl = getSmbUrlFromUnc(directoryUrl);
		try {
			SmbFile directory = new SmbFile(directoryUrl, getCIFSContext());
			return Arrays.stream(directory.listFiles(new DosFileFilter("*", SmbFile.ATTR_DIRECTORY))).map(SmbFile::getName).collect(Collectors.toList());
		} catch (Exception e) {
			throw new IDIApplicativeException("Samba error=" + e.getMessage(), e);
		} finally {
//			IDIStopWatch.endDebugWatch(id, String.format("[smb] [listDirs] dirPath =%s", directoryUrl));
		}
	}

	private SmbFile createSmbFile(String smbUrl) throws MalformedURLException, SmbException {
		SmbFile smbFileTo = new SmbFile(smbUrl, getCIFSContext());
		String toDirUrl = getDirPath(smbUrl);
		SmbFile smbDirTo = new SmbFile(toDirUrl, getCIFSContext());
		if (!smbDirTo.exists()) {
			smbDirTo.mkdirs();
		}
		return smbFileTo;
	}

	public boolean createFolderForUrl(String smbUrl) {
		try {
			String smbUrlFromUnc = getSmbUrlFromUnc(smbUrl);
			String toDirUrl = getDirPath(smbUrlFromUnc);
			SmbFile smbFileTo = new SmbFile(smbUrlFromUnc, getCIFSContext());
			smbFileTo.mkdirs();
			return smbFileTo.exists();
		} catch (Exception e) {
			throw new IDIApplicativeException("Samba error=" + e.getMessage(), e);
		}
	}

	public SmbFile getSmbFile(String filePath) throws MalformedURLException, SmbException {
		return new SmbFile(filePath, getCIFSContext());
	}

	public long getSize(String filePath) throws MalformedURLException, SmbException {
		return new SmbFile(getSmbUrlFromUnc(filePath), getCIFSContext()).length();
	}

	public String getParent(String filePath) throws MalformedURLException, SmbException {
		return new SmbFile(getSmbUrlFromUnc(filePath), getCIFSContext()).getParent().substring(4);
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.context = applicationContext;
	}


	// Fix the saveDataToFile method to match the usage in config
	public SmbFile saveDataToFile(String directory, String fileName, byte[] data) {
		String fullPath = directory;
		if (!directory.endsWith("/") && !directory.endsWith("\\")) {
			fullPath += "/";
		}
		fullPath += fileName;

		String toSmbUrl = getSmbUrlFromUnc(fullPath);
		OutputStream writer = null;
		try {
			SmbFile smbFileTo = createSmbFile(toSmbUrl);
			writer = smbFileTo.getOutputStream();
			writer.write(data);
			return smbFileTo;
		} catch (Exception e) {
			throw new IDIApplicativeException("Failed creating Samba file at=" + toSmbUrl, e);
		} finally {
			IOUtils.closeQuietly(writer);
		}
	}

	// Add the writeFile method that's referenced in the config
	public void writeFile(String directory, String fileName, byte[] data) {
		saveDataToFile(directory, fileName, data);
	}

	// Keep the original saveDataToFile method for backward compatibility
	public SmbFile saveDataToFile(byte[] data, String to) {
		String toSmbUrl = getSmbUrlFromUnc(to);
		OutputStream writer = null;
		try {
			SmbFile smbFileTo = createSmbFile(toSmbUrl);
			writer = smbFileTo.getOutputStream();
			writer.write(data);
			return smbFileTo;
		} catch (Exception e) {
			throw new IDIApplicativeException("Failed creating Samba file at=" + toSmbUrl, e);
		} finally {
			IOUtils.closeQuietly(writer);
		}
	}
}

