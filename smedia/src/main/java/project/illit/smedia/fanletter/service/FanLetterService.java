package project.illit.smedia.fanletter.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.illit.smedia.common.dto.PageResponse;
import project.illit.smedia.common.exception.BadRequestException;
import project.illit.smedia.common.exception.ForbiddenException;
import project.illit.smedia.common.exception.ResourceNotFoundException;
import project.illit.smedia.fandom.entity.FandomStatus;
import project.illit.smedia.fandom.entity.OfficialFandom;
import project.illit.smedia.fandom.repository.FandomMemberRepository;
import project.illit.smedia.fandom.service.FandomService;
import project.illit.smedia.fanletter.dto.CreateFanLetterRequest;
import project.illit.smedia.fanletter.dto.FanLetterResponse;
import project.illit.smedia.fanletter.dto.UpdateFanLetterRequest;
import project.illit.smedia.fanletter.entity.FanLetter;
import project.illit.smedia.fanletter.entity.FanLetterStatus;
import project.illit.smedia.fanletter.entity.LetterTemplate;
import project.illit.smedia.fanletter.entity.StickerItem;
import project.illit.smedia.fanletter.repository.FanLetterRepository;
import project.illit.smedia.post.entity.Media;
import project.illit.smedia.post.entity.MediaStatus;
import project.illit.smedia.post.entity.MediaTargetType;
import project.illit.smedia.post.repository.MediaRepository;
import project.illit.smedia.user.entity.ArtistProfile;
import project.illit.smedia.user.entity.User;
import project.illit.smedia.user.repository.ArtistProfileRepository;
import project.illit.smedia.user.repository.UserRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FanLetterService {

    private final FanLetterRepository fanLetterRepository;
    private final LetterTemplateService letterTemplateService;
    private final FandomService fandomService;
    private final FandomMemberRepository fandomMemberRepository;
    private final ArtistProfileRepository artistProfileRepository;
    private final UserRepository userRepository;
    private final MediaRepository mediaRepository;

    public FanLetterService(
            FanLetterRepository fanLetterRepository,
            LetterTemplateService letterTemplateService,
            FandomService fandomService,
            FandomMemberRepository fandomMemberRepository,
            ArtistProfileRepository artistProfileRepository,
            UserRepository userRepository,
            MediaRepository mediaRepository
    ) {
        this.fanLetterRepository = fanLetterRepository;
        this.letterTemplateService = letterTemplateService;
        this.fandomService = fandomService;
        this.fandomMemberRepository = fandomMemberRepository;
        this.artistProfileRepository = artistProfileRepository;
        this.userRepository = userRepository;
        this.mediaRepository = mediaRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<FanLetterResponse> getFandomPublicWall(
            Integer fandomId,
            Integer artistId,
            boolean isAdmin,
            Pageable pageable
    ) {
        OfficialFandom fandom = fandomService.requireFandom(fandomId);
        if (!isAdmin && !FandomStatus.ACTIVE.name().equalsIgnoreCase(fandom.getStatus())) {
            throw new ResourceNotFoundException("Fandom not found with id: " + fandomId);
        }

        List<Integer> fandomArtistIds = fandomMemberRepository.findArtistUserIdsByFandomId(fandomId);
        if (fandomArtistIds.isEmpty()) {
            return emptyPage(pageable);
        }

        boolean filterByArtist = artistId != null;
        if (filterByArtist && !fandomArtistIds.contains(artistId)) {
            throw new BadRequestException("Selected artist does not belong to fandom id: " + fandomId);
        }

        Page<FanLetter> page = fanLetterRepository.findPublicWallByArtistIds(
                fandomArtistIds,
                filterByArtist,
                filterByArtist ? artistId : 0,
                pageable
        );
        return enrichPage(page);
    }

    @Transactional(readOnly = true)
    public PageResponse<FanLetterResponse> getArtistPublicLetters(Integer artistUserId, Pageable pageable) {
        requireActiveArtist(artistUserId);
        Page<FanLetter> page = fanLetterRepository.findPublicLettersForArtist(artistUserId, pageable);
        return enrichPage(page);
    }

    @Transactional(readOnly = true)
    public PageResponse<FanLetterResponse> getArtistInbox(
            Integer artistUserId,
            String visibility,
            Pageable pageable
    ) {
        if (!artistProfileRepository.existsById(artistUserId)) {
            throw new ResourceNotFoundException("Artist profile not found for current user");
        }
        VisibilityFilter filter = VisibilityFilter.fromString(visibility);
        Page<FanLetter> page = fanLetterRepository.findArtistInbox(
                artistUserId,
                filter.filterByPublic,
                filter.publicValue,
                pageable
        );
        return enrichPage(page);
    }

    @Transactional(readOnly = true)
    public PageResponse<FanLetterResponse> getMySentLetters(
            Integer senderId,
            Integer artistId,
            Pageable pageable
    ) {
        boolean filterByArtist = artistId != null;
        Page<FanLetter> page = fanLetterRepository.findSentByUser(
                senderId,
                filterByArtist,
                filterByArtist ? artistId : 0,
                pageable
        );
        return enrichPage(page);
    }

    @Transactional(readOnly = true)
    public FanLetterResponse getLetterDetail(Integer letterId, Integer currentUserId, boolean isAdmin) {
        FanLetter letter = requireLetter(letterId);
        if (!FanLetterStatus.PUBLISHED.name().equalsIgnoreCase(letter.getStatus()) && !isAdmin) {
            throw new ResourceNotFoundException("Fan letter not found with id: " + letterId);
        }

        if (!Boolean.TRUE.equals(letter.getIsPublic())) {
            boolean isSender = currentUserId != null && letter.getUser() != null
                    && letter.getUser().getId().equals(currentUserId);
            boolean isRecipient = currentUserId != null && letter.getArtist() != null
                    && letter.getArtist().getUserId().equals(currentUserId);
            if (!isSender && !isRecipient && !isAdmin) {
                throw new ForbiddenException("This fan letter is private");
            }
        }

        Media bgMedia = loadSingleBackgroundMedia(letter.getBackgroundImageId());
        return FanLetterResponse.from(letter, bgMedia);
    }

    @Transactional
    public FanLetterResponse createFanLetter(Integer senderId, CreateFanLetterRequest request) {
        if (senderId.equals(request.artistId())) {
            throw new BadRequestException("You cannot send a fan letter to yourself");
        }

        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + senderId));
        ArtistProfile recipient = requireActiveArtist(request.artistId());

        LetterTemplate template = null;
        if (request.templateId() != null) {
            template = requireUsableTemplate(request.templateId());
        }

        boolean hasBgId = request.backgroundImageId() != null;
        boolean hasBgUrl = request.backgroundImageUrl() != null && !request.backgroundImageUrl().isBlank();
        if (hasBgId && hasBgUrl) {
            throw new BadRequestException("Provide either backgroundImageId or backgroundImageUrl, not both");
        }

        Media existingBgMedia = null;
        if (hasBgId) {
            existingBgMedia = requireActiveFanLetterMedia(request.backgroundImageId());
        }

        List<StickerItem> stickers = normalizeStickers(request.stickerList());
        boolean isPublic = request.isPublic() == null || request.isPublic();

        FanLetter letter = FanLetter.builder()
                .user(sender)
                .artist(recipient)
                .template(template)
                .content(request.content().trim())
                .backgroundImageId(existingBgMedia != null ? existingBgMedia.getId() : null)
                .stickerList(stickers)
                .isPublic(isPublic)
                .status(FanLetterStatus.PUBLISHED.name())
                .build();

        FanLetter saved = fanLetterRepository.save(letter);

        Media resolvedBgMedia = existingBgMedia;
        if (hasBgUrl) {
            String cleanUrl = request.backgroundImageUrl().trim();
            resolvedBgMedia = mediaRepository.save(Media.builder()
                    .targetId(saved.getId())
                    .targetType(MediaTargetType.FAN_LETTER.name())
                    .mediaUrl(cleanUrl)
                    .thumbnail(cleanUrl)
                    .status(MediaStatus.ACTIVE.name())
                    .build());
            saved.setBackgroundImageId(resolvedBgMedia.getId());
            saved = fanLetterRepository.save(saved);
        }

        return FanLetterResponse.from(saved, resolvedBgMedia);
    }

    @Transactional
    public FanLetterResponse updateFanLetter(
            Integer letterId,
            Integer currentUserId,
            UpdateFanLetterRequest request
    ) {
        FanLetter letter = requireLetter(letterId);
        if (!FanLetterStatus.PUBLISHED.name().equalsIgnoreCase(letter.getStatus())) {
            throw new BadRequestException("Only published fan letters can be edited");
        }
        if (letter.getUser() == null || !letter.getUser().getId().equals(currentUserId)) {
            throw new ForbiddenException("You can only edit your own fan letters");
        }

        if (request.templateId() != null) {
            letter.setTemplate(requireUsableTemplate(request.templateId()));
        }
        letter.setContent(request.content().trim());
        if (request.stickerList() != null) {
            letter.setStickerList(normalizeStickers(request.stickerList()));
        }
        if (request.isPublic() != null) {
            letter.setIsPublic(request.isPublic());
        }

        boolean clearBg = Boolean.TRUE.equals(request.clearBackgroundImage());
        boolean hasBgId = request.backgroundImageId() != null;
        boolean hasBgUrl = request.backgroundImageUrl() != null && !request.backgroundImageUrl().isBlank();
        if (hasBgId && hasBgUrl) {
            throw new BadRequestException("Provide either backgroundImageId or backgroundImageUrl, not both");
        }

        Media resolvedBgMedia = null;
        if (clearBg) {
            letter.setBackgroundImageId(null);
            mediaRepository.softDeleteActiveByTarget(MediaTargetType.FAN_LETTER.name(), letterId);
        } else if (hasBgId) {
            resolvedBgMedia = requireActiveFanLetterMedia(request.backgroundImageId());
            letter.setBackgroundImageId(resolvedBgMedia.getId());
        } else if (hasBgUrl) {
            mediaRepository.softDeleteActiveByTarget(MediaTargetType.FAN_LETTER.name(), letterId);
            String cleanUrl = request.backgroundImageUrl().trim();
            resolvedBgMedia = mediaRepository.save(Media.builder()
                    .targetId(letterId)
                    .targetType(MediaTargetType.FAN_LETTER.name())
                    .mediaUrl(cleanUrl)
                    .thumbnail(cleanUrl)
                    .status(MediaStatus.ACTIVE.name())
                    .build());
            letter.setBackgroundImageId(resolvedBgMedia.getId());
        } else {
            resolvedBgMedia = loadSingleBackgroundMedia(letter.getBackgroundImageId());
        }

        FanLetter updated = fanLetterRepository.save(letter);
        return FanLetterResponse.from(updated, resolvedBgMedia);
    }

    @Transactional
    public void deleteFanLetter(Integer letterId, Integer currentUserId, boolean isAdmin) {
        FanLetter letter = requireLetter(letterId);
        if (FanLetterStatus.DELETED.name().equalsIgnoreCase(letter.getStatus())) {
            throw new ResourceNotFoundException("Fan letter not found with id: " + letterId);
        }

        boolean isSender = letter.getUser() != null && letter.getUser().getId().equals(currentUserId);
        boolean isRecipient = letter.getArtist() != null && letter.getArtist().getUserId().equals(currentUserId);
        if (!isSender && !isRecipient && !isAdmin) {
            throw new ForbiddenException("You do not have permission to delete this fan letter");
        }

        letter.setStatus(FanLetterStatus.DELETED.name());
        fanLetterRepository.save(letter);
        mediaRepository.softDeleteActiveByTarget(MediaTargetType.FAN_LETTER.name(), letterId);
    }

    @Transactional(readOnly = true)
    public PageResponse<FanLetterResponse> listLettersForAdmin(
            Integer artistId,
            String status,
            Pageable pageable
    ) {
        boolean filterByArtist = artistId != null;
        boolean filterByStatus = status != null && !status.isBlank();
        String normalizedStatus = filterByStatus ? FanLetterStatus.fromString(status).name() : "";

        Page<FanLetter> page = fanLetterRepository.findAllForAdmin(
                filterByArtist,
                filterByArtist ? artistId : 0,
                filterByStatus,
                normalizedStatus,
                pageable
        );
        return enrichPage(page);
    }

    @Transactional
    public FanLetterResponse updateLetterStatus(Integer letterId, String statusValue) {
        FanLetter letter = requireLetter(letterId);
        FanLetterStatus newStatus = FanLetterStatus.fromString(statusValue);
        letter.setStatus(newStatus.name());
        FanLetter updated = fanLetterRepository.save(letter);

        if (newStatus == FanLetterStatus.DELETED) {
            mediaRepository.softDeleteActiveByTarget(MediaTargetType.FAN_LETTER.name(), letterId);
        }

        return FanLetterResponse.from(updated, loadSingleBackgroundMedia(updated.getBackgroundImageId()));
    }

    private FanLetter requireLetter(Integer letterId) {
        return fanLetterRepository.findWithDetailsById(letterId)
                .orElseThrow(() -> new ResourceNotFoundException("Fan letter not found with id: " + letterId));
    }

    private ArtistProfile requireActiveArtist(Integer artistUserId) {
        ArtistProfile artist = artistProfileRepository.findWithDetailsByUserId(artistUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Artist not found with id: " + artistUserId));
        if (artist.getUser() == null || !Boolean.TRUE.equals(artist.getUser().getIsActive())) {
            throw new ResourceNotFoundException("Artist not found with id: " + artistUserId);
        }
        return artist;
    }

    private LetterTemplate requireUsableTemplate(Integer templateId) {
        LetterTemplate template = letterTemplateService.requireTemplate(templateId);
        if (!Boolean.TRUE.equals(template.getIsActive())) {
            throw new BadRequestException("Selected letter template is no longer active");
        }
        int price = template.getPrice() != null ? template.getPrice() : 0;
        if (Boolean.TRUE.equals(template.getIsPremium()) && price > 0) {
            throw new BadRequestException("Paid premium templates require e-commerce checkout which is not enabled yet");
        }
        return template;
    }

    private Media requireActiveFanLetterMedia(Integer mediaId) {
        Media media = mediaRepository.findById(mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Background image media not found with id: " + mediaId));
        if (!MediaStatus.ACTIVE.name().equalsIgnoreCase(media.getStatus())) {
            throw new BadRequestException("Background image media is not active");
        }
        if (!MediaTargetType.FAN_LETTER.name().equalsIgnoreCase(media.getTargetType())) {
            throw new BadRequestException("Referenced media is not a FAN_LETTER background image");
        }
        return media;
    }

    private List<StickerItem> normalizeStickers(List<StickerItem> raw) {
        if (raw == null || raw.isEmpty()) {
            return new ArrayList<>();
        }
        List<StickerItem> normalized = new ArrayList<>(raw.size());
        for (StickerItem item : raw) {
            normalized.add(StickerItem.builder()
                    .id(item.getId().trim())
                    .x(item.getX())
                    .y(item.getY())
                    .scale(item.getScale())
                    .rotation(item.getRotation() != null ? item.getRotation() : 0.0)
                    .build());
        }
        return normalized;
    }

    private PageResponse<FanLetterResponse> enrichPage(Page<FanLetter> page) {
        List<FanLetter> letters = page.getContent();
        if (letters.isEmpty()) {
            return PageResponse.<FanLetterResponse>builder()
                    .items(List.of())
                    .page(page.getNumber())
                    .size(page.getSize())
                    .totalElements(page.getTotalElements())
                    .totalPages(page.getTotalPages())
                    .last(page.isLast())
                    .build();
        }

        Set<Integer> bgIds = letters.stream()
                .map(FanLetter::getBackgroundImageId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Integer, Media> bgById = loadBackgroundMediaMap(bgIds);
        List<FanLetterResponse> items = letters.stream()
                .map(letter -> FanLetterResponse.from(letter, bgById.get(letter.getBackgroundImageId())))
                .toList();

        return PageResponse.<FanLetterResponse>builder()
                .items(items)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    private Map<Integer, Media> loadBackgroundMediaMap(Set<Integer> bgIds) {
        if (bgIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Integer, Media> map = new HashMap<>();
        for (Media media : mediaRepository.findAllById(bgIds)) {
            if (MediaStatus.ACTIVE.name().equalsIgnoreCase(media.getStatus())) {
                map.put(media.getId(), media);
            }
        }
        return map;
    }

    private Media loadSingleBackgroundMedia(Integer bgId) {
        if (bgId == null) {
            return null;
        }
        return mediaRepository.findById(bgId)
                .filter(m -> MediaStatus.ACTIVE.name().equalsIgnoreCase(m.getStatus()))
                .orElse(null);
    }

    private PageResponse<FanLetterResponse> emptyPage(Pageable pageable) {
        return PageResponse.<FanLetterResponse>builder()
                .items(List.of())
                .page(pageable.getPageNumber())
                .size(pageable.getPageSize())
                .totalElements(0)
                .totalPages(0)
                .last(true)
                .build();
    }

    private enum VisibilityFilter {
        ALL(false, false),
        PUBLIC(true, true),
        PRIVATE(true, false);

        final boolean filterByPublic;
        final boolean publicValue;

        VisibilityFilter(boolean filterByPublic, boolean publicValue) {
            this.filterByPublic = filterByPublic;
            this.publicValue = publicValue;
        }

        static VisibilityFilter fromString(String value) {
            if (value == null || value.isBlank() || "ALL".equalsIgnoreCase(value.trim())) {
                return ALL;
            }
            try {
                return VisibilityFilter.valueOf(value.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Invalid visibility filter: " + value + ". Allowed values: ALL, PUBLIC, PRIVATE");
            }
        }
    }
}
