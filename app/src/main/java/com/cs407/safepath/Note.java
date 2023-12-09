package com.cs407.safepath;

public class Note {

    private int id;

    private String address;
    private String date;
    private String distance;

    private String destination;



    public Note(String address, String date, String distance, String destination) {
        this.address = address;
        this.date = date;
        this.distance = distance;
        this.destination = destination;
    }

    public String getAddress() {
        return address;
    }
    public String getDate() {
        return date;
    }
    public String getDistance() {
        return distance;
    }
    public String getDestination() {
        return destination;
    }

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }

}