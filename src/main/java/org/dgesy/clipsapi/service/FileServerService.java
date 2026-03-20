package org.dgesy.clipsapi.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class FileServerService {

    @Value("${fileserver.url}")
    private String fileServerUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public void storeVideo(String filename, byte[] data) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("video/mp4"));
        HttpEntity<ByteArrayResource> entity = new HttpEntity<>(
                new ByteArrayResource(data), headers);
        restTemplate.exchange(
                fileServerUrl + "/store/" + filename,
                HttpMethod.POST,
                entity,
                String.class
        );
    }

    public void storeThumbnail(String filename, byte[] data) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_JPEG);
        HttpEntity<ByteArrayResource> entity = new HttpEntity<>(
                new ByteArrayResource(data), headers);
        restTemplate.exchange(
                fileServerUrl + "/thumbnail/" + filename,
                HttpMethod.POST,
                entity,
                String.class
        );
    }

    public void deleteVideo(String filename) {
        restTemplate.delete(fileServerUrl + "/video/" + filename);
    }

    public String getVideoUrl(String filename) {
        return fileServerUrl + "/video/" + filename;
    }

    public String getThumbnailUrl(String filename) {
        return fileServerUrl + "/thumbnail/" + filename;
    }
}