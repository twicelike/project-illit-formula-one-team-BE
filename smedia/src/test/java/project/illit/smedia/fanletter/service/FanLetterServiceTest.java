package project.illit.smedia.fanletter.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import project.illit.smedia.common.dto.PageResponse;
import project.illit.smedia.common.exception.BadRequestException;
import project.illit.smedia.common.exception.ForbiddenException;
import project.illit.smedia.fandom.entity.Company;
import project.illit.smedia.fandom.entity.OfficialFandom;
import project.illit.smedia.fandom.repository.FandomMemberRepository;
import project.illit.smedia.fandom.service.FandomService;
import project.illit.smedia.fanletter.dto.CreateFanLetterRequest;
import project.illit.smedia.fanletter.dto.FanLetterResponse;
import project.illit.smedia.fanletter.entity.FanLetter;
import project.illit.smedia.fanletter.entity.LetterTemplate;
import project.illit.smedia.fanletter.entity.StickerItem;
import project.illit.smedia.fanletter.repository.FanLetterRepository;
import project.illit.smedia.post.entity.Media;
import project.illit.smedia.post.repository.MediaRepository;
import project.illit.smedia.user.entity.ArtistProfile;
import project.illit.smedia.user.entity.Role;
import project.illit.smedia.user.entity.User;
import project.illit.smedia.user.repository.ArtistProfileRepository;
import project.illit.smedia.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FanLetterServiceTest {

    @Mock
    private FanLetterRepository fanLetterRepository;
    @Mock
    private LetterTemplateService letterTemplateService;
    @Mock
    private FandomService fandomService;
    @Mock
    private FandomMemberRepository fandomMemberRepository;
    @Mock
    private ArtistProfileRepository artistProfileRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MediaRepository mediaRepository;

    private FanLetterService fanLetterService;
    private User fan;
    private User minjuUser;
    private ArtistProfile minjuProfile;
    private LetterTemplate freeTemplate;
    private OfficialFandom illitFandom;

    @BeforeEach
    void setUp() {
        fanLetterService = new FanLetterService(
                fanLetterRepository,
                letterTemplateService,
                fandomService,
                fandomMemberRepository,
                artistProfileRepository,
                userRepository,
                mediaRepository
        );
        Company belift = Company.builder().id(1).name("BELIFT LAB").build();
        illitFandom = OfficialFandom.builder().id(1).name("ILLIT").company(belift).status("ACTIVE").build();
        fan = User.builder().id(5).name("Minh Nguyen").role(Role.builder().id(3).name("FAN").build()).isActive(true).build();
        minjuUser = User.builder().id(3).name("Park Minju").role(Role.builder().id(2).name("ARTIST").build()).isActive(true).build();
        minjuProfile = ArtistProfile.builder().userId(3).user(minjuUser).stageName("MINJU").company(belift).build();
        freeTemplate = LetterTemplate.builder().id(1).imageUrl("https://cdn.example.com/tpl1.png").isPremium(false).price(0).isActive(true).build();
    }

    @Test
    void createFanLetter_savesStickersAndCreatesBackgroundMediaFromUrl() {
        when(userRepository.findById(5)).thenReturn(Optional.of(fan));
        when(artistProfileRepository.findWithDetailsByUserId(3)).thenReturn(Optional.of(minjuProfile));
        when(letterTemplateService.requireTemplate(1)).thenReturn(freeTemplate);
        when(fanLetterRepository.save(any(FanLetter.class))).thenAnswer(inv -> {
            FanLetter fl = inv.getArgument(0);
            if (fl.getId() == null) {
                fl.setId(40);
            }
            return fl;
        });
        when(mediaRepository.save(any(Media.class))).thenAnswer(inv -> {
            Media m = inv.getArgument(0);
            m.setId(500);
            return m;
        });

        StickerItem sticker = StickerItem.builder().id("bunny_01").x(120.0).y(80.0).scale(1.2).build();
        CreateFanLetterRequest request = new CreateFanLetterRequest(
                3,
                1,
                "Gửi Minju, luôn ủng hộ cậu!",
                null,
                "https://cdn.example.com/bg-pastel.jpg",
                List.of(sticker),
                true
        );

        FanLetterResponse response = fanLetterService.createFanLetter(5, request);

        assertThat(response.id()).isEqualTo(40);
        assertThat(response.recipient().stageName()).isEqualTo("MINJU");
        assertThat(response.backgroundImageId()).isEqualTo(500);
        assertThat(response.backgroundImage().mediaUrl()).isEqualTo("https://cdn.example.com/bg-pastel.jpg");
        assertThat(response.stickerList()).hasSize(1);
        assertThat(response.stickerList().get(0).getRotation()).isEqualTo(0.0);
        assertThat(response.isPublic()).isTrue();
    }

    @Test
    void createFanLetter_rejectsSelfSendAndPaidPremiumTemplate() {
        assertThatThrownBy(() -> fanLetterService.createFanLetter(3, new CreateFanLetterRequest(
                3, null, "Self letter", null, null, null, true
        ))).isInstanceOf(BadRequestException.class);

        LetterTemplate paidTemplate = LetterTemplate.builder().id(9).isPremium(true).price(100).isActive(true).build();
        when(userRepository.findById(5)).thenReturn(Optional.of(fan));
        when(artistProfileRepository.findWithDetailsByUserId(3)).thenReturn(Optional.of(minjuProfile));
        when(letterTemplateService.requireTemplate(9)).thenReturn(paidTemplate);

        assertThatThrownBy(() -> fanLetterService.createFanLetter(5, new CreateFanLetterRequest(
                3, 9, "Paid template letter", null, null, null, true
        ))).isInstanceOf(BadRequestException.class);
    }

    @Test
    void privateLetter_visibleOnlyToSenderRecipientAndAdmin() {
        FanLetter privateLetter = FanLetter.builder()
                .id(12)
                .user(fan)
                .artist(minjuProfile)
                .template(freeTemplate)
                .content("Secret note for Minju")
                .isPublic(false)
                .status("PUBLISHED")
                .createdAt(LocalDateTime.now())
                .build();

        when(fanLetterRepository.findWithDetailsById(12)).thenReturn(Optional.of(privateLetter));

        // Anonymous & stranger are blocked
        assertThatThrownBy(() -> fanLetterService.getLetterDetail(12, null, false))
                .isInstanceOf(ForbiddenException.class);
        assertThatThrownBy(() -> fanLetterService.getLetterDetail(12, 99, false))
                .isInstanceOf(ForbiddenException.class);

        // Sender & recipient artist can read it
        assertThat(fanLetterService.getLetterDetail(12, 5, false).content()).isEqualTo("Secret note for Minju");
        assertThat(fanLetterService.getLetterDetail(12, 3, false).content()).isEqualTo("Secret note for Minju");
    }

    @Test
    void fandomWall_validatesArtistBelongsToFandomAndBatchLoadsBackgrounds() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(fandomService.requireFandom(1)).thenReturn(illitFandom);
        when(fandomMemberRepository.findArtistUserIdsByFandomId(1)).thenReturn(List.of(2, 3));

        // Artist 4 (Chaewon) does not belong to ILLIT fandom
        assertThatThrownBy(() -> fanLetterService.getFandomPublicWall(1, 4, false, pageable))
                .isInstanceOf(BadRequestException.class);

        FanLetter letter = FanLetter.builder()
                .id(1)
                .user(fan)
                .artist(minjuProfile)
                .template(freeTemplate)
                .content("Public wall letter")
                .backgroundImageId(5)
                .isPublic(true)
                .status("PUBLISHED")
                .build();
        Media bg = Media.builder().id(5).mediaUrl("https://cdn.example.com/bg.jpg").status("ACTIVE").build();

        when(fanLetterRepository.findPublicWallByArtistIds(List.of(2, 3), true, 3, pageable))
                .thenReturn(new PageImpl<>(List.of(letter), pageable, 1));
        when(mediaRepository.findAllById(Set.of(5))).thenReturn(List.of(bg));

        PageResponse<FanLetterResponse> wall = fanLetterService.getFandomPublicWall(1, 3, false, pageable);
        assertThat(wall.getItems()).hasSize(1);
        assertThat(wall.getItems().get(0).backgroundImage().mediaUrl()).isEqualTo("https://cdn.example.com/bg.jpg");
    }

    @Test
    void deleteFanLetter_allowsRecipientArtistAndBlocksStranger() {
        FanLetter letter = FanLetter.builder()
                .id(15)
                .user(fan)
                .artist(minjuProfile)
                .status("PUBLISHED")
                .build();
        when(fanLetterRepository.findWithDetailsById(15)).thenReturn(Optional.of(letter));

        assertThatThrownBy(() -> fanLetterService.deleteFanLetter(15, 99, false))
                .isInstanceOf(ForbiddenException.class);

        // Recipient artist can soft-delete
        fanLetterService.deleteFanLetter(15, 3, false);
        assertThat(letter.getStatus()).isEqualTo("DELETED");
        verify(mediaRepository).softDeleteActiveByTarget("FAN_LETTER", 15);
    }
}
