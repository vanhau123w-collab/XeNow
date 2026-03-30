package com.rental.service;

import com.rental.dto.FileResponseDTO;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface FileService {
    FileResponseDTO saveFile(MultipartFile file, String folder) throws IOException;
    void deleteFile(String fileUrl);
}
