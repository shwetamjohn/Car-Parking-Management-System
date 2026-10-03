package com.parkingsystem.model;

/**
 * Model class representing a parking slot in the parking lot.
 * Maps to the PARKING_SLOTS table in Oracle DB.
 */
public class Slot {

    private int slotId;
    private String slotNumber;
    private String floor;
    private String slotType;   // e.g., "TWO_WHEELER", "CAR", "HEAVY"
    private String status;     // "AVAILABLE" or "OCCUPIED"

    // ----- Constructors -----

    public Slot() {}

    public Slot(int slotId, String slotNumber, String floor, String slotType, String status) {
        this.slotId     = slotId;
        this.slotNumber = slotNumber;
        this.floor      = floor;
        this.slotType   = slotType;
        this.status     = status;
    }

    // ----- Getters -----

    public int getSlotId() {
        return slotId;
    }

    public String getSlotNumber() {
        return slotNumber;
    }

    public String getFloor() {
        return floor;
    }

    public String getSlotType() {
        return slotType;
    }

    public String getStatus() {
        return status;
    }

    // ----- Setters -----

    public void setSlotId(int slotId) {
        this.slotId = slotId;
    }

    public void setSlotNumber(String slotNumber) {
        this.slotNumber = slotNumber;
    }

    public void setFloor(String floor) {
        this.floor = floor;
    }

    public void setSlotType(String slotType) {
        this.slotType = slotType;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // ----- Utility -----

    @Override
    public String toString() {
        return "Slot{" +
                "slotId=" + slotId +
                ", slotNumber='" + slotNumber + '\'' +
                ", floor='" + floor + '\'' +
                ", slotType='" + slotType + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
