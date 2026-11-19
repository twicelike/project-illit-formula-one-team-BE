package project.illit.smedia.user.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.illit.smedia.common.dto.PageResponse;
import project.illit.smedia.common.exception.ConflictException;
import project.illit.smedia.common.exception.ResourceNotFoundException;
import project.illit.smedia.fandom.entity.Company;
import project.illit.smedia.fandom.service.CompanyService;
import project.illit.smedia.user.dto.ArtistProfileResponse;
import project.illit.smedia.user.dto.CreateArtistAccountRequest;
import project.illit.smedia.user.dto.UpdateArtistProfileRequest;
import project.illit.smedia.user.entity.ArtistProfile;
import project.illit.smedia.user.entity.Role;
import project.illit.smedia.user.entity.RoleName;
import project.illit.smedia.user.entity.User;
import project.illit.smedia.user.repository.ArtistProfileRepository;
import project.illit.smedia.user.repository.RoleRepository;
import project.illit.smedia.user.repository.UserRepository;

@Service
public class ArtistProfileService {

    private final ArtistProfileRepository artistProfileRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CompanyService companyService;
    private final PasswordEncoder passwordEncoder;

    public ArtistProfileService(
            ArtistProfileRepository artistProfileRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            CompanyService companyService,
            PasswordEncoder passwordEncoder
    ) {
        this.artistProfileRepository = artistProfileRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.companyService = companyService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public PageResponse<ArtistProfileResponse> listArtists(Integer companyId, String keyword, Pageable pageable) {
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        Page<ArtistProfile> page;
        if (companyId != null && hasKeyword) {
            page = artistProfileRepository.findByCompany_IdAndStageNameContainingIgnoreCase(
                    companyId, keyword.trim(), pageable
            );
        } else if (companyId != null) {
            page = artistProfileRepository.findByCompany_Id(companyId, pageable);
        } else if (hasKeyword) {
            page = artistProfileRepository.findByStageNameContainingIgnoreCase(keyword.trim(), pageable);
        } else {
            page = artistProfileRepository.findAll(pageable);
        }
        return PageResponse.from(page.map(ArtistProfileResponse::from));
    }

    @Transactional(readOnly = true)
    public ArtistProfileResponse getArtistDetail(Integer artistUserId) {
        ArtistProfile profile = artistProfileRepository.findWithDetailsByUserId(artistUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Artist profile not found for user id: " + artistUserId));
        return ArtistProfileResponse.from(profile);
    }

    @Transactional
    public ArtistProfileResponse updateMyArtistProfile(Integer artistUserId, UpdateArtistProfileRequest request) {
        ArtistProfile profile = artistProfileRepository.findWithDetailsByUserId(artistUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Artist profile not found for current user"));

        profile.setStageName(request.stageName().trim());
        profile.setBio(request.bio());
        if (request.avatar() != null) {
            String trimmedAvatar = request.avatar().trim();
            profile.setAvatar(trimmedAvatar);
            if (profile.getUser() != null) {
                profile.getUser().setAvatar(trimmedAvatar);
            }
        }
        return ArtistProfileResponse.from(artistProfileRepository.save(profile));
    }

    @Transactional
    public ArtistProfileResponse provisionArtistAccount(CreateArtistAccountRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ConflictException("Email is already registered: " + normalizedEmail);
        }

        Company company = companyService.requireCompany(request.companyId());
        Role artistRole = roleRepository.findByNameIgnoreCase(RoleName.ARTIST.name())
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.ARTIST.name()).build()));

        User artistUser = User.builder()
                .role(artistRole)
                .email(normalizedEmail)
                .name(request.name().trim())
                .password(passwordEncoder.encode(request.password()))
                .avatar(request.avatar())
                .totalFollower(0)
                .totalFollowing(0)
                .isActive(true)
                .build();
        User savedUser = userRepository.save(artistUser);

        ArtistProfile profile = ArtistProfile.builder()
                .user(savedUser)
                .stageName(request.stageName().trim())
                .bio(request.bio())
                .avatar(request.avatar())
                .company(company)
                .followersCount(0)
                .debutDate(request.debutDate())
                .build();

        ArtistProfile savedProfile = artistProfileRepository.save(profile);
        return ArtistProfileResponse.from(savedProfile);
    }
}
