package com.calendar.app.models;

import java.time.LocalDate;
import java.time.LocalTime;

public class Event {
    private int id;
    private String title = "";
    private String description = "";
    private String location = "";
    private LocalDate eventDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String category = "";
    private int userId;
    private String userName = "";
    private String contactPhone = "";

    public Event() {}

    public Event(String title, String description, String location, LocalDate eventDate, 
                 LocalTime startTime, LocalTime endTime, String category, int userId) {
        this.title = title;
        this.description = description;
        this.location = location;
        this.eventDate = eventDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.category = category;
        this.userId = userId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public LocalDate getEventDate() { return eventDate; }
    public void setEventDate(LocalDate eventDate) { this.eventDate = eventDate; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
}