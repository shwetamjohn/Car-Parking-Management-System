package com.parkingsystem.ui;

import com.parkingsystem.dao.ParkingDAO;
import com.parkingsystem.dao.ParkingDAO.OpenTicketDetail;
import com.parkingsystem.dao.ParkingDAO.RevenueRecord;
import com.parkingsystem.model.Slot;
import com.parkingsystem.model.Ticket;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Main Parking Management System UI
 * - Opens Slot Grid view directly on start
 * - Allows user to click on available slots to register vehicle entry
 * - Allows user to click on occupied/reserved slots or click 'Exit' to process full checkout
 * - Prompts all fields on exit (Ticket ID, Vehicle No, Slot, Amount, Payment Mode)
 * - Fixed + Running (per minute) fee model
 * - Robust report generation joining Parking_Tickets, Vehicles, Parking_slots, Payments
 */
public class ParkingUI extends JFrame {

    private final ParkingDAO dao = new ParkingDAO();

    // Top status / filter panel components
    private JLabel lblSummary;
    private JComboBox<String> cmbFilterType;

    // Slot Grid Wrapper
    private JPanel gridContainer;

    // Selected Slot Tracker
    private Slot selectedSlot = null;
    private JLabel lblSelectedSlotInfo;

    // Action Buttons
    private JButton btnProceedEntry;
    private JButton btnProceedExit;
    private JButton btnRefreshGrid;
    private JButton btnManageSlots;
    private JButton btnReports;

    public ParkingUI() {
        setTitle("Car Parking Management System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(980, 680);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(880, 580));

        initComponents();
        layoutComponents();
        loadSlotGrid();
    }

    private void initComponents() {
        lblSummary = new JLabel("Loading slots...", SwingConstants.LEFT);
        lblSummary.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblSummary.setForeground(new Color(40, 60, 90));

        cmbFilterType = new JComboBox<>(new String[]{"ALL VEHICLE TYPES", "CAR", "TWO_WHEELER", "HEAVY"});
        cmbFilterType.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cmbFilterType.addActionListener(e -> loadSlotGrid());

        lblSelectedSlotInfo = new JLabel("Click any slot below to select it for Entry or Exit", SwingConstants.LEFT);
        lblSelectedSlotInfo.setFont(new Font("Segoe UI", Font.ITALIC, 13));
        lblSelectedSlotInfo.setForeground(new Color(70, 80, 95));

        btnProceedEntry = new JButton("🚗  Vehicle Entry");
        btnProceedExit  = new JButton("🏁  Vehicle Exit & Pay");
        btnRefreshGrid  = new JButton("🔄  Refresh Slots");
        btnManageSlots  = new JButton("🔧  Manage Slots");
        btnReports      = new JButton("📋  Revenue Reports");

        styleButton(btnProceedEntry, new Color(225, 245, 230), new Color(20, 100, 45), new Color(46, 204, 113));
        styleButton(btnProceedExit,  new Color(253, 237, 236), new Color(140, 30, 25), new Color(231, 76, 60));
        styleButton(btnRefreshGrid,  new Color(235, 245, 255), new Color(20, 60, 110), new Color(52, 152, 219));
        styleButton(btnManageSlots,  new Color(245, 245, 245), new Color(50, 50, 60),  new Color(149, 165, 166));
        styleButton(btnReports,      new Color(254, 249, 231), new Color(125, 80, 15), new Color(241, 196, 15));

        btnProceedEntry.addActionListener(e -> openEntryDialog(selectedSlot));
        btnProceedExit.addActionListener(e -> openExitDialog(selectedSlot));
        btnRefreshGrid.addActionListener(e -> loadSlotGrid());
        btnManageSlots.addActionListener(e -> manageSlotsWindow());
        btnReports.addActionListener(e -> generateReportWindow());
    }

    private void styleButton(JButton btn, Color bg, Color fg, Color borderCol) {
        btn.setBackground(bg);
        btn.setForeground(fg);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setFocusPainted(false);
        btn.setOpaque(true);
        btn.setContentAreaFilled(true);
        btn.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(borderCol, 1, true),
            BorderFactory.createEmptyBorder(7, 14, 7, 14)
        ));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    private void layoutComponents() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(new EmptyBorder(12, 16, 12, 16));
        root.setBackground(new Color(242, 244, 247));

        // Header Panel
        JPanel headerPanel = new JPanel(new BorderLayout(8, 8));
        headerPanel.setBackground(new Color(242, 244, 247));

        JLabel title = new JLabel("🅿  Car Parking Management System", SwingConstants.LEFT);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(new Color(30, 45, 70));

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        filterPanel.setOpaque(false);
        filterPanel.add(new JLabel("Filter Slots:"));
        filterPanel.add(cmbFilterType);
        filterPanel.add(btnRefreshGrid);

        headerPanel.add(title, BorderLayout.WEST);
        headerPanel.add(filterPanel, BorderLayout.EAST);
        root.add(headerPanel, BorderLayout.NORTH);

        // Center: Slot Grid in ScrollPane
        gridContainer = new JPanel();
        gridContainer.setLayout(new BoxLayout(gridContainer, BoxLayout.Y_AXIS));
        gridContainer.setBackground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(gridContainer);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(210, 215, 225), 1));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        root.add(scrollPane, BorderLayout.CENTER);

        // South: Legend + Selected Slot Bar + Actions
        JPanel southPanel = new JPanel(new BorderLayout(10, 8));
        southPanel.setBackground(new Color(242, 244, 247));
        southPanel.setBorder(new EmptyBorder(8, 0, 0, 0));

        // Legend & Rate Formula Explanation
        JPanel infoPanel = new JPanel(new BorderLayout(6, 6));
        infoPanel.setOpaque(false);

        JPanel legend = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        legend.setOpaque(false);
        legend.add(createBadge("🟢 AVAILABLE (Click to Park)", new Color(39, 174, 96), Color.WHITE));
        legend.add(createBadge("🔴 RESERVED (Click to Exit/View)", new Color(231, 76, 60), Color.WHITE));
        legend.add(createBadge("🟡 BLOCKED", new Color(241, 196, 15), new Color(50, 50, 50)));

        JLabel feeNotice = new JLabel("💡 Rates: Car (₹20 + ₹1/min) | Two-Wheeler (₹10 + ₹0.5/min) | Heavy (₹40 + ₹2/min)");
        feeNotice.setFont(new Font("Segoe UI", Font.BOLD, 11));
        feeNotice.setForeground(new Color(80, 90, 110));

        infoPanel.add(legend, BorderLayout.WEST);
        infoPanel.add(feeNotice, BorderLayout.EAST);

        // Selection & Action Bar
        JPanel actionBar = new JPanel(new BorderLayout(10, 6));
        actionBar.setBackground(Color.WHITE);
        actionBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 210, 225), 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        actionBar.add(lblSelectedSlotInfo, BorderLayout.CENTER);

        JPanel btnGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnGroup.setOpaque(false);
        btnGroup.add(btnProceedEntry);
        btnGroup.add(btnProceedExit);
        btnGroup.add(btnManageSlots);
        btnGroup.add(btnReports);
        actionBar.add(btnGroup, BorderLayout.EAST);

        southPanel.add(infoPanel, BorderLayout.NORTH);
        southPanel.add(actionBar, BorderLayout.SOUTH);
        root.add(southPanel, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private JLabel createBadge(String text, Color bg, Color fg) {
        JLabel l = new JLabel(" " + text + " ");
        l.setOpaque(true);
        l.setBackground(bg);
        l.setForeground(fg);
        l.setFont(new Font("Segoe UI", Font.BOLD, 11));
        l.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        return l;
    }

    // -------------------------------------------------------
    //  Load & Render Slot Grid
    // -------------------------------------------------------
    public void loadSlotGrid() {
        gridContainer.removeAll();

        List<Slot> allSlots = dao.getRealTimeSlotStatuses();
        String filter = (String) cmbFilterType.getSelectedItem();
        boolean filterActive = filter != null && !filter.startsWith("ALL");

        int totalAvailable = 0;
        int totalReserved = 0;
        int totalBlocked = 0;

        // Group slots by floor or display in standard responsive grid
        JPanel grid = new JPanel(new GridLayout(0, 5, 10, 10));
        grid.setBackground(Color.WHITE);
        grid.setBorder(new EmptyBorder(16, 16, 16, 16));

        for (Slot s : allSlots) {
            String status = s.getStatus() != null ? s.getStatus().toLowerCase() : "available";
            if (status.equals("available")) totalAvailable++;
            else if (status.equals("reserved")) totalReserved++;
            else totalBlocked++;

            if (filterActive && !s.getSlotType().equalsIgnoreCase(filter)) {
                continue;
            }

            JPanel slotCard = createSlotCard(s);
            grid.add(slotCard);
        }

        gridContainer.add(grid);
        gridContainer.revalidate();
        gridContainer.repaint();

        lblSummary.setText(String.format("Total: %d | Available: %d | Reserved: %d | Blocked: %d",
            allSlots.size(), totalAvailable, totalReserved, totalBlocked));
    }

    private JPanel createSlotCard(Slot s) {
        JPanel card = new JPanel(new BorderLayout(4, 4));
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(180, 190, 205), 1, true),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        String status = s.getStatus() != null ? s.getStatus().toLowerCase() : "available";
        Color headerColor;
        String statusLabel;

        if (status.equals("available")) {
            headerColor = new Color(39, 174, 96);
            statusLabel = "AVAILABLE";
        } else if (status.equals("blocked")) {
            headerColor = new Color(241, 196, 15);
            statusLabel = "BLOCKED";
        } else {
            headerColor = new Color(231, 76, 60);
            statusLabel = "RESERVED";
        }

        JLabel lblNo = new JLabel("🅿 " + s.getSlotNumber(), SwingConstants.CENTER);
        lblNo.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblNo.setOpaque(true);
        lblNo.setBackground(headerColor);
        lblNo.setForeground(status.equals("blocked") ? new Color(50, 50, 50) : Color.WHITE);
        lblNo.setBorder(new EmptyBorder(4, 4, 4, 4));

        JPanel body = new JPanel(new GridLayout(3, 1, 2, 2));
        body.setOpaque(false);
        JLabel lFloor = new JLabel("Floor: " + s.getFloor(), SwingConstants.CENTER);
        JLabel lType  = new JLabel("Type: " + s.getSlotType(), SwingConstants.CENTER);
        JLabel lStat  = new JLabel(statusLabel, SwingConstants.CENTER);
        lFloor.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lType.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lStat.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lStat.setForeground(headerColor);

        body.add(lFloor);
        body.add(lType);
        body.add(lStat);

        card.add(lblNo, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);

        // Highlight card when selected
        if (selectedSlot != null && selectedSlot.getSlotId().equals(s.getSlotId())) {
            card.setBackground(new Color(235, 245, 255));
            card.setBorder(BorderFactory.createLineBorder(new Color(41, 128, 185), 2, true));
        } else {
            card.setBackground(new Color(250, 252, 255));
        }

        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                selectedSlot = s;
                lblSelectedSlotInfo.setText("👉 Selected Slot: " + s.getSlotNumber() +
                    " (" + s.getSlotType() + ", Floor " + s.getFloor() + ") — Status: " + status.toUpperCase());

                // Double click or direct action
                if (e.getClickCount() == 2) {
                    if (status.equals("available")) {
                        openEntryDialog(s);
                    } else if (status.equals("reserved")) {
                        openExitDialog(s);
                    }
                }
                loadSlotGrid();
            }
        });

        return card;
    }

    // -------------------------------------------------------
    //  Vehicle Entry Dialog (With explicit chosen Slot)
    // -------------------------------------------------------
    private void openEntryDialog(Slot initialSlot) {
        JDialog dialog = new JDialog(this, "🚗 Vehicle Entry Registration", true);
        dialog.setSize(440, 360);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 12, 8, 12);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField txtVehNo = new JTextField(15);
        JTextField txtContact = new JTextField(15);
        JComboBox<String> cmbVehType = new JComboBox<>(new String[]{"CAR", "TWO_WHEELER", "HEAVY"});

        // List of Available Slots
        List<Slot> availableSlots = dao.getRealTimeSlotStatuses();
        DefaultComboBoxModel<String> slotComboModel = new DefaultComboBoxModel<>();
        slotComboModel.addElement("Auto-Allocate First Available Slot");

        for (Slot sl : availableSlots) {
            if ("available".equalsIgnoreCase(sl.getStatus())) {
                slotComboModel.addElement(sl.getSlotNumber() + " (" + sl.getSlotType() + ", Floor " + sl.getFloor() + ") [ID:" + sl.getSlotId() + "]");
            }
        }
        JComboBox<String> cmbSlotSelection = new JComboBox<>(slotComboModel);

        if (initialSlot != null && "available".equalsIgnoreCase(initialSlot.getStatus())) {
            cmbVehType.setSelectedItem(initialSlot.getSlotType().toUpperCase());
            for (int i = 0; i < slotComboModel.getSize(); i++) {
                if (slotComboModel.getElementAt(i).contains("[ID:" + initialSlot.getSlotId() + "]")) {
                    cmbSlotSelection.setSelectedIndex(i);
                    break;
                }
            }
        }

        gbc.gridx = 0; gbc.gridy = 0; dialog.add(new JLabel("Vehicle Number: *"), gbc);
        gbc.gridx = 1;                dialog.add(txtVehNo, gbc);

        gbc.gridx = 0; gbc.gridy = 1; dialog.add(new JLabel("Vehicle Type:"), gbc);
        gbc.gridx = 1;                dialog.add(cmbVehType, gbc);

        gbc.gridx = 0; gbc.gridy = 2; dialog.add(new JLabel("Owner Contact:"), gbc);
        gbc.gridx = 1;                dialog.add(txtContact, gbc);

        gbc.gridx = 0; gbc.gridy = 3; dialog.add(new JLabel("Assign Slot:"), gbc);
        gbc.gridx = 1;                dialog.add(cmbSlotSelection, gbc);

        JButton btnSubmit = new JButton("🎟  Generate & Issue Ticket");
        styleButton(btnSubmit, new Color(225, 245, 230), new Color(20, 100, 45), new Color(46, 204, 113));

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        dialog.add(btnSubmit, gbc);

        btnSubmit.addActionListener(e -> {
            String vehNo = txtVehNo.getText().trim().toUpperCase();
            String contact = txtContact.getText().trim();
            String type = (String) cmbVehType.getSelectedItem();
            String slotChoice = (String) cmbSlotSelection.getSelectedItem();

            if (vehNo.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please enter the Vehicle Number.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String chosenSlotId = null;
            if (slotChoice != null && slotChoice.contains("[ID:")) {
                chosenSlotId = slotChoice.substring(slotChoice.indexOf("[ID:") + 4, slotChoice.indexOf("]")).trim();
            }

            Ticket tkt = dao.registerVehicleEntry(vehNo, type, contact, chosenSlotId);

            if (tkt != null) {
                // Show Ticket with Slot Number clearly
                String ticketSlip =
                    "═══════════════════════════════════════\n" +
                    "         🅿 PARKING TICKET\n" +
                    "═══════════════════════════════════════\n" +
                    "  Ticket ID    : " + tkt.getTicketId() + "\n" +
                    "  Slot Number  : " + tkt.getSlotNo() + "\n" +
                    "  Vehicle No   : " + tkt.getVehicleNum() + "\n" +
                    "  Vehicle Type : " + tkt.getVehicleType() + "\n" +
                    "  Owner Contact: " + (contact.isEmpty() ? "N/A" : contact) + "\n" +
                    "  Entry Time   : " + tkt.getEntryTime() + "\n" +
                    "═══════════════════════════════════════\n" +
                    "  Fee Model: Fixed Base + Running/Min\n" +
                    "  Please keep ticket safe until exit.\n" +
                    "═══════════════════════════════════════";

                JOptionPane.showMessageDialog(dialog, ticketSlip, "✅ Ticket Issued Successfully", JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
                loadSlotGrid();
            } else {
                JOptionPane.showMessageDialog(dialog, "❌ Could not allocate parking slot. Parking may be full or slot was taken.", "Entry Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.setVisible(true);
    }

    // -------------------------------------------------------
    //  Vehicle Exit & Payment Dialog (Asks user to verify/fill all fields)
    // -------------------------------------------------------
    private void openExitDialog(Slot initialSlot) {
        JDialog dialog = new JDialog(this, "🏁 Vehicle Exit & Payment Processing", true);
        dialog.setSize(520, 520);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(new EmptyBorder(14, 18, 14, 18));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField txtTicketSearch = new JTextField(12);
        JButton btnLookup = new JButton("🔍 Find Ticket");
        styleButton(btnLookup, new Color(235, 245, 255), new Color(20, 60, 110), new Color(52, 152, 219));

        JTextField txtTicketId = new JTextField(15);
        JTextField txtVehicleNo = new JTextField(15);
        JTextField txtVehicleType = new JTextField(15);
        JTextField txtSlotNo = new JTextField(15);
        JTextField txtEntryTime = new JTextField(15);
        JTextField txtDuration = new JTextField(15);
        JTextField txtAmountDue = new JTextField(15);
        JComboBox<String> cmbPaymentMode = new JComboBox<>(new String[]{"CASH", "CARD", "UPI"});

        txtTicketId.setEditable(false);
        txtVehicleNo.setEditable(false);
        txtVehicleType.setEditable(false);
        txtSlotNo.setEditable(false);
        txtEntryTime.setEditable(false);
        txtDuration.setEditable(false);
        txtAmountDue.setFont(new Font("Segoe UI", Font.BOLD, 14));
        txtAmountDue.setForeground(new Color(180, 30, 20));

        // Row 0: Search
        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        searchRow.add(txtTicketSearch);
        searchRow.add(btnLookup);
        gbc.gridx = 0; gbc.gridy = 0; form.add(new JLabel("Enter Ticket ID:"), gbc);
        gbc.gridx = 1;                form.add(searchRow, gbc);

        gbc.gridx = 0; gbc.gridy = 1; form.add(new JLabel("Ticket Number:"), gbc);
        gbc.gridx = 1;                form.add(txtTicketId, gbc);

        gbc.gridx = 0; gbc.gridy = 2; form.add(new JLabel("Vehicle Number:"), gbc);
        gbc.gridx = 1;                form.add(txtVehicleNo, gbc);

        gbc.gridx = 0; gbc.gridy = 3; form.add(new JLabel("Vehicle Type:"), gbc);
        gbc.gridx = 1;                form.add(txtVehicleType, gbc);

        gbc.gridx = 0; gbc.gridy = 4; form.add(new JLabel("Allocated Slot:"), gbc);
        gbc.gridx = 1;                form.add(txtSlotNo, gbc);

        gbc.gridx = 0; gbc.gridy = 5; form.add(new JLabel("Entry Timestamp:"), gbc);
        gbc.gridx = 1;                form.add(txtEntryTime, gbc);

        gbc.gridx = 0; gbc.gridy = 6; form.add(new JLabel("Minutes Parked:"), gbc);
        gbc.gridx = 1;                form.add(txtDuration, gbc);

        gbc.gridx = 0; gbc.gridy = 7; form.add(new JLabel("Total Fee (₹): *"), gbc);
        gbc.gridx = 1;                form.add(txtAmountDue, gbc);

        gbc.gridx = 0; gbc.gridy = 8; form.add(new JLabel("Payment Mode: *"), gbc);
        gbc.gridx = 1;                form.add(cmbPaymentMode, gbc);

        Runnable populateTicket = () -> {
            String q = txtTicketSearch.getText().trim();
            if (q.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please enter a Ticket ID to look up.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            OpenTicketDetail d = dao.getOpenTicketDetail(q);
            if (d != null) {
                txtTicketId.setText(d.ticketId);
                txtVehicleNo.setText(d.vehicleNo);
                txtVehicleType.setText(d.vehicleType);
                txtSlotNo.setText(d.slotNo);
                txtEntryTime.setText(d.entryTimeFormatted);
                txtDuration.setText(d.minutesElapsed + " min (Fixed ₹" + d.fixedFee + " + ₹" + d.runningRate + "/min)");
                txtAmountDue.setText(String.format("%.2f", d.totalFee));
            } else {
                JOptionPane.showMessageDialog(dialog, "No active open ticket found for ID: " + q, "Not Found", JOptionPane.ERROR_MESSAGE);
            }
        };

        btnLookup.addActionListener(e -> populateTicket.run());

        // If user clicked an occupied slot, automatically find its ticket
        if (initialSlot != null && "reserved".equalsIgnoreCase(initialSlot.getStatus())) {
            List<RevenueRecord> latest = dao.fetchRevenueReportData("1970-01-01", "2099-12-31");
            txtTicketSearch.setText(initialSlot.getSlotNumber());
            // Attempt query by slot ID
            String detail = dao.getOccupiedSlotDetail(initialSlot.getSlotId());
            if (detail != null && detail.contains("Ticket ID")) {
                String[] lines = detail.split("\n");
                for (String l : lines) {
                    if (l.startsWith("Ticket ID")) {
                        String tId = l.substring(l.indexOf(":") + 1).trim();
                        txtTicketSearch.setText(tId);
                        populateTicket.run();
                        break;
                    }
                }
            }
        }

        JButton btnProcessExit = new JButton("💳  Confirm Payment & Release Slot");
        styleButton(btnProcessExit, new Color(225, 245, 230), new Color(20, 100, 45), new Color(46, 204, 113));

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        bottomPanel.add(btnProcessExit);

        btnProcessExit.addActionListener(e -> {
            String tId = txtTicketId.getText().trim();
            String vNo = txtVehicleNo.getText().trim();
            String sNo = txtSlotNo.getText().trim();
            String amtStr = txtAmountDue.getText().trim();
            String mode = (String) cmbPaymentMode.getSelectedItem();

            if (tId.isEmpty() || vNo.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please look up and load an active ticket first.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }

            double amt;
            try {
                amt = Double.parseDouble(amtStr);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Invalid amount value.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            boolean ok = dao.processVehicleExit(tId, vNo, sNo, amt, mode);
            if (ok) {
                String receipt =
                    "═══════════════════════════════════════\n" +
                    "          RECEIPT & GATE PASS\n" +
                    "═══════════════════════════════════════\n" +
                    "  Ticket ID    : " + tId + "\n" +
                    "  Vehicle No   : " + vNo + "\n" +
                    "  Slot Freed   : " + sNo + "\n" +
                    "  Amount Paid  : \u20b9" + String.format("%.2f", amt) + "\n" +
                    "  Payment Mode : " + mode + "\n" +
                    "  Slot Status  : AVAILABLE\n" +
                    "═══════════════════════════════════════\n" +
                    "  Thank you! Gate opened for exit.\n" +
                    "═══════════════════════════════════════";

                JOptionPane.showMessageDialog(dialog, receipt, "✅ Exit & Payment Complete", JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
                loadSlotGrid();
            } else {
                JOptionPane.showMessageDialog(dialog, "❌ Failed to process exit. Check DB connection.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        dialog.add(form, BorderLayout.CENTER);
        dialog.add(bottomPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    // -------------------------------------------------------
    //  Manage Parking Slots
    // -------------------------------------------------------
    public void manageSlotsWindow() {
        JDialog dialog = new JDialog(this, "🔧 Manage Parking Slots", true);
        dialog.setSize(420, 280);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 12, 8, 12);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill   = GridBagConstraints.HORIZONTAL;

        JTextField txtSlotNum = new JTextField(12);
        JTextField txtFloor   = new JTextField("1", 12);
        JComboBox<String> cmbSlotType = new JComboBox<>(new String[]{"CAR", "TWO_WHEELER", "HEAVY"});

        gbc.gridx = 0; gbc.gridy = 0; dialog.add(new JLabel("Slot Number (e.g. A-105):"), gbc);
        gbc.gridx = 1;                dialog.add(txtSlotNum, gbc);

        gbc.gridx = 0; gbc.gridy = 1; dialog.add(new JLabel("Floor Number (1, 2, 3):"), gbc);
        gbc.gridx = 1;                dialog.add(txtFloor, gbc);

        gbc.gridx = 0; gbc.gridy = 2; dialog.add(new JLabel("Vehicle Type Allowed:"), gbc);
        gbc.gridx = 1;                dialog.add(cmbSlotType, gbc);

        JButton btnSave = new JButton("💾  Save Slot");
        styleButton(btnSave, new Color(225, 238, 248), new Color(20, 50, 90), new Color(52, 152, 219));

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        dialog.add(btnSave, gbc);

        btnSave.addActionListener(e -> {
            String slotNum  = txtSlotNum.getText().trim().toUpperCase();
            String floor    = txtFloor.getText().trim();
            String slotType = (String) cmbSlotType.getSelectedItem();

            if (slotNum.isEmpty() || floor.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Slot Number and Floor are required.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }

            boolean ok = dao.addOrUpdateSlot(slotNum, floor, slotType);
            JOptionPane.showMessageDialog(dialog,
                ok ? "✅ Slot saved: " + slotNum
                   : "❌ Failed to save slot. Check DB connection.",
                ok ? "Success" : "Error",
                ok ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE);
            if (ok) {
                dialog.dispose();
                loadSlotGrid();
            }
        });

        dialog.setVisible(true);
    }

    // -------------------------------------------------------
    //  Revenue Report Window (Fully fixed join and date handling)
    // -------------------------------------------------------
    public void generateReportWindow() {
        JDialog dialog = new JDialog(this, "📋 Parking Revenue & Transaction Reports", true);
        dialog.setSize(920, 520);
        dialog.setLocationRelativeTo(this);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        top.setBackground(new Color(236, 240, 245));

        LocalDate now = LocalDate.now();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        JTextField txtStart = new JTextField(now.minusDays(30).format(fmt), 10);
        JTextField txtEnd   = new JTextField(now.plusDays(1).format(fmt), 10);

        JButton btnFetch = new JButton("🔍 Fetch Report");
        styleButton(btnFetch, new Color(225, 238, 248), new Color(20, 50, 90), new Color(52, 152, 219));

        top.add(new JLabel("From (YYYY-MM-DD):")); top.add(txtStart);
        top.add(new JLabel("To (YYYY-MM-DD):"));   top.add(txtEnd);
        top.add(btnFetch);

        String[] cols = {"Ticket ID", "Vehicle No", "Type", "Slot",
                         "Entry Time", "Exit Time", "Duration", "Amount (₹)", "Payment Mode"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setRowHeight(24);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

        Runnable doFetch = () -> {
            model.setRowCount(0);
            String start = txtStart.getText().trim();
            String end = txtEnd.getText().trim();

            List<RevenueRecord> records = dao.fetchRevenueReportData(start, end);

            double totalRevenue  = 0;
            int    totalVehicles = records.size();

            for (RevenueRecord r : records) {
                totalRevenue += r.amount;
                model.addRow(new Object[]{
                    r.ticketId,
                    r.vehicleNo,
                    r.vehicleType,
                    r.slotNo,
                    r.entryTime,
                    r.exitTime,
                    r.durationMinutes + " mins",
                    String.format("%.2f", r.amount),
                    r.paymentMode
                });
            }

            // Summary row
            model.addRow(new Object[]{
                "SUMMARY", "", "", "",
                "", "",
                "Total: " + totalVehicles + " vehicles",
                "Total: ₹" + String.format("%.2f", totalRevenue),
                ""
            });
        };

        btnFetch.addActionListener(e -> doFetch.run());
        doFetch.run(); // Initial load

        JPanel content = new JPanel(new BorderLayout(5, 5));
        content.add(top, BorderLayout.NORTH);
        content.add(new JScrollPane(table), BorderLayout.CENTER);
        dialog.setContentPane(content);
        dialog.setVisible(true);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> new ParkingUI().setVisible(true));
    }
}
