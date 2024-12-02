package team.secureloginsystemspring.controller;

import team.secureloginsystemspring.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {

    @Autowired
    private UserService userService;

    @GetMapping("/register")
    public String showRegisterPage() { return "register"; }

    @PostMapping("/register")
    public String register(@RequestParam String username, @RequestParam String password, Model model) {

        if (!password.matches("^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).{8,}$")) {
            model.addAttribute("error", "Password must meet the security policy!");
            return "register";
        }

        userService.registerUser(username, password);
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String showLoginPage() { return "login"; }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password, Model model) {

        if(!userService.authenticate(username, password)) {
            model.addAttribute("error", "Invalid username or password!");
            return "login";
        }
        else { return "redirect:/home"; }
    }

    @GetMapping("/home")
    public String showHomePage(Model model) { return "home"; }
}
