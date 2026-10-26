package com.smartinventory.inventoryservice.service.mapper;

import com.smartinventory.inventoryservice.dto.PurchaseItemResponseDTO;
import com.smartinventory.inventoryservice.dto.PurchaseResponseDTO;
import com.smartinventory.inventoryservice.dto.SupplierSummaryDTO;
import com.smartinventory.inventoryservice.entity.Purchase;
import com.smartinventory.inventoryservice.entity.PurchaseItem;
import com.smartinventory.inventoryservice.entity.Supplier;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Component for manual mapping between Purchase entities and DTOs.
 * Uses manual reflection-free mapping for maximum performance and type safety.
 */
@Component
public class PurchaseMapper {

    /**
     * Converts a Purchase entity to a PurchaseResponseDTO.
     *
     * @param purchase Source purchase entity
     * @return PurchaseResponseDTO or null if source is null
     */
    public PurchaseResponseDTO toResponseDTO(Purchase purchase) {
        if (purchase == null) {
            return null;
        }

        SupplierSummaryDTO supplierSummary = null;
        String supplierEmail = null;
        if (purchase.getSupplier() != null) {
            Supplier s = purchase.getSupplier();
            supplierEmail = s.getEmail();
            supplierSummary = SupplierSummaryDTO.builder()
                    .id(s.getId())
                    .name(s.getName())
                    .email(s.getEmail())
                    .build();
        }

        List<PurchaseItemResponseDTO> itemDTOs = Collections.emptyList();
        if (purchase.getItems() != null && !purchase.getItems().isEmpty()) {
            itemDTOs = purchase.getItems().stream()
                    .map(this::toItemResponseDTO)
                    .collect(Collectors.toList());
        }

        String purchaseNumber = purchase.getId() != null ? String.format("PO-%05d", purchase.getId()) : null;

        return PurchaseResponseDTO.builder()
                .id(purchase.getId())
                .purchaseNumber(purchaseNumber)
                .supplier(supplierSummary)
                .supplierEmail(supplierEmail)
                .purchaseDate(purchase.getPurchaseDate())
                .totalAmount(purchase.getTotalAmount())
                .status(purchase.getStatus())
                .notes(purchase.getNotes())
                .items(itemDTOs)
                .createdAt(purchase.getCreatedAt())
                .updatedAt(purchase.getUpdatedAt())
                .build();
    }

    /**
     * Converts a PurchaseItem entity to a PurchaseItemResponseDTO.
     *
     * @param item Source purchase item entity
     * @return PurchaseItemResponseDTO or null if source is null
     */
    public PurchaseItemResponseDTO toItemResponseDTO(PurchaseItem item) {
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

        return PurchaseItemResponseDTO.builder()
                .id(item.getId())
                .productId(productId)
                .productName(productName)
                .sku(sku)
                .quantity(item.getQuantity())
                .unitCost(item.getUnitCost())
                .totalCost(item.getTotalCost())
                .build();
    }
}
