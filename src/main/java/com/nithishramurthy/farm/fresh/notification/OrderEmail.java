package com.nithishramurthy.farm.fresh.notification;

/** A ready-to-send email: the same message as plain text and as HTML. */
public record OrderEmail(String to, String subject, String text, String html) {
}
