package com.parkingsystem.dao;

import com.parkingsystem.model.Slot;
import com.parkingsystem.model.Ticket;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object — handles all DB operations for the parking system.
 *
 * Assumed Oracle schema (create these tables in your DB):
 *
 * CREATE TABLE PARKING_SLOTS (
 *     SLOT_ID     NUMBER PRIMARY KEY,
 *     SLOT_NUMBER VARCHAR2(10) UNIQUE NOT NULL,
 *     FLOOR       VARCHAR2(10),
 *     SLOT_TYPE   VARCHAR2(20),   -- TWO_WHEELER | CAR | HEAVY
 *     STATUS      VARCHAR2(20)    -- AVAILABLE   | OCCUPIED
 * );
 *
 * CREATE TABLE PARKING_TICKETS (
 *     TICKET_ID    NUMBER PRIMARY KEY,
 *     VEHICLE_NUM  VARCHAR2(20) NOT NULL,
 *     VEHICLE_TYPE VARCHAR2(20),
 *     SLOT_ID      NUMBER REFERENCES PARKING_SLOTS(SLOT_ID),
 *     ENTRY_TIME   TIMESTAMP DEFAULT SYSTIMESTAMP,
 *     EXIT_TIME    TIMESTAMP
 * );
 *
 * CREATE SEQUENCE SEQ_TICKET_ID START WITH 1 INCREMENT BY 1;
 * CREATE SEQUENCE SEQ_SLOT_ID   START WITH 1 INCREMENT BY 1;
 *
 * Parking charge formula (computed in DB / here):
 *   TWO_WHEELER : Rs. 10 / hour
 *   CAR         : Rs. 20 / hour
 *   HEAVY       : Rs. 40 / hour
 */
public class ParkingDAO {

    // ------------------------------------------------------------------
    // 1. registerVehicleEntry
    // ------------------------------------------------------------------

    /**
     * Registers a vehicle entry:
     *  - Finds the first AVAILABLE slot matching the vehicle type.
     *  - Marks that slot as OCCUPIED.
     *  - Inserts a new ticket row and returns success.
     *
     * @param vehicleNum  licence plate, e.g. "MH12AB1234"
     * @param vehicleType "TWO_WHEELER" | "CAR" | "HEAVY"
     * @return true if entry was registered successfully
     */
    public boolean registerVehicleEntry(String vehicleNum, String vehicleType) {
        String findSlotSQL =
            "SELECT SLOT_ID FROM PARKING_SLOTS " +
            "WHERE SLOT_TYPE = ? AND STATUS = 'AVAILABLE' AND ROWNUM = 1";

        String markOccupiedSQL =
            "UPDATE PARKING_SLOTS SET STATUS = 'OCCUPIED' WHERE SLOT_ID = ?";

        String insertTicketSQL =
            "INSERT INTO PARKING_TICKETS (TICKET_ID, VEHICLE_NUM, VEHICLE_TYPE, SLOT_ID, ENTRY_TIME) " +
            "VALUES (SEQ_TICKET_ID.NEXTVAL, ?, ?, ?, SYSTIMESTAMP)";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false); // begin transaction

            // Step 1 — find available slot
            int slotId = -1;
            try (PreparedStatement ps = conn.prepareStatement(findSlotSQL)) {
                ps.setString(1, vehicleType);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        slotId = rs.getInt("SLOT_ID");
                    } else {
                        // No slot available for this type
                        conn.rollback();
                        return false;
                    }
                }
            }

            // Step 2 — mark slot OCCUPIED
            try (PreparedStatement ps = conn.prepareStatement(markOccupiedSQL)) {
                ps.setInt(1, slotId);
                ps.executeUpdate();
            }

            // Step 3 — insert ticket
            try (PreparedStatement ps = conn.prepareStatement(insertTicketSQL)) {
                ps.setString(1, vehicleNum);
                ps.setString(2, vehicleType);
                ps.setInt(3, slotId);
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
     *  - Fetches the open ticket for the given ticketId.
     *  - Calculates parking charge based on duration + vehicle type.
     *  - Updates exit time on the ticket row.
     *  - Frees the parking slot (AVAILABLE).
     *  - Returns the charge amount (0.0 on failure).
     *
     * @param ticketId    numeric ticket ID printed on the entry slip
     * @param paymentMode "CASH" | "CARD" | "UPI" (stored for future use)
     * @return computed parking charge in INR
     */
    public double processVehicleExit(int ticketId, String paymentMode) {
        // Fetch ticket details (slot + vehicle type + entry time)
        String fetchSQL =
            "SELECT T.SLOT_ID, T.VEHICLE_TYPE, T.ENTRY_TIME " +
            "FROM PARKING_TICKETS T " +
            "WHERE T.TICKET_ID = ? AND T.EXIT_TIME IS NULL";

        String updateExitSQL =
            "UPDATE PARKING_TICKETS SET EXIT_TIME = SYSTIMESTAMP WHERE TICKET_ID = ?";

        String freeSlotSQL =
            "UPDATE PARKING_SLOTS SET STATUS = 'AVAILABLE' WHERE SLOT_ID = ?";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            int    slotId      = -1;
            String vehicleType = "";
            Timestamp entryTs  = null;

            try (PreparedStatement ps = conn.prepareStatement(fetchSQL)) {
                ps.setInt(1, ticketId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        slotId      = rs.getInt("SLOT_ID");
                        vehicleType = rs.getString("VEHICLE_TYPE");
                        entryTs     = rs.getTimestamp("ENTRY_TIME");
                    } else {
                        conn.rollback();
                        return 0.0; // ticket not found or already exited
                    }
                }
            }

            // Calculate charge
            long   durationMillis = System.currentTimeMillis() - entryTs.getTime();
            long   hours          = Math.max(1, (long) Math.ceil(durationMillis / 3_600_000.0));
            double ratePerHour    = getRateForType(vehicleType);
            double charge         = hours * ratePerHour;

            // Update exit time
            try (PreparedStatement ps = conn.prepareStatement(updateExitSQL)) {
                ps.setInt(1, ticketId);
                ps.executeUpdate();
            }

            // Free slot
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
     * Fetches the current status of all parking slots ordered by floor
     * and slot number. Used to populate the slot-monitor grid in the UI.
     *
     * @return list of {@link Slot} objects reflecting live DB state
     */
    public List<Slot> getRealTimeSlotStatuses() {
        String sql =
            "SELECT SLOT_ID, SLOT_NUMBER, FLOOR, SLOT_TYPE, STATUS " +
            "FROM PARKING_SLOTS ORDER BY FLOOR, SLOT_NUMBER";

        List<Slot> slots = new ArrayList<>();
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                slots.add(new Slot(
                    rs.getInt("SLOT_ID"),
                    rs.getString("SLOT_NUMBER"),
                    rs.getString("FLOOR"),
                    rs.getString("SLOT_TYPE"),
                    rs.getString("STATUS")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return slots;
    }

    // ------------------------------------------------------------------
    // 4. addOrUpdateSlot
    // ------------------------------------------------------------------

    /**
     * Adds a new slot or updates an existing slot's floor/type.
     * Uses an Oracle MERGE (upsert) on SLOT_NUMBER as the natural key.
     *
     * @param slotNum slot label, e.g. "A-01"
     * @param floor   floor identifier, e.g. "G", "1", "2"
     * @param type    "TWO_WHEELER" | "CAR" | "HEAVY"
     * @return true on success
     */
    public boolean addOrUpdateSlot(String slotNum, String floor, String type) {
        // Oracle MERGE upsert — inserts if new, updates floor/type if existing
        String mergeSQL =
            "MERGE INTO PARKING_SLOTS tgt " +
            "USING (SELECT ? AS SLOT_NUMBER FROM DUAL) src " +
            "ON (tgt.SLOT_NUMBER = src.SLOT_NUMBER) " +
            "WHEN MATCHED THEN " +
            "    UPDATE SET tgt.FLOOR = ?, tgt.SLOT_TYPE = ? " +
            "WHEN NOT MATCHED THEN " +
            "    INSERT (SLOT_ID, SLOT_NUMBER, FLOOR, SLOT_TYPE, STATUS) " +
            "    VALUES (SEQ_SLOT_ID.NEXTVAL, ?, ?, ?, 'AVAILABLE')";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(mergeSQL)) {

            // ON clause param
            ps.setString(1, slotNum);
            // WHEN MATCHED — update params
            ps.setString(2, floor);
            ps.setString(3, type);
            // WHEN NOT MATCHED — insert params
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
    // 5. generateRevenueReport
    // ------------------------------------------------------------------

    /**
     * Returns a ResultSet of completed parking sessions within the given
     * date range, including computed charge per session.
     *
     * Caller is responsible for closing the returned ResultSet and its
     * underlying Connection — use try-with-resources.
     *
     * Columns returned:
     *   TICKET_ID, VEHICLE_NUM, VEHICLE_TYPE, SLOT_NUMBER,
     *   ENTRY_TIME, EXIT_TIME, HOURS_PARKED, CHARGE
     *
     * @param startDate "DD-MON-YYYY", e.g. "01-OCT-2026"
     * @param endDate   "DD-MON-YYYY", e.g. "31-OCT-2026"
     * @return ResultSet with revenue data
     */
    public ResultSet generateRevenueReport(String startDate, String endDate) throws SQLException {
        Connection conn = DBConnection.getConnection();

        String sql =
            "SELECT T.TICKET_ID, T.VEHICLE_NUM, T.VEHICLE_TYPE, S.SLOT_NUMBER, " +
            "       TO_CHAR(T.ENTRY_TIME, 'DD-MON-YYYY HH24:MI') AS ENTRY_TIME, " +
            "       TO_CHAR(T.EXIT_TIME,  'DD-MON-YYYY HH24:MI') AS EXIT_TIME, " +
            "       CEIL((T.EXIT_TIME - T.ENTRY_TIME) * 24) AS HOURS_PARKED, " +
            "       CEIL((T.EXIT_TIME - T.ENTRY_TIME) * 24) * " +
            "           CASE T.VEHICLE_TYPE " +
            "               WHEN 'TWO_WHEELER' THEN 10 " +
            "               WHEN 'HEAVY'       THEN 40 " +
            "               ELSE 20 " +
            "           END AS CHARGE " +
            "FROM PARKING_TICKETS T " +
            "JOIN PARKING_SLOTS   S ON T.SLOT_ID = S.SLOT_ID " +
            "WHERE T.EXIT_TIME IS NOT NULL " +
            "  AND TRUNC(T.ENTRY_TIME) BETWEEN TO_DATE(?, 'DD-MON-YYYY') " +
            "                               AND TO_DATE(?, 'DD-MON-YYYY') " +
            "ORDER BY T.ENTRY_TIME";

        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, startDate);
        ps.setString(2, endDate);
        return ps.executeQuery();
        // Note: caller must close the ResultSet (and hence the Connection)
    }
}
