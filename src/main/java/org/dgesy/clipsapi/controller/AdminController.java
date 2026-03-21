package org.dgesy.clipsapi.controller;

import org.dgesy.clipsapi.model.Clip;
import org.dgesy.clipsapi.model.User;
import org.dgesy.clipsapi.repository.ClipRepository;
import org.dgesy.clipsapi.repository.UserRepository;
import org.dgesy.clipsapi.service.ClipService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final ClipRepository clipRepository;
    private final UserRepository userRepository;
    private final ClipService clipService;

    public AdminController(ClipRepository clipRepository,
                           UserRepository userRepository,
                           ClipService clipService) {
        this.clipRepository = clipRepository;
        this.userRepository = userRepository;
        this.clipService = clipService;
    }

    @GetMapping("/clips")
    public ResponseEntity<?> getAllClips() {
        List<Clip> clips = clipRepository.findAllByOrderByCreatedAtDesc();
        return ResponseEntity.ok(clips);
    }

    @GetMapping("/clips/flagged")
    public ResponseEntity<?> getFlaggedClips() {
        return ResponseEntity.ok(clipRepository.findByIsFlaggedTrue());
    }

    @GetMapping("/clips/hidden")
    public ResponseEntity<?> getHiddenClips() {
        return ResponseEntity.ok(clipRepository.findByIsHiddenTrue());
    }

    @PostMapping("/clips/{shortId}/flag")
    public ResponseEntity<?> flagClip(@PathVariable String shortId) {
        return clipRepository.findByShortId(shortId).map(clip -> {
            clip.setFlagged(!clip.isFlagged());
            clipRepository.save(clip);
            return ResponseEntity.ok(Map.of("flagged", clip.isFlagged()));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/clips/{shortId}/hide")
    public ResponseEntity<?> hideClip(@PathVariable String shortId) {
        return clipRepository.findByShortId(shortId).map(clip -> {
            clip.setHidden(!clip.isHidden());
            clipRepository.save(clip);
            return ResponseEntity.ok(Map.of("hidden", clip.isHidden()));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/clips/{shortId}")
    public ResponseEntity<?> deleteClip(@PathVariable String shortId,
                                        @AuthenticationPrincipal String username) {
        try {
            clipService.deleteClip(shortId, username);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        List<User> users = userRepository.findAll();
        return ResponseEntity.ok(users.stream().map(u -> Map.of(
                "id", u.getId(),
                "username", u.getUsername(),
                "email", u.getEmail(),
                "role", u.getRole(),
                "isBanned", u.isBanned(),
                "storageUsedBytes", u.getStorageUsedBytes(),
                "createdAt", u.getCreatedAt().toString()
        )).toList());
    }

    @PostMapping("/users/{id}/ban")
    public ResponseEntity<?> banUser(@PathVariable Long id) {
        return userRepository.findById(id).map(user -> {
            if (user.getRole().equals("ADMIN")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Cannot ban an admin"));
            }
            user.setBanned(!user.isBanned());
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("banned", user.isBanned()));
        }).orElse(ResponseEntity.notFound().build());
    }
}