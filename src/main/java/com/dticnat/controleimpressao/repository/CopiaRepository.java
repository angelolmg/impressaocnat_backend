package com.dticnat.controleimpressao.repository;


import com.dticnat.controleimpressao.model.Copia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CopiaRepository extends JpaRepository<Copia, Long> {
}