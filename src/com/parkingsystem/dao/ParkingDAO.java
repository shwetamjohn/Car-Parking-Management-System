package com.parkingsystem.dao;

import com.parkingsystem.model.Slot;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object — handles ALL DB operations for the Car Parking System.
 *
 * ══════════════════════════════════════════════════════════════
 *  Oracle Schema (4 tables — run this DDL before first launch)
 * ══════════════════════════════════════════════════════════════
 *
 * CREATE TABLE VEHICLES (
 *     VEHICLE_NO    VARCHAR2(20) PRIMARY KEY,
 *     TYPE          VARCHAR2(20),        -- TWO_WHEELER | CAR | HEAVY
 *     OWNER_CONTACT VARCHAR2(30)
 * );
 *
 * CREATE TABLE PARKING_SLOTS (
 *     SLOT_ID      NUMBER PRIMARY KEY,
 *     SLOT_NO      VARCHAR2(10) UNIQUE NOT NULL,
 *     FLOOR        VARCHAR2(10),
 *     STATUS       VARCHAR2(20),         -- AVAILABLE | OCCUPIED | UNDER_MAINTENANCE
 *     TYPE_ALLOWED VARCHAR2(20)          -- TWO_WHEELER | CAR | HEAVY
 * );
 *
 * CREATE TABLE PARKING_TICKETS (
 *     TICKET_ID  NUMBER PRIMARY KEY,
 *     ENTRY_TIME TIMESTAMP DEFAULT SYSTIMESTAMP,
 *     EXIT_TIME  TIMESTAMP,
 *     VEHICLE_NO VARCHAR2(20) REFERENCES VEHICLES(VEHICLE_NO),
 *     SLOT_ID    NUMBER       REFERENCES PARKING_SLOTS(SLOT_ID)
 * );
 *
 * CREATE TABLE PAYMENTS (
 *     PAYMENT_ID   NUMBER PRIMARY KEY,
 *     AMOUNT       NUMBER(10,2),
 *     PAYMENT_TIME TIMESTAMP DEFAULT SYSTIMESTAMP,
 *     PAYMENT_MODE VARCHAR2(10),          -- CASH | CARD | UPI
 *     TICKET_ID    NUMBER REFERENCES PARKING_TICKETS(TICKET_ID)
 * );
 *
 * CREATE SEQUENCE SEQ_TICKET_ID  START WITH 1 INCREMENT BY 1;
 * CREATE SEQUENCE SEQ_SLOT_ID    START WITH 1 INCREMENT BY 1;
 * CREATE SEQUENCE SEQ_PAYMENT_ID START WITH 1 INCREMENT BY 1;
 *
 * ══════════════════════════════════════════
 *  Parking charge rates (INR)
 * ══════════════════════════════════════════
 *   TWO_WHEELER : ₹10 / hour  (minimum 1 hour)
 *   CAR         : ₹20 / hour
 *   HEAVY       : ₹40 / hour
 */
public class ParkingDAO {

    // ------------------------------------------------------------------
    // 1. registerVehicleEntry
    // ------------------------------------------------------------------

    /**
     * Registers a vehicle entry:
     *  1. Upserts the vehicle record into VEHICLES (Oracle MERGE).
     *  2. Finds the first AVAILABLE slot matching the vehicle type.
     *  3. Marks that slot OCCUPIED.
     *  4. Inserts a new row in PARKING_TICKETS.
     *
     * The entire operation is wrapped in a single transaction.
     *
     * @param vehicleNum   licence plate, e.g. "MH12AB1234"
     * @param vehicleType  "TWO_WHEELER" | "CAR" | "HEAVY"
     * @param ownerContact owner phone / email (stored in VEHICLES)
     * @return true on success; false if no slot available or DB error
     */
    public boolean registerVehicleEntry(String vehicleNum, String vehicleType,
                                        String ownerContact) {
        // MERGE keeps VEHICLES up-to-date on repeat visits
        String upsertVehicleSQL =
            "MERGE INTO VEHICLES tgt " +
            "USING (SELECT ? AS VEHICLE_NO FROM DUAL) src " +
            "ON (tgt.VEHICLE_NO = src.VEHICLE_NO) " +
            "WHEN MATCHED THEN " +
            "    UPDATE SET tgt.TYPE = ?, tgt.OWNER_CONTACT = ? " +
            "WHEN NOT MATCHED THEN " +
            "    INSERT (VEHICLE_NO, TYPE, OWNER_CONTACT) VALUES (?, ?, ?)";

        String findSlotSQL =
            "SELECT SLOT_ID FROM PARKING_SLOTS " +
            "WHERE TYPE_ALLOWED = ? AND STATUS = 'AVAILABLE' AND ROWNUM = 1 " +
            "FOR UPDATE";   // lock the row to prevent double-assignment

        String markOccupiedSQL =
            "UPDATE PARKING_SLOTS SET STATUS = 'OCCUPIED' WHERE SLOT_ID = ?";

        String insertTicketSQL =
            "INSERT INTO PARKING_TICKETS (TICKET_ID, VEHICLE_NO, SLOT_ID, ENTRY_TIME) " +
            "VALUES (SEQ_TICKET_ID.NEXTVAL, ?, ?, SYSTIMESTAMP)";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            // Step 1 — upsert vehicle record
            try (PreparedStatement ps = conn.prepareStatement(upsertVehicleSQL)) {
                ps.setString(1, vehicleNum);   // ON key
                ps.setString(2, vehicleType);  // UPDATE
                ps.setString(3, ownerContact); // UPDATE
                ps.setString(4, vehicleNum);   // INSERT
                ps.setString(5, vehicleType);  // INSERT
                ps.setString(6, ownerContact); // INSERT
                ps.executeUpdate();
            }

            // Step 2 — find first available slot for this vehicle type
            int slotId = -1;
            try (PreparedStatement ps = conn.prepareStatement(findSlotSQL)) {
                ps.setString(1, vehicleType);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        slotId = rs.getInt("SLOT_ID");
                    } else {
                        conn.rollback();
                        return false;  // parking full for this vehicle type
                    }
                }
            }

            // Step 3 — mark slot OCCUPIED
            try (PreparedStatement ps = conn.prepareStatement(markOccupiedSQL)) {
                ps.setInt(1, slotId);
                ps.executeUpdate();
            }

            // Step 4 — create parking ticket
            try (PreparedStatement ps = conn.prepareStatement(insertTicketSQL)) {
                ps.setString(1, vehicleNum);
                ps.setInt(2, slotId);
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
    // 2. processVehicleExit
    // ------------------------------------------------------------------

    /**
     * Processes a vehicle exit:
     *  1. Fetches the open ticket; joins VEHICLES to get vehicle type for rate.
     *  2. Calculates parking charge (duration × hourly rate, min 1 hour).
     *  3. Inserts a PAYMENTS record with amount + mode.
     *  4. Stamps EXIT_TIME on the ticket.
     *  5. Frees the parking slot back to AVAILABLE.
     *
     * @param ticketId    numeric ticket ID printed on entry slip
     * @param paymentMode "CASH" | "CARD" | "UPI"
     * @return computed charge in INR; 0.0 if ticket not found / already exited
     */
    public double processVehicleExit(int ticketId, String paymentMode) {
        // Join VEHICLES to get vehicle type (not stored in PARKING_TICKETS)
        String fetchSQL =
            "SELECT T.SLOT_ID, V.TYPE AS VEHICLE_TYPE, T.ENTRY_TIME " +
            "FROM PARKING_TICKETS T " +
            "JOIN VEHICLES V ON T.VEHICLE_NO = V.VEHICLE_NO " +
            "WHERE T.TICKET_ID = ? AND T.EXIT_TIME IS NULL";

        String insertPaymentSQL =
            "INSERT INTO PAYMENTS (PAYMENT_ID, AMOUNT, PAYMENT_TIME, PAYMENT_MODE, TICKET_ID) " +
            "VALUES (SEQ_PAYMENT_ID.NEXTVAL, ?, SYSTIMESTAMP, ?, ?)";

        String updateExitSQL =
            "UPDATE PARKING_TICKETS SET EXIT_TIME = SYSTIMESTAMP WHERE TICKET_ID = ?";

        String freeSlotSQL =
            "UPDATE PARKING_SLOTS SET STATUS = 'AVAILABLE' WHERE SLOT_ID = ?";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            int       slotId      = -1;
            String    vehicleType = "";
            Timestamp entryTs     = null;

            try (PreparedStatement ps = conn.prepareStatement(fetchSQL)) {
                ps.setInt(1, ticketId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        slotId      = rs.getInt("SLOT_ID");
                        vehicleType = rs.getString("VEHICLE_TYPE");
                        entryTs     = rs.getTimestamp("ENTRY_TIME");
                    } else {
                        conn.rollback();
                        return 0.0;  // not found or already exited
                    }
                }
            }

            // Calculate charge
            long   durationMillis = System.currentTimeMillis() - entryTs.getTime();
            long   hours          = Math.max(1L, (long) Math.ceil(durationMillis / 3_600_000.0));
            double ratePerHour    = getRateForType(vehicleType);
            double charge         = hours * ratePerHour;

            // Insert into PAYMENTS table
            try (PreparedStatement ps = conn.prepareStatement(insertPaymentSQL)) {
                ps.setDouble(1, charge);
                ps.setString(2, paymentMode);
                ps.setInt(3, ticketId);
                ps.executeUpdate();
            }

            // Stamp exit time on ticket
            try (PreparedStatement ps = conn.prepareStatement(updateExitSQL)) {
                ps.setInt(1, ticketId);
                ps.executeUpdate();
            }

            // Free the parking slot
            try (PreparedStatement ps = conn.prepareStatement(freeSlotSQL)) {
                ps.setInt(1, slotId);
                ps.executeUpdate();
            }

            conn.commit();
            return charge;

        } catch (SQLException e) {
            e.printStackTrace();
            return 0.0;
        }
    }

    /** Returns the hourly parking rate (INR) for a given vehicle type. */
    private double getRateForType(String vehicleType) {
        if (vehicleType == null) return 20.0;
        switch (vehicleType.toUpperCase()) {
            case "TWO_WHEELER": return 10.0;
            case "HEAVY":       return 40.0;
            case "CAR":
            default:            return 20.0;
        }
    }

    // ------------------------------------------------------------------
    // 3. getRealTimeSlotStatuses
    // ------------------------------------------------------------------

    /**
     * Fetches the live status of every parking slot from DB.
     * Ordered by FLOOR, then SLOT_NO for consistent grid display.
     * Status values: AVAILABLE | OCCUPIED | UNDER_MAINTENANCE.
     *
     * @return list of {@link Slot} objects
     */
    public List<Slot> getRealTimeSlotStatuses() {
        String sql =
            "SELECT SLOT_ID, SLOT_NO, FLOOR, TYPE_ALLOWED, STATUS " +
            "FROM PARKING_SLOTS ORDER BY FLOOR, SLOT_NO";

        List<Slot> slots = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                slots.add(new Slot(
                    rs.getInt("SLOT_ID"),
                    rs.getString("SLOT_NO"),
                    rs.getString("FLOOR"),
                    rs.getString("TYPE_ALLOWED"),
                    rs.getString("STATUS")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return slots;
    }

    // ------------------------------------------------------------------
    // 4. getOccupiedSlotDetail  (supports click-to-view in slot monitor)
    // ------------------------------------------------------------------

    /**
     * Returns a formatted detail string for an OCCUPIED slot by joining
     * PARKING_TICKETS and VEHICLES. Used when user clicks a slot cell.
     *
     * @param slotId the SLOT_ID of the occupied slot
     * @return multi-line detail string, or null if no active ticket found
     */
    public String getOccupiedSlotDetail(int slotId) {
        String sql =
            "SELECT T.TICKET_ID, T.VEHICLE_NO, V.TYPE, V.OWNER_CONTACT, " +
            "       TO_CHAR(T.ENTRY_TIME, 'DD-MON-YYYY HH24:MI') AS ENTRY_TIME, " +
            "       ROUND((SYSDATE - CAST(T.ENTRY_TIME AS DATE)) * 24, 1) AS HOURS_ELAPSED " +
            "FROM PARKING_TICKETS T " +
            "JOIN VEHICLES V ON T.VEHICLE_NO = V.VEHICLE_NO " +
            "WHERE T.SLOT_ID = ? AND T.EXIT_TIME IS NULL";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, slotId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double hoursElapsed = rs.getDouble("HOURS_ELAPSED");
                    long   billableHrs  = Math.max(1L, (long) Math.ceil(hoursElapsed));
                    double estFee       = billableHrs * getRateForType(rs.getString("TYPE"));

                    return String.format(
                        "Ticket ID     : %d%n" +
                        "Vehicle No    : %s%n" +
                        "Vehicle Type  : %s%n" +
                        "Owner Contact : %s%n" +
                        "Entry Time    : %s%n" +
                        "Time Elapsed  : %.1f hour(s)%n" +
                        "Estimated Fee : \u20b9%.2f",
                        rs.getInt("TICKET_ID"),
                        rs.getString("VEHICLE_NO"),
                        rs.getString("TYPE"),
                        rs.getString("OWNER_CONTACT"),
                        rs.getString("ENTRY_TIME"),
                        hoursElapsed,
                        estFee
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ------------------------------------------------------------------
    // 5. addOrUpdateSlot
    // ------------------------------------------------------------------

    /**
     * Adds a new parking slot or updates an existing one via Oracle MERGE.
     * Uses SLOT_NO as the natural unique key.
     *
     * @param slotNum slot label, e.g. "A-01"
     * @param floor   floor identifier, e.g. "G", "1", "2"
     * @param type    "TWO_WHEELER" | "CAR" | "HEAVY"
     * @return true on success
     */
    public boolean addOrUpdateSlot(String slotNum, String floor, String type) {
        String mergeSQL =
            "MERGE INTO PARKING_SLOTS tgt " +
            "USING (SELECT ? AS SLOT_NO FROM DUAL) src " +
            "ON (tgt.SLOT_NO = src.SLOT_NO) " +
            "WHEN MATCHED THEN " +
            "    UPDATE SET tgt.FLOOR = ?, tgt.TYPE_ALLOWED = ? " +
            "WHEN NOT MATCHED THEN " +
            "    INSERT (SLOT_ID, SLOT_NO, FLOOR, TYPE_ALLOWED, STATUS) " +
            "    VALUES (SEQ_SLOT_ID.NEXTVAL, ?, ?, ?, 'AVAILABLE')";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(mergeSQL)) {

            // ON clause
            ps.setString(1, slotNum);
            // WHEN MATCHED — update
            ps.setString(2, floor);
            ps.setString(3, type);
            // WHEN NOT MATCHED — insert
            ps.setString(4, slotNum);
            ps.setString(5, floor);
            ps.setString(6, type);

            ps.executeUpdate();
            conn.commit();
            return true;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // ------------------------------------------------------------------
    // 6. generateRevenueReport
    // ------------------------------------------------------------------

    /**
     * Returns a ResultSet of all completed sessions in the given date range.
     * Joins all 4 tables to produce a full revenue report including
     * payment mode from the PAYMENTS table.
     *
     * Columns returned:
     *   TICKET_ID, VEHICLE_NO, VEHICLE_TYPE, SLOT_NO,
     *   ENTRY_TIME, EXIT_TIME, HOURS_PARKED, AMOUNT, PAYMENT_MODE
     *
     * Caller MUST close the returned ResultSet (and its Connection).
     *
     * @param startDate "DD-MON-YYYY", e.g. "01-OCT-2026"
     * @param endDate   "DD-MON-YYYY", e.g. "31-OCT-2026"
     * @return ResultSet with revenue rows
     */
    public ResultSet generateRevenueReport(String startDate, String endDate)
            throws SQLException {

        Connection conn = DBConnection.getConnection();

        String sql =
            "SELECT T.TICKET_ID, T.VEHICLE_NO, V.TYPE AS VEHICLE_TYPE, S.SLOT_NO, " +
            "       TO_CHAR(T.ENTRY_TIME, 'DD-MON-YYYY HH24:MI') AS ENTRY_TIME, " +
            "       TO_CHAR(T.EXIT_TIME,  'DD-MON-YYYY HH24:MI') AS EXIT_TIME, " +
            "       CEIL((T.EXIT_TIME - T.ENTRY_TIME) * 24)       AS HOURS_PARKED, " +
            "       P.AMOUNT, P.PAYMENT_MODE " +
            "FROM   PARKING_TICKETS T " +
            "JOIN   VEHICLES      V ON T.VEHICLE_NO = V.VEHICLE_NO " +
            "JOIN   PARKING_SLOTS S ON T.SLOT_ID    = S.SLOT_ID " +
            "JOIN   PAYMENTS      P ON P.TICKET_ID  = T.TICKET_ID " +
            "WHERE  T.EXIT_TIME IS NOT NULL " +
            "  AND  TRUNC(T.ENTRY_TIME) " +
            "           BETWEEN TO_DATE(?, 'DD-MON-YYYY') " +
            "               AND TO_DATE(?, 'DD-MON-YYYY') " +
            "ORDER BY T.ENTRY_TIME";

        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, startDate);
        ps.setString(2, endDate);
        return ps.executeQuery();
        // Caller closes the ResultSet → closes Statement → closes Connection
    }
}
