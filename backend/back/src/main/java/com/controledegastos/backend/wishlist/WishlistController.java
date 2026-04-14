package com.controledegastos.backend.wishlist; // Declares the package for wishlist web endpoints.

import com.controledegastos.backend.wishlist.dto.WishlistPurchaseRequestDTO; // Imports the DTO used when a purchase is confirmed.
import com.controledegastos.backend.wishlist.dto.WishlistRequestDTO; // Imports the DTO used to create or update items.
import com.controledegastos.backend.wishlist.dto.WishlistResponseDTO; // Imports the DTO returned by the wishlist endpoints.
import com.controledegastos.backend.wishlist.dto.WishlistSortBy; // Imports the enum used to order the list response.
import com.controledegastos.backend.wishlist.dto.WishlistStatusFilter; // Imports the enum used to filter the list response.
import com.controledegastos.backend.wishlist.dto.WishlistSummaryDTO; // Imports the DTO returned by the summary endpoint.
import lombok.RequiredArgsConstructor; // Imports Lombok to generate the constructor for dependency injection.
import org.springframework.http.HttpStatus; // Imports the HTTP status enum used by create and delete responses.
import org.springframework.http.ResponseEntity; // Imports the HTTP response wrapper used by Spring MVC.
import org.springframework.web.bind.annotation.DeleteMapping; // Imports the DELETE mapping annotation.
import org.springframework.web.bind.annotation.GetMapping; // Imports the GET mapping annotation.
import org.springframework.web.bind.annotation.PathVariable; // Imports the path-variable binding annotation.
import org.springframework.web.bind.annotation.PostMapping; // Imports the POST mapping annotation.
import org.springframework.web.bind.annotation.PutMapping; // Imports the PUT mapping annotation.
import org.springframework.web.bind.annotation.RequestBody; // Imports the request-body binding annotation.
import org.springframework.web.bind.annotation.RequestMapping; // Imports the base route mapping annotation.
import org.springframework.web.bind.annotation.RequestParam; // Imports the query-parameter binding annotation.
import org.springframework.web.bind.annotation.RestController; // Marks the class as a REST controller.

import java.util.List; // Imports the list type used by the list endpoint.

@RestController // Registers the class as a JSON REST controller.
@RequestMapping("/api/wishlist") // Defines the base route of the wishlist module.
@RequiredArgsConstructor // Generates the constructor for the injected service dependency.
public class WishlistController { // Declares the controller that exposes the wishlist endpoints.

    private final WishlistService wishlistService; // Stores the service that owns the wishlist business rules.

    @PostMapping // Maps this method to HTTP POST on the base wishlist route.
    public ResponseEntity<WishlistResponseDTO> create( // Declares the endpoint that creates a new wishlist item.
            @RequestBody WishlistRequestDTO dto // Receives the payload used to create the wishlist item.
    ) { // Closes the method signature.
        return ResponseEntity.status(HttpStatus.CREATED).body(wishlistService.create(dto)); // Returns 201 with the created wishlist item.
    } // Closes the create endpoint.

    @GetMapping // Maps this method to HTTP GET on the base wishlist route.
    public ResponseEntity<List<WishlistResponseDTO>> findAll( // Declares the endpoint that lists wishlist items.
            @RequestParam(required = false) WishlistStatusFilter status, // Receives the optional filter that narrows the list by status.
            @RequestParam(required = false) WishlistSortBy sortBy // Receives the optional sorting strategy used by the list.
    ) { // Closes the method signature.
        return ResponseEntity.ok(wishlistService.findAll(status, sortBy)); // Returns 200 with the filtered and sorted wishlist list.
    } // Closes the list endpoint.

    @GetMapping("/summary") // Maps this method to HTTP GET on the summary route.
    public ResponseEntity<WishlistSummaryDTO> getSummary() { // Declares the endpoint that returns the wishlist summary.
        return ResponseEntity.ok(wishlistService.getSummary()); // Returns 200 with the summary DTO.
    } // Closes the summary endpoint.

    @PutMapping("/{id}") // Maps this method to HTTP PUT on one wishlist item route.
    public ResponseEntity<WishlistResponseDTO> update( // Declares the endpoint that updates a pending wishlist item.
            @PathVariable Long id, // Receives the id of the wishlist item being updated.
            @RequestBody WishlistRequestDTO dto // Receives the payload used to update the wishlist item.
    ) { // Closes the method signature.
        return ResponseEntity.ok(wishlistService.update(id, dto)); // Returns 200 with the updated wishlist item.
    } // Closes the update endpoint.

    @PostMapping("/{id}/purchase") // Maps this method to HTTP POST on the purchase route.
    public ResponseEntity<WishlistResponseDTO> markAsPurchased( // Declares the endpoint that marks a wishlist item as purchased.
            @PathVariable Long id, // Receives the id of the wishlist item being purchased.
            @RequestBody WishlistPurchaseRequestDTO dto // Receives the purchase information used to generate transactions.
    ) { // Closes the method signature.
        return ResponseEntity.ok(wishlistService.markAsPurchased(id, dto)); // Returns 200 with the purchased wishlist item.
    } // Closes the purchase endpoint.

    @PostMapping("/{id}/undo-purchase") // Maps this method to HTTP POST on the undo-purchase route.
    public ResponseEntity<WishlistResponseDTO> undoPurchase( // Declares the endpoint that reverts a purchased item back to pending.
            @PathVariable Long id // Receives the id of the wishlist item being reverted.
    ) { // Closes the method signature.
        return ResponseEntity.ok(wishlistService.undoPurchase(id)); // Returns 200 with the reverted wishlist item.
    } // Closes the undo-purchase endpoint.

    @DeleteMapping("/{id}") // Maps this method to HTTP DELETE on one wishlist item route.
    public ResponseEntity<Void> delete( // Declares the endpoint that deletes a wishlist item.
            @PathVariable Long id // Receives the id of the wishlist item being deleted.
    ) { // Closes the method signature.
        wishlistService.delete(id); // Delegates the deletion to the wishlist service.
        return ResponseEntity.noContent().build(); // Returns 204 to confirm the deletion succeeded.
    } // Closes the delete endpoint.
} // Closes the controller class.
