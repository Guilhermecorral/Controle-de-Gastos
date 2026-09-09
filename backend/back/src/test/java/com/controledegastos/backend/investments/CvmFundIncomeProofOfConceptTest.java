package com.controledegastos.backend.investments;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CvmFundIncomeProofOfConceptTest {
    @Test
    void keepsFiiAndFiagroCoverageSeparateAndManual() {
        var mxrf = CvmFundIncomeProofOfConcept.sourceFor("MXRF11").orElseThrow();
        var rura = CvmFundIncomeProofOfConcept.sourceFor("RURA11").orElseThrow();

        assertThat(mxrf.assetType()).isEqualTo(InvestmentPosition.AssetType.FII);
        assertThat(mxrf.coverage()).contains("cinco");
        assertThat(rura.assetType()).isEqualTo(InvestmentPosition.AssetType.FIAGRO);
        assertThat(rura.coverage()).contains("doze");
        assertThat(mxrf.supportsAutomaticSchedule()).isFalse();
        assertThat(rura.supportsAutomaticSchedule()).isFalse();
    }
}
