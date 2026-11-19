package project.illit.smedia.fanletter.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import project.illit.smedia.common.exception.ResourceNotFoundException;
import project.illit.smedia.fanletter.dto.LetterTemplateRequest;
import project.illit.smedia.fanletter.dto.LetterTemplateResponse;
import project.illit.smedia.fanletter.entity.LetterTemplate;
import project.illit.smedia.fanletter.repository.LetterTemplateRepository;

import java.util.List;

@Service
public class LetterTemplateService {

    private final LetterTemplateRepository templateRepository;

    public LetterTemplateService(LetterTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    @Transactional(readOnly = true)
    public List<LetterTemplateResponse> listActiveTemplates(boolean freeOnly) {
        List<LetterTemplate> templates = freeOnly
                ? templateRepository.findByIsActiveTrueAndIsPremiumFalseOrderByIdAsc()
                : templateRepository.findByIsActiveTrueOrderByIdAsc();
        return templates.stream().map(LetterTemplateResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<LetterTemplateResponse> listAllTemplatesForAdmin() {
        return templateRepository.findAllByOrderByIdAsc().stream()
                .map(LetterTemplateResponse::from)
                .toList();
    }

    @Transactional
    public LetterTemplateResponse createTemplate(LetterTemplateRequest request) {
        boolean premium = Boolean.TRUE.equals(request.isPremium());
        int price = request.price() != null ? request.price() : 0;
        boolean active = request.isActive() == null || request.isActive();

        LetterTemplate saved = templateRepository.save(LetterTemplate.builder()
                .imageUrl(request.imageUrl().trim())
                .isPremium(premium)
                .price(price)
                .isActive(active)
                .build());
        return LetterTemplateResponse.from(saved);
    }

    @Transactional
    public LetterTemplateResponse updateTemplate(Integer templateId, LetterTemplateRequest request) {
        LetterTemplate template = requireTemplate(templateId);
        template.setImageUrl(request.imageUrl().trim());
        if (request.isPremium() != null) {
            template.setIsPremium(request.isPremium());
        }
        if (request.price() != null) {
            template.setPrice(request.price());
        }
        if (request.isActive() != null) {
            template.setIsActive(request.isActive());
        }
        return LetterTemplateResponse.from(templateRepository.save(template));
    }

    @Transactional
    public LetterTemplateResponse updateTemplateStatus(Integer templateId, boolean active) {
        LetterTemplate template = requireTemplate(templateId);
        template.setIsActive(active);
        return LetterTemplateResponse.from(templateRepository.save(template));
    }

    @Transactional
    public void deleteTemplate(Integer templateId) {
        templateRepository.delete(requireTemplate(templateId));
    }

    public LetterTemplate requireTemplate(Integer templateId) {
        return templateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Letter template not found with id: " + templateId));
    }
}
