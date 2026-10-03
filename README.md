# 🅿 Car Parking Management System

A Java Swing/AWT + JDBC + Oracle XE desktop application for managing a parking lot —  
vehicle entry/exit, slot monitoring, slot management, and revenue reports.

---

## 📐 Architecture

```
ParkingUI  ──uses──►  ParkingDAO  ──uses──►  DBConnection
                           │
                    creates/fetches
                           │
                     Ticket   Slot
```

| Layer | Class | Responsibility |
|-------|-------|----------------|
| UI    | `ParkingUI`    | Swing forms, dialogs, event handlers |
| DAO   | `ParkingDAO`   | All JDBC queries & transactions |
| DB    | `DBConnection` | Single-point Oracle connection factory |
| Model | `Ticket`, `Slot` | Plain Java objects (POJOs) |

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
            │   └── Ticket.java
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
- `ojdbc8.jar` (or `ojdbc11.jar`) on your classpath

### 1. Oracle Schema — run this DDL in SQL Developer / SQL*Plus

```sql
CREATE TABLE PARKING_SLOTS (
    SLOT_ID     NUMBER PRIMARY KEY,
    SLOT_NUMBER VARCHAR2(10) UNIQUE NOT NULL,
    FLOOR       VARCHAR2(10),
    SLOT_TYPE   VARCHAR2(20),   -- TWO_WHEELER | CAR | HEAVY
    STATUS      VARCHAR2(20)    -- AVAILABLE   | OCCUPIED
);

CREATE TABLE PARKING_TICKETS (
    TICKET_ID    NUMBER PRIMARY KEY,
    VEHICLE_NUM  VARCHAR2(20) NOT NULL,
    VEHICLE_TYPE VARCHAR2(20),
    SLOT_ID      NUMBER REFERENCES PARKING_SLOTS(SLOT_ID),
    ENTRY_TIME   TIMESTAMP DEFAULT SYSTIMESTAMP,
    EXIT_TIME    TIMESTAMP
);

CREATE SEQUENCE SEQ_TICKET_ID START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE SEQ_SLOT_ID   START WITH 1 INCREMENT BY 1;
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
# Compile
javac -cp .;lib/ojdbc8.jar -d out src/com/parkingsystem/**/*.java

# Run
java -cp out;lib/ojdbc8.jar com.parkingsystem.ui.ParkingUI
```

> Place `ojdbc8.jar` inside a `lib/` folder at the project root.

---

## 💡 Features

| Feature | Description |
|---------|-------------|
| **Vehicle Entry** | Finds first available slot by type, marks it occupied, issues a ticket |
| **Vehicle Exit** | Calculates hourly charge, records exit time, frees the slot |
| **Slot Monitor** | Live colour-coded grid (🟢 Available / 🔴 Occupied) |
| **Manage Slots** | Add new slots or update floor/type via Oracle MERGE (upsert) |
| **Revenue Report** | Date-range report with per-session charges and totals |

### 💰 Parking Rates

| Vehicle Type  | Rate (INR) |
|---------------|-----------|
| Two Wheeler   | ₹10 / hour |
| Car           | ₹20 / hour |
| Heavy Vehicle | ₹40 / hour |

---

## 📦 Class Diagram Summary

```
ParkingUI
 ├── txtVehicleNum : JTextField
 ├── txtTicketId   : JTextField
 ├── cmbType       : JComboBox
 ├── btnEntry / btnExit / btnSlotMonitor / btnManageSlots / btnReports : JButton
 └── methods: main(), handleEntry(), handleExit(),
              displaySlotGrid(), manageSlotsWindow(), generateReportWindow()

ParkingDAO
 └── methods: registerVehicleEntry(), processVehicleExit(),
              getRealTimeSlotStatuses(), addOrUpdateSlot(), generateRevenueReport()

DBConnection
 ├── URL  = "jdbc:oracle:thin:@localhost:1521:xe"
 ├── USER = "SYSTEM"
 ├── PASS = "password"
 └── getConnection() : Connection

Ticket  →  ticketId, vehicleNum, vehicleType, slotId, entryTime, exitTime
Slot    →  slotId, slotNumber, floor, slotType, status
```

---

## 👥 Team

| Name | Role |
|------|------|
| shwetamjohn | GUI & Main Logic |

---

## 📄 License

MIT
