package br.com.travessia.trafego.repositorio;

import br.com.travessia.trafego.entidade.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UsuarioRepositorio extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByNome(String nome);
}