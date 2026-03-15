package com.agrovet.farmcare.models;

public enum StockSort {
    QUANTITY_DESC("quantity"),
    NAME_ASC("name");

    private final String param;

    StockSort(String param) {
        this.param = param;
    }

    public String param() {
        return param;
    }

    public static StockSort fromParam(String param) {
        if (param == null) return QUANTITY_DESC;
        return switch (param.trim().toLowerCase()) {
            case "name", "a-z", "az", "alpha" -> NAME_ASC;
            case "quantity", "qty", "high", "desc" -> QUANTITY_DESC;
            default -> QUANTITY_DESC;
        };
    }
}

