package com.controledegastos.backend.investments;
import com.controledegastos.backend.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface TaxOpeningBalanceRepository extends JpaRepository<TaxOpeningBalance, Long> {
    Optional<TaxOpeningBalance> findByUser(User user);
}
