package com.scamlens.desktop;

import com.scamlens.model.RiskLevel;
import com.scamlens.model.ScanRecord;
import com.scamlens.service.HistoryService;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Screen 4: every saved check in a table, with Delete and Delete all.
 * The full message is never shown here because it was never saved.
 */
public class HistoryPanel extends JPanel {

    private final HistoryService history;
    private final Runnable onCheckMessage;
    private final Page page = new Page();

    private final HistoryTableModel model = new HistoryTableModel();
    private final JTable table = new JTable(model);
    private final PillButton deleteSelected = new PillButton("Delete selected", PillButton.Style.DANGER);
    private final PillButton deleteAll = new PillButton("Delete all", PillButton.Style.DANGER);

    public HistoryPanel(HistoryService history, Runnable onCheckMessage) {
        super(new BorderLayout());
        this.history = history;
        this.onCheckMessage = onCheckMessage;
        styleTable();
        add(Theme.scroll(page), BorderLayout.CENTER);

        // EVENTS: buttons and table selection
        deleteSelected.addActionListener(e -> deleteSelectedRow());
        deleteAll.addActionListener(e -> deleteEverything());
        table.getSelectionModel().addListSelectionListener(e ->
                deleteSelected.setEnabled(table.getSelectedRow() >= 0));
    }

    /** Reloads the table from the history file. */
    public void refresh() {
        page.clear();
        page.row(new Chip("History", Theme.BLUE_LIGHT));
        page.gap(14);
        page.row(ScannerPanel.twoTone("Every check, ", "nothing more", 46));
        page.gap(10);
        page.row(Theme.label("Only the result of each check is saved, never the full message.",
                Theme.sans(16.5f), Theme.MUTED));
        page.gap(28);

        try {
            List<ScanRecord> records = history.findAll();
            model.setRecords(records);
            deleteSelected.setEnabled(false);
            page.row(records.isEmpty() ? emptyState() : tableCard(records.size()));
        } catch (IOException e) {
            page.row(Theme.label("Could not read the history file: " + e.getMessage(), Theme.sans(15), Theme.ERROR_TEXT));
        }
        page.refresh();
    }

    private void deleteSelectedRow() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        ScanRecord record = model.recordAt(row);
        String when = record.scannedAt().format(ResultPanel.DATE);
        if (confirm("Delete the check from " + when + "?", "Delete check")) {
            try {
                history.delete(record.id());
            } catch (IOException e) {
                showError(e);
            }
            refresh();
        }
    }

    private void deleteEverything() {
        int count = model.getRowCount();
        if (count > 0 && confirm("Delete all " + count + " saved checks? This cannot be undone.", "Delete all")) {
            try {
                history.deleteAll();
            } catch (IOException e) {
                showError(e);
            }
            refresh();
        }
    }

    private boolean confirm(String message, String title) {
        return JOptionPane.showConfirmDialog(this, message, title,
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }

    private void showError(IOException e) {
        JOptionPane.showMessageDialog(this, "Could not change the history file: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
    }

    // ---------- layout ----------

    private JComponent emptyState() {
        RoundedPanel box = new RoundedPanel(null, Theme.PANEL, Theme.LINE, 18, 24);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.add(Theme.label("No checks saved yet. Every message you check is listed here, without its full text.",
                Theme.sans(15), Theme.MUTED));
        box.add(Theme.gap(16));
        PillButton go = new PillButton("Check a message", PillButton.Style.PRIMARY);
        go.addActionListener(e -> onCheckMessage.run());
        box.add(Theme.flow(FlowLayout.LEFT, 0, go));
        return box;
    }

    private JComponent tableCard(int count) {
        RoundedPanel card = new RoundedPanel(new BorderLayout(), Theme.PANEL, Theme.LINE, 18, 0);
        JPanel toolbar = Theme.spread(
                Theme.label(count + " saved " + (count == 1 ? "check" : "checks"), Theme.mono(12.5f), Theme.MUTED),
                Theme.flow(FlowLayout.RIGHT, 8, deleteSelected, deleteAll));
        toolbar.setBorder(new EmptyBorder(16, 20, 14, 20));
        card.add(toolbar, BorderLayout.NORTH);

        JPanel tableArea = new JPanel(new BorderLayout());
        tableArea.setOpaque(false);
        tableArea.setBorder(new EmptyBorder(0, 20, 16, 20));
        tableArea.add(table.getTableHeader(), BorderLayout.NORTH);
        tableArea.add(table, BorderLayout.CENTER);
        card.add(tableArea, BorderLayout.CENTER);
        return card;
    }

    private void styleTable() {
        table.setRowHeight(46);
        table.setBackground(Theme.PANEL);
        table.setForeground(Theme.TEXT);
        table.setFont(Theme.sans(14));
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(Theme.LINE);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(false);
        table.setDefaultRenderer(Object.class, new Cell());

        int[] widths = {175, 125, 60, 390, 230};
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        // FUNCTIONAL: the header renderer is a lambda (TableCellRenderer has one method)
        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setDefaultRenderer((t, value, selected, focus, row, col) -> {
            JLabel label = new JLabel(String.valueOf(value));
            label.setOpaque(true);
            label.setBackground(Theme.PANEL);
            label.setForeground(Theme.MUTED);
            label.setFont(Theme.mono(12));
            label.setHorizontalAlignment(col == 2 ? SwingConstants.RIGHT : SwingConstants.LEFT);
            label.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, Theme.LINE_STRONG), new EmptyBorder(10, 12, 10, 12)));
            return label;
        });
    }

    /** How each cell looks (POLYMORPHISM: overrides the default renderer). */
    private static final class Cell extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focus, int row, int col) {
            super.getTableCellRendererComponent(table, value, selected, false, row, col);
            setBackground(selected ? Theme.blend(Theme.PANEL, Theme.BLUE, 0.28) : Theme.PANEL);
            setForeground(Theme.TEXT);
            setFont(Theme.sans(14));
            setIcon(null);
            setToolTipText(null);
            setHorizontalAlignment(SwingConstants.LEFT);
            setBorder(new EmptyBorder(0, 12, 0, 12));

            switch (col) {
                case 0 -> { setFont(Theme.mono(12.5f)); setForeground(Theme.MUTED); }
                case 1 -> {
                    RiskLevel level = (RiskLevel) value;
                    setText(level.label());
                    setForeground(Theme.riskColor(level));
                    setFont(Theme.sansBold(14));
                    setIcon(new Theme.DotIcon(Theme.riskColor(level)));
                    setIconTextGap(8);
                }
                case 2 -> { setFont(Theme.mono(13.5f)); setHorizontalAlignment(SwingConstants.RIGHT); }
                case 3 -> setToolTipText(String.valueOf(value));
                case 4 -> setForeground(Theme.MUTED);
                default -> { }
            }
            return this;
        }
    }

    /** Connects the list of ScanRecords to the JTable's rows and columns. */
    private static final class HistoryTableModel extends AbstractTableModel {
        private static final String[] COLUMNS = {"Checked", "Result", "Score", "Signs found", "Preview"};
        private List<ScanRecord> records = new ArrayList<>();

        void setRecords(List<ScanRecord> records) {
            this.records = records;
            fireTableDataChanged();   // tells the table to redraw
        }

        ScanRecord recordAt(int row) { return records.get(row); }

        @Override public int getRowCount()             { return records.size(); }
        @Override public int getColumnCount()          { return COLUMNS.length; }
        @Override public String getColumnName(int col) { return COLUMNS[col]; }

        @Override
        public Object getValueAt(int row, int col) {
            ScanRecord r = records.get(row);
            return switch (col) {
                case 0 -> r.scannedAt().format(ResultPanel.DATE);
                case 1 -> r.riskLevel();
                case 2 -> r.score();
                case 3 -> r.categories().isEmpty() ? "No warning signs" : r.categoryLabels();
                default -> r.preview() == null ? "Not kept" : r.preview();
            };
        }
    }
}
