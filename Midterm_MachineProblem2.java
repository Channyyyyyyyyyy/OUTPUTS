package midterm_machineproblem;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.data.general.DefaultPieDataset;

public class Midterm_MachineProblem2 extends JFrame {
    private JButton loadButton, exportButton;
    private JPanel totalEarningsPanel, totalGamesPanel, totalGenresPanel, avgEarningsPanel;
    private JPanel chartsPanel;
    private JTable gameTable;
    private List<GameData> gameDataList;
    private Map<String, Double> genreEarningsMap;
    private double totalEarnings;

    public Midterm_MachineProblem2() {
        setTitle("Game Genre Earnings Analysis");
        setSize(800, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));

        // Create main panels
        JPanel controlPanel = new JPanel(new FlowLayout());
        JPanel statsPanel = new JPanel(new GridLayout(1, 4, 10, 10));
        chartsPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        JPanel tablePanel = new JPanel(new BorderLayout());

        // Control buttons
        loadButton = new JButton("Load CSV");
        exportButton = new JButton("Export Data");
        controlPanel.add(loadButton);
        controlPanel.add(exportButton);

        // Stats panels - fixed to use JPanel instead of JLabel
        totalEarningsPanel = createStatsPanel("Total Earnings", "$0.00");
        totalGamesPanel = createStatsPanel("Total Games", "0");
        totalGenresPanel = createStatsPanel("Total Genres", "0");
        avgEarningsPanel = createStatsPanel("Average per Game", "$0.00");
        
        statsPanel.add(totalEarningsPanel);
        statsPanel.add(totalGamesPanel);
        statsPanel.add(totalGenresPanel);
        statsPanel.add(avgEarningsPanel);

        // Table setup
        gameTable = new JTable();
        JScrollPane scrollPane = new JScrollPane(gameTable);
        tablePanel.add(new JLabel("Game Data:"), BorderLayout.NORTH);
        tablePanel.add(scrollPane, BorderLayout.CENTER);

        // Add everything to the frame
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(controlPanel, BorderLayout.NORTH);
        topPanel.add(statsPanel, BorderLayout.CENTER);

        add(topPanel, BorderLayout.NORTH);
        add(chartsPanel, BorderLayout.CENTER);
        add(tablePanel, BorderLayout.SOUTH);

        // Setup event handlers
        setupEventHandlers();

        setLocationRelativeTo(null);
        setVisible(true);
    }

    private JPanel createStatsPanel(String title, String initialValue) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(title));
        
        JLabel valueLabel = new JLabel(initialValue, JLabel.CENTER);
        valueLabel.setFont(new Font("Arial", Font.BOLD, 16));
        
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
            File selectedFile = fileChooser.getSelectedFile();
            processCSVFile(selectedFile);
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
                JOptionPane.showMessageDialog(this, 
                    "Could not identify required columns in the CSV. Please ensure your CSV has genre and earnings columns.",
                    "Column Error", JOptionPane.ERROR_MESSAGE);
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
            
            updateUI();
            
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error reading file: " + e.getMessage(), 
                "File Error", JOptionPane.ERROR_MESSAGE);
        }
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
        
        JLabel totalEarningsLabel = (JLabel) totalEarningsPanel.getComponent(0);
        JLabel totalGamesLabel = (JLabel) totalGamesPanel.getComponent(0);
        JLabel totalGenresLabel = (JLabel) totalGenresPanel.getComponent(0);
        JLabel avgEarningsLabel = (JLabel) avgEarningsPanel.getComponent(0);
        
        totalEarningsLabel.setText(currencyFormat.format(totalEarnings));
        totalGamesLabel.setText(numberFormat.format(gameDataList.size()));
        totalGenresLabel.setText(numberFormat.format(genreEarningsMap.size()));
        
        double averageEarnings = gameDataList.isEmpty() ? 0 : totalEarnings / gameDataList.size();
        avgEarningsLabel.setText(currencyFormat.format(averageEarnings));
        
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
        );
        
        DecimalFormat currencyFormat = new DecimalFormat("$#,##0.00");
        for (GameData game : gameDataList) {
            model.addRow(new Object[] {
                game.getName(),
                game.getGenre(),
                currencyFormat.format(game.getEarnings())
            });
        }
        
        gameTable.setModel(model);
    }
    
    private void updateCharts() {
        chartsPanel.removeAll();
        
        // Create genre earnings pie chart
        DefaultPieDataset genreDataset = new DefaultPieDataset();
        for (Map.Entry<String, Double> entry : genreEarningsMap.entrySet()) {
            genreDataset.setValue(entry.getKey(), entry.getValue());
        }
        
        JFreeChart genreChart = ChartFactory.createPieChart(
            "Genre Earnings", genreDataset, true, true, false
        );
        
        // Create earnings distribution donut chart
        DefaultPieDataset distributionDataset = new DefaultPieDataset();
        for (Map.Entry<String, Double> entry : genreEarningsMap.entrySet()) {
            double percentage = (entry.getValue() / totalEarnings) * 100;
            distributionDataset.setValue(entry.getKey() + " (" + String.format("%.2f", percentage) + "%)", percentage);
        }
        
        JFreeChart distributionChart = ChartFactory.createPieChart(
            "Earnings Distribution", distributionDataset, true, true, false
        );
        
        // Make the distribution chart a donut chart
        PiePlot distributionPlot = (PiePlot) distributionChart.getPlot();
        distributionPlot.setCircular(true);
        distributionPlot.setLabelGenerator(null);
        
        // Add charts to panel
        ChartPanel genreChartPanel = new ChartPanel(genreChart);
        ChartPanel distributionChartPanel = new ChartPanel(distributionChart);
        
        chartsPanel.add(genreChartPanel);
        chartsPanel.add(distributionChartPanel);
        
        chartsPanel.revalidate();
        chartsPanel.repaint();
    }
    
    private void exportData() {
        if (gameDataList == null || gameDataList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please load data first before exporting.",
                "No Data", JOptionPane.WARNING_MESSAGE);
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
                for (Map.Entry<String, Double> entry : genreEarningsMap.entrySet()) {
                    double percentage = (entry.getValue() / totalEarnings) * 100;
                    writer.printf("%s,%.2f,%.2f%%%n", 
                        entry.getKey(), 
                        entry.getValue(), 
                        percentage);
                }
                
                JOptionPane.showMessageDialog(this, "Data exported successfully!", 
                    "Export Complete", JOptionPane.INFORMATION_MESSAGE);
                
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error exporting data: " + e.getMessage(), 
                    "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    // Data class to hold game information
    private static class GameData {
        private final String name;
        private final String genre;
        private final double earnings;
        
        public GameData(String name, String genre, double earnings) {
            this.name = name;
            this.genre = genre;
            this.earnings = earnings;
        }
        
        public String getName() { return name; }
        public String getGenre() { return genre; }
        public double getEarnings() { return earnings; }
    }
    
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(Midterm_MachineProblem2::new);
    }
}