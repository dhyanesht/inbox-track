package com.dino.inbox_track.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {


  // In your GlobalExceptionHandler class
  @ExceptionHandler(EmailFetchException.class)
  public ResponseEntity<String> handleEmailFetchException(EmailFetchException ex) {
    // Log the actual technical error (IOException) for the developers
    log.error("{} :", EmailFetchException.class.getName(), ex);

    // Return a user-friendly message to the API consumer
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body("Could not retrieve emails from the server.");
  }

}
