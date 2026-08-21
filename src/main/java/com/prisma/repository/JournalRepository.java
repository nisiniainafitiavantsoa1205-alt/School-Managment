package com.prisma.repository;

import com.prisma.entity.Journal;
import java.util.List;

public interface JournalRepository extends GenericRepository<Journal, Integer> {
    List<Journal> findRecentLogs(int limit);
}
