package com.controledegastos.backend.tax;

import com.controledegastos.backend.user.TaxProfileType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tax")
@RequiredArgsConstructor
public class TaxController {
    private final TaxObligationService service;

    public record TaxProfileResponse(TaxProfileType taxProfileType) {}
    public record TaxProfileRequest(@NotNull TaxProfileType taxProfileType) {}

    @GetMapping("/profile")
    public TaxProfileResponse profile() { return new TaxProfileResponse(service.getTaxProfile()); }

    @PutMapping("/profile")
    public TaxProfileResponse saveProfile(@Valid @RequestBody TaxProfileRequest request) {
        return new TaxProfileResponse(service.saveTaxProfile(request.taxProfileType()));
    }

    @GetMapping("/obligations")
    public List<TaxObligationResponse> list() { return service.listObligations(); }

    @PostMapping("/obligations")
    public ResponseEntity<TaxObligationResponse> create(@Valid @RequestBody TaxObligationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createObligation(request));
    }

    @PutMapping("/obligations/{id}")
    public TaxObligationResponse update(@PathVariable UUID id, @Valid @RequestBody TaxObligationRequest request) {
        return service.updateObligation(id, request);
    }

    @DeleteMapping("/obligations/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.deleteObligation(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/obligations/{id}/confirm-payment")
    public TaxObligationResponse confirmPayment(@PathVariable UUID id,
                                                 @Valid @RequestBody PaymentConfirmationRequest request) {
        return service.confirmPayment(id, request);
    }
}
