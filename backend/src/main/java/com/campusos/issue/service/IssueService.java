package com.campusos.issue.service;

import com.campusos.common.exception.BusinessException;
import com.campusos.issue.dto.IssueDtos;
import com.campusos.issue.entity.Issue;
import com.campusos.issue.entity.IssueActivity;
import com.campusos.issue.enums.IssueEnums;
import com.campusos.issue.repository.IssueActivityRepository;
import com.campusos.issue.repository.IssueRepository;
import com.campusos.security.AppUserDetails;
import com.campusos.user.Role;
import com.campusos.user.User;
import com.campusos.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class IssueService {
    private final IssueRepository issueRepository;
    private final IssueActivityRepository activityRepository;
    private final UserRepository userRepository;
    private final IssuePriorityService priorityService;
    private final AtomicLong referenceSeq = new AtomicLong(System.currentTimeMillis() % 100000);

    public IssueService(IssueRepository issueRepository, IssueActivityRepository activityRepository,
                        UserRepository userRepository, IssuePriorityService priorityService) {
        this.issueRepository = issueRepository;
        this.activityRepository = activityRepository;
        this.userRepository = userRepository;
        this.priorityService = priorityService;
    }

    @Transactional
    public IssueDtos.IssueResponse create(IssueDtos.CreateIssueRequest req, AppUserDetails actor) {
        User reporter = userRepository.findById(actor.getId())
                .orElseThrow(() -> BusinessException.notFound("Reporter not found."));
        var rec = priorityService.recommend(req.category(), req.priority(), req.affectedPeople(), req.safetyRisk(), Instant.now());
        IssueEnums.Priority finalPriority = req.priority() != null ? maxPriority(req.priority(), rec.priority()) : rec.priority();
        String reference = "ISS-" + String.format("%04d", referenceSeq.incrementAndGet() % 10000);
        if (issueRepository.findByReference(reference).isPresent()) {
            reference = "ISS-" + System.currentTimeMillis() % 100000;
        }
        Issue issue = new Issue(reference, req.title().trim(), req.description().trim(), req.category(),
                finalPriority, req.building().trim(),
                req.locationDetail() == null ? null : req.locationDetail().trim(), reporter);
        issue = issueRepository.save(issue);
        User actorUser = reporter;
        activityRepository.save(new IssueActivity(issue, actorUser, "CREATED", null, IssueEnums.Status.REPORTED,
                "Issue reported. " + rec.explanation()));
        return toResponse(issue, rec.explanation());
    }

    private IssueEnums.Priority maxPriority(IssueEnums.Priority a, IssueEnums.Priority b) {
        return a.ordinal() >= b.ordinal() ? a : b;
    }

    @Transactional(readOnly = true)
    public Page<IssueDtos.IssueResponse> list(AppUserDetails actor, IssueEnums.Status status,
                                              IssueEnums.Category category, IssueEnums.Priority priority,
                                              String q, boolean mine, boolean assignedToMe, Pageable pageable) {
        Long reporterId = null;
        Long assigneeId = null;
        if (mine || actor.getRole() == Role.STUDENT || actor.getRole() == Role.FACULTY) reporterId = actor.getId();
        if (assignedToMe) assigneeId = actor.getId();
        if (actor.getRole() == Role.TECHNICIAN && !mine && !assignedToMe) assigneeId = actor.getId();
        String query = (q == null || q.isBlank()) ? null : q.trim();
        return issueRepository.search(reporterId, assigneeId, status, category, priority, query, pageable)
                .map(i -> toResponse(i, null));
    }

    @Transactional(readOnly = true)
    public IssueDtos.IssueDetailResponse detail(Long id, AppUserDetails actor) {
        Issue issue = loadVisible(id, actor);
        List<IssueDtos.ActivityResponse> activity = activityRepository.findByIssueIdOrderByCreatedAtAsc(id)
                .stream().map(this::toActivity).toList();
        List<String> allowed = IssueWorkflow.allowedFrom(issue.getStatus()).stream().map(Enum::name).sorted().toList();
        return new IssueDtos.IssueDetailResponse(toResponse(issue, null), activity, allowed);
    }

    @Transactional
    public IssueDtos.IssueResponse assign(Long id, Long technicianId, AppUserDetails actor) {
        if (!actor.getRole().isAdmin()) throw BusinessException.forbidden("Only administrators can assign technicians.");
        Issue issue = issueRepository.findById(id).orElseThrow(() -> BusinessException.notFound("Issue not found."));
        User tech = userRepository.findById(technicianId)
                .orElseThrow(() -> BusinessException.notFound("Technician not found."));
        if (tech.getRole() != Role.TECHNICIAN || !tech.isActive()) {
            throw BusinessException.rule("Selected user is not an active technician.");
        }
        if (issue.getStatus() == IssueEnums.Status.RESOLVED || issue.getStatus() == IssueEnums.Status.CLOSED
                || issue.getStatus() == IssueEnums.Status.CANCELLED || issue.getStatus() == IssueEnums.Status.REJECTED) {
            throw BusinessException.conflict("ILLEGAL_TRANSITION", "Cannot assign a " + issue.getStatus() + " issue.");
        }
        IssueEnums.Status from = issue.getStatus();
        issue.setAssignee(tech);
        issue.setStatus(IssueEnums.Status.ASSIGNED);
        issue.touch();
        issueRepository.save(issue);
        User actorUser = userRepository.findById(actor.getId()).orElseThrow();
        activityRepository.save(new IssueActivity(issue, actorUser, "ASSIGNED", from, IssueEnums.Status.ASSIGNED,
                "Assigned to " + tech.getDisplayName() + "."));
        return toResponse(issue, null);
    }

    @Transactional
    public IssueDtos.IssueResponse transition(Long id, IssueDtos.TransitionRequest req, AppUserDetails actor) {
        Issue issue = issueRepository.findById(id).orElseThrow(() -> BusinessException.notFound("Issue not found."));
        assertCanAct(issue, actor, req.toStatus());
        IssueEnums.Status from = issue.getStatus();
        IssueEnums.Status to = req.toStatus();
        if (!IssueWorkflow.canTransition(from, to)) {
            throw BusinessException.conflict("ILLEGAL_TRANSITION",
                    "Cannot move issue from " + from + " to " + to + ".");
        }
        if (to == IssueEnums.Status.RESOLVED && (req.note() == null || req.note().isBlank())) {
            throw BusinessException.rule("Resolution notes are required to resolve an issue.");
        }
        if (to == IssueEnums.Status.CANCELLED && actor.getRole() == Role.STUDENT
                && !issue.getReporter().getId().equals(actor.getId())) {
            throw BusinessException.forbidden("You can only cancel your own issues.");
        }
        issue.setStatus(to);
        if (to == IssueEnums.Status.RESOLVED) {
            issue.setResolvedAt(Instant.now());
            issue.setResolutionNotes(req.note().trim());
        }
        if (to == IssueEnums.Status.IN_PROGRESS && from == IssueEnums.Status.RESOLVED) {
            issue.setResolvedAt(null);
        }
        if (to == IssueEnums.Status.TRIAGED) issue.setAssignee(null);
        issue.touch();
        issueRepository.save(issue);
        User actorUser = userRepository.findById(actor.getId()).orElseThrow();
        activityRepository.save(new IssueActivity(issue, actorUser, "TRANSITION", from, to, req.note()));
        return toResponse(issue, null);
    }

    @Transactional(readOnly = true)
    public List<IssueDtos.IssueResponse> overdue(AppUserDetails actor) {
        if (!actor.getRole().isStaff()) throw BusinessException.forbidden("Staff access required.");
        Instant cutoff = Instant.now().minus(48, ChronoUnit.HOURS);
        List<IssueEnums.Status> open = List.of(IssueEnums.Status.REPORTED, IssueEnums.Status.TRIAGED,
                IssueEnums.Status.ASSIGNED, IssueEnums.Status.IN_PROGRESS);
        return issueRepository.findOverdue(open, cutoff).stream().map(i -> toResponse(i, null)).toList();
    }

    private Issue loadVisible(Long id, AppUserDetails actor) {
        Issue issue = issueRepository.findById(id).orElseThrow(() -> BusinessException.notFound("Issue not found."));
        if (!canView(issue, actor)) throw BusinessException.notFound("Issue not found.");
        return issue;
    }

    private boolean canView(Issue issue, AppUserDetails actor) {
        return switch (actor.getRole()) {
            case CAMPUS_ADMIN -> true;
            case DEPT_ADMIN -> true;
            case TECHNICIAN -> issue.getReporter().getId().equals(actor.getId())
                    || (issue.getAssignee() != null && issue.getAssignee().getId().equals(actor.getId()));
            case STUDENT, FACULTY -> issue.getReporter().getId().equals(actor.getId());
        };
    }

    private void assertCanAct(Issue issue, AppUserDetails actor, IssueEnums.Status to) {
        Role role = actor.getRole();
        if (role == Role.CAMPUS_ADMIN || role == Role.DEPT_ADMIN) return;
        if (role == Role.TECHNICIAN) {
            if (issue.getAssignee() != null && issue.getAssignee().getId().equals(actor.getId())) return;
            throw BusinessException.forbidden("You can only update issues assigned to you.");
        }
        if (to == IssueEnums.Status.CANCELLED && issue.getReporter().getId().equals(actor.getId())) return;
        if (to == IssueEnums.Status.IN_PROGRESS
                && (issue.getStatus() == IssueEnums.Status.RESOLVED || issue.getStatus() == IssueEnums.Status.CLOSED)
                && issue.getReporter().getId().equals(actor.getId())) return;
        throw BusinessException.forbidden("You do not have permission to change this issue.");
    }

    public IssueDtos.IssueResponse toResponse(Issue issue, String explanation) {
        return new IssueDtos.IssueResponse(issue.getId(), issue.getReference(), issue.getTitle(), issue.getDescription(),
                issue.getCategory().name(), issue.getPriority().name(), issue.getStatus().name(), issue.getBuilding(),
                issue.getLocationDetail(), issue.getReporter().getId(), issue.getReporter().getDisplayName(),
                issue.getAssignee() == null ? null : issue.getAssignee().getId(),
                issue.getAssignee() == null ? null : issue.getAssignee().getDisplayName(),
                issue.getCreatedAt(), issue.getUpdatedAt(), issue.getResolvedAt(), issue.getResolutionNotes(), explanation);
    }

    private IssueDtos.ActivityResponse toActivity(IssueActivity a) {
        return new IssueDtos.ActivityResponse(a.getId(), a.getActor().getDisplayName(), a.getAction(),
                a.getFromStatus() == null ? null : a.getFromStatus().name(),
                a.getToStatus() == null ? null : a.getToStatus().name(), a.getNote(), a.getCreatedAt());
    }
}
