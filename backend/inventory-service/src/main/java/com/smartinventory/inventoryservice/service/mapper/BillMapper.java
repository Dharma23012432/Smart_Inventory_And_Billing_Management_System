package com.smartinventory.inventoryservice.service.mapper;

import com.smartinventory.inventoryservice.dto.BillItemResponseDTO;
import com.smartinventory.inventoryservice.dto.BillResponseDTO;
import com.smartinventory.inventoryservice.dto.CustomerSummaryDTO;
import com.smartinventory.inventoryservice.entity.Bill;
import com.smartinventory.inventoryservice.entity.BillItem;
import com.smartinventory.inventoryservice.entity.Customer;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Component for manual mapping between Bill entities and DTOs.
 * Uses manual reflection-free mapping for maximum performance and type safety.
 */
@Component
public class BillMapper {

    /**
     * Converts a Bill entity into a full BillResponseDTO.
     *
     * @param bill Source Bill entity
     * @return BillResponseDTO or null if bill is null
     */
    public BillResponseDTO toResponseDTO(Bill bill) {
        if (bill == null) {
            return null;
        }

        CustomerSummaryDTO customerSummary = null;
        if (bill.getCustomer() != null) {
            Customer c = bill.getCustomer();
            customerSummary = CustomerSummaryDTO.builder()
                    .id(c.getId())
                    .name(c.getName())
                    .email(c.getEmail())
                    .build();
        }

        List<BillItemResponseDTO> itemDTOs = Collections.emptyList();
        if (bill.getItems() != null && !bill.getItems().isEmpty()) {
            itemDTOs = bill.getItems().stream()
                    .map(this::toItemResponseDTO)
                    .collect(Collectors.toList());
        }

        return BillResponseDTO.builder()
                .id(bill.getId())
                .billNumber(bill.getBillNumber())
                .customer(customerSummary)
                .billDate(bill.getBillDate())
                .subtotal(bill.getSubtotal())
                .discountAmount(bill.getDiscountAmount())
                .taxAmount(bill.getTaxAmount())
                .totalAmount(bill.getTotalAmount())
                .status(bill.getStatus())
                .invoiceSent(bill.getInvoiceSent())
                .notes(bill.getNotes())
                .items(itemDTOs)
                .createdAt(bill.getCreatedAt())
                .updatedAt(bill.getUpdatedAt())
                .build();
    }

    /**
     * Converts a BillItem entity into a BillItemResponseDTO.
     *
     * @param item Source BillItem entity
     * @return BillItemResponseDTO or null if item is null
     */
    public BillItemResponseDTO toItemResponseDTO(BillItem item) {
        if (item == null) {
            return null;
        }

        Long productId = null;
        String productName = null;
        String sku = null;

        if (item.getProduct() != null) {
            productId = item.getProduct().getId();
            productName = item.getProduct().getName();
            sku = item.getProduct().getSku();
        }

        return BillItemResponseDTO.builder()
                .id(item.getId())
                .productId(productId)
                .productName(productName)
                .sku(sku)
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .discountPercent(item.getDiscountPercent())
                .totalPrice(item.getTotalPrice())
                .build();
    }
}
