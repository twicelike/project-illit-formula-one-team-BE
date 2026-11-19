package project.illit.smedia.fanletter.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import project.illit.smedia.fanletter.entity.LetterTemplate;

import java.util.List;

public interface LetterTemplateRepository extends JpaRepository<LetterTemplate, Integer> {

    List<LetterTemplate> findByIsActiveTrueOrderByIdAsc();

    List<LetterTemplate> findByIsActiveTrueAndIsPremiumFalseOrderByIdAsc();

    List<LetterTemplate> findAllByOrderByIdAsc();
}
