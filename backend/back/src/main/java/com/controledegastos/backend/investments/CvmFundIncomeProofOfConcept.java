package com.controledegastos.backend.investments;

import java.util.Map;
import java.util.Optional;

/** Evidence-only CVM paths for fund-history research; they never enable the automatic Agenda. */
public final class CvmFundIncomeProofOfConcept {
    public record Source(String symbol, InvestmentPosition.AssetType assetType, String datasetUrl,
                         String coverage, boolean supportsAutomaticSchedule, String limitation) {}

    private static final Map<String, Source> SOURCES = Map.of(
            "MXRF11", new Source("MXRF11", InvestmentPosition.AssetType.FII,
                    "https://dados.cvm.gov.br/dados/FII/DOC/INF_MENSAL/DADOS/", "cinco anos", false,
                    "O informe mensal é histórico e pode ser reapresentado; não informa previsão de pagamento para a Agenda."),
            "RURA11", new Source("RURA11", InvestmentPosition.AssetType.FIAGRO,
                    "https://dados.cvm.gov.br/dados/FIAGRO/DOC/INF_MENSAL/DADOS/", "doze meses", false,
                    "O conjunto FIAGRO possui janela menor e atualizações semanais; a cobertura não é histórica completa.")
    );

    private CvmFundIncomeProofOfConcept() {}

    public static Optional<Source> sourceFor(String symbol) {
        return Optional.ofNullable(SOURCES.get(AssetResolver.normalizeSymbol(symbol)));
    }
}
