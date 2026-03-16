import java.util.HashMap;
import java.util.ArrayList;
import java.util.Queue;
import java.util.LinkedList;

public class BookMyStayApp {

    public static void main(String[] args) {

        System.out.println("==================================");
        System.out.println("Book My Stay - Hotel Booking App");
        System.out.println("Version 5.1");
        System.out.println("==================================");

        RoomInventory inventory = new RoomInventory();

        ArrayList<Room> rooms = new ArrayList<>();
        rooms.add(new SingleRoom());
        rooms.add(new DoubleRoom());
        rooms.add(new SuiteRoom());

        RoomSearchService searchService = new RoomSearchService(inventory, rooms);
        searchService.displayAvailableRooms();

        BookingRequestQueue bookingQueue = new BookingRequestQueue();

        bookingQueue.addRequest(new Reservation("Rahul", "Single Room"));
        bookingQueue.addRequest(new Reservation("Anita", "Double Room"));
        bookingQueue.addRequest(new Reservation("Vikram", "Suite Room"));

        bookingQueue.displayRequests();
    }
}

abstract class Room {

    String type;
    int beds;
    int size;
    double price;

    Room(String type, int beds, int size, double price) {
        this.type = type;
        this.beds = beds;
        this.size = size;
        this.price = price;
    }

    void display() {
        System.out.println("Room Type: " + type);
        System.out.println("Beds: " + beds);
        System.out.println("Size: " + size + " sq.ft");
        System.out.println("Price per night: ₹" + price);
    }
}

class SingleRoom extends Room {

    SingleRoom() {
        super("Single Room", 1, 200, 2500);
    }
}

class DoubleRoom extends Room {

    DoubleRoom() {
        super("Double Room", 2, 350, 4000);
    }
}

class SuiteRoom extends Room {

    SuiteRoom() {
        super("Suite Room", 3, 600, 7500);
    }
}

class RoomInventory {

    HashMap<String, Integer> inventory;

    RoomInventory() {
        inventory = new HashMap<>();
        inventory.put("Single Room", 5);
        inventory.put("Double Room", 3);
        inventory.put("Suite Room", 2);
    }

    int getAvailability(String roomType) {
        return inventory.getOrDefault(roomType, 0);
    }

    void updateAvailability(String roomType, int count) {
        inventory.put(roomType, count);
    }
}

class RoomSearchService {

    RoomInventory inventory;
    ArrayList<Room> rooms;

    RoomSearchService(RoomInventory inventory, ArrayList<Room> rooms) {
        this.inventory = inventory;
        this.rooms = rooms;
    }

    void displayAvailableRooms() {
        for (Room room : rooms) {
            int available = inventory.getAvailability(room.type);
            if (available > 0) {
                room.display();
                System.out.println("Available: " + available);
                System.out.println();
            }
        }
    }
}

class Reservation {

    String guestName;
    String roomType;

    Reservation(String guestName, String roomType) {
        this.guestName = guestName;
        this.roomType = roomType;
    }

    void display() {
        System.out.println("Guest: " + guestName + " requested " + roomType);
    }
}

class BookingRequestQueue {

    Queue<Reservation> queue;

    BookingRequestQueue() {
        queue = new LinkedList<>();
    }

    void addRequest(Reservation reservation) {
        queue.add(reservation);
    }

    void displayRequests() {
        System.out.println("Booking Requests in Queue:");
        for (Reservation r : queue) {
            r.display();
        }
    }
}