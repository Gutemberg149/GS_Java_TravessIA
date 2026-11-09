package br.com.travessia.trafego.repositorio;

import br.com.travessia.trafego.entidade.Semaforo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SemaforoRepositorio extends JpaRepository<Semaforo, Long> {
}
