package com.controledegastos.backend.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Remove em lote somente dados financeiros pertencentes a uma conta.
 */
@Repository
@RequiredArgsConstructor
public class UserFinancialDataResetRepository {

    private final JdbcTemplate jdbcTemplate;

    public void deleteAllByUserId(Long userId) {
        jdbcTemplate.update("DELETE FROM tax_obligations WHERE user_id = ?", userId);
        // As transacoes referenciam movimentacoes e itens da wishlist, por isso saem primeiro.
        jdbcTemplate.update("DELETE FROM transactions WHERE user_id = ?", userId);

        jdbcTemplate.update("DELETE FROM wallet_earnings WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM investment_income_schedules WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM investment_movements WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM investment_positions WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM investment_portfolio_snapshots WHERE user_id = ?", userId);

        jdbcTemplate.update("""
                DELETE FROM investment_goal_contributions
                WHERE goal_id IN (SELECT id FROM investment_goals WHERE user_id = ?)
                """, userId);
        jdbcTemplate.update("DELETE FROM investment_goals WHERE user_id = ?", userId);

        jdbcTemplate.update("""
                DELETE FROM investment_import_items
                WHERE batch_id IN (SELECT id FROM investment_import_batches WHERE user_id = ?)
                """, userId);
        jdbcTemplate.update("DELETE FROM investment_import_batches WHERE user_id = ?", userId);

        jdbcTemplate.update("DELETE FROM tax_payments WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM tax_opening_balances WHERE user_id = ?", userId);

        jdbcTemplate.update("DELETE FROM wishlist_history_entries WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM wishlist_items WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM wishlist_lists WHERE user_id = ?", userId);
    }
}
