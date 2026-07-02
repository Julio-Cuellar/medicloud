package com.jclinical.attachments.web;

import com.jclinical.attachments.AttachmentEntity;
import com.jclinical.attachments.AttachmentService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/patients/{patientId}/attachments")
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService attachmentService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AttachmentResponse> upload(
            @PathVariable UUID patientId,
            @RequestParam UUID clinicId,
            @RequestParam String elementId,
            @RequestPart("file") MultipartFile file
    ) {
        AttachmentEntity saved = attachmentService.store(clinicId, patientId, elementId, file);
        return ResponseEntity.status(201).body(toResponse(saved));
    }

    @GetMapping
    public ResponseEntity<List<AttachmentResponse>> list(
            @PathVariable UUID patientId,
            @RequestParam String elementId
    ) {
        List<AttachmentResponse> responses = attachmentService.listByPatientAndElement(patientId, elementId).stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{attachmentId}/content")
    public ResponseEntity<byte[]> content(@PathVariable UUID patientId, @PathVariable UUID attachmentId) {
        AttachmentEntity attachment = attachmentService.get(attachmentId, patientId);
        byte[] bytes = attachmentService.readContent(attachment);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(attachment.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + attachment.getOriginalFilename() + "\"")
                .body(bytes);
    }

    @DeleteMapping("/{attachmentId}")
    public ResponseEntity<Void> delete(@PathVariable UUID patientId, @PathVariable UUID attachmentId) {
        attachmentService.delete(attachmentId, patientId);
        return ResponseEntity.noContent().build();
    }

    private AttachmentResponse toResponse(AttachmentEntity entity) {
        return new AttachmentResponse(
                entity.getId(),
                entity.getOriginalFilename(),
                entity.getContentType(),
                entity.getSizeBytes(),
                entity.getCreatedAt()
        );
    }
}
