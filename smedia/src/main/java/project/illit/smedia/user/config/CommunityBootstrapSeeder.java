package project.illit.smedia.user.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import project.illit.smedia.fandom.entity.Company;
import project.illit.smedia.fandom.entity.FandomMember;
import project.illit.smedia.fandom.entity.FandomMemberRole;
import project.illit.smedia.fandom.entity.FandomStatus;
import project.illit.smedia.fandom.entity.OfficialFandom;
import project.illit.smedia.fandom.repository.CompanyRepository;
import project.illit.smedia.fandom.repository.FandomMemberRepository;
import project.illit.smedia.fandom.repository.OfficialFandomRepository;
import project.illit.smedia.fanletter.entity.FanLetter;
import project.illit.smedia.fanletter.entity.FanLetterStatus;
import project.illit.smedia.fanletter.entity.LetterTemplate;
import project.illit.smedia.fanletter.entity.StickerItem;
import project.illit.smedia.fanletter.repository.FanLetterRepository;
import project.illit.smedia.fanletter.repository.LetterTemplateRepository;
import project.illit.smedia.interaction.entity.Comment;
import project.illit.smedia.interaction.repository.CommentRepository;
import project.illit.smedia.post.entity.Media;
import project.illit.smedia.post.entity.MediaStatus;
import project.illit.smedia.post.entity.MediaTargetType;
import project.illit.smedia.post.entity.Post;
import project.illit.smedia.post.entity.PostStatus;
import project.illit.smedia.post.repository.MediaRepository;
import project.illit.smedia.post.repository.PostRepository;
import project.illit.smedia.user.entity.ArtistProfile;
import project.illit.smedia.user.entity.MemberProfile;
import project.illit.smedia.user.entity.Role;
import project.illit.smedia.user.entity.RoleName;
import project.illit.smedia.user.entity.User;
import project.illit.smedia.user.repository.ArtistProfileRepository;
import project.illit.smedia.user.repository.MemberProfileRepository;
import project.illit.smedia.user.repository.RoleRepository;
import project.illit.smedia.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed-initial-data", havingValue = "true", matchIfMissing = true)
public class CommunityBootstrapSeeder implements ApplicationRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final MemberProfileRepository memberProfileRepository;
    private final ArtistProfileRepository artistProfileRepository;
    private final CompanyRepository companyRepository;
    private final OfficialFandomRepository fandomRepository;
    private final FandomMemberRepository fandomMemberRepository;
    private final LetterTemplateRepository letterTemplateRepository;
    private final PostRepository postRepository;
    private final MediaRepository mediaRepository;
    private final CommentRepository commentRepository;
    private final FanLetterRepository fanLetterRepository;
    private final PasswordEncoder passwordEncoder;

    private record SeedFandomDef(
            String name,
            String bannerUrl,
            String companyName,
            List<String> members
    ) {
    }

    private static final List<SeedFandomDef> FANDOM_CATALOG = List.of(
            new SeedFandomDef("ILLIT", "/sites/weverse/artists/hyolyn-logo.webp", "BELIFT LAB",
                    List.of("YUNAH", "MINJU", "MOKA", "WONHEE", "IROHA")),
            new SeedFandomDef("LE SSERAFIM", "/sites/weverse/dm/lesserafim.webp", "SOURCE MUSIC",
                    List.of("KIM CHAEWON", "SAKURA", "HUH YUNJIN", "KAZUHA", "HONG EUNCHAE")),
            new SeedFandomDef("SEVENTEEN", "/sites/weverse/artists/seventeen-logo.webp", "PLEDIS Entertainment",
                    List.of("S.COUPS", "JEONGHAN", "JOSHUA", "JUN", "HOSHI", "WONWOO", "WOOZI")),
            new SeedFandomDef("BTS", "/sites/weverse/hero/slide-12.webp", "BIGHIT MUSIC",
                    List.of("RM", "Jin", "SUGA", "j-hope", "Jimin", "V", "Jung Kook")),
            new SeedFandomDef("ENHYPEN", "/sites/weverse/hero/slide-14.webp", "BELIFT LAB",
                    List.of("JUNGWON", "HEESEUNG", "JAY", "JAKE", "SUNGHOON", "SUNOO", "NI-KI")),
            new SeedFandomDef("BOYNEXTDOOR", "/sites/weverse/hero/slide-11.webp", "KOZ Entertainment",
                    List.of("SUNGHO", "RIWOO", "JAEHYUN", "TAESAN", "LEEHAN", "WOONHAK")),
            new SeedFandomDef("TWS", "/sites/weverse/hero/slide-06.webp", "PLEDIS Entertainment",
                    List.of("SHINYU", "DOHOON", "YOUNGJAE", "HANJIN", "JIHOON", "KYUNGMIN")),
            new SeedFandomDef("KATSEYE", "/sites/weverse/artists/katseye-header.webp", "HYBE x Geffen",
                    List.of("DANIELA", "LARA", "MANON", "MEGAN", "SOPHIA", "YOONCHAE")),
            new SeedFandomDef("Red Velvet", "/sites/weverse/artists/redvelvet-header.webp", "SM Entertainment",
                    List.of("IRENE", "SEULGI", "WENDY", "JOY", "YERI")),
            new SeedFandomDef("STAYC", "/sites/weverse/artists/top-202.webp", "HIGH UP Entertainment",
                    List.of("Sumin", "Sieun", "ISA", "Seeun", "Yoon", "J")),
            new SeedFandomDef("Kep1er", "/sites/weverse/artists/hyolyn-header.webp", "WAKEONE",
                    List.of("YUJIN", "XIAOTING", "CHAEHYUN", "DAYEON", "HIKARU", "HUENING BAHIYYIH", "YOUNGEUN")),
            new SeedFandomDef("P1Harmony", "/sites/weverse/hero/slide-10.webp", "FNC Entertainment",
                    List.of("KEEHO", "THEO", "JIUNG", "INTAK", "SOUL", "JONGSEOB")),
            new SeedFandomDef("PiXXiE", "/sites/weverse/hero/slide-02.webp", "LIT Entertainment",
                    List.of("MABELZ", "PIMMA", "INGKHO")),
            new SeedFandomDef("JVKE", "/sites/weverse/artists/top-198.webp", "Weverse Global", List.of("JVKE")),
            new SeedFandomDef("JD1", "/sites/weverse/artists/top-199.webp", "Weverse Global", List.of("JD1")),
            new SeedFandomDef("LNGSHOT", "/sites/weverse/artists/top-200.webp", "Weverse Global", List.of("LNGSHOT")),
            new SeedFandomDef("LEE SUNG KYOUNG", "/sites/weverse/artists/top-201.webp", "YG Entertainment", List.of("LEE SUNG KYOUNG")),
            new SeedFandomDef("SF9", "/sites/weverse/artists/top-203.webp", "FNC Entertainment", List.of("YOUNG BIN", "IN SEONG", "CHA NI")),
            new SeedFandomDef("HYOLYN", "/sites/weverse/artists/seventeen-header.webp", "Weverse Global", List.of("HYOLYN")),
            new SeedFandomDef("LEE JUNHO", "/sites/weverse/artists/leejunho-header.webp", "JYP Entertainment", List.of("LEE JUNHO")),
            new SeedFandomDef("HANBIN", "/sites/weverse/artists/hanbin-header.webp", "Weverse Global", List.of("HANBIN")),
            new SeedFandomDef("BAEKHO(KANG DONG HO)", "/sites/weverse/artists/baekho-header.webp", "PLEDIS Entertainment", List.of("BAEKHO")),
            new SeedFandomDef("AHN HYO SEOP", "/sites/weverse/artists/ahnhyoseop-header.webp", "The Present Co.", List.of("AHN HYO SEOP")),
            new SeedFandomDef("LEE CHAE MIN", "/sites/weverse/artists/leechaemin-header.webp", "Weverse Global", List.of("LEE CHAE MIN")),
            new SeedFandomDef("OWIS", "/sites/weverse/artists/owis-header.webp", "Weverse Global", List.of("OWIS")),
            new SeedFandomDef("noa", "/sites/weverse/artists/noa_aburasoba-header.webp", "Weverse Global", List.of("noa")),
            new SeedFandomDef("Fifth Harmony", "/sites/weverse/artists/fifthharmony-header.webp", "Weverse Global", List.of("Ally Brooke", "Normani", "Dinah Jane", "Lauren Jauregui")),
            new SeedFandomDef("AKMU", "/sites/weverse/artists/akmu-header.webp", "YG Entertainment", List.of("LEE CHANHYUK", "LEE SUHYUN")),
            new SeedFandomDef("BIBI", "/sites/weverse/artists/bibi-header.webp", "Feel Ghood Music", List.of("BIBI")),
            new SeedFandomDef("KIM WOO BIN", "/sites/weverse/artists/kimwoobin-header.webp", "AM Entertainment", List.of("KIM WOO BIN")),
            new SeedFandomDef("3SKM", "/sites/weverse/artists/3skm-header.webp", "NIJISANJI", List.of("KITAMI YUSEI", "KAISEI", "SAKAKI NESS")),
            new SeedFandomDef("ERIC", "/sites/weverse/artists/eric-header.webp", "Weverse Global", List.of("ERIC")),
            new SeedFandomDef("ADAP", "/sites/weverse/artists/adap-header.webp", "Weverse Global", List.of("ADAP")),
            new SeedFandomDef("AHN BO HYUN", "/sites/weverse/artists/ahnbohyun-header.webp", "FN Entertainment", List.of("AHN BO HYUN")),
            new SeedFandomDef("Whee In", "/sites/weverse/artists/wheein-header.webp", "THE L1VE", List.of("Whee In")),
            new SeedFandomDef("HONG SEOK", "/sites/weverse/artists/hongseok-header.webp", "Weverse Global", List.of("HONG SEOK")),
            new SeedFandomDef("Mark Lee", "/sites/weverse/hero/slide-08.webp", "SM Entertainment", List.of("Mark Lee")),
            new SeedFandomDef("LEE SEYOUNG", "/sites/weverse/hero/slide-16.webp", "Prain TPC", List.of("LEE SEYOUNG")),
            new SeedFandomDef("ASC2NT", "/sites/weverse/hero/slide-17.webp", "NEW WAYS COMPANY", List.of("KARAM", "JAY", "REON", "KYLE", "HYOWON")),
            new SeedFandomDef("BYEON WOO SEOK", "/sites/weverse/dm/byeonwooseok.webp", "VARO Entertainment", List.of("BYEON WOO SEOK")),
            new SeedFandomDef("KIM MYUNGSOO(L)", "/sites/weverse/dm/kimmyungsoo.webp", "LOOKMEDIA", List.of("KIM MYUNGSOO(L)")),
            new SeedFandomDef("KINO", "/sites/weverse/dm/kino.webp", "NAKED", List.of("KINO")),
            new SeedFandomDef("SON NA EUN", "/sites/weverse/dm/sonnaeun.webp", "YG Entertainment", List.of("SON NA EUN")),
            new SeedFandomDef("WhOLLiN", "/sites/weverse/dm/whollin.webp", "Weverse Global", List.of("WhOLLiN")),
            new SeedFandomDef("IRISÉ", "/sites/weverse/dm/irise.webp", "Weverse Global", List.of("IRISÉ")),
            new SeedFandomDef("MUN KA YOUNG", "/sites/weverse/dm/winner.webp", "PEAK J", List.of("MUN KA YOUNG")),
            new SeedFandomDef("WINNER", "/sites/weverse/dm/yoonsanha.webp", "YG Entertainment", List.of("YOON", "JINU", "HOONY", "MINO")),
            new SeedFandomDef("YOON SAN-HA", "/sites/weverse/dm/chuu.webp", "Fantagio", List.of("YOON SAN-HA")),
            new SeedFandomDef("CHUU", "/sites/weverse/dm/tnx.webp", "ATRP", List.of("CHUU")),
            new SeedFandomDef("TNX", "/sites/weverse/dm/sb19.webp", "P NATION", List.of("TAEHUN", "HYUNSOO", "JUNHYEOK", "HWI", "SUNGJUN")),
            new SeedFandomDef("SB19", "/sites/weverse/dm/marklee.webp", "1Z Entertainment", List.of("PABLO", "JOSH", "STELL", "KEN", "JUSTIN")),
            new SeedFandomDef("DXMON", "/sites/weverse/dm/dxmon.webp", "H MUSIC", List.of("MINJAE", "SEITA", "HEE", "TK", "REX"))
    );

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        try {
            Role fanRole = ensureRole(RoleName.FAN);
            Role artistRole = ensureRole(RoleName.ARTIST);
            Role adminRole = ensureRole(RoleName.ADMIN);

            String defaultEncodedPass = passwordEncoder.encode("123456");

            ensureAdminUser("admin@weverse.local", "Weverse Admin", defaultEncodedPass, adminRole);
            User demoFan = ensureFanUser("fan@weverse.local", "GLLIT_Global", defaultEncodedPass, fanRole);

            seedLetterTemplates();

            boolean shouldSeedFandoms = fandomRepository.count() < FANDOM_CATALOG.size();
            OfficialFandom illitFandom = null;
            User yunahUser = null;

            if (shouldSeedFandoms) {
                log.info("Bootstrapping Weverse communities and artist profiles...");
                for (SeedFandomDef def : FANDOM_CATALOG) {
                    Company company = ensureCompany(def.companyName());
                    OfficialFandom fandom = ensureFandom(def.name(), def.bannerUrl(), company);

                    for (String memberStageName : def.members()) {
                        String emailSlug = sanitizeEmailSlug(def.name() + "." + memberStageName);
                        String email = emailSlug + "@artist.weverse.local";
                        User artistUser = ensureArtistUser(
                                email,
                                memberStageName,
                                def.bannerUrl(),
                                defaultEncodedPass,
                                artistRole,
                                company,
                                "Official Weverse artist member of " + def.name()
                        );
                        ensureFandomMembership(fandom, artistUser, FandomMemberRole.ARTIST.name());

                        if ("ILLIT".equalsIgnoreCase(def.name()) && "YUNAH".equalsIgnoreCase(memberStageName)) {
                            yunahUser = artistUser;
                        }
                    }

                    if ("ILLIT".equalsIgnoreCase(def.name())) {
                        illitFandom = fandom;
                    }
                }
            } else {
                illitFandom = fandomRepository.findByNameIgnoreCase("ILLIT").orElse(null);
                yunahUser = userRepository.findByEmailIgnoreCase("illit.yunah@artist.weverse.local").orElse(null);
            }

            if (illitFandom != null && demoFan != null) {
                ensureFandomMembership(illitFandom, demoFan, FandomMemberRole.MEMBER.name());
            }

            if (illitFandom != null && yunahUser != null && postRepository.count() == 0) {
                seedSampleIllitPostsAndLetters(illitFandom, yunahUser, demoFan);
            }
        } catch (Exception ex) {
            log.warn("CommunityBootstrapSeeder skipped or partially completed: {}", ex.getMessage());
        }
    }

    private Role ensureRole(RoleName name) {
        return roleRepository.findByNameIgnoreCase(name.name())
                .orElseGet(() -> roleRepository.save(Role.builder().name(name.name()).build()));
    }

    private Company ensureCompany(String name) {
        return companyRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> companyRepository.save(Company.builder()
                        .name(name)
                        .bio("Official entertainment agency on Weverse.")
                        .build()));
    }

    private OfficialFandom ensureFandom(String name, String bannerUrl, Company company) {
        return fandomRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> fandomRepository.save(OfficialFandom.builder()
                        .name(name)
                        .bannerUrl(bannerUrl)
                        .company(company)
                        .status(FandomStatus.ACTIVE.name())
                        .build()));
    }

    private void ensureAdminUser(String email, String name, String encodedPassword, Role adminRole) {
        userRepository.findByEmailIgnoreCase(email).orElseGet(() -> userRepository.save(User.builder()
                .email(email)
                .name(name)
                .password(encodedPassword)
                .role(adminRole)
                .avatar("/sites/weverse/logo192.png")
                .isActive(true)
                .build()));
    }

    private User ensureFanUser(String email, String name, String encodedPassword, Role fanRole) {
        return userRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
            User saved = userRepository.save(User.builder()
                    .email(email)
                    .name(name)
                    .password(encodedPassword)
                    .role(fanRole)
                    .avatar("/sites/weverse/artists/kep1er-logo.webp")
                    .isActive(true)
                    .build());
            memberProfileRepository.save(MemberProfile.builder()
                    .user(saved)
                    .membershipTier("GLLIT MEMBERSHIP")
                    .totalLoyaltyPoints(350)
                    .build());
            return saved;
        });
    }

    private User ensureArtistUser(
            String email,
            String stageName,
            String avatar,
            String encodedPassword,
            Role artistRole,
            Company company,
            String bio
    ) {
        return userRepository.findByEmailIgnoreCase(email).orElseGet(() -> {
            User saved = userRepository.save(User.builder()
                    .email(email)
                    .name(stageName)
                    .password(encodedPassword)
                    .role(artistRole)
                    .avatar(avatar)
                    .totalFollower(125000)
                    .isActive(true)
                    .build());
            artistProfileRepository.save(ArtistProfile.builder()
                    .user(saved)
                    .stageName(stageName)
                    .avatar(avatar)
                    .bio(bio)
                    .company(company)
                    .followersCount(125000)
                    .debutDate(LocalDateTime.now().minusMonths(14))
                    .build());
            return saved;
        });
    }

    private void ensureFandomMembership(OfficialFandom fandom, User user, String role) {
        if (!fandomMemberRepository.existsByFandom_IdAndUser_Id(fandom.getId(), user.getId())) {
            fandomMemberRepository.save(FandomMember.builder()
                    .fandom(fandom)
                    .user(user)
                    .role(role)
                    .build());
        }
    }

    private void seedLetterTemplates() {
        if (letterTemplateRepository.count() > 0) {
            return;
        }
        List<String> themes = List.of(
                "/sites/weverse/hero/slide-15.webp",
                "/sites/weverse/hero/slide-07.webp",
                "/sites/weverse/hero/slide-04.webp",
                "/sites/weverse/hero/slide-05.webp",
                "/sites/weverse/hero/slide-06.webp"
        );
        for (int i = 0; i < themes.size(); i++) {
            letterTemplateRepository.save(LetterTemplate.builder()
                    .imageUrl(themes.get(i))
                    .isPremium(i >= 3)
                    .price(i >= 3 ? 50 : 0)
                    .isActive(true)
                    .build());
        }
    }

    private void seedSampleIllitPostsAndLetters(OfficialFandom illit, User yunah, User fan) {
        Post artistPost = postRepository.save(Post.builder()
                .fandom(illit)
                .user(yunah)
                .content("Hello GLLIT! 💕 Thank you so much for cheering for ILLIT today. Did everyone eat a warm dinner? Let's meet again on Weverse soon! ✨")
                .isArtistPost(true)
                .totalReactions(18)
                .totalComments(1)
                .status(PostStatus.PUBLISHED.name())
                .build());

        mediaRepository.save(Media.builder()
                .targetId(artistPost.getId())
                .targetType(MediaTargetType.POST.name())
                .mediaUrl("/sites/weverse/hero/slide-15.webp")
                .thumbnail("/sites/weverse/hero/slide-15.webp")
                .status(MediaStatus.ACTIVE.name())
                .build());

        if (fan != null) {
            commentRepository.save(Comment.builder()
                    .post(artistPost)
                    .user(fan)
                    .content("We love you YUNAH!! Always cheering for ILLIT! 💖✨")
                    .totalReactions(5)
                    .totalReplies(0)
                    .build());

            Post fanPost = postRepository.save(Post.builder()
                    .fandom(illit)
                    .user(fan)
                    .content("Listening to ILLIT's album on repeat all morning! Who else is ready for the next comeback stage? 🎧💫")
                    .isArtistPost(false)
                    .totalReactions(12)
                    .totalComments(1)
                    .status(PostStatus.PUBLISHED.name())
                    .build());

            commentRepository.save(Comment.builder()
                    .post(fanPost)
                    .user(yunah)
                    .content("Thank you so much GLLIT!! See you at the stage soon 🥰")
                    .totalReactions(24)
                    .totalReplies(0)
                    .build());

            ArtistProfile yunahProfile = artistProfileRepository.findById(yunah.getId()).orElse(null);
            LetterTemplate firstTemplate = letterTemplateRepository.findByIsActiveTrueOrderByIdAsc()
                    .stream().findFirst().orElse(null);
            if (yunahProfile != null) {
                fanLetterRepository.save(FanLetter.builder()
                        .user(fan)
                        .artist(yunahProfile)
                        .template(firstTemplate)
                        .content("Dear YUNAH, thank you for always bringing bright energy and happiness to GLLIT every single day! 💌")
                        .stickerList(List.of(
                                StickerItem.builder().id("heart-sparkle").x(24.0).y(20.0).scale(1.0).rotation(0.0).build()
                        ))
                        .isPublic(true)
                        .status(FanLetterStatus.PUBLISHED.name())
                        .build());
            }
        }
    }

    private static String sanitizeEmailSlug(String raw) {
        String cleaned = raw.toLowerCase().replaceAll("[^a-z0-9]+", ".").replaceAll("^\\.+|\\.+$", "");
        return cleaned.isEmpty() ? "artist" : cleaned;
    }
}
