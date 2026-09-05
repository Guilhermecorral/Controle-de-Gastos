package com.controledegastos.backend.investments;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class InvestmentImportDtos {
    private InvestmentImportDtos() { }

    public record PreviewResponse(Long batchId, String filename, String format,
                                  List<PreviewItemResponse> items, List<String> warnings) { }

    public record PreviewItemResponse(Long id, int sourceRow, boolean selectedByDefault,
                                      boolean possibleDuplicate, String warning,
                                      InvestmentMovement.MovementType movementType,
                                      InvestmentPosition.AssetType assetType, String symbol, String name,
                                      String market, String exchange, String currency, BigDecimal quantity,
                                      BigDecimal unitPrice, OperationCosts costs, BigDecimal exchangeRate,
                                      LocalDate eventDate) { }

    public record ConfirmRequest(@NotEmpty List<@Valid ConfirmItemRequest> items) { }

    public record ConfirmItemRequest(@NotNull Long id, boolean selected,
                                     InvestmentMovement.MovementType movementType,
                                     InvestmentPosition.AssetType assetType, String symbol, String name,
                                     String market, String exchange, String currency, BigDecimal quantity,
                                     BigDecimal unitPrice, OperationCosts costs, BigDecimal exchangeRate,
                                     LocalDate eventDate) { }

    public record ConfirmResponse(int importedCount, String message) { }
}
