package com.dci.clearance.service;

import com.dci.clearance.dto.AttachmentRequest;
import com.dci.clearance.entity.Attachment;
import com.dci.clearance.repository.AttachmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;

    public List<Attachment> getAll() {
        return attachmentRepository.findAll();
    }

    public Attachment getById(Long id) {
        return attachmentRepository.findById(id).orElse(null);
    }

    public Attachment create(AttachmentRequest request) {
        Attachment attachment = Attachment.builder()
                .referenceNumber(request.getReferenceNumber())
                .crAttachment(request.getCrAttachment())
                .plateCertificationAttachment(request.getPlateCertificationAttachment())
                .actualPlateAttachment(request.getActualPlateAttachment())
                .build();

        return attachmentRepository.save(attachment);
    }

    public Attachment update(Long id, AttachmentRequest request) {
        Attachment attachment = attachmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Attachment not found"));

        attachment.setReferenceNumber(request.getReferenceNumber());
        attachment.setCrAttachment(request.getCrAttachment());
        attachment.setPlateCertificationAttachment(request.getPlateCertificationAttachment());
        attachment.setActualPlateAttachment(request.getActualPlateAttachment());

        return attachmentRepository.save(attachment);
    }

    public void delete(Long id) {
        attachmentRepository.deleteById(id);
    }

    public Attachment uploadAttachment(
            String referenceNumber,
            String requestedBy,  // Parameter kept for API compatibility but not stored
            MultipartFile crAttachment,
            MultipartFile plateCertificationAttachment,
            MultipartFile actualPlateAttachment
    ) throws IOException {

        Attachment attachment = Attachment.builder()
                .referenceNumber(referenceNumber)
                .crAttachment(getBytes(crAttachment))
                .plateCertificationAttachment(getBytes(plateCertificationAttachment))
                .actualPlateAttachment(getBytes(actualPlateAttachment))
                .build();

        return attachmentRepository.save(attachment);
    }

    private byte[] getBytes(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }
        return file.getBytes();
    }


}