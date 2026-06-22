package com.vendafacil.app;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

final class Product {
    long id;
    String name;
    String sku;
    String icon;
    long costCents;
    long priceCents;
    int stock;

    Product(long id, String name, String sku, String icon, long costCents, long priceCents, int stock) {
        this.id = id;
        this.name = name;
        this.sku = sku;
        this.icon = icon;
        this.costCents = costCents;
        this.priceCents = priceCents;
        this.stock = stock;
    }

    JSONObject toJson() throws JSONException {
        return new JSONObject()
                .put("id", id).put("name", name).put("sku", sku).put("icon", icon)
                .put("cost", costCents).put("price", priceCents).put("stock", stock);
    }

    static Product fromJson(JSONObject json) {
        return new Product(json.optLong("id"), json.optString("name"), json.optString("sku"), json.optString("icon"),
                json.optLong("cost"), json.optLong("price"), json.optInt("stock"));
    }
}

final class SaleItem {
    long productId;
    String productName;
    String productIcon;
    int quantity;
    long unitPriceCents;
    long unitCostCents;

    SaleItem(long productId, String productName, String productIcon, int quantity, long unitPriceCents, long unitCostCents) {
        this.productId = productId;
        this.productName = productName;
        this.productIcon = productIcon;
        this.quantity = quantity;
        this.unitPriceCents = unitPriceCents;
        this.unitCostCents = unitCostCents;
    }

    long total() { return unitPriceCents * quantity; }
    boolean hasCost() { return unitCostCents >= 0; }
    long profit() { return hasCost() ? (unitPriceCents - unitCostCents) * quantity : 0; }

    JSONObject toJson() throws JSONException {
        return new JSONObject().put("productId", productId).put("productName", productName).put("productIcon", productIcon)
                .put("quantity", quantity).put("unitPrice", unitPriceCents).put("unitCost", unitCostCents);
    }

    static SaleItem fromJson(JSONObject json) {
        return new SaleItem(json.optLong("productId"), json.optString("productName"), json.optString("productIcon"),
                json.optInt("quantity"), json.optLong("unitPrice"), json.optLong("unitCost"));
    }
}

final class Sale {
    long id;
    long timestamp;
    String customer;
    String payment;
    long discountCents;
    String note;
    long customerId;
    long paidCents;
    List<SaleItem> items;

    Sale(long id, long timestamp, String customer, String payment, long discountCents, String note,
         long customerId, long paidCents, List<SaleItem> items) {
        this.id = id;
        this.timestamp = timestamp;
        this.customer = customer;
        this.payment = payment;
        this.discountCents = discountCents;
        this.note = note;
        this.customerId = customerId;
        this.paidCents = paidCents;
        this.items = items;
    }

    long subtotal() {
        long value = 0;
        for (SaleItem item : items) value += item.total();
        return value;
    }

    long total() { return Math.max(0, subtotal() - discountCents); }

    long paid() { return Math.min(total(), Math.max(0, paidCents)); }

    long outstanding() { return Math.max(0, total() - paid()); }

    boolean isPending() { return outstanding() > 0; }

    long profit() {
        long value = 0;
        for (SaleItem item : items) value += item.profit();
        return value - discountCents;
    }

    boolean hasCompleteCost() {
        for (SaleItem item : items) if (!item.hasCost()) return false;
        return true;
    }

    int units() {
        int value = 0;
        for (SaleItem item : items) value += item.quantity;
        return value;
    }

    JSONObject toJson() throws JSONException {
        JSONArray entries = new JSONArray();
        for (SaleItem item : items) entries.put(item.toJson());
        return new JSONObject().put("id", id).put("timestamp", timestamp).put("customer", customer)
                .put("payment", payment).put("discount", discountCents).put("note", note)
                .put("customerId", customerId).put("paid", paidCents).put("items", entries);
    }

    static Sale fromJson(JSONObject json) {
        List<SaleItem> items = new ArrayList<>();
        JSONArray entries = json.optJSONArray("items");
        if (entries != null) {
            for (int i = 0; i < entries.length(); i++) {
                JSONObject entry = entries.optJSONObject(i);
                if (entry != null) items.add(SaleItem.fromJson(entry));
            }
        }
        Sale sale = new Sale(json.optLong("id"), json.optLong("timestamp"), json.optString("customer"),
                json.optString("payment"), json.optLong("discount"), json.optString("note"),
                json.optLong("customerId"), json.optLong("paid", -1), items);
        if (!json.has("paid")) sale.paidCents = sale.total();
        return sale;
    }
}

final class Customer {
    long id;
    String name;
    String phone;
    String note;
    long createdAt;

    Customer(long id, String name, String phone, String note, long createdAt) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.note = note;
        this.createdAt = createdAt;
    }

    JSONObject toJson() throws JSONException {
        return new JSONObject().put("id", id).put("name", name).put("phone", phone)
                .put("note", note).put("createdAt", createdAt);
    }

    static Customer fromJson(JSONObject json) {
        return new Customer(json.optLong("id"), json.optString("name"), json.optString("phone"),
                json.optString("note"), json.optLong("createdAt", System.currentTimeMillis()));
    }
}

final class CustomerPayment {
    long id;
    long customerId;
    long timestamp;
    long amountCents;

    CustomerPayment(long id, long customerId, long timestamp, long amountCents) {
        this.id = id;
        this.customerId = customerId;
        this.timestamp = timestamp;
        this.amountCents = amountCents;
    }

    JSONObject toJson() throws JSONException {
        return new JSONObject().put("id", id).put("customerId", customerId)
                .put("timestamp", timestamp).put("amount", amountCents);
    }

    static CustomerPayment fromJson(JSONObject json) {
        return new CustomerPayment(json.optLong("id"), json.optLong("customerId"),
                json.optLong("timestamp"), json.optLong("amount"));
    }
}

final class Expense {
    static final String NONE = "NONE";
    static final String DAY = "DAY";
    static final String WEEK = "WEEK";
    static final String MONTH = "MONTH";
    static final String YEAR = "YEAR";

    long id;
    String name;
    long amountCents;
    long firstDueTimestamp;
    String recurrenceUnit;
    int recurrenceInterval;
    String note;

    Expense(long id, String name, long amountCents, long firstDueTimestamp,
            String recurrenceUnit, int recurrenceInterval, String note) {
        this.id = id;
        this.name = name;
        this.amountCents = amountCents;
        this.firstDueTimestamp = firstDueTimestamp;
        this.recurrenceUnit = recurrenceUnit;
        this.recurrenceInterval = recurrenceInterval;
        this.note = note;
    }

    boolean isRecurring() { return !NONE.equals(recurrenceUnit); }

    JSONObject toJson() throws JSONException {
        return new JSONObject().put("id", id).put("name", name).put("amount", amountCents)
                .put("firstDue", firstDueTimestamp).put("recurrenceUnit", recurrenceUnit)
                .put("recurrenceInterval", recurrenceInterval).put("note", note);
    }

    static Expense fromJson(JSONObject json) {
        return new Expense(json.optLong("id"), json.optString("name"), json.optLong("amount"),
                json.optLong("firstDue"), json.optString("recurrenceUnit", NONE),
                Math.max(1, json.optInt("recurrenceInterval", 1)), json.optString("note"));
    }
}
