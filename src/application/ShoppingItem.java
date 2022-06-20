package application;

import java.io.Serializable;

public class ShoppingItem implements Comparable<ShoppingItem>, Serializable {
    private Integer id;
    private String username;

    private String name;
    private Double price;
    private Integer quantity;
    private Integer priority;

    public ShoppingItem() {id = -1;}

    public ShoppingItem(int id, String username, String name, Double price, Integer quantity, Integer priority) {
        this.id = id;
        this.username = username;
        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.priority = priority;
    }

    public ShoppingItem(String username, String name, Double price, Integer quantity, Integer priority) {
        if (name.length() == 0)
            throw new IllegalArgumentException("Please provide a name for this item");
        if (price < 0.0)
            throw new IllegalArgumentException("Item price cannot be negative");
        if (quantity < 1)
            throw new IllegalArgumentException("Please enter a quantity");
        if (priority < 1)
            throw new IllegalArgumentException("Please enter a priority");

        this.username = username;

        this.name = name;
        this.price = price;
        this.quantity = quantity;
        this.priority = priority;
    }

    public ShoppingItem(ShoppingItem that) {
        this.id = that.id;
        this.name = that.name;
        this.price = that.price;
        this.quantity = that.quantity;
        this.priority = that.priority;
    }

    public Integer getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public String getName() {
        return name;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    //define natural ordering for sorting items
    public int compareTo(ShoppingItem that) {
        if (this.priority > that.priority) return 1;
        if (this.priority < that.priority) return -1;
        if (Double.compare(this.price, that.price) > 0) return 1;
        if (Double.compare(this.price, that.price) < 0) return -1;
        if (this.quantity > that.quantity) return 1;
        if (this.quantity < that.quantity) return -1;
        if (this.name.compareTo(that.name) > 1) return 1;
        if (this.name.compareTo(that.name) < 1) return -1;
        return 0;
    }

    //used to determine if an item of the same name
    //is already in a shopping list (prevent duplicates)
    @Override
    public boolean equals(Object o) {
        if (o instanceof ShoppingItem) {
            ShoppingItem i = (ShoppingItem) o;
            return this.name.equals(i.name);
        }
        return false;
    }

    //not used in this app
    //ensure Items treated as equals return same hash code
    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    @Override
    public String toString() {
        return String.format("%s  (%d) ($%,.2f)", name, quantity, price);
    }
}
