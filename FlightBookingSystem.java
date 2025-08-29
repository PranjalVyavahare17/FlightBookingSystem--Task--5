import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class FlightBookingSystem extends JFrame {
    static class Flight {
        String code, source, destination, departTime, arriveTime;
        LocalDate date;
        double fare;
        int rows = 6, cols = 6;
        Map<String, Booking> seatMap = new LinkedHashMap<>();

        Flight(String code, String src, String dst, String dep, String arr, LocalDate date, double fare) {
            this.code = code;
            this.source = src;
            this.destination = dst;
            this.departTime = dep;
            this.arriveTime = arr;
            this.date = date;
            this.fare = fare;
        }

        int available() {
            return rows * cols - seatMap.size();
        }

        boolean isSeatAvailable(String seat) {
            return !seatMap.containsKey(seat);
        }
    }

    static class Booking {
        String id;
        Flight flight;
        String first, last, phone, seat;

        Booking(String id, Flight f, String first, String last, String phone, String seat) {
            this.id = id;
            this.flight = f;
            this.first = first;
            this.last = last;
            this.phone = phone;
            this.seat = seat;
        }

        String fullName() {
            return (first + " " + last).trim();
        }
    }

    java.util.List<Flight> allFlights = new ArrayList<>();
    java.util.List<Booking> allBookings = new ArrayList<>();
    CardLayout card = new CardLayout();
    JPanel root = new JPanel(card);
    JComboBox<String> cbSource, cbDestination;
    JTextField tfDate;
    DefaultTableModel flightsModel;
    JTable flightsTable;
    JTextField tfFirst, tfLast, tfPhone;
    JPanel seatGridPanel;
    JLabel lblSeatPicked;
    String selectedSeat = null;
    JTextArea summaryArea;
    Flight selectedFlight;
    JTable bookingsTable;
    DefaultTableModel bookingsModel;

    public FlightBookingSystem() {
        super("Flight Booking System");
        // Remove all custom UIManager colors and fonts
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(960, 640);
        setLocationRelativeTo(null);
        createDummyFlights();
        setJMenuBar(buildMenuBar());
        root.add(buildSearchPanel(), "search");
        root.add(buildPassengerPanel(), "passenger");
        root.add(buildSeatPanel(), "seats");
        root.add(buildSummaryPanel(), "summary");
        root.add(buildBookingsPanel(), "bookings");
        add(root);
        card.show(root, "search");
    }

    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();
        JMenu mApp = new JMenu("App");
        JMenuItem mAbout = new JMenuItem("About");
        mAbout.addActionListener(e -> JOptionPane.showMessageDialog(this, "Flight Booking System\nDemo."));
        JMenuItem mExit = new JMenuItem("Exit");
        mExit.addActionListener(e -> System.exit(0));
        mApp.add(mAbout);
        mApp.addSeparator();
        mApp.add(mExit);
        JMenu mView = new JMenu("View");
        JMenuItem mBookings = new JMenuItem("Booking History");
        mBookings.addActionListener(e -> {
            refreshBookingsTable();
            card.show(root, "bookings");
        });
        JMenuItem mSearch = new JMenuItem("Search Flights");
        mSearch.addActionListener(e -> card.show(root, "search"));
        mView.add(mSearch);
        mView.add(mBookings);
        bar.add(mApp);
        bar.add(mView);
        return bar;
    }

    // Utility method for bold black labels
    private JLabel createBoldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Color.BLACK);
        label.setFont(label.getFont().deriveFont(Font.BOLD));
        return label;
    }

    private JPanel buildSearchPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));
        JPanel form = new JPanel(new GridLayout(2, 4, 8, 8));
        String[] cities = { "BKK", "CNX", "DEL", "PNQ", "BOM", "BLR", "HYD", "GOI" };
        cbSource = new JComboBox<>(cities);
        cbDestination = new JComboBox<>(cities);
        tfDate = new JTextField(LocalDate.now().format(DateTimeFormatter.ISO_DATE));
        JButton btnSearch = new JButton("Search");
        btnSearch.addActionListener(e -> performSearch());
        form.add(createBoldLabel("From"));
        form.add(createBoldLabel("To"));
        form.add(createBoldLabel("Date (YYYY-MM-DD)"));
        form.add(new JLabel(" "));
        form.add(cbSource);
        form.add(cbDestination);
        form.add(tfDate);
        form.add(btnSearch);
        String[] cols = { "Flight", "From", "To", "Date", "Depart", "Arrive", "Fare", "Available" };
        flightsModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        flightsTable = new JTable(flightsModel);
        flightsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = new JScrollPane(flightsTable);
        JButton btnNext = new JButton("Select Flight & Continue");
        btnNext.addActionListener(e -> gotoPassenger());
        panel.add(form, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(btnNext, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildPassengerPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));
        JPanel form = new JPanel(new GridLayout(3, 2, 8, 8));
        tfFirst = new JTextField();
        tfLast = new JTextField();
        tfPhone = new JTextField();
        form.add(createBoldLabel("First Name"));
        form.add(tfFirst);
        form.add(createBoldLabel("Last Name"));
        form.add(tfLast);
        form.add(createBoldLabel("Phone"));
        form.add(tfPhone);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton back = new JButton("Back");
        back.addActionListener(e -> card.show(root, "search"));
        JButton next = new JButton("Continue to Seat Selection");
        next.addActionListener(e -> gotoSeatSelection());
        buttons.add(back);
        buttons.add(next);
        panel.add(form, BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildSeatPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));
        seatGridPanel = new JPanel(new GridLayout(6, 6, 6, 6));
        buildSeatGrid();
        lblSeatPicked = new JLabel("Selected Seat: -");
        JButton back = new JButton("Back");
        back.addActionListener(e -> card.show(root, "passenger"));
        JButton confirm = new JButton("Confirm Booking");
        confirm.addActionListener(e -> confirmBooking());
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(lblSeatPicked);
        bottom.add(back);
        bottom.add(confirm);
        panel.add(seatGridPanel, BorderLayout.CENTER);
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildSummaryPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));
        summaryArea = new JTextArea(14, 50);
        summaryArea.setEditable(false);
        JButton done = new JButton("Done");
        done.addActionListener(e -> {
            refreshFlightsTable(lastSearch());
            card.show(root, "search");
        });
        panel.add(new JScrollPane(summaryArea), BorderLayout.CENTER);
        panel.add(done, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildBookingsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));
        String[] cols = { "Booking ID", "Passenger", "Phone", "Flight", "Route", "Date", "Time", "Seat", "Fare" };
        bookingsModel = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        bookingsTable = new JTable(bookingsModel);
        JScrollPane scroll = new JScrollPane(bookingsTable);
        JButton cancel = new JButton("Cancel Selected Booking");
        cancel.addActionListener(e -> cancelSelectedBooking());
        JButton back = new JButton("Back");
        back.addActionListener(e -> card.show(root, "search"));
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bottom.add(back);
        bottom.add(cancel);
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    private void createDummyFlights() {
        String[] cities = { "BKK", "CNX", "DEL", "PNQ", "BOM", "BLR", "HYD", "GOI" };
        Random r = new Random(42);
        LocalDate today = LocalDate.now();
        int codeCounter = 101;
        // Always add a flight for BKK -> DEL for today
        allFlights.add(new Flight("QP" + codeCounter++, "BKK", "DEL",
                String.format("%02d:%02d", 8, 0),
                String.format("%02d:%02d", 10, 30),
                today, 3500));
        for (int d = 0; d < 5; d++) {
            LocalDate date = today.plusDays(d);
            for (int i = 0; i < 10; i++) {
                String src = cities[r.nextInt(cities.length)], dst;
                do {
                    dst = cities[r.nextInt(cities.length)];
                } while (dst.equals(src));
                int depMin = r.nextBoolean() ? 0 : 30;
                int arrMin = r.nextBoolean() ? 0 : 30;
                String dep = String.format("%02d:%02d", 6 + r.nextInt(14), depMin);
                String arr = String.format("%02d:%02d", 6 + r.nextInt(14), arrMin);
                double fare = 2000 + r.nextInt(8000);
                allFlights.add(new Flight("QP" + codeCounter++, src, dst, dep, arr, date, fare));
            }
        }
    }

    private void performSearch() {
        refreshFlightsTable(lastSearch());
    }

    private Map<String, String> lastSearch() {
        Map<String, String> q = new HashMap<>();
        q.put("src", Objects.toString(cbSource.getSelectedItem(), ""));
        q.put("dst", Objects.toString(cbDestination.getSelectedItem(), ""));
        q.put("date", tfDate.getText().trim());
        return q;
    }

    private void refreshFlightsTable(Map<String, String> query) {
        flightsModel.setRowCount(0);
        LocalDate d;
        try {
            d = LocalDate.parse(query.get("date"));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid date. Use YYYY-MM-DD.");
            return;
        }
        for (Flight f : allFlights)
            if (f.source.equals(query.get("src")) && f.destination.equals(query.get("dst")) && f.date.equals(d))
                flightsModel.addRow(new Object[] { f.code, f.source, f.destination, f.date.toString(), f.departTime,
                        f.arriveTime, String.format("%.2f", f.fare), f.available() });
        if (flightsModel.getRowCount() == 0)
            JOptionPane.showMessageDialog(this, "No flights found for selection.");
    }

    private void gotoPassenger() {
        int row = flightsTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a flight.");
            return;
        }
        String code = flightsModel.getValueAt(row, 0).toString();
        selectedFlight = allFlights.stream().filter(f -> f.code.equals(code)).findFirst().orElse(null);
        if (selectedFlight == null) {
            JOptionPane.showMessageDialog(this, "Error locating flight.");
            return;
        }
        if (selectedFlight.available() <= 0) {
            JOptionPane.showMessageDialog(this, "No seats available.");
            return;
        }
        tfFirst.setText("");
        tfLast.setText("");
        tfPhone.setText("");
        card.show(root, "passenger");
    }

    private void gotoSeatSelection() {
        if (tfFirst.getText().trim().isEmpty() || tfPhone.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Enter First Name and Phone.");
            return;
        }
        selectedSeat = null;
        buildSeatGrid();
        lblSeatPicked.setText("Selected Seat: -");
        card.show(root, "seats");
    }

    private void buildSeatGrid() {
        if (seatGridPanel == null)
            return; // Prevents NPE
        seatGridPanel.removeAll();
        seatGridPanel.setLayout(new GridLayout(6, 6, 6, 6));
        String rows = "ABCDEF";
        for (int r = 0; r < 6; r++)
            for (int c = 1; c <= 6; c++) {
                String seat = rows.charAt(r) + String.valueOf(c);
                JButton b = new JButton(seat);
                b.setFocusable(false);
                if (selectedFlight != null && !selectedFlight.isSeatAvailable(seat)) {
                    b.setEnabled(false);
                    b.setToolTipText("Booked");
                } else {
                    b.addActionListener(e -> {
                        selectedSeat = seat;
                        lblSeatPicked.setText("Selected Seat: " + selectedSeat);
                    });
                }
                seatGridPanel.add(b);
            }
        seatGridPanel.revalidate();
        seatGridPanel.repaint();
    }

    private void confirmBooking() {
        if (selectedFlight == null) {
            JOptionPane.showMessageDialog(this, "No flight selected.");
            return;
        }
        if (selectedSeat == null) {
            JOptionPane.showMessageDialog(this, "Select a seat.");
            return;
        }
        if (!selectedFlight.isSeatAvailable(selectedSeat)) {
            JOptionPane.showMessageDialog(this, "Seat booked.");
            buildSeatGrid();
            return;
        }
        String id = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Booking b = new Booking(id, selectedFlight, tfFirst.getText().trim(), tfLast.getText().trim(),
                tfPhone.getText().trim(), selectedSeat);
        selectedFlight.seatMap.put(selectedSeat, b);
        allBookings.add(b);
        showTicket(b);
        refreshBookingsTable();
        buildSeatGrid();
        selectedSeat = null;
    }

    private void showTicket(Booking b) {
        StringBuilder sb = new StringBuilder();
        sb.append("===== FLIGHT TICKET =====\n");
        sb.append("Booking ID : ").append(b.id).append("\n");
        sb.append("Name       : ").append(b.fullName()).append("\n");
        sb.append("Phone      : ").append(b.phone).append("\n");
        sb.append("--------------------------\n");
        sb.append("Flight     : ").append(b.flight.code).append("\n");
        sb.append("Route      : ").append(b.flight.source).append(" -> ").append(b.flight.destination).append("\n");
        sb.append("Date       : ").append(b.flight.date).append("\n");
        sb.append("Time       : ").append(b.flight.departTime).append(" - ").append(b.flight.arriveTime).append("\n");
        sb.append("Seat       : ").append(b.seat).append("\n");
        sb.append("Fare (INR) : ").append(String.format("%.2f", b.flight.fare)).append("\n");
        sb.append("==========================\n");
        summaryArea.setText(sb.toString());
        card.show(root, "summary");
    }

    private void refreshBookingsTable() {
        bookingsModel.setRowCount(0);
        for (Booking b : allBookings)
            bookingsModel.addRow(new Object[] { b.id, b.fullName(), b.phone, b.flight.code,
                    b.flight.source + "->" + b.flight.destination, b.flight.date.toString(), b.flight.departTime,
                    b.seat, String.format("%.2f", b.flight.fare) });
    }

    private void cancelSelectedBooking() {
        int row = bookingsTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a booking to cancel.");
            return;
        }
        String bookingId = bookingsModel.getValueAt(row, 0).toString();
        Booking target = null;
        for (Booking b : allBookings)
            if (b.id.equals(bookingId)) {
                target = b;
                break;
            }
        if (target == null) {
            JOptionPane.showMessageDialog(this, "Booking not found.");
            return;
        }
        int ok = JOptionPane.showConfirmDialog(this, "Cancel booking " + target.id + " for " + target.fullName() + "?",
                "Confirm", JOptionPane.YES_NO_OPTION);
        if (ok == JOptionPane.YES_OPTION) {
            target.flight.seatMap.remove(target.seat);
            allBookings.remove(target);
            refreshBookingsTable();
            JOptionPane.showMessageDialog(this, "Booking cancelled.");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            new FlightBookingSystem().setVisible(true);
        });
    }
}
