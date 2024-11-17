import java.sql.*;
import java.util.Scanner;

// Interface for common database operations
interface DatabaseOperations {
    void createTable();
    void add(Scanner scanner);
    void delete(Scanner scanner);
    void view(Scanner scanner);
}

// Abstract base class to manage the database connection
abstract class BaseEntity implements DatabaseOperations {
    protected static final String URL = "jdbc:mysql://localhost:3306/expense_tracker";
    protected static final String USER = "root";
    protected static final String PASSWORD = "21-Apr-06";
    protected static Connection conn;

    static {
        try {
            conn = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Connected to MySQL database.");
        } catch (SQLException e) {
            System.out.println("Connection failed: " + e.getMessage());
        }
    }

    protected void closeConnection() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            System.out.println("Error closing connection: " + e.getMessage());
        }
    }
}

// Account class implementing database operations specific to accounts
class Account extends BaseEntity {

    @Override
    public void createTable() {
        String createAccountsTable = "CREATE TABLE IF NOT EXISTS accounts ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "name VARCHAR(255) NOT NULL);";

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createAccountsTable);
            System.out.println("Accounts table created.");
        } catch (SQLException e) {
            System.out.println("Error creating accounts table: " + e.getMessage());
        }
    }

    @Override
    public void add(Scanner scanner) {
        System.out.print("Enter account name: ");
        String name = scanner.nextLine();

        String sql = "INSERT INTO accounts(name) VALUES(?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.executeUpdate();
            System.out.println("Account created successfully.");
        } catch (SQLException e) {
            System.out.println("Error creating account: " + e.getMessage());
        }
    }

    @Override
    public void delete(Scanner scanner) {
        System.out.println("Delete operation for Account not implemented.");
    }

    @Override
    public void view(Scanner scanner) {
        String sql = "SELECT * FROM accounts";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            System.out.println("\nAccounts:");
            while (rs.next()) {
                System.out.printf("%d - %s%n", rs.getInt("id"), rs.getString("name"));
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving accounts: " + e.getMessage());
        }
    }

    public int chooseAccount(Scanner scanner) {
        view(scanner);
        System.out.print("Enter account ID to select an account: ");
        return scanner.nextInt();
    }
}

// Expense class implementing database operations specific to expenses
class Expense extends BaseEntity {

    @Override
    public void createTable() {
        String createExpensesTable = "CREATE TABLE IF NOT EXISTS expenses ("
                + "id INT AUTO_INCREMENT PRIMARY KEY, "
                + "account_id INT, "
                + "amount DOUBLE NOT NULL, "
                + "description VARCHAR(255) NOT NULL, "
                + "date DATE NOT NULL, "
                + "FOREIGN KEY (account_id) REFERENCES accounts(id));";

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(createExpensesTable);
            System.out.println("Expenses table created.");
        } catch (SQLException e) {
            System.out.println("Error creating expenses table: " + e.getMessage());
        }
    }

    @Override
    public void add(Scanner scanner) {
        Account account = new Account();
        int accountId = account.chooseAccount(scanner);
        if (accountId == -1) {
            System.out.println("No account selected.");
            return;
        }

        System.out.print("Enter amount: ");
        double amount = scanner.nextDouble();
        scanner.nextLine();

        System.out.print("Enter date (yyyy-MM-dd): ");
        String date = scanner.nextLine();

        double totalForDate = getTotalExpenseForDate(accountId, date);
        if (totalForDate + amount > ExpenseTracker.DAILY_EXPENSE_LIMIT) {
            System.out.println("Overspending! You have exceeded the daily expense limit.");
            return;
        }

        System.out.print("Enter description: ");
        String description = scanner.nextLine();

        String sql = "INSERT INTO expenses(account_id, amount, description, date) VALUES(?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, accountId);
            pstmt.setDouble(2, amount);
            pstmt.setString(3, description);
            pstmt.setString(4, date);
            pstmt.executeUpdate();
            System.out.println("Expense added successfully.");
        } catch (SQLException e) {
            System.out.println("Error adding expense: " + e.getMessage());
        }
    }

    private double getTotalExpenseForDate(int accountId, String date) {
        String sql = "SELECT SUM(amount) AS total FROM expenses WHERE account_id = ? AND date = ?";
        double total = 0.0;

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, accountId);
            pstmt.setString(2, date);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                total = rs.getDouble("total");
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving total expenses for the date: " + e.getMessage());
        }
        return total;
    }

    @Override
    public void delete(Scanner scanner) {
        System.out.print("Enter date (yyyy-MM-dd) to delete expenses: ");
        String dateStr = scanner.nextLine();

        String sql = "DELETE FROM expenses WHERE date = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, dateStr);
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Expenses for the date deleted successfully.");
            } else {
                System.out.println("No expense found for the specified date.");
            }
        } catch (SQLException e) {
            System.out.println("Error deleting expenses: " + e.getMessage());
        }
    }

    @Override
    public void view(Scanner scanner) {
        Account account = new Account();
        int accountId = account.chooseAccount(scanner);
        if (accountId == -1) {
            System.out.println("No account selected.");
            return;
        }

        String sql = "SELECT * FROM expenses WHERE account_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, accountId);
            ResultSet rs = pstmt.executeQuery();

            System.out.println("\nID | Amount | Description | Date");
            System.out.println("--------------------------------------------");
            while (rs.next()) {
                System.out.printf("%d | %.2f | %s | %s%n",
                        rs.getInt("id"),
                        rs.getDouble("amount"),
                        rs.getString("description"),
                        rs.getDate("date").toString());
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving expenses: " + e.getMessage());
        }
    }
}

// Main application class
public class ExpenseTracker {
    public static double DAILY_EXPENSE_LIMIT;

    public static void main(String[] args) {
        Account account = new Account();
        Expense expense = new Expense();

        // Create tables
        account.createTable();
        expense.createTable();

        Scanner scanner = new Scanner(System.in);

        // Set daily expense limit
        setExpenseLimit(scanner);

        int choice;

        do {
            System.out.println("\nExpense Tracker");
            System.out.println("1. Create Account");
            System.out.println("2. Add Expense");
            System.out.println("3. View Expenses by Account");
            System.out.println("4. Delete Expense");
            System.out.println("5. Update Expense Limit");
            System.out.println("6. Exit");
            System.out.print("Choose an option: ");
            choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1 -> account.add(scanner);
                case 2 -> expense.add(scanner);
                case 3 -> expense.view(scanner);
                case 4 -> expense.delete(scanner);
                case 5 -> setExpenseLimit(scanner);
                case 6 -> System.out.println("Exiting...");
                default -> System.out.println("Invalid choice. Please try again.");
            }
        } while (choice != 6);

        scanner.close();
        expense.closeConnection();
    }

    private static void setExpenseLimit(Scanner scanner) {
        System.out.print("Enter expense limit: ");
        DAILY_EXPENSE_LIMIT = scanner.nextDouble();
        scanner.nextLine();
        System.out.println("Expense limit set to: " + DAILY_EXPENSE_LIMIT);
    }
}
