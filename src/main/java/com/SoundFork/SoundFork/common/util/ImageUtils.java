package com.SoundFork.SoundFork.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

@Slf4j
public class ImageUtils {

    private ImageUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    private static final Set<String> ALLOWED_FORMATS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final int SQUARE_SIZE = 400;
    private static final int AVATAR_MAX_SIZE = 500;

    public static String saveAvatar(MultipartFile file, Path targetDir) {
        String format = validateAndExtractFormat(file);

        try {
            BufferedImage original = ImageIO.read(file.getInputStream());
            if (original == null) {
                throw new IllegalArgumentException("Could not read image");
            }

            int w = original.getWidth();
            int h = original.getHeight();
            if (w > AVATAR_MAX_SIZE || h > AVATAR_MAX_SIZE) {
                double scale = Math.min((double) AVATAR_MAX_SIZE / w, (double) AVATAR_MAX_SIZE / h);
                w = (int) Math.round(w * scale);
                h = (int) Math.round(h * scale);
            }

            BufferedImage resized = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = createGraphics(resized);
            try {
                g.drawImage(original, 0, 0, w, h, null);
            } finally {
                g.dispose();
            }

            Files.createDirectories(targetDir);

            String fileName = UUID.randomUUID() + "." + format;
            Path targetPath = targetDir.resolve(fileName);
            ImageIO.write(resized, format, targetPath.toFile());

            log.info("Avatar saved: {}", targetPath);
            return targetPath.toString().replace("\\", "/");
        } catch (IOException e) {
            throw new RuntimeException("Error processing image: " + e.getMessage(), e);
        }
    }

    public static String saveSquareCover(MultipartFile file, Path targetDir) {
        String format = validateAndExtractFormat(file);

        try {
            BufferedImage original = ImageIO.read(file.getInputStream());
            if (original == null) {
                throw new IllegalArgumentException("Could not read image");
            }

            int size = Math.min(original.getWidth(), original.getHeight());
            int x = (original.getWidth() - size) / 2;
            int y = (original.getHeight() - size) / 2;
            BufferedImage cropped = original.getSubimage(x, y, size, size);

            BufferedImage resized = new BufferedImage(SQUARE_SIZE, SQUARE_SIZE, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = createGraphics(resized);
            try {
                g.drawImage(cropped, 0, 0, SQUARE_SIZE, SQUARE_SIZE, null);
            } finally {
                g.dispose();
            }

            Files.createDirectories(targetDir);

            String fileName = UUID.randomUUID() + "." + format;
            Path targetPath = targetDir.resolve(fileName);
            ImageIO.write(resized, format, targetPath.toFile());

            log.info("Cover saved: {}", targetPath);
            return targetPath.toString().replace("\\", "/");
        } catch (IOException e) {
            throw new RuntimeException("Error processing image: " + e.getMessage(), e);
        }
    }

    private static String validateAndExtractFormat(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file is empty");
        }

        String originalFilename = file.getOriginalFilename();
        String format = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            format = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        }
        if (!ALLOWED_FORMATS.contains(format)) {
            throw new IllegalArgumentException("Unsupported image format: " + format +
                    ". Allowed formats: " + String.join(", ", ALLOWED_FORMATS));
        }
        return format;
    }

    private static Graphics2D createGraphics(BufferedImage image) {
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        return g;
    }
}
