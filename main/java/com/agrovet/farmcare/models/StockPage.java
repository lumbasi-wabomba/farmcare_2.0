package com.agrovet.farmcare.models;

import java.util.List;

public record StockPage(
        List<Stock> items,
        int page,
        int pageSize,
        int totalItems,
        int totalPages,
        StockSort sort
) {}

