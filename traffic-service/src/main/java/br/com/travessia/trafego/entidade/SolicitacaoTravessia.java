package br.com.travessia.trafego.entidade;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "TB_TRAVESSIA_SOLICITACAO")
public class SolicitacaoTravessia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "USUARIO_ID", nullable = false)
    private Usuario usuario;

    @ManyToOne
    @JoinColumn(name = "SEMAFORO_ID", nullable = false)
    private Semaforo semaforo;

    @Column(nullable = false)
    private LocalDateTime dataHoraSolicitacao;

    @Column(nullable = false)
    private int segundosExtras;

    @Column(name = "CONDICAO_CLIMATICA")
    private String condicaoClimatica;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Semaforo getSemaforo() {
        return semaforo;
    }

    public void setSemaforo(Semaforo semaforo) {
        this.semaforo = semaforo;
    }

    public LocalDateTime getDataHoraSolicitacao() {
        return dataHoraSolicitacao;
    }

    public void setDataHoraSolicitacao(LocalDateTime dataHoraSolicitacao) {
        this.dataHoraSolicitacao = dataHoraSolicitacao;
    }

    public int getSegundosExtras() {
        return segundosExtras;
    }

    public void setSegundosExtras(int segundosExtras) {
        this.segundosExtras = segundosExtras;
    }

    public String getCondicaoClimatica() {
        return condicaoClimatica;
    }

    public void setCondicaoClimatica(String condicaoClimatica) {
        this.condicaoClimatica = condicaoClimatica;
    }
}
