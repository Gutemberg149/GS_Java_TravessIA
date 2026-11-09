package br.com.travessia.comum.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "clima-client", url = "${weather.base-url}")
public interface ClimaCliente {
    @GetMapping("/weather")
    Map<String, Object> obterClima(@RequestParam("lat") double lat,
                                   @RequestParam("lon") double lon,
                                   @RequestParam("appid") String apiKey,
                                   @RequestParam("units") String units,
                                   @RequestParam("lang") String lang);
}
