package com.prisma.repository;

import com.prisma.entity.Note;
import java.util.List;
import java.util.Optional;

public interface NoteRepository extends GenericRepository<Note, Integer> {
    List<Note> findByEleveAndPeriode(Integer eleveId, Integer periodeId);
    List<Note> findByClasseAndMatiereAndPeriode(Integer classeId, Integer matiereId, Integer periodeId);
    Optional<Note> findByEleveAndMatiereAndPeriode(Integer eleveId, Integer matiereId, Integer periodeId);
}
