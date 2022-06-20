package application;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.stage.Stage;
import javafx.util.converter.IntegerStringConverter;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.RoundingMode;
import java.net.Socket;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;


public class ShoppingCartController {
    @FXML
    private TextField quantTField;
    @FXML
    private TextField priorTField;
    @FXML
    private TextField budgetTField;
    @FXML
    private TextField itemTField;
    @FXML
    private TextField priceTField;

    private String username;
    private static ObjectOutputStream out;
    private static ObjectInputStream in;

    private final NumberFormat currency = NumberFormat.getCurrencyInstance();

    private ObservableList<ShoppingItem> shoppingList;
    @FXML private TableView<ShoppingItem> tableView;
    @FXML private TableColumn<ShoppingItem, Integer> priorityColumn;
    @FXML private TableColumn<ShoppingItem, String> itemColumn;
    @FXML private TableColumn<ShoppingItem, Integer> quantityColumn;
    @FXML private TableColumn<ShoppingItem, Double> priceColumn;

    Alert invalidItemAlert = new Alert(AlertType.NONE);
    Alert invalidCheckoutAlert = new Alert(AlertType.NONE);
    Alert purchasedItemsAlert = new Alert(AlertType.NONE);
    Alert invalidDeletionAlert = new Alert(AlertType.NONE);

    public void initialize() {
        currency.setRoundingMode(RoundingMode.HALF_UP);

        priorityColumn.setCellValueFactory(new PropertyValueFactory<>("priority"));
        priorityColumn.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        priorityColumn.setOnEditCommit(event -> {
            try {
                int priority = event.getNewValue();
                if (priority < 1)
                    throw new IllegalArgumentException();
                ShoppingItem updatedItem = event.getTableView().getItems().get(event.getTablePosition().getRow());
                updatedItem.setPriority(priority);
                sendData("updateItem", updatedItem);
                shoppingList.sort(null);
            } catch (IllegalArgumentException e) {
                tableView.refresh();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        itemColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        quantityColumn.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        priceColumn.setCellFactory(param -> new TableCell<>() {
            @Override
            public void updateItem(Double price, boolean empty) {
                if (empty || price == null)
                    setText(null);
                else
                    setText(currency.format(price));
            }
        });
    }

    public void initializeCart(String username, ObjectOutputStream out, ObjectInputStream in) {
        this.username = username;
        this.out = out;
        this.in = in;

        try {
            sendData("getItems", username);
            List<ShoppingItem> returnedList = (List<ShoppingItem>) in.readObject();
            returnedList.sort(null);
            shoppingList = FXCollections.observableArrayList(returnedList);
            tableView.setItems(shoppingList);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void initializeCart() {
        try {
            sendData("getItems", this.username);
            ArrayList<ShoppingItem> returnedList = (ArrayList<ShoppingItem>) in.readObject();
            returnedList.sort(null);
            shoppingList = FXCollections.observableArrayList(returnedList);
            tableView.setItems(shoppingList);
            tableView.refresh();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void sendData(String command, Object obj) throws IOException
    {
        out.writeObject(new Request(command, obj));
    }

    @FXML
    public void increaseQuantity() {
        int quantity = Integer.parseInt( quantTField.getText());
        quantity++;
        quantTField.setText( "" + quantity );
    }

    @FXML
    public void decreaseQuantity() {
        int quantity = Integer.parseInt( quantTField.getText() );
        if (quantity == 0) return;
        quantity--;
        quantTField.setText("" + quantity);
    }

    @FXML
    public void increasePriority() {
        int priority = Integer.parseInt( priorTField.getText() );
        priority++;
        priorTField.setText("" + priority);
    }

    @FXML
    public void decreasePriority() {
        int priority = Integer.parseInt(priorTField.getText());
        if (priority == 0) return;
        priority--;
        priorTField.setText("" + priority);
    }

    @FXML
    private void addToCart() {
        try {
            String name = itemTField.getText();
            double price = Double.parseDouble(priceTField.getText());
            int quantity = Integer.parseInt(quantTField.getText());
            int priority = Integer.parseInt(priorTField.getText());
            ShoppingItem newItem = new ShoppingItem(username, name, price, quantity, priority);

            sendData("addItem", newItem);
            Integer result = (Integer) in.readObject();
            if (result == 0)
                throw new IllegalArgumentException("This item is already in your shopping list.");

            newItem.setId(result);
            shoppingList.add(newItem);
            shoppingList.sort(null);
            tableView.refresh();

            itemTField.setText("");
            priceTField.setText("");
            quantTField.setText("0");
            priorTField.setText("0");
        } catch (NumberFormatException e) {
            String warning = "Please enter numeric values for Price, Quantity, and Priority\n" +
                    "Price may be a decimal number";
            invalidItemAlert(warning);
        } catch (IllegalArgumentException e) {
            invalidItemAlert(e.getMessage());
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void checkout() {
        try {
            Double budget = Double.parseDouble(budgetTField.getText());

            if (budget < 0.0)
                throw new IllegalArgumentException("Your budget cannot be negative.");
            if (shoppingList.isEmpty())
                throw new IllegalStateException("Your shopping list is empty");

            Object[] shoppingData = new Object[] {username, budget};
            sendData("goShopping", shoppingData);
            ArrayList<ShoppingItem> purchasedItems = (ArrayList<ShoppingItem>) in.readObject();

            if(!purchasedItems.isEmpty()) {
                purchasedItemsAlert(purchasedItems);
                initializeCart();
            }

        } catch (NumberFormatException e) {
            invalidCheckoutAlert("Please enter a numeric value for the Budget.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            invalidCheckoutAlert(e.getMessage());
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void deleteItem() {
        ShoppingItem item = tableView.getSelectionModel().getSelectedItem();
        if (item == null) {
            invalidDeletionAlert("No item was selected.");
            return;
        }
        try {
            sendData("deleteItem", item);
            shoppingList.remove(item);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void clearList() {
        if (shoppingList.isEmpty()) {
            invalidDeletionAlert("The shopping list is empty.");
            return;
        }
        try {
            sendData("clearList", username);
            shoppingList.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void logout(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("LoginPage.fxml"));
            Parent root = loader.load();

            SceneController controller = loader.getController();
            controller.setConnection(out, in);

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void invalidItemAlert(String message) {
        invalidItemAlert.setAlertType(AlertType.INFORMATION);
        invalidItemAlert.setTitle("Invalid Item");
        invalidItemAlert.setContentText(message);
        invalidItemAlert.setHeaderText("Invalid Item");
        invalidItemAlert.show();
        System.out.println("Invalid Item");
    }

    private void invalidCheckoutAlert(String message) {
        invalidCheckoutAlert.setAlertType(AlertType.INFORMATION);
        invalidCheckoutAlert.setTitle("Invalid Checkout");
        invalidCheckoutAlert.setContentText(message);
        invalidCheckoutAlert.setHeaderText("Invalid Checkout");
        invalidCheckoutAlert.show();
        System.out.println("Invalid Checkout");
    }

    private void purchasedItemsAlert(List<ShoppingItem> purchasedItems) {
        String header = "You purchased the following items:\n\n" +
                "Item (quantity) (price)\n\n";
        String footer = "Any items that were not purchased are saved in your shopping list.";

        StringBuilder purchasedMessage = new StringBuilder(header);
        for (ShoppingItem item : purchasedItems)
            purchasedMessage.append("- " + item + " " + "\n");
        purchasedMessage.append("\n" + footer);

        purchasedItemsAlert.setAlertType(AlertType.INFORMATION);
        purchasedItemsAlert.setTitle("Checkout Complete");
        purchasedItemsAlert.setContentText(purchasedMessage.toString());
        purchasedItemsAlert.setHeaderText("Checkout Complete");
        purchasedItemsAlert.show();
        System.out.println("Purchase Complete");
    }

    private void invalidDeletionAlert(String message) {
        invalidDeletionAlert.setAlertType(AlertType.INFORMATION);
        invalidDeletionAlert.setTitle("Cannot Delete Item(s)");
        invalidDeletionAlert.setContentText(message);
        invalidDeletionAlert.setHeaderText("Cannot Delete Item(s)");
        invalidDeletionAlert.show();
        System.out.println("Invalid Deletion");
    }
}
