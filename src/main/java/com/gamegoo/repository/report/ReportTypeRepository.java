package com.gamegoo.repository.report;

import com.gamegoo.domain.report.ReportType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReportTypeRepository extends JpaRepository<ReportType, Long> {

}
