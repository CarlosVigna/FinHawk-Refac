package com.carlos.finhawk_refac.service;


import com.carlos.finhawk_refac.dto.request.CategoryRequestDTO;
import com.carlos.finhawk_refac.dto.response.CategoryResponseDTO;
import com.carlos.finhawk_refac.entity.Category;
import com.carlos.finhawk_refac.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public CategoryResponseDTO create (CategoryRequestDTO dto){
        Category newCategory = new Category();
                newCategory.setName(dto.name());
                newCategory.setType(dto.type());

        Category saved = categoryRepository.save(newCategory);

        return new CategoryResponseDTO(
                saved.getId(),
                saved.getName(),
                saved.getType()
        );
    }

    public CategoryResponseDTO update (Long id, CategoryRequestDTO dto){
        Category oldCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        oldCategory.setName(dto.name());
        oldCategory.setType(dto.type());

        Category updated = categoryRepository.save(oldCategory);

        return new CategoryResponseDTO(
                updated.getId(),
                updated.getName(),
                updated.getType()
        );
    }


    public List<CategoryResponseDTO> getAll(){
        return categoryRepository.findAll()
                .stream().map(category -> new CategoryResponseDTO(
                        category.getId(),
                        category.getName(),
                        category.getType()
                )).toList();
    }


    public CategoryResponseDTO getById (Long id){
        Category category = categoryRepository.findById(id).orElseThrow(() -> new RuntimeException("Category not found"));

        return new CategoryResponseDTO(
                category.getId(),
                category.getName(),
                category.getType()
        );
    }

    public void delete(Long id){
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));

        categoryRepository.delete(category);
    }

}
