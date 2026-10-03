package com.parkingsystem.ui;

import com.parkingsystem.dao.ParkingDAO;
import com.parkingsystem.model.Slot;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Main UI class for the Car Parking Management System.
 * Built with Java Swing; uses ParkingDAO for all DB operations.
 *
 * Layout overview:
 *   ┌─────────────────────────────────────┐
 *   │          HEADER / LOGO              │
 *   ├─────────────────────────────────────┤
 *   │  Vehicle No  [________]             │
 *   │  Ticket ID   [________]             │
 *   │  Type        [ComboBox]             │
 *   │  [Entry] [Exit] [Slots] [Manage] [Reports] │
 *   └─────────────────────────────────────┘
 */
public class ParkingUI extends JFrame {

    // ---- UI Components (match class diagram attributes) ----
    private JTextField txtVehicleNum;
    private JTextField txtTicketId;
    private JComboBox<String> cmbType;
    private JButton btnEntry;
    private JButton btnExit;
    private JButton btnSlotMonitor;
    private JButton btnManageSlots;
    private JButton btnReports;

    // ---- DAO ----
    private final ParkingDAO dao = new ParkingDAO();

    // -------------------------------------------------------
    //  Constructor — builds the main window
    // -------------------------------------------------------
    public ParkingUI() {
        setTitle("Car Parking Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(520, 340);
        setLocationRelativeTo(null);
        setResizable(false);

        initComponents();
        layoutComponents();
        attachListeners();
    }

    // -------------------------------------------------------
    //  Component initialisation
    // -------------------------------------------------------
    private void initComponents() {
        txtVehicleNum = new JTextField(15);
        txtTicketId   = new JTextField(15);

        cmbType = new JComboBox<>(new String[]{"CAR", "TWO_WHEELER", "HEAVY"});

        btnEntry       = new JButton("🚗 Entry");
        btnExit        = new JButton("🏁 Exit");
        btnSlotMonitor = new JButton("📊 Slot Monitor");
        btnManageSlots = new JButton("🔧 Manage Slots");
        btnReports     = new JButton("📋 Reports");

        // Style buttons
        Color btnColor = new Color(52, 152, 219);
        Font  btnFont  = new Font("Segoe UI", Font.BOLD, 13);
        for (JButton b : new JButton[]{btnEntry, btnExit, btnSlotMonitor, btnManageSlots, btnReports}) {
            b.setBackground(btnColor);
            b.setForeground(Color.WHITE);
            b.setFont(btnFont);
            b.setFocusPainted(false);
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
    }

    // -------------------------------------------------------
    //  Layout
    // -------------------------------------------------------
    private void layoutComponents() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(15, 20, 15, 20));
        root.setBackground(new Color(236, 240, 241));

        // Header
        JLabel header = new JLabel("🅿  Car Parking Management System", SwingConstants.CENTER);
        header.setFont(new Font("Segoe UI", Font.BOLD, 18));
        header.setForeground(new Color(44, 62, 80));
        header.setBorder(new EmptyBorder(0, 0, 10, 0));
        root.add(header, BorderLayout.NORTH);

        // Form panel
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(new Color(236, 240, 241));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.anchor = GridBagConstraints.WEST;

        Font labelFont = new Font("Segoe UI", Font.PLAIN, 13);

        // Row 0 — Vehicle Number
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel lVeh = new JLabel("Vehicle Number:");
        lVeh.setFont(labelFont);
        form.add(lVeh, gbc);
        gbc.gridx = 1;
        form.add(txtVehicleNum, gbc);

        // Row 1 — Ticket ID
        gbc.gridx = 0; gbc.gridy = 1;
        JLabel lTkt = new JLabel("Ticket ID:");
        lTkt.setFont(labelFont);
        form.add(lTkt, gbc);
        gbc.gridx = 1;
        form.add(txtTicketId, gbc);

        // Row 2 — Vehicle Type
        gbc.gridx = 0; gbc.gridy = 2;
        JLabel lType = new JLabel("Vehicle Type:");
        lType.setFont(labelFont);
        form.add(lType, gbc);
        gbc.gridx = 1;
        form.add(cmbType, gbc);

        root.add(form, BorderLayout.CENTER);

        // Button bar
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        btnPanel.setBackground(new Color(236, 240, 241));
        btnPanel.add(btnEntry);
        btnPanel.add(btnExit);
        btnPanel.add(btnSlotMonitor);
        btnPanel.add(btnManageSlots);
        btnPanel.add(btnReports);
        root.add(btnPanel, BorderLayout.SOUTH);

        setContentPane(root);
    }

    // -------------------------------------------------------
    //  Event listeners
    // -------------------------------------------------------
    private void attachListeners() {
        btnEntry.addActionListener(e -> handleEntry());
        btnExit.addActionListener(e -> handleExit());
        btnSlotMonitor.addActionListener(e -> displaySlotGrid());
        btnManageSlots.addActionListener(e -> manageSlotsWindow());
        btnReports.addActionListener(e -> generateReportWindow());
    }

    // -------------------------------------------------------
    //  handleEntry — registers a vehicle entry
    // -------------------------------------------------------
    public void handleEntry() {
        String vehicleNum  = txtVehicleNum.getText().trim().toUpperCase();
        String vehicleType = (String) cmbType.getSelectedItem();

        if (vehicleNum.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please enter the Vehicle Number.", "Validation Error",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        boolean success = dao.registerVehicleEntry(vehicleNum, vehicleType);
        if (success) {
            JOptionPane.showMessageDialog(this,
                "✅ Entry registered!\n" +
                "Vehicle : " + vehicleNum + "\n" +
                "Type    : " + vehicleType,
                "Entry Successful", JOptionPane.INFORMATION_MESSAGE);
            txtVehicleNum.setText("");
        } else {
            JOptionPane.showMessageDialog(this,
                "❌ No available slot found for type: " + vehicleType +
                "\nOr a DB error occurred.",
                "Entry Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    // -------------------------------------------------------
    //  handleExit — processes a vehicle exit and shows charge
    // -------------------------------------------------------
    public void handleExit() {
        String ticketStr = txtTicketId.getText().trim();
        if (ticketStr.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please enter the Ticket ID.", "Validation Error",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        int ticketId;
        try {
            ticketId = Integer.parseInt(ticketStr);
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                "Ticket ID must be a numeric value.", "Validation Error",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Ask for payment mode
        String[] modes = {"CASH", "CARD", "UPI"};
        String paymentMode = (String) JOptionPane.showInputDialog(
            this, "Select Payment Mode:", "Payment",
            JOptionPane.PLAIN_MESSAGE, null, modes, modes[0]);

        if (paymentMode == null) return; // user cancelled

        double charge = dao.processVehicleExit(ticketId, paymentMode);
        if (charge > 0) {
            JOptionPane.showMessageDialog(this,
                "✅ Exit processed!\n" +
                "Ticket ID     : " + ticketId + "\n" +
                "Payment Mode  : " + paymentMode + "\n" +
                "Amount Charged: ₹" + String.format("%.2f", charge),
                "Exit Successful", JOptionPane.INFORMATION_MESSAGE);
            txtTicketId.setText("");
        } else {
            JOptionPane.showMessageDialog(this,
                "❌ Ticket ID not found or vehicle already exited.",
                "Exit Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    // -------------------------------------------------------
    //  displaySlotGrid — live slot monitor in a dialog
    // -------------------------------------------------------
    public void displaySlotGrid() {
        List<Slot> slots = dao.getRealTimeSlotStatuses();

        JDialog dialog = new JDialog(this, "Slot Monitor — Live Status", true);
        dialog.setSize(600, 420);
        dialog.setLocationRelativeTo(this);

        // Grid panel: each slot shown as a coloured label
        JPanel grid = new JPanel(new GridLayout(0, 6, 5, 5));
        grid.setBorder(new EmptyBorder(10, 10, 10, 10));
        grid.setBackground(Color.WHITE);

        for (Slot s : slots) {
            JLabel cell = new JLabel(s.getSlotNumber(), SwingConstants.CENTER);
            cell.setOpaque(true);
            cell.setFont(new Font("Segoe UI", Font.BOLD, 11));
            cell.setToolTipText(
                "Floor: " + s.getFloor() + " | Type: " + s.getSlotType());

            if ("AVAILABLE".equalsIgnoreCase(s.getStatus())) {
                cell.setBackground(new Color(39, 174, 96));   // green
                cell.setForeground(Color.WHITE);
            } else {
                cell.setBackground(new Color(231, 76, 60));   // red
                cell.setForeground(Color.WHITE);
            }
            grid.add(cell);
        }

        // Legend
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT));
        legend.setBackground(Color.WHITE);
        JLabel green = new JLabel("  Available  ");
        green.setOpaque(true); green.setBackground(new Color(39, 174, 96)); green.setForeground(Color.WHITE);
        JLabel red = new JLabel("  Occupied  ");
        red.setOpaque(true); red.setBackground(new Color(231, 76, 60)); red.setForeground(Color.WHITE);
        legend.add(green); legend.add(new JLabel("   ")); legend.add(red);

        JPanel content = new JPanel(new BorderLayout(5, 5));
        content.add(new JScrollPane(grid), BorderLayout.CENTER);
        content.add(legend, BorderLayout.SOUTH);

        dialog.setContentPane(content);
        dialog.setVisible(true);
    }

    // -------------------------------------------------------
    //  manageSlotsWindow — add / update parking slots
    // -------------------------------------------------------
    public void manageSlotsWindow() {
        JDialog dialog = new JDialog(this, "Manage Parking Slots", true);
        dialog.setSize(380, 260);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets  = new Insets(8, 10, 8, 10);
        gbc.anchor  = GridBagConstraints.WEST;
        gbc.fill    = GridBagConstraints.HORIZONTAL;

        JTextField txtSlotNum = new JTextField(12);
        JTextField txtFloor   = new JTextField(12);
        JComboBox<String> cmbSlotType =
            new JComboBox<>(new String[]{"CAR", "TWO_WHEELER", "HEAVY"});

        // Row 0
        gbc.gridx = 0; gbc.gridy = 0; dialog.add(new JLabel("Slot Number:"), gbc);
        gbc.gridx = 1; dialog.add(txtSlotNum, gbc);
        // Row 1
        gbc.gridx = 0; gbc.gridy = 1; dialog.add(new JLabel("Floor:"), gbc);
        gbc.gridx = 1; dialog.add(txtFloor, gbc);
        // Row 2
        gbc.gridx = 0; gbc.gridy = 2; dialog.add(new JLabel("Slot Type:"), gbc);
        gbc.gridx = 1; dialog.add(cmbSlotType, gbc);

        JButton btnSave = new JButton("💾 Save");
        btnSave.setBackground(new Color(52, 152, 219));
        btnSave.setForeground(Color.WHITE);
        btnSave.setFocusPainted(false);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        dialog.add(btnSave, gbc);

        btnSave.addActionListener(e -> {
            String slotNum  = txtSlotNum.getText().trim().toUpperCase();
            String floor    = txtFloor.getText().trim().toUpperCase();
            String slotType = (String) cmbSlotType.getSelectedItem();

            if (slotNum.isEmpty() || floor.isEmpty()) {
                JOptionPane.showMessageDialog(dialog,
                    "Slot Number and Floor are required.", "Validation",
                    JOptionPane.WARNING_MESSAGE);
                return;
            }

            boolean ok = dao.addOrUpdateSlot(slotNum, floor, slotType);
            JOptionPane.showMessageDialog(dialog,
                ok ? "✅ Slot saved: " + slotNum
                   : "❌ Failed to save slot. Check DB connection.",
                ok ? "Success" : "Error",
                ok ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);

            if (ok) dialog.dispose();
        });

        dialog.setVisible(true);
    }

    // -------------------------------------------------------
    //  generateReportWindow — revenue report in a JTable
    // -------------------------------------------------------
    public void generateReportWindow() {
        JDialog dialog = new JDialog(this, "Revenue Report", true);
        dialog.setSize(760, 460);
        dialog.setLocationRelativeTo(this);

        // Date inputs
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        top.setBackground(new Color(236, 240, 241));
        JTextField txtStart = new JTextField("01-OCT-2026", 12);
        JTextField txtEnd   = new JTextField("31-OCT-2026", 12);
        JButton    btnFetch = new JButton("🔍 Fetch");
        btnFetch.setBackground(new Color(52, 152, 219));
        btnFetch.setForeground(Color.WHITE);
        btnFetch.setFocusPainted(false);

        top.add(new JLabel("From:")); top.add(txtStart);
        top.add(new JLabel("To:"));   top.add(txtEnd);
        top.add(btnFetch);

        // Table
        String[] cols = {"Ticket ID", "Vehicle No", "Type", "Slot",
                         "Entry Time", "Exit Time", "Hours", "Charge (₹)"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        JTable table = new JTable(model);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setRowHeight(22);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));

        btnFetch.addActionListener(e -> {
            model.setRowCount(0); // clear previous rows
            try (ResultSet rs = dao.generateRevenueReport(
                    txtStart.getText().trim(), txtEnd.getText().trim())) {

                double total = 0;
                while (rs.next()) {
                    double charge = rs.getDouble("CHARGE");
                    total += charge;
                    model.addRow(new Object[]{
                        rs.getInt("TICKET_ID"),
                        rs.getString("VEHICLE_NUM"),
                        rs.getString("VEHICLE_TYPE"),
                        rs.getString("SLOT_NUMBER"),
                        rs.getString("ENTRY_TIME"),
                        rs.getString("EXIT_TIME"),
                        rs.getInt("HOURS_PARKED"),
                        String.format("%.2f", charge)
                    });
                }
                // Totals footer row
                model.addRow(new Object[]{"", "", "", "", "", "TOTAL", "", String.format("%.2f", total)});

            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(dialog,
                    "DB Error: " + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        });

        JPanel content = new JPanel(new BorderLayout(5, 5));
        content.add(top, BorderLayout.NORTH);
        content.add(new JScrollPane(table), BorderLayout.CENTER);
        dialog.setContentPane(content);
        dialog.setVisible(true);
    }

    // -------------------------------------------------------
    //  main — application entry point
    // -------------------------------------------------------
    public static void main(String[] args) {
        // Use system look-and-feel for a native appearance
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> new ParkingUI().setVisible(true));
    }
}
