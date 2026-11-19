package project.illit.smedia.interaction.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.illit.smedia.interaction.entity.Comment;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Integer> {

    @EntityGraph(attributePaths = {"user", "user.role", "post", "post.user", "post.fandom", "parent"})
    Optional<Comment> findWithDetailsById(Integer id);

    @EntityGraph(attributePaths = {"user", "user.role"})
    @Query("""
            SELECT c FROM Comment c
            JOIN c.user u
            LEFT JOIN u.role r
            WHERE c.post.id = :postId
              AND c.parent IS NULL
            ORDER BY CASE WHEN UPPER(r.name) = 'ARTIST' THEN 0 ELSE 1 END ASC,
                     c.createdAt DESC,
                     c.id DESC
            """)
    Page<Comment> findTopLevelCommentsArtistFirstLatest(
            @Param("postId") Integer postId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"user", "user.role"})
    @Query("""
            SELECT c FROM Comment c
            JOIN c.user u
            LEFT JOIN u.role r
            WHERE c.post.id = :postId
              AND c.parent IS NULL
            ORDER BY CASE WHEN UPPER(r.name) = 'ARTIST' THEN 0 ELSE 1 END ASC,
                     COALESCE(c.totalReactions, 0) DESC,
                     c.createdAt DESC,
                     c.id DESC
            """)
    Page<Comment> findTopLevelCommentsArtistFirstPopular(
            @Param("postId") Integer postId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"user", "user.role"})
    @Query("""
            SELECT c FROM Comment c
            JOIN c.user u
            LEFT JOIN u.role r
            WHERE c.parent.id = :parentId
            ORDER BY CASE WHEN UPPER(r.name) = 'ARTIST' THEN 0 ELSE 1 END ASC,
                     c.createdAt ASC,
                     c.id ASC
            """)
    Page<Comment> findRepliesArtistFirst(
            @Param("parentId") Integer parentId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"user", "user.role"})
    @Query("""
            SELECT c FROM Comment c
            JOIN c.user u
            LEFT JOIN u.role r
            WHERE c.parent.id IN :parentIds
            ORDER BY c.parent.id ASC,
                     CASE WHEN UPPER(r.name) = 'ARTIST' THEN 0 ELSE 1 END ASC,
                     c.createdAt ASC,
                     c.id ASC
            """)
    List<Comment> findRepliesByParentIdsArtistFirst(@Param("parentIds") Collection<Integer> parentIds);

    @Query("""
            SELECT c.id FROM Comment c
            WHERE c.parent.id = :parentId
            """)
    List<Integer> findReplyIdsByParentId(@Param("parentId") Integer parentId);

    @Query("""
            SELECT DISTINCT c.post.id, c.user.id
            FROM Comment c
            JOIN c.user u
            JOIN u.role r
            WHERE c.post.id IN :postIds
              AND UPPER(r.name) = 'ARTIST'
            """)
    List<Object[]> findDistinctArtistCommentersByPostIds(@Param("postIds") Collection<Integer> postIds);

    @Modifying
    @Query("""
            UPDATE Comment c
            SET c.totalReactions = COALESCE(c.totalReactions, 0) + 1
            WHERE c.id = :commentId
            """)
    int incrementTotalReactions(@Param("commentId") Integer commentId);

    @Modifying
    @Query("""
            UPDATE Comment c
            SET c.totalReactions = CASE WHEN COALESCE(c.totalReactions, 0) > 0 THEN COALESCE(c.totalReactions, 0) - 1 ELSE 0 END
            WHERE c.id = :commentId
            """)
    int decrementTotalReactions(@Param("commentId") Integer commentId);

    @Modifying
    @Query("""
            UPDATE Comment c
            SET c.totalReplies = COALESCE(c.totalReplies, 0) + 1
            WHERE c.id = :commentId
            """)
    int incrementTotalReplies(@Param("commentId") Integer commentId);

    @Modifying
    @Query("""
            UPDATE Comment c
            SET c.totalReplies = CASE WHEN COALESCE(c.totalReplies, 0) > 0 THEN COALESCE(c.totalReplies, 0) - 1 ELSE 0 END
            WHERE c.id = :commentId
            """)
    int decrementTotalReplies(@Param("commentId") Integer commentId);
}
