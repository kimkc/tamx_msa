package com.example.orderservice.client;


import com.example.orderservice.vo.ResponseCatalog;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


//order-service -> 유레카 -> http://127.0.0.1:50002라고 답을 줌, 그럼 http://127.0.0.1:50002/{userId}/orders 직접 호출
@FeignClient(name = "catalog-service")
public interface CatalogServiceClient {

    @GetMapping(value= "catalogs/{productId}")
    public ResponseCatalog getCatalog(@PathVariable("productId") String productId);
}
