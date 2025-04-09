package com.dticnat.controleimpressao.repository;

import com.dticnat.controleimpressao.model.Solicitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface SolicitationRepository extends JpaRepository<Solicitation, Long>, JpaSpecificationExecutor<Solicitation> {
}