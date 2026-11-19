package project.illit.smedia.interaction.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import project.illit.smedia.interaction.entity.Comment;

import java.util.Collection;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Integer> {

    @Query("""
            SELECT DISTINCT c.post.id, c.user.id
            FROM Comment c
            JOIN c.user u
            JOIN u.role r
            WHERE c.post.id IN :postIds
              AND UPPER(r.name) = 'ARTIST'
            """)
    List<Object[]> findDistinctArtistCommentersByPostIds(@Param("postIds") Collection<Integer> postIds);
}
