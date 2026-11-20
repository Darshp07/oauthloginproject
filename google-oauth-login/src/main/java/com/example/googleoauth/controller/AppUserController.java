
package com.example.googleoauth.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.googleoauth.Service.AppUserService;
import com.example.googleoauth.Service.GoogleCalendarService;
import com.example.googleoauth.Service.GoogleDriveService;
import com.example.googleoauth.Utility.ApiResponse;
import com.example.googleoauth.Utility.ResponseBuilder;
import com.example.googleoauth.dto.AppUserResponse;
import com.example.googleoauth.dto.GoogleDriveFileResponse;
import com.example.googleoauth.entity.AccountStatus;
import com.google.api.services.calendar.model.Event;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth/appUsers")
@RequiredArgsConstructor
@Tag(name = "User API")
public class AppUserController {

    private final AppUserService authService;
    private final GoogleDriveService googleDriveService;
    private  final GoogleCalendarService googleCalendarService;
    // ==========================================
    // 1. Get All Users
    // ==========================================

    @GetMapping("/GetAllUsers")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<AppUserResponse>> getUsers() {

        return authService.getUsers();
    }

    // ==========================================
    // 2. Get Logged-in User
    // ==========================================

    @GetMapping("/me")
    public ApiResponse<AppUserResponse> getCurrentUser(
            Authentication authentication) {

        return authService.getUserByEmail(
                authentication.getName()
        );
    }

    // ==========================================
    // 3. Update Account Status
    // ==========================================

    @PutMapping("/{userId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AppUserResponse> updateAccountStatus(
            @PathVariable Long userId,
            @Valid @RequestBody AccountStatusRequest request,
            Authentication authentication) {

        return authService.updateAccountStatus(
                userId,
                request.status(),
                authentication.getName()
        );
    }

    // ==========================================
    // 4. Register Local User
    // ==========================================

    @PostMapping("/register")
    public ApiResponse<String> register(
            @Valid @RequestBody RegisterRequest request) {

        try {

            return authService.registerLocalUser(
                    request.name().trim(),
                    request.email().trim(),
                    request.password()
            );

        } catch (IllegalArgumentException e) {

            return ResponseBuilder.error(
                    "Email is already registered"
            );
        }
    }
 // ==========================================
 // Google Drive - Get All Files
 // ==========================================

 @GetMapping("/drive/files")
 public ApiResponse<List<GoogleDriveFileResponse>> getAllDriveFiles(

         @RegisteredOAuth2AuthorizedClient("google")
         OAuth2AuthorizedClient authorizedClient) {

     try {

         String accessToken = authorizedClient
                 .getAccessToken()
                 .getTokenValue();

         List<GoogleDriveFileResponse> files =
                 googleDriveService.getAllFiles(accessToken);

         return ResponseBuilder.success(files);

     } catch (Exception e) {

         return ResponseBuilder.error(
                 "Unable to fetch Google Drive files: "
                 + e.getMessage()
         );
     }
 }
 

//==========================================
//Upload File to Google Drive
//==========================================

@PostMapping(
  value = "/drive/upload",
  consumes = MediaType.MULTIPART_FORM_DATA_VALUE
)
public ApiResponse<GoogleDriveFileResponse> uploadDriveFile(

      @RegisteredOAuth2AuthorizedClient("google")
      OAuth2AuthorizedClient authorizedClient,

      @RequestParam("file") MultipartFile file) {

  try {

      String accessToken = authorizedClient
              .getAccessToken()
              .getTokenValue();

      GoogleDriveFileResponse uploadedFile =
              googleDriveService.uploadFile(accessToken, file);

      return ResponseBuilder.success(uploadedFile);

  } catch (Exception e) {

      return ResponseBuilder.error(
              "Unable to upload Google Drive file: "
              + e.getMessage()
      );
  }
}

@GetMapping("/drive/download/{fileId}")
public ResponseEntity<?> downloadDriveFile(

        @PathVariable String fileId,

        @RegisteredOAuth2AuthorizedClient("google")
        OAuth2AuthorizedClient authorizedClient) {

    try {

        String accessToken = authorizedClient
                .getAccessToken()
                .getTokenValue();

        GoogleDriveService.DriveDownloadData downloadData =
                googleDriveService.downloadFile(
                        accessToken,
                        fileId
                );

        String mimeType = downloadData.mimeType() != null
                ? downloadData.mimeType()
                : "application/octet-stream";

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(
                MediaType.parseMediaType(mimeType)
        );

        headers.setContentDisposition(
                ContentDisposition.attachment()
                        .filename(
                                downloadData.fileName(),
                                StandardCharsets.UTF_8
                        )
                        .build()
        );

        return ResponseEntity.ok()
                .headers(headers)
                .body(
                        new InputStreamResource(
                                downloadData.inputStream()
                        )
                );

    } catch (Exception e) {

        return ResponseEntity
                .internalServerError()
                .contentType(MediaType.TEXT_PLAIN)
                .body(
                        "Unable to download file: " + e.getMessage()
                );
    }
}

@DeleteMapping("/drive/delete/{fileId}")
public ApiResponse<String> deleteDriveFile(
        @PathVariable String fileId,
        @RegisteredOAuth2AuthorizedClient("google")
        OAuth2AuthorizedClient authorizedClient) {

    try {
        String accessToken = authorizedClient
                .getAccessToken()
                .getTokenValue();

        googleDriveService.deleteFile(accessToken, fileId);

        return ResponseBuilder.success("File deleted successfully.");

    } catch (Exception e) {
        return ResponseBuilder.error(
                "Unable to delete file: " + e.getMessage()
        );
    }
}

//1. Create Calendar Event
@PostMapping("/calendar/events")
public ApiResponse<Event> createCalendarEvent(
     @RegisteredOAuth2AuthorizedClient("google")
     OAuth2AuthorizedClient authorizedClient,
     @RequestBody Event event) {

 try {
     String accessToken = authorizedClient
             .getAccessToken().getTokenValue();

     Event createdEvent =
             googleCalendarService.createEvent(accessToken, event);

     return ResponseBuilder.success(createdEvent);

 } catch (Exception e) {
     return ResponseBuilder.error(
             "Unable to create event: " + e.getMessage());
 }
}


//2. Get All Calendar Events
@GetMapping("/calendar/events")
public ApiResponse<List<Event>> getCalendarEvents(
     @RegisteredOAuth2AuthorizedClient("google")
     OAuth2AuthorizedClient authorizedClient) {

 try {
     String accessToken = authorizedClient
             .getAccessToken().getTokenValue();

     List<Event> events =
             googleCalendarService.getAllEvents(accessToken);

     return ResponseBuilder.success(events);

 } catch (Exception e) {
     return ResponseBuilder.error(
             "Unable to fetch events: " + e.getMessage());
 }
}


//3. Update Calendar Event
@PutMapping("/calendar/events/{eventId}")
public ApiResponse<Event> updateCalendarEvent(
     @PathVariable String eventId,
     @RegisteredOAuth2AuthorizedClient("google")
     OAuth2AuthorizedClient authorizedClient,
     @RequestBody Event event) {

 try {
     String accessToken = authorizedClient
             .getAccessToken().getTokenValue();

     Event updatedEvent =
             googleCalendarService.updateEvent(
                     accessToken, eventId, event);

     return ResponseBuilder.success(updatedEvent);

 } catch (Exception e) {
     return ResponseBuilder.error(
             "Unable to update event: " + e.getMessage());
 }
}


//4. Delete Calendar Event
@DeleteMapping("/calendar/events/{eventId}")
public ApiResponse<String> deleteCalendarEvent(
     @PathVariable String eventId,
     @RegisteredOAuth2AuthorizedClient("google")
     OAuth2AuthorizedClient authorizedClient) {

 try {
     String accessToken = authorizedClient
             .getAccessToken().getTokenValue();

     googleCalendarService.deleteEvent(accessToken, eventId);

     return ResponseBuilder.success("Event deleted successfully.");

 } catch (Exception e) {
     return ResponseBuilder.error(
             "Unable to delete event: " + e.getMessage());
 }
}
    // ==========================================
    // 5. Test API
    // ==========================================

    @Operation(
        security = @SecurityRequirement(name = "oauth2")
    )
    @GetMapping("/test")
    public String test() {

        return "OAuth token working!";
    }

    // ==========================================
    // DTO Records
    // ==========================================

    public record RegisterRequest(

            @NotBlank
            String name,

            @NotBlank
            @Email
            String email,

            @NotBlank
            @Size(min = 8, max = 72)
            String password

    ) {}

    public record AccountStatusRequest(

            @NotNull
            AccountStatus status

    ) {}
}
