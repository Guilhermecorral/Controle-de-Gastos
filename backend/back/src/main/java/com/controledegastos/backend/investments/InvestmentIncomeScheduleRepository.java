package com.controledegastos.backend.investments;

import com.controledegastos.backend.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvestmentIncomeScheduleRepository extends JpaRepository<InvestmentIncomeSchedule, Long> {
    List<InvestmentIncomeSchedule> findAllByUserOrderByPaymentDateAscCreatedAtDesc(User user);
    Optional<InvestmentIncomeSchedule> findByIdAndUser(Long id, User user);
}
