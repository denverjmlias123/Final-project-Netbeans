package groupprojectexe;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

public class StockAction extends javax.swing.JFrame {
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(StockAction.class.getName());
    
    private final int userId;
    private final int prodId;
    private final DashboardInventory parentDashboard;
    private final boolean isNewProduct; // NEW: Track if this is first-time stocking
    
    private int currentStock;
    private String productName;
    private String currentAvailability;
    
    // Constructor for existing products (default behavior)
    public StockAction(int userId, int prodId, DashboardInventory parent) {
        this(userId, prodId, parent, false);
    }
    // Constructor with New Product flag
    public StockAction(int userId, int prodId, DashboardInventory parent, boolean isNewProduct) {
        this.userId = userId;
        this.prodId = prodId;
        this.parentDashboard = parent;
        this.isNewProduct = isNewProduct; // Set the flag
        
        initComponents();
        setLocationRelativeTo(null);
        
        setupUI();
        loadProductInfo();
        updateReasons(); // Initial setup based on new/existing status
    }   
    private void setupUI() {
        txtPname.setEditable(false);
        txtCstock.setEditable(false);
        txtLowAlert.setEditable(true);
        
        cmbAction.removeAllItems();
        cmbAction.addItem("Stock In");
        cmbAction.addItem("Stock Out");
        
        cmbAction.addActionListener(e -> updateReasons());
    } 
    private void updateReasons() {
        cmbReason.removeAllItems();
        String selectedAction = cmbAction.getSelectedItem().toString();
        
        if (selectedAction.equals("Stock In")) {
            // LOGIC FOR NEW PRODUCT (First time stocking)
            if (isNewProduct) {
                cmbReason.addItem("New Stock");
                cmbReason.setEnabled(false); // Lock selection for new products
            }
            // LOGIC FOR EXISTING PRODUCT with 0 stock (Restocking from empty)
            else if (currentStock == 0) {
                cmbReason.addItem("Restock");
                cmbReason.addItem("Return from Supplier"); // If returning defective
                cmbReason.addItem("Correction");
                cmbReason.setSelectedItem("Restock"); // Auto-select
            }
            // LOGIC FOR EXISTING PRODUCT with existing stock (Adding more)
            else {
                cmbReason.addItem("Additional Stock");
                cmbReason.addItem("Restock");
                cmbReason.addItem("Return from Customer");
                cmbReason.addItem("Inventory Adjustment");
                cmbReason.setSelectedItem("Additional Stock"); // Auto-select for existing stock
            }
        } 
        else if (selectedAction.equals("Stock Out")) {
            cmbReason.addItem("Damaged");
            cmbReason.addItem("Expired");
            cmbReason.addItem("Sold"); // Manual adjustment
            cmbReason.addItem("Lost/Stolen");
            cmbReason.setEnabled(true);
        }
    }
    private void loadProductInfo() {
        String sql = "SELECT name, stock, low_stock_alert, availability FROM products WHERE prod_id = ? AND user_id = ?";
        
        try (Connection conn = MySqlConnector.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            
            pst.setInt(1, prodId);
            pst.setInt(2, userId);
            
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    productName = rs.getString("name");
                    currentStock = rs.getInt("stock");
                    currentAvailability = rs.getString("availability");
                    
                    txtPname.setText(productName);
                    txtCstock.setText(String.valueOf(currentStock));
                    txtLowAlert.setText(String.valueOf(rs.getInt("low_stock_alert")));
                    
                    // Update reasons based on loaded stock level
                    updateReasons();
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error loading product info: ", e);
            JOptionPane.showMessageDialog(this, "Error loading product details.", "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    private String calculateStatus(int newStock, int lowAlert) {
        if (newStock == 0) return "Out of Stock";
        if (newStock <= lowAlert) return "Low Stock";
        return "Full Stock";
    }
    private String calculateAvailability(int newStock, String action) {
        if (newStock == 0) return "Unavailable";
        if (action.equals("Stock In")) return "Available";
        return currentAvailability;
    }     

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        txtPname = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        txtCstock = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        txtQuantity = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        cmbAction = new javax.swing.JComboBox<>();
        jLabel12 = new javax.swing.JLabel();
        txtLowAlert = new javax.swing.JTextField();
        jLabel13 = new javax.swing.JLabel();
        cmbReason = new javax.swing.JComboBox<>();
        btnSave = new javax.swing.JButton();
        btnCancel = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setBackground(new java.awt.Color(20, 200, 130));

        jPanel2.setBackground(new java.awt.Color(15, 25, 35));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 20)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setText("Stock-In/Out Products");

        jLabel4.setFont(new java.awt.Font("Segoe UI", 1, 10)); // NOI18N
        jLabel4.setForeground(new java.awt.Color(255, 255, 255));
        jLabel4.setText("------------------------------------------------------------------------------------------");

        jLabel3.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(255, 255, 255));
        jLabel3.setText("▣");

        jLabel8.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel8.setForeground(new java.awt.Color(255, 255, 255));
        jLabel8.setText("Product Name:");

        txtPname.setBackground(new java.awt.Color(204, 204, 204));

        jLabel9.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel9.setForeground(new java.awt.Color(255, 255, 255));
        jLabel9.setText("Current Stock:");

        txtCstock.setBackground(new java.awt.Color(204, 204, 204));

        jLabel10.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel10.setForeground(new java.awt.Color(255, 255, 255));
        jLabel10.setText("Set Quantity:");

        jLabel11.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel11.setForeground(new java.awt.Color(255, 255, 255));
        jLabel11.setText("Select Action:");

        jLabel12.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel12.setForeground(new java.awt.Color(255, 255, 255));
        jLabel12.setText("Set Low Stocks Alert:");

        txtLowAlert.setBackground(new java.awt.Color(204, 204, 204));

        jLabel13.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel13.setForeground(new java.awt.Color(255, 255, 255));
        jLabel13.setText("Reason Of Action:");

        btnSave.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnSave.setText("SAVE");
        btnSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveActionPerformed(evt);
            }
        });

        btnCancel.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnCancel.setText("CANCEL");
        btnCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCancelActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(27, 27, 27)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel13)
                    .addComponent(jLabel12)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel10)
                        .addGap(109, 109, 109)
                        .addComponent(jLabel11))
                    .addComponent(jLabel9)
                    .addComponent(jLabel8)
                    .addComponent(jLabel4)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(25, 25, 25)
                        .addComponent(jLabel3)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel1)))
                .addContainerGap(29, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(btnSave, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnCancel, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(cmbReason, javax.swing.GroupLayout.PREFERRED_SIZE, 314, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(txtLowAlert, javax.swing.GroupLayout.PREFERRED_SIZE, 314, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                                .addComponent(txtQuantity, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(cmbAction, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addComponent(txtCstock, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 314, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtPname, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 314, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addGap(49, 49, 49))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel4)
                .addGap(18, 18, 18)
                .addComponent(jLabel8)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtPname, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel9)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtCstock, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel10)
                    .addComponent(jLabel11))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cmbAction)
                    .addComponent(txtQuantity, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel12)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtLowAlert, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel13)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cmbReason, javax.swing.GroupLayout.PREFERRED_SIZE, 32, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(30, 30, 30)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnSave, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnCancel, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(26, 26, 26))
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveActionPerformed
        int qty;
        int lowAlert;
        
        try {
            qty = Integer.parseInt(txtQuantity.getText().trim());
            lowAlert = Integer.parseInt(txtLowAlert.getText().trim());
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Please enter valid numbers.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (qty <= 0) {
            JOptionPane.showMessageDialog(this, "Quantity must be greater than 0.", "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String action = cmbAction.getSelectedItem().toString();
        int newStock = currentStock;
        
        if (action.equals("Stock In")) {
            newStock += qty;
        } else if (action.equals("Stock Out")) {
            if (qty > currentStock) {
                JOptionPane.showMessageDialog(this, "Cannot remove more than current stock.", "Stock Error", JOptionPane.WARNING_MESSAGE);
                return;
            }
            newStock -= qty;
        }

        String newStatus = calculateStatus(newStock, lowAlert);
        String newAvailability = calculateAvailability(newStock, action);

        Connection conn = null;
        try {
            conn = MySqlConnector.getConnection();
            conn.setAutoCommit(false);
            
            String updateSql = "UPDATE products SET stock=?, low_stock_alert=?, status=?, availability=? WHERE prod_id=?";
            try (PreparedStatement pstUpdate = conn.prepareStatement(updateSql)) {
                pstUpdate.setInt(1, newStock);
                pstUpdate.setInt(2, lowAlert);
                pstUpdate.setString(3, newStatus);
                pstUpdate.setString(4, newAvailability);
                pstUpdate.setInt(5, prodId);
                pstUpdate.executeUpdate();
            }
            
            String logSql = "INSERT INTO activity_log (user_id, product_name, quantity, action_type, reason, log_time) VALUES (?, ?, ?, ?, ?, NOW())";
            try (PreparedStatement pstLog = conn.prepareStatement(logSql)) {
                String reason = cmbReason.getSelectedItem().toString();
                pstLog.setInt(1, userId);
                pstLog.setString(2, productName);
                pstLog.setInt(3, qty);
                pstLog.setString(4, action);
                pstLog.setString(5, reason);
                pstLog.executeUpdate();
            }
            
            conn.commit();
            
            JOptionPane.showMessageDialog(this, "Stock Updated Successfully!");
            
            if (parentDashboard != null) {
                parentDashboard.refreshData();
            }
            
            this.dispose();
            
        } catch (SQLException e) {
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) { logger.log(Level.SEVERE, "Rollback failed: ", ex); }
            logger.log(Level.SEVERE, "Database Error: ", e);
            JOptionPane.showMessageDialog(this, "Database Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            try { if (conn != null) conn.close(); } catch (SQLException e) { logger.log(Level.WARNING, "Connection close failed: ", e); }
        }
    }//GEN-LAST:event_btnSaveActionPerformed

    private void btnCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelActionPerformed
        this.dispose();
    }//GEN-LAST:event_btnCancelActionPerformed

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(() -> new StockAction(0, 0, null).setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCancel;
    private javax.swing.JButton btnSave;
    private javax.swing.JComboBox<String> cmbAction;
    private javax.swing.JComboBox<String> cmbReason;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JTextField txtCstock;
    private javax.swing.JTextField txtLowAlert;
    private javax.swing.JTextField txtPname;
    private javax.swing.JTextField txtQuantity;
    // End of variables declaration//GEN-END:variables
}
