package project.illit.smedia.fandom.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.illit.smedia.common.dto.PageResponse;
import project.illit.smedia.common.exception.BadRequestException;
import project.illit.smedia.common.exception.ConflictException;
import project.illit.smedia.common.exception.ResourceNotFoundException;
import project.illit.smedia.fandom.dto.CreateFandomRequest;
import project.illit.smedia.fandom.dto.FandomDetailResponse;
import project.illit.smedia.fandom.dto.FandomMemberResponse;
import project.illit.smedia.fandom.dto.FandomSummaryResponse;
import project.illit.smedia.fandom.dto.MyCommunityResponse;
import project.illit.smedia.fandom.dto.UpdateFandomRequest;
import project.illit.smedia.fandom.entity.Company;
import project.illit.smedia.fandom.entity.FandomMember;
import project.illit.smedia.fandom.entity.FandomMemberRole;
import project.illit.smedia.fandom.entity.FandomStatus;
import project.illit.smedia.fandom.entity.OfficialFandom;
import project.illit.smedia.fandom.repository.FandomMemberRepository;
import project.illit.smedia.fandom.repository.OfficialFandomRepository;
import project.illit.smedia.user.dto.ArtistProfileResponse;
import project.illit.smedia.user.entity.MemberProfile;
import project.illit.smedia.user.entity.RoleName;
import project.illit.smedia.user.entity.User;
import project.illit.smedia.user.repository.ArtistProfileRepository;
import project.illit.smedia.user.repository.MemberProfileRepository;
import project.illit.smedia.user.repository.UserRepository;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class FandomService {

    private final OfficialFandomRepository fandomRepository;
    private final FandomMemberRepository fandomMemberRepository;
    private final CompanyService companyService;
    private final UserRepository userRepository;
    private final MemberProfileRepository memberProfileRepository;
    private final ArtistProfileRepository artistProfileRepository;

    public FandomService(
            OfficialFandomRepository fandomRepository,
            FandomMemberRepository fandomMemberRepository,
            CompanyService companyService,
            UserRepository userRepository,
            MemberProfileRepository memberProfileRepository,
            ArtistProfileRepository artistProfileRepository
    ) {
        this.fandomRepository = fandomRepository;
        this.fandomMemberRepository = fandomMemberRepository;
        this.companyService = companyService;
        this.userRepository = userRepository;
        this.memberProfileRepository = memberProfileRepository;
        this.artistProfileRepository = artistProfileRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<FandomSummaryResponse> discoverFandoms(
            Integer companyId,
            String keyword,
            Integer currentUserId,
            Pageable pageable
    ) {
        Page<OfficialFandom> page = queryFandoms(FandomStatus.ACTIVE.name(), companyId, keyword, pageable);
        return buildSummaryPage(page, currentUserId);
    }

    @Transactional(readOnly = true)
    public PageResponse<FandomSummaryResponse> listFandomsForAdmin(
            String status,
            Integer companyId,
            String keyword,
            Pageable pageable
    ) {
        String normalizedStatus = (status != null && !status.isBlank())
                ? FandomStatus.fromString(status).name()
                : null;
        Page<OfficialFandom> page = queryFandoms(normalizedStatus, companyId, keyword, pageable);
        return buildSummaryPage(page, null);
    }

    @Transactional(readOnly = true)
    public FandomDetailResponse getFandomDetail(Integer fandomId, Integer currentUserId, boolean includeInactive) {
        OfficialFandom fandom = requireFandom(fandomId);
        if (!includeInactive && !FandomStatus.ACTIVE.name().equalsIgnoreCase(fandom.getStatus())) {
            throw new ResourceNotFoundException("Fandom not found with id: " + fandomId);
        }

        long memberCount = fandomMemberRepository.countByFandom_Id(fandomId);
        List<ArtistProfileResponse> artists = loadArtistLineup(fandomId);
        String myRole = resolveMyRole(fandomId, currentUserId);

        return FandomDetailResponse.from(fandom, memberCount, artists, myRole);
    }

    @Transactional(readOnly = true)
    public List<ArtistProfileResponse> getFandomArtists(Integer fandomId, boolean includeInactive) {
        OfficialFandom fandom = requireFandom(fandomId);
        if (!includeInactive && !FandomStatus.ACTIVE.name().equalsIgnoreCase(fandom.getStatus())) {
            throw new ResourceNotFoundException("Fandom not found with id: " + fandomId);
        }
        return loadArtistLineup(fandomId);
    }

    @Transactional(readOnly = true)
    public PageResponse<MyCommunityResponse> getMyCommunities(Integer userId, Pageable pageable) {
        Page<FandomMember> memberships = fandomMemberRepository
                .findByUser_IdAndFandom_StatusIgnoreCaseOrderByJoinedAtDesc(
                        userId,
                        FandomStatus.ACTIVE.name(),
                        pageable
                );

        List<Integer> fandomIds = memberships.getContent().stream()
                .map(m -> m.getFandom().getId())
                .toList();

        Map<Integer, Long> memberCounts = loadMemberCounts(fandomIds);
        Map<Integer, Long> artistCounts = loadArtistCounts(fandomIds);

        Page<MyCommunityResponse> mapped = memberships.map(m -> {
            Integer fid = m.getFandom().getId();
            return MyCommunityResponse.from(
                    m,
                    memberCounts.getOrDefault(fid, 0L),
                    artistCounts.getOrDefault(fid, 0L)
            );
        });
        return PageResponse.from(mapped);
    }

    @Transactional(readOnly = true)
    public PageResponse<FandomMemberResponse> listFandomMembers(
            Integer fandomId,
            String roleFilter,
            boolean includeInactive,
            Pageable pageable
    ) {
        OfficialFandom fandom = requireFandom(fandomId);
        if (!includeInactive && !FandomStatus.ACTIVE.name().equalsIgnoreCase(fandom.getStatus())) {
            throw new ResourceNotFoundException("Fandom not found with id: " + fandomId);
        }

        Page<FandomMember> page;
        if (roleFilter != null && !roleFilter.isBlank()) {
            String normalizedRole = FandomMemberRole.fromString(roleFilter).name();
            page = fandomMemberRepository.findByFandom_IdAndRoleIgnoreCase(fandomId, normalizedRole, pageable);
        } else {
            page = fandomMemberRepository.findByFandom_Id(fandomId, pageable);
        }

        return PageResponse.from(page.map(FandomMemberResponse::from));
    }

    @Transactional
    public FandomMemberResponse joinFandom(Integer fandomId, Integer userId) {
        OfficialFandom fandom = requireFandom(fandomId);
        if (!FandomStatus.ACTIVE.name().equalsIgnoreCase(fandom.getStatus())) {
            throw new BadRequestException("Cannot join a fandom that is not active");
        }
        if (fandomMemberRepository.existsByFandom_IdAndUser_Id(fandomId, userId)) {
            throw new ConflictException("You have already joined this fandom");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        String initialRole = resolveInitialJoinRole(userId);

        FandomMember membership = FandomMember.builder()
                .fandom(fandom)
                .user(user)
                .role(initialRole)
                .build();

        return FandomMemberResponse.from(fandomMemberRepository.save(membership));
    }

    @Transactional
    public void leaveFandom(Integer fandomId, Integer userId) {
        FandomMember membership = fandomMemberRepository.findByFandom_IdAndUser_Id(fandomId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("You are not a member of this fandom"));

        if (FandomMemberRole.ARTIST.name().equalsIgnoreCase(membership.getRole())) {
            throw new BadRequestException("Official artists cannot self-leave their fandom; please contact an administrator");
        }

        fandomMemberRepository.delete(membership);
    }

    @Transactional
    public FandomSummaryResponse createFandom(CreateFandomRequest request) {
        String cleanName = request.name().trim();
        if (fandomRepository.existsByNameIgnoreCase(cleanName)) {
            throw new ConflictException("Fandom already exists with name: " + cleanName);
        }

        Company company = companyService.requireCompany(request.companyId());
        FandomStatus status = FandomStatus.fromString(request.status());

        OfficialFandom entity = OfficialFandom.builder()
                .company(company)
                .name(cleanName)
                .bannerUrl(request.bannerUrl() != null ? request.bannerUrl().trim() : null)
                .status(status.name())
                .build();

        OfficialFandom saved = fandomRepository.save(entity);
        return FandomSummaryResponse.from(saved, 0L, 0L, null);
    }

    @Transactional
    public FandomSummaryResponse updateFandom(Integer fandomId, UpdateFandomRequest request) {
        OfficialFandom fandom = requireFandom(fandomId);
        String cleanName = request.name().trim();

        fandomRepository.findByNameIgnoreCase(cleanName)
                .filter(existing -> !existing.getId().equals(fandomId))
                .ifPresent(duplicate -> {
                    throw new ConflictException("Fandom already exists with name: " + cleanName);
                });

        Company company = companyService.requireCompany(request.companyId());
        FandomStatus status = FandomStatus.fromString(request.status());

        fandom.setCompany(company);
        fandom.setName(cleanName);
        fandom.setBannerUrl(request.bannerUrl() != null ? request.bannerUrl().trim() : null);
        fandom.setStatus(status.name());

        OfficialFandom saved = fandomRepository.save(fandom);
        long memberCount = fandomMemberRepository.countByFandom_Id(fandomId);
        long artistCount = fandomMemberRepository.countByFandom_IdAndRoleIgnoreCase(fandomId, FandomMemberRole.ARTIST.name());
        return FandomSummaryResponse.from(saved, memberCount, artistCount, null);
    }

    @Transactional
    public FandomSummaryResponse updateFandomStatus(Integer fandomId, String statusValue) {
        OfficialFandom fandom = requireFandom(fandomId);
        FandomStatus status = FandomStatus.fromString(statusValue);
        fandom.setStatus(status.name());

        OfficialFandom saved = fandomRepository.save(fandom);
        long memberCount = fandomMemberRepository.countByFandom_Id(fandomId);
        long artistCount = fandomMemberRepository.countByFandom_IdAndRoleIgnoreCase(fandomId, FandomMemberRole.ARTIST.name());
        return FandomSummaryResponse.from(saved, memberCount, artistCount, null);
    }

    @Transactional
    public void deleteFandom(Integer fandomId) {
        fandomRepository.delete(requireFandom(fandomId));
    }

    @Transactional
    public FandomMemberResponse assignArtistToFandom(Integer fandomId, Integer artistUserId) {
        OfficialFandom fandom = requireFandom(fandomId);
        User artistUser = userRepository.findById(artistUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + artistUserId));

        boolean hasArtistSystemRole = artistUser.getRole() != null
                && RoleName.ARTIST.name().equalsIgnoreCase(artistUser.getRole().getName());
        if (!hasArtistSystemRole || !artistProfileRepository.existsById(artistUserId)) {
            throw new BadRequestException("Target user is not a registered artist account");
        }

        Optional<FandomMember> existingOpt = fandomMemberRepository.findByFandom_IdAndUser_Id(fandomId, artistUserId);
        if (existingOpt.isPresent()) {
            FandomMember existing = existingOpt.get();
            if (FandomMemberRole.ARTIST.name().equalsIgnoreCase(existing.getRole())) {
                throw new ConflictException("Artist is already assigned to this fandom");
            }
            existing.setRole(FandomMemberRole.ARTIST.name());
            return FandomMemberResponse.from(fandomMemberRepository.save(existing));
        }

        FandomMember membership = FandomMember.builder()
                .fandom(fandom)
                .user(artistUser)
                .role(FandomMemberRole.ARTIST.name())
                .build();
        return FandomMemberResponse.from(fandomMemberRepository.save(membership));
    }

    @Transactional
    public void removeArtistFromFandom(Integer fandomId, Integer artistUserId) {
        FandomMember membership = fandomMemberRepository.findByFandom_IdAndUser_Id(fandomId, artistUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Artist membership not found in this fandom"));

        if (!FandomMemberRole.ARTIST.name().equalsIgnoreCase(membership.getRole())) {
            throw new BadRequestException("Target user does not hold ARTIST role in this fandom");
        }

        fandomMemberRepository.delete(membership);
    }

    @Transactional
    public FandomMemberResponse updateMemberRole(Integer fandomId, Integer targetUserId, String roleValue) {
        FandomMemberRole newRole = FandomMemberRole.fromString(roleValue);
        FandomMember membership = fandomMemberRepository.findByFandom_IdAndUser_Id(fandomId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found in fandom"));

        if (newRole == FandomMemberRole.ARTIST && !artistProfileRepository.existsById(targetUserId)) {
            throw new BadRequestException("Cannot assign ARTIST fandom role to a user without an artist profile");
        }

        membership.setRole(newRole.name());
        return FandomMemberResponse.from(fandomMemberRepository.save(membership));
    }

    @Transactional
    public void removeMember(Integer fandomId, Integer targetUserId) {
        FandomMember membership = fandomMemberRepository.findByFandom_IdAndUser_Id(fandomId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found in fandom"));
        fandomMemberRepository.delete(membership);
    }

    public OfficialFandom requireFandom(Integer fandomId) {
        return fandomRepository.findWithCompanyById(fandomId)
                .orElseThrow(() -> new ResourceNotFoundException("Fandom not found with id: " + fandomId));
    }

    private Page<OfficialFandom> queryFandoms(String status, Integer companyId, String keyword, Pageable pageable) {
        boolean hasStatus = status != null && !status.isBlank();
        boolean hasCompany = companyId != null;
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        String trimmedKeyword = hasKeyword ? keyword.trim() : null;

        if (hasStatus && hasCompany && hasKeyword) {
            return fandomRepository.findByStatusIgnoreCaseAndCompany_IdAndNameContainingIgnoreCase(
                    status, companyId, trimmedKeyword, pageable
            );
        }
        if (hasStatus && hasCompany) {
            return fandomRepository.findByStatusIgnoreCaseAndCompany_Id(status, companyId, pageable);
        }
        if (hasStatus && hasKeyword) {
            return fandomRepository.findByStatusIgnoreCaseAndNameContainingIgnoreCase(status, trimmedKeyword, pageable);
        }
        if (hasCompany && hasKeyword) {
            return fandomRepository.findByCompany_IdAndNameContainingIgnoreCase(companyId, trimmedKeyword, pageable);
        }
        if (hasStatus) {
            return fandomRepository.findByStatusIgnoreCase(status, pageable);
        }
        if (hasCompany) {
            return fandomRepository.findByCompany_Id(companyId, pageable);
        }
        if (hasKeyword) {
            return fandomRepository.findByNameContainingIgnoreCase(trimmedKeyword, pageable);
        }
        return fandomRepository.findAll(pageable);
    }

    private PageResponse<FandomSummaryResponse> buildSummaryPage(Page<OfficialFandom> page, Integer currentUserId) {
        List<Integer> fandomIds = page.getContent().stream()
                .map(OfficialFandom::getId)
                .toList();

        Map<Integer, Long> memberCounts = loadMemberCounts(fandomIds);
        Map<Integer, Long> artistCounts = loadArtistCounts(fandomIds);
        Map<Integer, String> userRoleByFandom = loadUserRoles(currentUserId, fandomIds);

        Page<FandomSummaryResponse> mapped = page.map(fandom -> {
            Integer fid = fandom.getId();
            return FandomSummaryResponse.from(
                    fandom,
                    memberCounts.getOrDefault(fid, 0L),
                    artistCounts.getOrDefault(fid, 0L),
                    userRoleByFandom.get(fid)
            );
        });
        return PageResponse.from(mapped);
    }

    private List<ArtistProfileResponse> loadArtistLineup(Integer fandomId) {
        List<Integer> artistUserIds = fandomMemberRepository.findArtistUserIdsByFandomId(fandomId);
        if (artistUserIds.isEmpty()) {
            return List.of();
        }
        return artistProfileRepository.findWithDetailsByUserIdInOrderByStageNameAsc(artistUserIds).stream()
                .map(ArtistProfileResponse::from)
                .toList();
    }

    private String resolveMyRole(Integer fandomId, Integer currentUserId) {
        if (currentUserId == null) {
            return null;
        }
        return fandomMemberRepository.findByFandom_IdAndUser_Id(fandomId, currentUserId)
                .map(FandomMember::getRole)
                .orElse(null);
    }

    private String resolveInitialJoinRole(Integer userId) {
        return memberProfileRepository.findById(userId)
                .map(MemberProfile::getMembershipTier)
                .filter(tier -> "VIP".equalsIgnoreCase(tier))
                .map(ignored -> FandomMemberRole.VIP_MEMBER.name())
                .orElse(FandomMemberRole.MEMBER.name());
    }

    private Map<Integer, Long> loadMemberCounts(List<Integer> fandomIds) {
        if (fandomIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Integer, Long> counts = new HashMap<>();
        for (Object[] row : fandomMemberRepository.countMembersGroupedByFandomId(fandomIds)) {
            counts.put((Integer) row[0], ((Number) row[1]).longValue());
        }
        return counts;
    }

    private Map<Integer, Long> loadArtistCounts(List<Integer> fandomIds) {
        if (fandomIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Integer, Long> counts = new HashMap<>();
        for (Object[] row : fandomMemberRepository.countArtistsGroupedByFandomId(fandomIds)) {
            counts.put((Integer) row[0], ((Number) row[1]).longValue());
        }
        return counts;
    }

    private Map<Integer, String> loadUserRoles(Integer currentUserId, List<Integer> fandomIds) {
        if (currentUserId == null || fandomIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Integer, String> map = new HashMap<>();
        for (FandomMember member : fandomMemberRepository.findByUserIdAndFandomIdIn(currentUserId, fandomIds)) {
            map.put(member.getFandom().getId(), member.getRole());
        }
        return map;
    }
}
