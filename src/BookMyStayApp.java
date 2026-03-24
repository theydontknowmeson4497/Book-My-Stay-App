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
        System.out.println("Version 9.0 - Error Handling");
        System.out.println("==================================");

        RoomInventory inventory = new RoomInventory();
        BookingHistory history = new BookingHistory();
        BookingReportService reportService = new BookingReportService(history);

        ArrayList<Room> rooms = new ArrayList<>();
        rooms.add(new SingleRoom());
        rooms.add(new DoubleRoom());
        rooms.add(new SuiteRoom());

        RoomSearchService searchService = new RoomSearchService(inventory, rooms);
        searchService.displayAvailableRooms();

        BookingRequestQueue bookingQueue = new BookingRequestQueue();

        // Adding a mix of valid and invalid requests to test validation
        bookingQueue.addRequest(new Reservation("Rahul", "Single Room"));
        bookingQueue.addRequest(new Reservation("InvalidUser", "Penthouse")); // Invalid Room Type
        bookingQueue.addRequest(new Reservation("", "Double Room"));        // Invalid Guest Name
        bookingQueue.addRequest(new Reservation("Anita", "Double Room"));

        BookingService bookingService = new BookingService(inventory, history);
        AddOnServiceManager serviceManager = new AddOnServiceManager();

        System.out.println("--- Processing Bookings with Validation ---");
        while (bookingQueue.hasRequests()) {
            Reservation request = bookingQueue.getNextRequest();

            try {
                // Use Case 9: Validate before processing
                BookingValidator.validate(request, inventory);

                String allocatedRoomId = bookingService.processSingleRequest(request);

                if (allocatedRoomId != null) {
                    if (request.guestName.equals("Rahul")) {
                        serviceManager.addServiceToReservation(allocatedRoomId, new AddOnService("WiFi", 500));
                    }
                    serviceManager.displayServicesForReservation(allocatedRoomId);
                }
            } catch (InvalidBookingException e) {
                // Graceful failure handling
                System.out.println("VALIDATION ERROR: " + e.getMessage());
                System.out.println();
            }
        }

        System.out.println("==================================");
        System.out.println("ADMINISTRATIVE REPORTS");
        System.out.println("==================================");
        reportService.generateBookingAuditLog();
        reportService.generateSummaryReport();
    }
}

// --- USE CASE 9: CUSTOM EXCEPTIONS & VALIDATION ---

class InvalidBookingException extends Exception {
    public InvalidBookingException(String message) {
        super(message);
    }
}

class BookingValidator {
    public static void validate(Reservation res, RoomInventory inventory) throws InvalidBookingException {
        // 1. Check for empty inputs
        if (res.guestName == null || res.guestName.trim().isEmpty()) {
            throw new InvalidBookingException("Guest name cannot be empty.");
        }

        // 2. Check if room type exists in system
        if (!inventory.getAllRoomTypes().contains(res.roomType)) {
            throw new InvalidBookingException("Room type '" + res.roomType + "' does not exist.");
        }

        // 3. Check for availability (Fail-fast)
        if (inventory.getAvailability(res.roomType) <= 0) {
            throw new InvalidBookingException("No availability for " + res.roomType);
        }
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

    Set<String> getAllRoomTypes() {
        return inventory.keySet();
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
    String assignedRoomId;

    Reservation(String guestName, String roomType) {
        this.guestName = guestName;
        this.roomType = roomType;
    }
}

class BookingHistory {
    private List<Reservation> confirmedBookings = new ArrayList<>();

    void recordBooking(Reservation reservation) {
        confirmedBookings.add(reservation);
    }

    List<Reservation> getHistory() {
        return new ArrayList<>(confirmedBookings);
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

        // Final state guard
        int available = inventory.getAvailability(roomType);
        if (available > 0) {
            String roomId = roomType.replace(" ", "").substring(0,2).toUpperCase() + roomCounter++;

            allocatedRooms.putIfAbsent(roomType, new HashSet<>());
            allocatedRooms.get(roomType).add(roomId);
            inventory.decrementAvailability(roomType);

            request.assignedRoomId = roomId;
            history.recordBooking(request);

            System.out.println("Reservation Confirmed: " + request.guestName + " -> " + roomId);
            return roomId;
        }
        return null;
    }
}

// --- ADD-ON SERVICES ---

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

// --- REPORTING SERVICE ---

class BookingReportService {
    private BookingHistory history;

    BookingReportService(BookingHistory history) {
        this.history = history;
    }

    void generateBookingAuditLog() {
        System.out.println("--- Chronological Audit Log ---");
        List<Reservation> records = history.getHistory();
        for (int i = 0; i < records.size(); i++) {
            Reservation r = records.get(i);
            System.out.println((i + 1) + ". Guest: " + r.guestName + " | Room: " + r.roomType + " | ID: " + r.assignedRoomId);
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
        System.out.println("Total Valid Bookings: " + records.size());
        counts.forEach((type, count) -> System.out.println(type + "s Booked: " + count));
        System.out.println();
    }
}