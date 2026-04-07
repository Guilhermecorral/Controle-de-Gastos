package com.controledegastos.backend.transactions.DTO;

import com.controledegastos.backend.transactions.Transaction.TransactionCategory;
import com.controledegastos.backend.transactions.Transaction.TransactionType;
import com.controledegastos.backend.transactions.Transaction.PaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;


public record TransactionResponseDTO (

   Long id,
   TransactionType type,
   String description,
   TransactionCategory category,
   BigDecimal amount,
   PaymentMethod paymentMethod,
   LocalDate transactionDate,
   LocalDateTime createdAt

) {}
