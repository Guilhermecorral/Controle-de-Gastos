package com.controledegastos.backend.admin;

import com.controledegastos.backend.user.Repository.UserRepository;
import com.controledegastos.backend.user.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserFinancialDataResetRepositoryIntegrationTest {

    @Autowired
    private UserFinancialDataResetRepository resetRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldDeleteEveryFinancialAggregateAndPreserveAccountAndSecurityData() {
        User user = userRepository.saveAndFlush(User.builder()
                .name("Conta de Teste")
                .email("reset-data@farolfinanceiro.online")
                .password("encoded")
                .role(User.Role.ADMIN)
                .active(true)
                .twoFactorEnabled(true)
                .twoFactorSecretEncrypted("encrypted-secret")
                .build());
        Long userId = user.getId();
        LocalDate today = LocalDate.of(2026, 9, 12);
        LocalDateTime now = LocalDateTime.of(2026, 9, 12, 12, 0);

        jdbcTemplate.update("INSERT INTO refresh_tokens (user_id, token_hash, expires_at, created_at) VALUES (?, ?, ?, ?)", userId, "refresh-hash", Timestamp.valueOf(now.plusDays(1)), Timestamp.valueOf(now));
        jdbcTemplate.update("INSERT INTO password_reset_tokens (user_id, token_hash, expires_at, created_at) VALUES (?, ?, ?, ?)", userId, "reset-hash", OffsetDateTime.now().plusDays(1), OffsetDateTime.now());

        jdbcTemplate.update("INSERT INTO wishlist_lists (name, is_default, user_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?)", "Lista", true, userId, Timestamp.valueOf(now), Timestamp.valueOf(now));
        Long listId = id("wishlist_lists", "user_id", userId);
        jdbcTemplate.update("""
                INSERT INTO wishlist_items (description, original_price, discount_percent, final_price, priority, category, status, installments, first_installment_next_month, archived_after_purchase, user_id, wishlist_list_id, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, "Notebook", 1000, 0, 1000, "MEDIA", "COMPRAS", "PENDENTE", 1, false, false, userId, listId, Timestamp.valueOf(now), Timestamp.valueOf(now));
        Long itemId = id("wishlist_items", "user_id", userId);
        jdbcTemplate.update("INSERT INTO wishlist_history_entries (wishlist_item_id, user_id, action_type, description, final_price_snapshot, created_at) VALUES (?, ?, ?, ?, ?, ?)", itemId, userId, "CREATED", "Criado", 1000, Timestamp.valueOf(now));

        jdbcTemplate.update("""
                INSERT INTO investment_positions (user_id, asset_type, symbol, market, currency, name, quantity, average_price, purchase_date, daily_liquidity, iof_applicable, redeemed, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, userId, "ACAO", "PETR4", "BR", "BRL", "Petrobras PN", 10, 30, Date.valueOf(today), false, false, false, Timestamp.valueOf(now), Timestamp.valueOf(now));
        Long positionId = id("investment_positions", "user_id", userId);
        jdbcTemplate.update("""
                INSERT INTO investment_movements (user_id, position_id, movement_type, amount, quantity, unit_price, fees, event_date, automatic, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, userId, positionId, "COMPRA", 300, 10, 30, 0, Date.valueOf(today), false, Timestamp.valueOf(now));
        Long movementId = id("investment_movements", "user_id", userId);
        jdbcTemplate.update("""
                INSERT INTO transactions (user_id, wishlist_item_id, investment_movement_id, type, description, category, amount, payment_method, transaction_date, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, userId, itemId, movementId, "DESPESA", "Compra PETR4", "INVESTIMENTO", 300, "PIX", Date.valueOf(today), Timestamp.valueOf(now));

        jdbcTemplate.update("INSERT INTO corporate_events (symbol, event_type, amount_per_unit, tax_rate, ex_date, payment_date, source, source_reference, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", "PETR4", "DIVIDENDO", 1, 0, Date.valueOf(today), Date.valueOf(today.plusDays(10)), "TEST", "event-reset-test", Timestamp.valueOf(now));
        Long corporateEventId = jdbcTemplate.queryForObject("SELECT id FROM corporate_events WHERE source_reference = ?", Long.class, "event-reset-test");
        jdbcTemplate.update("INSERT INTO wallet_earnings (user_id, position_id, corporate_event_id, quantity_eligible, gross_amount, withheld_amount, net_amount, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", userId, positionId, corporateEventId, 10, 10, 0, 10, "PROVISIONADO", Timestamp.valueOf(now));
        jdbcTemplate.update("INSERT INTO investment_income_schedules (user_id, position_id, income_type, amount_per_unit, tax_rate, payment_date, status, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)", userId, positionId, "DIVIDENDO", 1, 0, Date.valueOf(today.plusDays(10)), "AGUARDANDO", Timestamp.valueOf(now));
        jdbcTemplate.update("INSERT INTO investment_portfolio_snapshots (user_id, snapshot_date, invested_amount, current_value, income_amount) VALUES (?, ?, ?, ?, ?)", userId, Date.valueOf(today), 300, 300, 0);

        jdbcTemplate.update("INSERT INTO investment_goals (user_id, name, target_amount, initial_amount, monthly_contribution, annual_growth_rate, active, created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)", userId, "Reserva", 10000, 100, 500, 0, true, Timestamp.valueOf(now));
        Long goalId = id("investment_goals", "user_id", userId);
        jdbcTemplate.update("INSERT INTO investment_goal_contributions (goal_id, amount, event_date, created_at) VALUES (?, ?, ?, ?)", goalId, 100, Date.valueOf(today), Timestamp.valueOf(now));

        jdbcTemplate.update("INSERT INTO investment_import_batches (user_id, original_filename, source_format, status, created_at) VALUES (?, ?, ?, ?, ?)", userId, "teste.csv", "CSV", "EM_REVISAO", Timestamp.valueOf(now));
        Long batchId = id("investment_import_batches", "user_id", userId);
        jdbcTemplate.update("""
                INSERT INTO investment_import_items (batch_id, source_row, movement_type, asset_type, name, market, currency, quantity, unit_price, brokerage_fee, b3_fee, other_costs, withheld_tax, exchange_rate, event_date, possible_duplicate, warning)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, batchId, 1, "COMPRA", "ACAO", "Petrobras", "BR", "BRL", 10, 30, 0, 0, 0, 0, 1, Date.valueOf(today), false, "");
        jdbcTemplate.update("INSERT INTO tax_opening_balances (user_id, start_date, common_loss, day_trade_loss, fund_loss, common_credit, day_trade_credit, pending_tax, source) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", userId, Date.valueOf(today), 0, 0, 0, 0, 0, 0, "TEST");
        jdbcTemplate.update("INSERT INTO tax_payments (user_id, period, revenue_code, amount, paid_at, due_date, account_label, note) VALUES (?, ?, ?, ?, ?, ?, ?, ?)", userId, "2026-09", "6015", 10, Date.valueOf(today), Date.valueOf(today), "Conta", "Teste");

        resetRepository.deleteAllByUserId(userId);

        assertUserDataCountIsZero(userId);
        assertThat(count("investment_goal_contributions", "goal_id", goalId)).isZero();
        assertThat(count("investment_import_items", "batch_id", batchId)).isZero();
        assertThat(count("users", "id", userId)).isOne();
        assertThat(count("refresh_tokens", "user_id", userId)).isOne();
        assertThat(count("password_reset_tokens", "user_id", userId)).isOne();
        assertThat(count("corporate_events", "id", corporateEventId)).isOne();

        User preserved = userRepository.findById(userId).orElseThrow();
        assertThat(preserved.getRole()).isEqualTo(User.Role.ADMIN);
        assertThat(preserved.isActive()).isTrue();
        assertThat(preserved.isTwoFactorEnabled()).isTrue();
        assertThat(preserved.getTwoFactorSecretEncrypted()).isEqualTo("encrypted-secret");
    }

    private void assertUserDataCountIsZero(Long userId) {
        for (String table : new String[]{
                "transactions", "wallet_earnings", "investment_income_schedules", "investment_movements",
                "investment_positions", "investment_portfolio_snapshots", "investment_goals",
                "investment_import_batches", "tax_payments", "tax_opening_balances",
                "wishlist_history_entries", "wishlist_items", "wishlist_lists"
        }) {
            assertThat(count(table, "user_id", userId)).as(table).isZero();
        }
    }

    private Long id(String table, String column, Long value) {
        return jdbcTemplate.queryForObject("SELECT id FROM " + table + " WHERE " + column + " = ?", Long.class, value);
    }

    private long count(String table, String column, Long value) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ?", Long.class, value);
    }
}
