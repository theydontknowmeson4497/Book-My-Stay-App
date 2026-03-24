import java.util.*;

public class BookMyStayApp {

    public static void main(String[] args) {

        System.out.println("==================================");
        System.out.println("Book My Stay - Hotel Booking App");
        System.out.println("Version 11.0 - Thread Safety");
        System.out.println("==================================");

        // 1. Setup Core Services
        RoomInventory inventory = new RoomInventory();
        BookingHistory history = new BookingHistory();
        BookingReportService reportService = new BookingReportService(history);

        ArrayList<Room> rooms = new ArrayList<>();
        rooms.add(new SingleRoom());
        rooms.add(new DoubleRoom());
        rooms.add(new SuiteRoom());

        BookingService bookingService = new BookingService(inventory, history);

        // 2. Simulate Concurrent Booking Requests (Use Case 11)
        // We create several guests trying to book the same room type simultaneously
        List<Thread> guestThreads = new ArrayList<>();
        String targetRoom = "Suite Room"; // Only 2 available in inventory

        System.out.println("--- Simulating Concurrent Requests for " + targetRoom + " ---");

        String[] guests = {"Rahul", "Anita", "Vikram", "Priya", "Siddharth"};

        for (String guestName : guests) {
            Reservation request = new Reservation(guestName, targetRoom);

            Thread thread = new Thread(() -> {
                try {
                    // Validate and process inside the thread
                    BookingValidator.validate(request, inventory);
                    bookingService.processSingleRequest(request);
                } catch (InvalidBookingException e) {
                    System.out.println("Concurrent Error [" + guestName + "]: " + e.getMessage());
                }
            });
            guestThreads.add(thread);
        }

        // Start all threads simultaneously
        for (Thread t : guestThreads) t.start();

        // Wait for all threads to finish before generating the report
        for (Thread t : guestThreads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // 3. Final Reports to verify consistency
        System.out.println("\n==================================");
        System.out.println("ADMINISTRATIVE REPORTS (Final State)");
        System.out.println("==================================");
        reportService.generateBookingAuditLog();
        reportService.generateSummaryReport();
    }
}

// --- USE CASE 11: THREAD SAFE SERVICES ---

class BookingService {
    private RoomInventory inventory;
    private BookingHistory history;
    private int roomCounter = 1;

    BookingService(RoomInventory inventory, BookingHistory history) {
        this.inventory = inventory;
        this.history = history;
    }

    // 'synchronized' ensures only one thread can execute this block at a time
    // preventing race conditions during allocation
    public synchronized String processSingleRequest(Reservation request) {
        String roomType = request.roomType;

        // Double-check availability inside the synchronized block
        if (inventory.getAvailability(roomType) > 0) {
            String roomId = roomType.replace(" ", "").substring(0,2).toUpperCase() + roomCounter++;

            inventory.decrementAvailability(roomType);
            request.assignedRoomId = roomId;
            history.recordBooking(request);

            System.out.println("SUCCESS: " + request.guestName + " allocated " + roomId);
            return roomId;
        }
        return null;
    }
}

class RoomInventory {
    // Using a synchronized map or manual synchronization for thread safety
    private Map<String, Integer> inventory;

    RoomInventory() {
        inventory = new HashMap<>();
        inventory.put("Single Room", 5);
        inventory.put("Double Room", 3);
        inventory.put("Suite Room", 2); // Only 2 available
    }

    // Synchronized to ensure threads read the most recent value
    public synchronized int getAvailability(String roomType) {
        return inventory.getOrDefault(roomType, 0);
    }

    public synchronized Set<String> getAllRoomTypes() {
        return new HashSet<>(inventory.keySet());
    }

    public synchronized void decrementAvailability(String roomType) {
        int count = inventory.getOrDefault(roomType, 0);
        if (count > 0) {
            inventory.put(roomType, count - 1);
        }
    }

    public synchronized void incrementAvailability(String roomType) {
        inventory.put(roomType, inventory.getOrDefault(roomType, 0) + 1);
    }
}

// --- CORE MODELS & VALIDATION ---

abstract class Room {
    String type; int beds; int size; double price;
    Room(String type, int beds, int size, double price) {
        this.type = type; this.beds = beds; this.size = size; this.price = price;
    }
}

class SingleRoom extends Room { SingleRoom() { super("Single Room", 1, 200, 2500); } }
class DoubleRoom extends Room { DoubleRoom() { super("Double Room", 2, 350, 4000); } }
class SuiteRoom extends Room { SuiteRoom() { super("Suite Room", 3, 600, 7500); } }

class Reservation {
    String guestName;
    String roomType;
    String assignedRoomId;
    boolean isCancelled = false;

    Reservation(String guestName, String roomType) {
        this.guestName = guestName;
        this.roomType = roomType;
    }
}

class BookingHistory {
    // Vector or synchronized list could be used, or manual sync
    private List<Reservation> confirmedBookings = Collections.synchronizedList(new ArrayList<>());

    void recordBooking(Reservation reservation) {
        confirmedBookings.add(reservation);
    }

    public synchronized Reservation findActiveBookingByGuest(String name) {
        for (Reservation r : confirmedBookings) {
            if (r.guestName.equalsIgnoreCase(name) && !r.isCancelled) return r;
        }
        return null;
    }

    public List<Reservation> getHistory() {
        synchronized(confirmedBookings) {
            return new ArrayList<>(confirmedBookings);
        }
    }
}

class BookingValidator {
    // Static validation utility
    public static void validate(Reservation res, RoomInventory inventory) throws InvalidBookingException {
        if (res.guestName == null || res.guestName.trim().isEmpty())
            throw new InvalidBookingException("Guest name is empty.");
        if (!inventory.getAllRoomTypes().contains(res.roomType))
            throw new InvalidBookingException("Invalid Room Type: " + res.roomType);

        // Note: In multi-threading, we still check availability here,
        // but the BookingService will re-check inside the synchronized block.
        if (inventory.getAvailability(res.roomType) <= 0)
            throw new InvalidBookingException("Sold out: " + res.roomType);
    }
}

class InvalidBookingException extends Exception {
    public InvalidBookingException(String message) { super(message); }
}

// --- REMAINING UTILITIES ---

class BookingReportService {
    private BookingHistory history;
    BookingReportService(BookingHistory history) { this.history = history; }

    void generateBookingAuditLog() {
        List<Reservation> records = history.getHistory();
        for (Reservation r : records) {
            System.out.println("Confirmed: " + r.guestName + " | Room: " + r.assignedRoomId);
        }
    }

    void generateSummaryReport() {
        List<Reservation> records = history.getHistory();
        System.out.println("Total Successful Bookings: " + records.size());
    }
}

class RoomSearchService {
    RoomInventory inventory; ArrayList<Room> rooms;
    RoomSearchService(RoomInventory i, ArrayList<Room> r) { this.inventory = i; this.rooms = r; }
    void displayAvailableRooms() {
        for (Room r : rooms) System.out.println(r.type + " available: " + inventory.getAvailability(r.type));
        System.out.println();
    }
}

class BookingRequestQueue {
    Queue<Reservation> queue = new LinkedList<>();
    void addRequest(Reservation res) { queue.add(res); }
    Reservation getNextRequest() { return queue.poll(); }
    boolean hasRequests() { return !queue.isEmpty(); }
}