CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(190) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    role VARCHAR(30) NOT NULL,
    department VARCHAR(120),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE resources (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    type VARCHAR(40) NOT NULL,
    building VARCHAR(120) NOT NULL,
    location VARCHAR(160),
    capacity INT,
    description VARCHAR(1000),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    requires_approval BOOLEAN NOT NULL DEFAULT FALSE,
    equipment_tags VARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE issues (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reference VARCHAR(20) NOT NULL UNIQUE,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(4000) NOT NULL,
    category VARCHAR(40) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    building VARCHAR(120) NOT NULL,
    location_detail VARCHAR(200),
    reporter_id BIGINT NOT NULL,
    assignee_id BIGINT,
    resolution_notes VARCHAR(4000),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    resolved_at TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_issue_reporter FOREIGN KEY (reporter_id) REFERENCES users(id),
    CONSTRAINT fk_issue_assignee FOREIGN KEY (assignee_id) REFERENCES users(id)
);

CREATE TABLE issue_activities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    issue_id BIGINT NOT NULL,
    actor_id BIGINT NOT NULL,
    action VARCHAR(60) NOT NULL,
    from_status VARCHAR(20),
    to_status VARCHAR(20),
    note VARCHAR(2000),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_activity_issue FOREIGN KEY (issue_id) REFERENCES issues(id),
    CONSTRAINT fk_activity_actor FOREIGN KEY (actor_id) REFERENCES users(id)
);

CREATE TABLE bookings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    resource_id BIGINT NOT NULL,
    owner_id BIGINT NOT NULL,
    purpose VARCHAR(300) NOT NULL,
    start_at TIMESTAMP NOT NULL,
    end_at TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL,
    cancel_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_booking_resource FOREIGN KEY (resource_id) REFERENCES resources(id),
    CONSTRAINT fk_booking_owner FOREIGN KEY (owner_id) REFERENCES users(id)
);

CREATE INDEX idx_issue_reporter_status ON issues(reporter_id, status);
CREATE INDEX idx_issue_assignee_status ON issues(assignee_id, status);
CREATE INDEX idx_issue_status_priority ON issues(status, priority, created_at);
CREATE INDEX idx_issue_category ON issues(category);
CREATE INDEX idx_activity_issue ON issue_activities(issue_id, created_at);
CREATE INDEX idx_booking_resource_window ON bookings(resource_id, start_at, end_at);
CREATE INDEX idx_booking_owner ON bookings(owner_id, start_at);
CREATE INDEX idx_booking_status ON bookings(status);
CREATE INDEX idx_resource_type_building ON resources(type, building);
