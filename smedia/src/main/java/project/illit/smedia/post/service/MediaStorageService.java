package project.illit.smedia.post.service;

import org.springframework.web.multipart.MultipartFile;
import project.illit.smedia.post.dto.MediaUploadResponse;

public interface MediaStorageService {

    MediaUploadResponse upload(MultipartFile file, String folder, Integer uploaderUserId);
}
