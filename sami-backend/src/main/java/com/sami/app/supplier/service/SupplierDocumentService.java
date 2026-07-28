package com.sami.app.supplier.service;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.common.storage.FileStorage;
import com.sami.app.security.CurrentActor;
import com.sami.app.supplier.domain.SupDocument;
import com.sami.app.supplier.domain.Supplier;
import com.sami.app.supplier.dto.SupplierDtos.DocumentResponse;
import com.sami.app.supplier.repository.SupDocumentRepository;
import com.sami.app.supplier.repository.SupDocumentTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Supplier documents (contracts, licenses, price lists, any file type) behind
 * the {@code FileStorage} port, tagged with a configurable document type.
 */
@Service
@RequiredArgsConstructor
public class SupplierDocumentService {

    private final SupplierService supplierService;
    private final SupDocumentRepository documentRepository;
    private final SupDocumentTypeRepository documentTypeRepository;
    private final FileStorage fileStorage;
    private final SupLogService logs;

    @Transactional(readOnly = true)
    public List<DocumentResponse> list(Long supplierId) {
        return documentRepository.findBySupplierIdOrderByCreatedAtDesc(supplierId).stream()
                .map(DocumentResponse::from)
                .toList();
    }

    @Transactional
    public DocumentResponse upload(Long supplierId, Long docTypeId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "A file is required");
        }
        Supplier supplier = supplierService.findOrThrow(supplierId);

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new ApiException(ErrorCode.BAD_REQUEST, "Could not read the uploaded file");
        }
        String key = fileStorage.store("supplier-documents/" + supplierId, content,
                file.getContentType());

        SupDocument document = documentRepository.save(SupDocument.builder()
                .supplier(supplier)
                .docType(docTypeId == null ? null : documentTypeRepository.findById(docTypeId)
                        .orElseThrow(() -> ResourceNotFoundException.of("Document type", docTypeId)))
                .fileKey(key)
                .fileName(file.getOriginalFilename() == null ? "document"
                        : file.getOriginalFilename())
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .uploadedBy(CurrentActor.id())
                .uploadedByEmail(CurrentActor.email())
                .build());

        logs.record(supplier, SupLogService.DOCUMENT_ADDED, "Document added",
                Map.of("fileName", document.getFileName()));
        return DocumentResponse.from(document);
    }

    @Transactional(readOnly = true)
    public Optional<DownloadedFile> download(Long supplierId, Long documentId) {
        return documentRepository.findById(documentId)
                .filter(d -> d.getSupplier().getId().equals(supplierId))
                .flatMap(d -> fileStorage.load(d.getFileKey())
                        .map(f -> new DownloadedFile(d.getFileName(),
                                d.getContentType() != null ? d.getContentType() : f.contentType(),
                                f.content())));
    }

    @Transactional
    public void delete(Long supplierId, Long documentId) {
        SupDocument document = documentRepository.findById(documentId)
                .filter(d -> d.getSupplier().getId().equals(supplierId))
                .orElseThrow(() -> ResourceNotFoundException.of("Document", documentId));
        fileStorage.delete(document.getFileKey());
        documentRepository.delete(document);
    }

    public record DownloadedFile(String fileName, String contentType, byte[] content) {
    }
}
