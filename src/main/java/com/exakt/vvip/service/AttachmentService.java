package com.exakt.vvip.service;

import com.exakt.vvip.dto.AttachmentController;
import com.exakt.vvip.entity.Attachment;
import com.exakt.vvip.repository.AttachmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

    public Attachment create(AttachmentController request) {

        Attachment attachment = Attachment.builder()
                .referenceNumber(request.getReferenceNumber())
                .crAttachment(request.getCrAttachment())
                .plateCertificationAttachment(request.getPlateCertificationAttachment())
                .actualPlateAttachment(request.getActualPlateAttachment())
                .build();

        return attachmentRepository.save(attachment);
    }

    public Attachment update(Long id, AttachmentController request) {

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
}