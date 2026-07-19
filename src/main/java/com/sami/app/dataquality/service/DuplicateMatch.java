package com.sami.app.dataquality.service;

/** One candidate that resembled the submitted value. */
public record DuplicateMatch(Long entityId, String value, double score) {
}
