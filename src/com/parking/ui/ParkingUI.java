package com.parking.ui;

import com.parking.db.DBConnection;
import com.parking.db.ParkingDAO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * ParkingUI - Student-Level Java Swing Car Parking Management System.
 * Clean, readable 2nd-year academic project interface with JTabbedPane,
 * FlowLayout/BorderLayout/GridBagLayout, and clear ActionListener handlers.
 */
public class ParkingUI extends JFrame {

    // Main Tabbed Interface
    private JTabbedPane tabbedPane;

    // --- Tab 1: Vehicle Entry Fields & Buttons ---
    private JTextField txtEntryVehicleNo;
    private JComboBox<String> cmbEntryVehicleType;
    private JTextField txtEntryOwnerContact;
    private JButton btnGenerateTicket;
    private JButton btnClearEntry;

    // --- Tab 2: Vehicle Exit & Payment Fields & Buttons ---
    private JTextField txtExitTicketId;
    private JButton btnCalculateFee;
    private JLabel lblFeeDetails;
    private JComboBox<String> cmbPaymentMode;
    private JButton btnProcessExit;
    private JButton btnClearExit;
    private double currentFeeAmount = 0.0;

    // --- Tab 3: Slot Monitoring Components ---
    private JTable tblSlots;
    private JButton btnRefreshSlots;

    // --- Tab 4: Slot Management Components ---
    private JTextField txtSlotNo;
    private JTextField txtSlotFloor;
    private JComboBox<String> cmbSlotAllowedType;
    private JComboBox<String> cmbSlotInitialStatus;
    private JButton btnAddSlot;
    private JTextField txtUpdateSlotId;
    private JComboBox<String> cmbUpdateStatus;
    private JButton btnUpdateStatus;
    private JTextField txtDeleteSlotId;
    private JButton btnDeleteSlot;

    // --- Tab 5: Reports / History Components ---
    private JTable tblTickets;
    private JTable tblPayments;
    private JButton btnRefreshReports;

    public ParkingUI() {
        // Frame Settings
        setTitle("Car Parking Management System");
        setSize(850, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Build UI
        initUI();
    }

    private void initUI() {
        JPanel mainContainer = new JPanel(new BorderLayout());

        // Header Panel
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(new Color(41, 128, 185));
        headerPanel.setBorder(new EmptyBorder(12, 12, 12, 12));
        JLabel lblTitle = new JLabel("CAR PARKING MANAGEMENT SYSTEM");
        lblTitle.setFont(new Font("Arial", Font.BOLD, 18));
        lblTitle.setForeground(Color.WHITE);
        headerPanel.add(lblTitle);
        mainContainer.add(headerPanel, BorderLayout.NORTH);

        // Tabbed Pane for Modules
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Arial", Font.PLAIN, 13));

        tabbedPane.addTab("Vehicle Entry", createEntryPanel());
        tabbedPane.addTab("Vehicle Exit & Payment", createExitPanel());
        tabbedPane.addTab("Slot Monitor", createSlotMonitorPanel());
        tabbedPane.addTab("Manage Slots", createManageSlotsPanel());
        tabbedPane.addTab("Records & Reports", createReportsPanel());

        mainContainer.add(tabbedPane, BorderLayout.CENTER);

        // Status Bar
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.setBorder(new EmptyBorder(4, 10, 4, 10));
        JLabel lblStatus = new JLabel("Database: Dockerized Oracle DB (localhost:1522/FREEPDB1) | Schema: PARKING");
        lblStatus.setFont(new Font("Arial", Font.ITALIC, 11));
        lblStatus.setForeground(Color.DARK_GRAY);
        statusPanel.add(lblStatus);
        mainContainer.add(statusPanel, BorderLayout.SOUTH);

        setContentPane(mainContainer);
    }

    // =========================================================================
    //  TAB 1: VEHICLE ENTRY
    // =========================================================================
    private JPanel createEntryPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(20, 30, 20, 30));

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;

        // Vehicle Number
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Vehicle Number (e.g. TN01AB1234):"), gbc);
        gbc.gridx = 1;
        txtEntryVehicleNo = new JTextField(18);
        formPanel.add(txtEntryVehicleNo, gbc);

        // Vehicle Type
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Vehicle Type:"), gbc);
        gbc.gridx = 1;
        cmbEntryVehicleType = new JComboBox<>(new String[]{"CAR", "TWO_WHEELER", "HEAVY"});
        formPanel.add(cmbEntryVehicleType, gbc);

        // Owner Contact
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Owner Contact (Phone):"), gbc);
        gbc.gridx = 1;
        txtEntryOwnerContact = new JTextField(18);
        formPanel.add(txtEntryOwnerContact, gbc);

        panel.add(formPanel, BorderLayout.CENTER);

        // Button Panel
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        btnGenerateTicket = new JButton("Generate Entry Ticket");
        btnGenerateTicket.setForeground(Color.BLACK);
        btnGenerateTicket.setFont(new Font("Arial", Font.BOLD, 12));

        btnClearEntry = new JButton("Clear Fields");
        btnClearEntry.setForeground(Color.BLACK);
        btnClearEntry.setFont(new Font("Arial", Font.PLAIN, 12));

        btnPanel.add(btnGenerateTicket);
        btnPanel.add(btnClearEntry);
        panel.add(btnPanel, BorderLayout.SOUTH);

        // Action Listeners
        btnGenerateTicket.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleVehicleEntry();
            }
        });

        btnClearEntry.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                txtEntryVehicleNo.setText("");
                txtEntryOwnerContact.setText("");
                cmbEntryVehicleType.setSelectedIndex(0);
            }
        });

        return panel;
    }

    private void handleVehicleEntry() {
        String vehicleNo = txtEntryVehicleNo.getText().trim();
        String vehicleType = (String) cmbEntryVehicleType.getSelectedItem();
        String ownerContact = txtEntryOwnerContact.getText().trim();

        if (vehicleNo.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a Vehicle Number!", "Input Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Call DAO atomic ticket generation
        String[] result = ParkingDAO.issueTicketWithAutoSlot(vehicleNo, vehicleType, ownerContact);

        if (result != null) {
            String ticketId = result[0];
            String slotId = result[1];
            String slotNo = result[2];

            String message = "========================================\n"
                           + "          PARKING TICKET ISSUED         \n"
                           + "========================================\n"
                           + "  Ticket ID    : " + ticketId + "\n"
                           + "  Vehicle No   : " + vehicleNo.toUpperCase() + "\n"
                           + "  Vehicle Type : " + vehicleType + "\n"
                           + "  Assigned Slot: " + slotNo + " (" + slotId + ")\n"
                           + "========================================\n"
                           + "Please keep this ticket safely for exit payment.";

            JOptionPane.showMessageDialog(this, message, "Ticket Generated Successfully", JOptionPane.INFORMATION_MESSAGE);

            // Clear inputs and populate exit tab ticket field for convenience
            txtEntryVehicleNo.setText("");
            txtEntryOwnerContact.setText("");
            txtExitTicketId.setText(ticketId);

            // Refresh slot monitor
            refreshSlotsTable();
        } else {
            JOptionPane.showMessageDialog(this,
                "Parking is FULL for vehicle type: " + vehicleType + " or an error occurred!",
                "Cannot Issue Ticket",
                JOptionPane.WARNING_MESSAGE
            );
        }
    }

    // =========================================================================
    //  TAB 2: VEHICLE EXIT & PAYMENT
    // =========================================================================
    private JPanel createExitPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(20, 30, 20, 30));

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;

        // Ticket ID Input
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Ticket ID (e.g. TCK_1):"), gbc);
        gbc.gridx = 1;
        txtExitTicketId = new JTextField(18);
        formPanel.add(txtExitTicketId, gbc);

        gbc.gridx = 2;
        btnCalculateFee = new JButton("Calculate Fee");
        btnCalculateFee.setForeground(Color.BLACK);
        formPanel.add(btnCalculateFee, gbc);

        // Fee Details Label
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Exit Summary:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 2;
        lblFeeDetails = new JLabel("<html><i>Enter Ticket ID and click Calculate Fee</i></html>");
        formPanel.add(lblFeeDetails, gbc);
        gbc.gridwidth = 1;

        // Payment Mode
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Payment Mode:"), gbc);
        gbc.gridx = 1;
        cmbPaymentMode = new JComboBox<>(new String[]{"CASH", "CARD", "UPI"});
        formPanel.add(cmbPaymentMode, gbc);

        panel.add(formPanel, BorderLayout.CENTER);

        // Button Panel
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        btnProcessExit = new JButton("Process Payment & Checkout");
        btnProcessExit.setForeground(Color.BLACK);
        btnProcessExit.setFont(new Font("Arial", Font.BOLD, 12));
        btnProcessExit.setEnabled(false);

        btnClearExit = new JButton("Clear");
        btnClearExit.setForeground(Color.BLACK);

        btnPanel.add(btnProcessExit);
        btnPanel.add(btnClearExit);
        panel.add(btnPanel, BorderLayout.SOUTH);

        // Action Listeners
        btnCalculateFee.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleCalculateFee();
            }
        });

        btnProcessExit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleProcessExit();
            }
        });

        btnClearExit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                txtExitTicketId.setText("");
                lblFeeDetails.setText("<html><i>Enter Ticket ID and click Calculate Fee</i></html>");
                btnProcessExit.setEnabled(false);
                currentFeeAmount = 0.0;
            }
        });

        return panel;
    }

    private void handleCalculateFee() {
        String ticketId = txtExitTicketId.getText().trim();
        if (ticketId.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a Ticket ID!", "Input Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Object[] details = ParkingDAO.calculateExitFee(ticketId);
        if (details != null) {
            String tId = (String) details[0];
            String vNo = (String) details[1];
            String vType = (String) details[2];
            String sId = (String) details[3];
            String entryTime = String.valueOf(details[4]);
            double hours = (Double) details[5];
            double amount = (Double) details[6];

            currentFeeAmount = amount;

            String htmlText = "<html>"
                    + "<b>Vehicle:</b> " + vNo + " (" + vType + ")<br>"
                    + "<b>Assigned Slot:</b> " + sId + "<br>"
                    + "<b>Entry Time:</b> " + entryTime + "<br>"
                    + "<b>Duration:</b> " + (int) hours + " hour(s)<br>"
                    + "<b style='color:red; font-size:13px;'>Total Payable: ₹" + String.format("%.2f", amount) + "</b>"
                    + "</html>";

            lblFeeDetails.setText(htmlText);
            btnProcessExit.setEnabled(true);
        } else {
            lblFeeDetails.setText("<html><span style='color:red;'>Ticket not found or already checked out!</span></html>");
            btnProcessExit.setEnabled(false);
            currentFeeAmount = 0.0;
            JOptionPane.showMessageDialog(this, "Active ticket not found for ID: " + ticketId, "Invalid Ticket", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleProcessExit() {
        String ticketId = txtExitTicketId.getText().trim();
        String paymentMode = (String) cmbPaymentMode.getSelectedItem();

        if (ticketId.isEmpty() || currentFeeAmount <= 0) {
            JOptionPane.showMessageDialog(this, "Please calculate the fee before processing payment.", "Warning", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String paymentId = ParkingDAO.checkoutTicketWithPayment(ticketId, currentFeeAmount, paymentMode);

        if (paymentId != null) {
            String receipt = "========================================\n"
                           + "          PAYMENT RECEIPT               \n"
                           + "========================================\n"
                           + "  Payment ID   : " + paymentId + "\n"
                           + "  Ticket ID    : " + ticketId + "\n"
                           + "  Amount Paid  : ₹" + String.format("%.2f", currentFeeAmount) + "\n"
                           + "  Payment Mode : " + paymentMode + "\n"
                           + "  Status       : SUCCESS (Slot Freed)\n"
                           + "========================================\n"
                           + "Thank you for parking with us!";

            JOptionPane.showMessageDialog(this, receipt, "Payment & Exit Successful", JOptionPane.INFORMATION_MESSAGE);

            // Reset tab
            txtExitTicketId.setText("");
            lblFeeDetails.setText("<html><i>Enter Ticket ID and click Calculate Fee</i></html>");
            btnProcessExit.setEnabled(false);
            currentFeeAmount = 0.0;

            // Refresh slots table
            refreshSlotsTable();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to complete checkout and payment.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =========================================================================
    //  TAB 3: SLOT MONITOR
    // =========================================================================
    private JPanel createSlotMonitorPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(15, 15, 15, 15));

        tblSlots = new JTable();
        JScrollPane scrollPane = new JScrollPane(tblSlots);
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnRefreshSlots = new JButton("Refresh Slots");
        btnRefreshSlots.setForeground(Color.BLACK);
        btnPanel.add(btnRefreshSlots);
        panel.add(btnPanel, BorderLayout.SOUTH);

        btnRefreshSlots.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshSlotsTable();
            }
        });

        return panel;
    }

    private void refreshSlotsTable() {
        String sql = "SELECT Slot_id, Slot_no, Floor, Status, Type_allowed FROM Parking_slots ORDER BY Floor ASC, Slot_no ASC";
        DefaultTableModel model = ParkingDAO.fetchTableData(sql);
        tblSlots.setModel(model);
    }

    // =========================================================================
    //  TAB 4: MANAGE SLOTS (ADD / UPDATE / DELETE)
    // =========================================================================
    private JPanel createManageSlotsPanel() {
        JPanel panel = new JPanel(new GridLayout(3, 1, 10, 10));
        panel.setBorder(new EmptyBorder(15, 20, 15, 20));

        // 1. Add Slot Panel
        JPanel addPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        addPanel.setBorder(BorderFactory.createTitledBorder("Add New Parking Slot"));
        addPanel.add(new JLabel("Slot No:"));
        txtSlotNo = new JTextField(6);
        addPanel.add(txtSlotNo);

        addPanel.add(new JLabel("Floor:"));
        txtSlotFloor = new JTextField(4);
        addPanel.add(txtSlotFloor);

        addPanel.add(new JLabel("Type:"));
        cmbSlotAllowedType = new JComboBox<>(new String[]{"CAR", "TWO_WHEELER", "HEAVY"});
        addPanel.add(cmbSlotAllowedType);

        addPanel.add(new JLabel("Status:"));
        cmbSlotInitialStatus = new JComboBox<>(new String[]{"available", "reserved", "blocked"});
        addPanel.add(cmbSlotInitialStatus);

        btnAddSlot = new JButton("Add Slot");
        btnAddSlot.setForeground(Color.BLACK);
        addPanel.add(btnAddSlot);

        // 2. Update Slot Status Panel
        JPanel updatePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        updatePanel.setBorder(BorderFactory.createTitledBorder("Update Slot Status"));
        updatePanel.add(new JLabel("Slot ID (e.g. SLOT_1):"));
        txtUpdateSlotId = new JTextField(8);
        updatePanel.add(txtUpdateSlotId);

        updatePanel.add(new JLabel("New Status:"));
        cmbUpdateStatus = new JComboBox<>(new String[]{"available", "reserved", "blocked"});
        updatePanel.add(cmbUpdateStatus);

        btnUpdateStatus = new JButton("Update Status");
        btnUpdateStatus.setForeground(Color.BLACK);
        updatePanel.add(btnUpdateStatus);

        // 3. Delete Slot Panel
        JPanel deletePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        deletePanel.setBorder(BorderFactory.createTitledBorder("Delete Slot"));
        deletePanel.add(new JLabel("Slot ID:"));
        txtDeleteSlotId = new JTextField(8);
        deletePanel.add(txtDeleteSlotId);

        btnDeleteSlot = new JButton("Delete Slot");
        btnDeleteSlot.setForeground(Color.BLACK);
        deletePanel.add(btnDeleteSlot);

        panel.add(addPanel);
        panel.add(updatePanel);
        panel.add(deletePanel);

        // Event Listeners
        btnAddSlot.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String slotNo = txtSlotNo.getText().trim();
                String floorStr = txtSlotFloor.getText().trim();
                String type = (String) cmbSlotAllowedType.getSelectedItem();
                String status = (String) cmbSlotInitialStatus.getSelectedItem();

                if (slotNo.isEmpty() || floorStr.isEmpty()) {
                    JOptionPane.showMessageDialog(ParkingUI.this, "Please provide Slot No and Floor!", "Warning", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                int floor;
                try {
                    floor = Integer.parseInt(floorStr);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(ParkingUI.this, "Floor must be a numeric value.", "Warning", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                if (ParkingDAO.addSlot(slotNo, floor, type, status)) {
                    JOptionPane.showMessageDialog(ParkingUI.this, "Slot added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    txtSlotNo.setText("");
                    txtSlotFloor.setText("");
                    refreshSlotsTable();
                }
            }
        });

        btnUpdateStatus.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String slotId = txtUpdateSlotId.getText().trim();
                String status = (String) cmbUpdateStatus.getSelectedItem();

                if (slotId.isEmpty()) {
                    JOptionPane.showMessageDialog(ParkingUI.this, "Please enter Slot ID to update!", "Warning", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                if (ParkingDAO.updateSlotStatus(slotId, status)) {
                    JOptionPane.showMessageDialog(ParkingUI.this, "Slot status updated!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    txtUpdateSlotId.setText("");
                    refreshSlotsTable();
                } else {
                    JOptionPane.showMessageDialog(ParkingUI.this, "Failed to update slot. Please check Slot ID.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        btnDeleteSlot.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String slotId = txtDeleteSlotId.getText().trim();
                if (slotId.isEmpty()) {
                    JOptionPane.showMessageDialog(ParkingUI.this, "Please enter Slot ID to delete!", "Warning", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                int confirm = JOptionPane.showConfirmDialog(
                    ParkingUI.this,
                    "Are you sure you want to delete slot: " + slotId + "?",
                    "Confirm Deletion",
                    JOptionPane.YES_NO_OPTION
                );

                if (confirm == JOptionPane.YES_OPTION) {
                    if (ParkingDAO.deleteSlot(slotId)) {
                        JOptionPane.showMessageDialog(ParkingUI.this, "Slot deleted successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                        txtDeleteSlotId.setText("");
                        refreshSlotsTable();
                    } else {
                        JOptionPane.showMessageDialog(ParkingUI.this, "Failed to delete slot. Ensure it is not referenced by active tickets.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        return panel;
    }

    // =========================================================================
    //  TAB 5: RECORDS & REPORTS
    // =========================================================================
    private JPanel createReportsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel tablesContainer = new JPanel(new GridLayout(2, 1, 10, 10));

        // Tickets Table Panel
        JPanel ticketsPanel = new JPanel(new BorderLayout());
        ticketsPanel.setBorder(BorderFactory.createTitledBorder("Active & Completed Parking Tickets"));
        tblTickets = new JTable();
        ticketsPanel.add(new JScrollPane(tblTickets), BorderLayout.CENTER);
        tablesContainer.add(ticketsPanel);

        // Payments Table Panel
        JPanel paymentsPanel = new JPanel(new BorderLayout());
        paymentsPanel.setBorder(BorderFactory.createTitledBorder("Payment Transactions"));
        tblPayments = new JTable();
        paymentsPanel.add(new JScrollPane(tblPayments), BorderLayout.CENTER);
        tablesContainer.add(paymentsPanel);

        panel.add(tablesContainer, BorderLayout.CENTER);

        // Refresh Button
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnRefreshReports = new JButton("Refresh Records");
        btnRefreshReports.setForeground(Color.BLACK);
        btnPanel.add(btnRefreshReports);
        panel.add(btnPanel, BorderLayout.SOUTH);

        btnRefreshReports.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshReportsTables();
            }
        });

        return panel;
    }

    private void refreshReportsTables() {
        String ticketsSql = "SELECT Ticket_ID, Entry_time, Exit_time, Vehicle_no, Slot_ID "
                          + "FROM Parking_Tickets ORDER BY Entry_time DESC";
        tblTickets.setModel(ParkingDAO.fetchTableData(ticketsSql));

        String paymentsSql = "SELECT Payment_ID, Amount, Payment_Time, Payment_mode, Ticket_id "
                           + "FROM Payments ORDER BY Payment_Time DESC";
        tblPayments.setModel(ParkingDAO.fetchTableData(paymentsSql));
    }

    // =========================================================================
    //  MAIN METHOD
    // =========================================================================
    public static void main(String[] args) {
        // Use standard CrossPlatform / System LookAndFeel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                ParkingUI ui = new ParkingUI();
                ui.setVisible(true);

                // Initial data load in background
                ui.refreshSlotsTable();
                ui.refreshReportsTables();
            }
        });
    }
}
