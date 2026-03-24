import java.util.*;
import java.io.*;

public class BookMyStayApp {

    public static void main(String[] args) {

        System.out.println("==================================");
        System.out.println("Book My Stay - Hotel Booking App");
        System.out.println("Version 12.0 - Persistence & Recovery");
        System.out.println("==================================");

        PersistenceService persistenceService = new PersistenceService();

        // 1. Attempt System Recovery (Load from File)
        System.out.println("--- System Startup: Checking for persisted state ---");
        SystemState state = persistenceService.loadState();

        RoomInventory inventory;
        BookingHistory history;

        if (state != null) {
            inventory = state.inventory;
            history = state.history;
            System.out.println("RECOVERY SUCCESSFUL: Restored " + history.getHistory().size() + " past bookings.");
        } else {
            inventory = new RoomInventory();
            history = new BookingHistory();
            System.out.println("INITIAL STARTUP: No previous state found. Initializing fresh inventory.");
        }

        BookingService bookingService = new BookingService(inventory, history);
        BookingReportService reportService = new BookingReportService(history);

        // 2. Perform a new booking to change the state
        System.out.println("\n--- Processing New Booking ---");
        Reservation newRequest = new Reservation("Siddharth", "Single Room");
        try {
            BookingValidator.validate(newRequest, inventory);
            bookingService.processSingleRequest(newRequest);
        } catch (InvalidBookingException e) {
            System.out.println("Error: " + e.getMessage());
        }

        // 3. Save State before Shutdown
        System.out.println("\n--- System Shutdown: Persisting state ---");
        persistenceService.saveState(new SystemState(inventory, history));

        // 4. Final Audit
        System.out.println("==================================");
        reportService.generateSummaryReport();
        System.out.println("System state saved to 'system_state.ser'. Run the app again to see recovery in action!");
    }
}

// --- USE CASE 12: PERSISTENCE & SERIALIZATION ---

// Wrapper class to hold the entire system state for serialization
class SystemState implements Serializable {
    private static final long serialVersionUID = 1L;
    RoomInventory inventory;
    BookingHistory history;

    SystemState(RoomInventory inventory, BookingHistory history) {
        this.inventory = inventory;
        this.history = history;
    }
}

class PersistenceService {
    private final String FILE_NAME = "system_state.ser";

    public void saveState(SystemState state) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_NAME))) {
            oos.writeObject(state);
            System.out.println("SUCCESS: System state serialized to disk.");
        } catch (IOException e) {
            System.err.println("FAILED: Could not persist state. " + e.getMessage());
        }
    }

    public SystemState loadState() {
        File file = new File(FILE_NAME);
        if (!file.exists()) return null;

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(FILE_NAME))) {
            return (SystemState) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("RECOVERY FAILED: Persistence file corrupted or incompatible.");
            return null;
        }
    }
}

// --- UPDATED CORE MODELS (Implementing Serializable) ---

class RoomInventory implements Serializable {
    private static final long serialVersionUID = 1L;
    private Map<String, Integer> inventory;

    RoomInventory() {
        inventory = new HashMap<>();
        inventory.put("Single Room", 5);
        inventory.put("Double Room", 3);
        inventory.put("Suite Room", 2);
    }

    public synchronized int getAvailability(String roomType) {
        return inventory.getOrDefault(roomType, 0);
    }

    public synchronized Set<String> getAllRoomTypes() {
        return new HashSet<>(inventory.keySet());
    }

    public synchronized void decrementAvailability(String roomType) {
        int count = inventory.getOrDefault(roomType, 0);
        if (count > 0) inventory.put(roomType, count - 1);
    }
}

class Reservation implements Serializable {
    private static final long serialVersionUID = 1L;
    String guestName;
    String roomType;
    String assignedRoomId;

    Reservation(String guestName, String roomType) {
        this.guestName = guestName;
        this.roomType = roomType;
    }
}

class BookingHistory implements Serializable {
    private static final long serialVersionUID = 1L;
    private List<Reservation> confirmedBookings = Collections.synchronizedList(new ArrayList<>());

    void recordBooking(Reservation reservation) {
        confirmedBookings.add(reservation);
    }

    List<Reservation> getHistory() {
        synchronized(confirmedBookings) {
            return new ArrayList<>(confirmedBookings);
        }
    }
}

// --- BUSINESS LOGIC & VALIDATION ---

class BookingService {
    private RoomInventory inventory;
    private BookingHistory history;
    private static int roomCounter = 101; // Static to avoid duplicate IDs on small restarts

    BookingService(RoomInventory inventory, BookingHistory history) {
        this.inventory = inventory;
        this.history = history;
    }

    public synchronized String processSingleRequest(Reservation request) {
        if (inventory.getAvailability(request.roomType) > 0) {
            String roomId = request.roomType.substring(0,2).toUpperCase() + (roomCounter++);
            inventory.decrementAvailability(request.roomType);
            request.assignedRoomId = roomId;
            history.recordBooking(request);
            System.out.println("BOOKED: " + request.guestName + " -> " + roomId);
            return roomId;
        }
        return null;
    }
}

class BookingValidator {
    public static void validate(Reservation res, RoomInventory inventory) throws InvalidBookingException {
        if (res.guestName == null || res.guestName.isEmpty()) throw new InvalidBookingException("Invalid Guest.");
        if (inventory.getAvailability(res.roomType) <= 0) throw new InvalidBookingException("No Rooms Available.");
    }
}

class InvalidBookingException extends Exception {
    public InvalidBookingException(String message) { super(message); }
}

class BookingReportService {
    private BookingHistory history;
    BookingReportService(BookingHistory history) { this.history = history; }

    void generateSummaryReport() {
        List<Reservation> records = history.getHistory();
        System.out.println("--- System Status Report ---");
        System.out.println("Total Persistent Records Found: " + records.size());
        for (Reservation r : records) {
            System.out.println(" - " + r.guestName + " (" + r.assignedRoomId + ")");
        }
    }
}

// --- DUMMY ROOM TYPES ---
abstract class Room { String type; Room(String t) { this.type = t; } }
class SingleRoom extends Room { SingleRoom() { super("Single Room"); } }
class DoubleRoom extends Room { DoubleRoom() { super("Double Room"); } }
class SuiteRoom extends Room { SuiteRoom() { super("Suite Room"); } }