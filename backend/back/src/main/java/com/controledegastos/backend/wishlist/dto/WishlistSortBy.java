package com.controledegastos.backend.wishlist.dto; // Declares the package for wishlist sorting options.

public enum WishlistSortBy { // Declares the supported sorting options of the wishlist v1.
    MENOR_PRECO, // Sorts the list by the lowest final price first.
    MAIOR_PRECO, // Sorts the list by the highest final price first.
    PRIORIDADE, // Sorts the list by priority from highest to lowest.
    ADICIONADOS_RECENTEMENTE, // Sorts the list by most recently created items first.
    PERSONALIZADO // Sorts the list using the custom strategy chosen for the product v1.
} // Closes the enum declaration.
