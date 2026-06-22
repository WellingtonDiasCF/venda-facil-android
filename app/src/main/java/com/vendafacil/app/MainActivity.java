package com.vendafacil.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.json.JSONObject;

public class MainActivity extends Activity {
    private static final int SAVE_REPORT_REQUEST = 1001;
    private static final int IMPORT_BACKUP_REQUEST = 1002;
    private static final String[] PAYMENT_METHODS = {"Pix", "Dinheiro", "Cartão de débito", "Cartão de crédito", "Outro"};
    private static final String[] PRODUCT_ICONS = {"", "🥨", "🥟", "🍢", "🌽", "🍟", "🍩", "🍫", "🍧", "🥤", "🧃", "🥫", "⚡", "☕", "🍽️"};
    private static final int BRAND = Color.rgb(21, 94, 84);
    private static final int BRAND_DARK = Color.rgb(14, 70, 63);
    private static final int ACCENT = Color.rgb(229, 154, 54);
    private static final int BACKGROUND = Color.rgb(247, 248, 245);
    private static final int TEXT = Color.rgb(24, 32, 30);
    private static final int MUTED = Color.rgb(100, 113, 109);
    private static final int BORDER = Color.rgb(224, 229, 225);
    private static final int DANGER = Color.rgb(176, 49, 49);

    private final NumberFormat currency = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    private final SimpleDateFormat dateTime = new SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", new Locale("pt", "BR"));
    private final SimpleDateFormat reportDate = new SimpleDateFormat("dd/MM/yyyy", new Locale("pt", "BR"));
    private final List<SaleItem> cart = new ArrayList<>();
    private LocalStore store;
    private List<Product> products;
    private List<Sale> sales;
    private List<Expense> expenses;
    private List<Customer> customers;
    private List<CustomerPayment> customerPayments;
    private FrameLayout content;
    private LinearLayout navigation;
    private int selectedTab = 0;
    private int reportDays = 0;
    private long customReportStart = 0;
    private long customReportEnd = 0;
    private String pendingCsv = "";
    private String draftCustomer = "";
    private long draftCustomerId = 0;
    private boolean draftPendingPayment = false;
    private String draftDiscount = "";
    private String draftNote = "";
    private int draftPayment = 0;
    private int expensePeriodMode = 0;
    private long customExpenseStart = 0;
    private long customExpenseEnd = 0;
    private int customerDebtDays = -1;
    private long customDebtStart = 0;
    private long customDebtEnd = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        store = new LocalStore(this);
        reload();
        buildShell();
        selectTab(0);
        if (store.shouldShowWelcome()) showWelcome();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null || data.getData() == null) return;
        if (requestCode == SAVE_REPORT_REQUEST) {
            try (OutputStream output = getContentResolver().openOutputStream(data.getData())) {
                if (output == null) throw new IllegalStateException("Destino indisponível");
                output.write(("\uFEFF" + pendingCsv).getBytes(StandardCharsets.UTF_8));
                toast("Relatório salvo com sucesso.");
            } catch (Exception exception) {
                toast("Não foi possível salvar o relatório.");
            }
        } else if (requestCode == IMPORT_BACKUP_REQUEST) {
            try {
                String backup = readTextFile(data.getData());
                JSONObject json = new JSONObject(backup);
                if (!"venda_facil_backup".equals(json.optString("format")) || json.optInt("schemaVersion") != 1) {
                    throw new IllegalArgumentException("Arquivo incompatível");
                }
                confirmBackupRestore(backup, json.optLong("exportedAt"));
            } catch (Exception exception) {
                toast("Este arquivo não é um backup válido do Venda Fácil.");
            }
        }
    }

    private void reload() {
        products = store.products();
        sales = store.sales();
        expenses = store.expenses();
        customers = store.customers();
        customerPayments = store.customerPayments();
    }

    private void buildShell() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BACKGROUND);

        LinearLayout appBar = new LinearLayout(this);
        appBar.setOrientation(LinearLayout.VERTICAL);
        appBar.setPadding(dp(20), dp(16), dp(20), dp(14));
        appBar.setBackgroundColor(BRAND);
        TextView brand = label("VENDA FÁCIL", 20, Color.WHITE, true);
        TextView tagline = label("Seu negócio organizado no bolso", 12, Color.rgb(210, 233, 228), false);
        appBar.addView(brand);
        appBar.addView(tagline);
        root.addView(appBar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        content = new FrameLayout(this);
        root.addView(content, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        navigation = new LinearLayout(this);
        navigation.setOrientation(LinearLayout.HORIZONTAL);
        navigation.setGravity(Gravity.CENTER);
        navigation.setPadding(dp(4), dp(5), dp(4), dp(7));
        navigation.setBackgroundColor(Color.WHITE);
        String[] names = {"Início", "Produtos", "Vender", "Clientes", "Despesas", "Relatórios"};
        String[] icons = {"⌂", "▦", "+", "☺", "R$", "▥"};
        for (int i = 0; i < names.length; i++) {
            final int tab = i;
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.setPadding(dp(4), dp(3), dp(4), dp(2));
            TextView icon = label(icons[i], i == 2 ? 24 : 19, MUTED, true);
            icon.setGravity(Gravity.CENTER);
            icon.setSingleLine(true);
            icon.setIncludeFontPadding(false);
            TextView name = label(names[i], 9, MUTED, false);
            name.setGravity(Gravity.CENTER);
            name.setSingleLine(true);
            name.setIncludeFontPadding(false);
            item.addView(icon, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(29)));
            item.addView(name, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(17)));
            item.setTag(new TextView[]{icon, name});
            item.setOnClickListener(view -> selectTab(tab));
            navigation.addView(item, new LinearLayout.LayoutParams(0, dp(58), 1));
        }
        root.addView(navigation, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(root);

        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            appBar.setPadding(dp(20), dp(16) + bars.top, dp(20), dp(14));
            navigation.setPadding(dp(4), dp(5), dp(4), dp(7) + bars.bottom);
            return windowInsets;
        });
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), root);
        controller.setAppearanceLightStatusBars(false);
        controller.setAppearanceLightNavigationBars(true);
        ViewCompat.requestApplyInsets(root);
    }

    private void selectTab(int tab) {
        selectedTab = tab;
        for (int i = 0; i < navigation.getChildCount(); i++) {
            TextView[] views = (TextView[]) navigation.getChildAt(i).getTag();
            int color = i == tab ? BRAND : MUTED;
            views[0].setTextColor(color);
            views[1].setTextColor(color);
            views[1].setTypeface(Typeface.DEFAULT, i == tab ? Typeface.BOLD : Typeface.NORMAL);
        }
        content.removeAllViews();
        if (tab == 0) showDashboard();
        else if (tab == 1) showProducts();
        else if (tab == 2) showSale();
        else if (tab == 3) showCustomers();
        else if (tab == 4) showExpenses();
        else showReports();
    }

    private LinearLayout page(String title, String subtitle) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(16), dp(18), dp(16), dp(28));
        box.addView(label(title, 25, TEXT, true));
        TextView sub = label(subtitle, 13, MUTED, false);
        sub.setPadding(0, dp(3), 0, dp(16));
        box.addView(sub);
        return box;
    }

    private void attachScrollable(LinearLayout page) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(page);
        content.removeAllViews();
        content.addView(scroll, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void showDashboard() {
        reload();
        LinearLayout page = page(greeting(), "Acompanhe o movimento de hoje");
        long start = startOfToday();
        List<Sale> today = salesSince(start);
        long revenue = sumTotal(today);
        int units = 0;
        for (Sale sale : today) units += sale.units();

        LinearLayout hero = card();
        hero.setBackground(roundRect(BRAND, 18, BRAND));
        hero.addView(label("VENDAS DE HOJE", 11, Color.rgb(202, 229, 223), true));
        TextView total = label(money(revenue), 31, Color.WHITE, true);
        total.setPadding(0, dp(7), 0, dp(4));
        hero.addView(total);
        hero.addView(label(today.size() + plural(today.size(), " venda", " vendas") + "  •  " + units + plural(units, " item", " itens"), 13, Color.WHITE, false));
        page.addView(hero, margins(0, 0, 0, 12));

        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        stats.addView(metricCard("Produtos", String.valueOf(products.size()), "cadastrados"), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        LinearLayout.LayoutParams secondParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        secondParams.setMargins(dp(10), 0, 0, 0);
        stats.addView(metricCard("Estoque baixo", String.valueOf(lowStockCount()), "até 5 unidades"), secondParams);
        page.addView(stats, margins(0, 0, 0, 18));

        long pendingTotal = 0;
        int pendingCustomers = 0;
        for (Customer customer : customers) {
            long customerDebt = customerOutstanding(customer.id);
            pendingTotal += customerDebt;
            if (customerDebt > 0) pendingCustomers++;
        }
        if (pendingTotal > 0) {
            LinearLayout receivables = card();
            receivables.setOrientation(LinearLayout.HORIZONTAL);
            receivables.setGravity(Gravity.CENTER_VERTICAL);
            receivables.setBackground(roundRect(Color.rgb(255, 247, 233), 14, Color.rgb(241, 211, 164)));
            LinearLayout text = new LinearLayout(this);
            text.setOrientation(LinearLayout.VERTICAL);
            text.addView(label("PAGAMENTOS PENDENTES", 10, MUTED, true));
            text.addView(label(money(pendingTotal), 20, DANGER, true));
            text.addView(label(pendingCustomers + plural(pendingCustomers, " cliente devendo", " clientes devendo"), 11, MUTED, false));
            receivables.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            receivables.addView(label("VER CLIENTES  ›", 12, BRAND, true));
            receivables.setOnClickListener(view -> selectTab(3));
            page.addView(receivables, margins(0, 0, 0, 18));
        }

        if (products.isEmpty()) {
            page.addView(nextStepCard("1", "Cadastre seu primeiro produto", "Informe nome, preço e estoque. Depois você já poderá vender.", "CADASTRAR AGORA", 1), margins(0, 0, 0, 14));
        } else if (sales.isEmpty()) {
            page.addView(nextStepCard("2", "Registre sua primeira venda", "Escolha os produtos, a quantidade e a forma de pagamento.", "FAZER PRIMEIRA VENDA", 2), margins(0, 0, 0, 14));
        }

        Button sell = primaryButton("REGISTRAR NOVA VENDA");
        sell.setOnClickListener(view -> selectTab(2));
        page.addView(sell, margins(0, 0, 0, 22));

        page.addView(sectionTitle("Vendas recentes"));
        if (sales.isEmpty()) {
            page.addView(emptyState("Nenhuma venda registrada", "Cadastre produtos e faça sua primeira venda."));
        } else {
            for (int i = 0; i < Math.min(5, sales.size()); i++) page.addView(saleRow(sales.get(i), true), margins(0, 0, 0, 9));
        }
        page.addView(sectionTitle("Backup completo"), margins(0, 18, 0, 0));
        LinearLayout backupCard = card();
        TextView backupHelp = label("Guarde produtos, vendas, clientes, pagamentos pendentes, estoque e despesas em um único arquivo.", 12, MUTED, false);
        backupHelp.setPadding(0, 0, 0, dp(12));
        backupCard.addView(backupHelp);
        Button exportBackup = primaryButton("ENVIAR BACKUP PELO WHATSAPP");
        exportBackup.setOnClickListener(view -> exportBackupToWhatsApp());
        backupCard.addView(exportBackup, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
        Button importBackup = secondaryButton("IMPORTAR E RESTAURAR BACKUP");
        importBackup.setOnClickListener(view -> chooseBackupFile());
        backupCard.addView(importBackup, margins(0, 9, 0, 0, ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
        page.addView(backupCard);
        attachScrollable(page);
    }

    private void showProducts() {
        reload();
        LinearLayout page = page("Produtos", products.size() + plural(products.size(), " produto cadastrado", " produtos cadastrados") + "  •  toque para editar");
        Button add = primaryButton("+  ADICIONAR PRODUTO");
        add.setOnClickListener(view -> productDialog(null));
        page.addView(add, margins(0, 0, 0, 18));

        if (products.isEmpty()) {
            page.addView(emptyState("Seu catálogo está vazio", "Adicione nome e preço. Custo e controle de estoque são opcionais."));
        } else {
            for (Product product : products) page.addView(productRow(product), margins(0, 0, 0, 10));
        }
        attachScrollable(page);
    }

    private View productRow(Product product) {
        LinearLayout row = card();
        row.setPadding(dp(15), dp(13), dp(12), dp(13));
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        TextView name = label(productDisplayName(product), 16, TEXT, true);
        info.addView(name);
        String sku = product.sku.isEmpty() ? "Sem código" : "Cód. " + product.sku;
        info.addView(label(sku, 11, MUTED, false));
        top.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView price = label(money(product.priceCents), 17, BRAND, true);
        top.addView(price);
        row.addView(top);
        View separator = new View(this);
        separator.setBackgroundColor(BORDER);
        row.addView(separator, margins(dp(1), dp(11), dp(1), dp(10), ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.HORIZONTAL);
        bottom.setGravity(Gravity.CENTER_VERTICAL);
        boolean lowStock = product.stock >= 0 && product.stock <= 5;
        int stockColor = lowStock ? DANGER : MUTED;
        String stockText = product.stock < 0 ? "Estoque não controlado" : "Estoque: " + product.stock;
        TextView stock = label(stockText, 12, stockColor, lowStock);
        bottom.addView(stock, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        String costText = product.costCents < 0 ? "Custo não informado" : "Custo " + money(product.costCents);
        bottom.addView(label(costText, 12, MUTED, false));
        row.addView(bottom);
        row.setOnClickListener(view -> productDialog(product));
        return row;
    }

    private void productDialog(Product product) {
        boolean editing = product != null;
        LinearLayout form = dialogForm();
        EditText name = input("Nome do produto", InputType.TYPE_CLASS_TEXT);
        EditText sku = input("Código / SKU (opcional)", InputType.TYPE_CLASS_TEXT);
        EditText cost = input("Custo (R$)", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText price = input("Preço de venda (R$)", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText stock = input("Deixe vazio para não controlar", InputType.TYPE_CLASS_NUMBER);
        String[] selectedIcon = {editing ? product.icon : ""};
        if (editing) {
            name.setText(product.name);
            sku.setText(product.sku);
            if (product.costCents >= 0) cost.setText(decimal(product.costCents));
            price.setText(decimal(product.priceCents));
            if (product.stock >= 0) stock.setText(String.valueOf(product.stock));
        }
        addField(form, "Nome *", name);
        addField(form, "Código", sku);
        form.addView(fieldLabel("Ícone (opcional)"));
        TextView iconField = selectionField(iconSelectionLabel(selectedIcon[0]));
        iconField.setOnClickListener(view -> showProductIconPicker(icon -> {
            selectedIcon[0] = icon;
            iconField.setText(iconSelectionLabel(icon));
        }));
        form.addView(iconField, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));
        addField(form, "Custo unitário (opcional)", cost);
        addField(form, "Preço de venda *", price);
        addField(form, "Estoque (opcional)", stock);
        if (editing && product.stock >= 0) {
            stock.setEnabled(false);
            stock.setAlpha(.72f);
            TextView stockHelp = label("O estoque é alterado somente por entradas e saídas.", 11, MUTED, false);
            stockHelp.setPadding(0, dp(7), 0, dp(7));
            form.addView(stockHelp);
            Button moveStock = secondaryButton("MOVIMENTAR ESTOQUE");
            moveStock.setOnClickListener(view -> stockMovementDialog(product, stock));
            form.addView(moveStock, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50)));
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(editing ? "Editar produto" : "Novo produto")
                .setView(form)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Salvar", null)
                .create();
        if (editing) dialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Excluir", (view, which) -> { });
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(BRAND);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                String productName = name.getText().toString().trim();
                String costText = cost.getText().toString().trim();
                Long costValue = costText.isEmpty() ? -1L : parseMoney(costText);
                Long priceValue = parseMoney(price.getText().toString());
                String stockText = stock.getText().toString().trim();
                Integer stockValue = stockText.isEmpty() ? -1 : parseInt(stockText);
                if (productName.isEmpty() || price.getText().toString().trim().isEmpty() || costValue == null
                        || priceValue == null || stockValue == null) {
                    toast("Preencha corretamente os campos obrigatórios.");
                    return;
                }
                if (priceValue <= 0 || costValue < -1 || stockValue < -1) {
                    toast("Preço deve ser positivo e os demais valores não podem ser negativos.");
                    return;
                }
                if (editing) {
                    product.name = productName;
                    product.sku = sku.getText().toString().trim();
                    product.icon = selectedIcon[0];
                    product.costCents = costValue;
                    product.priceCents = priceValue;
                    product.stock = stockValue;
                } else {
                    products.add(new Product(store.nextId(), productName, sku.getText().toString().trim(), selectedIcon[0], costValue, priceValue, stockValue));
                }
                store.saveProducts(products);
                dialog.dismiss();
                toast(editing ? "Produto atualizado." : "Produto adicionado.");
                showProducts();
            });
            if (editing) {
                Button remove = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
                remove.setTextColor(DANGER);
                remove.setOnClickListener(view -> confirmDeleteProduct(product, dialog));
            }
        });
        dialog.show();
    }

    private void stockMovementDialog(Product product, EditText stockField) {
        boolean[] entry = {true};
        LinearLayout form = dialogForm();
        LinearLayout current = card();
        current.setBackground(roundRect(Color.rgb(235, 245, 242), 13, Color.rgb(192, 220, 212)));
        current.addView(label("ESTOQUE ATUAL", 11, MUTED, true));
        TextView currentValue = label(String.valueOf(product.stock), 28, BRAND, true);
        currentValue.setPadding(0, dp(5), 0, 0);
        current.addView(currentValue);
        form.addView(current, margins(0, 0, 0, 12));

        form.addView(fieldLabel("Tipo de movimentação"));
        LinearLayout typeLine = new LinearLayout(this);
        typeLine.setOrientation(LinearLayout.HORIZONTAL);
        TextView entryChip = label("+  Entrada", 13, Color.WHITE, true);
        TextView exitChip = label("−  Saída", 13, BRAND, true);
        for (TextView chip : new TextView[]{entryChip, exitChip}) {
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(15), dp(10), dp(15), dp(10));
        }
        styleChoiceChip(entryChip, true);
        styleChoiceChip(exitChip, false);
        typeLine.addView(entryChip, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        LinearLayout.LayoutParams exitParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        exitParams.setMargins(dp(8), 0, 0, 0);
        typeLine.addView(exitChip, exitParams);
        form.addView(typeLine);

        form.addView(fieldLabel("Quantidade"));
        EditText quantity = input("Quantos itens?", InputType.TYPE_CLASS_NUMBER);
        form.addView(quantity, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));
        TextView preview = label("Saldo final: " + product.stock, 16, BRAND, true);
        preview.setGravity(Gravity.CENTER);
        preview.setPadding(dp(12), dp(13), dp(12), dp(13));
        preview.setBackground(roundRect(Color.rgb(248, 251, 249), 10, BORDER));
        form.addView(preview, margins(0, 12, 0, 0));

        Runnable updatePreview = () -> {
            Integer amount = parseInt(quantity.getText().toString());
            int finalStock = amount == null ? product.stock : product.stock + (entry[0] ? amount : -amount);
            preview.setText(String.format(new Locale("pt", "BR"), "Saldo final: %d", finalStock));
            preview.setTextColor(finalStock < 0 ? DANGER : BRAND);
        };
        entryChip.setOnClickListener(view -> {
            entry[0] = true;
            styleChoiceChip(entryChip, true);
            styleChoiceChip(exitChip, false);
            updatePreview.run();
        });
        exitChip.setOnClickListener(view -> {
            entry[0] = false;
            styleChoiceChip(entryChip, false);
            styleChoiceChip(exitChip, true);
            updatePreview.run();
        });
        quantity.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence text, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence text, int start, int before, int count) { updatePreview.run(); }
            @Override public void afterTextChanged(Editable editable) { }
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Movimentar estoque")
                .setView(form)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Confirmar", null)
                .create();
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(BRAND);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                Integer amount = parseInt(quantity.getText().toString());
                if (amount == null || amount <= 0) { toast("Informe uma quantidade válida."); return; }
                int finalStock = product.stock + (entry[0] ? amount : -amount);
                if (finalStock < 0) { toast("A saída não pode ser maior que o estoque atual."); return; }
                product.stock = finalStock;
                stockField.setText(String.valueOf(finalStock));
                store.saveProducts(products);
                dialog.dismiss();
                toast(entry[0] ? "Entrada registrada." : "Saída registrada.");
            });
        });
        dialog.show();
    }

    private void showProductIconPicker(IconSelectionListener listener) {
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setPadding(dp(16), dp(5), dp(16), dp(10));
        List<View> options = new ArrayList<>();
        List<String> values = new ArrayList<>();
        for (int i = 0; i < PRODUCT_ICONS.length; i += 4) {
            LinearLayout line = new LinearLayout(this);
            line.setOrientation(LinearLayout.HORIZONTAL);
            for (int j = i; j < Math.min(i + 4, PRODUCT_ICONS.length); j++) {
                LinearLayout option = new LinearLayout(this);
                option.setGravity(Gravity.CENTER);
                option.setPadding(dp(5), dp(10), dp(5), dp(10));
                option.setBackground(roundRect(Color.rgb(248, 251, 249), 11, BORDER));
                String symbol = PRODUCT_ICONS[j].isEmpty() ? "×" : PRODUCT_ICONS[j];
                TextView icon = label(symbol, PRODUCT_ICONS[j].isEmpty() ? 22 : 29, PRODUCT_ICONS[j].isEmpty() ? MUTED : TEXT, false);
                icon.setGravity(Gravity.CENTER);
                option.addView(icon, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(40)));
                options.add(option);
                values.add(PRODUCT_ICONS[j]);
                LinearLayout.LayoutParams optionParams = new LinearLayout.LayoutParams(0, dp(62), 1);
                optionParams.setMargins(j == i ? 0 : dp(7), 0, 0, 0);
                line.addView(option, optionParams);
            }
            grid.addView(line, margins(0, 0, 0, 8));
        }
        ScrollView scroll = new ScrollView(this);
        scroll.addView(grid);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Escolha um ícone")
                .setView(scroll)
                .setNegativeButton("Cancelar", null)
                .create();
        for (int i = 0; i < options.size(); i++) {
            final String icon = values.get(i);
            options.get(i).setOnClickListener(view -> {
                listener.onSelected(icon);
                dialog.dismiss();
            });
        }
        dialog.show();
    }

    private String iconSelectionLabel(String icon) {
        return icon == null || icon.isEmpty() ? "SEM ÍCONE  ›" : icon + "    ÍCONE SELECIONADO  ›";
    }

    private void confirmDeleteProduct(Product product, AlertDialog parent) {
        new AlertDialog.Builder(this).setTitle("Excluir produto?")
                .setMessage("O histórico de vendas será mantido, mas o produto sairá do catálogo.")
                .setNegativeButton("Voltar", null)
                .setPositiveButton("Excluir", (dialog, which) -> {
                    products.removeIf(entry -> entry.id == product.id);
                    cart.removeIf(entry -> entry.productId == product.id);
                    store.saveProducts(products);
                    parent.dismiss();
                    showProducts();
                    toast("Produto excluído.");
                }).show();
    }

    private void showSale() {
        reload();
        LinearLayout page = page("Nova venda", "Monte o pedido e confirme o recebimento");
        List<Product> available = availableProducts();
        if (products.isEmpty()) {
            page.addView(emptyState("Cadastre um produto primeiro", "Você precisa de ao menos um produto para registrar vendas."));
            Button go = primaryButton("CADASTRAR PRODUTO");
            go.setOnClickListener(view -> selectTab(1));
            page.addView(go, margins(0, 14, 0, 0));
            attachScrollable(page);
            return;
        }

        page.addView(sectionTitle("Adicionar item"));
        LinearLayout addCard = card();
        TextView productChoice = label(available.isEmpty() ? "Nenhum produto disponível" : "ESCOLHER PRODUTO  ›", 15, available.isEmpty() ? MUTED : BRAND, true);
        productChoice.setGravity(Gravity.CENTER_VERTICAL);
        productChoice.setPadding(dp(14), dp(13), dp(14), dp(13));
        productChoice.setBackground(roundRect(Color.rgb(246, 250, 248), 10, Color.rgb(190, 211, 205)));
        addCard.addView(productChoice, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView choiceHelp = label("Escolha o produto e informe a quantidade no próximo passo.", 11, MUTED, false);
        choiceHelp.setPadding(dp(3), dp(9), dp(3), 0);
        addCard.addView(choiceHelp);
        page.addView(addCard, margins(0, 0, 0, 19));

        page.addView(sectionTitle("Carrinho"));
        if (cart.isEmpty()) {
            TextView empty = label("Nenhum item adicionado.", 13, MUTED, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(20), 0, dp(24));
            page.addView(empty);
        } else {
            for (SaleItem item : cart) page.addView(cartRow(item), margins(0, 0, 0, 8));
        }

        LinearLayout details = card();
        details.addView(sectionTitle("Dados da venda"));
        details.addView(fieldLabel("Cliente cadastrado (opcional)"));
        TextView customerChoice = selectionField(draftCustomerId == 0 ? "VENDA SEM CLIENTE  ›" : draftCustomer + "  ›");
        details.addView(customerChoice, margins(0, 0, 0, 8, ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));
        TextView customerHelp = label(customers.isEmpty() ? "Cadastre clientes para vender fiado e acompanhar o que falta receber."
                : "Toque para escolher um cliente ou deixar a venda sem vínculo.", 11, MUTED, false);
        customerHelp.setPadding(dp(3), 0, dp(3), dp(9));
        details.addView(customerHelp);
        CheckBox pendingPayment = new CheckBox(this);
        pendingPayment.setText("Pagamento pendente (venda fiada)");
        pendingPayment.setTextSize(14);
        pendingPayment.setTextColor(TEXT);
        pendingPayment.setChecked(draftCustomerId != 0 && draftPendingPayment);
        pendingPayment.setVisibility(draftCustomerId == 0 ? View.GONE : View.VISIBLE);
        pendingPayment.setPadding(dp(2), dp(2), 0, dp(8));
        details.addView(pendingPayment);
        details.addView(fieldLabel("Forma de pagamento"));
        TextView paymentChoice = selectionField(pendingPayment.isChecked() ? "FIADO • receber depois" : PAYMENT_METHODS[draftPayment] + "  ›");
        paymentChoice.setEnabled(!pendingPayment.isChecked());
        paymentChoice.setAlpha(pendingPayment.isChecked() ? .55f : 1f);
        paymentChoice.setOnClickListener(view -> { if (!pendingPayment.isChecked()) showPaymentPicker(paymentChoice); });
        details.addView(paymentChoice, margins(0, 0, 0, 11, ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));
        details.addView(fieldLabel("Desconto (opcional)"));
        EditText discount = input("Desconto (R$)", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        discount.setText(draftDiscount);
        details.addView(discount, margins(0, 0, 0, 11, ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));
        details.addView(fieldLabel("Observação (opcional)"));
        EditText note = multilineInput("Ex.: entregar após as 18h, pedido já pago...");
        note.setText(draftNote);
        details.addView(note, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(94)));
        page.addView(details, margins(0, 10, 0, 12));

        long subtotal = cartSubtotal();
        Long draftDiscountValue = parseMoney(draftDiscount);
        long displayedTotal = Math.max(0, subtotal - (draftDiscountValue == null ? 0 : draftDiscountValue));
        LinearLayout totalCard = card();
        totalCard.setOrientation(LinearLayout.HORIZONTAL);
        totalCard.setGravity(Gravity.CENTER_VERTICAL);
        totalCard.addView(label("Total do pedido", 15, TEXT, true), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView totalValue = label(money(displayedTotal), 24, BRAND, true);
        totalCard.addView(totalValue);
        page.addView(totalCard, margins(0, 0, 0, 11));
        Button finish = primaryButton("CONCLUIR VENDA");
        finish.setEnabled(!cart.isEmpty());
        finish.setAlpha(cart.isEmpty() ? .45f : 1f);
        page.addView(finish);

        discount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence text, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence text, int start, int before, int count) {
                Long value = parseMoney(text.toString());
                totalValue.setText(money(Math.max(0, cartSubtotal() - (value == null ? 0 : value))));
            }
            @Override public void afterTextChanged(Editable editable) { }
        });
        pendingPayment.setOnCheckedChangeListener((button, checked) -> {
            draftPendingPayment = checked;
            paymentChoice.setText(checked ? "FIADO • receber depois" : PAYMENT_METHODS[draftPayment] + "  ›");
            paymentChoice.setEnabled(!checked);
            paymentChoice.setAlpha(checked ? .55f : 1f);
        });
        customerChoice.setOnClickListener(view -> {
            preserveDraft(discount, note);
            showCustomerPicker();
        });

        productChoice.setOnClickListener(view -> {
            preserveDraft(discount, note);
            if (available.isEmpty()) { toast("Todos os produtos com estoque controlado estão zerados."); return; }
            showProductPicker(available, this::showProductQuantityDialog);
        });
        finish.setOnClickListener(view -> {
            preserveDraft(discount, note);
            Long discountValue = parseMoney(draftDiscount);
            if (discountValue == null || discountValue < 0 || discountValue > cartSubtotal()) {
                toast("Informe um desconto válido, menor ou igual ao subtotal.");
                return;
            }
            finish.setEnabled(false);
            if (!finishSale(discountValue, draftPendingPayment ? "Fiado" : PAYMENT_METHODS[draftPayment], draftPendingPayment)) finish.setEnabled(true);
        });
        attachScrollable(page);
    }

    private void showProductPicker(List<Product> available, ProductSelectionListener listener) {
        LinearLayout options = new LinearLayout(this);
        options.setOrientation(LinearLayout.VERTICAL);
        options.setPadding(dp(16), dp(6), dp(16), dp(10));
        List<View> rows = new ArrayList<>();
        for (int i = 0; i < available.size(); i++) {
            Product product = available.get(i);
            String stockInfo = product.stock < 0 ? "Estoque não controlado" : "Estoque: " + product.stock;
            View row = modalChoiceRow(productDisplayName(product), money(product.priceCents) + "  •  " + stockInfo);
            rows.add(row);
            options.addView(row, margins(0, 0, 0, 9));
        }
        ScrollView scroll = new ScrollView(this);
        scroll.addView(options);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Escolha um produto")
                .setView(scroll)
                .setNegativeButton("Cancelar", null)
                .create();
        for (int i = 0; i < rows.size(); i++) {
            final Product product = available.get(i);
            rows.get(i).setOnClickListener(view -> {
                dialog.dismiss();
                listener.onSelected(product);
            });
        }
        dialog.show();
    }

    private void showProductQuantityDialog(Product product) {
        int alreadyInCart = 0;
        for (SaleItem item : cart) if (item.productId == product.id) alreadyInCart += item.quantity;
        int availableStock = product.stock < 0 ? Integer.MAX_VALUE : product.stock - alreadyInCart;
        if (availableStock <= 0) { toast("Todo o estoque deste produto já está no carrinho."); return; }

        LinearLayout form = dialogForm();
        LinearLayout productHeader = card();
        productHeader.setBackground(roundRect(Color.rgb(235, 245, 242), 13, Color.rgb(192, 220, 212)));
        productHeader.addView(label(productDisplayName(product), 17, TEXT, true));
        TextView price = label(money(product.priceCents), 20, BRAND, true);
        price.setPadding(0, dp(5), 0, dp(3));
        productHeader.addView(price);
        String stockText = product.stock < 0 ? "Estoque não controlado" : "Disponível: " + availableStock;
        productHeader.addView(label(stockText, 11, MUTED, false));
        form.addView(productHeader, margins(0, 0, 0, 12));
        form.addView(fieldLabel("Quantidade"));

        LinearLayout quantityLine = new LinearLayout(this);
        quantityLine.setOrientation(LinearLayout.HORIZONTAL);
        quantityLine.setGravity(Gravity.CENTER_VERTICAL);
        TextView minus = quantityControl("−", false);
        EditText quantity = input("Quantidade", InputType.TYPE_CLASS_NUMBER);
        quantity.setText("1");
        quantity.setTextSize(18);
        quantity.setGravity(Gravity.CENTER);
        TextView plus = quantityControl("+", true);
        quantityLine.addView(minus, new LinearLayout.LayoutParams(dp(54), dp(54)));
        LinearLayout.LayoutParams quantityParams = new LinearLayout.LayoutParams(0, dp(54), 1);
        quantityParams.setMargins(dp(9), 0, dp(9), 0);
        quantityLine.addView(quantity, quantityParams);
        quantityLine.addView(plus, new LinearLayout.LayoutParams(dp(54), dp(54)));
        form.addView(quantityLine);

        TextView preview = label("Total do item: " + money(product.priceCents), 16, BRAND, true);
        preview.setGravity(Gravity.CENTER);
        preview.setPadding(dp(12), dp(13), dp(12), dp(13));
        preview.setBackground(roundRect(Color.rgb(248, 251, 249), 10, BORDER));
        form.addView(preview, margins(0, 12, 0, 0));
        Runnable updatePreview = () -> {
            Integer amount = parseInt(quantity.getText().toString());
            long total = amount == null || amount <= 0 ? 0 : product.priceCents * amount;
            preview.setText(String.format(new Locale("pt", "BR"), "Total do item: %s", money(total)));
        };
        minus.setOnClickListener(view -> {
            Integer current = parseInt(quantity.getText().toString());
            quantity.setText(String.valueOf(Math.max(1, current == null ? 1 : current - 1)));
        });
        plus.setOnClickListener(view -> {
            Integer current = parseInt(quantity.getText().toString());
            int next = (current == null ? 0 : current) + 1;
            if (product.stock >= 0 && next > availableStock) { toast("Quantidade maior que o estoque disponível."); return; }
            quantity.setText(String.valueOf(next));
        });
        quantity.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence text, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence text, int start, int before, int count) { updatePreview.run(); }
            @Override public void afterTextChanged(Editable editable) { }
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Adicionar ao carrinho")
                .setView(form)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Adicionar", null)
                .create();
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(BRAND);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                Integer amount = parseInt(quantity.getText().toString());
                if (amount == null || amount <= 0) { toast("Informe uma quantidade válida."); return; }
                if (product.stock >= 0 && amount > availableStock) { toast("Estoque disponível: " + availableStock); return; }
                SaleItem existing = null;
                for (SaleItem item : cart) if (item.productId == product.id) existing = item;
                if (existing == null) cart.add(new SaleItem(product.id, product.name, product.icon, amount, product.priceCents, product.costCents));
                else existing.quantity += amount;
                dialog.dismiss();
                showSale();
            });
        });
        dialog.show();
    }

    private TextView quantityControl(String text, boolean filled) {
        TextView control = label(text, 24, filled ? Color.WHITE : BRAND, true);
        control.setGravity(Gravity.CENTER);
        control.setBackground(roundRect(filled ? BRAND : Color.WHITE, 12, BRAND));
        return control;
    }

    private void showPaymentPicker(TextView target) {
        LinearLayout options = new LinearLayout(this);
        options.setOrientation(LinearLayout.VERTICAL);
        options.setPadding(dp(16), dp(6), dp(16), dp(10));
        List<View> rows = new ArrayList<>();
        for (int i = 0; i < PAYMENT_METHODS.length; i++) {
            String subtitle = i == draftPayment ? "Selecionado" : "Toque para selecionar";
            View row = modalChoiceRow(PAYMENT_METHODS[i], subtitle);
            rows.add(row);
            options.addView(row, margins(0, 0, 0, 9));
        }
        ScrollView scroll = new ScrollView(this);
        scroll.addView(options);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Forma de pagamento")
                .setView(scroll)
                .setNegativeButton("Cancelar", null)
                .create();
        for (int i = 0; i < rows.size(); i++) {
            final int selected = i;
            rows.get(i).setOnClickListener(view -> {
                draftPayment = selected;
                target.setText(String.format(new Locale("pt", "BR"), "%s  ›", PAYMENT_METHODS[selected]));
                dialog.dismiss();
            });
        }
        dialog.show();
    }

    private void showCustomerPicker() {
        reload();
        LinearLayout options = new LinearLayout(this);
        options.setOrientation(LinearLayout.VERTICAL);
        options.setPadding(dp(16), dp(6), dp(16), dp(10));

        View noCustomer = modalChoiceRow("Venda sem cliente", "Não vincular esta venda a um cadastro");
        options.addView(noCustomer, margins(0, 0, 0, 9));
        List<View> rows = new ArrayList<>();
        for (Customer customer : customers) {
            long debt = customerOutstanding(customer.id);
            View row = modalChoiceRow(customer.name, customer.phone.isEmpty()
                    ? (debt > 0 ? "Pendente: " + money(debt) : "Sem pendências")
                    : customer.phone + (debt > 0 ? "  •  Pendente: " + money(debt) : ""));
            rows.add(row);
            options.addView(row, margins(0, 0, 0, 9));
        }
        Button add = secondaryButton("+  CADASTRAR NOVO CLIENTE");
        options.addView(add, margins(0, 4, 0, 0, ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));

        ScrollView scroll = new ScrollView(this);
        scroll.addView(options);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Escolha o cliente")
                .setView(scroll).setNegativeButton("Cancelar", null).create();
        noCustomer.setOnClickListener(view -> {
            draftCustomerId = 0;
            draftCustomer = "";
            draftPendingPayment = false;
            dialog.dismiss();
            showSale();
        });
        for (int i = 0; i < rows.size(); i++) {
            Customer customer = customers.get(i);
            rows.get(i).setOnClickListener(view -> {
                draftCustomerId = customer.id;
                draftCustomer = customer.name;
                dialog.dismiss();
                showSale();
            });
        }
        add.setOnClickListener(view -> {
            dialog.dismiss();
            customerDialog(null, true);
        });
        dialog.show();
    }

    private View modalChoiceRow(String title, String subtitle) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(15), dp(13), dp(13), dp(13));
        row.setBackground(roundRect(Color.rgb(248, 251, 249), 12, Color.rgb(205, 220, 215)));
        LinearLayout text = new LinearLayout(this);
        text.setOrientation(LinearLayout.VERTICAL);
        text.addView(label(title, 15, TEXT, true));
        TextView detail = label(subtitle, 12, MUTED, false);
        detail.setPadding(0, dp(3), 0, 0);
        text.addView(detail);
        row.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(label("›", 24, BRAND, false));
        return row;
    }

    private void preserveDraft(EditText discount, EditText note) {
        draftDiscount = discount.getText().toString().trim();
        draftNote = note.getText().toString().trim();
    }

    private View cartRow(SaleItem item) {
        LinearLayout row = card();
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout info = new LinearLayout(this);
        info.setOrientation(LinearLayout.VERTICAL);
        info.addView(label(item.quantity + "x  " + saleItemDisplayName(item), 14, TEXT, true));
        info.addView(label(money(item.unitPriceCents) + " cada", 11, MUTED, false));
        row.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(label(money(item.total()), 15, TEXT, true));
        TextView remove = label("  ×", 25, DANGER, false);
        remove.setPadding(dp(8), 0, 0, 0);
        remove.setOnClickListener(view -> { cart.remove(item); showSale(); });
        row.addView(remove);
        return row;
    }

    private boolean finishSale(long discount, String payment, boolean pending) {
        if (cart.isEmpty()) {
            toast("Adicione pelo menos um item antes de concluir.");
            return false;
        }
        for (SaleItem item : cart) {
            Product product = findProduct(item.productId);
            if (product == null || (product.stock >= 0 && product.stock < item.quantity)) {
                toast("O estoque de " + item.productName + " mudou. Revise o carrinho.");
                return false;
            }
        }
        for (SaleItem item : cart) {
            Product product = findProduct(item.productId);
            if (product.stock >= 0) product.stock -= item.quantity;
        }
        if (pending && draftCustomerId == 0) {
            toast("Escolha um cliente cadastrado para vender fiado.");
            return false;
        }
        long total = Math.max(0, cartSubtotal() - discount);
        Sale sale = new Sale(store.nextId(), System.currentTimeMillis(), draftCustomer, payment, discount, draftNote,
                draftCustomerId, pending ? 0 : total, new ArrayList<>(cart));
        sales.add(sale);
        store.saveProducts(products);
        store.saveSales(sales);
        cart.clear();
        draftCustomer = "";
        draftCustomerId = 0;
        draftPendingPayment = false;
        draftDiscount = "";
        draftNote = "";
        draftPayment = 0;
        new AlertDialog.Builder(this).setTitle(pending ? "Venda fiada registrada" : "Venda concluída")
                .setMessage((pending ? "Valor a receber: " : "Total recebido: ") + money(sale.total()) + "\nPagamento: " + payment)
                .setPositiveButton("Ver início", (dialog, which) -> selectTab(0))
                .setNegativeButton("Nova venda", (dialog, which) -> showSale())
                .show();
        return true;
    }

    private void showCustomers() {
        reload();
        long totalDebt = 0;
        int debtors = 0;
        for (Customer customer : customers) {
            long debt = customerOutstanding(customer.id);
            totalDebt += debt;
            if (debt > 0) debtors++;
        }
        LinearLayout page = page("Clientes", "Cadastre clientes e acompanhe as vendas fiadas");
        LinearLayout hero = card();
        hero.setBackground(roundRect(Color.rgb(255, 247, 233), 16, Color.rgb(241, 211, 164)));
        hero.addView(label("TOTAL A RECEBER", 11, MUTED, true));
        TextView amount = label(money(totalDebt), 28, totalDebt > 0 ? ACCENT : BRAND, true);
        amount.setPadding(0, dp(5), 0, dp(3));
        hero.addView(amount);
        hero.addView(label(debtors + plural(debtors, " cliente com pendência", " clientes com pendências"), 12, MUTED, false));
        page.addView(hero, margins(0, 0, 0, 13));

        Button add = primaryButton("+  CADASTRAR CLIENTE");
        add.setOnClickListener(view -> customerDialog(null, false));
        page.addView(add, margins(0, 0, 0, 19));
        page.addView(sectionTitle("Clientes cadastrados"));
        if (customers.isEmpty()) {
            page.addView(emptyState("Nenhum cliente cadastrado", "Cadastre quem compra com frequência ou precisa pagar depois."));
        } else {
            for (Customer customer : customers) page.addView(customerRow(customer), margins(0, 0, 0, 9));
        }
        attachScrollable(page);
    }

    private View customerRow(Customer customer) {
        long debt = customerOutstanding(customer.id);
        int pendingCount = customerPendingSales(customer.id, Long.MIN_VALUE, Long.MAX_VALUE).size();
        LinearLayout row = card();
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout identity = new LinearLayout(this);
        identity.setOrientation(LinearLayout.VERTICAL);
        identity.addView(label(customer.name, 15, TEXT, true));
        if (!customer.phone.isEmpty()) identity.addView(label(customer.phone, 11, MUTED, false));
        top.addView(identity, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView value = label(debt > 0 ? money(debt) : "Em dia", 15, debt > 0 ? DANGER : BRAND, true);
        top.addView(value);
        row.addView(top);
        TextView detail = label(debt > 0 ? pendingCount + plural(pendingCount, " venda pendente", " vendas pendentes") + "  •  toque para receber"
                : "Nenhum pagamento pendente  •  toque para ver", 11, MUTED, false);
        detail.setPadding(0, dp(5), 0, 0);
        row.addView(detail);
        row.setOnClickListener(view -> showCustomerDetails(customer));
        return row;
    }

    private void customerDialog(Customer existing, boolean returnToSale) {
        LinearLayout form = dialogForm();
        EditText name = input("Nome do cliente", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        EditText phone = input("Telefone (opcional)", InputType.TYPE_CLASS_PHONE);
        EditText note = multilineInput("Observação (opcional)");
        if (existing != null) {
            name.setText(existing.name);
            phone.setText(existing.phone);
            note.setText(existing.note);
        }
        addField(form, "Nome", name);
        addField(form, "Telefone (opcional)", phone);
        form.addView(fieldLabel("Observação (opcional)"));
        form.addView(note, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(82)));
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(existing == null ? "Novo cliente" : "Editar cliente")
                .setView(form).setNegativeButton("Cancelar", null)
                .setPositiveButton(existing == null ? "Cadastrar" : "Salvar", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            String customerName = name.getText().toString().trim();
            if (customerName.isEmpty()) { name.setError("Informe o nome"); return; }
            Customer saved;
            if (existing == null) {
                saved = new Customer(store.nextId(), customerName, phone.getText().toString().trim(),
                        note.getText().toString().trim(), System.currentTimeMillis());
                customers.add(saved);
            } else {
                saved = existing;
                saved.name = customerName;
                saved.phone = phone.getText().toString().trim();
                saved.note = note.getText().toString().trim();
                for (Sale sale : sales) if (sale.customerId == saved.id) sale.customer = saved.name;
                store.saveSales(sales);
            }
            store.saveCustomers(customers);
            dialog.dismiss();
            if (returnToSale) {
                draftCustomerId = saved.id;
                draftCustomer = saved.name;
                showSale();
            } else showCustomers();
            toast(existing == null ? "Cliente cadastrado." : "Cliente atualizado.");
        }));
        dialog.show();
    }

    private void showCustomerDetails(Customer customer) {
        reload();
        Customer current = findCustomer(customer.id);
        if (current == null) return;
        long start = customerDebtPeriodStart();
        long end = customerDebtPeriodEnd();
        List<Sale> pending = customerPendingSales(current.id, start, end);
        AlertDialog[] detailDialog = new AlertDialog[1];
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(18), dp(5), dp(18), dp(12));

        LinearLayout header = card();
        header.setBackground(roundRect(Color.rgb(255, 247, 233), 14, Color.rgb(241, 211, 164)));
        header.addView(label(current.name, 19, TEXT, true));
        if (!current.phone.isEmpty()) header.addView(label(current.phone, 12, MUTED, false));
        TextView balance = label(money(customerOutstanding(current.id)), 25, customerOutstanding(current.id) > 0 ? DANGER : BRAND, true);
        balance.setPadding(0, dp(8), 0, dp(2));
        header.addView(balance);
        header.addView(label("saldo total pendente", 11, MUTED, false));
        body.addView(header, margins(0, 0, 0, 12));
        Button whatsapp = primaryButton("ENVIAR PENDÊNCIAS PELO WHATSAPP");
        whatsapp.setOnClickListener(view -> shareCustomerAccount(current));
        body.addView(whatsapp, margins(0, 0, 0, 13, ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));

        body.addView(fieldLabel("MOSTRAR PENDÊNCIAS DO PERÍODO"));
        HorizontalScrollView periodScroll = new HorizontalScrollView(this);
        periodScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        int[] modes = {-1, 0, 7, 30, -2};
        String[] names = {"Tudo", "Hoje", "7 dias", "30 dias", "Escolher"};
        List<View> periodViews = new ArrayList<>();
        for (int i = 0; i < modes.length; i++) {
            boolean selected = customerDebtDays == modes[i];
            TextView chip = label(names[i], 12, selected ? Color.WHITE : BRAND, true);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(14), dp(9), dp(14), dp(9));
            chip.setBackground(roundRect(selected ? BRAND : Color.WHITE, 20, selected ? BRAND : BORDER));
            chip.setTag(modes[i]);
            chips.addView(chip, margins(0, 0, 7, 0, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            periodViews.add(chip);
        }
        periodScroll.addView(chips);
        body.addView(periodScroll, margins(0, 0, 0, 12));
        body.addView(label("Período: " + customerDebtPeriodLabel(), 11, MUTED, false), margins(0, 0, 0, 10));

        body.addView(fieldLabel("VENDAS A RECEBER"));
        List<CheckBox> checks = new ArrayList<>();
        if (pending.isEmpty()) {
            body.addView(emptyState("Tudo certo neste período", "Não há vendas pendentes para receber."));
        } else {
            for (Sale sale : pending) {
                CheckBox check = new CheckBox(this);
                check.setTag(sale);
                check.setText("Venda #" + shortId(sale.id) + "  •  " + reportDate.format(new Date(sale.timestamp))
                        + "\nFalta " + money(sale.outstanding()) + " de " + money(sale.total()));
                check.setTextSize(13);
                check.setTextColor(TEXT);
                check.setPadding(dp(8), dp(8), dp(8), dp(8));
                check.setBackground(roundRect(Color.rgb(249, 250, 248), 10, BORDER));
                checks.add(check);
                body.addView(check, margins(0, 0, 0, 7));
            }

            LinearLayout selectLine = new LinearLayout(this);
            selectLine.setOrientation(LinearLayout.HORIZONTAL);
            Button selectAll = secondaryButton("SELECIONAR TODAS");
            Button clear = secondaryButton("LIMPAR");
            selectLine.addView(selectAll, new LinearLayout.LayoutParams(0, dp(46), 1));
            LinearLayout.LayoutParams clearParams = new LinearLayout.LayoutParams(0, dp(46), 1);
            clearParams.setMargins(dp(8), 0, 0, 0);
            selectLine.addView(clear, clearParams);
            body.addView(selectLine, margins(0, 4, 0, 12));
            selectAll.setOnClickListener(view -> { for (CheckBox check : checks) check.setChecked(true); });
            clear.setOnClickListener(view -> { for (CheckBox check : checks) check.setChecked(false); });

            body.addView(fieldLabel("RECEBIMENTO PARCIAL (OPCIONAL)"));
            EditText partial = input("Valor recebido (R$)", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            body.addView(partial, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
            TextView partialHelp = label("O valor será abatido das vendas marcadas, começando pela mais antiga.", 11, MUTED, false);
            partialHelp.setPadding(dp(3), dp(6), dp(3), dp(10));
            body.addView(partialHelp);
            Button receivePartial = secondaryButton("RECEBER VALOR DIGITADO");
            body.addView(receivePartial, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
            Button receiveFull = primaryButton("MARCAR SELECIONADAS COMO PAGAS");
            body.addView(receiveFull, margins(0, 8, 0, 0, ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));

            receivePartial.setOnClickListener(view -> {
                List<Sale> selected = checkedSales(checks);
                Long value = parseMoney(partial.getText().toString());
                if (selected.isEmpty()) { toast("Marque pelo menos uma venda."); return; }
                if (value == null || value <= 0) { partial.setError("Informe o valor recebido"); return; }
                long selectedDebt = outstandingTotal(selected);
                if (value > selectedDebt) { partial.setError("Máximo: " + money(selectedDebt)); return; }
                if (detailDialog[0] != null) detailDialog[0].dismiss();
                applyCustomerPayment(selected, value);
                toast("Recebimento de " + money(value) + " registrado.");
            });
            receiveFull.setOnClickListener(view -> {
                List<Sale> selected = checkedSales(checks);
                if (selected.isEmpty()) { toast("Marque pelo menos uma venda."); return; }
                long value = outstandingTotal(selected);
                if (detailDialog[0] != null) detailDialog[0].dismiss();
                applyCustomerPayment(selected, value);
                toast("Pagamento de " + money(value) + " registrado.");
            });
        }

        body.addView(fieldLabel("HISTÓRICO DE PAGAMENTOS"));
        List<CustomerPayment> history = customerPaymentHistory(current.id);
        if (history.isEmpty()) {
            TextView noHistory = label("Nenhum recebimento foi registrado ainda.", 12, MUTED, false);
            noHistory.setPadding(dp(12), dp(11), dp(12), dp(11));
            noHistory.setBackground(roundRect(Color.rgb(249, 250, 248), 10, BORDER));
            body.addView(noHistory);
        } else {
            for (CustomerPayment payment : history) {
                LinearLayout paymentRow = new LinearLayout(this);
                paymentRow.setOrientation(LinearLayout.HORIZONTAL);
                paymentRow.setGravity(Gravity.CENTER_VERTICAL);
                paymentRow.setPadding(dp(12), dp(10), dp(12), dp(10));
                paymentRow.setBackground(roundRect(Color.rgb(235, 245, 242), 10, Color.rgb(192, 220, 212)));
                LinearLayout paymentInfo = new LinearLayout(this);
                paymentInfo.setOrientation(LinearLayout.VERTICAL);
                paymentInfo.addView(label("Pagamento recebido", 13, TEXT, true));
                paymentInfo.addView(label(dateTime.format(new Date(payment.timestamp)), 10, MUTED, false));
                paymentRow.addView(paymentInfo, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
                paymentRow.addView(label(money(payment.amountCents), 14, BRAND, true));
                body.addView(paymentRow, margins(0, 0, 0, 6));
            }
        }

        Button edit = secondaryButton("EDITAR DADOS DO CLIENTE");
        body.addView(edit, margins(0, 14, 0, 0, ViewGroup.LayoutParams.MATCH_PARENT, dp(49)));
        ScrollView scroll = new ScrollView(this);
        scroll.addView(body);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Conta do cliente")
                .setView(scroll).setPositiveButton("Fechar", null).create();
        detailDialog[0] = dialog;
        for (View view : periodViews) view.setOnClickListener(clicked -> {
            int mode = (int) clicked.getTag();
            dialog.dismiss();
            if (mode == -2) chooseCustomerDebtPeriod(current);
            else { customerDebtDays = mode; showCustomerDetails(current); }
        });
        edit.setOnClickListener(view -> { dialog.dismiss(); customerDialog(current, false); });
        dialog.show();
    }

    private List<Sale> checkedSales(List<CheckBox> checks) {
        List<Sale> selected = new ArrayList<>();
        for (CheckBox check : checks) if (check.isChecked()) selected.add((Sale) check.getTag());
        selected.sort((first, second) -> Long.compare(first.timestamp, second.timestamp));
        return selected;
    }

    private long outstandingTotal(List<Sale> entries) {
        long total = 0;
        for (Sale sale : entries) total += sale.outstanding();
        return total;
    }

    private void applyCustomerPayment(List<Sale> selected, long amount) {
        long remaining = amount;
        for (Sale selectedSale : selected) {
            if (remaining <= 0) break;
            Sale sale = findSale(selectedSale.id);
            if (sale == null) continue;
            long applied = Math.min(remaining, sale.outstanding());
            sale.paidCents = sale.paid() + applied;
            remaining -= applied;
        }
        store.saveSales(sales);
        long customerId = selected.get(0).customerId;
        customerPayments.add(new CustomerPayment(store.nextId(), customerId, System.currentTimeMillis(), amount));
        store.saveCustomerPayments(customerPayments);
        Customer customer = findCustomer(customerId);
        if (customer != null) showCustomerDetails(customer);
    }

    private List<CustomerPayment> customerPaymentHistory(long customerId) {
        List<CustomerPayment> result = new ArrayList<>();
        for (CustomerPayment payment : customerPayments) if (payment.customerId == customerId) result.add(payment);
        result.sort((first, second) -> Long.compare(second.timestamp, first.timestamp));
        return result;
    }

    private void shareCustomerAccount(Customer customer) {
        List<Sale> pending = customerPendingSales(customer.id, Long.MIN_VALUE, Long.MAX_VALUE);
        List<CustomerPayment> history = customerPaymentHistory(customer.id);
        StringBuilder message = new StringBuilder("*Resumo de pagamentos - Venda Fácil*\n")
                .append("Cliente: ").append(customer.name).append("\n\n");
        if (pending.isEmpty()) {
            message.append("Não há pagamentos pendentes. Tudo em dia!\n");
        } else {
            message.append("*Vendas pendentes*\n");
            for (Sale sale : pending) {
                message.append("• ").append(reportDate.format(new Date(sale.timestamp)))
                        .append(" - Venda #").append(shortId(sale.id)).append("\n")
                        .append("  Total: ").append(money(sale.total()))
                        .append(" | Pago: ").append(money(sale.paid()))
                        .append(" | Falta: *").append(money(sale.outstanding())).append("*\n");
            }
            message.append("\n*Total pendente: ").append(money(customerOutstanding(customer.id))).append("*\n");
        }
        if (!history.isEmpty()) {
            message.append("\n*Pagamentos registrados*\n");
            for (int i = 0; i < Math.min(8, history.size()); i++) {
                CustomerPayment payment = history.get(i);
                message.append("• ").append(reportDate.format(new Date(payment.timestamp)))
                        .append(": ").append(money(payment.amountCents)).append("\n");
            }
        }
        String text = message.toString().trim();
        String digits = customer.phone.replaceAll("\\D", "");
        if (digits.length() >= 10) {
            if (!digits.startsWith("55")) digits = "55" + digits;
            Intent direct = new Intent(Intent.ACTION_VIEW,
                    android.net.Uri.parse("https://wa.me/" + digits + "?text=" + android.net.Uri.encode(text)));
            direct.setPackage("com.whatsapp");
            try {
                startActivity(direct);
                return;
            } catch (ActivityNotFoundException exception) {
                direct.setPackage("com.whatsapp.w4b");
                try {
                    startActivity(direct);
                    return;
                } catch (ActivityNotFoundException ignored) { }
            }
        }
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, text);
        share.setPackage("com.whatsapp");
        try {
            startActivity(share);
        } catch (ActivityNotFoundException exception) {
            share.setPackage("com.whatsapp.w4b");
            try { startActivity(share); }
            catch (ActivityNotFoundException ignored) {
                share.setPackage(null);
                startActivity(Intent.createChooser(share, "Enviar conta do cliente"));
            }
        }
    }

    private void chooseCustomerDebtPeriod(Customer customer) {
        Calendar initial = Calendar.getInstance();
        DatePickerDialog startPicker = new DatePickerDialog(this, (view, year, month, day) -> {
            Calendar start = Calendar.getInstance();
            start.set(year, month, day, 0, 0, 0);
            start.set(Calendar.MILLISECOND, 0);
            DatePickerDialog endPicker = new DatePickerDialog(this, (endView, endYear, endMonth, endDay) -> {
                Calendar end = Calendar.getInstance();
                end.set(endYear, endMonth, endDay, 23, 59, 59);
                end.set(Calendar.MILLISECOND, 999);
                if (end.getTimeInMillis() < start.getTimeInMillis()) { toast("A data final deve vir depois da inicial."); return; }
                customDebtStart = start.getTimeInMillis();
                customDebtEnd = end.getTimeInMillis();
                customerDebtDays = -2;
                showCustomerDetails(customer);
            }, initial.get(Calendar.YEAR), initial.get(Calendar.MONTH), initial.get(Calendar.DAY_OF_MONTH));
            endPicker.setTitle("Data final");
            endPicker.getDatePicker().setMinDate(start.getTimeInMillis());
            endPicker.show();
        }, initial.get(Calendar.YEAR), initial.get(Calendar.MONTH), initial.get(Calendar.DAY_OF_MONTH));
        startPicker.setTitle("Data inicial");
        startPicker.show();
    }

    private void showExpenses() {
        reload();
        LinearLayout page = page("Despesas", "Organize aluguel, contas e outros custos do negócio");
        Button add = primaryButton("+  ADICIONAR DESPESA");
        add.setOnClickListener(view -> expenseDialog(null));
        page.addView(add, margins(0, 0, 0, 16));

        page.addView(sectionTitle("Resultado do negócio"));
        page.addView(expensePeriodSelector(), margins(0, 0, 0, 12));
        long start = expensePeriodStart();
        long end = expensePeriodEnd();
        TextView period = label("Período: " + expensePeriodLabel(), 12, BRAND, true);
        period.setPadding(0, 0, 0, dp(12));
        page.addView(period);

        List<Sale> periodSales = salesInPeriod(start, end);
        long revenue = sumTotal(periodSales);
        long salesProfit = 0;
        boolean completeCosts = true;
        for (Sale sale : periodSales) {
            salesProfit += sale.profit();
            if (!sale.hasCompleteCost()) completeCosts = false;
        }
        long expenseTotal = expensesTotalInPeriod(start, end);
        long businessResult = salesProfit - expenseTotal;

        LinearLayout result = card();
        result.setBackground(roundRect(BRAND, 17, BRAND));
        result.addView(label("RESULTADO ESTIMADO", 11, Color.rgb(202, 229, 223), true));
        TextView resultValue = label(completeCosts ? money(businessResult) : "Não calculado", 28, Color.WHITE, true);
        resultValue.setPadding(0, dp(7), 0, dp(5));
        result.addView(resultValue);
        String explanation = completeCosts
                ? "Lucro das vendas menos as despesas do período"
                : "Há produtos vendidos sem custo informado";
        result.addView(label(explanation, 12, Color.WHITE, false));
        page.addView(result, margins(0, 0, 0, 10));

        LinearLayout stats = new LinearLayout(this);
        stats.setOrientation(LinearLayout.HORIZONTAL);
        stats.addView(metricCard("Faturamento", money(revenue), periodSales.size() + plural(periodSales.size(), " venda", " vendas")),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        LinearLayout.LayoutParams expenseParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        expenseParams.setMargins(dp(9), 0, 0, 0);
        stats.addView(metricCard("Despesas", money(expenseTotal), "previstas no período"), expenseParams);
        page.addView(stats, margins(0, 0, 0, 20));

        page.addView(sectionTitle("Despesas cadastradas"));
        TextView help = label("Toque em uma despesa para editar ou excluir.", 11, MUTED, false);
        help.setPadding(0, 0, 0, dp(10));
        page.addView(help);
        if (expenses.isEmpty()) {
            page.addView(emptyState("Nenhuma despesa cadastrada", "Adicione aluguel, energia, internet ou qualquer outro custo do negócio."));
        } else {
            for (Expense expense : expenses) page.addView(expenseRow(expense), margins(0, 0, 0, 9));
        }
        attachScrollable(page);
    }

    private View expensePeriodSelector() {
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout line = new LinearLayout(this);
        line.setOrientation(LinearLayout.HORIZONTAL);
        int[] modes = {0, 30, -2};
        String[] titles = {"Este mês", "Últimos 30 dias", "Escolher período"};
        for (int i = 0; i < modes.length; i++) {
            boolean selected = expensePeriodMode == modes[i];
            TextView chip = label(titles[i], 13, selected ? Color.WHITE : BRAND, true);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(17), dp(10), dp(17), dp(10));
            chip.setBackground(roundRect(selected ? BRAND : Color.WHITE, 22, selected ? BRAND : BORDER));
            final int mode = modes[i];
            chip.setOnClickListener(view -> {
                if (mode == -2) chooseExpensePeriod();
                else { expensePeriodMode = mode; showExpenses(); }
            });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, dp(8), 0);
            line.addView(chip, params);
        }
        scroll.addView(line);
        return scroll;
    }

    private View expenseRow(Expense expense) {
        LinearLayout row = card();
        row.setPadding(dp(14), dp(13), dp(14), dp(13));
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(label(expense.name, 15, TEXT, true), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        top.addView(label(money(expense.amountCents), 16, DANGER, true));
        row.addView(top);
        long nextDue = nextExpenseDue(expense, startOfToday());
        String due = nextDue > 0 ? "Próxima: " + reportDate.format(new Date(nextDue)) : "Vencimento: " + reportDate.format(new Date(expense.firstDueTimestamp));
        TextView details = label(due + "  •  " + recurrenceLabel(expense), 11, MUTED, false);
        details.setPadding(0, dp(5), 0, 0);
        row.addView(details);
        row.setOnClickListener(view -> expenseDialog(expense));
        return row;
    }

    private void expenseDialog(Expense expense) {
        boolean editing = expense != null;
        long[] dueDate = {editing ? expense.firstDueTimestamp : todayAtNoon()};
        String[] selectedUnit = {editing ? expense.recurrenceUnit : Expense.NONE};

        LinearLayout form = dialogForm();
        EditText name = input("Ex.: Aluguel, energia, internet", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        EditText amount = input("Valor (R$)", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        EditText interval = input("1", InputType.TYPE_CLASS_NUMBER);
        EditText note = multilineInput("Informações adicionais sobre esta despesa");
        if (editing) {
            name.setText(expense.name);
            amount.setText(decimal(expense.amountCents));
            interval.setText(String.valueOf(expense.recurrenceInterval));
            note.setText(expense.note);
        } else interval.setText("1");

        form.addView(fieldLabel("Nome da despesa *"));
        form.addView(name, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));
        form.addView(fieldLabel("Valor *"));
        form.addView(amount, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));
        form.addView(fieldLabel("Primeiro vencimento *"));
        TextView dateField = selectionField(reportDate.format(new Date(dueDate[0])) + "  ›");
        dateField.setOnClickListener(view -> pickExpenseDate(dueDate, dateField));
        form.addView(dateField, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));
        TextView recurrenceHelp = label("Recorrência", 12, MUTED, true);
        recurrenceHelp.setPadding(0, dp(12), 0, dp(6));
        form.addView(recurrenceHelp);

        String[] units = {Expense.NONE, Expense.DAY, Expense.WEEK, Expense.MONTH, Expense.YEAR};
        String[] unitLabels = {"Não repetir", "Dias", "Semanas", "Meses", "Anos"};
        LinearLayout chipLine = new LinearLayout(this);
        chipLine.setOrientation(LinearLayout.HORIZONTAL);
        List<TextView> chips = new ArrayList<>();
        for (int i = 0; i < units.length; i++) {
            TextView chip = label(unitLabels[i], 12, BRAND, true);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(14), dp(9), dp(14), dp(9));
            chips.add(chip);
            final int chosen = i;
            chip.setOnClickListener(view -> {
                selectedUnit[0] = units[chosen];
                for (int j = 0; j < chips.size(); j++) styleChoiceChip(chips.get(j), units[j].equals(selectedUnit[0]));
                interval.setEnabled(!Expense.NONE.equals(selectedUnit[0]));
                interval.setAlpha(interval.isEnabled() ? 1f : .45f);
            });
            LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            chipParams.setMargins(0, 0, dp(7), 0);
            chipLine.addView(chip, chipParams);
        }
        for (int i = 0; i < chips.size(); i++) styleChoiceChip(chips.get(i), units[i].equals(selectedUnit[0]));
        HorizontalScrollView chipScroll = new HorizontalScrollView(this);
        chipScroll.setHorizontalScrollBarEnabled(false);
        chipScroll.addView(chipLine);
        form.addView(chipScroll);

        form.addView(fieldLabel("Repetir a cada"));
        form.addView(interval, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));
        interval.setEnabled(!Expense.NONE.equals(selectedUnit[0]));
        interval.setAlpha(interval.isEnabled() ? 1f : .45f);
        TextView example = label("Exemplo: aluguel no dia 20, repetindo a cada 1 mês.", 11, MUTED, false);
        example.setPadding(0, dp(6), 0, dp(4));
        form.addView(example);
        form.addView(fieldLabel("Observação (opcional)"));
        form.addView(note, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(90)));

        ScrollView scroll = new ScrollView(this);
        scroll.addView(form);
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(editing ? "Editar despesa" : "Nova despesa")
                .setView(scroll)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Salvar", null);
        if (editing) builder.setNeutralButton("Excluir", null);
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(BRAND);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
                String expenseName = name.getText().toString().trim();
                Long amountValue = parseMoney(amount.getText().toString());
                Integer every = Expense.NONE.equals(selectedUnit[0]) ? 1 : parseInt(interval.getText().toString());
                if (expenseName.isEmpty() || amount.getText().toString().trim().isEmpty()
                        || amountValue == null || amountValue <= 0 || every == null || every <= 0) {
                    toast("Preencha corretamente nome, valor e recorrência.");
                    return;
                }
                if (editing) {
                    expense.name = expenseName;
                    expense.amountCents = amountValue;
                    expense.firstDueTimestamp = dueDate[0];
                    expense.recurrenceUnit = selectedUnit[0];
                    expense.recurrenceInterval = every;
                    expense.note = note.getText().toString().trim();
                } else {
                    expenses.add(new Expense(store.nextId(), expenseName, amountValue, dueDate[0],
                            selectedUnit[0], every, note.getText().toString().trim()));
                }
                store.saveExpenses(expenses);
                dialog.dismiss();
                toast(editing ? "Despesa atualizada." : "Despesa adicionada.");
                showExpenses();
            });
            if (editing) {
                Button remove = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
                remove.setTextColor(DANGER);
                remove.setOnClickListener(view -> confirmDeleteExpense(expense, dialog));
            }
        });
        dialog.show();
    }

    private void pickExpenseDate(long[] dueDate, TextView target) {
        Calendar initial = Calendar.getInstance();
        initial.setTimeInMillis(dueDate[0]);
        DatePickerDialog picker = new DatePickerDialog(this, (view, year, month, day) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(year, month, day, 12, 0, 0);
            selected.set(Calendar.MILLISECOND, 0);
            dueDate[0] = selected.getTimeInMillis();
            target.setText(String.format(new Locale("pt", "BR"), "%s  ›", reportDate.format(selected.getTime())));
        }, initial.get(Calendar.YEAR), initial.get(Calendar.MONTH), initial.get(Calendar.DAY_OF_MONTH));
        picker.setTitle("Escolha o vencimento");
        picker.show();
    }

    private void confirmDeleteExpense(Expense expense, AlertDialog parent) {
        new AlertDialog.Builder(this)
                .setTitle("Excluir esta despesa?")
                .setMessage("A despesa e todas as recorrências previstas serão removidas.")
                .setNegativeButton("Voltar", null)
                .setPositiveButton("Excluir", (dialog, which) -> {
                    expenses.removeIf(item -> item.id == expense.id);
                    store.saveExpenses(expenses);
                    parent.dismiss();
                    showExpenses();
                    toast("Despesa excluída.");
                }).show();
    }

    private void showReports() {
        reload();
        LinearLayout page = page("Relatórios", "Veja com clareza como suas vendas estão indo");
        TextView periodHelp = label("Escolha abaixo o período que deseja analisar. Todos os números e listas serão atualizados automaticamente.", 12, MUTED, false);
        periodHelp.setPadding(dp(13), dp(11), dp(13), dp(11));
        periodHelp.setBackground(roundRect(Color.rgb(235, 243, 240), 10, Color.rgb(208, 226, 220)));
        page.addView(periodHelp, margins(0, 0, 0, 12));
        page.addView(periodSelector(), margins(0, 0, 0, 16));
        long since = periodStart();
        long until = periodEnd();
        TextView activePeriod = label("Período selecionado: " + periodLabel(), 12, BRAND, true);
        activePeriod.setPadding(0, 0, 0, dp(13));
        page.addView(activePeriod);
        List<Sale> filtered = salesInPeriod(since, until);
        long revenue = sumTotal(filtered);
        long profit = 0;
        long received = 0;
        long pendingAmount = 0;
        int units = 0;
        boolean canCalculateProfit = true;
        for (Sale sale : filtered) {
            profit += sale.profit();
            received += sale.paid();
            pendingAmount += sale.outstanding();
            units += sale.units();
            if (!sale.hasCompleteCost()) canCalculateProfit = false;
        }

        LinearLayout first = new LinearLayout(this);
        first.setOrientation(LinearLayout.HORIZONTAL);
        first.addView(metricCard("Faturamento", money(revenue), filtered.size() + plural(filtered.size(), " venda", " vendas")), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        LinearLayout.LayoutParams side = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        side.setMargins(dp(9), 0, 0, 0);
        first.addView(metricCard("Lucro estimado", canCalculateProfit ? money(profit) : "Não calculado",
                canCalculateProfit ? "após custos e descontos" : "há produto sem custo"), side);
        page.addView(first, margins(0, 0, 0, 9));

        LinearLayout second = new LinearLayout(this);
        second.setOrientation(LinearLayout.HORIZONTAL);
        second.addView(metricCard("Ticket médio", money(filtered.isEmpty() ? 0 : revenue / filtered.size()), "por venda"), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        LinearLayout.LayoutParams side2 = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        side2.setMargins(dp(9), 0, 0, 0);
        second.addView(metricCard("Itens vendidos", String.valueOf(units), "unidades"), side2);
        page.addView(second, margins(0, 0, 0, 9));

        LinearLayout receivableStats = new LinearLayout(this);
        receivableStats.setOrientation(LinearLayout.HORIZONTAL);
        receivableStats.addView(metricCard("Já recebido", money(received), "das vendas do período"), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        LinearLayout.LayoutParams pendingParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        pendingParams.setMargins(dp(9), 0, 0, 0);
        receivableStats.addView(metricCard("A receber", money(pendingAmount), pendingAmount > 0 ? "pagamentos pendentes" : "tudo em dia"), pendingParams);
        page.addView(receivableStats, margins(0, 0, 0, 20));

        page.addView(sectionTitle("Por forma de pagamento"));
        LinearLayout payments = card();
        Map<String, Long> paymentTotals = new LinkedHashMap<>();
        for (Sale sale : filtered) paymentTotals.put(sale.payment, paymentTotals.getOrDefault(sale.payment, 0L) + sale.total());
        if (paymentTotals.isEmpty()) payments.addView(label("Sem dados no período.", 13, MUTED, false));
        else for (Map.Entry<String, Long> entry : paymentTotals.entrySet()) {
            float ratio = revenue == 0 ? 0 : (float) entry.getValue() / revenue;
            payments.addView(distributionLine(entry.getKey(), money(entry.getValue()), ratio));
        }
        page.addView(payments, margins(0, 0, 0, 18));

        page.addView(sectionTitle("Clientes com pagamentos pendentes"));
        LinearLayout debtorsCard = card();
        Map<String, Long> debtorTotals = new LinkedHashMap<>();
        for (Sale sale : filtered) {
            if (!sale.isPending()) continue;
            String debtor = sale.customer.isEmpty() ? "Cliente não informado" : sale.customer;
            debtorTotals.put(debtor, debtorTotals.getOrDefault(debtor, 0L) + sale.outstanding());
        }
        if (debtorTotals.isEmpty()) debtorsCard.addView(label("Nenhum pagamento pendente no período.", 13, MUTED, false));
        else for (Map.Entry<String, Long> entry : debtorTotals.entrySet()) {
            debtorsCard.addView(reportLine(entry.getKey(), money(entry.getValue())));
        }
        page.addView(debtorsCard, margins(0, 0, 0, 18));

        page.addView(sectionTitle("Produtos mais vendidos"));
        LinearLayout ranking = card();
        Map<String, Integer> quantities = new LinkedHashMap<>();
        for (Sale sale : filtered) for (SaleItem item : sale.items) {
            String productName = saleItemDisplayName(item);
            quantities.put(productName, quantities.getOrDefault(productName, 0) + item.quantity);
        }
        List<Map.Entry<String, Integer>> sorted = new ArrayList<>(quantities.entrySet());
        sorted.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
        if (sorted.isEmpty()) ranking.addView(label("Sem dados no período.", 13, MUTED, false));
        else {
            int largest = sorted.get(0).getValue();
            for (int i = 0; i < Math.min(5, sorted.size()); i++) {
                Map.Entry<String, Integer> entry = sorted.get(i);
                ranking.addView(distributionLine((i + 1) + ".  " + entry.getKey(), entry.getValue() + " un.", (float) entry.getValue() / largest));
            }
        }
        page.addView(ranking, margins(0, 0, 0, 18));

        LinearLayout exportCard = card();
        exportCard.addView(label("Compartilhar ou guardar", 15, TEXT, true));
        TextView exportHelp = label("Envie um resumo fácil de ler ou salve todos os detalhes em CSV.", 11, MUTED, false);
        exportHelp.setPadding(0, dp(5), 0, dp(12));
        exportCard.addView(exportHelp);
        Button whatsapp = primaryButton("ENVIAR RESUMO PELO WHATSAPP");
        whatsapp.setOnClickListener(view -> shareOnWhatsApp(filtered));
        exportCard.addView(whatsapp, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
        Button save = secondaryButton("SALVAR RELATÓRIO CSV");
        save.setOnClickListener(view -> saveCsv(filtered));
        exportCard.addView(save, margins(0, 9, 0, 0, ViewGroup.LayoutParams.MATCH_PARENT, dp(52)));
        page.addView(exportCard, margins(0, 0, 0, 20));
        page.addView(sectionTitle("Histórico de vendas"));
        TextView historyHelp = label("Toque em uma venda para ver os itens, o pagamento e outras informações.", 11, MUTED, false);
        historyHelp.setPadding(0, 0, 0, dp(10));
        page.addView(historyHelp);
        if (filtered.isEmpty()) page.addView(emptyState("Nenhuma venda no período", "Escolha outro período ou registre uma nova venda."));
        else for (Sale sale : filtered) page.addView(saleRow(sale, true), margins(0, 0, 0, 9));
        attachScrollable(page);
    }

    private View periodSelector() {
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout line = new LinearLayout(this);
        line.setOrientation(LinearLayout.HORIZONTAL);
        int[] days = {0, 7, 30, -2};
        String[] titles = {"Hoje", "7 dias", "30 dias", "Escolher período"};
        for (int i = 0; i < days.length; i++) {
            TextView chip = label(titles[i], 13, reportDays == days[i] ? Color.WHITE : BRAND, true);
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(dp(17), dp(10), dp(17), dp(10));
            chip.setBackground(roundRect(reportDays == days[i] ? BRAND : Color.WHITE, 22, reportDays == days[i] ? BRAND : BORDER));
            final int value = days[i];
            chip.setOnClickListener(view -> {
                if (value == -2) chooseCustomPeriod();
                else { reportDays = value; showReports(); }
            });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(0, 0, dp(8), 0);
            line.addView(chip, params);
        }
        scroll.addView(line);
        return scroll;
    }

    private void chooseCustomPeriod() {
        Calendar initial = Calendar.getInstance();
        DatePickerDialog startPicker = new DatePickerDialog(this, (view, year, month, day) -> {
            Calendar start = Calendar.getInstance();
            start.set(year, month, day, 0, 0, 0);
            start.set(Calendar.MILLISECOND, 0);
            toast("Agora escolha a data final.");
            chooseCustomPeriodEnd(start.getTimeInMillis());
        }, initial.get(Calendar.YEAR), initial.get(Calendar.MONTH), initial.get(Calendar.DAY_OF_MONTH));
        startPicker.setTitle("Data inicial do relatório");
        startPicker.show();
    }

    private void chooseCustomPeriodEnd(long startMillis) {
        Calendar initial = Calendar.getInstance();
        DatePickerDialog endPicker = new DatePickerDialog(this, (view, year, month, day) -> {
            Calendar end = Calendar.getInstance();
            end.set(year, month, day, 23, 59, 59);
            end.set(Calendar.MILLISECOND, 999);
            if (end.getTimeInMillis() < startMillis) {
                toast("A data final não pode ser anterior à inicial.");
                chooseCustomPeriodEnd(startMillis);
                return;
            }
            customReportStart = startMillis;
            customReportEnd = end.getTimeInMillis();
            reportDays = -2;
            showReports();
        }, initial.get(Calendar.YEAR), initial.get(Calendar.MONTH), initial.get(Calendar.DAY_OF_MONTH));
        endPicker.setTitle("Data final do relatório");
        endPicker.getDatePicker().setMinDate(startMillis);
        endPicker.show();
    }

    private View saleRow(Sale sale, boolean clickable) {
        LinearLayout row = card();
        row.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        String customer = sale.customer.isEmpty() ? "Venda #" + shortId(sale.id) : sale.customer;
        top.addView(label(customer, 14, TEXT, true), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        top.addView(label(money(sale.total()), 16, BRAND, true));
        row.addView(top);
        String paymentStatus = sale.isPending() ? "  •  falta " + money(sale.outstanding()) : "";
        TextView details = label(dateTime.format(new Date(sale.timestamp)) + "  •  " + sale.payment + paymentStatus + "  •  " + sale.units() + plural(sale.units(), " item", " itens"), 11, sale.isPending() ? DANGER : MUTED, false);
        details.setPadding(0, dp(4), 0, 0);
        row.addView(details);
        if (clickable) row.setOnClickListener(view -> saleDetails(sale));
        return row;
    }

    private void saleDetails(Sale sale) {
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(18), dp(6), dp(18), dp(10));

        LinearLayout header = card();
        header.setBackground(roundRect(Color.rgb(235, 245, 242), 14, Color.rgb(192, 220, 212)));
        header.addView(label("VENDA #" + shortId(sale.id), 11, MUTED, true));
        TextView total = label(money(sale.total()), 27, BRAND, true);
        total.setPadding(0, dp(5), 0, dp(5));
        header.addView(total);
        header.addView(label(dateTime.format(new Date(sale.timestamp)), 12, MUTED, false));
        body.addView(header, margins(0, 0, 0, 13));

        body.addView(fieldLabel("INFORMAÇÕES"));
        LinearLayout info = card();
        info.addView(reportLine("Pagamento", sale.payment));
        info.addView(reportLine("Cliente", sale.customer.isEmpty() ? "Não informado" : sale.customer));
        info.addView(reportLine("Situação", sale.isPending() ? (sale.paid() > 0 ? "Pago parcialmente" : "Pagamento pendente") : "Pago"));
        info.addView(reportLine("Itens", sale.units() + plural(sale.units(), " unidade", " unidades")));
        body.addView(info, margins(0, 0, 0, 13));

        body.addView(fieldLabel("PRODUTOS"));
        for (SaleItem item : sale.items) {
            LinearLayout itemRow = new LinearLayout(this);
            itemRow.setOrientation(LinearLayout.HORIZONTAL);
            itemRow.setGravity(Gravity.CENTER_VERTICAL);
            itemRow.setPadding(dp(13), dp(11), dp(13), dp(11));
            itemRow.setBackground(roundRect(Color.rgb(249, 250, 248), 10, BORDER));
            LinearLayout itemText = new LinearLayout(this);
            itemText.setOrientation(LinearLayout.VERTICAL);
            itemText.addView(label(item.quantity + "x  " + saleItemDisplayName(item), 14, TEXT, true));
            itemText.addView(label(money(item.unitPriceCents) + " cada", 11, MUTED, false));
            itemRow.addView(itemText, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            itemRow.addView(label(money(item.total()), 14, BRAND, true));
            body.addView(itemRow, margins(0, 0, 0, 7));
        }

        LinearLayout totals = card();
        totals.addView(reportLine("Subtotal", money(sale.subtotal())));
        if (sale.discountCents > 0) totals.addView(reportLine("Desconto", "-" + money(sale.discountCents)));
        totals.addView(reportLine("Total da venda", money(sale.total())));
        if (sale.isPending() || sale.paid() != sale.total()) totals.addView(reportLine("Já recebido", money(sale.paid())));
        if (sale.isPending()) totals.addView(reportLine("Falta receber", money(sale.outstanding())));
        body.addView(totals, margins(0, 6, 0, 13));

        if (!sale.note.isEmpty()) {
            body.addView(fieldLabel("OBSERVAÇÃO"));
            TextView note = label(sale.note, 13, TEXT, false);
            note.setPadding(dp(13), dp(12), dp(13), dp(12));
            note.setBackground(roundRect(Color.rgb(255, 249, 239), 10, Color.rgb(241, 216, 177)));
            body.addView(note);
        }

        ScrollView scroll = new ScrollView(this);
        scroll.addView(body);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Detalhes da venda")
                .setView(scroll).setPositiveButton("Fechar", null)
                .setNegativeButton("Cancelar venda", null).create();
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(DANGER);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(view -> confirmCancelSale(sale, dialog));
        });
        dialog.show();
    }

    private void confirmCancelSale(Sale sale, AlertDialog parent) {
        new AlertDialog.Builder(this).setTitle("Cancelar esta venda?")
                .setMessage("A venda será apagada e os itens voltarão ao estoque atual.")
                .setNegativeButton("Voltar", null)
                .setPositiveButton("Cancelar venda", (dialog, which) -> {
                    for (SaleItem item : sale.items) {
                        Product product = findProduct(item.productId);
                        if (product != null && product.stock >= 0) product.stock += item.quantity;
                    }
                    sales.removeIf(entry -> entry.id == sale.id);
                    store.saveProducts(products);
                    store.saveSales(sales);
                    parent.dismiss();
                    showReports();
                    toast("Venda cancelada e estoque atualizado.");
                }).show();
    }

    private String buildCsv(List<Sale> entries) {
        StringBuilder csv = new StringBuilder("Data;Venda;Cliente;Pagamento;Situação;Produto;Quantidade;Valor unitário;Desconto da venda;Total da venda;Valor recebido;Valor pendente;Observação\n");
        SimpleDateFormat csvDate = new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("pt", "BR"));
        for (Sale sale : entries) {
            for (SaleItem item : sale.items) {
                csv.append(csvValue(csvDate.format(new Date(sale.timestamp)))).append(';')
                        .append(shortId(sale.id)).append(';').append(csvValue(sale.customer)).append(';')
                        .append(csvValue(sale.payment)).append(';').append(csvValue(sale.isPending() ? "Pendente" : "Pago")).append(';')
                        .append(csvValue(saleItemDisplayName(item))).append(';')
                        .append(item.quantity).append(';').append(decimal(item.unitPriceCents)).append(';')
                        .append(decimal(sale.discountCents)).append(';').append(decimal(sale.total())).append(';')
                        .append(decimal(sale.paid())).append(';').append(decimal(sale.outstanding())).append(';')
                        .append(csvValue(sale.note)).append('\n');
            }
        }
        return csv.toString();
    }

    private void shareOnWhatsApp(List<Sale> entries) {
        long revenue = sumTotal(entries);
        long profit = 0;
        long discounts = 0;
        long received = 0;
        long outstanding = 0;
        int units = 0;
        boolean canCalculateProfit = true;
        Map<String, Integer> productQuantities = new LinkedHashMap<>();
        Map<String, Long> productTotals = new LinkedHashMap<>();
        Map<String, Long> paymentTotals = new LinkedHashMap<>();
        Map<String, Long> debtorTotals = new LinkedHashMap<>();
        for (Sale sale : entries) {
            profit += sale.profit();
            units += sale.units();
            discounts += sale.discountCents;
            received += sale.paid();
            outstanding += sale.outstanding();
            if (!sale.hasCompleteCost()) canCalculateProfit = false;
            paymentTotals.put(sale.payment, paymentTotals.getOrDefault(sale.payment, 0L) + sale.total());
            if (sale.isPending()) {
                String debtor = sale.customer.isEmpty() ? "Cliente não informado" : sale.customer;
                debtorTotals.put(debtor, debtorTotals.getOrDefault(debtor, 0L) + sale.outstanding());
            }
            for (SaleItem item : sale.items) {
                String productName = saleItemDisplayName(item);
                productQuantities.put(productName, productQuantities.getOrDefault(productName, 0) + item.quantity);
                productTotals.put(productName, productTotals.getOrDefault(productName, 0L) + item.total());
            }
        }

        List<Map.Entry<String, Integer>> productsByQuantity = new ArrayList<>(productQuantities.entrySet());
        productsByQuantity.sort((first, second) -> Integer.compare(second.getValue(), first.getValue()));
        StringBuilder summary = new StringBuilder("*Relatório de vendas - Venda Fácil*\n")
                .append("Período: ").append(periodLabel()).append("\n\n")
                .append("*Resumo*\n")
                .append("Vendas: ").append(entries.size()).append("\n")
                .append("Itens vendidos: ").append(units).append("\n")
                .append("Faturamento: ").append(money(revenue)).append("\n")
                .append("Já recebido: ").append(money(received)).append("\n")
                .append("A receber: ").append(money(outstanding)).append("\n")
                .append("Descontos: ").append(money(discounts)).append("\n")
                .append("Ticket médio: ").append(money(entries.isEmpty() ? 0 : revenue / entries.size())).append("\n")
                .append("Lucro estimado: ").append(canCalculateProfit ? money(profit) : "não calculado (produto sem custo)");

        if (!productsByQuantity.isEmpty()) {
            summary.append("\n\n*Produtos vendidos*\n");
            for (Map.Entry<String, Integer> entry : productsByQuantity) {
                summary.append("• ").append(entry.getKey()).append(": ")
                        .append(entry.getValue()).append(" un. — ")
                        .append(money(productTotals.get(entry.getKey()))).append("\n");
            }
        }
        if (!paymentTotals.isEmpty()) {
            summary.append("\n*Formas de pagamento*\n");
            for (Map.Entry<String, Long> entry : paymentTotals.entrySet()) {
                summary.append("• ").append(entry.getKey()).append(": ").append(money(entry.getValue())).append("\n");
            }
        }
        if (!debtorTotals.isEmpty()) {
            summary.append("\n*Clientes com pagamento pendente*\n");
            for (Map.Entry<String, Long> entry : debtorTotals.entrySet()) {
                summary.append("• ").append(entry.getKey()).append(": falta ").append(money(entry.getValue())).append("\n");
            }
        }
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_TEXT, summary.toString().trim());
        share.setPackage("com.whatsapp");
        try {
            startActivity(share);
        } catch (ActivityNotFoundException exception) {
            share.setPackage("com.whatsapp.w4b");
            try {
                startActivity(share);
            } catch (ActivityNotFoundException businessException) {
                share.setPackage(null);
                startActivity(Intent.createChooser(share, "Compartilhar relatório"));
            }
        }
    }

    private void saveCsv(List<Sale> entries) {
        pendingCsv = buildCsv(entries);
        String start = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(periodStart()));
        String end = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date(periodEnd()));
        Intent save = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        save.addCategory(Intent.CATEGORY_OPENABLE);
        save.setType("text/csv");
        save.putExtra(Intent.EXTRA_TITLE, "relatorio-vendas_" + start + "_a_" + end + ".csv");
        startActivityForResult(save, SAVE_REPORT_REQUEST);
    }

    private void exportBackupToWhatsApp() {
        try {
            String backup = store.createBackup();
            File folder = new File(getCacheDir(), "backups");
            if (!folder.exists() && !folder.mkdirs()) throw new IllegalStateException("Pasta indisponível");
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.US).format(new Date());
            File file = new File(folder, "Backup-VendaFacil_" + timestamp + ".json");
            try (FileOutputStream output = new FileOutputStream(file)) {
                output.write(backup.getBytes(StandardCharsets.UTF_8));
            }
            android.net.Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("application/json");
            share.putExtra(Intent.EXTRA_SUBJECT, "Backup completo do Venda Fácil");
            share.putExtra(Intent.EXTRA_STREAM, uri);
            share.setClipData(ClipData.newRawUri("Backup Venda Fácil", uri));
            share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            share.setPackage("com.whatsapp");
            try {
                startActivity(share);
            } catch (ActivityNotFoundException exception) {
                share.setPackage("com.whatsapp.w4b");
                try {
                    startActivity(share);
                } catch (ActivityNotFoundException businessException) {
                    share.setPackage(null);
                    startActivity(Intent.createChooser(share, "Enviar backup"));
                }
            }
        } catch (Exception exception) {
            toast("Não foi possível criar o backup.");
        }
    }

    private void chooseBackupFile() {
        Intent choose = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        choose.addCategory(Intent.CATEGORY_OPENABLE);
        choose.setType("*/*");
        startActivityForResult(choose, IMPORT_BACKUP_REQUEST);
    }

    private String readTextFile(android.net.Uri uri) throws Exception {
        StringBuilder content = new StringBuilder();
        try (InputStream stream = getContentResolver().openInputStream(uri)) {
            if (stream == null) throw new IllegalArgumentException("Arquivo indisponível");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                char[] buffer = new char[4096];
                int read;
                while ((read = reader.read(buffer)) != -1) {
                    content.append(buffer, 0, read);
                    if (content.length() > 25_000_000) throw new IllegalArgumentException("Arquivo muito grande");
                }
            }
        }
        return content.toString();
    }

    private void confirmBackupRestore(String backup, long exportedAt) {
        String date = exportedAt > 0 ? dateTime.format(new Date(exportedAt)) : "data não informada";
        new AlertDialog.Builder(this)
                .setTitle("Restaurar backup completo?")
                .setMessage("Backup criado em " + date + ".\n\nTodos os produtos, vendas, clientes, pagamentos pendentes, estoques e despesas atuais serão substituídos pelos dados deste arquivo.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Restaurar tudo", (dialog, which) -> {
                    try {
                        store.restoreBackup(backup);
                        cart.clear();
                        draftCustomer = "";
                        draftCustomerId = 0;
                        draftPendingPayment = false;
                        draftDiscount = "";
                        draftNote = "";
                        draftPayment = 0;
                        reload();
                        selectTab(0);
                        toast("Backup restaurado com sucesso.");
                    } catch (Exception exception) {
                        toast("O backup está incompleto ou danificado.");
                    }
                }).show();
    }

    private LinearLayout metricCard(String title, String value, String caption) {
        LinearLayout card = card();
        card.setPadding(dp(13), dp(13), dp(13), dp(13));
        card.addView(label(title.toUpperCase(new Locale("pt", "BR")), 10, MUTED, true));
        TextView amount = label(value, value.length() > 12 ? 16 : 21, TEXT, true);
        amount.setPadding(0, dp(5), 0, dp(3));
        card.addView(amount);
        card.addView(label(caption, 10, MUTED, false));
        return card;
    }

    private View nextStepCard(String number, String title, String description, String action, int targetTab) {
        LinearLayout box = card();
        box.setBackground(roundRect(Color.rgb(255, 249, 239), 14, Color.rgb(241, 216, 177)));
        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.HORIZONTAL);
        heading.setGravity(Gravity.CENTER_VERTICAL);
        TextView badge = label(number, 14, Color.WHITE, true);
        badge.setGravity(Gravity.CENTER);
        badge.setBackground(roundRect(ACCENT, 20, ACCENT));
        heading.addView(badge, new LinearLayout.LayoutParams(dp(32), dp(32)));
        TextView titleView = label(title, 15, TEXT, true);
        titleView.setPadding(dp(10), 0, 0, 0);
        heading.addView(titleView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        box.addView(heading);
        TextView body = label(description, 12, MUTED, false);
        body.setPadding(0, dp(10), 0, dp(10));
        box.addView(body);
        TextView actionView = label(action + "  ›", 12, BRAND, true);
        actionView.setGravity(Gravity.END);
        box.addView(actionView);
        box.setOnClickListener(view -> selectTab(targetTab));
        return box;
    }

    private View reportLine(String name, String value) {
        LinearLayout line = new LinearLayout(this);
        line.setOrientation(LinearLayout.HORIZONTAL);
        line.setGravity(Gravity.CENTER_VERTICAL);
        line.setPadding(0, dp(7), 0, dp(7));
        line.addView(label(name, 13, TEXT, false), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        line.addView(label(value, 13, BRAND, true));
        return line;
    }

    private View distributionLine(String name, String value, float ratio) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(0, dp(6), 0, dp(8));
        box.addView(reportLine(name, value));
        LinearLayout track = new LinearLayout(this);
        track.setOrientation(LinearLayout.HORIZONTAL);
        track.setBackground(roundRect(Color.rgb(232, 236, 233), 4, Color.rgb(232, 236, 233)));
        float visibleRatio = Math.max(.025f, Math.min(1f, ratio));
        View fill = new View(this);
        fill.setBackground(roundRect(ACCENT, 4, ACCENT));
        track.addView(fill, new LinearLayout.LayoutParams(0, dp(6), visibleRatio));
        View remainder = new View(this);
        track.addView(remainder, new LinearLayout.LayoutParams(0, dp(6), 1f - visibleRatio));
        box.addView(track, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(6)));
        return box;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(15), dp(15), dp(15), dp(15));
        card.setBackground(roundRect(Color.WHITE, 14, BORDER));
        return card;
    }

    private void showWelcome() {
        new AlertDialog.Builder(this)
                .setTitle("Bem-vindo ao Venda Fácil")
                .setMessage("Organizar suas vendas é simples:\n\n1. Cadastre seus produtos\n2. Registre cada venda\n3. Acompanhe os resultados nos relatórios\n\nTudo fica salvo somente neste aparelho.")
                .setPositiveButton("COMEÇAR", (dialog, which) -> store.markWelcomeSeen())
                .setCancelable(false)
                .show();
    }

    private LinearLayout emptyState(String title, String subtitle) {
        LinearLayout box = card();
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(22), dp(32), dp(22), dp(32));
        TextView mark = label("＋", 34, ACCENT, false);
        mark.setGravity(Gravity.CENTER);
        box.addView(mark);
        TextView heading = label(title, 16, TEXT, true);
        heading.setGravity(Gravity.CENTER);
        box.addView(heading);
        TextView sub = label(subtitle, 12, MUTED, false);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, dp(6), 0, 0);
        box.addView(sub);
        return box;
    }

    private TextView sectionTitle(String text) {
        TextView view = label(text, 15, TEXT, true);
        view.setPadding(0, 0, 0, dp(9));
        return view;
    }

    private Button primaryButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(13);
        button.setTextColor(Color.WHITE);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setAllCaps(false);
        button.setGravity(Gravity.CENTER);
        button.setBackground(roundRect(BRAND, 12, BRAND));
        button.setStateListAnimator(null);
        button.setMinHeight(0);
        return button;
    }

    private Button secondaryButton(String text) {
        Button button = primaryButton(text);
        button.setTextColor(BRAND);
        button.setBackground(roundRect(Color.WHITE, 12, BRAND));
        return button;
    }

    private TextView label(String text, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(Typeface.DEFAULT, bold ? Typeface.BOLD : Typeface.NORMAL);
        view.setLineSpacing(0, 1.08f);
        return view;
    }

    private EditText input(String hint, int inputType) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setTextSize(14);
        input.setTextColor(TEXT);
        input.setHintTextColor(Color.rgb(145, 154, 151));
        input.setSingleLine(true);
        input.setInputType(inputType);
        input.setPadding(dp(12), 0, dp(12), 0);
        input.setBackground(roundRect(Color.WHITE, 9, Color.rgb(198, 207, 203)));
        return input;
    }

    private EditText multilineInput(String hint) {
        EditText input = input(hint, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setSingleLine(false);
        input.setGravity(Gravity.TOP | Gravity.START);
        input.setPadding(dp(12), dp(11), dp(12), dp(11));
        return input;
    }

    private TextView selectionField(String text) {
        TextView field = label(text, 14, TEXT, true);
        field.setGravity(Gravity.CENTER_VERTICAL);
        field.setPadding(dp(14), 0, dp(14), 0);
        field.setBackground(roundRect(Color.WHITE, 9, Color.rgb(198, 207, 203)));
        return field;
    }

    private TextView fieldLabel(String text) {
        TextView fieldLabel = label(text, 12, MUTED, true);
        fieldLabel.setPadding(0, dp(5), 0, dp(5));
        return fieldLabel;
    }

    private LinearLayout dialogForm() {
        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(22), dp(5), dp(22), 0);
        return form;
    }

    private void addField(LinearLayout form, String title, EditText input) {
        TextView label = label(title, 12, MUTED, true);
        label.setPadding(0, dp(9), 0, dp(4));
        form.addView(label);
        form.addView(input, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));
    }

    private GradientDrawable roundRect(int fill, int radiusDp, int stroke) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(radiusDp));
        drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private LinearLayout.LayoutParams margins(int left, int top, int right, int bottom) {
        return margins(dp(left), dp(top), dp(right), dp(bottom), ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams margins(int left, int top, int right, int bottom, int width, int height) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
        params.setMargins(left, top, right, bottom);
        return params;
    }

    private List<Product> availableProducts() {
        List<Product> result = new ArrayList<>();
        for (Product product : products) if (product.stock < 0 || product.stock > 0) result.add(product);
        return result;
    }

    private Product findProduct(long id) {
        for (Product product : products) if (product.id == id) return product;
        return null;
    }

    private Customer findCustomer(long id) {
        for (Customer customer : customers) if (customer.id == id) return customer;
        return null;
    }

    private Sale findSale(long id) {
        for (Sale sale : sales) if (sale.id == id) return sale;
        return null;
    }

    private List<Sale> customerPendingSales(long customerId, long start, long end) {
        List<Sale> result = new ArrayList<>();
        for (Sale sale : sales) {
            if (sale.customerId == customerId && sale.isPending() && sale.timestamp >= start && sale.timestamp <= end) {
                result.add(sale);
            }
        }
        result.sort((first, second) -> Long.compare(first.timestamp, second.timestamp));
        return result;
    }

    private long customerOutstanding(long customerId) {
        return outstandingTotal(customerPendingSales(customerId, Long.MIN_VALUE, Long.MAX_VALUE));
    }

    private long customerDebtPeriodStart() {
        if (customerDebtDays == -1) return Long.MIN_VALUE;
        if (customerDebtDays == -2 && customDebtStart > 0) return customDebtStart;
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        if (customerDebtDays > 0) calendar.add(Calendar.DAY_OF_YEAR, -(customerDebtDays - 1));
        return calendar.getTimeInMillis();
    }

    private long customerDebtPeriodEnd() {
        if (customerDebtDays == -1) return Long.MAX_VALUE;
        if (customerDebtDays == -2 && customDebtEnd > 0) return customDebtEnd;
        return System.currentTimeMillis();
    }

    private String customerDebtPeriodLabel() {
        if (customerDebtDays == -1) return "todas as datas";
        if (customerDebtDays == 0) return "hoje";
        if (customerDebtDays > 0) return "últimos " + customerDebtDays + " dias";
        return reportDate.format(new Date(customerDebtPeriodStart())) + " até " + reportDate.format(new Date(customerDebtPeriodEnd()));
    }

    private int lowStockCount() {
        int count = 0;
        for (Product product : products) if (product.stock >= 0 && product.stock <= 5) count++;
        return count;
    }

    private long cartSubtotal() {
        long total = 0;
        for (SaleItem item : cart) total += item.total();
        return total;
    }

    private long sumTotal(List<Sale> entries) {
        long result = 0;
        for (Sale sale : entries) result += sale.total();
        return result;
    }

    private List<Sale> salesSince(long timestamp) {
        List<Sale> result = new ArrayList<>();
        for (Sale sale : sales) if (sale.timestamp >= timestamp) result.add(sale);
        return result;
    }

    private List<Sale> salesInPeriod(long start, long end) {
        List<Sale> result = new ArrayList<>();
        for (Sale sale : sales) if (sale.timestamp >= start && sale.timestamp <= end) result.add(sale);
        return result;
    }

    private long periodStart() {
        if (reportDays == -2 && customReportStart > 0) return customReportStart;
        if (reportDays == 0) return startOfToday();
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_YEAR, -(reportDays - 1));
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private long periodEnd() {
        if (reportDays == -2 && customReportEnd > 0) return customReportEnd;
        return System.currentTimeMillis();
    }

    private String periodLabel() {
        if (reportDays == 0) return "Hoje (" + reportDate.format(new Date()) + ")";
        if (reportDays == 7) return "Últimos 7 dias";
        if (reportDays == 30) return "Últimos 30 dias";
        return reportDate.format(new Date(periodStart())) + " até " + reportDate.format(new Date(periodEnd()));
    }

    private long expensePeriodStart() {
        if (expensePeriodMode == -2 && customExpenseStart > 0) return customExpenseStart;
        Calendar calendar = Calendar.getInstance();
        if (expensePeriodMode == 0) calendar.set(Calendar.DAY_OF_MONTH, 1);
        else calendar.add(Calendar.DAY_OF_YEAR, -29);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private long expensePeriodEnd() {
        if (expensePeriodMode == -2 && customExpenseEnd > 0) return customExpenseEnd;
        if (expensePeriodMode == 30) return System.currentTimeMillis();
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.MONTH, 1);
        calendar.add(Calendar.MILLISECOND, -1);
        return calendar.getTimeInMillis();
    }

    private String expensePeriodLabel() {
        if (expensePeriodMode == 0) {
            String month = new SimpleDateFormat("MMMM 'de' yyyy", new Locale("pt", "BR")).format(new Date());
            return month.substring(0, 1).toUpperCase(new Locale("pt", "BR")) + month.substring(1);
        }
        if (expensePeriodMode == 30) return "Últimos 30 dias";
        return reportDate.format(new Date(expensePeriodStart())) + " até " + reportDate.format(new Date(expensePeriodEnd()));
    }

    private void chooseExpensePeriod() {
        Calendar initial = Calendar.getInstance();
        DatePickerDialog startPicker = new DatePickerDialog(this, (view, year, month, day) -> {
            Calendar start = Calendar.getInstance();
            start.set(year, month, day, 0, 0, 0);
            start.set(Calendar.MILLISECOND, 0);
            toast("Agora escolha a data final.");
            chooseExpensePeriodEnd(start.getTimeInMillis());
        }, initial.get(Calendar.YEAR), initial.get(Calendar.MONTH), initial.get(Calendar.DAY_OF_MONTH));
        startPicker.setTitle("Início do período");
        startPicker.show();
    }

    private void chooseExpensePeriodEnd(long startMillis) {
        Calendar initial = Calendar.getInstance();
        DatePickerDialog endPicker = new DatePickerDialog(this, (view, year, month, day) -> {
            Calendar end = Calendar.getInstance();
            end.set(year, month, day, 23, 59, 59);
            end.set(Calendar.MILLISECOND, 999);
            if (end.getTimeInMillis() < startMillis) {
                toast("A data final não pode ser anterior à inicial.");
                return;
            }
            customExpenseStart = startMillis;
            customExpenseEnd = end.getTimeInMillis();
            expensePeriodMode = -2;
            showExpenses();
        }, initial.get(Calendar.YEAR), initial.get(Calendar.MONTH), initial.get(Calendar.DAY_OF_MONTH));
        endPicker.setTitle("Fim do período");
        endPicker.getDatePicker().setMinDate(startMillis);
        endPicker.show();
    }

    private long expensesTotalInPeriod(long start, long end) {
        long total = 0;
        for (Expense expense : expenses) total += expense.amountCents * expenseOccurrences(expense, start, end);
        return total;
    }

    private int expenseOccurrences(Expense expense, long start, long end) {
        Calendar occurrence = Calendar.getInstance();
        occurrence.setTimeInMillis(expense.firstDueTimestamp);
        int count = 0;
        int safety = 0;
        while (occurrence.getTimeInMillis() <= end && safety++ < 50000) {
            if (occurrence.getTimeInMillis() >= start) count++;
            if (!expense.isRecurring()) break;
            advanceExpenseOccurrence(occurrence, expense);
        }
        return count;
    }

    private long nextExpenseDue(Expense expense, long from) {
        Calendar occurrence = Calendar.getInstance();
        occurrence.setTimeInMillis(expense.firstDueTimestamp);
        int safety = 0;
        while (occurrence.getTimeInMillis() < from && expense.isRecurring() && safety++ < 50000) {
            advanceExpenseOccurrence(occurrence, expense);
        }
        return occurrence.getTimeInMillis() >= from ? occurrence.getTimeInMillis() : -1;
    }

    private void advanceExpenseOccurrence(Calendar calendar, Expense expense) {
        int amount = Math.max(1, expense.recurrenceInterval);
        if (Expense.DAY.equals(expense.recurrenceUnit)) calendar.add(Calendar.DAY_OF_YEAR, amount);
        else if (Expense.WEEK.equals(expense.recurrenceUnit)) calendar.add(Calendar.WEEK_OF_YEAR, amount);
        else if (Expense.MONTH.equals(expense.recurrenceUnit)) calendar.add(Calendar.MONTH, amount);
        else if (Expense.YEAR.equals(expense.recurrenceUnit)) calendar.add(Calendar.YEAR, amount);
    }

    private String recurrenceLabel(Expense expense) {
        if (!expense.isRecurring()) return "Pagamento único";
        int interval = Math.max(1, expense.recurrenceInterval);
        if (interval == 1) {
            if (Expense.DAY.equals(expense.recurrenceUnit)) return "Todos os dias";
            if (Expense.WEEK.equals(expense.recurrenceUnit)) return "Toda semana";
            if (Expense.MONTH.equals(expense.recurrenceUnit)) return "Todo mês";
            return "Todo ano";
        }
        String unit;
        if (Expense.DAY.equals(expense.recurrenceUnit)) unit = "dias";
        else if (Expense.WEEK.equals(expense.recurrenceUnit)) unit = "semanas";
        else if (Expense.MONTH.equals(expense.recurrenceUnit)) unit = "meses";
        else unit = "anos";
        return "A cada " + interval + " " + unit;
    }

    private void styleChoiceChip(TextView chip, boolean selected) {
        chip.setTextColor(selected ? Color.WHITE : BRAND);
        chip.setBackground(roundRect(selected ? BRAND : Color.WHITE, 20, selected ? BRAND : BORDER));
    }

    private long todayAtNoon() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 12);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private long startOfToday() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private String greeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour < 12) return "Bom dia!";
        if (hour < 18) return "Boa tarde!";
        return "Boa noite!";
    }

    private String money(long cents) { return currency.format(cents / 100.0); }

    private String decimal(long cents) { return String.format(new Locale("pt", "BR"), "%.2f", cents / 100.0); }

    private Long parseMoney(String raw) {
        if (raw == null || raw.trim().isEmpty()) return 0L;
        try {
            String clean = raw.trim().replace("R$", "").replace(" ", "");
            if (clean.contains(",")) clean = clean.replace(".", "").replace(',', '.');
            return Math.round(Double.parseDouble(clean) * 100);
        } catch (Exception exception) { return null; }
    }

    private Integer parseInt(String raw) {
        try { return Integer.parseInt(raw.trim()); } catch (Exception exception) { return null; }
    }

    private String csvValue(String value) { return "\"" + value.replace("\"", "\"\"") + "\""; }
    private String productDisplayName(Product product) { return product.icon.isEmpty() ? product.name : product.icon + "  " + product.name; }
    private String saleItemDisplayName(SaleItem item) { return item.productIcon.isEmpty() ? item.productName : item.productIcon + "  " + item.productName; }
    private String shortId(long id) { String value = String.valueOf(id); return value.substring(Math.max(0, value.length() - 6)); }
    private String plural(int count, String singular, String plural) { return count == 1 ? singular : plural; }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private void toast(String text) { Toast.makeText(this, text, Toast.LENGTH_SHORT).show(); }

    private interface ProductSelectionListener {
        void onSelected(Product product);
    }

    private interface IconSelectionListener {
        void onSelected(String icon);
    }
}
