package com.campusos.issue.repository;

import com.campusos.issue.entity.Issue;
import com.campusos.issue.enums.IssueEnums;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface IssueRepository extends JpaRepository<Issue, Long> {
    Optional<Issue> findByReference(String reference);
    long countByStatusIn(List<IssueEnums.Status> statuses);
    long countByPriorityAndStatusIn(IssueEnums.Priority priority, List<IssueEnums.Status> statuses);

    @Query("select i from Issue i where i.status in :statuses and i.createdAt < :cutoff order by i.createdAt asc")
    List<Issue> findOverdue(@Param("statuses") List<IssueEnums.Status> statuses, @Param("cutoff") Instant cutoff);

    @Query("""
        select i from Issue i
        where (:reporterId is null or i.reporter.id = :reporterId)
          and (:assigneeId is null or i.assignee.id = :assigneeId)
          and (:status is null or i.status = :status)
          and (:category is null or i.category = :category)
          and (:priority is null or i.priority = :priority)
          and (:q is null or lower(i.title) like lower(concat('%', :q, '%'))
               or lower(i.description) like lower(concat('%', :q, '%'))
               or lower(i.reference) like lower(concat('%', :q, '%')))
        """)
    Page<Issue> search(@Param("reporterId") Long reporterId,
                       @Param("assigneeId") Long assigneeId,
                       @Param("status") IssueEnums.Status status,
                       @Param("category") IssueEnums.Category category,
                       @Param("priority") IssueEnums.Priority priority,
                       @Param("q") String q,
                       Pageable pageable);

    @Query("select i.category, count(i) from Issue i group by i.category order by count(i) desc")
    List<Object[]> countByCategory();

    @Query("select i.status, count(i) from Issue i group by i.status")
    List<Object[]> countByStatus();

    @Query("select i from Issue i where i.resolvedAt is not null and i.resolvedAt >= :from and i.resolvedAt < :to")
    List<Issue> findResolvedBetween(@Param("from") Instant from, @Param("to") Instant to);

    @Query("select count(i) from Issue i where i.createdAt >= :from and i.createdAt < :to")
    long countCreatedBetween(@Param("from") Instant from, @Param("to") Instant to);
}
