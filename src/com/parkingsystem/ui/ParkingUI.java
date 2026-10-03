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
 * Built with Java Swing; delegates all DB work to {@link ParkingDAO}.
 *
 * Layout:
 *   ┌──────────────────────────────────────────┐
 *   │       🅿 Car Parking Management System   │  ← header
 *   ├──────────────────────────────────────────┤
 *   │  Vehicle No   [__________]               │
 *   │  Owner Contact[__________]               │  ← new (maps to VEHICLES.OWNER_CONTACT)
 *   │  Ticket ID    [__________]               │
 *   │  Type         [ComboBox ]                │
 *   ├──────────────────────────────────────────┤
 *   │ [Entry] [Exit] [Slots] [Manage] [Reports]│  ← button bar
 *   └──────────────────────────────────────────┘
 */
public class ParkingUI extends JFrame {

    // ---- UI Components (class-diagram attributes) ----
    private JTextField        txtVehicleNum;
    private JTextField        txtTicketId;
    private JComboBox<String> cmbType;
    private JButton           btnEntry;
    private JButton           btnExit;
    private JButton           btnSlotMonitor;
    private JButton           btnManageSlots;
    private JButton           btnReports;

    // ---- Extra field required by VEHICLES table ----
    private JTextField txtOwnerContact;

    // ---- DAO ----
    private final ParkingDAO dao = new ParkingDAO();

    // -------------------------------------------------------
    //  Constructor
    // -------------------------------------------------------
    public ParkingUI() {
        setTitle("Car Parking Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(540, 370);
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
        txtVehicleNum   = new JTextField(15);
        txtOwnerContact = new JTextField(15);
        txtTicketId     = new JTextField(15);

        cmbType = new JComboBox<>(new String[]{"CAR", "TWO_WHEELER", "HEAVY"});

        btnEntry       = new JButton("🚗  Entry");
        btnExit        = new JButton("🏁  Exit");
        btnSlotMonitor = new JButton("📊  Slot Monitor");
        btnManageSlots = new JButton("🔧  Manage Slots");
        btnReports     = new JButton("📋  Reports");

        Color btnColor = new Color(52, 152, 219);
        Font  btnFont  = new Font("Segoe UI", Font.BOLD, 12);
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
        header.setFont(new Font("Segoe UI", Font.BOLD, 17));
        header.setForeground(new Color(44, 62, 80));
        header.setBorder(new EmptyBorder(0, 0, 10, 0));
        root.add(header, BorderLayout.NORTH);

        // Form
        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(new Color(236, 240, 241));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.anchor = GridBagConstraints.WEST;
        Font labelFont = new Font("Segoe UI", Font.PLAIN, 13);

        // Row 0 — Vehicle Number
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel lVeh = new JLabel("Vehicle Number:"); lVeh.setFont(labelFont);
        form.add(lVeh, gbc);
        gbc.gridx = 1; form.add(txtVehicleNum, gbc);

        // Row 1 — Owner Contact
        gbc.gridx = 0; gbc.gridy = 1;
        JLabel lOwner = new JLabel("Owner Contact:"); lOwner.setFont(labelFont);
        form.add(lOwner, gbc);
        gbc.gridx = 1; form.add(txtOwnerContact, gbc);

        // Row 2 — Ticket ID
        gbc.gridx = 0; gbc.gridy = 2;
        JLabel lTkt = new JLabel("Ticket ID:"); lTkt.setFont(labelFont);
        form.add(lTkt, gbc);
        gbc.gridx = 1; form.add(txtTicketId, gbc);

        // Row 3 — Vehicle Type
        gbc.gridx = 0; gbc.gridy = 3;
        JLabel lType = new JLabel("Vehicle Type:"); lType.setFont(labelFont);
        form.add(lType, gbc);
        gbc.gridx = 1; form.add(cmbType, gbc);

        root.add(form, BorderLayout.CENTER);

        // Button bar
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        btnPanel.setBackground(new Color(236, 240, 241));
        for (JButton b : new JButton[]{btnEntry, btnExit, btnSlotMonitor, btnManageSlots, btnReports}) {
            btnPanel.add(b);
        }
        root.add(btnPanel, BorderLayout.SOUTH);

        setContentPane(root);
    }

    // -------------------------------------------------------
    //  Listeners
    // -------------------------------------------------------
    private void attachListeners() {
        btnEntry.addActionListener(e -> handleEntry());
        btnExit.addActionListener(e -> handleExit());
        btnSlotMonitor.addActionListener(e -> displaySlotGrid());
        btnManageSlots.addActionListener(e -> manageSlotsWindow());
        btnReports.addActionListener(e -> generateReportWindow());
    }

    // -------------------------------------------------------
    //  handleEntry
    // -------------------------------------------------------
    /**
     * Collects Vehicle Number, Owner Contact, and Vehicle Type from the form,
     * then calls {@link ParkingDAO#registerVehicleEntry} which upserts VEHICLES
     * and inserts into PARKING_TICKETS.
     */
    public void handleEntry() {
        String vehicleNum    = txtVehicleNum.getText().trim().toUpperCase();
        String ownerContact  = txtOwnerContact.getText().trim();
        String vehicleType   = (String) cmbType.getSelectedItem();

        if (vehicleNum.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please enter the Vehicle Number.", "Validation Error",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        boolean success = dao.registerVehicleEntry(vehicleNum, vehicleType, ownerContact);

        if (success) {
            JOptionPane.showMessageDialog(this,
                "✅ Entry Registered!\n\n" +
                "Vehicle  : " + vehicleNum   + "\n" +
                "Type     : " + vehicleType  + "\n" +
                "Contact  : " + (ownerContact.isEmpty() ? "N/A" : ownerContact),
                "Entry Successful", JOptionPane.INFORMATION_MESSAGE);
            txtVehicleNum.setText("");
            txtOwnerContact.setText("");
        } else {
            JOptionPane.showMessageDialog(this,
                "❌ No available slot for type: " + vehicleType +
                "\n\nParking may be full or a DB error occurred.",
                "Entry Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    // -------------------------------------------------------
    //  handleExit
    // -------------------------------------------------------
    /**
     * Validates the Ticket ID, prompts for payment mode, then calls
     * {@link ParkingDAO#processVehicleExit} which inserts into PAYMENTS,
     * stamps EXIT_TIME, and frees the slot.
     */
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

        // Ask for payment mode before processing
        String[] modes = {"CASH", "CARD", "UPI"};
        String paymentMode = (String) JOptionPane.showInputDialog(
            this, "Select Payment Mode:", "Payment",
            JOptionPane.PLAIN_MESSAGE, null, modes, modes[0]);
        if (paymentMode == null) return; // user cancelled

        double charge = dao.processVehicleExit(ticketId, paymentMode);

        if (charge > 0) {
            JOptionPane.showMessageDialog(this,
                "✅ Exit Processed!\n\n" +
                "Ticket ID      : " + ticketId    + "\n" +
                "Payment Mode   : " + paymentMode + "\n" +
                "Amount Charged : \u20b9" + String.format("%.2f", charge),
                "Exit Successful", JOptionPane.INFORMATION_MESSAGE);
            txtTicketId.setText("");
        } else {
            JOptionPane.showMessageDialog(this,
                "❌ Ticket ID not found or vehicle already exited.",
                "Exit Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    // -------------------------------------------------------
    //  displaySlotGrid
    // -------------------------------------------------------
    /**
     * Opens a dialog showing a colour-coded grid of all parking slots.
     * Clicking an OCCUPIED slot queries DB (via getOccupiedSlotDetail)
     * and shows vehicle number, owner contact, entry time, and estimated fee.
     *
     * 🟢 Green  = AVAILABLE
     * 🔴 Red    = OCCUPIED        (click to see vehicle details)
     * 🟡 Yellow = UNDER_MAINTENANCE
     */
    public void displaySlotGrid() {
        List<Slot> slots = dao.getRealTimeSlotStatuses();

        JDialog dialog = new JDialog(this, "Slot Monitor — Live Status", true);
        dialog.setSize(640, 460);
        dialog.setLocationRelativeTo(this);

        JPanel grid = new JPanel(new GridLayout(0, 6, 6, 6));
        grid.setBorder(new EmptyBorder(12, 12, 12, 12));
        grid.setBackground(Color.WHITE);

        for (Slot s : slots) {
            JLabel cell = new JLabel(s.getSlotNumber(), SwingConstants.CENTER);
            cell.setOpaque(true);
            cell.setFont(new Font("Segoe UI", Font.BOLD, 11));
            cell.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));
            cell.setToolTipText("Floor: " + s.getFloor() + " | Type: " + s.getSlotType()
                                + " | " + s.getStatus());

            switch (s.getStatus().toUpperCase()) {
                case "AVAILABLE":
                    cell.setBackground(new Color(39, 174, 96));
                    cell.setForeground(Color.WHITE);
                    break;
                case "UNDER_MAINTENANCE":
                    cell.setBackground(new Color(241, 196, 15));
                    cell.setForeground(new Color(44, 62, 80));
                    break;
                case "OCCUPIED":
                default:
                    cell.setBackground(new Color(231, 76, 60));
                    cell.setForeground(Color.WHITE);
                    // Click on OCCUPIED slot → show vehicle details
                    final int slotId = s.getSlotId();
                    final String slotNo = s.getSlotNumber();
                    cell.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    cell.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            String detail = dao.getOccupiedSlotDetail(slotId);
                            if (detail != null) {
                                JOptionPane.showMessageDialog(dialog,
                                    detail,
                                    "Slot " + slotNo + " — Occupancy Details",
                                    JOptionPane.INFORMATION_MESSAGE);
                            } else {
                                JOptionPane.showMessageDialog(dialog,
                                    "No active ticket found for this slot.",
                                    "No Data", JOptionPane.WARNING_MESSAGE);
                            }
                        }
                    });
                    break;
            }
            grid.add(cell);
        }

        // Legend
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        legend.setBackground(new Color(245, 245, 245));
        legend.add(legendLabel("  Available  ",     new Color(39, 174, 96),  Color.WHITE));
        legend.add(legendLabel("  Occupied (click) ",new Color(231, 76, 60), Color.WHITE));
        legend.add(legendLabel("  Maintenance  ",   new Color(241, 196, 15), new Color(44, 62, 80)));

        JPanel content = new JPanel(new BorderLayout(4, 4));
        content.add(new JScrollPane(grid), BorderLayout.CENTER);
        content.add(legend, BorderLayout.SOUTH);

        dialog.setContentPane(content);
        dialog.setVisible(true);
    }

    /** Helper: creates a small coloured legend label. */
    private JLabel legendLabel(String text, Color bg, Color fg) {
        JLabel l = new JLabel(text);
        l.setOpaque(true);
        l.setBackground(bg);
        l.setForeground(fg);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        return l;
    }

    // -------------------------------------------------------
    //  manageSlotsWindow
    // -------------------------------------------------------
    /**
     * Opens a dialog for adding or updating a parking slot.
     * Calls {@link ParkingDAO#addOrUpdateSlot} which uses Oracle MERGE.
     */
    public void manageSlotsWindow() {
        JDialog dialog = new JDialog(this, "Manage Parking Slots", true);
        dialog.setSize(380, 260);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill   = GridBagConstraints.HORIZONTAL;

        JTextField txtSlotNum = new JTextField(12);
        JTextField txtFloor   = new JTextField(12);
        JComboBox<String> cmbSlotType =
            new JComboBox<>(new String[]{"CAR", "TWO_WHEELER", "HEAVY"});

        gbc.gridx = 0; gbc.gridy = 0; dialog.add(new JLabel("Slot Number:"), gbc);
        gbc.gridx = 1;                dialog.add(txtSlotNum, gbc);

        gbc.gridx = 0; gbc.gridy = 1; dialog.add(new JLabel("Floor / Zone:"), gbc);
        gbc.gridx = 1;                dialog.add(txtFloor, gbc);

        gbc.gridx = 0; gbc.gridy = 2; dialog.add(new JLabel("Type Allowed:"), gbc);
        gbc.gridx = 1;                dialog.add(cmbSlotType, gbc);

        JButton btnSave = new JButton("💾  Save Slot");
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
    //  generateReportWindow
    // -------------------------------------------------------
    /**
     * Opens the Revenue Report dialog.
     * Fetches data via {@link ParkingDAO#generateRevenueReport} which
     * joins all 4 tables and includes PAYMENT_MODE from the PAYMENTS table.
     */
    public void generateReportWindow() {
        JDialog dialog = new JDialog(this, "Revenue Report", true);
        dialog.setSize(820, 460);
        dialog.setLocationRelativeTo(this);

        // Date-range inputs
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        top.setBackground(new Color(236, 240, 241));
        JTextField txtStart = new JTextField("01-OCT-2026", 12);
        JTextField txtEnd   = new JTextField("31-OCT-2026", 12);
        JButton    btnFetch = new JButton("🔍 Fetch Report");
        btnFetch.setBackground(new Color(52, 152, 219));
        btnFetch.setForeground(Color.WHITE);
        btnFetch.setFocusPainted(false);

        top.add(new JLabel("From:")); top.add(txtStart);
        top.add(new JLabel("To:"));   top.add(txtEnd);
        top.add(btnFetch);

        // Table — columns match the 4-table JOIN query
        String[] cols = {"Ticket ID", "Vehicle No", "Type", "Slot",
                         "Entry Time", "Exit Time", "Hours", "Amount (₹)", "Payment Mode"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setRowHeight(22);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        btnFetch.addActionListener(e -> {
            model.setRowCount(0);
            try (ResultSet rs = dao.generateRevenueReport(
                    txtStart.getText().trim(), txtEnd.getText().trim())) {

                double totalRevenue  = 0;
                int    totalVehicles = 0;

                while (rs.next()) {
                    double amount = rs.getDouble("AMOUNT");
                    totalRevenue += amount;
                    totalVehicles++;
                    model.addRow(new Object[]{
                        rs.getInt("TICKET_ID"),
                        rs.getString("VEHICLE_NO"),
                        rs.getString("VEHICLE_TYPE"),
                        rs.getString("SLOT_NO"),
                        rs.getString("ENTRY_TIME"),
                        rs.getString("EXIT_TIME"),
                        rs.getInt("HOURS_PARKED"),
                        String.format("%.2f", amount),
                        rs.getString("PAYMENT_MODE")
                    });
                }

                // Summary footer row
                model.addRow(new Object[]{
                    "", "", "", "", "",
                    "TOTAL (" + totalVehicles + " vehicles)",
                    "", String.format("%.2f", totalRevenue), ""
                });

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
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> new ParkingUI().setVisible(true));
    }
}
