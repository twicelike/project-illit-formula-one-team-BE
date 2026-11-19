package project.illit.smedia.post.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.illit.smedia.post.entity.Post;

import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Integer> {

    @EntityGraph(attributePaths = {"user", "user.role", "fandom"})
    Optional<Post> findWithDetailsById(Integer id);

    @EntityGraph(attributePaths = {"user", "user.role", "fandom"})
    @Query("""
            SELECT p FROM Post p
            WHERE p.fandom.id = :fandomId
              AND UPPER(p.status) = UPPER(:status)
              AND (:filterByArtist = false OR p.isArtistPost = :artistPost)
              AND (:onlyWithMedia = false OR EXISTS (
                    SELECT 1 FROM Media m
                    WHERE m.targetId = p.id
                      AND m.targetType = 'POST'
                      AND UPPER(m.status) = 'ACTIVE'
              ))
            ORDER BY p.createdAt DESC, p.id DESC
            """)
    Page<Post> findFandomFeedLatest(
            @Param("fandomId") Integer fandomId,
            @Param("status") String status,
            @Param("filterByArtist") boolean filterByArtist,
            @Param("artistPost") boolean artistPost,
            @Param("onlyWithMedia") boolean onlyWithMedia,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"user", "user.role", "fandom"})
    @Query("""
            SELECT p FROM Post p
            WHERE p.fandom.id = :fandomId
              AND UPPER(p.status) = UPPER(:status)
              AND (:filterByArtist = false OR p.isArtistPost = :artistPost)
              AND (:onlyWithMedia = false OR EXISTS (
                    SELECT 1 FROM Media m
                    WHERE m.targetId = p.id
                      AND m.targetType = 'POST'
                      AND UPPER(m.status) = 'ACTIVE'
              ))
            ORDER BY (COALESCE(p.totalReactions, 0) * 2 + COALESCE(p.totalComments, 0) * 3) DESC, p.createdAt DESC, p.id DESC
            """)
    Page<Post> findFandomFeedHot(
            @Param("fandomId") Integer fandomId,
            @Param("status") String status,
            @Param("filterByArtist") boolean filterByArtist,
            @Param("artistPost") boolean artistPost,
            @Param("onlyWithMedia") boolean onlyWithMedia,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"user", "user.role", "fandom"})
    Page<Post> findByUser_IdAndStatusIgnoreCaseOrderByCreatedAtDesc(
            Integer userId,
            String status,
            Pageable pageable
    );

    @Override
    @EntityGraph(attributePaths = {"user", "user.role", "fandom"})
    Page<Post> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"user", "user.role", "fandom"})
    Page<Post> findByFandom_Id(Integer fandomId, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "user.role", "fandom"})
    Page<Post> findByStatusIgnoreCase(String status, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "user.role", "fandom"})
    Page<Post> findByFandom_IdAndStatusIgnoreCase(Integer fandomId, String status, Pageable pageable);
}
