package com.controledegastos.backend.investments;

import jakarta.persistence.Column;
import jakarta.persistence.Lob;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MarketDataSnapshotMappingTest {
    @Test
    void mapsPayloadToPostgresTextInsteadOfLargeObjectOid() throws NoSuchFieldException {
        var payloadField = MarketDataSnapshot.class.getDeclaredField("payload");
        var column = payloadField.getAnnotation(Column.class);

        assertThat(payloadField.isAnnotationPresent(Lob.class)).isFalse();
        assertThat(column).isNotNull();
        assertThat(column.columnDefinition()).isEqualTo("TEXT");
    }
}
