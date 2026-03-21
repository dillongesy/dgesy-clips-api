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
                                    @AuthenticationPrincipal String username) {
        try {
            Clip clip = clipService.uploadClip(file, username);
            return ResponseEntity.ok(Map.of(
                    "shortId", clip.getShortId(),
                    "url", "https://clips.dgesy.org/v/" + clip.getShortId()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/mine")
    public ResponseEntity<?> myClips(@AuthenticationPrincipal String username) {
        List<Clip> clips = clipService.getUserClips(username);
        return ResponseEntity.ok(clips);
    }

    @GetMapping("/shared")
    public ResponseEntity<?> sharedWithMe(@AuthenticationPrincipal String username) {
        List<Clip> clips = clipService.getSharedWithUser(username);
        return ResponseEntity.ok(clips);
    }

    @DeleteMapping("/{shortId}")
    public ResponseEntity<?> delete(@PathVariable String shortId,
                                    @AuthenticationPrincipal String username) {
        try {
            clipService.deleteClip(shortId, username);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{shortId}/share")
    public ResponseEntity<?> share(@PathVariable String shortId,
                                @RequestBody Map<String, String> body,
                                @AuthenticationPrincipal String username) {
        try {
            clipService.shareClip(shortId, username, body.get("username"));
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
                    "thumbnailUrl", "/api/clips/thumbnail/" + clip.getShortId(),
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

        String videoUrl = fileServerService.getVideoUrl(clip.getFilename());
        String rangeHeader = request.getHeader("Range");

        java.net.URL url = new java.net.URL(videoUrl);
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();

        if (rangeHeader != null) {
            conn.setRequestProperty("Range", rangeHeader);
        }

        conn.connect();

        response.setStatus(rangeHeader != null ? 206 : 200);
        response.setContentType("video/mp4");

        String contentRange = conn.getHeaderField("Content-Range");
        String contentLength = conn.getHeaderField("Content-Length");

        if (contentRange != null) response.setHeader("Content-Range", contentRange);
        if (contentLength != null) response.setHeader("Content-Length", contentLength);
        response.setHeader("Accept-Ranges", "bytes");

        try (var in = conn.getInputStream();
            var out = response.getOutputStream()) {
            in.transferTo(out);
        }
    }

    @GetMapping("/thumbnail/{shortId}")
    public void thumbnailClip(@PathVariable String shortId,
                            jakarta.servlet.http.HttpServletResponse response)
            throws Exception {
        Clip clip = clipService.getClipByShortId(shortId);
        String thumbnailUrl = fileServerService.getThumbnailUrl(
                clip.getShortId() + ".jpg");
        response.sendRedirect(thumbnailUrl);
    }
}