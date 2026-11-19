package project.illit.smedia.fandom.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import project.illit.smedia.common.dto.PageResponse;
import project.illit.smedia.common.exception.BadRequestException;
import project.illit.smedia.common.exception.ConflictException;
import project.illit.smedia.common.exception.ResourceNotFoundException;
import project.illit.smedia.fandom.dto.CreateFandomRequest;
import project.illit.smedia.fandom.dto.FandomDetailResponse;
import project.illit.smedia.fandom.dto.FandomMemberResponse;
import project.illit.smedia.fandom.dto.FandomSummaryResponse;
import project.illit.smedia.fandom.entity.Company;
import project.illit.smedia.fandom.entity.FandomMember;
import project.illit.smedia.fandom.entity.OfficialFandom;
import project.illit.smedia.fandom.repository.FandomMemberRepository;
import project.illit.smedia.fandom.repository.OfficialFandomRepository;
import project.illit.smedia.user.entity.ArtistProfile;
import project.illit.smedia.user.entity.MemberProfile;
import project.illit.smedia.user.entity.Role;
import project.illit.smedia.user.entity.User;
import project.illit.smedia.user.repository.ArtistProfileRepository;
import project.illit.smedia.user.repository.MemberProfileRepository;
import project.illit.smedia.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FandomServiceTest {

    @Mock
    private OfficialFandomRepository fandomRepository;
    @Mock
    private FandomMemberRepository fandomMemberRepository;
    @Mock
    private CompanyService companyService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MemberProfileRepository memberProfileRepository;
    @Mock
    private ArtistProfileRepository artistProfileRepository;

    private FandomService fandomService;
    private Company belift;
    private OfficialFandom illitFandom;

    @BeforeEach
    void setUp() {
        fandomService = new FandomService(
                fandomRepository,
                fandomMemberRepository,
                companyService,
                userRepository,
                memberProfileRepository,
                artistProfileRepository
        );
        belift = Company.builder().id(1).name("BELIFT LAB").bio("HYBE Label").build();
        illitFandom = OfficialFandom.builder()
                .id(1)
                .company(belift)
                .name("ILLIT")
                .status("ACTIVE")
                .bannerUrl("https://cdn.example.com/illit.jpg")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void discoverFandoms_batchLoadsCountsAndUserMembershipStatus() {
        PageRequest pageable = PageRequest.of(0, 10);
        Page<OfficialFandom> page = new PageImpl<>(List.of(illitFandom), pageable, 1);
        FandomMember membership = FandomMember.builder().id(3L).fandom(illitFandom).role("VIP_MEMBER").build();

        when(fandomRepository.findByStatusIgnoreCase("ACTIVE", pageable)).thenReturn(page);
        when(fandomMemberRepository.countMembersGroupedByFandomId(List.of(1)))
                .thenReturn(List.<Object[]>of(new Object[]{1, 5L}));
        when(fandomMemberRepository.countArtistsGroupedByFandomId(List.of(1)))
                .thenReturn(List.<Object[]>of(new Object[]{1, 2L}));
        when(fandomMemberRepository.findByUserIdAndFandomIdIn(5, List.of(1)))
                .thenReturn(List.of(membership));

        PageResponse<FandomSummaryResponse> result = fandomService.discoverFandoms(null, null, 5, pageable);

        assertThat(result.getItems()).hasSize(1);
        FandomSummaryResponse item = result.getItems().get(0);
        assertThat(item.name()).isEqualTo("ILLIT");
        assertThat(item.memberCount()).isEqualTo(5L);
        assertThat(item.artistCount()).isEqualTo(2L);
        assertThat(item.joined()).isTrue();
        assertThat(item.myRole()).isEqualTo("VIP_MEMBER");
    }

    @Test
    void getFandomDetail_returnsArtistLineupAndHidesInactiveFromNonAdmin() {
        User yunahUser = User.builder().id(2).name("Noh Yunah").avatar("https://cdn.example.com/yunah.jpg").build();
        ArtistProfile yunah = ArtistProfile.builder()
                .userId(2)
                .user(yunahUser)
                .stageName("YUNAH")
                .company(belift)
                .followersCount(3)
                .build();

        when(fandomRepository.findWithCompanyById(1)).thenReturn(Optional.of(illitFandom));
        when(fandomMemberRepository.countByFandom_Id(1)).thenReturn(5L);
        when(fandomMemberRepository.findArtistUserIdsByFandomId(1)).thenReturn(List.of(2));
        when(artistProfileRepository.findWithDetailsByUserIdInOrderByStageNameAsc(List.of(2)))
                .thenReturn(List.of(yunah));
        when(fandomMemberRepository.findByFandom_IdAndUser_Id(1, 5)).thenReturn(Optional.empty());

        FandomDetailResponse detail = fandomService.getFandomDetail(1, 5, false);

        assertThat(detail.artists()).hasSize(1);
        assertThat(detail.artists().get(0).stageName()).isEqualTo("YUNAH");
        assertThat(detail.availableTabs()).containsExactly("FEED", "ARTIST", "FAN_LETTER", "LIVE", "MEDIA", "EVENTS");
        assertThat(detail.joined()).isFalse();

        // Inactive fandom must be hidden from non-admin
        OfficialFandom archived = OfficialFandom.builder().id(9).name("OLD").status("ARCHIVED").build();
        when(fandomRepository.findWithCompanyById(9)).thenReturn(Optional.of(archived));
        assertThatThrownBy(() -> fandomService.getFandomDetail(9, 5, false))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void joinFandom_assignsVipRoleForVipFanAndPreventsDuplicateJoin() {
        User fan = User.builder()
                .id(5)
                .name("Minh Nguyen")
                .role(Role.builder().id(3).name("FAN").build())
                .build();
        MemberProfile vipProfile = MemberProfile.builder().userId(5).membershipTier("VIP").build();

        when(fandomRepository.findWithCompanyById(1)).thenReturn(Optional.of(illitFandom));
        when(fandomMemberRepository.existsByFandom_IdAndUser_Id(1, 5)).thenReturn(false);
        when(userRepository.findById(5)).thenReturn(Optional.of(fan));
        when(memberProfileRepository.findById(5)).thenReturn(Optional.of(vipProfile));
        when(fandomMemberRepository.save(any(FandomMember.class))).thenAnswer(inv -> {
            FandomMember m = inv.getArgument(0);
            m.setId(99L);
            return m;
        });

        FandomMemberResponse response = fandomService.joinFandom(1, 5);
        assertThat(response.fandomRole()).isEqualTo("VIP_MEMBER");
        assertThat(response.verified()).isFalse();

        when(fandomMemberRepository.existsByFandom_IdAndUser_Id(1, 5)).thenReturn(true);
        assertThatThrownBy(() -> fandomService.joinFandom(1, 5))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void leaveFandom_blocksOfficialArtistFromSelfLeaving() {
        FandomMember artistMembership = FandomMember.builder().id(1L).fandom(illitFandom).role("ARTIST").build();
        when(fandomMemberRepository.findByFandom_IdAndUser_Id(1, 2)).thenReturn(Optional.of(artistMembership));

        assertThatThrownBy(() -> fandomService.leaveFandom(1, 2))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void assignArtistToFandom_rejectsNonArtistAndAssignsValidArtist() {
        User regularFan = User.builder().id(6).role(Role.builder().id(3).name("FAN").build()).build();
        when(fandomRepository.findWithCompanyById(1)).thenReturn(Optional.of(illitFandom));
        when(userRepository.findById(6)).thenReturn(Optional.of(regularFan));

        assertThatThrownBy(() -> fandomService.assignArtistToFandom(1, 6))
                .isInstanceOf(BadRequestException.class);

        User artistUser = User.builder().id(2).name("Noh Yunah").role(Role.builder().id(2).name("ARTIST").build()).build();
        when(userRepository.findById(2)).thenReturn(Optional.of(artistUser));
        when(artistProfileRepository.existsById(2)).thenReturn(true);
        when(fandomMemberRepository.findByFandom_IdAndUser_Id(1, 2)).thenReturn(Optional.empty());
        when(fandomMemberRepository.save(any(FandomMember.class))).thenAnswer(inv -> inv.getArgument(0));

        FandomMemberResponse assigned = fandomService.assignArtistToFandom(1, 2);
        assertThat(assigned.fandomRole()).isEqualTo("ARTIST");
        assertThat(assigned.verified()).isTrue();

        ArgumentCaptor<FandomMember> captor = ArgumentCaptor.forClass(FandomMember.class);
        verify(fandomMemberRepository).save(captor.capture());
        assertThat(captor.getValue().getRole()).isEqualTo("ARTIST");
    }

    @Test
    void createFandom_validatesStatusAndUniqueness() {
        when(fandomRepository.existsByNameIgnoreCase("ILLIT")).thenReturn(true);
        assertThatThrownBy(() -> fandomService.createFandom(new CreateFandomRequest(1, "ILLIT", null, "ACTIVE")))
                .isInstanceOf(ConflictException.class);

        when(fandomRepository.existsByNameIgnoreCase("NEWGROUP")).thenReturn(false);
        when(companyService.requireCompany(1)).thenReturn(belift);
        assertThatThrownBy(() -> fandomService.createFandom(new CreateFandomRequest(1, "NEWGROUP", null, "INVALID_STATUS")))
                .isInstanceOf(BadRequestException.class);
    }
}
