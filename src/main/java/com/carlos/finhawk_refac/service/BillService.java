package com.carlos.finhawk_refac.service;

import com.carlos.finhawk_refac.dto.request.BillRequestDTO;
import com.carlos.finhawk_refac.dto.response.AccountResponseDTO;
import com.carlos.finhawk_refac.dto.response.BillResponseDTO;
import com.carlos.finhawk_refac.dto.response.CategoryResponseDTO;
import com.carlos.finhawk_refac.entity.Account;
import com.carlos.finhawk_refac.entity.Bill;
import com.carlos.finhawk_refac.entity.Category;
import com.carlos.finhawk_refac.repository.AccountRepository;
import com.carlos.finhawk_refac.repository.BillRepository;
import com.carlos.finhawk_refac.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BillService {

    public final BillRepository billRepository;
    public final CategoryRepository categoryRepository;
    public final AccountRepository accountRepository;


    public BillService(BillRepository billRepository, CategoryRepository categoryRepository, AccountRepository accountRepository) {
        this.billRepository = billRepository;
        this.categoryRepository = categoryRepository;
        this.accountRepository = accountRepository;
    }

    public BillResponseDTO create(BillRequestDTO dto) {

        Category category = categoryRepository.findById(dto.categoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        Account account = accountRepository.findById(dto.accountId())
                .orElseThrow(() -> new RuntimeException("Account not found"));

        Bill newBill = new Bill();
        newBill.setDescription(dto.description());
        newBill.setMaturity(dto.maturity());
        newBill.setEmission(dto.emission());
        newBill.setInstallmentAmount(dto.installmentAmount());
        newBill.setParcelNumber(dto.parcelNumber());
        newBill.setPeriodicity(dto.periodicity());
        newBill.setStatus(dto.status());
        newBill.setCategory(category);
        newBill.setAccount(account);

        Bill saved = billRepository.save(newBill);

        CategoryResponseDTO categoryResponse = new CategoryResponseDTO(
                category.getId(),
                category.getName(),
                category.getType()
        );

        return new BillResponseDTO(
                saved.getId(),
                saved.getDescription(),
                saved.getMaturity(),
                saved.getEmission(),
                saved.getInstallmentAmount(),
                saved.getParcelNumber(),
                saved.getPeriodicity(),
                saved.getStatus(),
                category.getName()
        );
    }

    public BillResponseDTO update(Long id, BillRequestDTO dto){
        Bill oldBill = billRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bill not found"));

        Category category = categoryRepository.findById(dto.categoryId())
                .orElseThrow(() -> new RuntimeException("Category not found"));

        Account account = accountRepository.findById(dto.accountId())
                .orElseThrow(() -> new RuntimeException("Account not found"));

        oldBill.setDescription(dto.description());
        oldBill.setMaturity(dto.maturity());
        oldBill.setEmission(dto.emission());
        oldBill.setInstallmentAmount(dto.installmentAmount());
        oldBill.setParcelNumber(dto.parcelNumber());
        oldBill.setPeriodicity(dto.periodicity());
        oldBill.setStatus(dto.status());
        oldBill.setCategory(category);
        oldBill.setAccount(account);

        Bill updated = billRepository.save(oldBill);

        return new BillResponseDTO(
                updated.getId(),
                updated.getDescription(),
                updated.getMaturity(),
                updated.getEmission(),
                updated.getInstallmentAmount(),
                updated.getParcelNumber(),
                updated.getPeriodicity(),
                updated.getStatus(),
                category.getName()
        );
    }

    public List<BillResponseDTO> getAll(){
        return billRepository.findAll()
                .stream().map(bill -> new BillResponseDTO(
                        bill.getId(),
                        bill.getDescription(),
                        bill.getMaturity(),
                        bill.getEmission(),
                        bill.getInstallmentAmount(),
                        bill.getParcelNumber(),
                        bill.getPeriodicity(),
                        bill.getStatus(),
                        bill.getCategory().getName()
                ))
                .toList();

    }

    public BillResponseDTO getById(Long id){
        Bill bill = billRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bill not found"));

        return new BillResponseDTO(
                bill.getId(),
                bill.getDescription(),
                bill.getMaturity(),
                bill.getEmission(),
                bill.getInstallmentAmount(),
                bill.getParcelNumber(),
                bill.getPeriodicity(),
                bill.getStatus(),
                bill.getCategory().getName()
        );

    }

    public void delete(Long id){
        Bill bill = billRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Bill not found"));

        billRepository.delete(bill);
    }

}

