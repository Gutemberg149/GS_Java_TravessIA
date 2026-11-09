package br.com.travessia.gateway.controller;

import br.com.travessia.comum.dto.LoginRequestDto;
import br.com.travessia.comum.dto.UsuarioDto;
import br.com.travessia.comum.seguranca.JwtUtil;
import br.com.travessia.gateway.feign.TrafficClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação", description = "Endpoints para cadastro e login de usuários com geração de token JWT")
public class AutenticacaoController {

    private final TrafficClient trafficClient;
    private final JwtUtil jwtUtil;

    public AutenticacaoController(TrafficClient trafficClient, JwtUtil jwtUtil) {
        this.trafficClient = trafficClient;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/signup")
    @Operation(summary = "Cadastrar novo usuário", description = "Registra um novo usuário (exige ser PCD ou 60+) e retorna o token JWT")
    public ResponseEntity<Map<String, Object>> cadastrar(@Valid @RequestBody UsuarioDto usuarioDto) {
        if (!usuarioDto.isPcd() && usuarioDto.getIdade() < 60) {
            throw new IllegalArgumentException("Cadastro permitido apenas para pessoas com 60 anos ou mais (60+) ou PCD.");
        }
        Map<String, Object> usuarioCriado = trafficClient.cadastrarUsuario(usuarioDto);
        String papel = String.valueOf(usuarioCriado.getOrDefault("papel", usuarioDto.getPapel()));
        String token = jwtUtil.gerarToken(usuarioDto.getNome(), papel);

        Map<String, Object> resposta = new HashMap<>();
        resposta.put("usuario", usuarioCriado);
        resposta.put("token", token);
        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }

    @PostMapping("/login")
    @Operation(summary = "Autenticar usuário", description = "Realiza login pelo nome do usuário e retorna um token JWT")
    public ResponseEntity<Map<String, Object>> login(@Valid @RequestBody LoginRequestDto loginDto) {
        Map<String, Object> usuario = trafficClient.buscarUsuarioPorLogin(loginDto);
        String papel = String.valueOf(usuario.getOrDefault("papel", "USUARIO"));
        String token = jwtUtil.gerarToken(loginDto.getNome(), papel);

        Map<String, Object> resposta = new HashMap<>();
        resposta.put("usuario", usuario);
        resposta.put("token", token);
        return ResponseEntity.ok(resposta);
    }
}
