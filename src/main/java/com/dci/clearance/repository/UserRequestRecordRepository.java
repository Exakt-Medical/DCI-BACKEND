package com.dci.clearance.repository;

import com.dci.clearance.entity.UserRequestRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRequestRecordRepository extends JpaRepository<UserRequestRecord, Long> {
    List<UserRequestRecord> findByUserIdOrderByDateUpdatedDesc(Long userId);

}
