package br.com.travessia.trafego.servico;

import br.com.travessia.comum.feign.ClimaCliente;
import br.com.travessia.trafego.entidade.Semaforo;
import br.com.travessia.trafego.entidade.SolicitacaoTravessia;
import br.com.travessia.trafego.entidade.Usuario;
import br.com.travessia.trafego.repositorio.SemaforoRepositorio;
import br.com.travessia.trafego.repositorio.SolicitacaoTravessiaRepositorio;
import br.com.travessia.trafego.repositorio.UsuarioRepositorio;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ServicoSolicitacaoTravessia {

    private static final Logger log = LoggerFactory.getLogger(ServicoSolicitacaoTravessia.class);

//    Abaixo (RAIO_TERRA_METROS) o raio médio do planeta Terra em metros,
//    usada na linha calcularDistanciaMetros para converter  graus de latitude/longitude em metros.
    private static final double RAIO_TERRA_METROS = 6371000.0;
    public static final double RAIO_MAXIMO_PROXIMIDADE_METROS = 10.0;

    private final UsuarioRepositorio usuarioRepo;
    private final SemaforoRepositorio semaforoRepo;
    private final SolicitacaoTravessiaRepositorio solicitacaoRepo;
    private final ClimaCliente climaCliente;
    private final ChatClient chatClient;
    private final String weatherApiKey;

    public ServicoSolicitacaoTravessia(UsuarioRepositorio usuarioRepo,
                                       SemaforoRepositorio semaforoRepo,
                                       SolicitacaoTravessiaRepositorio solicitacaoRepo,
                                       ClimaCliente climaCliente,
                                       ChatClient.Builder chatClientBuilder,
                                       @Value("${weather.api-key}") String weatherApiKey) {
        this.usuarioRepo = usuarioRepo;
        this.semaforoRepo = semaforoRepo;
        this.solicitacaoRepo = solicitacaoRepo;
        this.climaCliente = climaCliente;
        this.chatClient = chatClientBuilder.build();
        this.weatherApiKey = weatherApiKey;
    }

    public Map<String, Object> verificarProximidade(Long semaforoId, double latitudeUsuario, double longitudeUsuario) {
        Semaforo semaforo = semaforoRepo.findById(semaforoId)
                .orElseThrow(() -> new IllegalArgumentException("Semáforo não encontrado"));

        double distanciaMetros = calcularDistanciaMetros(
                latitudeUsuario, longitudeUsuario,
                semaforo.getLatitude(), semaforo.getLongitude()
        );
        double distanciaArredondada = Math.round(distanciaMetros * 100.0) / 100.0;
        boolean dentroDoRaio = distanciaMetros <= RAIO_MAXIMO_PROXIMIDADE_METROS;
        boolean botaoDesbloqueado = semaforo.isAtivo() && dentroDoRaio;

        Map<String, Object> status = new LinkedHashMap<>();
        status.put("semaforoId", semaforo.getId());
        status.put("descricao", semaforo.getDescricao());
        status.put("semaforoAtivo", semaforo.isAtivo());
        status.put("distanciaMetros", distanciaArredondada);
        status.put("raioMaximoMetros", RAIO_MAXIMO_PROXIMIDADE_METROS);
        status.put("proximo", dentroDoRaio);
        status.put("botaoDesbloqueado", botaoDesbloqueado);
        if (!semaforo.isAtivo()) {
            status.put("mensagem", "Semáforo inativo no momento.");
        } else if (!dentroDoRaio) {
            status.put("mensagem", String.format(Locale.US,
                    "Aproxime-se do semáforo para liberar o botão (distância atual: %.1f m, máximo permitido: %.0f m).",
                    distanciaMetros, RAIO_MAXIMO_PROXIMIDADE_METROS));
        } else {
            status.put("mensagem", "Você está próximo ao semáforo. Botão de travessia liberado!");
        }
        return status;
    }

    @Transactional
    public SolicitacaoTravessia criarSolicitacao(Long usuarioId, Long semaforoId, Double latitudeUsuario, Double longitudeUsuario) {
        Usuario usuario = usuarioRepo.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));
        Semaforo semaforo = semaforoRepo.findById(semaforoId)
                .orElseThrow(() -> new IllegalArgumentException("Semáforo não encontrado"));

        if (!semaforo.isAtivo()) {
            throw new IllegalArgumentException("O semáforo selecionado está inativo e não pode receber solicitações de travessia");
        }

        if (latitudeUsuario == null || longitudeUsuario == null) {
            throw new IllegalArgumentException("A localização GPS do aparelho (latitudeUsuario e longitudeUsuario) é obrigatória para solicitar a travessia");
        }

        double distanciaMetros = calcularDistanciaMetros(
                latitudeUsuario, longitudeUsuario,
                semaforo.getLatitude(), semaforo.getLongitude()
        );
        if (distanciaMetros > RAIO_MAXIMO_PROXIMIDADE_METROS) {
            throw new IllegalArgumentException(String.format(Locale.US,
                    "Botão bloqueado: o usuário está a %.1f metros do semáforo (máximo permitido: %.0f metros). Aproxime-se do semáforo para solicitar a travessia.",
                    distanciaMetros, RAIO_MAXIMO_PROXIMIDADE_METROS));
        }

        String descricao = consultarCondicaoClimatica(semaforo.getLatitude(), semaforo.getLongitude());
        int segundosExtras = decidirSegundosExtrasComIA(descricao);

        SolicitacaoTravessia solicitacao = new SolicitacaoTravessia();
        solicitacao.setUsuario(usuario);
        solicitacao.setSemaforo(semaforo);
        solicitacao.setDataHoraSolicitacao(LocalDateTime.now());
        solicitacao.setSegundosExtras(segundosExtras);
        solicitacao.setCondicaoClimatica(descricao);
        return solicitacaoRepo.save(solicitacao);
    }

    public double calcularDistanciaMetros(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return RAIO_TERRA_METROS * c;
    }

    private String consultarCondicaoClimatica(double latitude, double longitude) {
        try {
            Map<String, Object> clima = climaCliente.obterClima(latitude, longitude, weatherApiKey, "metric", "pt_br");
            if (clima != null && clima.get("weather") instanceof List<?> weatherList && !weatherList.isEmpty()) {
                if (weatherList.get(0) instanceof Map<?, ?> firstWeather) {
                    Object description = firstWeather.get("description");
                    if (description != null && !description.toString().isBlank()) {
                        String desc = description.toString();
                        return Character.toUpperCase(desc.charAt(0)) + desc.substring(1);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Falha ao consultar OpenWeatherMap: {}", e.getMessage());
        }
        return "Céu limpo";
    }

    private int decidirSegundosExtrasComIA(String condicaoClimatica) {
        String prompt = String.format(
                "Condição climática atual informada pelo OpenWeatherMap: '%s'. " +
                "Regra: se houver indicação de chuva, garoa ou tempestade (ex.: chuva, garoa, chuvisco, tempestade, rain, drizzle, thunderstorm), " +
                "responda APENAS com o número 10. Caso esteja seco/limpo/nublado sem chuva, responda APENAS com o número 5.",
                condicaoClimatica
        );
        try {
            String resposta = chatClient.prompt(prompt).call().content();
            log.info("Condição climática: '{}' -> Resposta OpenAI: '{}'", condicaoClimatica, resposta);
            if (resposta != null) {
                int valor = Integer.parseInt(resposta.replaceAll("\\D", ""));
                if (valor == 5 || valor == 10) {
                    return valor;
                }
            }
        } catch (Exception e) {
            log.warn("Falha ao consultar OpenAI, aplicando regra de contingência: {}", e.getMessage());
        }
        String lower = condicaoClimatica.toLowerCase();
        return (lower.contains("chuva") || lower.contains("garoa") || lower.contains("chuvisco")
                || lower.contains("tempestade") || lower.contains("rain") || lower.contains("drizzle")) ? 10 : 5;
    }
}
