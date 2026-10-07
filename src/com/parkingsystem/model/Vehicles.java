package com.parkingsystem.model;

/**
 * Model class representing the VEHICLES table.
 * Vehicle_no is the primary key (registration number).
 * A vehicle can generate many parking tickets (1-to-N with PARKING_TICKETS).
 */
public class Vehicles {

    private String vehicleNo;      // PK  — e.g. "MH12AB1234"
    private String type;           // "TWO_WHEELER" | "CAR" | "HEAVY"
    private String ownerContact;   // owner phone or email

    // ----- Constructors -----

    public Vehicles() {}

    public Vehicles(String vehicleNo, String type, String ownerContact) {
        this.vehicleNo    = vehicleNo;
        this.type         = type;
        this.ownerContact = ownerContact;
    }

    // ----- Getters -----

    public String getVehicleNo()    { return vehicleNo; }
    public String getType()         { return type; }
    public String getOwnerContact() { return ownerContact; }

    // ----- Setters -----

    public void setVehicleNo(String vehicleNo)       { this.vehicleNo = vehicleNo; }
    public void setType(String type)                 { this.type = type; }
    public void setOwnerContact(String ownerContact) { this.ownerContact = ownerContact; }

    // ----- Utility -----

    @Override
    public String toString() {
        return "Vehicles{vehicleNo='" + vehicleNo + '\'' +
                ", type='" + type + '\'' +
                ", ownerContact='" + ownerContact + '\'' + '}';
    }
}
