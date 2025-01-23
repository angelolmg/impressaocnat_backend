package com.dticnat.controleimpressao.repository;


import com.dticnat.controleimpressao.model.Copy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CopyRepository extends JpaRepository<Copy, Long> {
    List<Copy> findAllByRequestId(Long id);
}