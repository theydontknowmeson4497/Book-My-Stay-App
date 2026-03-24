import java.util.HashMap;
import java.util.ArrayList;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class BookMyStayApp {

    public static void main(String[] args) {

        System.out.println("==================================");
        System.out.println("Book My Stay - Hotel Booking App");
        System.out.println("Version 7.0 - Add-On Services");
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
        bookingQueue.addRequest(new Reservation("Priya", "Single Room"));

        BookingService bookingService = new BookingService(inventory);
        AddOnServiceManager serviceManager = new AddOnServiceManager();

        // Processing requests and handling Add-On Services (Use Case 7)
        while (bookingQueue.hasRequests()) {
            Reservation request = bookingQueue.getNextRequest();
            String allocatedRoomId = bookingService.processSingleRequest(request);

            // If booking was successful, simulate guest selecting add-ons
            if (allocatedRoomId != null) {
                if (request.guestName.equals("Rahul")) {
                    serviceManager.addServiceToReservation(allocatedRoomId, new AddOnService("WiFi", 500));
                    serviceManager.addServiceToReservation(allocatedRoomId, new AddOnService("Breakfast", 800));
                } else if (request.guestName.equals("Anita")) {
                    serviceManager.addServiceToReservation(allocatedRoomId, new AddOnService("Late Checkout", 1200));
                }

                // Displaying the services and total cost for the guest
                serviceManager.displayServicesForReservation(allocatedRoomId);
            }
        }
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

    void decrementAvailability(String roomType) {
        int count = inventory.getOrDefault(roomType, 0);
        if (count > 0) {
            inventory.put(roomType, count - 1);
        }
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
}

class BookingRequestQueue {

    Queue<Reservation> queue;

    BookingRequestQueue() {
        queue = new LinkedList<>();
    }

    void addRequest(Reservation reservation) {
        queue.add(reservation);
    }

    Reservation getNextRequest() {
        return queue.poll();
    }

    boolean hasRequests() {
        return !queue.isEmpty();
    }
}

class BookingService {

    RoomInventory inventory;
    HashMap<String, Set<String>> allocatedRooms;
    int roomCounter;

    BookingService(RoomInventory inventory) {
        this.inventory = inventory;
        this.allocatedRooms = new HashMap<>();
        this.roomCounter = 1;
    }

    // Refactored to process one request and return the ID for service mapping
    String processSingleRequest(Reservation request) {
        String roomType = request.roomType;
        int available = inventory.getAvailability(roomType);

        if (available > 0) {
            String roomId = roomType.replace(" ", "").substring(0,2).toUpperCase() + roomCounter;
            roomCounter++;

            allocatedRooms.putIfAbsent(roomType, new HashSet<>());
            Set<String> roomSet = allocatedRooms.get(roomType);

            if (!roomSet.contains(roomId)) {
                roomSet.add(roomId);
                inventory.decrementAvailability(roomType);

                System.out.println("Reservation Confirmed for " + request.guestName);
                System.out.println("Room Type: " + roomType);
                System.out.println("Allocated Room ID: " + roomId);
                return roomId;
            }
        } else {
            System.out.println("Reservation Failed for " + request.guestName + " (No rooms available for " + roomType + ")");
            System.out.println();
        }
        return null;
    }
}

// --- NEW CLASSES FOR USE CASE 7 ---

class AddOnService {
    String serviceName;
    double cost;

    AddOnService(String serviceName, double cost) {
        this.serviceName = serviceName;
        this.cost = cost;
    }

    @Override
    public String toString() {
        return serviceName + " (₹" + cost + ")";
    }
}

class AddOnServiceManager {
    // Map<String, List<AddOnService>> maps Reservation ID to multiple services
    private Map<String, List<AddOnService>> reservationServices;

    AddOnServiceManager() {
        this.reservationServices = new HashMap<>();
    }

    void addServiceToReservation(String roomId, AddOnService service) {
        reservationServices.putIfAbsent(roomId, new ArrayList<>());
        reservationServices.get(roomId).add(service);
    }

    void displayServicesForReservation(String roomId) {
        List<AddOnService> services = reservationServices.get(roomId);
        if (services != null && !services.isEmpty()) {
            System.out.println("Add-on Services for Room " + roomId + ":");
            double totalExtra = 0;
            for (AddOnService s : services) {
                System.out.println(" - " + s);
                totalExtra += s.cost;
            }
            System.out.println("Total Additional Cost for Services: ₹" + totalExtra);
            System.out.println();
        }
    }
}