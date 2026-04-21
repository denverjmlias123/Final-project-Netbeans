package groupprojectexe;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.RowFilter;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

public class DashboardInventory extends javax.swing.JFrame {
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(DashboardInventory.class.getName());
    
    // User Session Data
    private final int userId;
    private final String userName;
    private String userRole;
    
    // Timer for Live Updates
    private Timer refreshTimer;

    public DashboardInventory(int userId, String username) {
        initComponents();
        this.userId = userId;
        this.userName = username;
        
        // NEW: Fetch role and setup button visibility
        setupUserRoleAndAccess();
        
        // Update title with dynamic role
        lblTitle.setText("User: " + userName + " | Role: " + userRole);
        
        setupTableEditors();
        setupListeners();
        
        // Setup Live Update Timer (5 seconds)
        refreshTimer = new Timer(5000, e -> refreshData());
        refreshTimer.start();
        
        refreshData();
    }
    // NEW: Method to check role and configure navigation button
    private void setupUserRoleAndAccess() {
        userRole = "Unknown"; // Default fallback
        
        // 1. Fetch Role from Database
        String sql = "SELECT role FROM users WHERE user_id = ?";
        try (Connection conn = MySqlConnector.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, userId);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    userRole = rs.getString("role");
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error fetching user role: ", e);
        }
        
        // 2. Setup Button Visibility (Only Admins can see/access POS)
        boolean isAdmin = "Admin".equalsIgnoreCase(userRole);
        
        // Ensure button exists (avoid NullPointerException if GUI doesn't have it yet)
        if (btnAccessPOS != null) {
            btnAccessPOS.setVisible(isAdmin);
            
            // 3. Add Action Listener
            // Remove existing listeners first to prevent duplicates if called multiple times
            for (java.awt.event.ActionListener al : btnAccessPOS.getActionListeners()) {
                btnAccessPOS.removeActionListener(al);
            }
            
            if (isAdmin) {
                btnAccessPOS.addActionListener(e -> {
                    // Open DashboardPOS passing current session info
                    new DashboardPOS(userId, userName).setVisible(true);
                    // Close current window
                    this.dispose();
                });
            }
        }
    }
    
    // --- Initialization & Setup ---
    private void setupTableEditors() {
        // Editor for Availability Column (Index 5)
        TableColumn availColumn = tblShowAddedProduct.getColumnModel().getColumn(5);
        JComboBox<String> availCombo = new JComboBox<>();
        availCombo.addItem("Available");
        availCombo.addItem("Unavailable");
        availColumn.setCellEditor(new DefaultCellEditor(availCombo));
    }
    private void setupListeners() {
        // 1. Table Model Listener (Availability Update)
        tblShowAddedProduct.getModel().addTableModelListener(e -> {
            if (e.getType() == javax.swing.event.TableModelEvent.UPDATE) {
                int row = e.getFirstRow();
                int col = e.getColumn();
                if (col == 5) { // Availability Column
                    DefaultTableModel model = (DefaultTableModel) tblShowAddedProduct.getModel();
                    int prodId = Integer.parseInt(model.getValueAt(row, 0).toString());
                    String newAvailability = model.getValueAt(row, 5).toString();
                    updateAvailabilityInDB(prodId, newAvailability);
                }
            }
        });

        // 2. Double Click Listener (Quick Stock Action for Out of Stock items)
        tblShowAddedProduct.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = tblShowAddedProduct.getSelectedRow();
                    if (row == -1) return;
                    
                    int modelRow = tblShowAddedProduct.convertRowIndexToModel(row);
                    String status = tblShowAddedProduct.getModel().getValueAt(modelRow, 6).toString();
                    
                    if (status.equalsIgnoreCase("Out of Stock")) {
                        int confirm = JOptionPane.showConfirmDialog(rootPane, 
                                "This product is Out of Stock. Do you want to restock?", 
                                "Restock", JOptionPane.YES_NO_OPTION);
                        
                        if (confirm == JOptionPane.YES_OPTION) {
                            int prodId = Integer.parseInt(tblShowAddedProduct.getModel().getValueAt(modelRow, 0).toString());
                            // UPDATED: Explicitly pass 'false' for existing products
                            new StockAction(userId, prodId, DashboardInventory.this, false).setVisible(true);
                        }
                    }
                }
            }
        });
    }
    private void updateAvailabilityInDB(int prodId, String availability) {
        String sql = "UPDATE products SET availability = ? WHERE prod_id = ?";
        try (Connection conn = MySqlConnector.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, availability);
            pst.setInt(2, prodId);
            pst.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error updating availability: ", e);
            JOptionPane.showMessageDialog(this, "Error updating availability.");
        }
    }
    
    // --- Data Loading Methods ---
    public void refreshData() {
        loadDashboardCards();
        loadRecentActivity();
        loadStockAlerts();
        loadProductsTable();
        loadCategoriesCombo();
        loadAssignUsers();
        loadTimeLog();
        loadTransactionList();
        loadCashTenderedList();
        loadSalesReport();
        loadAllProductList();
        loadAllInventoryList();
    }
    private void loadDashboardCards() {
        try (Connection conn = MySqlConnector.getConnection()) {
            // 1. Count Products
            try (PreparedStatement pst = conn.prepareStatement("SELECT COUNT(*) FROM products WHERE user_id = ?")) {
                pst.setInt(1, userId);
                try (ResultSet rs = pst.executeQuery()) {
                    if (rs.next()) lblCountProduct.setText(String.valueOf(rs.getInt(1)));
                }
            }
            
            // 2. Low Stock Count
            try (PreparedStatement pst = conn.prepareStatement("SELECT COUNT(*) FROM products WHERE user_id = ? AND stock <= low_stock_alert AND stock > 0")) {
                pst.setInt(1, userId);
                try (ResultSet rs = pst.executeQuery()) {
                    if (rs.next()) lblLowStock.setText(String.valueOf(rs.getInt(1)));
                }
            }
            
            // 3. Out of Stock Count
            try (PreparedStatement pst = conn.prepareStatement("SELECT COUNT(*) FROM products WHERE user_id = ? AND stock = 0")) {
                pst.setInt(1, userId);
                try (ResultSet rs = pst.executeQuery()) {
                    if (rs.next()) lblOutOfStock.setText(String.valueOf(rs.getInt(1)));
                }
            }
            
            // 4. Attention Logic
            String attnSql = "(SELECT name, 'Out of Stock' as alert FROM products WHERE user_id = ? AND stock = 0 LIMIT 1) " +
                             "UNION (SELECT name, 'Low Stock' as alert FROM products WHERE user_id = ? AND stock <= low_stock_alert AND stock > 0 LIMIT 1) LIMIT 1";
            try (PreparedStatement pst = conn.prepareStatement(attnSql)) {
                pst.setInt(1, userId);
                pst.setInt(2, userId);
                try (ResultSet rs = pst.executeQuery()) {
                    if (rs.next()) lblAttention.setText(rs.getString("name") + " (" + rs.getString("alert") + ")");
                    else lblAttention.setText("All Good");
                }
            }
            
            // 5. Today's Sales
            String salesSql = "SELECT SUM(total_amount) FROM transactions WHERE (user_id = ? OR user_id IN (SELECT user_id FROM users WHERE parent_id = ?)) AND DATE(trans_date) = CURDATE()";
            try (PreparedStatement pst = conn.prepareStatement(salesSql)) {
                pst.setInt(1, userId);
                pst.setInt(2, userId);
                try (ResultSet rs = pst.executeQuery()) {
                    if (rs.next()) {
                        double sales = rs.getDouble(1);
                        lblTodaySales.setText(sales > 0 ? String.format("P %.2f", sales) : "P 0.00");
                    }
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error loading dashboard cards: ", e);
        }
    }
    private void loadRecentActivity() {
        DefaultTableModel model = (DefaultTableModel) tblRecentActivity.getModel();
        model.setRowCount(0);
        String sql = "SELECT product_name, quantity, action_type, log_time, reason FROM activity_log WHERE user_id = ? AND action_type != 'Sold' ORDER BY log_time DESC LIMIT 20";
        
        try (Connection conn = MySqlConnector.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, userId);
            try (ResultSet rs = pst.executeQuery()) {
                while(rs.next()) {
                    model.addRow(new Object[]{
                        rs.getString("product_name"),
                        rs.getInt("quantity"),
                        rs.getString("action_type"),
                        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(rs.getTimestamp("log_time")),
                        rs.getString("reason")
                    });
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error loading recent activity: ", e);
        }
    }    
    private void loadStockAlerts() {
        DefaultTableModel model = (DefaultTableModel) tblStockAlert.getModel();
        model.setRowCount(0);
        String sql = "SELECT name, stock, low_stock_alert FROM products WHERE user_id = ? AND (stock <= low_stock_alert OR stock = 0)";
        
        try (Connection conn = MySqlConnector.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, userId);
            try (ResultSet rs = pst.executeQuery()) {
                while(rs.next()) {
                    int stock = rs.getInt("stock");
                    model.addRow(new Object[]{
                        rs.getString("name"),
                        stock,
                        stock == 0 ? "Critical" : "Warning",
                        stock == 0 ? "Out of Stock" : "Low Stock"
                    });
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error loading stock alerts: ", e);
        }
    }
    private void loadProductsTable() {
        DefaultTableModel model = (DefaultTableModel) tblShowAddedProduct.getModel();
        model.setRowCount(0);
        String sql = "SELECT prod_id, name, category, stock, price, availability, low_stock_alert FROM products WHERE user_id = ?";
        
        try (Connection conn = MySqlConnector.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, userId);
            try (ResultSet rs = pst.executeQuery()) {
                while(rs.next()) {
                    int stock = rs.getInt("stock");
                    int lowAlert = rs.getInt("low_stock_alert");
                    
                    // Calculate status dynamically
                    String status;
                    if (stock == 0) status = "Out of Stock";
                    else if (stock <= lowAlert) status = "Low Stock";
                    else status = "Full Stock";
                    
                    model.addRow(new Object[]{
                        rs.getInt("prod_id"),
                        rs.getString("name"),
                        rs.getString("category"),
                        stock,
                        rs.getDouble("price"),
                        rs.getString("availability"),
                        status
                    });
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error loading products: ", e);
        }
    }
    private void loadAssignUsers() {
        DefaultTableModel model = (DefaultTableModel) tblShowAssignUser.getModel();
        model.setRowCount(0);
        model.setColumnIdentifiers(new Object[]{"Username", "Role", "Assigned By"});
        
        String sql = "SELECT username, role FROM users WHERE parent_id = ? AND is_active = 1";
        
        try (Connection conn = MySqlConnector.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setInt(1, userId);
            try (ResultSet rs = pst.executeQuery()) {
                while(rs.next()) {
                    model.addRow(new Object[]{ rs.getString("username"), rs.getString("role"), this.userName });
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error loading users: ", e);
        }
    }
    
    private void loadTimeLog() {
        DefaultTableModel model = (DefaultTableModel) tblTimeLogUser.getModel();
        model.setRowCount(0);
        model.setColumnIdentifiers(new Object[]{"Name", "Login", "Logout", "Role", "Module"});
        
        Map<Integer, Object[]> activeSessions = new HashMap<>();
        List<Object[]> displayRows = new ArrayList<>();
        
        try (Connection conn = MySqlConnector.getConnection()) {
            String sql = "SELECT t.user_id, t.username, t.action, t.log_time, t.module, u.role " +
                         "FROM time_log t " +
                         "JOIN users u ON t.user_id = u.user_id " +
                         "WHERE t.user_id = ? OR u.parent_id = ? " +
                         "ORDER BY t.log_time ASC";
            
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setInt(1, userId);
            pst.setInt(2, userId);
            ResultSet rs = pst.executeQuery();
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            
            while(rs.next()) {
                int uId = rs.getInt("user_id");
                String uName = rs.getString("username");
                String action = rs.getString("action");
                String role = rs.getString("role");
                String module = rs.getString("module");
                java.sql.Timestamp time = rs.getTimestamp("log_time");
                
                if (action.equalsIgnoreCase("Login")) {
                    activeSessions.put(uId, new Object[]{time, role, uName, module});
                } else if (action.equalsIgnoreCase("Logout")) {
                    if (activeSessions.containsKey(uId)) {
                        Object[] sessionData = activeSessions.get(uId);
                        java.sql.Timestamp loginTime = (java.sql.Timestamp) sessionData[0];
                        String sessionRole = (String) sessionData[1];
                        String sessionName = (String) sessionData[2];
                        String sessionModule = (String) sessionData[3];
                        
                        displayRows.add(new Object[]{
                            sessionName,
                            sdf.format(loginTime),
                            sdf.format(time),
                            sessionRole,
                            sessionModule != null ? sessionModule : "N/A"
                        });
                        
                        activeSessions.remove(uId);
                    }
                }
            }
            
            // Add active sessions (users still logged in)
            for (Object[] sessionData : activeSessions.values()) {
                java.sql.Timestamp loginTime = (java.sql.Timestamp) sessionData[0];
                String sessionRole = (String) sessionData[1];
                String sessionName = (String) sessionData[2];
                String sessionModule = (String) sessionData[3];
                
                displayRows.add(new Object[]{
                    sessionName,
                    sdf.format(loginTime),
                    "Active",
                    sessionRole,
                    sessionModule != null ? sessionModule : "N/A"
                });
            }
            
            Collections.reverse(displayRows);
            
            for (Object[] row : displayRows) {
                model.addRow(row);
            }
            
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error loading time log: ", e);
        }
    }
    // --- Other Load Methods (Similar Try-With-Resources Pattern Applied) ---
    private void loadTransactionList() {
        DefaultTableModel model = (DefaultTableModel) tblTransactionList.getModel();
        model.setRowCount(0);
        model.setColumnIdentifiers(new Object[]{"Trans ID", "Date & Time", "Cashier", "Method", "Total Amount"});
        try (Connection conn = MySqlConnector.getConnection();
             PreparedStatement pst = conn.prepareStatement("SELECT t.trans_id, t.trans_date, u.username, t.payment_method, t.total_amount FROM transactions t JOIN users u ON t.user_id = u.user_id WHERE t.user_id = ? OR u.parent_id = ? ORDER BY t.trans_date DESC")) {
             pst.setInt(1, userId); pst.setInt(2, userId);
             try (ResultSet rs = pst.executeQuery()) {
                 while(rs.next()) model.addRow(new Object[]{ rs.getInt(1), new SimpleDateFormat("yyyy-MM-dd HH:mm").format(rs.getTimestamp(2)), rs.getString(3), rs.getString(4), String.format("%.2f", rs.getDouble(5)) });
             }
        } catch (SQLException e) { logger.log(Level.SEVERE, "Error: ", e); }
    }
    private void loadSalesReport() {
        DefaultTableModel model = (DefaultTableModel) tblSalesReport.getModel();
        model.setRowCount(0);
        model.setColumnIdentifiers(new Object[]{"Date", "Gross Sales"});
        try (Connection conn = MySqlConnector.getConnection();
             PreparedStatement pst = conn.prepareStatement("SELECT DATE(trans_date) as sale_date, SUM(total_amount) as daily_total FROM transactions WHERE user_id = ? OR user_id IN (SELECT user_id FROM users WHERE parent_id = ?) GROUP BY DATE(trans_date) ORDER BY sale_date DESC")) {
             pst.setInt(1, userId); pst.setInt(2, userId);
             try (ResultSet rs = pst.executeQuery()) {
                 while(rs.next()) model.addRow(new Object[]{ rs.getDate(1), String.format("%.2f", rs.getDouble(2)) });
             }
        } catch (SQLException e) { logger.log(Level.SEVERE, "Error: ", e); }
    }
    private void loadAllProductList() {
        DefaultTableModel model = (DefaultTableModel) tblAllProductList.getModel();
        model.setRowCount(0);
        model.setColumnIdentifiers(new Object[]{"Product Name", "Total Sold", "Price"});
        try (Connection conn = MySqlConnector.getConnection();
             PreparedStatement pst = conn.prepareStatement("SELECT ti.product_name, SUM(ti.quantity) as total_qty, MAX(ti.unit_price) as unit_price FROM transaction_items ti JOIN transactions t ON ti.trans_id = t.trans_id JOIN users u ON t.user_id = u.user_id WHERE t.user_id = ? OR u.parent_id = ? GROUP BY ti.product_name ORDER BY total_qty DESC")) {
             pst.setInt(1, userId); pst.setInt(2, userId);
             try (ResultSet rs = pst.executeQuery()) {
                 while(rs.next()) model.addRow(new Object[]{ rs.getString(1), rs.getInt(2), String.format("%.2f", rs.getDouble(3)) });
             }
        } catch (SQLException e) { logger.log(Level.SEVERE, "Error: ", e); }
    }
    private void loadAllInventoryList() {
        DefaultTableModel model = (DefaultTableModel) tblAllInventoryList.getModel();
        model.setRowCount(0);
        model.setColumnIdentifiers(new Object[]{"Product Name", "Category", "Stock", "Price", "Status"});
        try (Connection conn = MySqlConnector.getConnection();
             PreparedStatement pst = conn.prepareStatement("SELECT name, category, stock, price, low_stock_alert FROM products WHERE user_id = ?")) {
             pst.setInt(1, userId);
             try (ResultSet rs = pst.executeQuery()) {
                 while(rs.next()) {
                     int stock = rs.getInt("stock");
                     int lowAlert = rs.getInt("low_stock_alert");
                     
                     String status;
                     if (stock == 0) status = "Out of Stock";
                     else if (stock <= lowAlert) status = "Low Stock";
                     else status = "Full Stock";
                     
                     model.addRow(new Object[]{ rs.getString("name"), rs.getString("category"), stock, rs.getDouble("price"), status });
                 }
             }
        } catch (SQLException e) { logger.log(Level.SEVERE, "Error: ", e); }
    }
    private void loadCashTenderedList() {
        DefaultTableModel model = (DefaultTableModel) tblCashTenderedList.getModel();
        model.setRowCount(0);
        model.setColumnIdentifiers(new Object[]{"Date & Time", "Bill Total", "Cash Tendered", "Change"});
        try (Connection conn = MySqlConnector.getConnection();
             PreparedStatement pst = conn.prepareStatement("SELECT t.trans_date, t.total_amount, t.cash_tendered, t.change_given FROM transactions t JOIN users u ON t.user_id = u.user_id WHERE t.payment_method = 'Cash' AND (t.user_id = ? OR u.parent_id = ?) ORDER BY t.trans_date DESC")) {
             pst.setInt(1, userId); pst.setInt(2, userId);
             try (ResultSet rs = pst.executeQuery()) {
                 while(rs.next()) model.addRow(new Object[]{ new SimpleDateFormat("yyyy-MM-dd HH:mm").format(rs.getTimestamp(1)), String.format("%.2f", rs.getDouble(2)), String.format("%.2f", rs.getDouble(3)), String.format("%.2f", rs.getDouble(4)) });
             }
        } catch (SQLException e) { logger.log(Level.SEVERE, "Error: ", e); }
    }
    private void loadCategoriesCombo() {
        Object selected = cmbCategory.getSelectedItem();
        cmbCategory.removeAllItems();
        cmbCategory.addItem("All");
        try (Connection conn = MySqlConnector.getConnection();
             PreparedStatement pst = conn.prepareStatement("SELECT DISTINCT category FROM products WHERE user_id = ? AND category IS NOT NULL AND category != ''")) {
             pst.setInt(1, userId);
             try (ResultSet rs = pst.executeQuery()) {
                 while(rs.next()) cmbCategory.addItem(rs.getString(1));
             }
             cmbCategory.setSelectedItem(selected != null ? selected : "All");
        } catch (SQLException e) { logger.log(Level.SEVERE, "Error: ", e); }
    }
    private void recordTimeLog(String action) {
        try (Connection conn = MySqlConnector.getConnection();
             PreparedStatement pst = conn.prepareStatement("INSERT INTO time_log (user_id, username, action, log_time) VALUES (?, ?, ?, NOW())")) {
            pst.setInt(1, userId);
            pst.setString(2, userName);
            pst.setString(3, action);
            pst.executeUpdate();
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error recording time log: ", e);
        }
    }
    
    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jPanel28 = new javax.swing.JPanel();
        jPanel29 = new javax.swing.JPanel();
        jPanel30 = new javax.swing.JPanel();
        jLabel18 = new javax.swing.JLabel();
        btnDashboard = new javax.swing.JButton();
        btnProducts = new javax.swing.JButton();
        btnUser = new javax.swing.JButton();
        btnLogOut = new javax.swing.JButton();
        btnReport = new javax.swing.JButton();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel4 = new javax.swing.JPanel();
        jLabel = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jPanel8 = new javax.swing.JPanel();
        jPanel12 = new javax.swing.JPanel();
        jLabel7 = new javax.swing.JLabel();
        lblTodaySales = new javax.swing.JLabel();
        jPanel9 = new javax.swing.JPanel();
        jPanel13 = new javax.swing.JPanel();
        jLabel8 = new javax.swing.JLabel();
        lblLowStock = new javax.swing.JLabel();
        jPanel10 = new javax.swing.JPanel();
        jPanel20 = new javax.swing.JPanel();
        jLabel15 = new javax.swing.JLabel();
        lblCountProduct = new javax.swing.JLabel();
        jPanel11 = new javax.swing.JPanel();
        jPanel19 = new javax.swing.JPanel();
        jLabel14 = new javax.swing.JLabel();
        lblOutOfStock = new javax.swing.JLabel();
        jPanel21 = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        lblAttention = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        tblStockAlert = new javax.swing.JTable();
        jScrollPane3 = new javax.swing.JScrollPane();
        tblRecentActivity = new javax.swing.JTable();
        lblTitle = new javax.swing.JLabel();
        jPanel5 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        txtSearch = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        cmbCategory = new javax.swing.JComboBox<>();
        jLabel3 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblShowAddedProduct = new javax.swing.JTable();
        jPanel22 = new javax.swing.JPanel();
        btnADD = new javax.swing.JButton();
        jPanel23 = new javax.swing.JPanel();
        btnEDIT = new javax.swing.JButton();
        jPanel24 = new javax.swing.JPanel();
        btnStockAction = new javax.swing.JButton();
        jPanel25 = new javax.swing.JPanel();
        btnDelete = new javax.swing.JButton();
        jPanel26 = new javax.swing.JPanel();
        btnDetails = new javax.swing.JButton();
        jPanel6 = new javax.swing.JPanel();
        jPanel27 = new javax.swing.JPanel();
        txtAddAssignUserFromAdmin = new javax.swing.JTextField();
        cmbAssignUserRole = new javax.swing.JComboBox<>();
        btnSaveUser = new javax.swing.JButton();
        jLabel16 = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        btnEditUser = new javax.swing.JButton();
        jScrollPane4 = new javax.swing.JScrollPane();
        tblShowAssignUser = new javax.swing.JTable();
        jScrollPane5 = new javax.swing.JScrollPane();
        tblTimeLogUser = new javax.swing.JTable();
        jLabel5 = new javax.swing.JLabel();
        jPanel7 = new javax.swing.JPanel();
        jTabbedPane2 = new javax.swing.JTabbedPane();
        jPanel14 = new javax.swing.JPanel();
        jLabel9 = new javax.swing.JLabel();
        jScrollPane6 = new javax.swing.JScrollPane();
        tblAllProductList = new javax.swing.JTable();
        jPanel15 = new javax.swing.JPanel();
        jLabel10 = new javax.swing.JLabel();
        jScrollPane7 = new javax.swing.JScrollPane();
        tblAllInventoryList = new javax.swing.JTable();
        jPanel16 = new javax.swing.JPanel();
        jLabel11 = new javax.swing.JLabel();
        jScrollPane8 = new javax.swing.JScrollPane();
        tblTransactionList = new javax.swing.JTable();
        jPanel17 = new javax.swing.JPanel();
        jLabel12 = new javax.swing.JLabel();
        jScrollPane9 = new javax.swing.JScrollPane();
        tblSalesReport = new javax.swing.JTable();
        jPanel18 = new javax.swing.JPanel();
        jLabel13 = new javax.swing.JLabel();
        jScrollPane10 = new javax.swing.JScrollPane();
        tblCashTenderedList = new javax.swing.JTable();
        btnAccessPOS = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setBackground(new java.awt.Color(20, 200, 130));

        jPanel2.setBackground(new java.awt.Color(255, 255, 255));

        jPanel3.setBackground(new java.awt.Color(245, 196, 0));
        jPanel3.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel28.setBackground(new java.awt.Color(28, 43, 58));

        jPanel29.setBackground(new java.awt.Color(255, 51, 0));
        jPanel29.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jPanel30.setBackground(new java.awt.Color(28, 43, 58));
        jPanel30.setLayout(new org.netbeans.lib.awtextra.AbsoluteLayout());

        jLabel18.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Gemini_Generated_Image_45mrif45mrif45mr-removebg-preview.png"))); // NOI18N
        jPanel30.add(jLabel18, new org.netbeans.lib.awtextra.AbsoluteConstraints(-120, -110, 490, 310));

        btnDashboard.setText("Dashboard");
        btnDashboard.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDashboardActionPerformed(evt);
            }
        });
        jPanel30.add(btnDashboard, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 210, 201, 40));

        btnProducts.setText("Products");
        btnProducts.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnProductsActionPerformed(evt);
            }
        });
        jPanel30.add(btnProducts, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 260, 201, 40));

        btnUser.setText("User's");
        btnUser.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnUserActionPerformed(evt);
            }
        });
        jPanel30.add(btnUser, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 310, 201, 40));

        btnLogOut.setText("Log Out");
        btnLogOut.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLogOutActionPerformed(evt);
            }
        });
        jPanel30.add(btnLogOut, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 490, 201, 35));

        btnReport.setText("Reports");
        btnReport.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnReportActionPerformed(evt);
            }
        });
        jPanel30.add(btnReport, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 360, 201, 40));

        jTabbedPane1.setBackground(new java.awt.Color(44, 76, 109));
        jTabbedPane1.setForeground(new java.awt.Color(255, 255, 255));
        jTabbedPane1.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N

        jPanel4.setBackground(new java.awt.Color(27, 47, 67));

        jLabel.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel.setForeground(new java.awt.Color(255, 255, 255));
        jLabel.setText("CRAZY CRUNCH INVENTORY");

        jLabel4.setFont(new java.awt.Font("Segoe UI", 1, 10)); // NOI18N
        jLabel4.setForeground(new java.awt.Color(255, 255, 255));
        jLabel4.setText("-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");

        jPanel8.setBackground(new java.awt.Color(20, 200, 130));

        jPanel12.setBackground(new java.awt.Color(38, 66, 95));

        jLabel7.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel7.setForeground(new java.awt.Color(255, 255, 255));
        jLabel7.setText("Today Sales");

        lblTodaySales.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblTodaySales.setForeground(new java.awt.Color(255, 255, 255));
        lblTodaySales.setText("lblTodaySales");

        javax.swing.GroupLayout jPanel12Layout = new javax.swing.GroupLayout(jPanel12);
        jPanel12.setLayout(jPanel12Layout);
        jPanel12Layout.setHorizontalGroup(
            jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel12Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel7)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(lblTodaySales, javax.swing.GroupLayout.DEFAULT_SIZE, 88, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel12Layout.setVerticalGroup(
            jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel12Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel12Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblTodaySales, javax.swing.GroupLayout.DEFAULT_SIZE, 104, Short.MAX_VALUE))
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel12, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel12, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel9.setBackground(new java.awt.Color(255, 153, 51));

        jPanel13.setBackground(new java.awt.Color(38, 66, 95));

        jLabel8.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel8.setForeground(new java.awt.Color(255, 255, 255));
        jLabel8.setText("Low Stock");

        lblLowStock.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblLowStock.setForeground(new java.awt.Color(255, 255, 255));
        lblLowStock.setText("lblLowStock");

        javax.swing.GroupLayout jPanel13Layout = new javax.swing.GroupLayout(jPanel13);
        jPanel13.setLayout(jPanel13Layout);
        jPanel13Layout.setHorizontalGroup(
            jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel13Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel8)
                .addGap(21, 21, 21)
                .addComponent(lblLowStock, javax.swing.GroupLayout.DEFAULT_SIZE, 88, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel13Layout.setVerticalGroup(
            jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel13Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8, javax.swing.GroupLayout.DEFAULT_SIZE, 104, Short.MAX_VALUE)
                    .addComponent(lblLowStock, javax.swing.GroupLayout.DEFAULT_SIZE, 104, Short.MAX_VALUE))
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel9Layout = new javax.swing.GroupLayout(jPanel9);
        jPanel9.setLayout(jPanel9Layout);
        jPanel9Layout.setHorizontalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel13, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel9Layout.setVerticalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel9Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel13, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel10.setBackground(new java.awt.Color(51, 51, 255));

        jPanel20.setBackground(new java.awt.Color(38, 66, 95));

        jLabel15.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel15.setForeground(new java.awt.Color(255, 255, 255));
        jLabel15.setText("Product Count");

        lblCountProduct.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblCountProduct.setForeground(new java.awt.Color(255, 255, 255));
        lblCountProduct.setText("lblCountP");

        javax.swing.GroupLayout jPanel20Layout = new javax.swing.GroupLayout(jPanel20);
        jPanel20.setLayout(jPanel20Layout);
        jPanel20Layout.setHorizontalGroup(
            jPanel20Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel20Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel15)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblCountProduct, javax.swing.GroupLayout.DEFAULT_SIZE, 78, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel20Layout.setVerticalGroup(
            jPanel20Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel20Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel20Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel15, javax.swing.GroupLayout.DEFAULT_SIZE, 104, Short.MAX_VALUE)
                    .addComponent(lblCountProduct, javax.swing.GroupLayout.DEFAULT_SIZE, 104, Short.MAX_VALUE))
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel10Layout = new javax.swing.GroupLayout(jPanel10);
        jPanel10.setLayout(jPanel10Layout);
        jPanel10Layout.setHorizontalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel20, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel10Layout.setVerticalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel10Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel20, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel11.setBackground(new java.awt.Color(255, 51, 51));

        jPanel19.setBackground(new java.awt.Color(38, 66, 95));

        jLabel14.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel14.setForeground(new java.awt.Color(255, 255, 255));
        jLabel14.setText("OutOfStock");

        lblOutOfStock.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblOutOfStock.setForeground(new java.awt.Color(255, 255, 255));
        lblOutOfStock.setText("lblOutS");

        javax.swing.GroupLayout jPanel19Layout = new javax.swing.GroupLayout(jPanel19);
        jPanel19.setLayout(jPanel19Layout);
        jPanel19Layout.setHorizontalGroup(
            jPanel19Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel19Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel14)
                .addGap(13, 13, 13)
                .addComponent(lblOutOfStock, javax.swing.GroupLayout.DEFAULT_SIZE, 88, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel19Layout.setVerticalGroup(
            jPanel19Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel19Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel19Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel14, javax.swing.GroupLayout.DEFAULT_SIZE, 104, Short.MAX_VALUE)
                    .addComponent(lblOutOfStock, javax.swing.GroupLayout.DEFAULT_SIZE, 104, Short.MAX_VALUE))
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel11Layout = new javax.swing.GroupLayout(jPanel11);
        jPanel11.setLayout(jPanel11Layout);
        jPanel11Layout.setHorizontalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel19, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel11Layout.setVerticalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel11Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel19, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel21.setBackground(new java.awt.Color(244, 164, 94));

        jLabel6.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel6.setText("ALERT NOTIFACATION:");

        lblAttention.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lblAttention.setText("Appear what product is need lblAttention");

        javax.swing.GroupLayout jPanel21Layout = new javax.swing.GroupLayout(jPanel21);
        jPanel21.setLayout(jPanel21Layout);
        jPanel21Layout.setHorizontalGroup(
            jPanel21Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel21Layout.createSequentialGroup()
                .addGap(71, 71, 71)
                .addComponent(jLabel6)
                .addGap(78, 78, 78)
                .addComponent(lblAttention)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel21Layout.setVerticalGroup(
            jPanel21Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel21Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jLabel6, javax.swing.GroupLayout.DEFAULT_SIZE, 45, Short.MAX_VALUE)
                .addComponent(lblAttention))
        );

        tblStockAlert.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Product Name", "Stock", "Alert", "Status"
            }
        ));
        jScrollPane2.setViewportView(tblStockAlert);

        tblRecentActivity.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Action", "Name", "Quantity", "Time", "Reason"
            }
        ));
        jScrollPane3.setViewportView(tblRecentActivity);

        lblTitle.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        lblTitle.setForeground(new java.awt.Color(255, 255, 255));
        lblTitle.setText("Appear whos user and title is. lbTitle");

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel4)
                            .addGroup(jPanel4Layout.createSequentialGroup()
                                .addComponent(jPanel8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jPanel9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jPanel11, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jPanel10, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel4Layout.createSequentialGroup()
                                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 394, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jPanel21, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addGap(56, 56, 56)
                        .addComponent(jLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(lblTitle)
                        .addGap(27, 27, 27)))
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel)
                    .addComponent(lblTitle))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel9, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel10, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel11, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel21, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 272, Short.MAX_VALUE)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addContainerGap())
        );

        jTabbedPane1.addTab("DASHBOARD", jPanel4);

        jPanel5.setBackground(new java.awt.Color(27, 47, 67));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 20)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setText("Product Management");

        txtSearch.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtSearchKeyReleased(evt);
            }
        });

        jLabel2.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(255, 255, 255));
        jLabel2.setText("Category");

        cmbCategory.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        cmbCategory.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbCategoryActionPerformed(evt);
            }
        });

        jLabel3.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(255, 255, 255));
        jLabel3.setText("Search");

        tblShowAddedProduct.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "ID", "Name", "Category", "Stock", "Price", "Availability", "Status"
            }
        ));
        jScrollPane1.setViewportView(tblShowAddedProduct);

        jPanel22.setBackground(new java.awt.Color(20, 200, 130));
        jPanel22.setPreferredSize(new java.awt.Dimension(150, 100));

        btnADD.setText("ADD");
        btnADD.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnADDActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel22Layout = new javax.swing.GroupLayout(jPanel22);
        jPanel22.setLayout(jPanel22Layout);
        jPanel22Layout.setHorizontalGroup(
            jPanel22Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel22Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(btnADD, javax.swing.GroupLayout.DEFAULT_SIZE, 126, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel22Layout.setVerticalGroup(
            jPanel22Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel22Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(btnADD, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanel23.setBackground(new java.awt.Color(51, 51, 255));
        jPanel23.setPreferredSize(new java.awt.Dimension(150, 100));

        btnEDIT.setText("EDIT");
        btnEDIT.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEDITActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel23Layout = new javax.swing.GroupLayout(jPanel23);
        jPanel23.setLayout(jPanel23Layout);
        jPanel23Layout.setHorizontalGroup(
            jPanel23Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel23Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(btnEDIT, javax.swing.GroupLayout.DEFAULT_SIZE, 126, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel23Layout.setVerticalGroup(
            jPanel23Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel23Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(btnEDIT, javax.swing.GroupLayout.DEFAULT_SIZE, 74, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanel24.setBackground(new java.awt.Color(255, 153, 51));
        jPanel24.setPreferredSize(new java.awt.Dimension(150, 100));

        btnStockAction.setText("STOCK ACTION");
        btnStockAction.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnStockActionActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel24Layout = new javax.swing.GroupLayout(jPanel24);
        jPanel24.setLayout(jPanel24Layout);
        jPanel24Layout.setHorizontalGroup(
            jPanel24Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel24Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(btnStockAction, javax.swing.GroupLayout.DEFAULT_SIZE, 126, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel24Layout.setVerticalGroup(
            jPanel24Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel24Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(btnStockAction, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanel25.setBackground(new java.awt.Color(255, 51, 51));
        jPanel25.setPreferredSize(new java.awt.Dimension(150, 100));

        btnDelete.setText("DELETE");
        btnDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDeleteActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel25Layout = new javax.swing.GroupLayout(jPanel25);
        jPanel25.setLayout(jPanel25Layout);
        jPanel25Layout.setHorizontalGroup(
            jPanel25Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel25Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(btnDelete, javax.swing.GroupLayout.DEFAULT_SIZE, 126, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel25Layout.setVerticalGroup(
            jPanel25Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel25Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(btnDelete, javax.swing.GroupLayout.DEFAULT_SIZE, 74, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanel26.setBackground(new java.awt.Color(153, 0, 255));

        btnDetails.setText("DETAILS");
        btnDetails.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDetailsActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel26Layout = new javax.swing.GroupLayout(jPanel26);
        jPanel26.setLayout(jPanel26Layout);
        jPanel26Layout.setHorizontalGroup(
            jPanel26Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel26Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(btnDetails, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel26Layout.setVerticalGroup(
            jPanel26Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel26Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(btnDetails, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGap(30, 30, 30)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel5Layout.createSequentialGroup()
                        .addComponent(jPanel22, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel23, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel24, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel25, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel26, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(jScrollPane1)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addGap(111, 111, 111)
                        .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 160, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 37, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(cmbCategory, javax.swing.GroupLayout.PREFERRED_SIZE, 155, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel2)))
                .addGap(30, 30, 30))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGap(22, 22, 22)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2)
                    .addComponent(cmbCategory, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 369, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(jPanel23, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jPanel24, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jPanel26, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jPanel22, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(jPanel25, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        jTabbedPane1.addTab("PRODUCT", jPanel5);

        jPanel6.setBackground(new java.awt.Color(27, 47, 67));

        jPanel27.setBackground(new java.awt.Color(38, 66, 95));

        cmbAssignUserRole.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "POS", "INVENTORY" }));

        btnSaveUser.setText("Save");
        btnSaveUser.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveUserActionPerformed(evt);
            }
        });

        jLabel16.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        jLabel16.setForeground(new java.awt.Color(255, 255, 255));
        jLabel16.setText("Select Role ");

        jLabel17.setFont(new java.awt.Font("Segoe UI", 1, 13)); // NOI18N
        jLabel17.setForeground(new java.awt.Color(255, 255, 255));
        jLabel17.setText("AssignUser by Admin");

        btnEditUser.setText("Edit");
        btnEditUser.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnEditUserActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel27Layout = new javax.swing.GroupLayout(jPanel27);
        jPanel27.setLayout(jPanel27Layout);
        jPanel27Layout.setHorizontalGroup(
            jPanel27Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel27Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel27Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(txtAddAssignUserFromAdmin)
                    .addComponent(cmbAssignUserRole, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel27Layout.createSequentialGroup()
                        .addComponent(btnSaveUser, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 30, Short.MAX_VALUE)
                        .addComponent(btnEditUser, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel27Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel17)
                .addGap(58, 58, 58))
            .addGroup(jPanel27Layout.createSequentialGroup()
                .addGap(92, 92, 92)
                .addComponent(jLabel16)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel27Layout.setVerticalGroup(
            jPanel27Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel27Layout.createSequentialGroup()
                .addGap(27, 27, 27)
                .addComponent(jLabel17)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtAddAssignUserFromAdmin, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(26, 26, 26)
                .addComponent(jLabel16)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cmbAssignUserRole, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel27Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnEditUser, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnSaveUser, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(32, 32, 32))
        );

        tblShowAssignUser.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Name", "Role", "Assign By"
            }
        ));
        jScrollPane4.setViewportView(tblShowAssignUser);

        tblTimeLogUser.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Name", "Log In", "Log Out", "Time Log Module", "Role"
            }
        ));
        jScrollPane5.setViewportView(tblTimeLogUser);

        jLabel5.setFont(new java.awt.Font("Segoe UI", 1, 20)); // NOI18N
        jLabel5.setForeground(new java.awt.Color(255, 255, 255));
        jLabel5.setText("Time Logs");

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(jPanel27, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 524, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jScrollPane5))
                .addContainerGap())
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGap(84, 84, 84)
                .addComponent(jLabel5)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 298, Short.MAX_VALUE)
                    .addComponent(jPanel27, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane5, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("USER'S", jPanel6);

        jPanel7.setBackground(new java.awt.Color(15, 25, 35));

        jTabbedPane2.setBackground(new java.awt.Color(44, 76, 109));
        jTabbedPane2.setForeground(new java.awt.Color(255, 255, 255));
        jTabbedPane2.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N

        jPanel14.setBackground(new java.awt.Color(27, 47, 67));

        jLabel9.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel9.setForeground(new java.awt.Color(255, 255, 255));
        jLabel9.setText("All Product List");

        tblAllProductList.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane6.setViewportView(tblAllProductList);

        javax.swing.GroupLayout jPanel14Layout = new javax.swing.GroupLayout(jPanel14);
        jPanel14.setLayout(jPanel14Layout);
        jPanel14Layout.setHorizontalGroup(
            jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel14Layout.createSequentialGroup()
                .addGroup(jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel14Layout.createSequentialGroup()
                        .addGap(70, 70, 70)
                        .addComponent(jLabel9))
                    .addGroup(jPanel14Layout.createSequentialGroup()
                        .addGap(35, 35, 35)
                        .addComponent(jScrollPane6, javax.swing.GroupLayout.PREFERRED_SIZE, 746, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel14Layout.setVerticalGroup(
            jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel14Layout.createSequentialGroup()
                .addGap(27, 27, 27)
                .addComponent(jLabel9)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(49, Short.MAX_VALUE))
        );

        jTabbedPane2.addTab("Product List", jPanel14);

        jPanel15.setBackground(new java.awt.Color(27, 47, 67));

        jLabel10.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel10.setForeground(new java.awt.Color(255, 255, 255));
        jLabel10.setText("All Inventory List");

        tblAllInventoryList.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane7.setViewportView(tblAllInventoryList);

        javax.swing.GroupLayout jPanel15Layout = new javax.swing.GroupLayout(jPanel15);
        jPanel15.setLayout(jPanel15Layout);
        jPanel15Layout.setHorizontalGroup(
            jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel15Layout.createSequentialGroup()
                .addGap(68, 68, 68)
                .addComponent(jLabel10)
                .addContainerGap(550, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel15Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jScrollPane7, javax.swing.GroupLayout.PREFERRED_SIZE, 746, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(33, 33, 33))
        );
        jPanel15Layout.setVerticalGroup(
            jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel15Layout.createSequentialGroup()
                .addGap(33, 33, 33)
                .addComponent(jLabel10)
                .addGap(18, 18, 18)
                .addComponent(jScrollPane7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPane2.addTab("Inventory List", jPanel15);

        jPanel16.setBackground(new java.awt.Color(27, 47, 67));

        jLabel11.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel11.setForeground(new java.awt.Color(255, 255, 255));
        jLabel11.setText("Transaction List");

        tblTransactionList.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane8.setViewportView(tblTransactionList);

        javax.swing.GroupLayout jPanel16Layout = new javax.swing.GroupLayout(jPanel16);
        jPanel16.setLayout(jPanel16Layout);
        jPanel16Layout.setHorizontalGroup(
            jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel16Layout.createSequentialGroup()
                .addGap(68, 68, 68)
                .addComponent(jLabel11)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel16Layout.createSequentialGroup()
                .addContainerGap(33, Short.MAX_VALUE)
                .addComponent(jScrollPane8, javax.swing.GroupLayout.PREFERRED_SIZE, 746, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(31, 31, 31))
        );
        jPanel16Layout.setVerticalGroup(
            jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel16Layout.createSequentialGroup()
                .addGap(33, 33, 33)
                .addComponent(jLabel11)
                .addGap(18, 18, 18)
                .addComponent(jScrollPane8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPane2.addTab("Transaction List", jPanel16);

        jPanel17.setBackground(new java.awt.Color(27, 47, 67));

        jLabel12.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel12.setForeground(new java.awt.Color(255, 255, 255));
        jLabel12.setText("Sales Report List");

        tblSalesReport.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane9.setViewportView(tblSalesReport);

        javax.swing.GroupLayout jPanel17Layout = new javax.swing.GroupLayout(jPanel17);
        jPanel17.setLayout(jPanel17Layout);
        jPanel17Layout.setHorizontalGroup(
            jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel17Layout.createSequentialGroup()
                .addGap(68, 68, 68)
                .addComponent(jLabel12)
                .addContainerGap(556, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel17Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jScrollPane9, javax.swing.GroupLayout.PREFERRED_SIZE, 746, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(34, 34, 34))
        );
        jPanel17Layout.setVerticalGroup(
            jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel17Layout.createSequentialGroup()
                .addGap(33, 33, 33)
                .addComponent(jLabel12)
                .addGap(18, 18, 18)
                .addComponent(jScrollPane9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jTabbedPane2.addTab("Sales Report List", jPanel17);

        jPanel18.setBackground(new java.awt.Color(27, 47, 67));

        jLabel13.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel13.setForeground(new java.awt.Color(255, 255, 255));
        jLabel13.setText("Cash Tendered List");

        tblCashTenderedList.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane10.setViewportView(tblCashTenderedList);

        javax.swing.GroupLayout jPanel18Layout = new javax.swing.GroupLayout(jPanel18);
        jPanel18.setLayout(jPanel18Layout);
        jPanel18Layout.setHorizontalGroup(
            jPanel18Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel18Layout.createSequentialGroup()
                .addGap(68, 68, 68)
                .addComponent(jLabel13)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel18Layout.createSequentialGroup()
                .addContainerGap(33, Short.MAX_VALUE)
                .addComponent(jScrollPane10, javax.swing.GroupLayout.PREFERRED_SIZE, 746, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(31, 31, 31))
        );
        jPanel18Layout.setVerticalGroup(
            jPanel18Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel18Layout.createSequentialGroup()
                .addGap(33, 33, 33)
                .addComponent(jLabel13)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane10, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(37, Short.MAX_VALUE))
        );

        jTabbedPane2.addTab("Cash Tendered", jPanel18);

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane2)
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane2)
        );

        jTabbedPane1.addTab("REPORTS", jPanel7);

        jPanel30.add(jTabbedPane1, new org.netbeans.lib.awtextra.AbsoluteConstraints(370, 0, 820, -1));

        btnAccessPOS.setText("Access POS");
        jPanel30.add(btnAccessPOS, new org.netbeans.lib.awtextra.AbsoluteConstraints(70, 410, 200, 40));

        jPanel29.add(jPanel30, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 10, 1200, 590));

        javax.swing.GroupLayout jPanel28Layout = new javax.swing.GroupLayout(jPanel28);
        jPanel28.setLayout(jPanel28Layout);
        jPanel28Layout.setHorizontalGroup(
            jPanel28Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel28Layout.createSequentialGroup()
                .addComponent(jPanel29, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel28Layout.setVerticalGroup(
            jPanel28Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel28Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel29, javax.swing.GroupLayout.DEFAULT_SIZE, 607, Short.MAX_VALUE))
        );

        jPanel3.add(jPanel28, new org.netbeans.lib.awtextra.AbsoluteConstraints(0, 0, 1210, 620));

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel3, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, 1198, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnADDActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnADDActionPerformed
        new AddFoodSystem(userId, this).setVisible(true);
    }//GEN-LAST:event_btnADDActionPerformed

    private void btnEDITActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEDITActionPerformed
        int selectedRow = tblShowAddedProduct.getSelectedRow();
        if (selectedRow == -1) { JOptionPane.showMessageDialog(this, "Please select a product."); return; }
        int modelRow = tblShowAddedProduct.convertRowIndexToModel(selectedRow);
        int prodId = Integer.parseInt(tblShowAddedProduct.getModel().getValueAt(modelRow, 0).toString());
        new EditFoodSystem(userId, prodId, this).setVisible(true);
    }//GEN-LAST:event_btnEDITActionPerformed

    private void btnStockActionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnStockActionActionPerformed
        int selectedRow = tblShowAddedProduct.getSelectedRow();
        if (selectedRow == -1) { JOptionPane.showMessageDialog(this, "Please select a product."); return; }
        int modelRow = tblShowAddedProduct.convertRowIndexToModel(selectedRow);
        int prodId = Integer.parseInt(tblShowAddedProduct.getModel().getValueAt(modelRow, 0).toString());
        
        // UPDATED: Explicitly pass 'false' for existing products
        new StockAction(userId, prodId, this, false).setVisible(true);
    }//GEN-LAST:event_btnStockActionActionPerformed

    private void btnDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDeleteActionPerformed
        int selectedRow = tblShowAddedProduct.getSelectedRow();
        if (selectedRow == -1) { JOptionPane.showMessageDialog(this, "Select a product to delete."); return; }
        int modelRow = tblShowAddedProduct.convertRowIndexToModel(selectedRow);
        int prodId = Integer.parseInt(tblShowAddedProduct.getModel().getValueAt(modelRow, 0).toString());
        String prodName = tblShowAddedProduct.getModel().getValueAt(modelRow, 1).toString();

        int confirm = JOptionPane.showConfirmDialog(this, "Delete '" + prodName + "'?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            String sql = "DELETE FROM products WHERE prod_id = ? AND user_id = ?";
            try (Connection conn = MySqlConnector.getConnection();
                 PreparedStatement pst = conn.prepareStatement(sql)) {
                pst.setInt(1, prodId);
                pst.setInt(2, userId);
                if (pst.executeUpdate() > 0) {
                    JOptionPane.showMessageDialog(this, "Deleted.");
                    refreshData();
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
            }
        }
    }//GEN-LAST:event_btnDeleteActionPerformed

    private void btnLogOutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLogOutActionPerformed
        recordTimeLog("Logout");
        refreshTimer.stop();
        new LogInUser().setVisible(true);
        this.dispose();
    }//GEN-LAST:event_btnLogOutActionPerformed

    private void txtSearchKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtSearchKeyReleased
        // Simple live search filter using TableRowSorter
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>((DefaultTableModel) tblShowAddedProduct.getModel());
        tblShowAddedProduct.setRowSorter(sorter);
        String text = txtSearch.getText();
        sorter.setRowFilter(text.trim().length() == 0 ? null : RowFilter.regexFilter("(?i)" + text, 1));
    }//GEN-LAST:event_txtSearchKeyReleased

    private void cmbCategoryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbCategoryActionPerformed
        if (cmbCategory.getSelectedItem() == null) return;
        String selected = cmbCategory.getSelectedItem().toString();
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>((DefaultTableModel) tblShowAddedProduct.getModel());
        tblShowAddedProduct.setRowSorter(sorter);
        sorter.setRowFilter(selected.equals("All") ? null : RowFilter.regexFilter("^" + selected + "$", 2));
    }//GEN-LAST:event_cmbCategoryActionPerformed

    private void btnSaveUserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveUserActionPerformed
        String username = txtAddAssignUserFromAdmin.getText().trim();
        if (cmbAssignUserRole.getSelectedItem() == null) { JOptionPane.showMessageDialog(this, "Select a role."); return; }
        String role = cmbAssignUserRole.getSelectedItem().toString();
        if (username.isEmpty()) { JOptionPane.showMessageDialog(this, "Enter username."); return; }

        String sql = "INSERT INTO users (username, password, role, parent_id) VALUES (?, '1234', ?, ?)";
        try (Connection conn = MySqlConnector.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            pst.setString(1, username);
            pst.setString(2, role);
            pst.setInt(3, userId);
            pst.executeUpdate();
            JOptionPane.showMessageDialog(this, "User Created! Password: 1234");
            txtAddAssignUserFromAdmin.setText("");
            refreshData();
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) JOptionPane.showMessageDialog(this, "Username exists.");
            else JOptionPane.showMessageDialog(this, "Error: " + e.getMessage());
        }
    }//GEN-LAST:event_btnSaveUserActionPerformed

    private void btnEditUserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnEditUserActionPerformed
        int selectedRow = tblShowAssignUser.getSelectedRow();
        if (selectedRow == -1) { JOptionPane.showMessageDialog(this, "Select a user."); return; }
        
        String username = tblShowAssignUser.getValueAt(selectedRow, 0).toString();
        String currentRole = tblShowAssignUser.getValueAt(selectedRow, 1).toString();
        
        String[] options = {"Change Role", "Deactivate User", "Cancel"};
        int choice = JOptionPane.showOptionDialog(this, "User: " + username, "Manage User", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        
        if (choice == 0) { // Change Role
            String[] roles = {"POS", "Inventory"};
            String newRole = (String) JOptionPane.showInputDialog(this, "Select new role:", "Change Role", JOptionPane.PLAIN_MESSAGE, null, roles, currentRole);
            if (newRole != null && !newRole.equals(currentRole)) {
                try (Connection conn = MySqlConnector.getConnection();
                     PreparedStatement pst = conn.prepareStatement("UPDATE users SET role = ? WHERE username = ? AND parent_id = ?")) {
                    pst.setString(1, newRole);
                    pst.setString(2, username);
                    pst.setInt(3, userId);
                    pst.executeUpdate();
                    JOptionPane.showMessageDialog(this, "Role updated.");
                    refreshData();
                } catch (SQLException e) { JOptionPane.showMessageDialog(this, "Error: " + e.getMessage()); }
            }
        } else if (choice == 1) { // Deactivate
            int confirm = JOptionPane.showConfirmDialog(this, "Deactivate user? They won't be able to log in.", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                try (Connection conn = MySqlConnector.getConnection();
                     PreparedStatement pst = conn.prepareStatement("UPDATE users SET is_active = 0 WHERE username = ? AND parent_id = ?")) {
                    pst.setString(1, username);
                    pst.setInt(2, userId);
                    pst.executeUpdate();
                    JOptionPane.showMessageDialog(this, "User deactivated.");
                    refreshData();
                } catch (SQLException e) { JOptionPane.showMessageDialog(this, "Error: " + e.getMessage()); }
            }
        }
    }//GEN-LAST:event_btnEditUserActionPerformed

    private void btnDetailsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDetailsActionPerformed
        int selectedRow = tblShowAddedProduct.getSelectedRow();
        
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, 
                "Please select a product to view details.", 
                "No Selection", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int modelRow = tblShowAddedProduct.convertRowIndexToModel(selectedRow);
        int prodId = Integer.parseInt(tblShowAddedProduct.getModel().getValueAt(modelRow, 0).toString());
        
        String sql = "SELECT * FROM products WHERE prod_id = ? AND user_id = ?";
        
        try (Connection conn = MySqlConnector.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            
            pst.setInt(1, prodId);
            pst.setInt(2, userId);
            
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    String name = rs.getString("name");
                    String category = rs.getString("category");
                    int stock = rs.getInt("stock");
                    double price = rs.getDouble("price");
                    String availability = rs.getString("availability");
                    String status = rs.getString("status");
                    int lowStockAlert = rs.getInt("low_stock_alert");
                    
                    StringBuilder details = new StringBuilder();
                    details.append("═══════════════════════════════════════\n");
                    details.append("         PRODUCT DETAILS\n");
                    details.append("═══════════════════════════════════════\n\n");
                    details.append(String.format("%-20s: %d\n", "Product ID", prodId));
                    details.append(String.format("%-20s: %s\n", "Name", name));
                    details.append(String.format("%-20s: %s\n", "Category", (category != null ? category : "Uncategorized")));
                    details.append(String.format("%-20s: %d\n", "Current Stock", stock));
                    details.append(String.format("%-20s: P %.2f\n", "Unit Price", price));
                    details.append(String.format("%-20s: P %.2f\n", "Stock Value", (price * stock)));
                    details.append(String.format("%-20s: %s\n", "Availability", availability));
                    details.append(String.format("%-20s: %s\n", "Status", status));
                    details.append(String.format("%-20s: %d\n", "Low Stock Alert At", lowStockAlert));
                    details.append("\n═══════════════════════════════════════");
                    
                    JOptionPane.showMessageDialog(this, 
                        details.toString(), 
                        "Product Details - " + name, 
                        JOptionPane.INFORMATION_MESSAGE);
                        
                } else {
                    JOptionPane.showMessageDialog(this, 
                        "Product not found or access denied.", 
                        "Error", 
                        JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error loading product details", e);
            JOptionPane.showMessageDialog(this, 
                "Database Error: " + e.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }//GEN-LAST:event_btnDetailsActionPerformed

    private void btnDashboardActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnDashboardActionPerformed
        jTabbedPane1.setSelectedIndex(0);
    }//GEN-LAST:event_btnDashboardActionPerformed

    private void btnProductsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnProductsActionPerformed
        jTabbedPane1.setSelectedIndex(1);
    }//GEN-LAST:event_btnProductsActionPerformed

    private void btnUserActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUserActionPerformed
        jTabbedPane1.setSelectedIndex(2);
    }//GEN-LAST:event_btnUserActionPerformed

    private void btnReportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnReportActionPerformed
        jTabbedPane1.setSelectedIndex(3);
    }//GEN-LAST:event_btnReportActionPerformed

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(() -> new DashboardInventory(0, "Admin").setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnADD;
    private javax.swing.JButton btnAccessPOS;
    private javax.swing.JButton btnDashboard;
    private javax.swing.JButton btnDelete;
    private javax.swing.JButton btnDetails;
    private javax.swing.JButton btnEDIT;
    private javax.swing.JButton btnEditUser;
    private javax.swing.JButton btnLogOut;
    private javax.swing.JButton btnProducts;
    private javax.swing.JButton btnReport;
    private javax.swing.JButton btnSaveUser;
    private javax.swing.JButton btnStockAction;
    private javax.swing.JButton btnUser;
    private javax.swing.JComboBox<String> cmbAssignUserRole;
    private javax.swing.JComboBox<String> cmbCategory;
    private javax.swing.JLabel jLabel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JPanel jPanel13;
    private javax.swing.JPanel jPanel14;
    private javax.swing.JPanel jPanel15;
    private javax.swing.JPanel jPanel16;
    private javax.swing.JPanel jPanel17;
    private javax.swing.JPanel jPanel18;
    private javax.swing.JPanel jPanel19;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel20;
    private javax.swing.JPanel jPanel21;
    private javax.swing.JPanel jPanel22;
    private javax.swing.JPanel jPanel23;
    private javax.swing.JPanel jPanel24;
    private javax.swing.JPanel jPanel25;
    private javax.swing.JPanel jPanel26;
    private javax.swing.JPanel jPanel27;
    private javax.swing.JPanel jPanel28;
    private javax.swing.JPanel jPanel29;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel30;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane10;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JScrollPane jScrollPane7;
    private javax.swing.JScrollPane jScrollPane8;
    private javax.swing.JScrollPane jScrollPane9;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTabbedPane jTabbedPane2;
    private javax.swing.JLabel lblAttention;
    private javax.swing.JLabel lblCountProduct;
    private javax.swing.JLabel lblLowStock;
    private javax.swing.JLabel lblOutOfStock;
    private javax.swing.JLabel lblTitle;
    private javax.swing.JLabel lblTodaySales;
    private javax.swing.JTable tblAllInventoryList;
    private javax.swing.JTable tblAllProductList;
    private javax.swing.JTable tblCashTenderedList;
    private javax.swing.JTable tblRecentActivity;
    private javax.swing.JTable tblSalesReport;
    private javax.swing.JTable tblShowAddedProduct;
    private javax.swing.JTable tblShowAssignUser;
    private javax.swing.JTable tblStockAlert;
    private javax.swing.JTable tblTimeLogUser;
    private javax.swing.JTable tblTransactionList;
    private javax.swing.JTextField txtAddAssignUserFromAdmin;
    private javax.swing.JTextField txtSearch;
    // End of variables declaration//GEN-END:variables
}
