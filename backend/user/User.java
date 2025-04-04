package backend.user;

import java.util.ArrayList;


public class User {
    
    public static final String CAT_WATCHING = "WATCHING_CAT";
    // static final String SOMEWHERE_ELSE = "SOMEWHERE_ELSE";
    static final String PLAYING_GAME = "PLAYING_GAME";



    // connect database or sum
    static int numUsers = 0;
    static ArrayList<User> allUsers = new ArrayList<>();

    // String email;
    // String password;


    String type = "USER_INIT";

    int userId;
    String currentlyDoing; // CAT_WATCHING or GAME_PLAYING
    PlayerData playerData;



    public User() {
        numUsers++;
        userId = numUsers;
        allUsers.add(this);
    }

    public void setCurrentlyDoingTo(String newActivity) {
        currentlyDoing = newActivity;
    }

    public String getCurrentlyDoing() {
        return currentlyDoing;
    }
}
