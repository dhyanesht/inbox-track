package com.dino.inbox_track.client;

import com.dino.inbox_track.config.GmailProperties;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;


@Component
@Slf4j
@EnableConfigurationProperties(GmailProperties.class)
public class GmailServiceFactory {

    private static final List<String> SCOPES = Collections.singletonList(GmailScopes.GMAIL_READONLY);
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String APPLICATION_NAME = "Inbox Track Spring Boot";

    private Gmail gmailService;
    private final GmailProperties gmailProperties;

    public GmailServiceFactory(GmailProperties gmailProperties) {
        this.gmailProperties = gmailProperties;
    }


    @Bean
    public Gmail getService() throws IOException, GeneralSecurityException {
        if (gmailService == null) {
            gmailService = buildService();
        }
        return gmailService;
    }

    private Gmail buildService() throws IOException, GeneralSecurityException {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        gmailService = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT)).setApplicationName(
            APPLICATION_NAME).build();
        return gmailService;
    }

    private Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets from file
        File credentialsFile = new File(gmailProperties.credentialsPath());
        if (!credentialsFile.exists()) {
            log.error("Credentials file not found at {}. Please add your Gmail credentials.", credentialsFile.getAbsolutePath());
            // Optionally create placeholder
            credentialsFile.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(credentialsFile)) {
                writer.write("Credentials file not found. Place your credentials.json here.");
            }
            throw new IllegalStateException("Gmail credentials are missing, check the placeholder file.");
        }
        InputStream in = new FileInputStream(credentialsFile);
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, JSON_FACTORY, clientSecrets,
            SCOPES).setDataStoreFactory(new FileDataStoreFactory(new File(gmailProperties.tokensDirectory())))
            .setAccessType("offline")
            .build();

        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();

        try {
            log.info("Starting OAuth2 authorization flow...");
            return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
        } catch (Exception e) {
            throw new IOException("OAuth authorization failed", e);
        }
    }
}
