package com.scamlens.desktop;

import com.scamlens.analyzer.MessageAnalyzer;
import com.scamlens.model.AnalysisResult;
import com.scamlens.service.HistoryService;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * The Swing desktop version of ScamLens. Start it with run-desktop.bat.
 *
 * It uses exactly the same engine (MessageAnalyzer) and the same history file
 * (data/scan-history.txt) as the website. Only the screens are different.
 *
 *   header  : logo + Scanner / Dashboard / History buttons
 *   centre  : a CardLayout holding the four screens; one is shown at a time
 */
public class DesktopApp {

    static final String SCANNER = "scanner";
    static final String RESULT = "result";
    static final String DASHBOARD = "dashboard";
    static final String HISTORY = "history";

    private final JFrame frame = new JFrame("ScamLens");
    private final CardLayout cards = new CardLayout();
    private final JPanel screens = new JPanel(cards);

    private final ScannerPanel scannerPanel;
    private final ResultPanel resultPanel;
    private final DashboardPanel dashboardPanel;
    private final HistoryPanel historyPanel;

    private final PillButton navScanner = new PillButton("Scanner", PillButton.Style.NAV);
    private final PillButton navDashboard = new PillButton("Dashboard", PillButton.Style.NAV);
    private final PillButton navHistory = new PillButton("History", PillButton.Style.NAV);

    public DesktopApp(MessageAnalyzer analyzer, HistoryService history) {
        // COMPOSITION: the app is built from four screens.
        // FUNCTIONAL: each screen is told what to do next with a lambda or method reference.
        scannerPanel = new ScannerPanel(analyzer, history, this::openResult);
        resultPanel = new ResultPanel(() -> showScreen(SCANNER));
        dashboardPanel = new DashboardPanel(history, () -> showScreen(SCANNER));
        historyPanel = new HistoryPanel(history, () -> showScreen(SCANNER));

        screens.add(scannerPanel, SCANNER);
        screens.add(resultPanel, RESULT);
        screens.add(dashboardPanel, DASHBOARD);
        screens.add(historyPanel, HISTORY);

        // EVENTS: navigation buttons
        navScanner.addActionListener(e -> showScreen(SCANNER));
        navDashboard.addActionListener(e -> showScreen(DASHBOARD));
        navHistory.addActionListener(e -> showScreen(HISTORY));

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setIconImages(LogoIcon.windowIcons());
        frame.getContentPane().setBackground(Theme.BG);
        frame.setLayout(new BorderLayout());
        frame.add(buildHeader(), BorderLayout.NORTH);
        frame.add(screens, BorderLayout.CENTER);
        frame.setMinimumSize(new Dimension(1100, 720));
        frame.setSize(1280, 860);
        frame.setLocationRelativeTo(null);
        showScreen(SCANNER);
    }

    public void open() {
        frame.setVisible(true);
        scannerPanel.scrollToTop();
    }

    // ----- used by the screenshot checks -----
    JFrame frame()               { return frame; }
    ScannerPanel scannerPanel()  { return scannerPanel; }

    /** Called by the Scanner when a check is finished. */
    private void openResult(AnalysisResult result, String savedStatus) {
        resultPanel.show(result, savedStatus);
        showScreen(RESULT);
    }

    /** Shows one screen and highlights its button. Dashboard and History reload their data first. */
    void showScreen(String name) {
        if (name.equals(DASHBOARD)) dashboardPanel.refresh();
        if (name.equals(HISTORY)) historyPanel.refresh();
        cards.show(screens, name);

        Map<PillButton, Boolean> active = Map.of(
                navScanner, name.equals(SCANNER) || name.equals(RESULT),
                navDashboard, name.equals(DASHBOARD),
                navHistory, name.equals(HISTORY));
        active.forEach(PillButton::setActive);
    }

    private JPanel buildHeader() {
        JLabel logo = new JLabel(new LogoIcon(34));
        JPanel brand = Theme.flow(FlowLayout.LEFT, 10, logo,
                Theme.flow(FlowLayout.LEFT, 0,
                        Theme.label("Scam", Theme.sansBold(18), Theme.TEXT),
                        Theme.label("Lens", Theme.sansBold(18), Theme.BLUE_LIGHT)));
        JPanel nav = Theme.flow(FlowLayout.CENTER, 6, navScanner, navDashboard, navHistory);
        JPanel spacer = new JPanel();
        spacer.setOpaque(false);

        JPanel header = new JPanel(new GridLayout(1, 3));
        header.setBackground(Theme.BG);
        header.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, Theme.LINE), new EmptyBorder(14, 40, 14, 40)));
        for (JPanel part : List.of(brand, nav, spacer)) {
            JPanel holder = new JPanel(new BorderLayout());
            holder.setOpaque(false);
            holder.add(part, BorderLayout.CENTER);
            header.add(holder);
        }
        return header;
    }

    // =====================================================================
    // START
    // =====================================================================

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        // Swing must be used from its own thread (the Event Dispatch Thread)
        SwingUtilities.invokeLater(() -> {
            Theme.applyDialogColours();
            MessageAnalyzer analyzer = new MessageAnalyzer();
            HistoryService history = openHistory();
            new DesktopApp(analyzer, history).open();
        });
    }

    /** Opens data/scan-history.txt; if that fails, uses a temporary file so the app still works. */
    private static HistoryService openHistory() {
        try {
            return new HistoryService(HistoryService.defaultFile());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null,
                    "The history file could not be opened, so history won't be kept after you close the app.\n\n"
                            + e.getMessage(), "History unavailable", JOptionPane.WARNING_MESSAGE);
            try {
                Path temporary = Files.createTempFile("scamlens-history", ".txt");
                return new HistoryService(temporary);
            } catch (IOException again) {
                throw new IllegalStateException("No place to save history", again);
            }
        }
    }
}
