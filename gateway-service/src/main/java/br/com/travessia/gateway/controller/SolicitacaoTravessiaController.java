package br.com.travessia.gateway.controller;

import br.com.travessia.comum.dto.SemaforoDto;
import br.com.travessia.comum.dto.SolicitacaoTravessiaDto;
import br.com.travessia.comum.dto.UsuarioDto;
import br.com.travessia.gateway.feign.TrafficClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "TravessIA Gateway", description = "Endpoints de gerenciamento de solicitações de travessia, semáforos e usuários")
public class SolicitacaoTravessiaController {

    private final TrafficClient trafficClient;

    public SolicitacaoTravessiaController(TrafficClient trafficClient) {
        this.trafficClient = trafficClient;
    }

    @PostMapping("/solicitacoes")
    @Operation(summary = "Solicitar tempo extra de travessia", description = "Exige que o aparelho do usuário esteja a no máximo 10 metros do semáforo ativo; consulta clima no OpenWeatherMap (pt_br) e usa IA para definir 5s (seco) ou 10s (chuva).")
    public ResponseEntity<Map<String, Object>> criarSolicitacao(@Valid @RequestBody SolicitacaoTravessiaDto solicitacao) {
        return ResponseEntity.status(HttpStatus.CREATED).body(trafficClient.criarSolicitacao(solicitacao));
    }

    @GetMapping("/solicitacoes")
    @Operation(summary = "Listar solicitações de travessia (paginado)")
    public ResponseEntity<Map<String, Object>> listarSolicitacoes(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(trafficClient.listarSolicitacoes(page, size));
    }

    @GetMapping("/solicitacoes/{id}")
    @Operation(summary = "Buscar solicitação de travessia por ID")
    public ResponseEntity<Map<String, Object>> obterSolicitacao(@PathVariable(name = "id") Long id) {
        return ResponseEntity.ok(trafficClient.obterSolicitacao(id));
    }

    @PutMapping("/solicitacoes/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Atualizar solicitação de travessia (Apenas ADMIN)")
    public ResponseEntity<Map<String, Object>> atualizarSolicitacao(
            @PathVariable(name = "id") Long id,
            @Valid @RequestBody SolicitacaoTravessiaDto solicitacao) {
        return ResponseEntity.ok(trafficClient.atualizarSolicitacao(id, solicitacao));
    }

    @DeleteMapping("/solicitacoes/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Remover solicitação de travessia (Apenas ADMIN)")
    public ResponseEntity<Void> removerSolicitacao(@PathVariable(name = "id") Long id) {
        trafficClient.removerSolicitacao(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/semaforos")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cadastrar semáforo (Apenas ADMIN)", description = "Apenas administradores (ADMIN) podem adicionar novos semáforos; usuários comuns (USUARIO) não possuem permissão.")
    public ResponseEntity<Map<String, Object>> criarSemaforo(@Valid @RequestBody SemaforoDto semaforo) {
        return ResponseEntity.status(HttpStatus.CREATED).body(trafficClient.criarSemaforo(semaforo));
    }

    @GetMapping("/semaforos")
    @Operation(summary = "Listar semáforos (paginado)")
    public ResponseEntity<Map<String, Object>> listarSemaforos(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(trafficClient.listarSemaforos(page, size));
    }

    @GetMapping("/semaforos/{id}")
    @Operation(summary = "Buscar semáforo por ID")
    public ResponseEntity<Map<String, Object>> obterSemaforo(@PathVariable(name = "id") Long id) {
        return ResponseEntity.ok(trafficClient.obterSemaforo(id));
    }

    @GetMapping("/semaforos/{id}/proximidade")
    @Operation(summary = "Verificar proximidade GPS para desbloquear botão no App", description = "Calcula a distância em metros entre o GPS atual do celular (latitude/longitude) e o semáforo. Retorna botaoDesbloqueado=true apenas se o semáforo estiver ativo e a distância for <= 10m.")
    public ResponseEntity<Map<String, Object>> verificarProximidade(
            @PathVariable(name = "id") Long id,
            @RequestParam(name = "latitude") double latitude,
            @RequestParam(name = "longitude") double longitude) {
        return ResponseEntity.ok(trafficClient.verificarProximidade(id, latitude, longitude));
    }

    @PutMapping("/semaforos/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Atualizar semáforo (Apenas ADMIN)")
    public ResponseEntity<Map<String, Object>> atualizarSemaforo(
            @PathVariable(name = "id") Long id,
            @Valid @RequestBody SemaforoDto semaforo) {
        return ResponseEntity.ok(trafficClient.atualizarSemaforo(id, semaforo));
    }

    @DeleteMapping("/semaforos/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Remover semáforo (Apenas ADMIN)")
    public ResponseEntity<Void> removerSemaforo(@PathVariable(name = "id") Long id) {
        trafficClient.removerSemaforo(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/usuarios")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar usuários (paginado - Apenas ADMIN)")
    public ResponseEntity<Map<String, Object>> listarUsuarios(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(trafficClient.listarUsuarios(page, size));
    }

    @GetMapping("/usuarios/{id}")
    @Operation(summary = "Buscar usuário por ID")
    public ResponseEntity<Map<String, Object>> obterUsuario(@PathVariable(name = "id") Long id) {
        return ResponseEntity.ok(trafficClient.obterUsuario(id));
    }

    @PutMapping("/usuarios/{id}")
    @Operation(summary = "Atualizar usuário")
    public ResponseEntity<Map<String, Object>> atualizarUsuario(
            @PathVariable(name = "id") Long id,
            @Valid @RequestBody UsuarioDto usuario) {
        return ResponseEntity.ok(trafficClient.atualizarUsuario(id, usuario));
    }

    @DeleteMapping("/usuarios/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Remover usuário (Apenas ADMIN)")
    public ResponseEntity<Void> removerUsuario(@PathVariable(name = "id") Long id) {
        trafficClient.removerUsuario(id);
        return ResponseEntity.noContent().build();
    }
}
