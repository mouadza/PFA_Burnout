package com.burncare.burncare_app.controllers;

import com.burncare.burncare_app.dto.UserDTO;
import com.burncare.burncare_app.entities.User;
import com.burncare.burncare_app.services.AdminService;
import com.burncare.burncare_app.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final UserService userService; // ✅ Ajout du UserService

    // Injection des dépendances via le constructeur
    public AdminController(AdminService adminService, UserService userService) {
        this.adminService = adminService;
        this.userService = userService;
    }

    // GET : Récupérer tous les utilisateurs
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    // ✅ PUT : Mettre à jour / Approuver un utilisateur
    // C'est cette méthode qui résout l'erreur 405
    @PutMapping("/users/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody UserDTO userDTO) {
        // On utilise la méthode updateUser qu'on a créée dans UserService
        User updatedUser = userService.updateUser(id, userDTO);
        return ResponseEntity.ok(updatedUser);
    }

    // DELETE : Supprimer un utilisateur par ID
    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        adminService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}