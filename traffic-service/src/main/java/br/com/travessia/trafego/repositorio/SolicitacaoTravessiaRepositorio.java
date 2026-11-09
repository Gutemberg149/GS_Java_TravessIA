package br.com.travessia.trafego.repositorio;

import br.com.travessia.trafego.entidade.SolicitacaoTravessia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface SolicitacaoTravessiaRepositorio extends JpaRepository<SolicitacaoTravessia, Long> {
    @Transactional
    void deleteBySemaforoId(Long semaforoId);

    @Transactional
    void deleteByUsuarioId(Long usuarioId);
}
