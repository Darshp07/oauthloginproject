package com.example.googleoauth.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.googleoauth.dto.GoogleDriveFileResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.InputStreamContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;

@Service
public class GoogleDriveService {

    private static final String APPLICATION_NAME =
            "Google OAuth Login Project";

    private static final GsonFactory JSON_FACTORY =
            GsonFactory.getDefaultInstance();

    // ==========================================
    // Download Response Data
    // ==========================================

    public record DriveDownloadData(
            String fileName,
            String mimeType,
            InputStream inputStream
    ) {}

    // ==========================================
    // Create Google Drive Client
    // ==========================================

    private Drive getDriveService(String accessToken) throws IOException {

        try {
            GoogleCredentials credentials =
                    GoogleCredentials.create(
                            new AccessToken(accessToken, null)
                    );

            NetHttpTransport httpTransport =
                    GoogleNetHttpTransport.newTrustedTransport();

            return new Drive.Builder(
                    httpTransport,
                    JSON_FACTORY,
                    new HttpCredentialsAdapter(credentials)
            )
            .setApplicationName(APPLICATION_NAME)
            .build();

        } catch (Exception e) {
            throw new IOException(
                    "Unable to create Google Drive service: "
                    + e.getMessage(), e
            );
        }
    }

    // ==========================================
    // 1. Get All Drive Files
    // ==========================================

    public List<GoogleDriveFileResponse> getAllFiles(
            String accessToken) throws IOException {

        try {
            Drive driveService = getDriveService(accessToken);

            FileList result = driveService.files()
                    .list()
                    .setPageSize(100)
                    .setOrderBy("modifiedTime desc")
                    .setFields(
                            "files(id,name,mimeType,webViewLink,"
                            + "modifiedTime,size,iconLink),nextPageToken"
                    )
                    .execute();

            return result.getFiles()
                    .stream()
                    .map(file -> new GoogleDriveFileResponse(
                            file.getId(),
                            file.getName(),
                            file.getMimeType(),
                            file.getWebViewLink(),
                            file.getModifiedTime() == null
                                    ? null
                                    : file.getModifiedTime().toString(),
                            file.getSize() == null
                                    ? null
                                    : file.getSize().toString(),
                            file.getIconLink()
                    ))
                    .toList();

        } catch (Exception e) {
            throw new IOException(
                    "Unable to fetch Google Drive files: "
                    + e.getMessage(), e
            );
        }
    }

    // ==========================================
    // 2. Upload File to Google Drive
    // ==========================================

    public GoogleDriveFileResponse uploadFile(
            String accessToken,
            MultipartFile multipartFile) throws IOException {

        try {
            if (multipartFile == null || multipartFile.isEmpty()) {
                throw new IllegalArgumentException(
                        "Please select a file to upload."
                );
            }

            Drive driveService = getDriveService(accessToken);

            File fileMetadata = new File();
            fileMetadata.setName(multipartFile.getOriginalFilename());

            InputStreamContent mediaContent =
                    new InputStreamContent(
                            multipartFile.getContentType() != null
                                    ? multipartFile.getContentType()
                                    : "application/octet-stream",
                            multipartFile.getInputStream()
                    );

            mediaContent.setLength(multipartFile.getSize());

            File uploadedFile = driveService.files()
                    .create(fileMetadata, mediaContent)
                    .setFields(
                            "id,name,mimeType,webViewLink,"
                            + "modifiedTime,size,iconLink"
                    )
                    .execute();

            return new GoogleDriveFileResponse(
                    uploadedFile.getId(),
                    uploadedFile.getName(),
                    uploadedFile.getMimeType(),
                    uploadedFile.getWebViewLink(),
                    uploadedFile.getModifiedTime() == null
                            ? null
                            : uploadedFile.getModifiedTime().toString(),
                    uploadedFile.getSize() == null
                            ? null
                            : uploadedFile.getSize().toString(),
                    uploadedFile.getIconLink()
            );

        } catch (Exception e) {
            throw new IOException(
                    "Unable to upload Google Drive file: "
                    + e.getMessage(), e
            );
        }
    }

    // ==========================================
    // 3. Download File from Google Drive
    // ==========================================

    public DriveDownloadData downloadFile(
            String accessToken,
            String fileId) throws IOException {

        try {
            if (fileId == null || fileId.isBlank()) {
                throw new IllegalArgumentException(
                        "File ID is required."
                );
            }

            Drive driveService = getDriveService(accessToken);

            // Get file information
            File file = driveService.files()
                    .get(fileId)
                    .setFields("id,name,mimeType")
                    .execute();

            // Google Docs/Sheets/Slides need export, not media download
            if (file.getMimeType() != null &&
                    file.getMimeType().startsWith("application/vnd.google-apps.")) {

                throw new IOException(
                        "Google Workspace files need export format. "
                        + "Direct download is supported for regular files."
                );
            }

            // Download file content as stream
            InputStream inputStream = driveService.files()
                    .get(fileId)
                    .executeMediaAsInputStream();

            return new DriveDownloadData(
                    file.getName(),
                    file.getMimeType() != null
                            ? file.getMimeType()
                            : "application/octet-stream",
                    inputStream
            );

        } catch (Exception e) {
            throw new IOException(
                    "Unable to download Google Drive file: "
                    + e.getMessage(), e
            );
        }
    }
    public void deleteFile(String accessToken, String fileId) throws IOException {
        try {
            if (fileId == null || fileId.isBlank()) {
                throw new IllegalArgumentException("File ID is required.");
            }

            Drive driveService = getDriveService(accessToken);

            driveService.files()
                    .delete(fileId)
                    .execute();

        } catch (Exception e) {
            throw new IOException(
                    "Unable to delete Google Drive file: " + e.getMessage(), e
            );
        }
    }
}