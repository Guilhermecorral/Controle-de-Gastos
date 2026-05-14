package com.controledegastos.backend.wishlist;

import com.controledegastos.backend.wishlist.dto.WishlistPurchaseRequestDTO;
import com.controledegastos.backend.wishlist.dto.WishlistRequestDTO;
import com.controledegastos.backend.wishlist.dto.WishlistResponseDTO;
import com.controledegastos.backend.wishlist.dto.WishlistSortBy;
import com.controledegastos.backend.wishlist.dto.WishlistStatusFilter;
import com.controledegastos.backend.wishlist.dto.WishlistSummaryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Expoe os endpoints do modulo de wishlist.
 */
@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    /**
     * Cria um novo item pendente na wishlist do usuario autenticado.
     */
    @PostMapping
    public ResponseEntity<WishlistResponseDTO> create(@RequestBody WishlistRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(wishlistService.create(dto));
    }

    /**
     * Lista os itens da wishlist aplicando filtros de status e ordenacao quando informados.
     */
    @GetMapping
    public ResponseEntity<List<WishlistResponseDTO>> findAll(
            @RequestParam(required = false) WishlistStatusFilter status,
            @RequestParam(required = false) WishlistSortBy sortBy
    ) {
        return ResponseEntity.ok(wishlistService.findAll(status, sortBy));
    }

    /**
     * Devolve o resumo com contagens e valores totais da wishlist.
     */
    @GetMapping("/summary")
    public ResponseEntity<WishlistSummaryDTO> getSummary() {
        return ResponseEntity.ok(wishlistService.getSummary());
    }

    /**
     * Atualiza um item ainda pendente da wishlist.
     */
    @PutMapping("/{id}")
    public ResponseEntity<WishlistResponseDTO> update(
            @PathVariable Long id,
            @RequestBody WishlistRequestDTO dto
    ) {
        return ResponseEntity.ok(wishlistService.update(id, dto));
    }

    /**
     * Marca um item como comprado e dispara a integracao financeira correspondente.
     */
    @PostMapping("/{id}/purchase")
    public ResponseEntity<WishlistResponseDTO> markAsPurchased(
            @PathVariable Long id,
            @RequestBody WishlistPurchaseRequestDTO dto
    ) {
        return ResponseEntity.ok(wishlistService.markAsPurchased(id, dto));
    }

    /**
     * Desfaz uma compra e reverte as transacoes criadas a partir dela.
     */
    @PostMapping("/{id}/undo-purchase")
    public ResponseEntity<WishlistResponseDTO> undoPurchase(@PathVariable Long id) {
        return ResponseEntity.ok(wishlistService.undoPurchase(id));
    }

    /**
     * Remove definitivamente um item da wishlist e qualquer transacao vinculada a ele.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        wishlistService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
