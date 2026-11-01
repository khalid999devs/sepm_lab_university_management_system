package com.kuet.ums.repository;

import com.kuet.ums.entity.Notice;
import org.springframework.data.jpa.repository.*;
import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    @EntityGraph(attributePaths = "author")
    List<Notice> findAllByOrderByPublishedAtDescIdDesc();
}
