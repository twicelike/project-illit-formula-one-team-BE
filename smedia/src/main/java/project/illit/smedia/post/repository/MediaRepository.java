package project.illit.smedia.post.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.illit.smedia.post.entity.Media;

import java.util.Collection;
import java.util.List;

public interface MediaRepository extends JpaRepository<Media, Integer> {

    List<Media> findByTargetTypeAndTargetIdAndStatusIgnoreCaseOrderByIdAsc(
            String targetType,
            Integer targetId,
            String status
    );

    List<Media> findByTargetTypeAndTargetIdInAndStatusIgnoreCaseOrderByIdAsc(
            String targetType,
            Collection<Integer> targetIds,
            String status
    );

    @Modifying
    @Query("""
            UPDATE Media m
            SET m.status = 'DELETED'
            WHERE m.targetType = :targetType
              AND m.targetId = :targetId
              AND UPPER(m.status) = 'ACTIVE'
            """)
    int softDeleteActiveByTarget(
            @Param("targetType") String targetType,
            @Param("targetId") Integer targetId
    );

    @Query("""
            SELECT m, p.id, u.name, p.isArtistPost
            FROM Media m
            JOIN Post p ON p.id = m.targetId
            JOIN p.user u
            WHERE m.targetType = 'POST'
              AND UPPER(m.status) = 'ACTIVE'
              AND p.fandom.id = :fandomId
              AND UPPER(p.status) = 'PUBLISHED'
              AND (:artistOnly = false OR p.isArtistPost = true)
            ORDER BY m.createdAt DESC, m.id DESC
            """)
    Page<Object[]> findFandomPostGallery(
            @Param("fandomId") Integer fandomId,
            @Param("artistOnly") boolean artistOnly,
            Pageable pageable
    );
}
