-- ========================================================
-- Car Parking Management System - Oracle Database Schema
-- Compatible with Oracle Database 23c Free / 21c / 19c
-- ========================================================

-- Drop existing tables and sequences if re-running
BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE Payments CASCADE CONSTRAINTS';
EXCEPTION WHEN OTHERS THEN NULL;
END;
/
BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE Parking_Tickets CASCADE CONSTRAINTS';
EXCEPTION WHEN OTHERS THEN NULL;
END;
/
BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE Vehicles CASCADE CONSTRAINTS';
EXCEPTION WHEN OTHERS THEN NULL;
END;
/
BEGIN
    EXECUTE IMMEDIATE 'DROP TABLE Parking_slots CASCADE CONSTRAINTS';
EXCEPTION WHEN OTHERS THEN NULL;
END;
/
BEGIN
    EXECUTE IMMEDIATE 'DROP SEQUENCE SEQ_SLOT_ID';
EXCEPTION WHEN OTHERS THEN NULL;
END;
/
BEGIN
    EXECUTE IMMEDIATE 'DROP SEQUENCE SEQ_TICKET_ID';
EXCEPTION WHEN OTHERS THEN NULL;
END;
/
BEGIN
    EXECUTE IMMEDIATE 'DROP SEQUENCE SEQ_PAYMENT_ID';
EXCEPTION WHEN OTHERS THEN NULL;
END;
/

-- --------------------------------------------------------
-- Sequences Reference
-- --------------------------------------------------------
CREATE SEQUENCE SEQ_SLOT_ID START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE SEQ_TICKET_ID START WITH 1 INCREMENT BY 1 NOCACHE;
CREATE SEQUENCE SEQ_PAYMENT_ID START WITH 1 INCREMENT BY 1 NOCACHE;

-- --------------------------------------------------------
-- Table: Parking_slots
-- --------------------------------------------------------
CREATE TABLE Parking_slots (
    Slot_id VARCHAR2(20) PRIMARY KEY,
    Slot_no VARCHAR2(10) NOT NULL UNIQUE,
    Floor NUMBER(3) NOT NULL,
    Status VARCHAR2(20) DEFAULT 'available' CHECK (Status IN ('available', 'reserved', 'blocked')),
    Type_allowed VARCHAR2(20)
);

-- --------------------------------------------------------
-- Table: Vehicles
-- --------------------------------------------------------
CREATE TABLE Vehicles (
    Vehicle_no VARCHAR2(20) PRIMARY KEY,
    Type VARCHAR2(20) NOT NULL,
    Owner_contact VARCHAR2(15)
);

-- --------------------------------------------------------
-- Table: Parking_Tickets
-- --------------------------------------------------------
CREATE TABLE Parking_Tickets (
    Ticket_ID VARCHAR2(20) PRIMARY KEY,
    Entry_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    Exit_time TIMESTAMP,
    Vehicle_no VARCHAR2(20) NOT NULL REFERENCES Vehicles(Vehicle_no) ON DELETE CASCADE,
    Slot_ID VARCHAR2(20) NOT NULL REFERENCES Parking_slots(Slot_id) ON DELETE CASCADE
);

-- --------------------------------------------------------
-- Table: Payments
-- --------------------------------------------------------
CREATE TABLE Payments (
    Payment_ID VARCHAR2(20) PRIMARY KEY,
    Amount NUMBER(10, 2) NOT NULL,
    Payment_Time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    Payment_mode VARCHAR2(20),
    Ticket_id VARCHAR2(20) NOT NULL REFERENCES Parking_Tickets(Ticket_ID) ON DELETE CASCADE
);

-- --------------------------------------------------------
-- Seed Initial Parking Slots (Optional Demonstration Data)
-- --------------------------------------------------------
INSERT INTO Parking_slots (Slot_id, Slot_no, Floor, Status, Type_allowed)
VALUES ('SLOT_' || SEQ_SLOT_ID.NEXTVAL, 'A-101', 1, 'available', 'CAR');

INSERT INTO Parking_slots (Slot_id, Slot_no, Floor, Status, Type_allowed)
VALUES ('SLOT_' || SEQ_SLOT_ID.NEXTVAL, 'A-102', 1, 'available', 'CAR');

INSERT INTO Parking_slots (Slot_id, Slot_no, Floor, Status, Type_allowed)
VALUES ('SLOT_' || SEQ_SLOT_ID.NEXTVAL, 'B-101', 1, 'available', 'TWO_WHEELER');

INSERT INTO Parking_slots (Slot_id, Slot_no, Floor, Status, Type_allowed)
VALUES ('SLOT_' || SEQ_SLOT_ID.NEXTVAL, 'B-102', 1, 'available', 'TWO_WHEELER');

INSERT INTO Parking_slots (Slot_id, Slot_no, Floor, Status, Type_allowed)
VALUES ('SLOT_' || SEQ_SLOT_ID.NEXTVAL, 'C-101', 2, 'available', 'HEAVY');

COMMIT;
