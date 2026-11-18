package com.mitabla.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {
    private static final int GREEN = Color.rgb(24, 57, 46);
    private static final int MUTED = Color.rgb(112, 132, 111);
    private static final int LIME = Color.rgb(213, 237, 155);
    private static final int MAX_ROWS = 100;
    private static final int MAX_COLUMNS = 30;

    private int rows = 4;
    private int columns = 3;
    private String[][] cells = new String[rows][columns];
    private EditText rowsField;
    private EditText columnsField;
    private TextView countLabel;
    private LinearLayout grid;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        for (int r = 0; r < rows; r++) for (int c = 0; c < columns; c++) cells[r][c] = "";
        restore();
        getWindow().setStatusBarColor(GREEN);
        getWindow().setNavigationBarColor(GREEN);
        createScreen();
        drawGrid();
    }

    private int dp(float value) {
        return (int) (getResources().getDisplayMetrics().density * value + 0.5f);
    }

    private GradientDrawable background(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    private TextView text(String content, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(content);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private LinearLayout vertical() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private void space(LinearLayout layout, int height) {
        layout.addView(new View(this), new LinearLayout.LayoutParams(1, dp(height)));
    }

    private String columnName(int column) {
        StringBuilder name = new StringBuilder();
        for (int n = column + 1; n > 0; n = (n - 1) / 26) {
            name.insert(0, (char) ('A' + (n - 1) % 26));
        }
        return name.toString();
    }

    private void createScreen() {
        LinearLayout root = vertical();
        root.setBackgroundColor(Color.rgb(247, 247, 240));
        setContentView(root);

        // Android 15 draws under system bars; keep controls visible on all supported versions.
        if (Build.VERSION.SDK_INT >= 35) {
            root.setOnApplyWindowInsetsListener((view, insets) -> {
                android.graphics.Insets bars = insets.getInsets(
                    WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                view.setPadding(0, bars.top, 0, bars.bottom);
                return insets;
            });
        }

        TextView top = text("▦  mi tabla.", 22, Color.WHITE, true);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(22), 0, 0, 0);
        top.setBackgroundColor(GREEN);
        root.addView(top, new LinearLayout.LayoutParams(-1, dp(68)));

        ScrollView page = new ScrollView(this);
        page.setFillViewport(true);
        root.addView(page, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout content = vertical();
        content.setPadding(dp(16), dp(32), dp(16), dp(35));
        page.addView(content);

        TextView eyebrow = text("—  ORGANIZA TUS IDEAS", 11, MUTED, true);
        content.addView(eyebrow);
        space(content, 12);
        TextView title = text("Una tabla para\n todo lo que importa.", 32, GREEN, true);
        title.setLineSpacing(dp(3), 1f);
        content.addView(title);
        space(content, 12);
        content.addView(text("Crea tu tabla, añade tus datos y cópiala cuando esté lista.", 14, MUTED, false));
        space(content, 30);

        LinearLayout card = vertical();
        card.setPadding(dp(17), dp(26), dp(17), dp(25));
        card.setBackground(background(Color.WHITE, 16));
        content.addView(card, new LinearLayout.LayoutParams(-1, -2));

        card.addView(text("01 / CONFIGURACIÓN", 11, MUTED, true));
        space(card, 6);
        card.addView(text("Diseña tu tabla", 23, GREEN, true));
        space(card, 24);

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        card.addView(controls);
        rowsField = addNumberControl(controls, "Filas", rows, MAX_ROWS);
        TextView cross = text("×", 22, MUTED, false);
        cross.setGravity(Gravity.CENTER);
        controls.addView(cross, new LinearLayout.LayoutParams(dp(28), dp(78)));
        columnsField = addNumberControl(controls, "Columnas", columns, MAX_COLUMNS);
        space(card, 14);

        Button apply = button("Aplicar tamaño   ↗", LIME, GREEN);
        card.addView(apply, new LinearLayout.LayoutParams(-1, dp(49)));
        apply.setOnClickListener(view -> applySize());
        space(card, 12);
        card.addView(text("Hasta 100 filas y 30 columnas. Se conservan los datos que quepan.", 12, MUTED, false));
        space(card, 29);
        View line = new View(this);
        line.setBackgroundColor(Color.rgb(233, 239, 231));
        card.addView(line, new LinearLayout.LayoutParams(-1, dp(1)));
        space(card, 29);

        card.addView(text("02 / TUS DATOS", 11, MUTED, true));
        space(card, 6);
        card.addView(text("Rellena las celdas", 23, GREEN, true));
        space(card, 8);
        countLabel = text("", 12, MUTED, true);
        card.addView(countLabel);
        space(card, 11);
        card.addView(text("Toca una celda para escribir. Desliza la tabla a los lados.", 12, MUTED, false));
        space(card, 15);

        HorizontalScrollView horizontal = new HorizontalScrollView(this);
        horizontal.setHorizontalScrollBarEnabled(true);
        horizontal.setBackground(background(Color.rgb(239, 245, 237), 8));
        card.addView(horizontal, new LinearLayout.LayoutParams(-1, dp(410)));
        ScrollView verticalScroll = new ScrollView(this);
        horizontal.addView(verticalScroll);
        grid = vertical();
        verticalScroll.addView(grid);
        space(card, 22);

        Button copy = button("▢  Copiar tabla   ↗", GREEN, Color.WHITE);
        card.addView(copy, new LinearLayout.LayoutParams(-1, dp(52)));
        copy.setOnClickListener(view -> copyTable());
        space(card, 9);
        Button clear = button("Borrar datos", Color.rgb(244, 247, 241), GREEN);
        card.addView(clear, new LinearLayout.LayoutParams(-1, dp(48)));
        clear.setOnClickListener(view -> clearTable());
        space(card, 12);
        card.addView(text("Pégala en Excel, Google Sheets, un documento o un mensaje.", 12, MUTED, false));
    }

    private EditText addNumberControl(LinearLayout parent, String label, int value, int max) {
        LinearLayout group = vertical();
        parent.addView(group, new LinearLayout.LayoutParams(0, dp(78), 1));
        group.addView(text(label, 13, GREEN, true));
        space(group, 7);
        LinearLayout stepper = new LinearLayout(this);
        stepper.setGravity(Gravity.CENTER_VERTICAL);
        stepper.setBackground(background(Color.rgb(245, 248, 242), 8));
        group.addView(stepper, new LinearLayout.LayoutParams(-1, dp(48)));
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(String.valueOf(value));
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setSelectAllOnFocus(true);
        input.setTextColor(GREEN);
        input.setTextSize(17);
        input.setGravity(Gravity.CENTER);
        input.setBackgroundColor(Color.TRANSPARENT);
        input.setPadding(0, 0, 0, 0);
        input.setContentDescription("Número de " + label.toLowerCase());
        TextView minus = text("−", 25, GREEN, false);
        minus.setGravity(Gravity.CENTER);
        stepper.addView(minus, new LinearLayout.LayoutParams(dp(35), -1));
        stepper.addView(input, new LinearLayout.LayoutParams(0, -1, 1));
        TextView plus = text("+", 24, GREEN, false);
        plus.setGravity(Gravity.CENTER);
        stepper.addView(plus, new LinearLayout.LayoutParams(dp(35), -1));
        minus.setOnClickListener(view -> changeNumber(input, -1, max));
        plus.setOnClickListener(view -> changeNumber(input, 1, max));
        return input;
    }

    private void changeNumber(EditText input, int step, int max) {
        int current;
        try { current = Integer.parseInt(input.getText().toString()); }
        catch (NumberFormatException error) { current = 1; }
        input.setText(String.valueOf(Math.max(1, Math.min(max, current + step))));
    }

    private Button button(String label, int bg, int fg) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(13);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(fg);
        button.setAllCaps(false);
        button.setBackground(background(bg, 9));
        return button;
    }

    private void applySize() {
        int newRows, newColumns;
        try {
            newRows = Integer.parseInt(rowsField.getText().toString());
            newColumns = Integer.parseInt(columnsField.getText().toString());
        } catch (NumberFormatException error) {
            Toast.makeText(this, "Escribe números válidos.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (newRows < 1 || newRows > MAX_ROWS || newColumns < 1 || newColumns > MAX_COLUMNS) {
            Toast.makeText(this, "Elige entre 1–100 filas y 1–30 columnas.", Toast.LENGTH_LONG).show();
            return;
        }
        String[][] resized = new String[newRows][newColumns];
        for (int r = 0; r < newRows; r++) {
            for (int c = 0; c < newColumns; c++) {
                resized[r][c] = r < rows && c < columns ? cells[r][c] : "";
            }
        }
        rows = newRows;
        columns = newColumns;
        cells = resized;
        save();
        drawGrid();
        ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
            .hideSoftInputFromWindow(rowsField.getWindowToken(), 0);
        rowsField.clearFocus();
        columnsField.clearFocus();
        Toast.makeText(this, "Tabla actualizada", Toast.LENGTH_SHORT).show();
    }

    private void drawGrid() {
        countLabel.setText(rows + (rows == 1 ? " fila · " : " filas · ")
            + columns + (columns == 1 ? " columna" : " columnas"));
        grid.removeAllViews();
        LinearLayout header = new LinearLayout(this);
        header.addView(gridLabel("", true, dp(42)));
        for (int c = 0; c < columns; c++) header.addView(gridLabel(columnName(c), true, dp(150)));
        grid.addView(header);
        for (int r = 0; r < rows; r++) {
            LinearLayout row = new LinearLayout(this);
            row.addView(gridLabel(String.valueOf(r + 1), false, dp(42)));
            for (int c = 0; c < columns; c++) {
                final int rowIndex = r;
                final int columnIndex = c;
                EditText cell = new EditText(this);
                cell.setText(cells[r][c]);
                cell.setTextSize(13);
                cell.setTextColor(GREEN);
                cell.setSingleLine(false);
                cell.setGravity(Gravity.TOP | Gravity.START);
                cell.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                    | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
                cell.setPadding(dp(10), dp(12), dp(8), dp(7));
                cell.setBackground(background(
                    r % 2 == 0 ? Color.WHITE : Color.rgb(251, 253, 249), 0));
                cell.setContentDescription("Fila " + (r + 1) + ", columna " + columnName(c));
                if (r == 0) cell.setHint("Escribe aquí...");
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(150), dp(57));
                params.setMargins(0, 0, dp(1), dp(1));
                row.addView(cell, params);
                cell.addTextChangedListener(new TextWatcher() {
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        cells[rowIndex][columnIndex] = s.toString();
                        save();
                    }
                    public void afterTextChanged(Editable editable) {}
                });
            }
            grid.addView(row);
        }
    }

    private TextView gridLabel(String label, boolean heading, int width) {
        TextView view = text(label, 12, MUTED, true);
        view.setGravity(Gravity.CENTER);
        view.setBackgroundColor(heading ? Color.rgb(236, 245, 232) : Color.rgb(244, 249, 241));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, dp(heading ? 38 : 57));
        params.setMargins(0, 0, dp(1), dp(1));
        view.setLayoutParams(params);
        return view;
    }

    private void copyTable() {
        StringBuilder plain = new StringBuilder();
        StringBuilder html = new StringBuilder("<table border=\"1\" cellspacing=\"0\" cellpadding=\"6\"><tbody>");
        for (int r = 0; r < rows; r++) {
            if (r > 0) plain.append('\n');
            html.append("<tr>");
            for (int c = 0; c < columns; c++) {
                if (c > 0) plain.append('\t');
                String value = cells[r][c];
                plain.append(value.replace('\t', ' ').replace('\n', ' ').replace('\r', ' '));
                html.append("<td>").append(escapeHtml(value).replace("\n", "<br>")).append("</td>");
            }
            html.append("</tr>");
        }
        html.append("</tbody></table>");
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newHtmlText("Mi Tabla", plain.toString(), html.toString()));
        Toast.makeText(this, "¡Tabla copiada! Ya puedes pegarla.", Toast.LENGTH_SHORT).show();
    }

    private String escapeHtml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;")
            .replace(">", "&gt;").replace("\"", "&quot;");
    }

    private void clearTable() {
        new AlertDialog.Builder(this)
            .setTitle("Borrar datos")
            .setMessage("¿Borrar todos los datos de la tabla?")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Borrar", (dialog, which) -> {
                for (int r = 0; r < rows; r++) for (int c = 0; c < columns; c++) cells[r][c] = "";
                save();
                drawGrid();
                Toast.makeText(this, "Datos borrados", Toast.LENGTH_SHORT).show();
            }).show();
    }

    private void save() {
        try {
            JSONObject data = new JSONObject();
            data.put("rows", rows);
            data.put("columns", columns);
            JSONArray values = new JSONArray();
            for (int r = 0; r < rows; r++) {
                JSONArray row = new JSONArray();
                for (int c = 0; c < columns; c++) row.put(cells[r][c]);
                values.put(row);
            }
            data.put("cells", values);
            getPreferences(MODE_PRIVATE).edit().putString("table", data.toString()).apply();
        } catch (Exception ignored) {
            Toast.makeText(this, "No se pudieron guardar los datos.", Toast.LENGTH_SHORT).show();
        }
    }

    private void restore() {
        try {
            String stored = getPreferences(MODE_PRIVATE).getString("table", null);
            if (stored == null) return;
            JSONObject data = new JSONObject(stored);
            int savedRows = data.getInt("rows");
            int savedColumns = data.getInt("columns");
            if (savedRows < 1 || savedRows > MAX_ROWS || savedColumns < 1 || savedColumns > MAX_COLUMNS) return;
            JSONArray values = data.getJSONArray("cells");
            String[][] restored = new String[savedRows][savedColumns];
            for (int r = 0; r < savedRows; r++) {
                JSONArray row = values.optJSONArray(r);
                for (int c = 0; c < savedColumns; c++) {
                    restored[r][c] = row == null ? "" : row.optString(c, "");
                }
            }
            rows = savedRows;
            columns = savedColumns;
            cells = restored;
        } catch (Exception ignored) {
            // A damaged saved table should not prevent the app from opening.
        }
    }
}
