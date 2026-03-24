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
        System.out.println("Version 8.0 - History & Reporting");
        System.out.println("==================================");

        // 1. Setup Core Services
        RoomInventory inventory = new RoomInventory();
        BookingHistory history = new BookingHistory();
        BookingReportService reportService = new BookingReportService(history);

        ArrayList<Room> rooms = new ArrayList<>();
        rooms.add(new SingleRoom());
        rooms.add(new DoubleRoom());
        rooms.add(new SuiteRoom());

        RoomSearchService searchService = new RoomSearchService(inventory, rooms);
        searchService.displayAvailableRooms();

        // 2. Setup Booking Requests
        BookingRequestQueue bookingQueue = new BookingRequestQueue();
        bookingQueue.addRequest(new Reservation("Rahul", "Single Room"));
        bookingQueue.addRequest(new Reservation("Anita", "Double Room"));
        bookingQueue.addRequest(new Reservation("Vikram", "Suite Room"));
        bookingQueue.addRequest(new Reservation("Priya", "Single Room"));

        // 3. Process Bookings with History Tracking
        BookingService bookingService = new BookingService(inventory, history);
        AddOnServiceManager serviceManager = new AddOnServiceManager();

        System.out.println("--- Processing Bookings ---");
        while (bookingQueue.hasRequests()) {
            Reservation request = bookingQueue.getNextRequest();
            String allocatedRoomId = bookingService.processSingleRequest(request);

            if (allocatedRoomId != null) {
                // Example Add-ons for Use Case 7
                if (request.guestName.equals("Rahul")) {
                    serviceManager.addServiceToReservation(allocatedRoomId, new AddOnService("WiFi", 500));
                }
                serviceManager.displayServicesForReservation(allocatedRoomId);
            }
        }

        // 4. Use Case 8: Generate Reports from History
        System.out.println("==================================");
        System.out.println("ADMINISTRATIVE REPORTS");
        System.out.println("==================================");
        reportService.generateBookingAuditLog();
        reportService.generateSummaryReport();
    }
}

// --- CORE MODELS ---

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
        System.out.println("Room Type: " + type + " | Price: ₹" + price);
    }
}

class SingleRoom extends Room { SingleRoom() { super("Single Room", 1, 200, 2500); } }
class DoubleRoom extends Room { DoubleRoom() { super("Double Room", 2, 350, 4000); } }
class SuiteRoom extends Room { SuiteRoom() { super("Suite Room", 3, 600, 7500); } }

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
        if (count > 0) inventory.put(roomType, count - 1);
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
        System.out.println("--- Current Room Availability ---");
        for (Room room : rooms) {
            int available = inventory.getAvailability(room.type);
            if (available > 0) {
                System.out.println(room.type + ": " + available + " units available");
            }
        }
        System.out.println();
    }
}

// --- BOOKING LOGIC & HISTORY ---

class Reservation {
    String guestName;
    String roomType;
    String assignedRoomId; // Added for historical tracking

    Reservation(String guestName, String roomType) {
        this.guestName = guestName;
        this.roomType = roomType;
    }
}

class BookingHistory {
    // List preserves insertion order for chronological tracking
    private List<Reservation> confirmedBookings = new ArrayList<>();

    void recordBooking(Reservation reservation) {
        confirmedBookings.add(reservation);
    }

    List<Reservation> getHistory() {
        return new ArrayList<>(confirmedBookings); // Return copy to prevent external modification
    }
}

class BookingRequestQueue {
    Queue<Reservation> queue = new LinkedList<>();
    void addRequest(Reservation res) { queue.add(res); }
    Reservation getNextRequest() { return queue.poll(); }
    boolean hasRequests() { return !queue.isEmpty(); }
}

class BookingService {
    RoomInventory inventory;
    BookingHistory history;
    HashMap<String, Set<String>> allocatedRooms;
    int roomCounter;

    BookingService(RoomInventory inventory, BookingHistory history) {
        this.inventory = inventory;
        this.history = history;
        this.allocatedRooms = new HashMap<>();
        this.roomCounter = 1;
    }

    String processSingleRequest(Reservation request) {
        String roomType = request.roomType;
        int available = inventory.getAvailability(roomType);

        if (available > 0) {
            String roomId = roomType.replace(" ", "").substring(0,2).toUpperCase() + roomCounter++;

            allocatedRooms.putIfAbsent(roomType, new HashSet<>());
            allocatedRooms.get(roomType).add(roomId);
            inventory.decrementAvailability(roomType);

            // Set historical data and save to history
            request.assignedRoomId = roomId;
            history.recordBooking(request);

            System.out.println("Reservation Confirmed: " + request.guestName + " -> " + roomId);
            return roomId;
        } else {
            System.out.println("Reservation Failed: " + request.guestName + " (Sold Out)");
            return null;
        }
    }
}

// --- USE CASE 7: ADD-ON SERVICES ---

class AddOnService {
    String serviceName;
    double cost;
    AddOnService(String serviceName, double cost) {
        this.serviceName = serviceName;
        this.cost = cost;
    }
    @Override
    public String toString() { return serviceName + " (₹" + cost + ")"; }
}

class AddOnServiceManager {
    private Map<String, List<AddOnService>> reservationServices = new HashMap<>();

    void addServiceToReservation(String roomId, AddOnService service) {
        reservationServices.putIfAbsent(roomId, new ArrayList<>());
        reservationServices.get(roomId).add(service);
    }

    void displayServicesForReservation(String roomId) {
        List<AddOnService> services = reservationServices.get(roomId);
        if (services != null && !services.isEmpty()) {
            System.out.print("   Services for " + roomId + ": ");
            for (AddOnService s : services) System.out.print(s + " ");
            System.out.println();
        }
    }
}

// --- USE CASE 8: REPORTING SERVICE ---

class BookingReportService {
    private BookingHistory history;

    BookingReportService(BookingHistory history) {
        this.history = history;
    }

    void generateBookingAuditLog() {
        System.out.println("--- Chronological Audit Log ---");
        List<Reservation> records = history.getHistory();
        if (records.isEmpty()) {
            System.out.println("No records found.");
        } else {
            for (int i = 0; i < records.size(); i++) {
                Reservation r = records.get(i);
                System.out.println((i + 1) + ". Guest: " + r.guestName + " | Room: " + r.roomType + " | ID: " + r.assignedRoomId);
            }
        }
        System.out.println();
    }

    void generateSummaryReport() {
        List<Reservation> records = history.getHistory();
        Map<String, Integer> counts = new HashMap<>();

        for (Reservation r : records) {
            counts.put(r.roomType, counts.getOrDefault(r.roomType, 0) + 1);
        }

        System.out.println("--- Occupancy Summary ---");
        System.out.println("Total Bookings Processed: " + records.size());
        counts.forEach((type, count) -> System.out.println(type + "s Booked: " + count));
        System.out.println();
    }
}