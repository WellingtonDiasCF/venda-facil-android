package com.vendafacil.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class LocalStore {
    private static final String PREFS = "venda_facil_data";
    private static final String PRODUCTS = "products";
    private static final String SALES = "sales";
    private static final String EXPENSES = "expenses";
    private static final String CUSTOMERS = "customers";
    private static final String CUSTOMER_PAYMENTS = "customer_payments";
    private final SharedPreferences preferences;

    LocalStore(Context context) {
        preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    List<Product> products() {
        List<Product> result = new ArrayList<>();
        try {
            JSONArray json = new JSONArray(preferences.getString(PRODUCTS, "[]"));
            for (int i = 0; i < json.length(); i++) result.add(Product.fromJson(json.getJSONObject(i)));
        } catch (Exception ignored) { }
        result.sort(Comparator.comparing(product -> product.name.toLowerCase()));
        return result;
    }

    List<Sale> sales() {
        List<Sale> result = new ArrayList<>();
        try {
            JSONArray json = new JSONArray(preferences.getString(SALES, "[]"));
            for (int i = 0; i < json.length(); i++) result.add(Sale.fromJson(json.getJSONObject(i)));
        } catch (Exception ignored) { }
        result.sort((first, second) -> Long.compare(second.timestamp, first.timestamp));
        return result;
    }

    List<Expense> expenses() {
        List<Expense> result = new ArrayList<>();
        try {
            JSONArray json = new JSONArray(preferences.getString(EXPENSES, "[]"));
            for (int i = 0; i < json.length(); i++) result.add(Expense.fromJson(json.getJSONObject(i)));
        } catch (Exception ignored) { }
        result.sort(Comparator.comparingLong(expense -> expense.firstDueTimestamp));
        return result;
    }

    List<Customer> customers() {
        List<Customer> result = new ArrayList<>();
        try {
            JSONArray json = new JSONArray(preferences.getString(CUSTOMERS, "[]"));
            for (int i = 0; i < json.length(); i++) result.add(Customer.fromJson(json.getJSONObject(i)));
        } catch (Exception ignored) { }
        result.sort(Comparator.comparing(customer -> customer.name.toLowerCase()));
        return result;
    }

    List<CustomerPayment> customerPayments() {
        List<CustomerPayment> result = new ArrayList<>();
        try {
            JSONArray json = new JSONArray(preferences.getString(CUSTOMER_PAYMENTS, "[]"));
            for (int i = 0; i < json.length(); i++) result.add(CustomerPayment.fromJson(json.getJSONObject(i)));
        } catch (Exception ignored) { }
        result.sort((first, second) -> Long.compare(second.timestamp, first.timestamp));
        return result;
    }

    void saveProducts(List<Product> products) {
        JSONArray json = new JSONArray();
        try { for (Product product : products) json.put(product.toJson()); } catch (Exception ignored) { }
        preferences.edit().putString(PRODUCTS, json.toString()).apply();
    }

    void saveSales(List<Sale> sales) {
        JSONArray json = new JSONArray();
        try { for (Sale sale : sales) json.put(sale.toJson()); } catch (Exception ignored) { }
        preferences.edit().putString(SALES, json.toString()).apply();
    }

    void saveExpenses(List<Expense> expenses) {
        JSONArray json = new JSONArray();
        try { for (Expense expense : expenses) json.put(expense.toJson()); } catch (Exception ignored) { }
        preferences.edit().putString(EXPENSES, json.toString()).apply();
    }

    void saveCustomers(List<Customer> customers) {
        JSONArray json = new JSONArray();
        try { for (Customer customer : customers) json.put(customer.toJson()); } catch (Exception ignored) { }
        preferences.edit().putString(CUSTOMERS, json.toString()).apply();
    }

    void saveCustomerPayments(List<CustomerPayment> payments) {
        JSONArray json = new JSONArray();
        try { for (CustomerPayment payment : payments) json.put(payment.toJson()); } catch (Exception ignored) { }
        preferences.edit().putString(CUSTOMER_PAYMENTS, json.toString()).apply();
    }

    String createBackup() throws Exception {
        return new JSONObject()
                .put("format", "venda_facil_backup")
                .put("schemaVersion", 1)
                .put("exportedAt", System.currentTimeMillis())
                .put("products", new JSONArray(preferences.getString(PRODUCTS, "[]")))
                .put("sales", new JSONArray(preferences.getString(SALES, "[]")))
                .put("expenses", new JSONArray(preferences.getString(EXPENSES, "[]")))
                .put("customers", new JSONArray(preferences.getString(CUSTOMERS, "[]")))
                .put("customerPayments", new JSONArray(preferences.getString(CUSTOMER_PAYMENTS, "[]")))
                .toString(2);
    }

    void restoreBackup(String content) throws Exception {
        JSONObject backup = new JSONObject(content);
        if (!"venda_facil_backup".equals(backup.optString("format")) || backup.optInt("schemaVersion") != 1) {
            throw new IllegalArgumentException("Formato de backup inválido");
        }
        JSONArray productJson = backup.getJSONArray("products");
        JSONArray saleJson = backup.getJSONArray("sales");
        JSONArray expenseJson = backup.getJSONArray("expenses");
        JSONArray customerJson = backup.optJSONArray("customers");
        if (customerJson == null) customerJson = new JSONArray();
        JSONArray paymentJson = backup.optJSONArray("customerPayments");
        if (paymentJson == null) paymentJson = new JSONArray();
        long greatestId = 0;
        for (int i = 0; i < productJson.length(); i++) {
            Product product = Product.fromJson(productJson.getJSONObject(i));
            if (product.name.trim().isEmpty() || product.priceCents <= 0) throw new IllegalArgumentException("Produto inválido");
            greatestId = Math.max(greatestId, product.id);
        }
        for (int i = 0; i < saleJson.length(); i++) {
            Sale sale = Sale.fromJson(saleJson.getJSONObject(i));
            if (sale.timestamp <= 0) throw new IllegalArgumentException("Venda inválida");
            greatestId = Math.max(greatestId, sale.id);
        }
        for (int i = 0; i < expenseJson.length(); i++) {
            Expense expense = Expense.fromJson(expenseJson.getJSONObject(i));
            if (expense.name.trim().isEmpty() || expense.amountCents <= 0 || expense.firstDueTimestamp <= 0) {
                throw new IllegalArgumentException("Despesa inválida");
            }
            greatestId = Math.max(greatestId, expense.id);
        }
        for (int i = 0; i < customerJson.length(); i++) {
            Customer customer = Customer.fromJson(customerJson.getJSONObject(i));
            if (customer.name.trim().isEmpty()) throw new IllegalArgumentException("Cliente inválido");
            greatestId = Math.max(greatestId, customer.id);
        }
        for (int i = 0; i < paymentJson.length(); i++) {
            CustomerPayment payment = CustomerPayment.fromJson(paymentJson.getJSONObject(i));
            if (payment.customerId <= 0 || payment.timestamp <= 0 || payment.amountCents <= 0) {
                throw new IllegalArgumentException("Recebimento inválido");
            }
            greatestId = Math.max(greatestId, payment.id);
        }
        preferences.edit()
                .putString(PRODUCTS, productJson.toString())
                .putString(SALES, saleJson.toString())
                .putString(EXPENSES, expenseJson.toString())
                .putString(CUSTOMERS, customerJson.toString())
                .putString(CUSTOMER_PAYMENTS, paymentJson.toString())
                .putLong("last_id", greatestId)
                .apply();
    }

    long nextId() {
        long now = System.currentTimeMillis();
        long previous = preferences.getLong("last_id", 0);
        long next = Math.max(now, previous + 1);
        preferences.edit().putLong("last_id", next).apply();
        return next;
    }

    boolean shouldShowWelcome() {
        return !preferences.getBoolean("welcome_seen", false);
    }

    void markWelcomeSeen() {
        preferences.edit().putBoolean("welcome_seen", true).apply();
    }
}
