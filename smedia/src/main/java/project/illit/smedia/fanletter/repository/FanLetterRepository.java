package project.illit.smedia.fanletter.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.illit.smedia.fanletter.entity.FanLetter;

import java.util.Collection;
import java.util.Optional;

public interface FanLetterRepository extends JpaRepository<FanLetter, Integer> {

    @EntityGraph(attributePaths = {"user", "user.role", "artist", "artist.user", "artist.company", "template"})
    Optional<FanLetter> findWithDetailsById(Integer id);

    @EntityGraph(attributePaths = {"user", "user.role", "artist", "artist.user", "artist.company", "template"})
    @Query("""
            SELECT fl FROM FanLetter fl
            WHERE UPPER(fl.status) = 'PUBLISHED'
              AND fl.isPublic = true
              AND fl.artist.userId IN :artistUserIds
              AND (:filterByArtist = false OR fl.artist.userId = :artistId)
            ORDER BY fl.createdAt DESC, fl.id DESC
            """)
    Page<FanLetter> findPublicWallByArtistIds(
            @Param("artistUserIds") Collection<Integer> artistUserIds,
            @Param("filterByArtist") boolean filterByArtist,
            @Param("artistId") int artistId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"user", "user.role", "artist", "artist.user", "artist.company", "template"})
    @Query("""
            SELECT fl FROM FanLetter fl
            WHERE UPPER(fl.status) = 'PUBLISHED'
              AND fl.isPublic = true
              AND fl.artist.userId = :artistId
            ORDER BY fl.createdAt DESC, fl.id DESC
            """)
    Page<FanLetter> findPublicLettersForArtist(
            @Param("artistId") Integer artistId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"user", "user.role", "artist", "artist.user", "artist.company", "template"})
    @Query("""
            SELECT fl FROM FanLetter fl
            WHERE UPPER(fl.status) = 'PUBLISHED'
              AND fl.artist.userId = :artistId
              AND (:filterByPublic = false OR fl.isPublic = :isPublic)
            ORDER BY fl.createdAt DESC, fl.id DESC
            """)
    Page<FanLetter> findArtistInbox(
            @Param("artistId") Integer artistId,
            @Param("filterByPublic") boolean filterByPublic,
            @Param("isPublic") boolean isPublic,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"user", "user.role", "artist", "artist.user", "artist.company", "template"})
    @Query("""
            SELECT fl FROM FanLetter fl
            WHERE fl.user.id = :senderId
              AND UPPER(fl.status) <> 'DELETED'
              AND (:filterByArtist = false OR fl.artist.userId = :artistId)
            ORDER BY fl.createdAt DESC, fl.id DESC
            """)
    Page<FanLetter> findSentByUser(
            @Param("senderId") Integer senderId,
            @Param("filterByArtist") boolean filterByArtist,
            @Param("artistId") int artistId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"user", "user.role", "artist", "artist.user", "artist.company", "template"})
    @Query("""
            SELECT fl FROM FanLetter fl
            WHERE (:filterByArtist = false OR fl.artist.userId = :artistId)
              AND (:filterByStatus = false OR UPPER(fl.status) = UPPER(:status))
            ORDER BY fl.createdAt DESC, fl.id DESC
            """)
    Page<FanLetter> findAllForAdmin(
            @Param("filterByArtist") boolean filterByArtist,
            @Param("artistId") int artistId,
            @Param("filterByStatus") boolean filterByStatus,
            @Param("status") String status,
            Pageable pageable
    );
}
