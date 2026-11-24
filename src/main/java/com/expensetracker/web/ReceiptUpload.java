package com.expensetracker.web;

import com.expensetracker.service.ReceiptFileValidator;
import com.expensetracker.service.ServiceException;
import jakarta.servlet.http.Part;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;

final class ReceiptUpload implements AutoCloseable {
    static final int MAX_FILE_BYTES = 20 * 1024 * 1024;
    private final Path path;

    private ReceiptUpload(Path path) {
        this.path = path;
    }

    static ReceiptUpload read(Part part, long maximum) throws IOException {
        if (part == null || part.getSubmittedFileName() == null || part.getSubmittedFileName().isBlank()) {
            return new ReceiptUpload(null);
        }
        String name = part.getSubmittedFileName().toLowerCase(Locale.ROOT);
        int dot = name.lastIndexOf('.');
        String extension = dot < 0 ? "" : name.substring(dot + 1);
        if (!Set.of("png", "jpg", "jpeg").contains(extension)) {
            throw new ServiceException("Choose a JPG or PNG receipt image.");
        }
        Path file = Files.createTempFile("expense-receipt-", "." + extension);
        try (var input = part.getInputStream()) {
            Files.copy(input, file, StandardCopyOption.REPLACE_EXISTING);
            ReceiptFileValidator.validate(file, Math.min(maximum, MAX_FILE_BYTES));
            return new ReceiptUpload(file);
        } catch (RuntimeException | IOException exception) {
            Files.deleteIfExists(file);
            throw exception;
        }
    }

    Path path() {
        return path;
    }

    void validateDimensions() {
        try (var source = ImageIO.createImageInputStream(path.toFile())) {
            if (source == null) {
                throw new ServiceException("The receipt image could not be decoded.");
            }
            var readers = ImageIO.getImageReaders(source);
            if (!readers.hasNext()) {
                throw new ServiceException("The receipt image could not be decoded.");
            }
            var reader = readers.next();
            try {
                reader.setInput(source);
                if ((long) reader.getWidth(0) * reader.getHeight(0) > 24_000_000) {
                    throw new ServiceException("Use a receipt image smaller than 24 megapixels for scanning.");
                }
            } finally {
                reader.dispose();
            }
        } catch (IOException exception) {
            throw new ServiceException("The receipt image could not be decoded.", exception);
        }
    }

    @Override
    public void close() throws IOException {
        if (path != null) {
            Files.deleteIfExists(path);
        }
    }
}
