package br.com.travessia.comum.dto;

import jakarta.validation.constraints.NotNull;

public class SolicitacaoTravessiaDto {
    @NotNull
    private Long usuarioId;

    @NotNull
    private Long semaforoId;

    @NotNull(message = "A latitude atual do aparelho (GPS) é obrigatória")
    private Double latitudeUsuario;

    @NotNull(message = "A longitude atual do aparelho (GPS) é obrigatória")
    private Double longitudeUsuario;

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public Long getSemaforoId() {
        return semaforoId;
    }

    public void setSemaforoId(Long semaforoId) {
        this.semaforoId = semaforoId;
    }

    public Double getLatitudeUsuario() {
        return latitudeUsuario;
    }

    public void setLatitudeUsuario(Double latitudeUsuario) {
        this.latitudeUsuario = latitudeUsuario;
    }

    public Double getLongitudeUsuario() {
        return longitudeUsuario;
    }

    public void setLongitudeUsuario(Double longitudeUsuario) {
        this.longitudeUsuario = longitudeUsuario;
    }
}
