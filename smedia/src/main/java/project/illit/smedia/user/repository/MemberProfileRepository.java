package project.illit.smedia.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.illit.smedia.user.entity.MemberProfile;

public interface MemberProfileRepository extends JpaRepository<MemberProfile, Integer> {
}
