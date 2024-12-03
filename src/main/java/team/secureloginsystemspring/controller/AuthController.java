package team.secureloginsystemspring.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import team.secureloginsystemspring.model.User;
import team.secureloginsystemspring.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Controller
public class AuthController {

    @Value("${google.recaptcha.secret.key}")
    private String recaptchaSecretKey;

    @Value("${google.recaptcha.site.key}")
    private String recaptchaSiteKey;

    @Autowired
    private UserService userService;

    @GetMapping("/register")
    public String showRegisterPage(Model model) {
        model.addAttribute("recaptchaSiteKey", recaptchaSiteKey);
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String username, @RequestParam String password,
                           @RequestParam String email, @RequestParam("g-recaptcha-response") String gRecaptchaResponse,
                           Model model) {
        model.addAttribute("recaptchaSiteKey", recaptchaSiteKey);
        if (!password.matches("^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).{8,}$")) {
            model.addAttribute("error", "Password must meet the security policy (Minimum 8 characters, including a capital letter, a number and a symbol (!, @, #))");
            return "register";
        }
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            model.addAttribute("error", "Invalid email format!");
            return "register";
        }
        if (userService.isUsernameTaken(username)) {
            model.addAttribute("error", "Username is already taken!");
            return "register";
        }
        if (userService.isEmailTaken(email)) {
            model.addAttribute("error", "Email is already registered!");
            return "register";
        }
        if (!verifyRecaptcha(gRecaptchaResponse)) {
            model.addAttribute("error", "reCAPTCHA verification failed!");
            return "register";
        }
        userService.registerUser(username, password, email);
        return "redirect:/login";
    }

    @GetMapping("/activate")
    public String activateAccount(@RequestParam String token, Model model) {
        Optional<User> userOpt = userService.findByActivationToken(token);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setActivated(true);
            user.setActivationtoken(null);
            userService.save(user);
            model.addAttribute("message", "Your account has been activated successfully!");
            return "activation_status";
        } else {
            model.addAttribute("message", "Invalid activation token!");
            return "activation_status";
        }
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
    public String showHomePage(Model model) {
        User user = userService.getCurrentUser();
        model.addAttribute("user", user);
        return "home";
    }

    private boolean verifyRecaptcha(String gRecaptchaResponse) {
        String url = "https://www.google.com/recaptcha/api/siteverify";
        String params = "secret=" + recaptchaSecretKey + "&response=" + gRecaptchaResponse;

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(url + "?" + params, HttpMethod.POST, entity, String.class);

        return response.getBody().contains("\"success\": true");
    }
}
