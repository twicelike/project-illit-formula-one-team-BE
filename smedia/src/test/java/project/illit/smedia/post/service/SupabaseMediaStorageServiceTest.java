package project.illit.smedia.post.service;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import project.illit.smedia.common.exception.BadRequestException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SupabaseMediaStorageServiceTest {

    private final SupabaseMediaStorageService storageService =
            new SupabaseMediaStorageService("", "", "smedia-media", 1024);

    @Test
    void rejectsEmptyOversizedAndUnsupportedFiles() {
        MockMultipartFile empty = new MockMultipartFile("file", "a.jpg", "image/jpeg", new byte[0]);
        assertThatThrownBy(() -> storageService.validateFile(empty))
                .isInstanceOf(BadRequestException.class);

        MockMultipartFile tooBig = new MockMultipartFile("file", "big.jpg", "image/jpeg", new byte[2048]);
        assertThatThrownBy(() -> storageService.validateFile(tooBig))
                .isInstanceOf(BadRequestException.class);

        MockMultipartFile exe = new MockMultipartFile("file", "bad.exe", "application/octet-stream", new byte[10]);
        assertThatThrownBy(() -> storageService.validateFile(exe))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void sanitizesFolderAndRejectsPathTraversal() {
        assertThat(storageService.sanitizeFolder(null)).isEqualTo("posts");
        assertThat(storageService.sanitizeFolder("  AVATARS ")).isEqualTo("avatars");
        assertThatThrownBy(() -> storageService.sanitizeFolder("../secret"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void throwsClearMessageWhenSupabaseStorageIsNotConfigured() {
        MockMultipartFile validImg = new MockMultipartFile("file", "pic.png", "image/png", new byte[64]);
        assertThatThrownBy(() -> storageService.upload(validImg, "posts", 5))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Supabase Storage is not configured");
    }
}
