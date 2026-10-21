package com.smartinventory.notificationservice.event;

import java.io.Serializable;

/**
 * Payload carried by LowStockDetected event.
 */
public class LowStockDetectedPayload implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long productId;
    private String productName;
    private String sku;
    private Integer currentQuantity;
    private Integer reorderLevel;
    private Long supplierId;
    private String supplierName;
    private String supplierEmail;

    public LowStockDetectedPayload() {
    }

    public LowStockDetectedPayload(Long productId, String productName, String sku, Integer currentQuantity,
                                  Integer reorderLevel, Long supplierId, String supplierName, String supplierEmail) {
        this.productId = productId;
        this.productName = productName;
        this.sku = sku;
        this.currentQuantity = currentQuantity;
        this.reorderLevel = reorderLevel;
        this.supplierId = supplierId;
        this.supplierName = supplierName;
        this.supplierEmail = supplierEmail;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public Integer getCurrentQuantity() {
        return currentQuantity;
    }

    public void setCurrentQuantity(Integer currentQuantity) {
        this.currentQuantity = currentQuantity;
    }

    public Integer getReorderLevel() {
        return reorderLevel;
    }

    public void setReorderLevel(Integer reorderLevel) {
        this.reorderLevel = reorderLevel;
    }

    public Long getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(Long supplierId) {
        this.supplierId = supplierId;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public String getSupplierEmail() {
        return supplierEmail;
    }

    public void setSupplierEmail(String supplierEmail) {
        this.supplierEmail = supplierEmail;
    }
}
