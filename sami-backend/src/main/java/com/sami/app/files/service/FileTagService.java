package com.sami.app.files.service;

import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.files.domain.FileTag;
import com.sami.app.files.domain.FileTagLink;
import com.sami.app.files.repository.FileTagLinkRepository;
import com.sami.app.files.repository.FileTagRepository;
import com.sami.app.files.repository.ManagedFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Tag assignment and tag-based lookup. */
@Service
@RequiredArgsConstructor
public class FileTagService {

    private final FileTagRepository tagRepository;
    private final FileTagLinkRepository linkRepository;
    private final ManagedFileRepository fileRepository;
    private final FileAuditService auditService;

    @Transactional(readOnly = true)
    public List<FileTag> all() {
        return tagRepository.findAllByOrderByDisplayOrderAsc();
    }

    @Transactional(readOnly = true)
    public List<FileTag> tagsOf(Long fileId) {
        List<Long> tagIds = linkRepository.findAllByIdFileId(fileId).stream()
                .map(l -> l.getId().getTagId())
                .toList();
        return tagIds.isEmpty() ? List.of() : tagRepository.findAllByIdIn(tagIds);
    }

    /** Replaces the file's tag set. */
    @Transactional
    public List<FileTag> assign(UUID fileUuid, List<Long> tagIds) {
        Long fileId = fileRepository.findByFileUuid(fileUuid)
                .orElseThrow(() -> ResourceNotFoundException.of("File", fileUuid))
                .getId();

        linkRepository.deleteAllByIdFileId(fileId);
        for (Long tagId : tagIds) {
            tagRepository.findById(tagId)
                    .orElseThrow(() -> ResourceNotFoundException.of("Tag", tagId));
            linkRepository.save(FileTagLink.builder()
                    .id(FileTagLink.Key.builder().fileId(fileId).tagId(tagId).build())
                    .build());
        }
        auditService.record("file", fileId, "Updated", null, Map.of("tags", tagIds));
        return tagsOf(fileId);
    }

    @Transactional(readOnly = true)
    public List<Long> fileIdsWithTags(List<Long> tagIds) {
        return tagIds == null || tagIds.isEmpty() ? List.of() : linkRepository.findFileIdsByTagIds(tagIds);
    }
}
