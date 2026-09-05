package com.controledegastos.backend.investments;
import com.controledegastos.backend.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface TaxPaymentRepository extends JpaRepository<TaxPayment, Long> {
    List<TaxPayment> findAllByUser(User user);
    boolean existsByUserAndPeriodAndRevenueCode(User user, String period, String revenueCode);
}
