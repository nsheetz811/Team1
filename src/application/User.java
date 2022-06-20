package application;

import java.util.ArrayList;
import java.util.List;

public class User {
    private String username;
    private String password;
    //private List<ShoppingItem> shoppingCart = new ArrayList<>();

    public User() {username = "empty";}

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
