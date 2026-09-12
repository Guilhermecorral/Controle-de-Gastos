package com.controledegastos.backend.admin;

import com.controledegastos.backend.transactions.TransactionReceiptStorageService;
import com.controledegastos.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mantem a identidade da conta enquanto remove seu estado financeiro de homologacao.
 */
@Service
@RequiredArgsConstructor
public class UserFinancialDataResetService {

    private final UserFinancialDataResetRepository resetRepository;
    private final TransactionReceiptStorageService receiptStorageService;

    @Transactional
    public void reset(User user) {
        resetRepository.deleteAllByUserId(user.getId());
        receiptStorageService.deleteAllReceipts(user.getId());
    }
}
