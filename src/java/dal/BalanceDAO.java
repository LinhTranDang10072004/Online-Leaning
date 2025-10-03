package dal;

import model.BalanceDTOSeller;
import model.User;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BalanceDAO extends DBContext {
    
    //tong so tien da nhan
    public double getBalance(int createdBy) {
        String sql = "SELECT SUM(CAST(p.Amount AS FLOAT)) as TotalBalance " +
                     "FROM [Order] o " +
                     "JOIN Payment p ON o.Order_Id = p.Order_Id " +
                     "JOIN Order_Detail od ON o.Order_Id = od.Order_Id " +
                     "JOIN Course c ON od.Course_Id = c.Course_Id " +
                     "WHERE c.Created_By = ? AND p.Payment_Status = 'completed'";
        try (PreparedStatement stm = connection.prepareStatement(sql)) {
            stm.setInt(1, createdBy);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                return rs.getDouble("TotalBalance");
            }
        } catch (SQLException ex) {
            java.util.logging.Logger.getLogger(BalanceDAO.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        return 0.0;
    }

    //lay danh sach giao dich
    public List<BalanceDTOSeller> getTransactions(int createdBy, String status, String fromDate, String toDate, String searchTerm, int page, int pageSize) {
        List<BalanceDTOSeller> transactions = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT o.Order_Id, o.Created_At AS OrderDate, c.Course_Id, c.Title AS CourseName, p.Amount, o.Status AS Payment_Status, p.Payment_Method " +
            "FROM [Order] o " +
            "JOIN Payment p ON o.Order_Id = p.Order_Id " +
            "JOIN Order_Detail od ON o.Order_Id = od.Order_Id " +
            "JOIN Course c ON od.Course_Id = c.Course_Id " +
            "WHERE c.Created_By = ?"
        );
        List<Object> parameters = new ArrayList<>();
        parameters.add(createdBy);

        if (status != null && !status.trim().isEmpty() && !status.equals("all")) {
            sql.append(" AND o.Status = ?");
            parameters.add(status);
        }
        if (fromDate != null && !fromDate.isEmpty()) {
            sql.append(" AND o.Created_At >= ?");
            parameters.add(fromDate + " 00:00:00");
        }
        if (toDate != null && !toDate.isEmpty()) {
            sql.append(" AND o.Created_At <= ?");
            parameters.add(toDate + " 23:59:59");
        }
        if (searchTerm != null && !searchTerm.isEmpty()) {
            sql.append(" AND ('Course Sale: ' + c.Title + ' (Order #' + CAST(o.Order_Id AS NVARCHAR(10)) + ')') LIKE ?");
            parameters.add("%" + searchTerm + "%");
        }
        sql.append(" ORDER BY CASE WHEN o.Status = 'cancelled' THEN 1 ELSE 0 END, o.Created_At DESC");
        sql.append(" OFFSET ? ROWS FETCH NEXT ? ROWS ONLY");
        try (PreparedStatement stm = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < parameters.size(); i++) {
                stm.setObject(i + 1, parameters.get(i));
            }
            stm.setInt(parameters.size() + 1, (page - 1) * pageSize);
            stm.setInt(parameters.size() + 2, pageSize);
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                transactions.add(new BalanceDTOSeller(
                    rs.getInt("Order_Id"),
                    rs.getTimestamp("OrderDate"),
                    rs.getInt("Course_Id"),
                    rs.getString("CourseName"),
                    rs.getDouble("Amount"),
                    rs.getString("Payment_Status"),
                    rs.getString("Payment_Method")
                ));
            }
        } catch (SQLException ex) {
            java.util.logging.Logger.getLogger(BalanceDAO.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        return transactions;
    }
    
    //tong so giao dich

    public int getTransactionCount(int createdBy, String status, String fromDate, String toDate, String searchTerm) {
        StringBuilder sql = new StringBuilder(
            "SELECT COUNT(*) AS Total " +
            "FROM [Order] o " +
            "JOIN Payment p ON o.Order_Id = p.Order_Id " +
            "JOIN Order_Detail od ON o.Order_Id = od.Order_Id " +
            "JOIN Course c ON od.Course_Id = c.Course_Id " +
            "WHERE c.Created_By = ?"
        );
        List<Object> parameters = new ArrayList<>();
        parameters.add(createdBy);

        if (status != null && !status.trim().isEmpty() && !status.equals("all")) {
            sql.append(" AND o.Status = ?");
            parameters.add(status);
        }
        if (fromDate != null && !fromDate.isEmpty()) {
            sql.append(" AND o.Created_At >= ?");
            parameters.add(fromDate + " 00:00:00");
        }
        if (toDate != null && !toDate.isEmpty()) {
            sql.append(" AND o.Created_At <= ?");
            parameters.add(toDate + " 23:59:59");
        }
        if (searchTerm != null && !searchTerm.isEmpty()) {
            sql.append(" AND ('Course Sale: ' + c.Title + ' (Order #' + CAST(o.Order_Id AS NVARCHAR(10)) + ')') LIKE ?");
            parameters.add("%" + searchTerm + "%");
        }
        try (PreparedStatement stm = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < parameters.size(); i++) {
                stm.setObject(i + 1, parameters.get(i));
            }
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                return rs.getInt("Total");
            }
        } catch (SQLException ex) {
            java.util.logging.Logger.getLogger(BalanceDAO.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        return 0;
    }
    
    // lay giao dich cu ther cho detail

    public BalanceDTOSeller getTransactionByOrderId(long orderId, int createdBy) {
        String sql = "SELECT o.Order_Id, o.Created_At AS OrderDate, c.Course_Id, c.Title AS CourseName, p.Amount, o.Status AS Payment_Status, p.Payment_Method " +
                     "FROM [Order] o " +
                     "JOIN Payment p ON o.Order_Id = p.Order_Id " +
                     "JOIN Order_Detail od ON o.Order_Id = od.Order_Id " +
                     "JOIN Course c ON od.Course_Id = c.Course_Id " +
                     "WHERE o.Order_Id = ? AND c.Created_By = ?";
        try (PreparedStatement stm = connection.prepareStatement(sql)) {
            stm.setLong(1, orderId);
            stm.setInt(2, createdBy);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                return new BalanceDTOSeller(
                    rs.getInt("Order_Id"),
                    rs.getTimestamp("OrderDate"),
                    rs.getInt("Course_Id"),
                    rs.getString("CourseName"),
                    rs.getDouble("Amount"),
                    rs.getString("Payment_Status"),
                    rs.getString("Payment_Method")
                );
            }
        } catch (SQLException ex) {
            java.util.logging.Logger.getLogger(BalanceDAO.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        return null;
    }

    // lay thong tin nguoi mua
    public User getBuyerByOrderId(long orderId) {
        String sql = "SELECT u.UserID, u.FirstName, u.MiddleName, u.LastName, u.Email, u.Phone, u.Address " +
                     "FROM [Order] o " +
                     "JOIN Users u ON o.User_Id = u.UserID " +
                     "WHERE o.Order_Id = ?";
        try (PreparedStatement stm = connection.prepareStatement(sql)) {
            stm.setLong(1, orderId);
            ResultSet rs = stm.executeQuery();
            if (rs.next()) {
                return new User(
                    rs.getLong("UserID"),
                    rs.getString("FirstName"),
                    rs.getString("MiddleName"),
                    rs.getString("LastName"),
                    null,
                    rs.getString("Phone"),
                    rs.getString("Address"),
                    rs.getString("Email"),
                    null,
                    null,
                    null,
                    null,
                    null
                );
            }
        } catch (SQLException ex) {
            java.util.logging.Logger.getLogger(BalanceDAO.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        return null;
    }

    // chuyenen tu completed thanh cancelleed 
    public boolean updatePaymentStatus(long orderId, String status) {
        // Restrict to 'completed' or 'cancelled' to avoid CHECK constraint
        if (!status.equals("completed") && !status.equals("cancelled")) {
            return false;
        }
        String sql = "UPDATE Payment SET Payment_Status = ?, Payment_Date = GETDATE() WHERE Order_Id = ?;" +
                     "UPDATE [Order] SET Status = ? WHERE Order_Id = ?";
        try (PreparedStatement stm = connection.prepareStatement(sql)) {
            stm.setString(1, status);
            stm.setLong(2, orderId);
            stm.setString(3, status);
            stm.setLong(4, orderId);
            int rowsAffected = stm.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException ex) {
            java.util.logging.Logger.getLogger(BalanceDAO.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
            return false;
        }
    }

    //lay cac trang thai khac nhau cua order
    public List<String> getPaymentStatuses() {
        List<String> statuses = new ArrayList<>();
        String sql = "SELECT DISTINCT Status FROM [Order] WHERE Status IS NOT NULL ORDER BY Status";
        try (PreparedStatement stm = connection.prepareStatement(sql)) {
            ResultSet rs = stm.executeQuery();
            while (rs.next()) {
                statuses.add(rs.getString("Status"));
            }
        } catch (SQLException ex) {
            java.util.logging.Logger.getLogger(BalanceDAO.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        return statuses;
    }
}