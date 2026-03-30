package com.whistleup.backend.controllers;

import com.whistleup.backend.resource.HomeServiceCategoryResource;
import com.whistleup.backend.service.HomeServicesCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/whistleup/service/catalog")
@CrossOrigin("*")
@RequiredArgsConstructor
public class HomeServicesCatalogController {

    private final HomeServicesCatalogService homeServicesCatalogService;

    @GetMapping("/all")
    public ResponseEntity<List<HomeServiceCategoryResource>> getAll() {
        return ResponseEntity.ok(homeServicesCatalogService.getCatalog());
    }
}
