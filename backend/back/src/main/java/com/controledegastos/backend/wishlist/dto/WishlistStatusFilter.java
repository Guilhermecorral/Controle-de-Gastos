package com.controledegastos.backend.wishlist.dto; // Declares the package for wishlist filter options.

public enum WishlistStatusFilter { // Declares the supported filter options of the wishlist v1.
    PENDENTE, // Filters only pending wishlist items.
    COMPRADO, // Filters only purchased wishlist items.
    TODOS // Returns every wishlist item regardless of status.
} // Closes the enum declaration.
