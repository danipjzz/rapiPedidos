package com.rapipedido.api.catalog;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/catalog")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/restaurants/{id}/menu")
    public ResponseEntity<Map<String, Object>> getMenu(@PathVariable("id") Long id) {
        List<DishResponse> menu = catalogService.getMenu(id);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", "SUCCESS");
        body.put("message", "Menú obtenido correctamente");
        body.put("data", menu);
        return ResponseEntity.ok(body);
    }
}