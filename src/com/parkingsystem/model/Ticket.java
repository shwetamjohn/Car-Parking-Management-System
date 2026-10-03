package com.parkingsystem.model;

/**
 * Model class representing a parking ticket issued on vehicle entry.
 * Maps to the PARKING_TICKETS table in Oracle DB.
 */
public class Ticket {

    private int    ticketId;
    private String vehicleNum;
    private String vehicleType;  // "TWO_WHEELER", "CAR", "HEAVY"
    private int    slotId;
    private String entryTime;    // Stored as formatted string from DB timestamp
    private String exitTime;     // null until vehicle exits

    // ----- Constructors -----

    public Ticket() {}

    public Ticket(int ticketId, String vehicleNum, String vehicleType,
                  int slotId, String entryTime, String exitTime) {
        this.ticketId    = ticketId;
        this.vehicleNum  = vehicleNum;
        this.vehicleType = vehicleType;
        this.slotId      = slotId;
        this.entryTime   = entryTime;
        this.exitTime    = exitTime;
    }

    // ----- Getters -----

    public int getTicketId() {
        return ticketId;
    }

    public String getVehicleNum() {
        return vehicleNum;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public int getSlotId() {
        return slotId;
    }

    public String getEntryTime() {
        return entryTime;
    }

    public String getExitTime() {
        return exitTime;
    }

    // ----- Setters -----

    public void setTicketId(int ticketId) {
        this.ticketId = ticketId;
    }

    public void setVehicleNum(String vehicleNum) {
        this.vehicleNum = vehicleNum;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public void setSlotId(int slotId) {
        this.slotId = slotId;
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
                "ticketId=" + ticketId +
                ", vehicleNum='" + vehicleNum + '\'' +
                ", vehicleType='" + vehicleType + '\'' +
                ", slotId=" + slotId +
                ", entryTime='" + entryTime + '\'' +
                ", exitTime='" + exitTime + '\'' +
                '}';
    }
}
