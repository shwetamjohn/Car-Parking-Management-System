package com.parking.db;

import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

/**
 * ParkingDAO - Data Access Object for Car Parking Management System.
 * Uses PreparedStatements, Oracle sequences, and atomic transaction control.
 */
public class ParkingDAO {

    // =========================================================================
    //  1. PARKING SLOTS MANAGEMENT
    // =========================================================================

    /**
     * Adds a new parking slot using sequence SEQ_SLOT_ID.
     *
     * @param slotNo      Slot label (e.g. "A-101")
     * @param floor       Floor number
     * @param typeAllowed Allowed vehicle type (CAR, TWO_WHEELER, HEAVY)
     * @param status      Slot status (available, reserved, blocked)
     * @return true if added successfully, false otherwise
     */
    public static boolean addSlot(String slotNo, int floor, String typeAllowed, String status) {
        String sql = "INSERT INTO Parking_slots (Slot_id, Slot_no, Floor, Status, Type_allowed) "
                   + "VALUES ('SLOT_' || SEQ_SLOT_ID.NEXTVAL, ?, ?, ?, ?)";

        String slotStatus = (status != null && !status.trim().isEmpty())
                ? status.trim().toLowerCase()
                : "available";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, slotNo.trim().toUpperCase());
            ps.setInt(2, floor);
            ps.setString(3, slotStatus);
            ps.setString(4, typeAllowed != null ? typeAllowed.trim().toUpperCase() : "CAR");

            int rows = ps.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            handleException("Error adding parking slot: " + slotNo, e);
            return false;
        }
    }

    /**
     * Updates the status of an existing slot ('available', 'reserved', 'blocked').
     *
     * @param slotId Slot Primary Key (e.g., "SLOT_1")
     * @param status New status
     * @return true if updated successfully
     */
    public static boolean updateSlotStatus(String slotId, String status) {
        String sql = "UPDATE Parking_slots SET Status = ? WHERE Slot_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, status.trim().toLowerCase());
            ps.setString(2, slotId.trim());

            int rows = ps.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            handleException("Error updating slot status: " + slotId, e);
            return false;
        }
    }

    /**
     * Deletes a parking slot by Slot_id.
     *
     * @param slotId Slot Primary Key
     * @return true if deleted
     */
    public static boolean deleteSlot(String slotId) {
        String sql = "DELETE FROM Parking_slots WHERE Slot_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, slotId.trim());
            int rows = ps.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            handleException("Error deleting slot: " + slotId, e);
            return false;
        }
    }

    /**
     * Gets a list of available slot IDs suitable for a given vehicle type.
     *
     * @param vehicleType Vehicle type
     * @return List of slot ID strings (e.g. "SLOT_1")
     */
    public static List<String> getAvailableSlotIds(String vehicleType) {
        List<String> slotList = new ArrayList<>();
        String sql = "SELECT Slot_id, Slot_no, Floor FROM Parking_slots "
                   + "WHERE Status = 'available' AND (Type_allowed = ? OR Type_allowed IS NULL) "
                   + "ORDER BY Floor ASC, Slot_no ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, vehicleType.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    slotList.add(rs.getString("Slot_id"));
                }
            }
        } catch (SQLException e) {
            handleException("Error fetching available slots for type: " + vehicleType, e);
        }
        return slotList;
    }

    // =========================================================================
    //  2. VEHICLE MANAGEMENT
    // =========================================================================

    /**
     * Adds a vehicle or updates contact/type if it exists.
     *
     * @param vehicleNo    Vehicle registration number (String)
     * @param type         Vehicle type (CAR, TWO_WHEELER, HEAVY)
     * @param ownerContact Contact details
     * @return true if successful
     */
    public static boolean addVehicle(String vehicleNo, String type, String ownerContact) {
        String sql = "MERGE INTO Vehicles tgt "
                   + "USING (SELECT ? AS Vehicle_no FROM DUAL) src "
                   + "ON (tgt.Vehicle_no = src.Vehicle_no) "
                   + "WHEN MATCHED THEN "
                   + "  UPDATE SET tgt.Type = ?, tgt.Owner_contact = ? "
                   + "WHEN NOT MATCHED THEN "
                   + "  INSERT (Vehicle_no, Type, Owner_contact) VALUES (?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            String vNo = vehicleNo.trim().toUpperCase();
            String vType = type.trim().toUpperCase();
            String vContact = (ownerContact != null) ? ownerContact.trim() : "";

            ps.setString(1, vNo);
            ps.setString(2, vType);
            ps.setString(3, vContact);
            ps.setString(4, vNo);
            ps.setString(5, vType);
            ps.setString(6, vContact);

            int rows = ps.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            handleException("Error saving vehicle details: " + vehicleNo, e);
            return false;
        }
    }

    // =========================================================================
    //  3. TICKET OPERATIONS (ATOMIC TRANSACTIONS)
    // =========================================================================

    /**
     * Issues a parking ticket atomically:
     * 1. Inserts or updates vehicle record.
     * 2. Finds the first available slot for vehicle type.
     * 3. Inserts ticket with 'TCK_' || SEQ_TICKET_ID.NEXTVAL.
     * 4. Updates slot status to 'blocked'.
     * 5. Commits transaction and returns ticket details [Ticket_ID, Slot_id, Slot_no].
     *
     * @param vehicleNo    Vehicle alphanumeric plate
     * @param vehicleType  Vehicle type
     * @param ownerContact Owner contact
     * @return String array {Ticket_ID, Slot_id, Slot_no} or null if failed / full
     */
    public static String[] issueTicketWithAutoSlot(String vehicleNo, String vehicleType, String ownerContact) {
        String findSlotSql = "SELECT Slot_id, Slot_no FROM Parking_slots "
                           + "WHERE Status = 'available' AND (Type_allowed = ? OR Type_allowed IS NULL) "
                           + "ORDER BY Floor ASC, Slot_no ASC "
                           + "FETCH FIRST 1 ROWS ONLY";

        String insertTicketSql = "INSERT INTO Parking_Tickets (Ticket_ID, Entry_time, Exit_time, Vehicle_no, Slot_ID) "
                               + "VALUES ('TCK_' || SEQ_TICKET_ID.NEXTVAL, CURRENT_TIMESTAMP, NULL, ?, ?)";

        String getCurrValSql = "SELECT 'TCK_' || SEQ_TICKET_ID.CURRVAL AS GENERATED_ID FROM DUAL";

        String updateSlotSql = "UPDATE Parking_slots SET Status = 'blocked' WHERE Slot_id = ?";

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Begin transaction

            // 1. Ensure Vehicle is registered
            addVehicle(vehicleNo, vehicleType, ownerContact);

            // 2. Find available slot
            String slotId = null;
            String slotNo = null;
            try (PreparedStatement psFind = conn.prepareStatement(findSlotSql)) {
                psFind.setString(1, vehicleType.trim().toUpperCase());
                try (ResultSet rs = psFind.executeQuery()) {
                    if (rs.next()) {
                        slotId = rs.getString("Slot_id");
                        slotNo = rs.getString("Slot_no");
                    }
                }
            }

            if (slotId == null) {
                conn.rollback();
                return null; // Parking is full for this type
            }

            // 3. Insert Ticket
            try (PreparedStatement psTicket = conn.prepareStatement(insertTicketSql)) {
                psTicket.setString(1, vehicleNo.trim().toUpperCase());
                psTicket.setString(2, slotId);
                psTicket.executeUpdate();
            }

            // 4. Retrieve generated Ticket ID
            String ticketId = null;
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(getCurrValSql)) {
                if (rs.next()) {
                    ticketId = rs.getString("GENERATED_ID");
                }
            }

            // 5. Block the slot
            try (PreparedStatement psSlot = conn.prepareStatement(updateSlotSql)) {
                psSlot.setString(1, slotId);
                psSlot.executeUpdate();
            }

            conn.commit(); // Transaction success!
            return new String[] { ticketId, slotId, slotNo };

        } catch (SQLException e) {
            rollbackQuietly(conn);
            handleException("Transaction failed during ticket generation for vehicle: " + vehicleNo, e);
            return null;
        } finally {
            closeQuietly(conn);
        }
    }

    /**
     * Issues a ticket given a specific selected Slot ID.
     *
     * @param vehicleNo Vehicle alphanumeric registration
     * @param slotId    Specific slot ID (e.g., 'SLOT_1')
     * @return Generated Ticket_ID (e.g. 'TCK_1') or null
     */
    public static String issueTicket(String vehicleNo, String slotId) {
        String insertTicketSql = "INSERT INTO Parking_Tickets (Ticket_ID, Entry_time, Exit_time, Vehicle_no, Slot_ID) "
                               + "VALUES ('TCK_' || SEQ_TICKET_ID.NEXTVAL, CURRENT_TIMESTAMP, NULL, ?, ?)";

        String getCurrValSql = "SELECT 'TCK_' || SEQ_TICKET_ID.CURRVAL AS GENERATED_ID FROM DUAL";
        String updateSlotSql = "UPDATE Parking_slots SET Status = 'blocked' WHERE Slot_id = ?";

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Begin transaction

            // 1. Insert ticket
            try (PreparedStatement psTicket = conn.prepareStatement(insertTicketSql)) {
                psTicket.setString(1, vehicleNo.trim().toUpperCase());
                psTicket.setString(2, slotId.trim());
                psTicket.executeUpdate();
            }

            // 2. Fetch Ticket ID
            String ticketId = null;
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(getCurrValSql)) {
                if (rs.next()) {
                    ticketId = rs.getString("GENERATED_ID");
                }
            }

            // 3. Mark slot as blocked
            try (PreparedStatement psSlot = conn.prepareStatement(updateSlotSql)) {
                psSlot.setString(1, slotId.trim());
                psSlot.executeUpdate();
            }

            conn.commit();
            return ticketId;

        } catch (SQLException e) {
            rollbackQuietly(conn);
            handleException("Transaction failed while issuing ticket for slot: " + slotId, e);
            return null;
        } finally {
            closeQuietly(conn);
        }
    }

    /**
     * Calculates parking fee and fetches ticket info prior to checkout.
     * Rate structure:
     * - TWO_WHEELER: ₹10 / hr (minimum ₹10)
     * - CAR: ₹20 / hr (minimum ₹20)
     * - HEAVY: ₹40 / hr (minimum ₹40)
     *
     * @param ticketId Ticket ID (e.g. "TCK_1")
     * @return Object array {Ticket_ID, Vehicle_no, Vehicle_Type, Slot_id, Entry_Time, Hours_Spent, Total_Amount} or null
     */
    public static Object[] calculateExitFee(String ticketId) {
        String sql = "SELECT t.Ticket_ID, t.Vehicle_no, v.Type, t.Slot_ID, t.Entry_time, "
                   + "ROUND((CAST(CURRENT_TIMESTAMP AS DATE) - CAST(t.Entry_time AS DATE)) * 24, 2) AS HOURS_SPENT "
                   + "FROM Parking_Tickets t "
                   + "JOIN Vehicles v ON t.Vehicle_no = v.Vehicle_no "
                   + "WHERE t.Ticket_ID = ? AND t.Exit_time IS NULL";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, ticketId.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String tId = rs.getString("Ticket_ID");
                    String vNo = rs.getString("Vehicle_no");
                    String vType = rs.getString("Type");
                    String sId = rs.getString("Slot_ID");
                    String entryTime = rs.getString("Entry_time");
                    double hours = rs.getDouble("HOURS_SPENT");
                    if (hours < 1.0) {
                        hours = 1.0; // minimum 1 hour fee
                    }

                    double ratePerHour = 20.0; // default CAR
                    if ("TWO_WHEELER".equalsIgnoreCase(vType)) {
                        ratePerHour = 10.0;
                    } else if ("HEAVY".equalsIgnoreCase(vType)) {
                        ratePerHour = 40.0;
                    }

                    double totalAmount = Math.ceil(hours) * ratePerHour;
                    return new Object[] { tId, vNo, vType, sId, entryTime, Math.ceil(hours), totalAmount };
                }
            }
        } catch (SQLException e) {
            handleException("Error calculating exit fee for ticket: " + ticketId, e);
        }
        return null;
    }

    /**
     * Checks out a ticket and records payment atomically:
     * 1. Inserts payment record into Payments using SEQ_PAYMENT_ID.
     * 2. Updates Exit_time on Parking_Tickets to CURRENT_TIMESTAMP.
     * 3. Sets associated slot status back to 'available'.
     * 4. Commits transaction and returns Payment_ID.
     *
     * @param ticketId    Ticket ID
     * @param amount      Amount paid
     * @param paymentMode Payment mode (CASH, CARD, UPI)
     * @return Generated Payment_ID or null if failed
     */
    public static String checkoutTicketWithPayment(String ticketId, double amount, String paymentMode) {
        String findSlotSql = "SELECT Slot_ID FROM Parking_Tickets WHERE Ticket_ID = ? AND Exit_time IS NULL";
        String insertPaymentSql = "INSERT INTO Payments (Payment_ID, Amount, Payment_Time, Payment_mode, Ticket_id) "
                                + "VALUES ('PAY_' || SEQ_PAYMENT_ID.NEXTVAL, ?, CURRENT_TIMESTAMP, ?, ?)";
        String getCurrValSql = "SELECT 'PAY_' || SEQ_PAYMENT_ID.CURRVAL AS GENERATED_ID FROM DUAL";
        String updateTicketSql = "UPDATE Parking_Tickets SET Exit_time = CURRENT_TIMESTAMP WHERE Ticket_ID = ?";
        String freeSlotSql = "UPDATE Parking_slots SET Status = 'available' WHERE Slot_id = ?";

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Begin transaction

            // 1. Verify active ticket and get slot
            String slotId = null;
            try (PreparedStatement psFind = conn.prepareStatement(findSlotSql)) {
                psFind.setString(1, ticketId.trim());
                try (ResultSet rs = psFind.executeQuery()) {
                    if (rs.next()) {
                        slotId = rs.getString("Slot_ID");
                    } else {
                        throw new SQLException("Active ticket not found or already checked out: " + ticketId);
                    }
                }
            }

            // 2. Insert Payment
            try (PreparedStatement psPay = conn.prepareStatement(insertPaymentSql)) {
                psPay.setDouble(1, amount);
                psPay.setString(2, paymentMode != null ? paymentMode.trim().toUpperCase() : "CASH");
                psPay.setString(3, ticketId.trim());
                psPay.executeUpdate();
            }

            // 3. Get generated Payment ID
            String paymentId = null;
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(getCurrValSql)) {
                if (rs.next()) {
                    paymentId = rs.getString("GENERATED_ID");
                }
            }

            // 4. Update Exit Timestamp
            try (PreparedStatement psUpdateTicket = conn.prepareStatement(updateTicketSql)) {
                psUpdateTicket.setString(1, ticketId.trim());
                psUpdateTicket.executeUpdate();
            }

            // 5. Free Slot
            try (PreparedStatement psFreeSlot = conn.prepareStatement(freeSlotSql)) {
                psFreeSlot.setString(1, slotId);
                psFreeSlot.executeUpdate();
            }

            conn.commit();
            return paymentId;

        } catch (SQLException e) {
            rollbackQuietly(conn);
            handleException("Transaction failed during checkout and payment for ticket: " + ticketId, e);
            return null;
        } finally {
            closeQuietly(conn);
        }
    }

    // =========================================================================
    //  4. UI BINDING HELPER: JTABLE MODEL BUILDER
    // =========================================================================

    /**
     * Executes any SELECT query and returns a ready-to-use DefaultTableModel for JTable.
     *
     * @param query  SQL SELECT query
     * @param params Query parameters
     * @return Populated DefaultTableModel
     */
    public static DefaultTableModel fetchTableData(String query, Object... params) {
        Vector<String> columnNames = new Vector<>();
        Vector<Vector<Object>> data = new Vector<>();

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    ps.setObject(i + 1, params[i]);
                }
            }

            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();

                // Column headers
                for (int i = 1; i <= columnCount; i++) {
                    columnNames.add(metaData.getColumnLabel(i));
                }

                // Table rows
                while (rs.next()) {
                    Vector<Object> row = new Vector<>(columnCount);
                    for (int i = 1; i <= columnCount; i++) {
                        row.add(rs.getObject(i));
                    }
                    data.add(row);
                }
            }

        } catch (SQLException e) {
            handleException("Error loading table data for query: " + query, e);
        }

        return new DefaultTableModel(data, columnNames) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    // =========================================================================
    //  HELPER METHODS
    // =========================================================================

    private static void rollbackQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException ex) {
                System.err.println("[ParkingDAO] Rollback error: " + ex.getMessage());
            }
        }
    }

    private static void closeQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.setAutoCommit(true);
                conn.close();
            } catch (SQLException ex) {
                System.err.println("[ParkingDAO] Connection close error: " + ex.getMessage());
            }
        }
    }

    private static void handleException(String context, SQLException e) {
        System.err.println("[ParkingDAO] " + context);
        System.err.println("SQL State: " + e.getSQLState() + " | Vendor Code: " + e.getErrorCode());
        e.printStackTrace();

        String message = context + "\n\nError: " + e.getMessage()
                       + "\nSQL State: " + e.getSQLState()
                       + "\nError Code: " + e.getErrorCode();

        try {
            JOptionPane.showMessageDialog(
                null,
                message,
                "Database Error",
                JOptionPane.ERROR_MESSAGE
            );
        } catch (Exception ignored) {}
    }
}
