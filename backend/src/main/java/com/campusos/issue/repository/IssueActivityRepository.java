package com.campusos.issue.repository;

import com.campusos.issue.entity.IssueActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface IssueActivityRepository extends JpaRepository<IssueActivity, Long> {
    List<IssueActivity> findByIssueIdOrderByCreatedAtAsc(Long issueId);
}
