package com.corems.documentms.app.util;

import com.corems.documentms.app.config.DocumentConfig;
import com.corems.documentms.app.model.DocumentStreamResult;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Utility to build streaming HTTP responses for document content.
 */
public final class StreamResponseHelper {

    private static final ExecutorService copierExecutor = Executors.newCachedThreadPool(new ThreadFactory() {
        private final ThreadFactory defaultFactory = Executors.defaultThreadFactory();

        @Override
        public Thread newThread(Runnable r) {
            Thread t = defaultFactory.newThread(r);
            t.setDaemon(true);
            t.setName("stream-copy-" + t.threadId());
            return t;
        }
    });

    private StreamResponseHelper() {
        // utility
    }

    public static ResponseEntity<Resource> buildStreamResponse(DocumentStreamResult streamResult,
                                                               DocumentConfig documentConfig,
                                                               String dispositionType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                streamResult.getContentType() != null ? streamResult.getContentType() : "application/octet-stream"));
        headers.setContentLength(streamResult.getSize() != null ? streamResult.getSize() : -1);
        headers.setContentDisposition(ContentDisposition.builder(dispositionType)
                .filename(streamResult.getFilename(), StandardCharsets.UTF_8)
                .build());
        headers.setCacheControl("no-cache, no-store, must-revalidate");
        headers.setPragma("no-cache");
        headers.setExpires(0);

        int bufferSize = documentConfig != null && documentConfig.getStream() != null
                ? documentConfig.getStream().getBufferSize()
                : 8192;

        // Use piped streams so we can perform the S3->pipe copy in a separate thread while
        // returning an InputStreamResource that the MVC infrastructure will read from.
        try {
            final PipedOutputStream pipedOut = new PipedOutputStream();
            final PipedInputStream pipedIn = new PipedInputStream(pipedOut, Math.max(bufferSize, 1024));

            // Start copier in background to stream data from the original InputStream into the pipe.
            startPipeCopier(streamResult, pipedOut, bufferSize);

            InputStreamResource resource = new InputStreamResource(pipedIn) {
                @Override
                public long contentLength() {
                    // unknown size when streaming, let framework handle
                    return -1;
                }
            };

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);
        } catch (IOException e) {
            // If piped streams cannot be created, fall back to direct InputStreamResource (non-buffered)
            InputStreamResource resource = new InputStreamResource(new BufferedInputStream(streamResult.getStream(), bufferSize));
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);
        }
    }

    // Extracted copier logic into a dedicated method for clarity and testability.
    private static void startPipeCopier(DocumentStreamResult streamResult, PipedOutputStream pipedOut, int bufferSize) {
        copierExecutor.submit(() -> {
            try (InputStream in = new BufferedInputStream(streamResult.getStream(), bufferSize);
                 PipedOutputStream out = pipedOut) {
                byte[] buf = new byte[bufferSize];
                int read;
                while ((read = in.read(buf)) != -1) {
                    try {
                        out.write(buf, 0, read);
                        out.flush();
                    } catch (IOException writeEx) {
                        // client likely disconnected; stop copying
                        break;
                    }
                }
            } catch (IOException ex) {
                // best-effort: there's nowhere to propagate; the reading side will get EOF/exception
            }
        });
    }

    // Should be called on application shutdown to try to terminate copier threads gracefully.
    public static void shutdown() {
        copierExecutor.shutdown();
        try {
            if (!copierExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
                copierExecutor.shutdownNow();
            }
        } catch (InterruptedException ignored) {
            copierExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
