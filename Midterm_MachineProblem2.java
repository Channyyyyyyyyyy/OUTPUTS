package mm2;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.RingPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import org.jfree.ui.RectangleEdge;
// Add these imports for loading animation
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Timer;
import javax.swing.SwingWorker;

public class MM2 extends JFrame {
    private JButton loadButton, exportButton;
    private JPanel totalEarningsPanel, totalGamesPanel, totalGenresPanel, avgEarningsPanel;
    private JPanel chartsPanel;
    private JTable gameTable;
    private List<GameData> gameDataList;
    private Map<String, Double> genreEarningsMap;
    private double totalEarnings;
    
    // Add these variables for loading animation
    private JPanel loadingOverlay;
    private Timer animationTimer;
    private int animationAngle = 0;
    private boolean isProcessing = false;
    private JLabel statusLabel;
    
    // Define modern color scheme
    private static final Color PRIMARY_COLOR = new Color(42, 54, 88); // Dark blue
    private static final Color ACCENT_COLOR = new Color(64, 196, 170); // Teal
    private static final Color BACKGROUND_COLOR = new Color(240, 242, 245); // Light gray
    private static final Color TEXT_COLOR = new Color(30, 30, 30); // Dark gray
    private static final Color HIGHLIGHT_COLOR = new Color(255, 150, 96); // Orange
    private static final Color[] CHART_COLORS = {
        new Color(57, 106, 177),   // blue
        new Color(218, 124, 48),   // orange
        new Color(62, 150, 81),    // green
        new Color(204, 37, 41),    // red
        new Color(107, 76, 154),   // purple
        new Color(146, 36, 40),    // brown
        new Color(148, 139, 61),   // olive
        new Color(77, 190, 238),   // light blue
        new Color(231, 107, 243),  // pink
        new Color(80, 175, 153),   // teal
    };

    public MM2() {
        setTitle("Game Genre Earnings Analysis");
        setSize(1000, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // Set window icon if available
        try {
            // You would need to add this icon file to your project
            // ImageIcon icon = new ImageIcon(getClass().getResource("/resources/game_icon.png"));
            // setIconImage(icon.getImage());
        } catch (Exception e) {
            // Icon not found, continue without it
        }
        
        // Apply modern look and feel
        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBackground(BACKGROUND_COLOR);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        setContentPane(mainPanel);

        // Create header with app title
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(PRIMARY_COLOR);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        
        JLabel titleLabel = new JLabel("Game Genre Earnings Analysis");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(Color.WHITE);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        // Control buttons with modern styling
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        controlPanel.setBackground(PRIMARY_COLOR);
        
        loadButton = createStyledButton("Load CSV", new ImageIcon());
        exportButton = createStyledButton("Export Data", new ImageIcon());
        
        controlPanel.add(loadButton);
        controlPanel.add(exportButton);
        headerPanel.add(controlPanel, BorderLayout.EAST);
        
        // Stats panels with card-like styling
        JPanel statsPanel = new JPanel(new GridLayout(1, 4, 15, 0));
        statsPanel.setBackground(BACKGROUND_COLOR);
        statsPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 15, 0));
        
        totalEarningsPanel = createStatsCard("Total Earnings", "$0.00", ACCENT_COLOR);
        totalGamesPanel = createStatsCard("Total Games", "0", PRIMARY_COLOR);
        totalGenresPanel = createStatsCard("Total Genres", "0", HIGHLIGHT_COLOR);
        avgEarningsPanel = createStatsCard("Average per Game", "$0.00", new Color(104, 109, 224));
        
        statsPanel.add(totalEarningsPanel);
        statsPanel.add(totalGamesPanel);
        statsPanel.add(totalGenresPanel);
        statsPanel.add(avgEarningsPanel);

        // Chart panel with card styling
        chartsPanel = new JPanel(new GridLayout(1, 2, 15, 0));
        chartsPanel.setBackground(BACKGROUND_COLOR);
        
        // Table with modern styling
        gameTable = new JTable();
        gameTable.setRowHeight(30);
        gameTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        gameTable.setGridColor(new Color(230, 230, 230));
        gameTable.setSelectionBackground(ACCENT_COLOR.brighter());
        gameTable.setSelectionForeground(TEXT_COLOR);
        gameTable.setBorder(BorderFactory.createEmptyBorder());
        
        JTableHeader header = gameTable.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setBackground(PRIMARY_COLOR);
        header.setForeground(Color.WHITE);
        header.setBorder(BorderFactory.createEmptyBorder());
        
        JScrollPane scrollPane = new JScrollPane(gameTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Color.WHITE);
        
        JPanel tablePanel = new JPanel(new BorderLayout(0, 10));
        tablePanel.setBackground(BACKGROUND_COLOR);
        
        JLabel tableTitle = new JLabel("Game Data");
        tableTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        tableTitle.setForeground(TEXT_COLOR);
        
        tablePanel.add(tableTitle, BorderLayout.NORTH);
        tablePanel.add(scrollPane, BorderLayout.CENTER);
        
        // Card-style container for table
        JPanel tableCard = new JPanel(new BorderLayout());
        tableCard.setBackground(Color.WHITE);
        tableCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        tableCard.add(tablePanel, BorderLayout.CENTER);
        
        // Add all components to the main panel
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(statsPanel, BorderLayout.CENTER);
        
        // Create a tabbed pane for charts and table
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 14));
        tabbedPane.setBackground(Color.WHITE);
        tabbedPane.setForeground(TEXT_COLOR);
        
        JPanel chartsCard = new JPanel(new BorderLayout());
        chartsCard.setBackground(Color.WHITE);
        chartsCard.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        chartsCard.add(chartsPanel, BorderLayout.CENTER);
        
        tabbedPane.addTab("Charts", chartsCard);
        tabbedPane.addTab("Data Table", tableCard);
        
        JPanel contentPanel = new JPanel(new BorderLayout(0, 15));
        contentPanel.setBackground(BACKGROUND_COLOR);
        contentPanel.add(statsPanel, BorderLayout.NORTH);
        contentPanel.add(tabbedPane, BorderLayout.CENTER);
        
        mainPanel.add(contentPanel, BorderLayout.CENTER);
        
        // Add a status bar
        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.setBackground(new Color(230, 230, 230));
        statusBar.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        statusLabel = new JLabel("Ready to load data");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusBar.add(statusLabel, BorderLayout.WEST);
        
        mainPanel.add(statusBar, BorderLayout.SOUTH);

        // Initialize loading animation
        initLoadingAnimation();

        // Setup event handlers
        setupEventHandlers();
        
        // Add hover effects to buttons
        addButtonHoverEffect(loadButton);
        addButtonHoverEffect(exportButton);

        setLocationRelativeTo(null);
        setVisible(true);
    }
    
    // Add loading animation methods
    private JPanel createLoadingOverlay() {
        return new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Semi-transparent background
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
                g2d.setColor(new Color(240, 240, 240));
                g2d.fillRect(0, 0, getWidth(), getHeight());
                
                // Draw loading spinner
                int centerX = getWidth() / 2;
                int centerY = getHeight() / 2;
                int radius = 30;
                
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
                g2d.setColor(ACCENT_COLOR);
                g2d.setStroke(new BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                
                g2d.drawArc(centerX - radius, centerY - radius, radius * 2, radius * 2, 
                            animationAngle, 270);
                
                // Draw text below spinner
                g2d.setFont(new Font("Segoe UI", Font.BOLD, 16));
                g2d.setColor(TEXT_COLOR);
                
                String message = "Processing...";
                int textWidth = g2d.getFontMetrics().stringWidth(message);
                g2d.drawString(message, centerX - textWidth / 2, centerY + radius + 30);
                
                g2d.dispose();
            }
        };
    }

    private void initLoadingAnimation() {
        loadingOverlay = createLoadingOverlay();
        loadingOverlay.setOpaque(false);
        loadingOverlay.setVisible(false);
        
        // Add overlay as glass pane to show above other components
        setGlassPane(loadingOverlay);
        
        // Create animation timer
        animationTimer = new Timer(50, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                animationAngle = (animationAngle + 10) % 360;
                loadingOverlay.repaint();
            }
        });
    }

    // Method to show the loading overlay
    public void showProcessingAnimation() {
        if (!isProcessing) {
            isProcessing = true;
            loadingOverlay.setVisible(true);
            animationTimer.start();
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            statusLabel.setText("Processing...");
        }
    }

    // Method to hide the loading overlay
    public void hideProcessingAnimation() {
        if (isProcessing) {
            animationTimer.stop();
            loadingOverlay.setVisible(false);
            isProcessing = false;
            setCursor(Cursor.getDefaultCursor());
            statusLabel.setText("Ready");
        }
    }
    
    private JButton createStyledButton(String text, ImageIcon icon) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setForeground(Color.WHITE);
        button.setBackground(ACCENT_COLOR);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        
        if (icon != null && icon.getImage() != null) {
            button.setIcon(icon);
            button.setIconTextGap(10);
        }
        
        return button;
    }
    
    private void addButtonHoverEffect(JButton button) {
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(ACCENT_COLOR.darker());
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(ACCENT_COLOR);
            }
        });
    }
    
    private JPanel createStatsCard(String title, String initialValue, Color accentColor) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        titleLabel.setForeground(new Color(120, 120, 120));
        
        JLabel valueLabel = new JLabel(initialValue, JLabel.CENTER);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        valueLabel.setForeground(accentColor);
        
        // Add a small colored indicator
        JPanel indicator = new JPanel();
        indicator.setBackground(accentColor);
        indicator.setPreferredSize(new Dimension(5, 40));
        
        panel.add(indicator, BorderLayout.WEST);
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(valueLabel, BorderLayout.CENTER);
        
        return panel;
    }

    private void setupEventHandlers() {
        loadButton.addActionListener(e -> loadCSVFile());
        exportButton.addActionListener(e -> exportData());
    }

    private void loadCSVFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("CSV Files", "csv"));
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            final File selectedFile = fileChooser.getSelectedFile();
            
            // Show loading animation
            showProcessingAnimation();
            
            // Process file in background thread to keep UI responsive
            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    processCSVFile(selectedFile);
                    return null;
                }
                
                @Override
                protected void done() {
                    // Hide loading animation when processing is complete
                    hideProcessingAnimation();
                }
            };
            worker.execute();
        }
    }

    private void processCSVFile(File file) {
        genreEarningsMap = new HashMap<>();
        gameDataList = new ArrayList<>();
        totalEarnings = 0;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine(); // Read header
            String[] headers = parseCSVLine(line);
            
            int gameNameIndex = findColumnIndex(headers, "name", "title");
            int genreIndex = findColumnIndex(headers, "genre");
            int earningsIndex = findColumnIndex(headers, "earnings", "money", "revenue");
            
            if (genreIndex == -1 || earningsIndex == -1) {
                showNotification("Could not identify required columns in the CSV. Please ensure your CSV has genre and earnings columns.", "Column Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                
                String[] columns = parseCSVLine(line);
                if (columns.length > Math.max(genreIndex, earningsIndex)) {
                    String gameName = gameNameIndex != -1 ? columns[gameNameIndex].trim() : "Unknown Game";
                    String genre = columns[genreIndex].trim();
                    String earningsStr = columns[earningsIndex].trim();
                    
                    if (!genre.isEmpty() && !earningsStr.isEmpty()) {
                        try {
                            double earnings = Double.parseDouble(earningsStr);
                            totalEarnings += earnings;
                            genreEarningsMap.put(genre, genreEarningsMap.getOrDefault(genre, 0.0) + earnings);
                            
                            gameDataList.add(new GameData(gameName, genre, earnings));
                        } catch (NumberFormatException e) {
                            // Skip invalid earnings values
                        }
                    }
                }
            }
            
            // Sleep a bit to simulate longer processing for larger files
            try {
                Thread.sleep(800);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            updateUI();
            showNotification("Data loaded successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            
        } catch (IOException e) {
            showNotification("Error reading file: " + e.getMessage(), "File Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void showNotification(String message, String title, int messageType) {
        // Create custom-styled dialog
        JDialog dialog = new JDialog(this, title, true);
        dialog.setLayout(new BorderLayout());
        
        // Icon based on message type
        String iconType = messageType == JOptionPane.ERROR_MESSAGE ? "error" :
                         messageType == JOptionPane.WARNING_MESSAGE ? "warning" : "info";
        
        // Create content panel
        JPanel contentPanel = new JPanel(new BorderLayout(20, 20));
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        contentPanel.setBackground(Color.WHITE);
        
        // Add icon
        JLabel iconLabel = new JLabel(UIManager.getIcon("OptionPane." + iconType + "Icon"));
        contentPanel.add(iconLabel, BorderLayout.WEST);
        
        // Add message
        JLabel messageLabel = new JLabel("<html><body width='300px'>" + message + "</body></html>");
        messageLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        contentPanel.add(messageLabel, BorderLayout.CENTER);
        
        // Add OK button
        JButton okButton = new JButton("OK");
        okButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        okButton.setForeground(Color.WHITE);
        okButton.setBackground(ACCENT_COLOR);
        okButton.setFocusPainted(false);
        okButton.setBorderPainted(false);
        okButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        okButton.addActionListener(e -> dialog.dispose());
        
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.add(okButton);
        
        contentPanel.add(buttonPanel, BorderLayout.SOUTH);
        dialog.add(contentPanel);
        
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }
    
    private String[] parseCSVLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;
        
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    currentField.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                result.add(currentField.toString());
                currentField = new StringBuilder();
            } else {
                currentField.append(c);
            }
        }
        
        result.add(currentField.toString());
        return result.toArray(new String[0]);
    }
    
    private int findColumnIndex(String[] headers, String... possibleNames) {
        for (int i = 0; i < headers.length; i++) {
            String header = headers[i].toLowerCase();
            for (String name : possibleNames) {
                if (header.contains(name.toLowerCase())) {
                    return i;
                }
            }
        }
        return -1;
    }

    private void updateUI() {
        // Update statistics
        DecimalFormat currencyFormat = new DecimalFormat("$#,##0.00");
        DecimalFormat numberFormat = new DecimalFormat("#,##0");
        
        // Extract labels from panels
        Component[] totalEarningsComponents = totalEarningsPanel.getComponents();
        Component[] totalGamesComponents = totalGamesPanel.getComponents();
        Component[] totalGenresComponents = totalGenresPanel.getComponents();
        Component[] avgEarningsComponents = avgEarningsPanel.getComponents();
        
        JLabel totalEarningsLabel = null;
        JLabel totalGamesLabel = null;
        JLabel totalGenresLabel = null;
        JLabel avgEarningsLabel = null;
        
        // Find the value labels (should be the JLabel in the CENTER position)
        for (Component c : totalEarningsComponents) {
            if (c instanceof JLabel && ((BorderLayout)totalEarningsPanel.getLayout()).getConstraints(c).equals(BorderLayout.CENTER)) {
                totalEarningsLabel = (JLabel)c;
                break;
            }
        }
        
        for (Component c : totalGamesComponents) {
            if (c instanceof JLabel && ((BorderLayout)totalGamesPanel.getLayout()).getConstraints(c).equals(BorderLayout.CENTER)) {
                totalGamesLabel = (JLabel)c;
                break;
            }
        }
        
        for (Component c : totalGenresComponents) {
            if (c instanceof JLabel && ((BorderLayout)totalGenresPanel.getLayout()).getConstraints(c).equals(BorderLayout.CENTER)) {
                totalGenresLabel = (JLabel)c;
                break;
            }
        }
        
        for (Component c : avgEarningsComponents) {
            if (c instanceof JLabel && ((BorderLayout)avgEarningsPanel.getLayout()).getConstraints(c).equals(BorderLayout.CENTER)) {
                avgEarningsLabel = (JLabel)c;
                break;
            }
        }
        
        // Update the labels
        if (totalEarningsLabel != null) totalEarningsLabel.setText(currencyFormat.format(totalEarnings));
        if (totalGamesLabel != null) totalGamesLabel.setText(numberFormat.format(gameDataList.size()));
        if (totalGenresLabel != null) totalGenresLabel.setText(numberFormat.format(genreEarningsMap.size()));
        
        double averageEarnings = gameDataList.isEmpty() ? 0 : totalEarnings / gameDataList.size();
        if (avgEarningsLabel != null) avgEarningsLabel.setText(currencyFormat.format(averageEarnings));
        
        // Update table
        updateGameTable();
        
        // Update charts
        updateCharts();
    }
    
    private void updateGameTable() {
        // Sort by earnings (highest first)
        gameDataList.sort((g1, g2) -> Double.compare(g2.getEarnings(), g1.getEarnings()));
        
        DefaultTableModel model = new DefaultTableModel(
            new Object[] {"Game", "Genre", "Earnings"}, 0
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Make table non-editable
            }
        };
        
        DecimalFormat currencyFormat = new DecimalFormat("$#,##0.00");
        for (GameData game : gameDataList) {
            model.addRow(new Object[] {
                game.getName(),
                game.getGenre(),
                currencyFormat.format(game.getEarnings())
            });
        }
        
        gameTable.setModel(model);
        
        // Add zebra striping to table
        gameTable.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 248, 250));
                }
                
                // Center alignment for specific columns
                ((JLabel) c).setHorizontalAlignment(column == 2 ? JLabel.RIGHT : JLabel.LEFT);
                c.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                
                return c;
            }
        });
    }
    
    private void updateCharts() {
    chartsPanel.removeAll();
    
    // Create genre earnings pie chart with modern styling
    DefaultPieDataset genreDataset = new DefaultPieDataset();
    
    // Sort genres by earnings for better visualization
    List<Map.Entry<String, Double>> sortedGenres = new ArrayList<>(genreEarningsMap.entrySet());
    sortedGenres.sort(Map.Entry.<String, Double>comparingByValue().reversed());
    
    // Create an empty dataset first for animation
    final DefaultPieDataset animatedGenreDataset = new DefaultPieDataset();
    
    // Create the chart with empty dataset
    final JFreeChart genreChart = ChartFactory.createPieChart(
        "Genre Earnings", animatedGenreDataset, true, true, false
    );
    
    // Apply modern styling to genre chart
    styleChart(genreChart, "Top Genres by Revenue");
    
    // Create earnings distribution donut chart with empty dataset
    final DefaultPieDataset animatedDistributionDataset = new DefaultPieDataset();
    
    // Create and style the donut chart
    final JFreeChart donutChart = createDonutChart("Revenue Distribution", animatedDistributionDataset);
    styleChart(donutChart, "Revenue Distribution by Genre");
    
    // Create chart panels with card-like styling
    final ChartPanel genreChartPanel = createChartPanel(genreChart);
    final ChartPanel donutChartPanel = createChartPanel(donutChart);
    
    chartsPanel.add(genreChartPanel);
    chartsPanel.add(donutChartPanel);
    
    chartsPanel.revalidate();
    chartsPanel.repaint();
    
    // Start animations after panels are visible
    SwingUtilities.invokeLater(() -> {
        // Animation timer for pie chart
        final Timer pieChartTimer = new Timer(1000, null);
        final int[] pieCount = {0};
        
        pieChartTimer.addActionListener(e -> {
            if (pieCount[0] < sortedGenres.size()) {
                Map.Entry<String, Double> entry = sortedGenres.get(pieCount[0]);
                animatedGenreDataset.setValue(entry.getKey(), entry.getValue());
                pieCount[0]++;
                
                // Style the newly added section
                PiePlot piePlot = (PiePlot) genreChart.getPlot();
                piePlot.setSectionPaint(entry.getKey(), CHART_COLORS[(pieCount[0] - 1) % CHART_COLORS.length]);
            } else {
                pieChartTimer.stop();
            }
        });
        
        // Animation timer for donut chart
        final Timer donutChartTimer = new Timer(1000, null);
        final int[] donutCount = {0};
        
        donutChartTimer.addActionListener(e -> {
            if (donutCount[0] < sortedGenres.size()) {
                Map.Entry<String, Double> entry = sortedGenres.get(donutCount[0]);
                double percentage = (entry.getValue() / totalEarnings) * 100;
                animatedDistributionDataset.setValue(
                    entry.getKey() + " (" + String.format("%.1f", percentage) + "%)", 
                    percentage
                );
                donutCount[0]++;
                
                // Style the newly added section
                RingPlot ringPlot = (RingPlot) donutChart.getPlot();
                ringPlot.setSectionPaint(
                entry.getKey() + " (" + String.format("%.1f", percentage) + "%)", 
                    CHART_COLORS[(donutCount[0] - 1) % CHART_COLORS.length]
                   );
            } else {
                donutChartTimer.stop();
            }
        });
        
        // Start the animations with a slight delay
        Timer startDelayTimer = new Timer(3500, e -> {
            pieChartTimer.start();
            // Start donut chart animation after pie chart is halfway done
            Timer donutDelayTimer = new Timer((sortedGenres.size() * 90) / 2, event -> {
                donutChartTimer.start();
                ((Timer) event.getSource()).stop();
            });
            donutDelayTimer.setRepeats(false);
            donutDelayTimer.start();
            ((Timer) e.getSource()).stop();
        });
        startDelayTimer.setRepeats(false);
        startDelayTimer.start();
    });
}
    
   private JFreeChart createDonutChart(String title, DefaultPieDataset dataset) {
    JFreeChart chart = ChartFactory.createRingChart(
        title, dataset, true, true, false
    );
    
    // Get the plot and customize it
    RingPlot plot = (RingPlot) chart.getPlot();
    plot.setLabelFont(new Font("Segoe UI", Font.PLAIN, 12));
    plot.setLabelBackgroundPaint(new Color(255, 255, 255, 220));
    plot.setLabelOutlinePaint(null);
    plot.setShadowPaint(null);
    plot.setSectionDepth(0.35); // Adjust thickness of the ring
    plot.setCircular(true);
    plot.setSectionOutlinesVisible(true);
    plot.setSectionOutlinePaint(Color.WHITE);
    plot.setSectionOutlineStroke(new BasicStroke(2.0f));
    plot.setSeparatorsVisible(true);
    
    // Add a text in the center showing total with animation
    DecimalFormat formatter = new DecimalFormat("$#,##0");
    String centerText = "Total\n" + formatter.format(totalEarnings);
    final TextTitle subtitle = new TextTitle("");
    subtitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
    subtitle.setPaint(TEXT_COLOR);
    subtitle.setPosition(RectangleEdge.BOTTOM);
    chart.addSubtitle(subtitle);
    
    // Animate the text after a delay
    Timer textAnimTimer = new Timer(1200, e -> {
        final StringBuilder animText = new StringBuilder();
        final String[] lines = centerText.split("\n");
        final int[] charCount = {0};
        final int totalChars = centerText.length();
        
        Timer charTimer = new Timer(40, event -> {
            if (charCount[0] < totalChars) {
                int lineIdx = 0;
                int pos = charCount[0];
                
                // Find which line we're on
                while (lineIdx < lines.length - 1 && pos >= lines[lineIdx].length() + 1) {
                    pos -= lines[lineIdx].length() + 1; // +1 for the newline
                    lineIdx++;
                }
                
                if (lineIdx < lines.length) {
                    // Build the text so far
                    animText.setLength(0);
                    for (int i = 0; i < lineIdx; i++) {
                        animText.append(lines[i]).append("\n");
                    }
                    
                    // Add the current line up to current position
                    if (pos < lines[lineIdx].length()) {
                        animText.append(lines[lineIdx].substring(0, pos + 1));
                    } else {
                        animText.append(lines[lineIdx]);
                        if (lineIdx < lines.length - 1) {
                            animText.append("\n");
                        }
                    }
                }
                
                subtitle.setText(animText.toString());
                chart.fireChartChanged();
                charCount[0]++;
            } else {
                ((Timer) event.getSource()).stop();
            }
        });
        charTimer.start();
        ((Timer) e.getSource()).stop();
    });
    textAnimTimer.setRepeats(false);
    textAnimTimer.start();
    
    return chart;
}
    
    private void styleChart(JFreeChart chart, String title) {
    chart.setBackgroundPaint(Color.WHITE);
    
    // Set title
    TextTitle textTitle = new TextTitle(title);
    textTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
    textTitle.setPaint(TEXT_COLOR);
    chart.setTitle(textTitle);
    
    // Style the plot
    Plot plot = chart.getPlot();
    plot.setBackgroundPaint(Color.WHITE);
    plot.setOutlineVisible(false);
    
    // If it's a pie plot, apply additional styling
    if (plot instanceof PiePlot) {
        PiePlot piePlot = (PiePlot) plot;
        piePlot.setLabelFont(new Font("Segoe UI", Font.PLAIN, 12));
        piePlot.setLabelBackgroundPaint(new Color(255, 255, 255, 220));
        piePlot.setLabelOutlinePaint(null);
        piePlot.setShadowPaint(null);
        piePlot.setCircular(true);
        piePlot.setSectionOutlinesVisible(true);
        piePlot.setSectionOutlinePaint(Color.WHITE);
        piePlot.setSectionOutlineStroke(new BasicStroke(2.0f));
        piePlot.setLabelShadowPaint(null);
        
        // Set colors for the sections
        int colorIndex = 0;
        for (int i = 0; i < ((DefaultPieDataset)piePlot.getDataset()).getItemCount(); i++) {
            piePlot.setSectionPaint(((DefaultPieDataset)piePlot.getDataset()).getKey(i), 
                    CHART_COLORS[colorIndex % CHART_COLORS.length]);
            colorIndex++;
        }
    }
    
    // Style the legend
    chart.getLegend().setBackgroundPaint(Color.WHITE);
    chart.getLegend().setItemFont(new Font("Segoe UI", Font.PLAIN, 12));
}

private ChartPanel createChartPanel(JFreeChart chart) {
    ChartPanel panel = new ChartPanel(chart);
    panel.setBackground(Color.WHITE);
    panel.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(new Color(220, 220, 220), 1),
        BorderFactory.createEmptyBorder(15, 15, 15, 15)
    ));
    return panel;
}

 private void exportData() {
        if (gameDataList == null || gameDataList.isEmpty()) {
            showNotification("Please load data first before exporting.", "No Data", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("CSV Files", "csv"));
        fileChooser.setSelectedFile(new File("game_earnings_analysis.csv"));
        
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            
            try (PrintWriter writer = new PrintWriter(file)) {
                // Write header
                writer.println("Game,Genre,Earnings");
                
                // Write game data
                for (GameData game : gameDataList) {
                    writer.printf("\"%s\",\"%s\",%.2f%n", 
                        game.getName(), 
                        game.getGenre(), 
                        game.getEarnings());
                }
                
                // Write summary
                writer.println("\nSummary:");
                writer.printf("Total Earnings,%.2f%n", totalEarnings);
                writer.printf("Total Games,%d%n", gameDataList.size());
                writer.printf("Total Genres,%d%n", genreEarningsMap.size());
                writer.printf("Average Earnings per Game,%.2f%n", totalEarnings / gameDataList.size());
                
                // Write genre breakdown
                writer.println("\nGenre Breakdown:");
                writer.println("Genre,Revenue,Percentage");
                
                for (Map.Entry<String, Double> entry : genreEarningsMap.entrySet()) {
                    double percentage = (entry.getValue() / totalEarnings) * 100;
                    writer.printf("%s,%.2f,%.2f%%%n", 
                        entry.getKey(), 
                        entry.getValue(), 
                        percentage);
                }
                
                showNotification("Data exported successfully to " + file.getName(), "Export Complete", JOptionPane.INFORMATION_MESSAGE);
                
            } catch (IOException e) {
                showNotification("Error exporting data: " + e.getMessage(), "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

private String escapeCSV(String value) {
    if (value == null) return "";
    
    if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
        // Escape quotes by doubling them and wrap in quotes
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    
    return value;
}

public static void main(String[] args) {
    try {
        // Set the Nimbus look and feel for a modern appearance
        for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
            if ("Nimbus".equals(info.getName())) {
                UIManager.setLookAndFeel(info.getClassName());
                break;
            }
        }
    } catch (Exception e) {
        // If Nimbus is not available, use the default look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ex) {
            // Use the default look and feel
        }
    }
    
    SwingUtilities.invokeLater(() -> new MM2());
}

// Class to store game data
private static class GameData {
    private String name;
    private String genre;
    private double earnings;
    
    public GameData(String name, String genre, double earnings) {
        this.name = name;
        this.genre = genre;
        this.earnings = earnings;
    }
    
    public String getName() {
        return name;
    }
    
    public String getGenre() {
        return genre;
    }
    
    public double getEarnings() {
        return earnings;
    }
}
}
