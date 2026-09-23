package com.capacitacion.loanapp.api.controller;

import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    /**
     * Retorna los tipos de productos de préstamo disponibles.
     * En producción esto vendría de la base de datos.
     */
    @GetMapping
    public List<Map<String, Object>> getProducts() {
        return List.of(
            Map.ofEntries(
                   Map.entry("id", 1), Map.entry("name", "Préstamo Personal"),
                   Map.entry("description", "Para gastos personales y emergencias"),
                   Map.entry("minAmount", 1000), Map.entry("maxAmount", 20000),
                   Map.entry("maxTermMonths", 60), Map.entry("monthlyRate", 1.10),
                   Map.entry("imageUrl", "https://picsum.photos/seed/personal/400/300"),
                   Map.entry("featured", false), Map.entry("active", true), Map.entry("inStock", true)),
            Map.ofEntries(
                   Map.entry("id", 2), Map.entry("name", "Préstamo de Consumo"),
                   Map.entry("description", "Para compras de bienes y servicios"),
                   Map.entry("minAmount", 5000), Map.entry("maxAmount", 50000),
                   Map.entry("maxTermMonths", 120), Map.entry("monthlyRate", 0.95),
                   Map.entry("imageUrl", "https://picsum.photos/seed/consumo/400/300"),
                   Map.entry("featured", true), Map.entry("active", true), Map.entry("inStock", true)),
            Map.ofEntries(
                   Map.entry("id", 3), Map.entry("name", "Préstamo Hipotecario"),
                   Map.entry("description", "Para adquisición de vivienda"),
                   Map.entry("minAmount", 50000), Map.entry("maxAmount", 500000),
                   Map.entry("maxTermMonths", 360), Map.entry("monthlyRate", 0.85),
                   Map.entry("imageUrl", "https://picsum.photos/seed/hipoteca/400/300"),
                   Map.entry("featured", true), Map.entry("active", true), Map.entry("inStock", true)),
            Map.ofEntries(
                   Map.entry("id", 4), Map.entry("name", "Préstamo Vehicular"),
                   Map.entry("description", "Financiamiento de vehículos nuevos y usados"),
                   Map.entry("minAmount", 10000), Map.entry("maxAmount", 80000),
                   Map.entry("maxTermMonths", 84), Map.entry("monthlyRate", 0.90),
                   Map.entry("imageUrl", "https://picsum.photos/seed/vehiculo/400/300"),
                   Map.entry("featured", false), Map.entry("active", true), Map.entry("inStock", false))
        );
    }
}
