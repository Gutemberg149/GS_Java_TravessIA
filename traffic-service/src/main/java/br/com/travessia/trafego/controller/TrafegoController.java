package br.com.travessia.trafego.controller;

import br.com.travessia.comum.dto.LoginRequestDto;
import br.com.travessia.comum.dto.SemaforoDto;
import br.com.travessia.comum.dto.SolicitacaoTravessiaDto;
import br.com.travessia.comum.dto.UsuarioDto;
import br.com.travessia.comum.enums.Papel;
import br.com.travessia.trafego.entidade.Semaforo;
import br.com.travessia.trafego.entidade.SolicitacaoTravessia;
import br.com.travessia.trafego.entidade.Usuario;
import br.com.travessia.trafego.repositorio.SemaforoRepositorio;
import br.com.travessia.trafego.repositorio.SolicitacaoTravessiaRepositorio;
import br.com.travessia.trafego.repositorio.UsuarioRepositorio;
import br.com.travessia.trafego.servico.ServicoSolicitacaoTravessia;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class TrafegoController {

    private final UsuarioRepositorio usuarioRepo;
    private final SemaforoRepositorio semaforoRepo;
    private final SolicitacaoTravessiaRepositorio solicitacaoRepo;
    private final ServicoSolicitacaoTravessia servicoSolicitacao;

    public TrafegoController(UsuarioRepositorio usuarioRepo,
                             SemaforoRepositorio semaforoRepo,
                             SolicitacaoTravessiaRepositorio solicitacaoRepo,
                             ServicoSolicitacaoTravessia servicoSolicitacao) {
        this.usuarioRepo = usuarioRepo;
        this.semaforoRepo = semaforoRepo;
        this.solicitacaoRepo = solicitacaoRepo;
        this.servicoSolicitacao = servicoSolicitacao;
    }

    @PostMapping("/usuarios")
    public ResponseEntity<Usuario> criarUsuario(@Valid @RequestBody UsuarioDto dto) {
        if (!dto.isPcd() && dto.getIdade() < 60) {
            throw new IllegalArgumentException("Cadastro permitido apenas para pessoas com 60 anos ou mais (60+) ou PCD.");
        }
        Papel papel = Papel.valueOf(dto.getPapel().toUpperCase());
        Usuario u = new Usuario();
        u.setNome(dto.getNome());
        u.setIdade(dto.getIdade());
        u.setPcd(dto.isPcd());
        u.setPapel(papel);
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioRepo.save(u));
    }

    @PostMapping("/usuarios/login")
    public ResponseEntity<Usuario> loginUsuario(@Valid @RequestBody LoginRequestDto dto) {
        Usuario u = usuarioRepo.findByNome(dto.getNome())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado: " + dto.getNome()));
        return ResponseEntity.ok(u);
    }

    @GetMapping("/usuarios")
    public ResponseEntity<Page<Usuario>> listarUsuarios(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(usuarioRepo.findAll(PageRequest.of(page, size)));
    }

    @GetMapping("/usuarios/{id}")
    public ResponseEntity<Usuario> obterUsuario(@PathVariable(name = "id") Long id) {
        Usuario u = usuarioRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        return ResponseEntity.ok(u);
    }

    @PutMapping("/usuarios/{id}")
    public ResponseEntity<Usuario> atualizarUsuario(@PathVariable(name = "id") Long id, @Valid @RequestBody UsuarioDto dto) {
        if (!dto.isPcd() && dto.getIdade() < 60) {
            throw new IllegalArgumentException("Cadastro permitido apenas para pessoas com 60 anos ou mais (60+) ou PCD.");
        }
        Papel papel = Papel.valueOf(dto.getPapel().toUpperCase());
        Usuario u = usuarioRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        u.setNome(dto.getNome());
        u.setIdade(dto.getIdade());
        u.setPcd(dto.isPcd());
        u.setPapel(papel);
        return ResponseEntity.ok(usuarioRepo.save(u));
    }

    @DeleteMapping("/usuarios/{id}")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<Void> removerUsuario(@PathVariable(name = "id") Long id) {
        solicitacaoRepo.deleteByUsuarioId(id);
        usuarioRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/semaforos")
    public ResponseEntity<Semaforo> criarSemaforo(@Valid @RequestBody SemaforoDto dto) {
        Semaforo s = new Semaforo();
        s.setDescricao(dto.getDescricao());
        s.setLatitude(dto.getLatitude());
        s.setLongitude(dto.getLongitude());
        s.setAtivo(dto.getAtivo() != null ? dto.getAtivo() : true);
        return ResponseEntity.status(HttpStatus.CREATED).body(semaforoRepo.save(s));
    }

    @GetMapping("/semaforos")
    public ResponseEntity<Page<Semaforo>> listarSemaforos(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(semaforoRepo.findAll(PageRequest.of(page, size)));
    }

    @GetMapping("/semaforos/{id}")
    public ResponseEntity<Semaforo> obterSemaforo(@PathVariable(name = "id") Long id) {
        Semaforo s = semaforoRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Semáforo não encontrado"));
        return ResponseEntity.ok(s);
    }

    @GetMapping("/semaforos/{id}/proximidade")
    public ResponseEntity<Map<String, Object>> verificarProximidade(
            @PathVariable(name = "id") Long id,
            @RequestParam(name = "latitude") double latitude,
            @RequestParam(name = "longitude") double longitude) {
        return ResponseEntity.ok(servicoSolicitacao.verificarProximidade(id, latitude, longitude));
    }

    @PutMapping("/semaforos/{id}")
    public ResponseEntity<Semaforo> atualizarSemaforo(@PathVariable(name = "id") Long id, @Valid @RequestBody SemaforoDto dto) {
        Semaforo s = semaforoRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Semáforo não encontrado"));
        s.setDescricao(dto.getDescricao());
        s.setLatitude(dto.getLatitude());
        s.setLongitude(dto.getLongitude());
        if (dto.getAtivo() != null) {
            s.setAtivo(dto.getAtivo());
        }
        return ResponseEntity.ok(semaforoRepo.save(s));
    }

    @DeleteMapping("/semaforos/{id}")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<Void> removerSemaforo(@PathVariable(name = "id") Long id) {
        solicitacaoRepo.deleteBySemaforoId(id);
        semaforoRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/solicitacoes")
    public ResponseEntity<SolicitacaoTravessia> criarSolicitacao(@Valid @RequestBody SolicitacaoTravessiaDto dto) {
        SolicitacaoTravessia criada = servicoSolicitacao.criarSolicitacao(
                dto.getUsuarioId(),
                dto.getSemaforoId(),
                dto.getLatitudeUsuario(),
                dto.getLongitudeUsuario()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(criada);
    }

    @GetMapping("/solicitacoes")
    public ResponseEntity<Page<SolicitacaoTravessia>> listarSolicitacoes(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(solicitacaoRepo.findAll(PageRequest.of(page, size)));
    }

    @GetMapping("/solicitacoes/{id}")
    public ResponseEntity<SolicitacaoTravessia> obterSolicitacao(@PathVariable(name = "id") Long id) {
        SolicitacaoTravessia st = solicitacaoRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Solicitação não encontrada"));
        return ResponseEntity.ok(st);
    }

    @PutMapping("/solicitacoes/{id}")
    public ResponseEntity<SolicitacaoTravessia> atualizarSolicitacao(
            @PathVariable(name = "id") Long id,
            @Valid @RequestBody SolicitacaoTravessiaDto dto) {
        SolicitacaoTravessia st = solicitacaoRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Solicitação não encontrada"));
        Usuario u = usuarioRepo.findById(dto.getUsuarioId())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        Semaforo s = semaforoRepo.findById(dto.getSemaforoId())
                .orElseThrow(() -> new IllegalArgumentException("Semáforo não encontrado"));
        st.setUsuario(u);
        st.setSemaforo(s);
        return ResponseEntity.ok(solicitacaoRepo.save(st));
    }

    @DeleteMapping("/solicitacoes/{id}")
    public ResponseEntity<Void> removerSolicitacao(@PathVariable(name = "id") Long id) {
        solicitacaoRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
