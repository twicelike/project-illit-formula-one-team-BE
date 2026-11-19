package project.illit.smedia.post.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;
import project.illit.smedia.common.exception.AppException;
import project.illit.smedia.common.exception.BadRequestException;
import project.illit.smedia.post.dto.MediaUploadResponse;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class SupabaseMediaStorageService implements MediaStorageService {

    private static final Map<String, String> ALLOWED_MIME_TO_EXT = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "image/gif", "gif",
            "video/mp4", "mp4",
            "video/webm", "webm"
    );

    private static final Set<String> ALLOWED_FOLDERS = Set.of(
            "posts",
            "avatars",
            "banners",
            "fan-letters"
    );

    private final String supabaseUrl;
    private final String serviceRoleKey;
    private final String bucket;
    private final long maxFileSizeBytes;
    private final RestClient restClient;

    public SupabaseMediaStorageService(
            @Value("${app.storage.supabase.url:}") String supabaseUrl,
            @Value("${app.storage.supabase.service-role-key:}") String serviceRoleKey,
            @Value("${app.storage.supabase.bucket:smedia-media}") String bucket,
            @Value("${app.storage.max-file-size-bytes:10485760}") long maxFileSizeBytes
    ) {
        this.supabaseUrl = supabaseUrl != null ? supabaseUrl.trim().replaceAll("/+$", "") : "";
        this.serviceRoleKey = serviceRoleKey != null ? serviceRoleKey.trim() : "";
        this.bucket = (bucket != null && !bucket.isBlank()) ? bucket.trim() : "smedia-media";
        this.maxFileSizeBytes = maxFileSizeBytes;
        this.restClient = RestClient.create();
    }

    @Override
    public MediaUploadResponse upload(MultipartFile file, String folder, Integer uploaderUserId) {
        validateFile(file);
        String sanitizedFolder = sanitizeFolder(folder);

        if (supabaseUrl.isBlank() || serviceRoleKey.isBlank()) {
            throw new BadRequestException(
                    "Supabase Storage is not configured yet. Set app.storage.supabase.url and service-role-key in application-dev.yaml, or attach external media URLs directly."
            );
        }

        String contentType = file.getContentType().toLowerCase();
        String ext = ALLOWED_MIME_TO_EXT.get(contentType);
        String fileName = UUID.randomUUID() + "." + ext;
        String objectPath = sanitizedFolder + "/" + uploaderUserId + "/" + fileName;

        byte[] payload;
        try {
            payload = file.getBytes();
        } catch (IOException ex) {
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read uploaded file bytes");
        }

        String uploadEndpoint = supabaseUrl + "/storage/v1/object/" + bucket + "/" + objectPath;
        try {
            restClient.post()
                    .uri(uploadEndpoint)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + serviceRoleKey)
                    .header("apikey", serviceRoleKey)
                    .header("x-upsert", "false")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            throw new AppException(HttpStatus.BAD_GATEWAY, "Failed to upload media to Supabase Storage");
        }

        String publicUrl = supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + objectPath;
        return new MediaUploadResponse(publicUrl, fileName, contentType, file.getSize(), bucket);
    }

    void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded file must not be empty");
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new BadRequestException("File exceeds maximum allowed size of " + (maxFileSizeBytes / (1024 * 1024)) + " MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TO_EXT.containsKey(contentType.toLowerCase())) {
            throw new BadRequestException(
                    "Unsupported file type. Allowed types: JPEG, PNG, WEBP, GIF, MP4, WEBM"
            );
        }
    }

    String sanitizeFolder(String folder) {
        if (folder == null || folder.isBlank()) {
            return "posts";
        }
        String normalized = folder.trim().toLowerCase();
        if (!ALLOWED_FOLDERS.contains(normalized)) {
            throw new BadRequestException("Invalid upload folder. Allowed values: posts, avatars, banners, fan-letters");
        }
        return normalized;
    }
}
