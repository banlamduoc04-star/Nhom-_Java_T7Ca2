package com.oism.backend.service;

import com.oism.backend.dto.product.SkuRequest;
import com.oism.backend.dto.product.SkuResponse;

import java.util.List;

public interface SkuService {

    SkuResponse addSkuToProduct(Long productId, SkuRequest request);

    List<SkuResponse> getSkusByProductId(Long productId);

    SkuResponse getSkuByBarcode(String barcode);

    SkuResponse updateSku(Long skuId, SkuRequest request);

    void deleteSku(Long skuId);
}
