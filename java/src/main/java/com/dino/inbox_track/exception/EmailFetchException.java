package com.dino.inbox_track.exception;

import java.io.IOException;

public class EmailFetchException extends RuntimeException {

  public EmailFetchException(String message, Exception e) {
    super(message);
  }
}
