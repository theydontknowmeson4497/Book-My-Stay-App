import java.util.HashMap;
import java.util.ArrayList;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Stack;

public class BookMyStayApp {

    public static void main(String[] args) {

        System.out.println("==================================");
        System.out.println("Book My Stay - Hotel Booking App");
        System.out.println("Version 10.0 - Cancellation & Rollback");
        System.out.println("==================================");

        // 1. Setup Core Services
        RoomInventory inventory = new RoomInventory();
        BookingHistory history = new BookingHistory();
        BookingReportService reportService = new BookingReportService(history);

        ArrayList<Room> rooms = new ArrayList<>();
        rooms.add(new SingleRoom());
        rooms.add(new DoubleRoom());
        rooms.add(new SuiteRoom());

        // 2. Setup Booking & Cancellation Services
        BookingService bookingService = new BookingService(inventory, history);
        CancellationService cancellationService = new CancellationService(inventory, history);
        AddOnServiceManager serviceManager = new AddOnServiceManager();

        // 3. Process Initial Bookings
        BookingRequestQueue bookingQueue = new BookingRequestQueue();
        bookingQueue.addRequest(new Reservation("Rahul", "Single Room"));
        bookingQueue.addRequest(new Reservation("Anita", "Double Room"));

        System.out.println("--- Processing Initial Bookings ---");
        while (bookingQueue.hasRequests()) {
            Reservation request = bookingQueue.getNextRequest();
            try {
                BookingValidator.validate(request, inventory);
                bookingService.processSingleRequest(request);
            } catch (InvalidBookingException e) {
                System.out.println("Validation Error: " + e.getMessage());
            }
        }
        System.out.println();

        // 4. Use Case 10: Perform Cancellation (Rollback)
        System.out.println("--- Initiating Cancellation ---");
        // Attempting to cancel Rahul's booking
        cancellationService.cancelBooking("Rahul");

        // Attempting to cancel a non-existent booking (Validation Check)
        cancellationService.cancelBooking("Vikram");
        System.out.println();

        // 5. Final Reports
        System.out.println("==================================");
        System.out.println("ADMINISTRATIVE REPORTS");
        System.out.println("==================================");
        reportService.generateBookingAuditLog();
        reportService.generateSummaryReport();
    }
}

// --- USE CASE 10: CANCELLATION & ROLLBACK ---

class CancellationService {
    private RoomInventory inventory;
    private BookingHistory history;
    private Stack<String> releasedRoomIds; // LIFO Rollback Logic

    CancellationService(RoomInventory inventory, BookingHistory history) {
        this.inventory = inventory;
        this.history = history;
        this.releasedRoomIds = new Stack<>();
    }

    void cancelBooking(String guestName) {
        Reservation reservation = history.findActiveBookingByGuest(guestName);

        if (reservation != null && !reservation.isCancelled) {
            // 1. Mark as cancelled in history
            reservation.isCancelled = true;

            // 2. Record Room ID for rollback/recycling
            releasedRoomIds.push(reservation.assignedRoomId);

            // 3. Restore Inventory (State Reversal)
            inventory.incrementAvailability(reservation.roomType);

            System.out.println("Cancellation Successful for: " + guestName);
            System.out.println("Rollback Performed: Room " + reservation.assignedRoomId + " returned to inventory.");
        } else {
            System.out.println("Cancellation Failed: No active booking found for guest '" + guestName + "'.");
        }
    }
}

// --- CORE MODELS & INVENTORY ---

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

    // New for Use Case 10: Restoration Logic
    void incrementAvailability(String roomType) {
        inventory.put(roomType, inventory.getOrDefault(roomType, 0) + 1);
    }
}

abstract class Room {
    String type;
    int beds;
    int size;
    double price;

    Room(String type, int beds, int size, double price) {
        this.type = type; this.beds = beds; this.size = size; this.price = price;
    }
    void display() { System.out.println("Room Type: " + type + " | Price: ₹" + price); }
}

class SingleRoom extends Room { SingleRoom() { super("Single Room", 1, 200, 2500); } }
class DoubleRoom extends Room { DoubleRoom() { super("Double Room", 2, 350, 4000); } }
class SuiteRoom extends Room { SuiteRoom() { super("Suite Room", 3, 600, 7500); } }

// --- BOOKING LOGIC & VALIDATION ---

class Reservation {
    String guestName;
    String roomType;
    String assignedRoomId;
    boolean isCancelled = false; // New state for tracking

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

    Reservation findActiveBookingByGuest(String name) {
        for (Reservation r : confirmedBookings) {
            if (r.guestName.equalsIgnoreCase(name) && !r.isCancelled) return r;
        }
        return null;
    }

    List<Reservation> getHistory() {
        return new ArrayList<>(confirmedBookings);
    }
}

class BookingValidator {
    public static void validate(Reservation res, RoomInventory inventory) throws InvalidBookingException {
        if (res.guestName == null || res.guestName.trim().isEmpty())
            throw new InvalidBookingException("Guest name empty.");
        if (!inventory.getAllRoomTypes().contains(res.roomType))
            throw new InvalidBookingException("Invalid Room Type.");
        if (inventory.getAvailability(res.roomType) <= 0)
            throw new InvalidBookingException("Sold out.");
    }
}

class InvalidBookingException extends Exception {
    public InvalidBookingException(String message) { super(message); }
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
    int roomCounter = 1;

    BookingService(RoomInventory inventory, BookingHistory history) {
        this.inventory = inventory;
        this.history = history;
    }

    String processSingleRequest(Reservation request) {
        String roomId = request.roomType.replace(" ", "").substring(0,2).toUpperCase() + roomCounter++;
        inventory.decrementAvailability(request.roomType);
        request.assignedRoomId = roomId;
        history.recordBooking(request);
        System.out.println("Reservation Confirmed: " + request.guestName + " -> " + roomId);
        return roomId;
    }
}

// --- ADD-ONS & REPORTING ---

class AddOnServiceManager {
    private Map<String, List<AddOnService>> reservationServices = new HashMap<>();
    void addServiceToReservation(String roomId, AddOnService service) {
        reservationServices.putIfAbsent(roomId, new ArrayList<>());
        reservationServices.get(roomId).add(service);
    }
    void displayServicesForReservation(String roomId) {
        List<AddOnService> services = reservationServices.get(roomId);
        if (services != null) {
            System.out.print("   Services: ");
            for (AddOnService s : services) System.out.print(s + " ");
            System.out.println();
        }
    }
}

class AddOnService {
    String name; double cost;
    AddOnService(String n, double c) { this.name = n; this.cost = c; }
    @Override public String toString() { return name + " (₹" + cost + ")"; }
}

class BookingReportService {
    private BookingHistory history;
    BookingReportService(BookingHistory history) { this.history = history; }

    void generateBookingAuditLog() {
        System.out.println("--- Chronological Audit Log ---");
        List<Reservation> records = history.getHistory();
        for (Reservation r : records) {
            String status = r.isCancelled ? "[CANCELLED]" : "[ACTIVE]";
            System.out.println(status + " Guest: " + r.guestName + " | Room: " + r.assignedRoomId);
        }
    }

    void generateSummaryReport() {
        List<Reservation> records = history.getHistory();
        long activeCount = records.stream().filter(r -> !r.isCancelled).count();
        System.out.println("\n--- Summary Report ---");
        System.out.println("Total Records: " + records.size());
        System.out.println("Active Bookings: " + activeCount);
        System.out.println("Cancellations: " + (records.size() - activeCount));
    }
}

class RoomSearchService {
    RoomInventory inventory; ArrayList<Room> rooms;
    RoomSearchService(RoomInventory i, ArrayList<Room> r) { this.inventory = i; this.rooms = r; }
    void displayAvailableRooms() {
        System.out.println("--- Current Availability ---");
        for (Room r : rooms) System.out.println(r.type + ": " + inventory.getAvailability(r.type));
        System.out.println();
    }
}