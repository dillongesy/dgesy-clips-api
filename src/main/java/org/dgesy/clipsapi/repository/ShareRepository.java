package org.dgesy.clipsapi.repository;

import org.dgesy.clipsapi.model.Clip;
import org.dgesy.clipsapi.model.Share;
import org.dgesy.clipsapi.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ShareRepository extends JpaRepository<Share, Long> {
    List<Share> findBySharedWith(User sharedWith);
    List<Share> findByClip(Clip clip);
    boolean existsByClipAndSharedWith(Clip clip, User sharedWith);
    void deleteByClipAndSharedWith(Clip clip, User sharedWith);
}