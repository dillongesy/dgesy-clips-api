package org.dgesy.clipsapi.service;

import org.dgesy.clipsapi.model.Clip;
import org.dgesy.clipsapi.model.Share;
import org.dgesy.clipsapi.model.User;
import org.dgesy.clipsapi.repository.ClipRepository;
import org.dgesy.clipsapi.repository.ShareRepository;
import org.dgesy.clipsapi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;

@Service
public class ClipService {

    private final ClipRepository clipRepository;
    private final ShareRepository shareRepository;
    private final UserRepository userRepository;
    private final FileServerService fileServerService;

    @Value("${app.max-file-size-bytes}")
    private long maxFileSizeBytes;

    public ClipService(ClipRepository clipRepository,
                       ShareRepository shareRepository,
                       UserRepository userRepository,
                       FileServerService fileServerService) {
        this.clipRepository = clipRepository;
        this.shareRepository = shareRepository;
        this.userRepository = userRepository;
        this.fileServerService = fileServerService;
    }

    public Clip uploadClip(MultipartFile file, String username) throws IOException, InterruptedException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Save original to temp file
        File tempInput = File.createTempFile("upload_", ".tmp");
        file.transferTo(tempInput);

        // Compress with ffmpeg
        File tempOutput = File.createTempFile("compressed_", ".mp4");
        ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg", "-i", tempInput.getAbsolutePath(),
                "-vcodec", "libx264", "-crf", "28",
                "-acodec", "aac", "-b:a", "128k",
                "-movflags", "+faststart",
                "-y", tempOutput.getAbsolutePath()
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();
        process.waitFor();

        byte[] compressed = Files.readAllBytes(tempOutput.toPath());

        // Check size after compression
        if (compressed.length > maxFileSizeBytes) {
            tempInput.delete();
            tempOutput.delete();
            throw new RuntimeException("File exceeds 500MB limit after compression");
        }

        // Generate short ID and filename
        String shortId = UUID.randomUUID().toString().substring(0, 8);
        String filename = shortId + ".mp4";

        // Generate thumbnail
        File thumbFile = File.createTempFile("thumb_", ".jpg");
        ProcessBuilder thumbPb = new ProcessBuilder(
                "ffmpeg", "-i", tempOutput.getAbsolutePath(),
                "-ss", "00:00:01",
                "-vframes", "1",
                "-y", thumbFile.getAbsolutePath()
        );
        thumbPb.redirectErrorStream(true);
        thumbPb.start().waitFor();

        // Get duration
        ProcessBuilder durationPb = new ProcessBuilder(
                "ffprobe", "-v", "error",
                "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1",
                tempOutput.getAbsolutePath()
        );
        Process durationProcess = durationPb.start();
        String durationStr = new String(durationProcess.getInputStream().readAllBytes()).trim();
        int duration = (int) Double.parseDouble(durationStr);

        // Store files on file server
        fileServerService.storeVideo(filename, compressed);
        fileServerService.storeThumbnail(shortId + ".jpg",
                Files.readAllBytes(thumbFile.toPath()));

        // Cleanup temp files
        tempInput.delete();
        tempOutput.delete();
        thumbFile.delete();

        // Save clip metadata
        Clip clip = new Clip();
        clip.setShortId(shortId);
        clip.setUser(user);
        clip.setFilename(filename);
        clip.setOriginalName(file.getOriginalFilename());
        clip.setSizeBytes(compressed.length);
        clip.setDurationSeconds(duration);
        clip.setPrivate(true);
        clipRepository.save(clip);

        // Update user storage
        user.setStorageUsedBytes(user.getStorageUsedBytes() + compressed.length);
        userRepository.save(user);

        return clip;
    }

    public List<Clip> getUserClips(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return clipRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public List<Clip> getSharedWithUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return shareRepository.findBySharedWith(user)
                .stream()
                .map(Share::getClip)
                .filter(c -> !c.isHidden())
                .toList();
    }

    public Clip getClipByShortId(String shortId) {
        return clipRepository.findByShortId(shortId)
                .orElseThrow(() -> new RuntimeException("Clip not found"));
    }

    public void deleteClip(String shortId, String username) {
        Clip clip = getClipByShortId(shortId);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!clip.getUser().getId().equals(user.getId())
                && !user.getRole().equals("ADMIN")) {
            throw new RuntimeException("Unauthorized");
        }

        // Update storage
        User owner = clip.getUser();
        owner.setStorageUsedBytes(
                Math.max(0, owner.getStorageUsedBytes() - clip.getSizeBytes()));
        userRepository.save(owner);

        shareRepository.deleteAll(shareRepository.findByClip(clip));
        fileServerService.deleteVideo(clip.getFilename());
        clipRepository.delete(clip);
    }

    public void shareClip(String shortId, String ownerUsername, String targetUsername) {
        Clip clip = getClipByShortId(shortId);
        User owner = userRepository.findByUsername(ownerUsername)
                .orElseThrow(() -> new RuntimeException("User not found"));
        User target = userRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new RuntimeException("Target user not found"));

        if (!clip.getUser().getId().equals(owner.getId())) {
            throw new RuntimeException("Unauthorized");
        }

        if (shareRepository.existsByClipAndSharedWith(clip, target)) {
            throw new RuntimeException("Already shared with this user");
        }

        Share share = new Share();
        share.setClip(clip);
        share.setSharedBy(owner);
        share.setSharedWith(target);
        shareRepository.save(share);
    }

    public void incrementViewCount(String shortId) {
        clipRepository.findByShortId(shortId).ifPresent(clip -> {
            clip.setViewCount(clip.getViewCount() + 1);
            clipRepository.save(clip);
        });
    }
}