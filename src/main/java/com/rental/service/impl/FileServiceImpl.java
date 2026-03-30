package com.rental.service.impl;

import com.rental.dto.FileResponseDTO;
import com.rental.service.FileService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class FileServiceImpl implements FileService {

    @Value("${app.upload.base-dir:uploads}")
    private String baseDir;

    @Value("${app.upload.max-size-bytes:5242880}") // 5MB
    private Long maxSizeBytes;

    @Value("${app.upload.allowed-folders:avatars,cars}")
    private String allowedFoldersStr;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");

    @Override
    public FileResponseDTO saveFile(MultipartFile file, String folder) throws IOException {
        // 1. Validate folder whitelist
        List<String> allowedFolders = Arrays.asList(allowedFoldersStr.split(","));
        if (!allowedFolders.contains(folder)) {
            throw new IllegalArgumentException("Thư mục upload không hợp lệ. Chỉ cho phép: " + allowedFoldersStr);
        }

        // 2. Validate file name and extension
        String originalFileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        if (originalFileName.isEmpty()) {
            throw new IllegalArgumentException("Tên file không được để trống");
        }

        String extension = getFileExtension(originalFileName).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Định dạng file không được phép. Chỉ chấp nhận: " + ALLOWED_EXTENSIONS);
        }

        // 3. Validate size
        if (file.getSize() > maxSizeBytes) {
            throw new IllegalArgumentException("Dung lượng file quá lớn. Tối đa cho phép: " + (maxSizeBytes / 1024 / 1024) + "MB");
        }

        // 4. Generate sanitized and timestamped filename
        String sanitizedName = originalFileName.replaceAll("[^a-zA-Z0-9\\.\\-]", "_");
        String fileName = System.currentTimeMillis() + "_" + sanitizedName;

        // 5. Path Traversal Prevention
        Path uploadPath = Paths.get(baseDir).toAbsolutePath().normalize();
        Path targetDir = uploadPath.resolve(folder).normalize();
        
        if (!targetDir.startsWith(uploadPath)) {
            throw new SecurityException("Phát hiện hành vi truy cập thư mục bất thường (Path Traversal Detection)");
        }

        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
        }

        Path targetPath = targetDir.resolve(fileName).normalize();
        if (!targetPath.getParent().equals(targetDir)) {
             throw new SecurityException("Phát hiện hành vi truy cập file bất thường");
        }

        // 6. Write file
        Files.copy(file.getInputStream(), targetPath);

        // 7. Return Response DTO
        return FileResponseDTO.builder()
                .fileName(fileName)
                .folder(folder)
                .fileUrl("/uploads/" + folder + "/" + fileName)
                .size(file.getSize())
                .uploadedAt(Instant.now())
                .build();
    }

    @Override
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith("/uploads/")) return;
        
        try {
            // Remove leading slash and use relative path to base uploads dir
            String relativePath = fileUrl.replaceFirst("^/uploads/", "");
            Path filePath = Paths.get(baseDir).resolve(relativePath).normalize();
            
            // Check if file is inside baseDir to prevent deleting sensitive files
            if (filePath.startsWith(Paths.get(baseDir).toAbsolutePath().normalize())) {
                Files.deleteIfExists(filePath);
            }
        } catch (IOException e) {
            System.err.println("Could not delete file: " + fileUrl + " - " + e.getMessage());
        }
    }

    private String getFileExtension(String fileName) {
        int lastIndex = fileName.lastIndexOf('.');
        if (lastIndex == -1) return "";
        return fileName.substring(lastIndex + 1);
    }
}
