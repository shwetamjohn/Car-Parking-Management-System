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
 *   │  Owner Contact[__________]               │
 *   │  Ticket ID    [__________]               │
 *   │  Type         [ComboBox ]                │
 *   ├──────────────────────────────────────────┤
 *   │ [Entry] [Exit] [Slots] [Manage] [Reports]│
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
     * Activity diagram flow:
     *  Enter Vehicle No & Type → Query available slots → Slot available?
     *    Yes → Assign slot → Mark OCCUPIED → Generate Ticket → Print Ticket
     *    No  → Show "Parking Full" message
     *
     * The generated Ticket ID is shown prominently so the attendant
     * can hand it to the driver — they will need it to exit.
     */
    public void handleEntry() {
        String vehicleNum   = txtVehicleNum.getText().trim().toUpperCase();
        String ownerContact = txtOwnerContact.getText().trim();
        String vehicleType  = (String) cmbType.getSelectedItem();

        if (vehicleNum.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Please enter the Vehicle Number.", "Validation Error",
                JOptionPane.WARNING_MESSAGE);
            return;
        }

        // registerVehicleEntry now returns the Ticket ID (not just boolean)
        int ticketId = dao.registerVehicleEntry(vehicleNum, vehicleType, ownerContact);

        if (ticketId > 0) {
            // Print Parking Ticket — show Ticket ID prominently (driver needs this to exit)
            JOptionPane.showMessageDialog(this,
                "════════════════════════════\n" +
                "      PARKING TICKET\n" +
                "════════════════════════════\n" +
                "  Ticket ID   : " + ticketId   + "\n" +
                "  Vehicle No  : " + vehicleNum  + "\n" +
                "  Type        : " + vehicleType + "\n" +
                "  Contact     : " + (ownerContact.isEmpty() ? "N/A" : ownerContact) + "\n" +
                "════════════════════════════\n" +
                "  Please keep this ticket.\n" +
                "  Show it at exit gate.\n" +
                "════════════════════════════",
                "✅ Entry Registered — Ticket #" + ticketId,
                JOptionPane.INFORMATION_MESSAGE);

            // Auto-fill the Ticket ID field so attendant can reference it quickly
            txtTicketId.setText(String.valueOf(ticketId));
            txtVehicleNum.setText("");
            txtOwnerContact.setText("");

        } else if (ticketId == -1) {
            // No available slot — show "Parking Full" as per activity diagram
            JOptionPane.showMessageDialog(this,
                "⚠️  Parking Full!\n\n" +
                "No available slot for vehicle type: " + vehicleType + "\n" +
                "Please ask the driver to wait or try a different type.",
                "Parking Full", JOptionPane.WARNING_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this,
                "❌ A database error occurred. Please check the connection.",
                "Entry Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    // -------------------------------------------------------
    //  handleExit
    // -------------------------------------------------------
    /**
     * Activity diagram flow:
     *  Enter Ticket ID → Fetch Entry Time from DB → Calculate duration & fee
     *  → Display fee on screen → Collect payment → Record in PAYMENTS table
     *  → Update EXIT_TIME → Update slot to AVAILABLE → Print Payment Receipt
     *
     * Fee is displayed BEFORE asking for payment mode (matches activity diagram).
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

        // Step 1: Calculate and DISPLAY fee BEFORE asking payment mode
        double estimatedFee = dao.getEstimatedFee(ticketId);
        if (estimatedFee < 0) {
            JOptionPane.showMessageDialog(this,
                "❌ Ticket ID #" + ticketId + " not found or vehicle has already exited.",
                "Exit Failed", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Step 2: Show fee on screen (as per activity diagram)
        JOptionPane.showMessageDialog(this,
            "Ticket ID  : " + ticketId + "\n" +
            "Amount Due : \u20b9" + String.format("%.2f", estimatedFee),
            "Parking Fee", JOptionPane.INFORMATION_MESSAGE);

        // Step 3: Collect payment mode
        String[] modes = {"CASH", "CARD", "UPI"};
        String paymentMode = (String) JOptionPane.showInputDialog(
            this, "Select Payment Mode:", "Collect Payment",
            JOptionPane.PLAIN_MESSAGE, null, modes, modes[0]);
        if (paymentMode == null) return; // attendant cancelled

        // Step 4: Process exit — inserts PAYMENTS, stamps EXIT_TIME, frees slot
        double charge = dao.processVehicleExit(ticketId, paymentMode);

        if (charge > 0) {
            // Print Payment Receipt
            JOptionPane.showMessageDialog(this,
                "════════════════════════════\n" +
                "      PAYMENT RECEIPT\n" +
                "════════════════════════════\n" +
                "  Ticket ID    : " + ticketId    + "\n" +
                "  Payment Mode : " + paymentMode + "\n" +
                "  Amount Paid  : \u20b9" + String.format("%.2f", charge) + "\n" +
                "════════════════════════════\n" +
                "  Thank you! Drive safely.\n" +
                "════════════════════════════",
                "✅ Exit Processed — Receipt",
                JOptionPane.INFORMATION_MESSAGE);
            txtTicketId.setText("");
        } else {
            JOptionPane.showMessageDialog(this,
                "❌ Could not process exit. Please try again.",
                "Exit Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    // -------------------------------------------------------
    //  displaySlotGrid
    // -------------------------------------------------------
    /**
     * Live colour-coded grid of all parking slots.
     *  🟢 AVAILABLE         — green
     *  🔴 OCCUPIED          — red  (click to see vehicle details + elapsed time)
     *  🟡 UNDER_MAINTENANCE — yellow
     *
     * A Refresh button re-queries the DB without closing the dialog.
     */
    public void displaySlotGrid() {
        JDialog dialog = new JDialog(this, "Slot Monitor — Live Status", true);
        dialog.setSize(660, 500);
        dialog.setLocationRelativeTo(this);

        // Grid panel is rebuilt on each refresh
        JPanel wrapper = new JPanel(new BorderLayout());

        // Legend (static — stays at bottom)
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        legend.setBackground(new Color(245, 245, 245));
        legend.add(legendLabel("  Available  ",      new Color(39, 174, 96),  Color.WHITE));
        legend.add(legendLabel("  Occupied (click) ", new Color(231, 76, 60), Color.WHITE));
        legend.add(legendLabel("  Maintenance  ",    new Color(241, 196, 15), new Color(44, 62, 80)));

        // Refresh button
        JButton btnRefresh = new JButton("🔄 Refresh");
        btnRefresh.setBackground(new Color(46, 204, 113));
        btnRefresh.setForeground(Color.WHITE);
        btnRefresh.setFocusPainted(false);

        JPanel south = new JPanel(new BorderLayout());
        south.add(legend, BorderLayout.WEST);
        south.add(btnRefresh, BorderLayout.EAST);

        wrapper.add(south, BorderLayout.SOUTH);

        // Rebuild grid into wrapper
        Runnable buildGrid = () -> {
            if (wrapper.getComponentCount() > 1) {
                wrapper.remove(0); // remove old grid
            }
            List<Slot> slots = dao.getRealTimeSlotStatuses();

            JPanel grid = new JPanel(new GridLayout(0, 6, 6, 6));
            grid.setBorder(new EmptyBorder(12, 12, 12, 12));
            grid.setBackground(Color.WHITE);

            for (Slot s : slots) {
                JLabel cell = new JLabel(s.getSlotNumber(), SwingConstants.CENTER);
                cell.setOpaque(true);
                cell.setFont(new Font("Segoe UI", Font.BOLD, 11));
                cell.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));
                cell.setToolTipText("Floor: " + s.getFloor()
                    + " | Type: " + s.getSlotType()
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
                        final int    slotId = s.getSlotId();
                        final String slotNo = s.getSlotNumber();
                        cell.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                        cell.addMouseListener(new MouseAdapter() {
                            @Override
                            public void mouseClicked(MouseEvent e) {
                                String detail = dao.getOccupiedSlotDetail(slotId);
                                JOptionPane.showMessageDialog(dialog,
                                    detail != null ? detail
                                                   : "No active ticket for this slot.",
                                    "Slot " + slotNo + " — Occupancy Details",
                                    detail != null ? JOptionPane.INFORMATION_MESSAGE
                                                   : JOptionPane.WARNING_MESSAGE);
                            }
                        });
                        break;
                }
                grid.add(cell);
            }
            wrapper.add(new JScrollPane(grid), BorderLayout.CENTER);
            wrapper.revalidate();
            wrapper.repaint();
        };

        buildGrid.run(); // initial load
        btnRefresh.addActionListener(e -> buildGrid.run());

        dialog.setContentPane(wrapper);
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
     * Revenue Report — joins all 4 tables.
     * UC-05: date range selection, aggregate totals, vehicle count.
     */
    public void generateReportWindow() {
        JDialog dialog = new JDialog(this, "Revenue Report", true);
        dialog.setSize(850, 480);
        dialog.setLocationRelativeTo(this);

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

        String[] cols = {"Ticket ID", "Vehicle No", "Type", "Slot",
                         "Entry Time", "Exit Time", "Hours", "Amount (\u20b9)", "Payment Mode"};
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

                // Summary row — total revenue + vehicle count (UC-05)
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
