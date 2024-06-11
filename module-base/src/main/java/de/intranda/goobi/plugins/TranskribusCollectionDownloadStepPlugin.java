package de.intranda.goobi.plugins;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Enumeration;

/**
 * This file is part of a plugin for Goobi - a Workflow tool for the support of mass digitization.
 *
 * Visit the websites for more information.
 *          - https://goobi.io
 *          - https://www.intranda.com
 *          - https://github.com/intranda/goobi
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free Software Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 */

import java.util.HashMap;
import java.util.List;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.commons.io.FileUtils;
import org.goobi.beans.Processproperty;
import org.goobi.beans.Step;
import org.goobi.production.enums.LogType;
import org.goobi.production.enums.PluginGuiType;
import org.goobi.production.enums.PluginReturnValue;
import org.goobi.production.enums.PluginType;
import org.goobi.production.enums.StepReturnValue;
import org.goobi.production.plugin.interfaces.IStepPluginVersion2;

import de.sub.goobi.config.ConfigPlugins;
import de.sub.goobi.config.ConfigurationHelper;
import de.sub.goobi.helper.Helper;
import de.sub.goobi.helper.StorageProvider;
import de.sub.goobi.helper.StorageProviderInterface;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import net.xeoh.plugins.base.annotations.PluginImplementation;

@PluginImplementation
@Log4j2
public class TranskribusCollectionDownloadStepPlugin implements IStepPluginVersion2 {

    @Getter
    private String title = "intranda_step_transkribus_collection_download";
    @Getter
    private Step step;
    private String transkribusLogin;
    private String transkribusPassword;
    private String transkribusApiUrl;
    private String transkribusCollection;
    private long downloadDelay;
    private String returnPath;

    @Override
    public void initialize(Step step, String returnPath) {
        this.returnPath = returnPath;
        this.step = step;

        // read parameters from correct block in configuration file
        SubnodeConfiguration myconfig = ConfigPlugins.getProjectAndStepConfig("intranda_step_transkribus_collection", step);
        transkribusLogin = myconfig.getString("transkribusLogin");
        transkribusPassword = myconfig.getString("transkribusPassword");
        transkribusApiUrl = myconfig.getString("transkribusApiUrl");
        transkribusCollection = myconfig.getString("transkribusCollection");
        downloadDelay = myconfig.getLong("downloadDelay", 3000);
        log.info("TranskribusCollectionDownload step plugin initialized");
    }

    @Override
    public PluginGuiType getPluginGuiType() {
        return PluginGuiType.NONE;
    }

    @Override
    public String getPagePath() {
        return "/uii/plugin_step_transkribus_collection.xhtml";
    }

    @Override
    public PluginType getType() {
        return PluginType.Step;
    }

    @Override
    public String cancel() {
        return "/uii" + returnPath;
    }

    @Override
    public String finish() {
        return "/uii" + returnPath;
    }

    @Override
    public int getInterfaceVersion() {
        return 0;
    }

    @Override
    public HashMap<String, StepReturnValue> validate() {
        return null;
    }

    @Override
    public boolean execute() {
        PluginReturnValue ret = run();
        return ret != PluginReturnValue.ERROR;
    }

    @Override
    public PluginReturnValue run() {
        try {

            // find the correct property with the transkribus document id
            String documentId = null;
            for (Processproperty pp : step.getProzess().getEigenschaften()) {
                if (TranskribusHelper.DOCUMENT_ID_PROPERTY.equals(pp.getTitel())) {
                    documentId = pp.getWert();
                    break;
                }
            }

            // if no documentId found stop plugin
            if (documentId == null) {
                Helper.addMessageToProcessJournal(step.getProcessId(), LogType.ERROR,
                        "Download from Transkribus collection canceled because no property '" + TranskribusHelper.DOCUMENT_ID_PROPERTY
                                + "' was found with ID in it.");
                return PluginReturnValue.ERROR;
            }

            // Login into Transkribus and get a session ID
            String sessionId = TranskribusHelper.getSessionId(transkribusApiUrl, transkribusLogin, transkribusPassword);

            // Trigger the export and get a download URL
            String downloadUrl = TranskribusHelper.startExport(documentId, sessionId, transkribusApiUrl, transkribusCollection, downloadDelay);

            // download the result into the temp folder
            File zipFile = new File(ConfigurationHelper.getInstance().getTemporaryFolder() + "/" + documentId + ".zip");
            FileUtils.copyURLToFile(new URL(downloadUrl), zipFile);

            // extract the alto files and move them to the OCR folder
            StorageProviderInterface sp = StorageProvider.getInstance();

            File tempDir = new File(ConfigurationHelper.getInstance().getTemporaryFolder() + "/" + documentId + "/");
            sp.createDirectories(tempDir.toPath());
            unzipFile(zipFile, tempDir);

            // check if OCR folder exists already, if yes rename it
            Path ocr = Paths.get(step.getProzess().getOcrDirectory());
            if (sp.isFileExists(ocr)) {
                Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                String format = "yyyy-MM-dd-HHmmssSSS";
                String suffix = new SimpleDateFormat(format).format(timestamp);
                String ocrbackup = step.getProzess().getOcrDirectory().substring(0, step.getProzess().getOcrDirectory().length() - 1) + "_" + suffix;
                sp.move(ocr, Paths.get(ocrbackup));
            }

            // create new alto folder, copy extracted files into it and synchronize file names
            Path altofolder = Paths.get(step.getProzess().getOcrAltoDirectory());
            sp.createDirectories(altofolder);
            sp.move(tempDir.toPath(), altofolder);
            synchronizeFilenames(step.getProzess().getImagesTifDirectory(false), step.getProzess().getOcrAltoDirectory());

            // delete the downloaded zip file and the temporary folder after the extraction
            sp.deleteFile(zipFile.toPath());

            // Write Success message with Document ID into the Journal
            Helper.addMessageToProcessJournal(step.getProcessId(), LogType.INFO,
                    "Download from Transkribus collection " + transkribusCollection + " successfull for document with ID " + documentId + " from URL "
                            + downloadUrl);

            log.info("TranskribusCollectionDownload step plugin executed");
            return PluginReturnValue.FINISH;

        } catch (Exception e) {
            log.error("TranskribusCollection - Error while downloading the results", e);
            Helper.addMessageToProcessJournal(step.getProcessId(), LogType.ERROR,
                    "Download from Transkribus collection canceled because of an unexpected exception: " + e.getMessage());
            return PluginReturnValue.ERROR;
        }

    }

    /**
     * Unzip just the alto files from the zip file
     * 
     * @param zipFilePath
     * @param destDir
     * @throws IOException
     */
    private static void unzipFile(File file, File destDir) throws IOException {
        try (ZipFile zipFile = new ZipFile(file)) {
            Enumeration<ZipArchiveEntry> entries = zipFile.getEntries();
            while (entries.hasMoreElements()) {
                ZipArchiveEntry entry = entries.nextElement();

                // Check if the entry is within the "alto" subfolder and is not a directory
                if (entry.getName().contains("alto/") && !entry.isDirectory()) {
                    // Get only the file name, ignore the directory path
                    String fileName = new File(entry.getName()).getName();
                    File entryDestination = new File(destDir, fileName);

                    // Extract the file
                    try (InputStream in = zipFile.getInputStream(entry);
                            FileOutputStream out = new FileOutputStream(entryDestination)) {
                        // Copy data from the archive to the output file
                        byte[] buffer = new byte[1024];
                        int len;
                        while ((len = in.read(buffer)) > 0) {
                            out.write(buffer, 0, len);
                        }
                    } catch (IOException e) {
                        throw e;
                    }
                }
            }
        } catch (IOException e) {
            throw e;
        }
    }

    public static void synchronizeFilenames(String dir1, String dir2) throws IOException {
        List<Path> list1 = StorageProvider.getInstance().listFiles(dir1);
        List<Path> list2 = StorageProvider.getInstance().listFiles(dir2);

        // Iterate over the files in the second directory
        for (int i = 0; i < list2.size(); i++) {
            if (i >= list1.size()) {
                break; // If there are more files in the second directory, stop renaming
            }

            Path file2 = list2.get(i);
            String newName = getNewFileName(list1.get(i).toFile().getName(), file2.toFile().getName());
            File newFile = new File(dir2, newName);
            StorageProvider.getInstance().renameTo(file2, newFile.getAbsolutePath());
        }
    }

    /**
     * Generates a new file name based on the name from the first directory and retains the extension of the second file.
     *
     * @param nameFromFirstDir The file name from the first directory.
     * @param nameFromSecondDir The file name from the second directory.
     * @return the new file name with the retained extension.
     */
    private static String getNewFileName(String nameFromFirstDir, String nameFromSecondDir) {
        // Extract the extension from the second file name
        int dotIndex = nameFromSecondDir.lastIndexOf('.');
        if (dotIndex == -1) {
            return nameFromFirstDir; // No extension in the second file
        }
        String extension = nameFromSecondDir.substring(dotIndex);

        // Find where the base name of the first file ends (assuming it could have an extension)
        int baseNameEndIndex = nameFromFirstDir.lastIndexOf('.');
        if (baseNameEndIndex != -1) {
            return nameFromFirstDir.substring(0, baseNameEndIndex) + extension;
        }
        return nameFromFirstDir + extension;
    }
}
