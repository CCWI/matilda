package edu.hm.ccwi.matilda.auth.rest;

import edu.hm.ccwi.matilda.auth.model.User;
import edu.hm.ccwi.matilda.auth.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${matilda.auth.route.user}")
public class UserController extends BaseController{

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {

        List<User> userList = userService.getAllUsers();

        return ResponseEntity.ok(userList);
    }

    @GetMapping(value = "/{username}")
    public ResponseEntity<User> getUserByName(@PathVariable("username") String username) {

        User aUser = userService.getUserByUsername(username);

        return ResponseEntity.ok(aUser);
    }
}
