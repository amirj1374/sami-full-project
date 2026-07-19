package com.sami.app.files.spi;

import java.io.IOException;
import java.io.InputStream;

/**
 * A binary payload handed to storage providers and processors.
 *
 * <p>Deliberately stream-oriented: the ERP stores scanned contracts, repair
 * photo sets and video, so nothing in this module may assume the whole file
 * fits comfortably in heap. {@link #openStream()} may be called more than once
 * — implementations back it with a re-readable source (staged temp file or
 * stored object), never a consumed stream.
 */
public interface FileContent {

    InputStream openStream() throws IOException;

    long size();

    String contentType();

    String filename();
}
