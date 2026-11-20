package com.example.googleoauth.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

import org.springframework.stereotype.Service;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;

@Service
public class GoogleCalendarService {

    private static final String APPLICATION_NAME = "Google OAuth Login";

    private Calendar getCalendarService(String accessToken) throws Exception {

        GoogleCredential credential = new GoogleCredential()
                .setAccessToken(accessToken);

        return new Calendar.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                credential
        )
        .setApplicationName(APPLICATION_NAME)
        .build();
    }

    // 1. Create Event
    public Event createEvent(String accessToken, Event event) throws IOException {

        try {
            Calendar calendar = getCalendarService(accessToken);

            return calendar.events()
                    .insert("primary", event)
                    .execute();

        } catch (Exception e) {
            throw new IOException("Unable to create event: " + e.getMessage(), e);
        }
    }

    // 2. Get All Events
    public List<Event> getAllEvents(String accessToken) throws IOException {

        try {
            Calendar calendar = getCalendarService(accessToken);

            Events events = calendar.events()
                    .list("primary")
                    .setTimeMin(new com.google.api.client.util.DateTime(
                            Instant.now().toEpochMilli()))
                    .setMaxResults(100)
                    .setSingleEvents(true)
                    .setOrderBy("startTime")
                    .execute();

            return events.getItems();

        } catch (Exception e) {
            throw new IOException("Unable to fetch events: " + e.getMessage(), e);
        }
    }

    // 3. Update Event
    public Event updateEvent(
            String accessToken,
            String eventId,
            Event updatedEvent) throws IOException {

        try {
            Calendar calendar = getCalendarService(accessToken);

            return calendar.events()
                    .update("primary", eventId, updatedEvent)
                    .execute();

        } catch (Exception e) {
            throw new IOException("Unable to update event: " + e.getMessage(), e);
        }
    }

    // 4. Delete Event
    public void deleteEvent(String accessToken, String eventId) throws IOException {

        try {
            Calendar calendar = getCalendarService(accessToken);

            calendar.events()
                    .delete("primary", eventId)
                    .execute();

        } catch (Exception e) {
            throw new IOException("Unable to delete event: " + e.getMessage(), e);
        }
    }
}