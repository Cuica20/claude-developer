package com.capacitacion.loanapp.infrastructure.init;

import com.capacitacion.loanapp.domain.model.Loan;
import com.capacitacion.loanapp.infrastructure.persistence.LoanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final LoanRepository loanRepository;

    @Override
    public void run(String... args) {
        if (loanRepository.count() > 0) return;

        loanRepository.save(loan("Maria García", "maria@ejemplo.com",
            LocalDate.of(1990, 3, 15), bd(4500), 720, bd(15000), 36, Loan.LoanStatus.APPROVED,
            "Aprobado automáticamente por buen score"));
        loanRepository.save(loan("Carlos López", "carlos@ejemplo.com",
            LocalDate.of(1985, 7, 22), bd(8000), 680, bd(40000), 60, Loan.LoanStatus.PENDING,
            null));
        loanRepository.save(loan("Ana Martínez", "ana@ejemplo.com",
            LocalDate.of(1992, 11, 5), bd(3000), 610, bd(8000), 24, Loan.LoanStatus.APPROVED,
            "Primera solicitud aprobada"));
        loanRepository.save(loan("Luis Rodríguez", "luis@ejemplo.com",
            LocalDate.of(1978, 1, 30), bd(12000), 750, bd(80000), 120, Loan.LoanStatus.PENDING,
            null));
        loanRepository.save(loan("Sofia Torres", "sofia@ejemplo.com",
            LocalDate.of(1995, 6, 18), bd(2800), 590, bd(12000), 48, Loan.LoanStatus.REJECTED,
            "RN-003: Score insuficiente para el monto solicitado"));
        loanRepository.save(loan("Diego Herrera", "diego@ejemplo.com",
            LocalDate.of(1988, 9, 12), bd(6500), 700, bd(30000), 48, Loan.LoanStatus.DISBURSED,
            "Desembolsado el 2024-01-15"));
        loanRepository.save(loan("Elena Ruiz", "elena@ejemplo.com",
            LocalDate.of(2000, 4, 25), bd(2200), 640, bd(5000), 18, Loan.LoanStatus.PENDING,
            null));
        loanRepository.save(loan("Miguel Vargas", "miguel@ejemplo.com",
            LocalDate.of(1975, 12, 3), bd(15000), 780, bd(200000), 240, Loan.LoanStatus.APPROVED,
            "Préstamo hipotecario aprobado"));

        log.info("Base de datos inicializada con {} solicitudes de ejemplo", loanRepository.count());
    }

    private Loan loan(String name, String email, LocalDate birth, BigDecimal income,
                      int score, BigDecimal amount, int term, Loan.LoanStatus status, String notes) {
        BigDecimal rate = amount.compareTo(bd(50000)) > 0 ? bd(0.0085)
                        : amount.compareTo(bd(20000)) > 0 ? bd(0.0095) : bd(0.0110);
        double pow = Math.pow(1 + rate.doubleValue(), -term);
        BigDecimal installment = amount.multiply(rate)
            .divide(BigDecimal.ONE.subtract(BigDecimal.valueOf(pow)), 2, java.math.RoundingMode.HALF_UP);
        return Loan.builder()
            .applicantName(name).applicantEmail(email).birthDate(birth)
            .monthlyIncome(income).creditScore(score)
            .amount(amount).termMonths(term).monthlyInstallment(installment)
            .status(status).createdAt(LocalDateTime.now().minusDays((long)(Math.random()*90)))
            .notes(notes).build();
    }

    private BigDecimal bd(double v) { return BigDecimal.valueOf(v); }
}
