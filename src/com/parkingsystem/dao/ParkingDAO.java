package com.parkingsystem.dao;

import com.parkingsystem.model.Slot;
import com.parkingsystem.model.Ticket;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object — handles ALL DB operations for the Car Parking System.
 * Matches schema.sql definitions:
 *  - Parking_slots (Slot_id VARCHAR2(20), Slot_no VARCHAR2(10), Floor NUMBER(3), Status VARCHAR2(20) [available, reserved, blocked], Type_allowed VARCHAR2(20))
 *  - Vehicles (Vehicle_no VARCHAR2(20), Type VARCHAR2(20), Owner_contact VARCHAR2(15))
 *  - Parking_Tickets (Ticket_ID VARCHAR2(20), Entry_time TIMESTAMP, Exit_time TIMESTAMP, Vehicle_no VARCHAR2(20), Slot_ID VARCHAR2(20))
 *  - Payments (Payment_ID VARCHAR2(20), Amount NUMBER(10,2), Payment_Time TIMESTAMP, Payment_mode VARCHAR2(20), Ticket_id VARCHAR2(20))
 *  - Sequences: SEQ_SLOT_ID, SEQ_TICKET_ID, SEQ_PAYMENT_ID
 */
public class ParkingDAO {

    // ==================================================================
    //  Fee Calculation Config: Fixed Base Fee + Running Fee per Minute
    // ==================================================================
    // CAR:         Fixed ₹20 + ₹1.00 / minute
    // TWO_WHEELER: Fixed ₹10 + ₹0.50 / minute
    // HEAVY:       Fixed ₹40 + ₹2.00 / minute
    public static class FeeRate {
        public final double fixedBase;
        public final double runningPerMinute;

        public FeeRate(double fixedBase, double runningPerMinute) {
            this.fixedBase = fixedBase;
            this.runningPerMinute = runningPerMinute;
        }
    }

    public static FeeRate getFeeRateForType(String vehicleType) {
        if (vehicleType == null) return new FeeRate(20.0, 1.0);
        switch (vehicleType.toUpperCase()) {
            case "TWO_WHEELER":
                return new FeeRate(10.0, 0.50);
            case "HEAVY":
                return new FeeRate(40.0, 2.00);
            case "CAR":
            default:
                return new FeeRate(20.0, 1.00);
        }
    }

    public static double calculateFee(String vehicleType, long minutesElapsed) {
        long minutes = Math.max(1L, minutesElapsed);
        FeeRate rate = getFeeRateForType(vehicleType);
        return rate.fixedBase + (rate.runningPerMinute * minutes);
    }

    // ------------------------------------------------------------------
    // 1. registerVehicleEntry (Supports choosing specific slot or auto-allocating)
    // ------------------------------------------------------------------

    /**
     * Registers vehicle entry. If chosenSlotId is provided, assigns that specific slot.
     * Otherwise finds the first available matching slot.
     *
     * @param vehicleNum    Licence plate
     * @param vehicleType   CAR, TWO_WHEELER, HEAVY
     * @param ownerContact  Owner phone/contact
     * @param chosenSlotId  Selected Slot_id (optional, null to auto-allocate)
     * @return Generated Ticket object containing Ticket_ID, Slot_no, etc., or null on failure
     */
    public Ticket registerVehicleEntry(String vehicleNum, String vehicleType,
                                      String ownerContact, String chosenSlotId) {
        String upsertVehicleSQL =
            "MERGE INTO Vehicles tgt " +
            "USING (SELECT ? AS Vehicle_no FROM DUAL) src " +
            "ON (tgt.Vehicle_no = src.Vehicle_no) " +
            "WHEN MATCHED THEN " +
            "    UPDATE SET tgt.Type = ?, tgt.Owner_contact = ? " +
            "WHEN NOT MATCHED THEN " +
            "    INSERT (Vehicle_no, Type, Owner_contact) VALUES (?, ?, ?)";

        String findSpecificSlotSQL =
            "SELECT Slot_id, Slot_no FROM Parking_slots " +
            "WHERE Slot_id = ? AND LOWER(Status) = 'available' FOR UPDATE";

        String findAutoSlotSQL =
            "SELECT Slot_id, Slot_no FROM Parking_slots " +
            "WHERE UPPER(Type_allowed) = UPPER(?) AND LOWER(Status) = 'available' AND ROWNUM = 1 " +
            "FOR UPDATE";

        String markReservedSQL =
            "UPDATE Parking_slots SET Status = 'reserved' WHERE Slot_id = ?";

        String getNextTicketSeqSQL = "SELECT SEQ_TICKET_ID.NEXTVAL AS SEQ FROM DUAL";

        String insertTicketSQL =
            "INSERT INTO Parking_Tickets (Ticket_ID, Entry_time, Vehicle_no, Slot_ID) " +
            "VALUES (?, CURRENT_TIMESTAMP, ?, ?)";

        String getTicketDetailsSQL =
            "SELECT TO_CHAR(Entry_time, 'DD-MON-YYYY HH24:MI:SS') AS Entry_str " +
            "FROM Parking_Tickets WHERE Ticket_ID = ?";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            // 1. Upsert Vehicle
            try (PreparedStatement ps = conn.prepareStatement(upsertVehicleSQL)) {
                ps.setString(1, vehicleNum);
                ps.setString(2, vehicleType);
                ps.setString(3, ownerContact);
                ps.setString(4, vehicleNum);
                ps.setString(5, vehicleType);
                ps.setString(6, ownerContact);
                ps.executeUpdate();
            }

            // 2. Validate/Find Slot
            String slotId = null;
            String slotNo = null;

            if (chosenSlotId != null && !chosenSlotId.trim().isEmpty()) {
                try (PreparedStatement ps = conn.prepareStatement(findSpecificSlotSQL)) {
                    ps.setString(1, chosenSlotId.trim());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            slotId = rs.getString("Slot_id");
                            slotNo = rs.getString("Slot_no");
                        } else {
                            conn.rollback();
                            return null; // Slot already taken or not available
                        }
                    }
                }
            } else {
                try (PreparedStatement ps = conn.prepareStatement(findAutoSlotSQL)) {
                    ps.setString(1, vehicleType);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            slotId = rs.getString("Slot_id");
                            slotNo = rs.getString("Slot_no");
                        } else {
                            conn.rollback();
                            return null; // Parking full
                        }
                    }
                }
            }

            // 3. Mark Slot Reserved
            try (PreparedStatement ps = conn.prepareStatement(markReservedSQL)) {
                ps.setString(1, slotId);
                ps.executeUpdate();
            }

            // 4. Generate Ticket_ID from sequence
            long seqNum = 1;
            try (PreparedStatement ps = conn.prepareStatement(getNextTicketSeqSQL);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    seqNum = rs.getLong("SEQ");
                }
            }
            String ticketIdStr = "TKT_" + seqNum;

            // 5. Insert Ticket
            try (PreparedStatement ps = conn.prepareStatement(insertTicketSQL)) {
                ps.setString(1, ticketIdStr);
                ps.setString(2, vehicleNum);
                ps.setString(3, slotId);
                ps.executeUpdate();
            }

            // 6. Read formatted Entry Time
            String entryTimeStr = "";
            try (PreparedStatement ps = conn.prepareStatement(getTicketDetailsSQL)) {
                ps.setString(1, ticketIdStr);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        entryTimeStr = rs.getString("Entry_str");
                    }
                }
            }

            conn.commit();
            return new Ticket(ticketIdStr, vehicleNum, vehicleType, slotId, slotNo, entryTimeStr, null);

        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    // ------------------------------------------------------------------
    // 2. Lookup Open Ticket / Estimate Fee Details
    // ------------------------------------------------------------------

    public static class OpenTicketDetail {
        public String ticketId;
        public String vehicleNo;
        public String vehicleType;
        public String ownerContact;
        public String slotId;
        public String slotNo;
        public Timestamp entryTime;
        public String entryTimeFormatted;
        public long minutesElapsed;
        public double fixedFee;
        public double runningRate;
        public double totalFee;
    }

    public OpenTicketDetail getOpenTicketDetail(String ticketInput) {
        String sql =
            "SELECT T.Ticket_ID, T.Vehicle_no, V.Type AS Vehicle_Type, V.Owner_contact, " +
            "       T.Slot_ID, S.Slot_no, T.Entry_time, " +
            "       TO_CHAR(T.Entry_time, 'DD-MON-YYYY HH24:MI:SS') AS Entry_Str, " +
            "       ROUND((SYSDATE - CAST(T.Entry_time AS DATE)) * 24 * 60) AS Minutes_Elapsed " +
            "FROM Parking_Tickets T " +
            "JOIN Vehicles V ON T.Vehicle_no = V.Vehicle_no " +
            "JOIN Parking_slots S ON T.Slot_ID = S.Slot_id " +
            "WHERE (T.Ticket_ID = ? OR T.Ticket_ID = ?) AND T.Exit_time IS NULL";

        String cleanInput = ticketInput != null ? ticketInput.trim() : "";
        String normalizedId = cleanInput.startsWith("TKT_") ? cleanInput : "TKT_" + cleanInput;

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, cleanInput);
            ps.setString(2, normalizedId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    OpenTicketDetail d = new OpenTicketDetail();
                    d.ticketId = rs.getString("Ticket_ID");
                    d.vehicleNo = rs.getString("Vehicle_no");
                    d.vehicleType = rs.getString("Vehicle_Type");
                    d.ownerContact = rs.getString("Owner_contact") != null ? rs.getString("Owner_contact") : "N/A";
                    d.slotId = rs.getString("Slot_ID");
                    d.slotNo = rs.getString("Slot_no");
                    d.entryTime = rs.getTimestamp("Entry_time");
                    d.entryTimeFormatted = rs.getString("Entry_Str");
                    d.minutesElapsed = Math.max(1L, rs.getLong("Minutes_Elapsed"));

                    FeeRate rate = getFeeRateForType(d.vehicleType);
                    d.fixedFee = rate.fixedBase;
                    d.runningRate = rate.runningPerMinute;
                    d.totalFee = d.fixedFee + (d.runningRate * d.minutesElapsed);

                    return d;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ------------------------------------------------------------------
    // 3. processVehicleExit
    // ------------------------------------------------------------------

    /**
     * Completes exit transaction:
     *  - Inserts Payment record (Payment_ID: 'PAY_' || SEQ_PAYMENT_ID.NEXTVAL)
     *  - Updates Exit_time on Parking_Tickets
     *  - Frees Parking_slots status back to 'available'
     */
    public boolean processVehicleExit(String ticketId, String vehicleNo, String slotNo,
                                      double amount, String paymentMode) {
        String insertPaymentSQL =
            "INSERT INTO Payments (Payment_ID, Amount, Payment_Time, Payment_mode, Ticket_id) " +
            "VALUES ('PAY_' || SEQ_PAYMENT_ID.NEXTVAL, ?, CURRENT_TIMESTAMP, ?, ?)";

        String updateExitSQL =
            "UPDATE Parking_Tickets SET Exit_time = CURRENT_TIMESTAMP WHERE Ticket_ID = ?";

        String freeSlotSQL =
            "UPDATE Parking_slots SET Status = 'available' WHERE Slot_id = (SELECT Slot_ID FROM Parking_Tickets WHERE Ticket_ID = ?)";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            // 1. Insert Payment
            try (PreparedStatement ps = conn.prepareStatement(insertPaymentSQL)) {
                ps.setDouble(1, amount);
                ps.setString(2, paymentMode != null ? paymentMode : "CASH");
                ps.setString(3, ticketId);
                ps.executeUpdate();
            }

            // 2. Stamp Exit Time
            try (PreparedStatement ps = conn.prepareStatement(updateExitSQL)) {
                ps.setString(1, ticketId);
                ps.executeUpdate();
            }

            // 3. Free Slot
            try (PreparedStatement ps = conn.prepareStatement(freeSlotSQL)) {
                ps.setString(1, ticketId);
                ps.executeUpdate();
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ------------------------------------------------------------------
    // 4. getRealTimeSlotStatuses
    // ------------------------------------------------------------------

    public List<Slot> getRealTimeSlotStatuses() {
        String sql =
            "SELECT Slot_id, Slot_no, Floor, Type_allowed, Status " +
            "FROM Parking_slots ORDER BY Floor, Slot_no";

        List<Slot> slots = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                slots.add(new Slot(
                    rs.getString("Slot_id"),
                    rs.getString("Slot_no"),
                    String.valueOf(rs.getInt("Floor")),
                    rs.getString("Type_allowed"),
                    rs.getString("Status")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return slots;
    }

    // ------------------------------------------------------------------
    // 5. getOccupiedSlotDetail
    // ------------------------------------------------------------------

    public String getOccupiedSlotDetail(String slotId) {
        String sql =
            "SELECT T.Ticket_ID, T.Vehicle_no, V.Type, V.Owner_contact, S.Slot_no, " +
            "       TO_CHAR(T.Entry_time, 'DD-MON-YYYY HH24:MI:SS') AS Entry_time, " +
            "       ROUND((SYSDATE - CAST(T.Entry_time AS DATE)) * 24 * 60) AS Minutes_Elapsed " +
            "FROM Parking_Tickets T " +
            "JOIN Vehicles V ON T.Vehicle_no = V.Vehicle_no " +
            "JOIN Parking_slots S ON T.Slot_ID = S.Slot_id " +
            "WHERE T.Slot_ID = ? AND T.Exit_time IS NULL";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, slotId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long minutes = Math.max(1L, rs.getLong("Minutes_Elapsed"));
                    String vType = rs.getString("Type");
                    FeeRate rate = getFeeRateForType(vType);
                    double totalFee = rate.fixedBase + (rate.runningPerMinute * minutes);

                    return String.format(
                        "Ticket ID     : %s%n" +
                        "Slot Number   : %s%n" +
                        "Vehicle No    : %s%n" +
                        "Vehicle Type  : %s%n" +
                        "Owner Contact : %s%n" +
                        "Entry Time    : %s%n" +
                        "Time Elapsed  : %d minute(s)%n" +
                        "Fee Formula   : Fixed \u20b9%.2f + (\u20b9%.2f \u00d7 %d mins)%n" +
                        "Estimated Fee : \u20b9%.2f",
                        rs.getString("Ticket_ID"),
                        rs.getString("Slot_no"),
                        rs.getString("Vehicle_no"),
                        vType,
                        rs.getString("Owner_contact") != null ? rs.getString("Owner_contact") : "N/A",
                        rs.getString("Entry_time"),
                        minutes,
                        rate.fixedBase,
                        rate.runningPerMinute,
                        minutes,
                        totalFee
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ------------------------------------------------------------------
    // 6. addOrUpdateSlot
    // ------------------------------------------------------------------

    public boolean addOrUpdateSlot(String slotNum, String floorStr, String type) {
        int floor = 1;
        try {
            floor = Integer.parseInt(floorStr.trim());
        } catch (NumberFormatException e) {
            floor = 1;
        }

        String mergeSQL =
            "MERGE INTO Parking_slots tgt " +
            "USING (SELECT ? AS Slot_no FROM DUAL) src " +
            "ON (tgt.Slot_no = src.Slot_no) " +
            "WHEN MATCHED THEN " +
            "    UPDATE SET tgt.Floor = ?, tgt.Type_allowed = ? " +
            "WHEN NOT MATCHED THEN " +
            "    INSERT (Slot_id, Slot_no, Floor, Status, Type_allowed) " +
            "    VALUES ('SLOT_' || SEQ_SLOT_ID.NEXTVAL, ?, ?, 'available', ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(mergeSQL)) {

            ps.setString(1, slotNum);
            ps.setInt(2, floor);
            ps.setString(3, type);
            ps.setString(4, slotNum);
            ps.setInt(5, floor);
            ps.setString(6, type);

            ps.executeUpdate();
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ------------------------------------------------------------------
    // 7. generateRevenueReport (Robust data retrieval joining all 4 tables)
    // ------------------------------------------------------------------

    public static class RevenueRecord {
        public String ticketId;
        public String vehicleNo;
        public String vehicleType;
        public String slotNo;
        public String entryTime;
        public String exitTime;
        public long durationMinutes;
        public double amount;
        public String paymentMode;
    }

    public List<RevenueRecord> fetchRevenueReportData(String startDate, String endDate) {
        List<RevenueRecord> records = new ArrayList<>();

        // Robust date parsing and joining of Parking_Tickets, Vehicles, Parking_slots, Payments
        String sql =
            "SELECT T.Ticket_ID, T.Vehicle_no, V.Type AS Vehicle_Type, S.Slot_no, " +
            "       TO_CHAR(T.Entry_time, 'YYYY-MM-DD HH24:MI') AS Entry_Formatted, " +
            "       TO_CHAR(T.Exit_time, 'YYYY-MM-DD HH24:MI') AS Exit_Formatted, " +
            "       ROUND((CAST(T.Exit_time AS DATE) - CAST(T.Entry_time AS DATE)) * 24 * 60) AS Duration_Minutes, " +
            "       P.Amount, P.Payment_mode " +
            "FROM Parking_Tickets T " +
            "JOIN Vehicles V ON T.Vehicle_no = V.Vehicle_no " +
            "JOIN Parking_slots S ON T.Slot_ID = S.Slot_id " +
            "JOIN Payments P ON P.Ticket_id = T.Ticket_ID " +
            "WHERE T.Exit_time IS NOT NULL " +
            "  AND TRUNC(CAST(T.Entry_time AS DATE)) >= TO_DATE(?, 'YYYY-MM-DD') " +
            "  AND TRUNC(CAST(T.Entry_time AS DATE)) <= TO_DATE(?, 'YYYY-MM-DD') " +
            "ORDER BY T.Entry_time DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, startDate);
            ps.setString(2, endDate);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    RevenueRecord rec = new RevenueRecord();
                    rec.ticketId = rs.getString("Ticket_ID");
                    rec.vehicleNo = rs.getString("Vehicle_no");
                    rec.vehicleType = rs.getString("Vehicle_Type");
                    rec.slotNo = rs.getString("Slot_no");
                    rec.entryTime = rs.getString("Entry_Formatted");
                    rec.exitTime = rs.getString("Exit_Formatted");
                    rec.durationMinutes = rs.getLong("Duration_Minutes");
                    rec.amount = rs.getDouble("Amount");
                    rec.paymentMode = rs.getString("Payment_mode");
                    records.add(rec);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return records;
    }
}

