package application;

import application.ShoppingItem;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ShoppingBudget {

    public static Object[] goShopping(List<ShoppingItem> items, Double budget) {

        List<ShoppingItem> purchasedItems = new ArrayList<>();
        System.out.println("Starting goShopping:");
        for (ShoppingItem item : items) {
            if (budget >= (item.getPrice() * item.getQuantity())) {
                ShoppingItem purchasedItem = new ShoppingItem(item);
                purchasedItems.add(purchasedItem);
                item.setQuantity(0);
                budget -= item.getPrice();
            } else if (budget >= item.getPrice()) {
                ShoppingItem purchasedItem = new ShoppingItem(item);
                int purchasedQuantity = (int) (budget / item.getPrice());
                int remainingQuantity = item.getQuantity() - purchasedQuantity;
                purchasedItem.setQuantity(purchasedQuantity);
                item.setQuantity(remainingQuantity);
                purchasedItems.add(purchasedItem);
                break;
            } else break;
        }
        Object[] shoppingResults = new Object[] {items, purchasedItems};
        return shoppingResults;
    }
}
