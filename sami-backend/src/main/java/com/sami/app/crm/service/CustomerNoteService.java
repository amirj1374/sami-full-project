package com.sami.app.crm.service;

import com.sami.app.common.exception.ApiException;
import com.sami.app.common.exception.ErrorCode;
import com.sami.app.common.exception.ResourceNotFoundException;
import com.sami.app.crm.domain.Customer;
import com.sami.app.crm.domain.CustomerNote;
import com.sami.app.crm.dto.NoteDtos.NoteRequest;
import com.sami.app.crm.dto.NoteDtos.NoteResponse;
import com.sami.app.crm.repository.CustomerNoteRepository;
import com.sami.app.crm.repository.CustomerRepository;
import com.sami.app.security.CurrentActor;
import com.sami.app.common.tenancy.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Customer notes. PRIVATE notes are only ever returned to their author; only
 * the author may delete their own note (attachments are a future extension via
 * the FileStorage port).
 */
@Service
@RequiredArgsConstructor
public class CustomerNoteService {

    private final CustomerNoteRepository noteRepository;
    private final CustomerRepository customerRepository;
    private final CustomerEventService events;
    private final TenantContext tenantContext;

    @Transactional(readOnly = true)
    public Page<NoteResponse> list(Long customerId, Pageable pageable) {
        requireCustomer(customerId);
        Long tenantId = tenantContext.requireTenantId();
        Long viewerId = CurrentActor.id();
        return noteRepository.findVisible(tenantId, customerId, viewerId == null ? -1L : viewerId,
                        CustomerNote.Visibility.PUBLIC, pageable)
                .map(NoteResponse::from);
    }

    @Transactional
    public NoteResponse add(Long customerId, NoteRequest request) {
        requireCustomer(customerId);
        Long tenantId = tenantContext.requireTenantId();
        CustomerNote note = noteRepository.save(CustomerNote.builder()
                .tenantId(tenantId)
                .customer(customerRepository.getReferenceById(customerId))
                .authorId(CurrentActor.id())
                .authorEmail(CurrentActor.email())
                .priority(request.priority())
                .visibility(request.visibility())
                .body(request.body().trim())
                .build());
        events.record(customerId, CustomerEventService.NOTE_ADDED, "Note added",
                Map.of("priority", request.priority().name(),
                        "visibility", request.visibility().name()));
        return NoteResponse.from(note);
    }

    /** Authors may delete their own notes; nobody else's. */
    @Transactional
    public void delete(Long customerId, Long noteId) {
        CustomerNote note = noteRepository.findByIdAndTenantId(noteId, tenantContext.requireTenantId())
                .orElseThrow(() -> ResourceNotFoundException.of("Note", noteId));
        if (!note.getCustomer().getId().equals(customerId)) {
            throw ResourceNotFoundException.of("Note", noteId);
        }
        Long actorId = CurrentActor.id();
        if (note.getAuthorId() != null && !note.getAuthorId().equals(actorId)) {
            throw new ApiException(ErrorCode.OPERATION_NOT_ALLOWED,
                    "Only the author can delete a note");
        }
        noteRepository.delete(note);
    }

    private Customer requireCustomer(Long id) {
        return customerRepository.findByIdAndTenantId(id, tenantContext.requireTenantId())
                .orElseThrow(() -> ResourceNotFoundException.of("Customer", id));
    }
}
