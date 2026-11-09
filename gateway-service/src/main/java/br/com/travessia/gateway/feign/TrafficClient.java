package br.com.travessia.gateway.feign;

import br.com.travessia.comum.dto.LoginRequestDto;
import br.com.travessia.comum.dto.SemaforoDto;
import br.com.travessia.comum.dto.SolicitacaoTravessiaDto;
import br.com.travessia.comum.dto.UsuarioDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "traffic-service", url = "${traffic-service.url:http://traffic:8081}")
public interface TrafficClient {

    @PostMapping("/api/usuarios")
    Map<String, Object> cadastrarUsuario(@RequestBody UsuarioDto usuario);

    @PostMapping("/api/usuarios/login")
    Map<String, Object> buscarUsuarioPorLogin(@RequestBody LoginRequestDto login);

    @GetMapping("/api/usuarios")
    Map<String, Object> listarUsuarios(@RequestParam("page") int page, @RequestParam("size") int size);

    @GetMapping("/api/usuarios/{id}")
    Map<String, Object> obterUsuario(@PathVariable("id") Long id);

    @PutMapping("/api/usuarios/{id}")
    Map<String, Object> atualizarUsuario(@PathVariable("id") Long id, @RequestBody UsuarioDto usuario);

    @DeleteMapping("/api/usuarios/{id}")
    void removerUsuario(@PathVariable("id") Long id);

    @PostMapping("/api/semaforos")
    Map<String, Object> criarSemaforo(@RequestBody SemaforoDto semaforo);

    @GetMapping("/api/semaforos")
    Map<String, Object> listarSemaforos(@RequestParam("page") int page, @RequestParam("size") int size);

    @GetMapping("/api/semaforos/{id}")
    Map<String, Object> obterSemaforo(@PathVariable("id") Long id);

    @GetMapping("/api/semaforos/{id}/proximidade")
    Map<String, Object> verificarProximidade(
            @PathVariable("id") Long id,
            @RequestParam("latitude") double latitude,
            @RequestParam("longitude") double longitude);

    @PutMapping("/api/semaforos/{id}")
    Map<String, Object> atualizarSemaforo(@PathVariable("id") Long id, @RequestBody SemaforoDto semaforo);

    @DeleteMapping("/api/semaforos/{id}")
    void removerSemaforo(@PathVariable("id") Long id);

    @PostMapping("/api/solicitacoes")
    Map<String, Object> criarSolicitacao(@RequestBody SolicitacaoTravessiaDto solicitacao);

    @GetMapping("/api/solicitacoes")
    Map<String, Object> listarSolicitacoes(@RequestParam("page") int page, @RequestParam("size") int size);

    @GetMapping("/api/solicitacoes/{id}")
    Map<String, Object> obterSolicitacao(@PathVariable("id") Long id);

    @PutMapping("/api/solicitacoes/{id}")
    Map<String, Object> atualizarSolicitacao(@PathVariable("id") Long id, @RequestBody SolicitacaoTravessiaDto solicitacao);

    @DeleteMapping("/api/solicitacoes/{id}")
    void removerSolicitacao(@PathVariable("id") Long id);
}
