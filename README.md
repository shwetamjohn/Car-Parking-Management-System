# 🅿 Car Parking Management System

A Java Swing/AWT + JDBC + Oracle XE desktop application for managing a parking lot —  
vehicle entry/exit, real-time slot monitoring, slot management, and revenue reports.

---

## 📐 Architecture

```
ParkingUI  ──uses──►  ParkingDAO  ──uses──►  DBConnection
                           │
               creates / fetches / manages
                           │
              Ticket   Slot   Vehicles   Payment
```

| Layer | Class | Responsibility |
|-------|-------|----------------|
| UI    | `ParkingUI`    | Swing forms, dialogs, event handlers |
| DAO   | `ParkingDAO`   | All JDBC queries & transactions |
| DB    | `DBConnection` | Single-point Oracle connection factory |
| Model | `Ticket`, `Slot`, `Vehicles`, `Payment` | Plain Java POJOs |

---

## 🗂 Project Structure

```
Car-Parking-Management-System/
├── .gitignore
├── README.md
└── src/
    └── com/
        └── parkingsystem/
            ├── model/
            │   ├── Slot.java
            │   ├── Ticket.java
            │   ├── Vehicles.java       ← maps to VEHICLES table
            │   └── Payment.java        ← maps to PAYMENTS table
            ├── dao/
            │   ├── DBConnection.java
            │   └── ParkingDAO.java
            └── ui/
                └── ParkingUI.java
```

---

## ⚙️ Setup

### Prerequisites
- JDK 11+
- Oracle Database XE (or any Oracle instance on port 1521)
- `ojdbc8.jar` (or `ojdbc11.jar`) on your classpath — place it in `lib/`

### 1. Oracle Schema — run this DDL in SQL Developer / SQL*Plus

```sql
-- Table 1: Vehicles (master record per vehicle registration)
CREATE TABLE VEHICLES (
    VEHICLE_NO    VARCHAR2(20) PRIMARY KEY,
    TYPE          VARCHAR2(20),        -- TWO_WHEELER | CAR | HEAVY
    OWNER_CONTACT VARCHAR2(30)
);

-- Table 2: Parking Slots
CREATE TABLE PARKING_SLOTS (
    SLOT_ID      NUMBER PRIMARY KEY,
    SLOT_NO      VARCHAR2(10) UNIQUE NOT NULL,
    FLOOR        VARCHAR2(10),
    STATUS       VARCHAR2(20),         -- AVAILABLE | OCCUPIED | UNDER_MAINTENANCE
    TYPE_ALLOWED VARCHAR2(20)          -- TWO_WHEELER | CAR | HEAVY
);

-- Table 3: Parking Tickets (one per parking session)
CREATE TABLE PARKING_TICKETS (
    TICKET_ID  NUMBER PRIMARY KEY,
    ENTRY_TIME TIMESTAMP DEFAULT SYSTIMESTAMP,
    EXIT_TIME  TIMESTAMP,
    VEHICLE_NO VARCHAR2(20) REFERENCES VEHICLES(VEHICLE_NO),
    SLOT_ID    NUMBER       REFERENCES PARKING_SLOTS(SLOT_ID)
);

-- Table 4: Payments (one per exit transaction)
CREATE TABLE PAYMENTS (
    PAYMENT_ID   NUMBER PRIMARY KEY,
    AMOUNT       NUMBER(10,2),
    PAYMENT_TIME TIMESTAMP DEFAULT SYSTIMESTAMP,
    PAYMENT_MODE VARCHAR2(10),          -- CASH | CARD | UPI
    TICKET_ID    NUMBER REFERENCES PARKING_TICKETS(TICKET_ID)
);

-- Sequences
CREATE SEQUENCE SEQ_TICKET_ID  START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE SEQ_SLOT_ID    START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE SEQ_PAYMENT_ID START WITH 1 INCREMENT BY 1;
```

### 2. Configure DB Credentials

Edit `src/com/parkingsystem/dao/DBConnection.java`:

```java
public static final String URL  = "jdbc:oracle:thin:@localhost:1521:xe";
public static final String USER = "SYSTEM";
public static final String PASS = "your_password_here";   // ← change this
```

### 3. Compile & Run

```bash
# Compile all source files
javac -cp .;lib/ojdbc8.jar -d out src/com/parkingsystem/model/*.java ^
      src/com/parkingsystem/dao/*.java src/com/parkingsystem/ui/*.java

# Run the application
java -cp out;lib/ojdbc8.jar com.parkingsystem.ui.ParkingUI
```

---

## 💡 Features

| Feature | Description |
|---------|-------------|
| **Vehicle Entry** | Upserts VEHICLES record, finds first available slot by type, marks it OCCUPIED, creates PARKING_TICKET |
| **Vehicle Exit** | Calculates charge via duration × rate, inserts PAYMENTS record, stamps EXIT_TIME, frees slot |
| **Slot Monitor** | Live colour-coded grid — click any 🔴 OCCUPIED slot to see vehicle, owner, entry time & estimated fee |
| **Manage Slots** | Add new slots or update floor/type via Oracle MERGE (upsert on SLOT_NO) |
| **Revenue Report** | Date-range report joining all 4 tables: vehicle, slot, ticket, payment |

### 💰 Parking Rates

| Vehicle Type  | Rate (INR) |
|---------------|-----------|
| Two Wheeler   | ₹10 / hour |
| Car           | ₹20 / hour |
| Heavy Vehicle | ₹40 / hour |

> Minimum charge = 1 hour regardless of actual duration.

---

## 🗃 ER Diagram Summary

```
VEHICLES ──(1)──── generates ────(N)── PARKING_TICKETS
                                              │
PARKING_SLOTS ──(1)── assigned to ──(N)──────┘
                                              │
                                        settled by
                                              │
                                          PAYMENTS
```

**Functional Dependencies (from schema):**

- `VEHICLES`: Vehicle_No → Type, Owner_contact
- `PARKING_SLOTS`: Slot_id → Slot_no, Floor, Status, Type_allowed
- `PARKING_TICKETS`: Ticket_ID → Entry_time, Exit_time, Vehicle_No, Slot_id
- `PAYMENTS`: Payment_ID → Amount, Payment_time, Payment_mode, Ticket_ID

---

## 📦 Class Diagram

```
ParkingUI
 ├── txtVehicleNum   : JTextField
 ├── txtOwnerContact : JTextField        ← maps to VEHICLES.OWNER_CONTACT
 ├── txtTicketId     : JTextField
 ├── cmbType         : JComboBox
 ├── btnEntry / btnExit / btnSlotMonitor / btnManageSlots / btnReports : JButton
 └── methods: main(), handleEntry(), handleExit(),
              displaySlotGrid(), manageSlotsWindow(), generateReportWindow()

ParkingDAO
 └── methods: registerVehicleEntry(vehicleNum, vehicleType, ownerContact)
              processVehicleExit(ticketId, paymentMode)
              getRealTimeSlotStatuses()
              getOccupiedSlotDetail(slotId)   ← supports click-to-view
              addOrUpdateSlot(slotNum, floor, type)
              generateRevenueReport(startDate, endDate)

DBConnection
 ├── URL  = "jdbc:oracle:thin:@localhost:1521:xe"
 ├── USER = "SYSTEM"
 ├── PASS = "password"
 └── getConnection() : Connection

Ticket   → ticketId, vehicleNum, vehicleType, slotId, entryTime, exitTime
Slot     → slotId, slotNumber, floor, slotType, status
Vehicles → vehicleNo, type, ownerContact
Payment  → paymentId, amount, paymentTime, paymentMode, ticketId
```

---

## 👥 Team

| Name | Role |
|------|------|
| shwetamjohn | GUI & Main Logic |

---
