package dev.nifties.integration.springframework.boot.actuate.profiler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.FileSystemResource;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;

/**
 * Originally, copied from Spring Boot's HeapDumpWebEndpoint, wraps File as a WriteableResource, deleting the underlying
 * file from file-system as soon as the resource is read and stream closed.
 */
final class TemporaryFileSystemResource extends FileSystemResource {
    private final Log logger = LogFactory.getLog(getClass());

    TemporaryFileSystemResource(File file) {
        super(file);
    }

    @Override
    public ReadableByteChannel readableChannel() throws IOException {
        ReadableByteChannel readableChannel = super.readableChannel();
        return new ReadableByteChannel() {

            @Override
            public boolean isOpen() {
                return readableChannel.isOpen();
            }

            @Override
            public void close() throws IOException {
                closeThenDeleteFile(readableChannel);
            }

            @Override
            public int read(ByteBuffer dst) throws IOException {
                return readableChannel.read(dst);
            }
        };
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new FilterInputStream(super.getInputStream()) {
            @Override
            public void close() throws IOException {
                closeThenDeleteFile(this.in);
            }
        };
    }

    private void closeThenDeleteFile(Closeable closeable) throws IOException {
        try {
            closeable.close();
        } finally {
            deleteFile();
        }
    }

    private void deleteFile() {
        try {
            Files.delete(getFile().toPath());
        } catch (IOException ex) {
            TemporaryFileSystemResource.this.logger
                    .warn("Failed to delete temporary file '" + getFile() + "'", ex);
        }
    }

    @Override
    public boolean isFile() {
        // Prevent zero-copy so we can delete the file on close
        return false;
    }
}
