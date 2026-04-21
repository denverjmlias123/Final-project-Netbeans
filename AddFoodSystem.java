package groupprojectexe;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

public class AddFoodSystem extends javax.swing.JFrame {
    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(AddFoodSystem.class.getName());
    
    private final int userId;
    private final DashboardInventory parentDashboard;
    private final String ADD_NEW_OPTION = "++ Add New Category ++";

    public AddFoodSystem(int userId, DashboardInventory parent) {
        this.userId = userId;
        this.parentDashboard = parent;
        
        initComponents();
        setLocationRelativeTo(null);
        
        setupInitialState();
    }
    private void setupInitialState() {
        // Populate Status Combo
        cmbSelectStatus.removeAllItems();
        cmbSelectStatus.addItem("Available");
        cmbSelectStatus.addItem("Unavailable");

        // Load existing categories
        loadCategories();

        // Listener for Category Combo Box
        cmbSelectCategory.addActionListener(e -> {
            if (cmbSelectCategory.getSelectedItem() == null) return;
            
            String selected = cmbSelectCategory.getSelectedItem().toString();
            if (selected.equals(ADD_NEW_OPTION)) {
                switchToAddMode();
            }
        });

        // Set initial UI state
        txtAddCategory.setEnabled(false);
        cmbSelectCategory.setEnabled(true);
        btnAddCategory.setText("Add New Category");
    }      
    private void loadCategories() {
        cmbSelectCategory.removeAllItems();
        boolean hasItems = false;

        String sql = "SELECT DISTINCT category FROM products WHERE user_id = ?";
        
        try (Connection conn = MySqlConnector.getConnection();
             PreparedStatement pst = conn.prepareStatement(sql)) {
            
            pst.setInt(1, userId);
            try (var rs = pst.executeQuery()) {
                while (rs.next()) {
                    String cat = rs.getString("category");
                    if (cat != null && !cat.trim().isEmpty()) {
                        cmbSelectCategory.addItem(cat);
                        hasItems = true;
                    }
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error loading categories: ", e);
            JOptionPane.showMessageDialog(this, "Error loading categories: " + e.getMessage());
        }

        cmbSelectCategory.addItem(ADD_NEW_OPTION);
        cmbSelectCategory.setSelectedItem(hasItems ? cmbSelectCategory.getItemAt(0) : ADD_NEW_OPTION);
    }
    private void switchToAddMode() {
        txtAddCategory.setEnabled(true);
        txtAddCategory.setText("");
        txtAddCategory.requestFocus();
        cmbSelectCategory.setEnabled(false);
        btnAddCategory.setText("Select Existing");
    }
    private void switchToSelectMode() {
        txtAddCategory.setEnabled(false);
        txtAddCategory.setText("");
        cmbSelectCategory.setEnabled(true);
        if (cmbSelectCategory.getItemCount() > 0) {
            cmbSelectCategory.setSelectedIndex(0);
        }
        btnAddCategory.setText("Add New Category");
    }
    private String determineCategory() {
        if (txtAddCategory.isEnabled() && !txtAddCategory.getText().trim().isEmpty()) {
            return txtAddCategory.getText().trim();
        } else if (cmbSelectCategory.isEnabled() && cmbSelectCategory.getSelectedItem() != null) {
            String selected = cmbSelectCategory.getSelectedItem().toString();
            if (!selected.equals(ADD_NEW_OPTION)) {
                return selected;
            }
        }
        return "Uncategorized";
    }
    private void clearForm() {
        txtAddProduct.setText("");
        txtEnterPrice.setText("");
        txtAddCategory.setText("");
        if (txtAddCategory.isEnabled()) {
            switchToSelectMode();
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        txtAddProduct = new javax.swing.JTextField();
        txtAddCategory = new javax.swing.JTextField();
        btnAddCategory = new javax.swing.JButton();
        jLabel10 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        txtEnterPrice = new javax.swing.JTextField();
        cmbSelectCategory = new javax.swing.JComboBox<>();
        jLabel12 = new javax.swing.JLabel();
        cmbSelectStatus = new javax.swing.JComboBox<>();
        btnSave = new javax.swing.JButton();
        btnCancel = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setBackground(new java.awt.Color(20, 200, 130));

        jPanel2.setBackground(new java.awt.Color(15, 25, 35));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 20)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setText("Add Food Product Sale");

        jLabel4.setFont(new java.awt.Font("Segoe UI", 1, 10)); // NOI18N
        jLabel4.setForeground(new java.awt.Color(255, 255, 255));
        jLabel4.setText("------------------------------------------------------------------------------------------");

        jLabel8.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel8.setForeground(new java.awt.Color(255, 255, 255));
        jLabel8.setText("Enter Product Name:");

        jLabel3.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(255, 255, 255));
        jLabel3.setText("▣");

        jLabel9.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel9.setForeground(new java.awt.Color(255, 255, 255));
        jLabel9.setText("Select Category Name:");

        txtAddCategory.setBackground(new java.awt.Color(204, 204, 204));

        btnAddCategory.setBackground(new java.awt.Color(204, 204, 204));
        btnAddCategory.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        btnAddCategory.setText("+");
        btnAddCategory.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddCategoryActionPerformed(evt);
            }
        });

        jLabel10.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel10.setForeground(new java.awt.Color(255, 255, 255));
        jLabel10.setText("Enter New Category Name:");

        jLabel11.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel11.setForeground(new java.awt.Color(255, 255, 255));
        jLabel11.setText("Enter Selling Price:");

        jLabel12.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel12.setForeground(new java.awt.Color(255, 255, 255));
        jLabel12.setText("Status:");

        btnSave.setBackground(new java.awt.Color(204, 204, 204));
        btnSave.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnSave.setText("Save");
        btnSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSaveActionPerformed(evt);
            }
        });

        btnCancel.setBackground(new java.awt.Color(204, 204, 204));
        btnCancel.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnCancel.setText("Cancel");
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
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabel4))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(37, 37, 37)
                        .addComponent(jLabel3)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel1))
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                            .addGap(35, 35, 35)
                            .addComponent(btnSave, javax.swing.GroupLayout.PREFERRED_SIZE, 135, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(btnCancel, javax.swing.GroupLayout.PREFERRED_SIZE, 135, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jPanel2Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(cmbSelectStatus, javax.swing.GroupLayout.PREFERRED_SIZE, 304, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel2Layout.createSequentialGroup()
                                .addGap(27, 27, 27)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel11)
                                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(jLabel8)
                                        .addComponent(jLabel9)
                                        .addComponent(jLabel10)
                                        .addGroup(jPanel2Layout.createSequentialGroup()
                                            .addGap(10, 10, 10)
                                            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                .addComponent(txtAddProduct, javax.swing.GroupLayout.DEFAULT_SIZE, 304, Short.MAX_VALUE)
                                                .addGroup(jPanel2Layout.createSequentialGroup()
                                                    .addGap(252, 252, 252)
                                                    .addComponent(btnAddCategory, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE))
                                                .addComponent(cmbSelectCategory, javax.swing.GroupLayout.PREFERRED_SIZE, 245, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addComponent(txtAddCategory))))
                                    .addComponent(jLabel12)
                                    .addGroup(jPanel2Layout.createSequentialGroup()
                                        .addGap(10, 10, 10)
                                        .addComponent(txtEnterPrice, javax.swing.GroupLayout.PREFERRED_SIZE, 304, javax.swing.GroupLayout.PREFERRED_SIZE)))))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel4)
                .addGap(18, 18, 18)
                .addComponent(jLabel8)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtAddProduct, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel9)
                .addGap(8, 8, 8)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(btnAddCategory, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cmbSelectCategory))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel10)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtAddCategory, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel11)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtEnterPrice, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel12)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cmbSelectStatus, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(42, 42, 42)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnSave, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnCancel, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(38, 38, 38))
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
                .addContainerGap()
                .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 497, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void btnAddCategoryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnAddCategoryActionPerformed
        if (txtAddCategory.isEnabled()) {
            switchToSelectMode();
        } else {
            switchToAddMode();
        }
    }//GEN-LAST:event_btnAddCategoryActionPerformed

    private void btnSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSaveActionPerformed
        String name = txtAddProduct.getText().trim();
        String priceStr = txtEnterPrice.getText().trim();

        // 1. Determine Category
        String category = "";
        
        if (txtAddCategory.isEnabled() && !txtAddCategory.getText().trim().isEmpty()) {
            category = txtAddCategory.getText().trim();
        } else if (cmbSelectCategory.isEnabled() && cmbSelectCategory.getSelectedItem() != null) {
            String selected = cmbSelectCategory.getSelectedItem().toString();
            if (!selected.equals(ADD_NEW_OPTION)) {
                category = selected;
            }
        }
        
        if (category.isEmpty()) {
            category = "Uncategorized";
        }

        // 2. Validation
        if (name.isEmpty() || priceStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in Name and Price.");
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Price must be a valid number.");
            return;
        }

        // 3. Database Transaction
        Connection conn = null;
        try {
            conn = MySqlConnector.getConnection();
            conn.setAutoCommit(false); // Start Transaction

            // A. Insert Product
            // FIX: Removed extra '?' placeholder. 
            // Columns: user_id, name, category, stock, price, availability, status, low_stock_alert
            // Values:  ?(1),   ?(2),    ?(3),     0,    ?(4),       ?(5),        ?(6),      25
            String productSql = "INSERT INTO products (user_id, name, category, stock, price, availability, status, low_stock_alert) VALUES (?, ?, ?, 0, ?, ?, ?, 25)";
            
            try (PreparedStatement pstProduct = conn.prepareStatement(productSql)) {
                pstProduct.setInt(1, userId);
                pstProduct.setString(2, name);
                pstProduct.setString(3, category);
                pstProduct.setDouble(4, price);
                pstProduct.setString(5, "Unavailable"); 
                pstProduct.setString(6, "Out of Stock");
                pstProduct.executeUpdate();
            }

            // B. Insert Activity Log
            String logSql = "INSERT INTO activity_log (user_id, product_name, quantity, action_type, reason, log_time) VALUES (?, ?, ?, ?, ?, NOW())";
            try (PreparedStatement pstLog = conn.prepareStatement(logSql)) {
                pstLog.setInt(1, userId);
                pstLog.setString(2, name);
                pstLog.setInt(3, 0);
                pstLog.setString(4, "Product Added");
                pstLog.setString(5, "New product initialized");
                pstLog.executeUpdate();
            }

            conn.commit(); // Commit Transaction
            
            JOptionPane.showMessageDialog(this, "Product Added Successfully!");
            
            // Reset Form
            txtAddProduct.setText("");
            txtEnterPrice.setText("");
            txtAddCategory.setText("");
            
            // Refresh Parent Dashboard
            if (parentDashboard != null) {
                parentDashboard.refreshData();
            }
            
            this.dispose();

        } catch (SQLException e) {
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                logger.log(Level.SEVERE, "Rollback failed: ", ex);
            }
            logger.log(Level.SEVERE, "Database Error: ", e);
            JOptionPane.showMessageDialog(this, "Error adding product: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        } finally {
            try {
                if (conn != null) conn.close();
            } catch (SQLException e) {
                logger.log(Level.WARNING, "Connection close failed: ", e);
            }
        }
    }//GEN-LAST:event_btnSaveActionPerformed

    private void btnCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCancelActionPerformed
        this.dispose();
    }//GEN-LAST:event_btnCancelActionPerformed

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(() -> new AddFoodSystem(0, null).setVisible(true));
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAddCategory;
    private javax.swing.JButton btnCancel;
    private javax.swing.JButton btnSave;
    private javax.swing.JComboBox<String> cmbSelectCategory;
    private javax.swing.JComboBox<String> cmbSelectStatus;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JTextField txtAddCategory;
    private javax.swing.JTextField txtAddProduct;
    private javax.swing.JTextField txtEnterPrice;
    // End of variables declaration//GEN-END:variables
}
