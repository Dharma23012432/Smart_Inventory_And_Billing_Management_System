package com.smartinventory.notificationservice.service.mapper;

import com.smartinventory.notificationservice.dto.NotificationApprovalResponseDTO;
import com.smartinventory.notificationservice.dto.NotificationResponseDTO;
import com.smartinventory.notificationservice.entity.Notification;
import org.springframework.stereotype.Component;

/**
 * Manual mapper for converting Notification entities to DTOs.
 * Free of reflection and third-party mapping libraries.
 */
@Component
public class NotificationMapper {

    /**
     * Converts a Notification entity to NotificationResponseDTO.
     *
     * @param entity Notification entity
     * @return NotificationResponseDTO or null
     */
    public NotificationResponseDTO toDTO(Notification entity) {
        if (entity == null) {
            return null;
        }

        NotificationResponseDTO dto = new NotificationResponseDTO();
        dto.setId(entity.getId());
        dto.setType(entity.getType());
        dto.setStatus(entity.getStatus());
        dto.setRecipientEmail(entity.getRecipientEmail());
        dto.setSubject(entity.getSubject());
        dto.setBody(entity.getBody());
        dto.setReferenceId(entity.getReferenceId());
        dto.setReferenceType(entity.getReferenceType());
        dto.setErrorMessage(entity.getErrorMessage());
        dto.setSentAt(entity.getSentAt());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());

        return dto;
    }

    /**
     * Converts a Notification entity to NotificationApprovalResponseDTO.
     *
     * @param entity Notification entity
     * @return NotificationApprovalResponseDTO or null
     */
    public NotificationApprovalResponseDTO toApprovalDTO(Notification entity) {
        if (entity == null) {
            return null;
        }

        NotificationApprovalResponseDTO dto = new NotificationApprovalResponseDTO();
        dto.setId(entity.getId());
        dto.setStatus(entity.getStatus());
        dto.setRecipientEmail(entity.getRecipientEmail());
        dto.setSentAt(entity.getSentAt());

        return dto;
    }
}
