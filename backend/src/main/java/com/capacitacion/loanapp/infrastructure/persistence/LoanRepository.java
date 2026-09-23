package com.capacitacion.loanapp.infrastructure.persistence;

import com.capacitacion.loanapp.domain.model.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {
    List<Loan> findByApplicantEmailOrderByCreatedAtDesc(String email);
    List<Loan> findByStatusOrderByCreatedAtDesc(Loan.LoanStatus status);
}
