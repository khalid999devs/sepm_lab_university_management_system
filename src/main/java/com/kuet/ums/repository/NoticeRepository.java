package com.kuet.ums.repository;

import com.kuet.ums.entity.Notice;
import java.util.List;
import org.springframework.data.jpa.repository.*;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    @EntityGraph(attributePaths = "author")
    List<Notice> findAllByOrderByPublishedAtDescIdDesc();
}
