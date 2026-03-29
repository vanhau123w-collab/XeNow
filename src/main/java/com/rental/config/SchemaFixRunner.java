package com.rental.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.core.annotation.Order;

@Component
@RequiredArgsConstructor
@Order(1) // Run before DataSeeder
public class SchemaFixRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("🔧 Database: Cleaning up redundant columns (Brand, Model, Name)...");
        try {
            // Drop redundant columns if they exist
            jdbcTemplate.execute("ALTER TABLE Vehicle DROP COLUMN IF EXISTS Brand");
            jdbcTemplate.execute("ALTER TABLE Vehicle DROP COLUMN IF EXISTS Model");
            jdbcTemplate.execute("ALTER TABLE Vehicle DROP COLUMN IF EXISTS Name");
            System.out.println("✅ Schema cleanup successful!");
        } catch (Exception e) {
            System.err.println("⚠️ Warning: Cleanup partially completed or columns already removed: " + e.getMessage());
        }
    }
}
