package com.SoundFork.SoundFork.common.migration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class MigrationRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public MigrationRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        try {
            Boolean exists = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='projects' AND column_name='status')",
                Boolean.class);
            if (Boolean.TRUE.equals(exists)) {
                jdbcTemplate.execute("ALTER TABLE projects DROP COLUMN status");
                log.info("Migration: dropped column 'status' from projects table");
            }
        } catch (Exception e) {
            log.warn("Migration (drop status column) failed: {}", e.getMessage());
        }
        try {
            List<Map<String, Object>> tables = jdbcTemplate.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'");
            log.info("Database tables: {}", tables.stream().map(t -> (String)t.get("table_name")).toList());
        } catch (Exception e) {
            log.warn("Could not list tables: {}", e.getMessage());
        }
    }
}
