package com.sami.app.metadata.service;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.metadata.domain.MetaEntity;
import com.sami.app.metadata.domain.MetaForm;
import com.sami.app.metadata.domain.MetaFormLayout;
import com.sami.app.metadata.domain.MetaFormVersion;
import com.sami.app.metadata.domain.MetaRecordFormVersion;
import com.sami.app.metadata.event.MetadataDomainEvent;
import com.sami.app.metadata.repository.MetaEntityRepository;
import com.sami.app.metadata.repository.MetaFormLayoutRepository;
import com.sami.app.metadata.repository.MetaFormRepository;
import com.sami.app.metadata.repository.MetaFormVersionRepository;
import com.sami.app.metadata.repository.MetaRecordFormVersionRepository;
import com.sami.app.security.CurrentActor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Dynamic forms with immutable published versions and target-aware layouts.
 *
 * <p>Publishing freezes a schema snapshot; a record is bound to the version it
 * was captured with, so historical records stay renderable no matter how the
 * form evolves afterwards.
 */
@Service
@RequiredArgsConstructor
public class FormService {

    private final MetaFormRepository formRepository;
    private final MetaFormVersionRepository versionRepository;
    private final MetaFormLayoutRepository layoutRepository;
    private final MetaRecordFormVersionRepository recordVersionRepository;
    private final MetaEntityRepository entityRepository;
    private final MetaAuditService audit;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<MetaForm> list() {
        return formRepository.findAllBy();
    }

    @Transactional
    public MetaForm create(String moduleCode, String entityCode, String code, String name, String description) {
        if (formRepository.existsByCode(code)) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT, "Form code already exists: " + code);
        }
        MetaEntity entity = entityRepository.findByModuleCodeAndEntityCode(moduleCode, entityCode)
                .orElseThrow(() -> new ApiException(ErrorCode.BAD_REQUEST,
                        "Entity is not registered as extensible: " + moduleCode + "." + entityCode));
        MetaForm form = formRepository.save(MetaForm.builder()
                .entity(entity).code(code).name(name).description(description).build());
        audit.record("FORM", form.getId(), "CREATED", null, Map.of("code", code));
        return form;
    }

    /** Opens a new draft version, seeded from the latest version's schema. */
    @Transactional
    public MetaFormVersion createDraft(Long formId, Map<String, Object> schema, String changeNote) {
        MetaForm form = formRepository.findById(formId)
                .orElseThrow(() -> new ResourceNotFoundException("Form not found: " + formId));
        int next = versionRepository.findFirstByFormIdOrderByVersionNoDesc(formId)
                .map(v -> v.getVersionNo() + 1).orElse(1);
        MetaFormVersion version = versionRepository.save(MetaFormVersion.builder()
                .form(form)
                .versionNo(next)
                .status(MetaFormVersion.Status.DRAFT.name())
                .schemaJson(schema == null ? new HashMap<>() : new HashMap<>(schema))
                .changeNote(changeNote)
                .build());
        audit.record("FORM_VERSION", version.getId(), "DRAFT_CREATED", null,
                Map.of("form", form.getCode(), "versionNo", next));
        return version;
    }

    /** Publishes a draft and archives the previously published version. */
    @Transactional
    public MetaFormVersion publish(Long versionId) {
        MetaFormVersion version = versionRepository.findWithFormById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Form version not found: " + versionId));
        if (MetaFormVersion.Status.PUBLISHED.name().equals(version.getStatus())) {
            return version;
        }
        versionRepository.findFirstByFormIdAndStatusOrderByVersionNoDesc(
                        version.getForm().getId(), MetaFormVersion.Status.PUBLISHED.name())
                .ifPresent(previous -> {
                    previous.setStatus(MetaFormVersion.Status.ARCHIVED.name());
                    versionRepository.save(previous);
                });
        version.setStatus(MetaFormVersion.Status.PUBLISHED.name());
        version.setPublishedAt(Instant.now());
        version.setPublishedBy(CurrentActor.id());
        MetaFormVersion saved = versionRepository.save(version);

        audit.record("FORM_VERSION", versionId, "PUBLISHED", null,
                Map.of("form", version.getForm().getCode(), "versionNo", version.getVersionNo()));
        eventPublisher.publishEvent(new MetadataDomainEvent(
                "meta-form-" + versionId, MetadataDomainEvent.FORM_PUBLISHED,
                version.getForm().getEntity().getModuleCode(),
                version.getForm().getEntity().getEntityCode(), versionId,
                Map.of("versionNo", version.getVersionNo()), Instant.now()));
        return saved;
    }

    @Transactional(readOnly = true)
    public MetaFormVersion publishedVersion(String formCode) {
        MetaForm form = formRepository.findByCode(formCode)
                .orElseThrow(() -> new ResourceNotFoundException("Form not found: " + formCode));
        return versionRepository.findFirstByFormIdAndStatusOrderByVersionNoDesc(
                        form.getId(), MetaFormVersion.Status.PUBLISHED.name())
                .orElseThrow(() -> new ApiException(ErrorCode.BAD_REQUEST,
                        "Form has no published version: " + formCode));
    }

    @Transactional(readOnly = true)
    public List<MetaFormVersion> versions(Long formId) {
        return versionRepository.findByFormIdOrderByVersionNoDesc(formId);
    }

    @Transactional
    public MetaFormLayout addLayout(Long versionId, String targetType, String targetValue,
                                    Map<String, Object> layout, Integer priority) {
        versionRepository.findById(versionId)
                .orElseThrow(() -> new ResourceNotFoundException("Form version not found: " + versionId));
        MetaFormLayout saved = layoutRepository.save(MetaFormLayout.builder()
                .formVersionId(versionId)
                .targetType(targetType == null ? MetaFormLayout.TargetType.DEFAULT.name() : targetType.toUpperCase())
                .targetValue(targetValue)
                .layoutJson(layout == null ? new HashMap<>() : new HashMap<>(layout))
                .priority(priority == null ? 100 : priority)
                .build());
        audit.record("LAYOUT", saved.getId(), "CREATED", null,
                Map.of("targetType", saved.getTargetType(), "targetValue", String.valueOf(targetValue)));
        eventPublisher.publishEvent(new MetadataDomainEvent(
                "meta-layout-" + saved.getId(), MetadataDomainEvent.LAYOUT_CHANGED,
                null, null, saved.getId(), Map.of("targetType", saved.getTargetType()), Instant.now()));
        return saved;
    }

    /**
     * Picks the layout for a render context. The most specific match wins:
     * STAGE → DEVICE → BRANCH → COMPANY → ROLE → DEFAULT, then lowest priority.
     */
    @Transactional(readOnly = true)
    public MetaFormLayout resolveLayout(Long versionId, Map<String, String> context) {
        List<MetaFormLayout> layouts = layoutRepository.findByFormVersionIdOrderByPriorityAsc(versionId);
        List<String> order = List.of("STAGE", "DEVICE", "BRANCH", "COMPANY", "ROLE");
        for (String targetType : order) {
            String wanted = context == null ? null : context.get(targetType.toLowerCase());
            if (wanted == null) {
                continue;
            }
            for (MetaFormLayout layout : layouts) {
                if (layout.getTargetType().equals(targetType) && wanted.equals(layout.getTargetValue())) {
                    return layout;
                }
            }
        }
        return layouts.stream()
                .filter(l -> MetaFormLayout.TargetType.DEFAULT.name().equals(l.getTargetType()))
                .findFirst()
                .orElse(layouts.isEmpty() ? null : layouts.get(0));
    }

    /** Binds a record to the form version it was captured with. */
    @Transactional
    public MetaRecordFormVersion bindRecord(String moduleCode, String entityCode, Long recordId, Long versionId) {
        MetaRecordFormVersion binding = recordVersionRepository
                .findByModuleCodeAndEntityCodeAndRecordId(moduleCode, entityCode, recordId)
                .orElseGet(() -> MetaRecordFormVersion.builder()
                        .moduleCode(moduleCode).entityCode(entityCode).recordId(recordId).build());
        binding.setFormVersionId(versionId);
        binding.setCapturedAt(Instant.now());
        return recordVersionRepository.save(binding);
    }

    /** The schema a historical record must still be rendered with. */
    @Transactional(readOnly = true)
    public MetaFormVersion versionForRecord(String moduleCode, String entityCode, Long recordId) {
        return recordVersionRepository
                .findByModuleCodeAndEntityCodeAndRecordId(moduleCode, entityCode, recordId)
                .flatMap(b -> versionRepository.findWithFormById(b.getFormVersionId()))
                .orElse(null);
    }
}
