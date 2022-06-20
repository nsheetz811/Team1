package application;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.sql.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ClientHandler implements Runnable {
    private static ObjectOutputStream nOut;
    private static ObjectInputStream nIn;
    private static Socket ss;
    private static Request request;
    private static Connection c = null;
    private static Statement stmt = null;

    public ClientHandler(Socket clientSocket) throws IOException {
        ss=clientSocket;
        nOut=new ObjectOutputStream(ss.getOutputStream());
        nIn=new ObjectInputStream(ss.getInputStream());
    }

    @Override
    public void run() {
        try{
            dbc();

            while(true)
            {
                System.out.println("[CLIENT HANDLER]: Ready for new command");
                request = (Request) nIn.readObject();
                processRequest(request);
                System.out.println("[CLIENT HANDLER]: Processed a " + request.command + " request");
            }
/*
            ss.close();
            nIn.close();
            nOut.close();
            c.close();*/

        } catch(IOException | ClassNotFoundException e)
        {
            e.printStackTrace();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    private void processRequest(Request r) throws IOException, SQLException, ClassNotFoundException {
        /*
            Behavior:
                Switch statement determines how to process request based on supplied command field
         */
        switch (r.command)
        {
            case "addUser":
                writeUser((String[]) r.payload);
                break;
            case "getUser":
                readUser((String[]) r.payload);
                break;
            case "addItem":
                writeItem((ShoppingItem) r.payload);
                break;
            case "updateItem":
                updateItem((ShoppingItem) r.payload);
                break;
            case "getItems":
                readItems((String) r.payload);
                break;
            case "deleteItem":
                deleteItem((ShoppingItem) r.payload);
                break;
            case "clearList":
                clearList((String) r.payload);
                break;
            case "goShopping":
                goShopping((Object[]) r.payload);
                break;
            default:
                break;
        }
    }

    private void writeUser(String[] userData) throws SQLException {
        stmt = c.createStatement();
        String query = "SELECT * FROM users WHERE username='" + userData[0] + "'";
        ResultSet rs = stmt.executeQuery(query);
        try {
            if (!rs.next()) {
                query = "INSERT INTO users (username, password) VALUES ('" + userData[0] + "', '" + userData[1] + "')";
                stmt.executeUpdate(query);
                c.commit();
                nOut.writeObject(Boolean.valueOf(true));
            } else {
                nOut.writeObject(Boolean.valueOf(false));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void readUser(String[] userData) throws SQLException, IOException {
        stmt = c.createStatement();
        String query = "SELECT * FROM users WHERE username='" + userData[0] + "' AND password='" + userData[1] + "'";
        ResultSet rs = stmt.executeQuery(query);

        if (rs.next()) {
            nOut.writeObject(Boolean.valueOf(true));
        } else {
            nOut.writeObject(Boolean.valueOf(false));
        }
    }

    private void writeItem(ShoppingItem item) throws SQLException {
        String username = item.getUsername();
        String name = item.getName();

        stmt = c.createStatement();
        String query = "SELECT * FROM items WHERE username='" + username + "' AND item_name='" + name + "'";
        ResultSet rs = stmt.executeQuery(query);

        try {
            if (!rs.next()) {
                query = "INSERT INTO items (item_name, item_quantity, item_cost, item_priority, username) " +
                        "VALUES (?, ?, ?, ?, ?) RETURNING item_id";
                PreparedStatement pstmt = c.prepareStatement(query);
                pstmt.setObject(1, item.getName());
                pstmt.setObject(2, item.getQuantity());
                pstmt.setObject(3, item.getPrice());
                pstmt.setObject(4, item.getPriority());
                pstmt.setObject(5, item.getUsername());
                pstmt.execute();
                c.commit();
                ResultSet rsid = pstmt.getResultSet();
                rsid.next();
                int newId = rsid.getInt(1);
                nOut.writeObject(Integer.valueOf(newId));
            } else {
                nOut.writeObject(Integer.valueOf(0));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateItem(ShoppingItem item) throws SQLException {
        stmt = c.createStatement();
        String query = "UPDATE items SET item_cost=" + item.getPrice() + ", item_quantity=" + item.getQuantity() +
                ", item_priority=" + item.getPriority() + " WHERE item_id=" + item.getId();
        stmt.executeUpdate(query);
        c.commit();
        System.out.println("update" + item);
    }

    private void readItems(String username) throws SQLException, IOException {
        stmt = c.createStatement();
        String query = "SELECT * FROM items WHERE username='" + username + "'";
        ResultSet rs = stmt.executeQuery(query);

        List<ShoppingItem> items = new ArrayList<>();

        while (rs.next()) {
            ShoppingItem item = new ShoppingItem(rs.getInt("item_id"), rs.getString("username"),
                    rs.getString("item_name"), rs.getDouble("item_cost"),
                    rs.getInt("item_quantity"), rs.getInt("item_priority"));
            items.add(item);
        }

        nOut.writeObject(items);
    }

    private List<ShoppingItem> readItemsInternal(String username) throws SQLException, IOException {
        stmt = c.createStatement();
        String query = "SELECT * FROM items WHERE username='" + username + "' ORDER BY item_priority";
        ResultSet rs = stmt.executeQuery(query);

        List<ShoppingItem> items = new ArrayList<>();

        while (rs.next()) {
            ShoppingItem item = new ShoppingItem(rs.getInt("item_id"), rs.getString("username"),
                    rs.getString("item_name"), rs.getDouble("item_cost"),
                    rs.getInt("item_quantity"), rs.getInt("item_priority"));
            items.add(item);
        }

        return items;
    }

    private void deleteItem(ShoppingItem item) throws SQLException {
        stmt = c.createStatement();
        String query = "DELETE FROM items WHERE item_id=" + item.getId();
        stmt.executeUpdate(query);
        c.commit();
        System.out.println("delete:" + item);
    }

    private void clearList(String username) throws SQLException {
        stmt = c.createStatement();
        String query = "DELETE FROM items WHERE username='" + username + "'";
        stmt.executeUpdate(query);
        c.commit();
    }

    private void goShopping(Object[] shoppingData) throws SQLException, IOException {
        List<ShoppingItem> shoppingList = readItemsInternal((String) shoppingData[0]);
        Double budget = (Double) shoppingData[1];

        Object[] shoppingResults = ShoppingBudget.goShopping(shoppingList, budget);
        shoppingList = (ArrayList<ShoppingItem>) shoppingResults[0];
        List<ShoppingItem> purchasedItems = (List<ShoppingItem>) shoppingResults[1];

        for (ShoppingItem item : shoppingList) {
            if (item.getQuantity() == 0) deleteItem(item);
            else updateItem(item);
        }

        nOut.writeObject(purchasedItems);
    }

    private static void dbc()
    {
        try {
            Class.forName("org.postgresql.Driver");
            c = DriverManager
                    .getConnection("jdbc:postgresql://shopping-db.cryrvdf1mysl.us-east-1.rds.amazonaws.com:5432/shopping","postgres","password");
            c.setAutoCommit(false);
            System.out.println("[CLIENT HANDLER]: Opened database successfully");

        } catch ( Exception e ) {
            System.err.println( e.getClass().getName()+": "+ e.getMessage() );
            System.exit(0);
        }
    }
}
