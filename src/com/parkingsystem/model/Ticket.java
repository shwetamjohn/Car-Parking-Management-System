package com.parkingsystem.model;

/**
 * Model class representing a parking ticket issued on vehicle entry.
 * Maps to the Parking_Tickets table in Oracle DB.
 */
public class Ticket {

    private String ticketId;
    private String vehicleNum;
    private String vehicleType;  // "TWO_WHEELER", "CAR", "HEAVY"
    private String slotId;
    private String slotNo;
    private String entryTime;    // Stored as formatted string from DB timestamp
    private String exitTime;     // null until vehicle exits

    // ----- Constructors -----

    public Ticket() {}

    public Ticket(String ticketId, String vehicleNum, String vehicleType,
                  String slotId, String slotNo, String entryTime, String exitTime) {
        this.ticketId    = ticketId;
        this.vehicleNum  = vehicleNum;
        this.vehicleType = vehicleType;
        this.slotId      = slotId;
        this.slotNo      = slotNo;
        this.entryTime   = entryTime;
        this.exitTime    = exitTime;
    }

    // ----- Getters -----

    public String getTicketId() {
        return ticketId;
    }

    public String getVehicleNum() {
        return vehicleNum;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public String getSlotId() {
        return slotId;
    }

    public String getSlotNo() {
        return slotNo;
    }

    public String getEntryTime() {
        return entryTime;
    }

    public String getExitTime() {
        return exitTime;
    }

    // ----- Setters -----

    public void setTicketId(String ticketId) {
        this.ticketId = ticketId;
    }

    public void setVehicleNum(String vehicleNum) {
        this.vehicleNum = vehicleNum;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public void setSlotId(String slotId) {
        this.slotId = slotId;
    }

    public void setSlotNo(String slotNo) {
        this.slotNo = slotNo;
    }

    public void setEntryTime(String entryTime) {
        this.entryTime = entryTime;
    }

    public void setExitTime(String exitTime) {
        this.exitTime = exitTime;
    }

    // ----- Utility -----

    @Override
    public String toString() {
        return "Ticket{" +
                "ticketId='" + ticketId + '\'' +
                ", vehicleNum='" + vehicleNum + '\'' +
                ", vehicleType='" + vehicleType + '\'' +
                ", slotId='" + slotId + '\'' +
                ", slotNo='" + slotNo + '\'' +
                ", entryTime='" + entryTime + '\'' +
                ", exitTime='" + exitTime + '\'' +
                '}';
    }
}

