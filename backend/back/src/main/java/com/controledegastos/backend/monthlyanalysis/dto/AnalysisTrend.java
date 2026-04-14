package com.controledegastos.backend.monthlyanalysis.dto; // Declares the package for comparison-trend values.

public enum AnalysisTrend { // Declares the enum used to express whether a result got better, worse or stayed equal.
    MELHOR, // Indicates that the selected period performed better than the compared period.
    PIOR, // Indicates that the selected period performed worse than the compared period.
    IGUAL // Indicates that the selected period matched the compared period.
} // Closes the enum declaration.
