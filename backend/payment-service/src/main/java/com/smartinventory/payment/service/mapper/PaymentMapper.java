package com.smartinventory.payment.service.mapper;

import com.smartinventory.payment.dto.PaymentResponseDTO;
import com.smartinventory.payment.entity.Payment;
import org.springframework.stereotype.Component;

/**
 * Manual mapper for converting between {@link Payment} entity and {@link PaymentResponseDTO}.
 * Uses zero reflection libraries for high performance and type safety.
 */
@Component
public class PaymentMapper {

    /**
     * Converts a Payment entity to a PaymentResponseDTO.
     *
     * @param payment the Payment entity
     * @return PaymentResponseDTO or null if payment is null
     */
    public PaymentResponseDTO toDTO(Payment payment) {
        if (payment == null) {
            return null;
        }

        PaymentResponseDTO dto = new PaymentResponseDTO();
        dto.setPaymentId(payment.getId());
        if (payment.getBill() != null) {
            dto.setBillId(payment.getBill().getId());
            dto.setBillNumber(payment.getBill().getBillNumber());
        }
        dto.setAmountPaid(payment.getAmountPaid());
        dto.setPaymentMethod(payment.getPaymentMethod());
        dto.setReferenceNumber(payment.getReferenceNumber());
        dto.setStatus(payment.getStatus());
        dto.setPaymentDate(payment.getPaymentDate());

        return dto;
    }
}
