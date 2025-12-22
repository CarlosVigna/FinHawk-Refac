package com.carlos.finhawk_refac.controller;

import com.carlos.finhawk_refac.dto.request.CategoryRequestDTO;
import com.carlos.finhawk_refac.dto.response.CategoryResponseDTO;
import com.carlos.finhawk_refac.service.CategoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/category")
public class CategoryController {

    public final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    public ResponseEntity<CategoryResponseDTO> create (@RequestBody CategoryRequestDTO dto){
        CategoryResponseDTO newCategory = categoryService.create(dto);
        return ResponseEntity.ok(newCategory);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponseDTO> update (@PathVariable Long id, @RequestBody CategoryRequestDTO dto){
        CategoryResponseDTO updated = categoryService.update(id, dto);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponseDTO> getById (@PathVariable Long id){
        CategoryResponseDTO category = categoryService.getById(id);
        return ResponseEntity.ok(category);
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponseDTO>> getAll(){
        List<CategoryResponseDTO> categories = categoryService.getAll();
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<CategoryResponseDTO>> getAllByAccountId(@PathVariable Long accountId) {
        List<CategoryResponseDTO> categories = categoryService.getAllByAccountId(accountId);
        return ResponseEntity.ok(categories);
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id){
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

