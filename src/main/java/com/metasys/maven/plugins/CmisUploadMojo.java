package com.metasys.maven.plugins;

import org.apache.chemistry.opencmis.client.api.*;
import org.apache.chemistry.opencmis.client.runtime.OperationContextImpl;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.exceptions.CmisRuntimeException;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ContentStreamImpl;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.Detector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigInteger;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Upload a folder with sub-folders and files to a CMIS compliant repository.
 */
@Mojo(name = "cmis-upload", defaultPhase = LifecyclePhase.INSTALL)
public class CmisUploadMojo extends AbstractMojo {

    public static final String DEFAULT_USERNAME = "admin";

    public static final String DEFAULT_PASSWORD = "admin";

    /**
     * The username.
     */
    @Parameter(property = "username", defaultValue = DEFAULT_USERNAME, alias = "username")
    private String username;

    /**
     * The password.
     */
    @Parameter(property = "password", defaultValue = DEFAULT_PASSWORD, alias = "password")
    private String password;

    /**
     * The CMIS endpoint URL.
     */
    @Parameter(property = "url", required = true, alias = "url")
    private URL cmisUrl;

    /**
     * The path to upload the document to.
     */
    @Parameter(property = "localPath", alias = "localPath", required = true)
    private String localPath;

    /**
     * The path to upload the document to.
     */
    @Parameter(property = "destPath", alias = "destPath", required = true)
    private String destPath;

    /**
     * If the file can be overwritten or not
     */
    @Parameter(property = "overwrite", alias = "overwrite", defaultValue = "false", required = true)
    private boolean overwrite;


    private Session _session;

    @Override
    public void execute() throws MojoExecutionException {
        _session = createSession();

        try {
            Collection<File> files = FileUtils.listFilesAndDirs(
                    new File(localPath),
                    FileFileFilter.FILE,
                    DirectoryFileFilter.DIRECTORY
            );

            boolean skippedRoot = false;

            for (File file : files) {
                if(!skippedRoot) {
                    skippedRoot = true;
                    continue;
                }

                String fullFolderPath = new File(destPath, file.getAbsolutePath().substring(localPath.length())).getPath();
                String parentFolder = new File(fullFolderPath).getParentFile().getPath();

                if (file.isDirectory()) {
                    getLog().info("Creating directory " + file.getName() + " in " + parentFolder);

                    if ((getObjectByPath(fullFolderPath)) == null) {
                        Folder folder = createFolder(parentFolder, file.getName(), "cmis:folder");
                        getLog().info("Created folder " + folder.getPath() + "(" + folder.getId() + ")");
                    }
                } else if (file.isFile()) {
                    getLog().info("Creating file " + file.getName() + " in " + parentFolder);
                    CmisObject parentFolderObject;
                    if ((parentFolderObject = getObjectByPath(parentFolder)) == null) {
                        parentFolderObject = createFolder(parentFolder, file.getName(), "cmis:folder");
                        getLog().info("Created folder " + ((Folder)parentFolderObject).getPath() + "(" + parentFolderObject.getId() + ")");
                    }
                    if (getObjectByPath(fullFolderPath) != null && overwrite) {
                        CmisObject object = _session.getObjectByPath(fullFolderPath);
                        _session.delete(object);
                    }
                    uploadFile(file.getPath(), file, (Folder) parentFolderObject);
                }
            }
        } catch (IOException ex) {
            getLog().error("Could not open the file to be uploaded.", ex);
        } catch (CmisRuntimeException ex) {
            ex.printStackTrace();
        } catch (PluginException ex) {
            ex.printStackTrace();
        }
    }

    private void uploadFile(String path, File file, Folder destinationFolder) throws IOException {
        Map<String, Serializable> properties = new HashMap<String, Serializable>();

        properties.put(PropertyIds.NAME, file.getName());
        properties.put(PropertyIds.OBJECT_TYPE_ID, "cmis:document");

        FileInputStream fileInputStream = new FileInputStream(file);
        TikaConfig config = TikaConfig.getDefaultConfig();
        Detector detector = config.getDetector();
        TikaInputStream stream = TikaInputStream.get(fileInputStream);

        Metadata metadata = new Metadata();
        metadata.add(Metadata.RESOURCE_NAME_KEY, file.getName());

        MediaType type = detector.detect(stream, metadata);

        ContentStream contentStream = new ContentStreamImpl(file.getAbsolutePath(), BigInteger.valueOf(file.length()), type.getType() + "/" + type.getSubtype(), fileInputStream);
        _session.clear();
        Document document = destinationFolder.createDocument(properties, contentStream, VersioningState.MAJOR);
        getLog().info("Uploaded '" + file.getName() + "' to '" + path + "' with a document id of '" + document.getId() + "' as " + type.getType() + "/" + type.getSubtype());
    }

    private CmisObject getObjectByPath(String path) {
        try {
            OperationContext oc = new OperationContextImpl();
            oc.setCacheEnabled(false);
            return _session.getObjectByPath(path, oc);
        } catch (Exception e) {
            // ignore
        }
        return null;
    }

    private Folder createFolder(String parentPath, String folderName, String objectTypeId) throws PluginException {
        if (getLog().isDebugEnabled()) {
            getLog().info("Creating folder '" + folderName + "' in location '" + parentPath + "' folder type is '" + objectTypeId + "'");
        }
        Folder root;
        if (parentPath == null) {
            root = _session.getRootFolder();
        } else {
            root = (Folder) _session.getObjectByPath(parentPath);
        }

        Map<String, String> newFolderProps = new HashMap<String, String>();
        newFolderProps.put(PropertyIds.OBJECT_TYPE_ID, objectTypeId);
        newFolderProps.put(PropertyIds.NAME, folderName);
        Folder folder = root.createFolder(newFolderProps, null, null, null, _session.getDefaultContext());
        if (getLog().isDebugEnabled()) {
            getLog().debug("Created folder " + folder.getId());
        }
        return folder;
    }

    private Session createSession() {
        Map<String, String> parameter = new HashMap<String, String>();

        // user credentials
        parameter.put(SessionParameter.USER, username);
        parameter.put(SessionParameter.PASSWORD, password);

        // connection settings
        parameter.put(SessionParameter.ATOMPUB_URL, cmisUrl.toExternalForm());
        parameter.put(SessionParameter.BINDING_TYPE, BindingType.ATOMPUB.value());

        // create session
        SessionFactory factory = SessionFactoryImpl.newInstance();

        List<Repository> repositories = factory.getRepositories(parameter);

        Repository repository = repositories.get(0);

        return repository.createSession();
    }
}