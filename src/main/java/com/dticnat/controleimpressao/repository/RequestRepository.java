package com.dticnat.controleimpressao.repository;

import com.dticnat.controleimpressao.model.Request;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RequestRepository extends JpaRepository<Request, Long> {
    List<Request> findAllByOrderByIdAsc();
}