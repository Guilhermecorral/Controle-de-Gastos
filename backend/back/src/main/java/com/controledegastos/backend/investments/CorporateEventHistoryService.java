package com.controledegastos.backend.investments;

import com.controledegastos.backend.config.ResourceNotFoundException;
import com.controledegastos.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Read-only historical research path; it deliberately does not publish Agenda entries. */
@Service
@RequiredArgsConstructor
public class CorporateEventHistoryService {
    private final InvestmentPositionRepository positionRepository;
    private final B3HistoricalCashDividendProvider historicalProvider;
    private final CorporateEventPilotAccess pilotAccess;
    private final com.controledegastos.backend.security.AuthenticatedUserService authenticatedUserService;

    @Transactional(readOnly = true)
    public List<B3HistoricalCashDividendProvider.HistoricalCashDividend> history(String rawSymbol) {
        User user = authenticatedUserService.getAuthenticatedUser();
        pilotAccess.require(user);
        String symbol = AssetResolver.normalizeSymbol(rawSymbol);
        InvestmentPosition position = positionRepository.findAllByUserOrderByCreatedAtDesc(user).stream()
                .filter(item -> symbol.equals(AssetResolver.normalizeSymbol(item.getSymbol())))
                .findFirst().orElseThrow(() -> new ResourceNotFoundException("O ativo não está na carteira atual"));
        InstrumentCatalog.Instrument instrument = InstrumentCatalog.find(symbol, position.getAssetType())
                .filter(item -> item.capabilities().supportsHistoricalQuote())
                .orElseThrow(() -> new IllegalArgumentException("O histórico de proventos ainda não é suportado para este ativo"));
        return historicalProvider.history(instrument);
    }
}
