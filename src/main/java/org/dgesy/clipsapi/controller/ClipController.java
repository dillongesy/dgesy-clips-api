package org.dgesy.clipsapi.controller;

import org.dgesy.clipsapi.model.Clip;
import org.dgesy.clipsapi.service.ClipService;
import org.dgesy.clipsapi.service.FileServerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/clips")
public class ClipController {

    private final ClipService clipService;
    private final FileServerService fileServerService;

    @Value("${fileserver.url}")
    private String fileServerUrl;

    public ClipController(ClipService clipService,
                          FileServerService fileServerService) {
        this.clipService = clipService;
        this.fileServerService = fileServerService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file,
                                    @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Clip clip = clipService.uploadClip(file, userDetails.getUsername());
            return ResponseEntity.ok(Map.of(
                    "shortId", clip.getShortId(),
                    "url", "https://clips.dgesy.org/v/" + clip.getShortId()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/mine")
    public ResponseEntity<?> myClips(@AuthenticationPrincipal UserDetails userDetails) {
        List<Clip> clips = clipService.getUserClips(userDetails.getUsername());
        return ResponseEntity.ok(clips);
    }

    @GetMapping("/shared")
    public ResponseEntity<?> sharedWithMe(@AuthenticationPrincipal UserDetails userDetails) {
        List<Clip> clips = clipService.getSharedWithUser(userDetails.getUsername());
        return ResponseEntity.ok(clips);
    }

    @DeleteMapping("/{shortId}")
    public ResponseEntity<?> delete(@PathVariable String shortId,
                                    @AuthenticationPrincipal UserDetails userDetails) {
        try {
            clipService.deleteClip(shortId, userDetails.getUsername());
            return ResponseEntity.ok(Map.of("success", true));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{shortId}/share")
    public ResponseEntity<?> share(@PathVariable String shortId,
                                   @RequestBody Map<String, String> body,
                                   @AuthenticationPrincipal UserDetails userDetails) {
        try {
            clipService.shareClip(shortId, userDetails.getUsername(),
                    body.get("username"));
            return ResponseEntity.ok(Map.of("success", true));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/view/{shortId}")
    public ResponseEntity<?> viewClip(@PathVariable String shortId) {
        try {
            Clip clip = clipService.getClipByShortId(shortId);
            if (clip.isHidden()) {
                return ResponseEntity.status(404).body(Map.of("error", "Not found"));
            }
            clipService.incrementViewCount(shortId);
            return ResponseEntity.ok(Map.of(
                    "shortId", clip.getShortId(),
                    "originalName", clip.getOriginalName(),
                    "duration", clip.getDurationSeconds(),
                    "viewCount", clip.getViewCount(),
                    "createdAt", clip.getCreatedAt().toString(),
                    "streamUrl", "/api/clips/stream/" + clip.getShortId(),
                    "thumbnailUrl", fileServerUrl + "/thumbnail/" +
                            clip.getShortId() + ".jpg",
                    "owner", clip.getUser().getUsername()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(Map.of("error", "Not found"));
        }
    }

    @GetMapping("/stream/{shortId}")
    public void streamClip(@PathVariable String shortId,
                           jakarta.servlet.http.HttpServletRequest request,
                           jakarta.servlet.http.HttpServletResponse response)
            throws Exception {
        Clip clip = clipService.getClipByShortId(shortId);
        if (clip.isHidden()) {
            response.setStatus(404);
            return;
        }
        String videoUrl = fileServerUrl + "/video/" + clip.getFilename();
        response.sendRedirect(videoUrl);
    }
}