package com.sami.app.purchasing.service;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.common.storage.FileStorage;
import com.sami.app.purchasing.domain.Purchase;
import com.sami.app.purchasing.domain.PurchaseAttachment;
import com.sami.app.purchasing.dto.PurchaseDtos.AttachmentResponse;
import com.sami.app.purchasing.repository.PurchaseAttachmentRepository;
import com.sami.app.security.CurrentActor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Unlimited purchase attachments (invoices, receipts, quotes, photos, any
 * document) behind the {@code FileStorage} port — any content type; size is
 * bounded by the servlet multipart limits.
 */
@Service
@RequiredArgsConstructor
public class PurchaseAttachmentService {

    private final PurchaseService purchaseService;
    private final PurchaseAttachmentRepository attachmentRepository;
    private final FileStorage fileStorage;
    private final PurchaseLogService logs;

    @Transactional(readOnly = true)
    public List<AttachmentResponse> list(Long purchaseId) {
        return attachmentRepository.findByPurchaseIdOrderByCreatedAtDesc(purchaseId).stream()
                .map(AttachmentResponse::from)
                .toList();
    }

    @Transactional
    public AttachmentResponse upload(Long purchaseId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "A file is required");
        }
        Purchase purchase = purchaseService.findWithDetailsOrThrow(purchaseId);

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "Could not read the uploaded file");
        }
        String key = fileStorage.store("purchase-attachments/" + purchaseId, content,
                file.getContentType());

        PurchaseAttachment attachment = attachmentRepository.save(PurchaseAttachment.builder()
                .purchase(purchase)
                .fileKey(key)
                .fileName(file.getOriginalFilename() == null ? "attachment"
                        : file.getOriginalFilename())
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .uploadedBy(CurrentActor.id())
                .uploadedByEmail(CurrentActor.email())
                .build());

        logs.record(purchase, PurchaseLogService.ATTACHMENT_ADDED, "Attachment added",
                Map.of("fileName", attachment.getFileName()));
        return AttachmentResponse.from(attachment);
    }

    @Transactional(readOnly = true)
    public Optional<DownloadedFile> download(Long purchaseId, Long attachmentId) {
        return attachmentRepository.findById(attachmentId)
                .filter(a -> a.getPurchase().getId().equals(purchaseId))
                .flatMap(a -> fileStorage.load(a.getFileKey())
                        .map(f -> new DownloadedFile(a.getFileName(),
                                a.getContentType() != null ? a.getContentType() : f.contentType(),
                                f.content())));
    }

    @Transactional
    public void delete(Long purchaseId, Long attachmentId) {
        PurchaseAttachment attachment = attachmentRepository.findById(attachmentId)
                .filter(a -> a.getPurchase().getId().equals(purchaseId))
                .orElseThrow(() -> ResourceNotFoundException.of("Attachment", attachmentId));
        fileStorage.delete(attachment.getFileKey());
        attachmentRepository.delete(attachment);
    }

    public record DownloadedFile(String fileName, String contentType, byte[] content) {
    }
}
