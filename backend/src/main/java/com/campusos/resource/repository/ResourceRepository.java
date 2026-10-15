package com.campusos.resource.repository;

import com.campusos.resource.entity.CampusResource;
import com.campusos.resource.enums.ResourceEnums;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ResourceRepository extends JpaRepository<CampusResource, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from CampusResource r where r.id = :id")
    Optional<CampusResource> findByIdForUpdate(@Param("id") Long id);

    @Query("""
        select r from CampusResource r
        where (:type is null or r.type = :type)
          and (:building is null or lower(r.building) = lower(:building))
          and (:minCapacity is null or r.capacity >= :minCapacity)
          and (:q is null or lower(r.name) like lower(concat('%', :q, '%')))
        order by r.name asc
        """)
    List<CampusResource> search(@Param("type") ResourceEnums.Type type,
                                @Param("building") String building,
                                @Param("minCapacity") Integer minCapacity,
                                @Param("q") String q);

    List<CampusResource> findByStatus(ResourceEnums.Status status);
    List<CampusResource> findByTypeAndStatus(ResourceEnums.Type type, ResourceEnums.Status status);
}
