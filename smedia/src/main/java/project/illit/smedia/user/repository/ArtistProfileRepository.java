package project.illit.smedia.user.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import project.illit.smedia.user.entity.ArtistProfile;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ArtistProfileRepository extends JpaRepository<ArtistProfile, Integer> {

    @EntityGraph(attributePaths = {"company", "user"})
    Optional<ArtistProfile> findWithDetailsByUserId(Integer userId);

    @EntityGraph(attributePaths = {"company", "user"})
    List<ArtistProfile> findWithDetailsByUserIdInOrderByStageNameAsc(Collection<Integer> userIds);

    @Override
    @EntityGraph(attributePaths = {"company", "user"})
    Page<ArtistProfile> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"company", "user"})
    Page<ArtistProfile> findByCompany_Id(Integer companyId, Pageable pageable);

    @EntityGraph(attributePaths = {"company", "user"})
    Page<ArtistProfile> findByStageNameContainingIgnoreCase(String stageName, Pageable pageable);

    @EntityGraph(attributePaths = {"company", "user"})
    Page<ArtistProfile> findByCompany_IdAndStageNameContainingIgnoreCase(
            Integer companyId,
            String stageName,
            Pageable pageable
    );
}
