package org.dgesy.clipsapi.repository;

import org.dgesy.clipsapi.model.Clip;
import org.dgesy.clipsapi.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ClipRepository extends JpaRepository<Clip, Long> {
    Optional<Clip> findByShortId(String shortId);
    List<Clip> findByUser(User user);
    List<Clip> findByUserOrderByCreatedAtDesc(User user);
    List<Clip> findAllByOrderByCreatedAtDesc();
    List<Clip> findByIsFlaggedTrue();
    List<Clip> findByIsHiddenTrue();
}