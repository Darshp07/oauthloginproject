
package com.example.googleoauth.dto;

public record GoogleDriveFileResponse(
        String id,
        String name,
        String mimeType,
        String webViewLink,
        String modifiedTime,
        String size,
        String iconLink
) {}
