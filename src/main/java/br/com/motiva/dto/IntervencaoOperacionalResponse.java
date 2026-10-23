package br.com.motiva.dto;

import br.com.motiva.model.EquipeManutencao;
import br.com.motiva.model.IntervencaoOperacional;
import br.com.motiva.model.TrechoRodovia;

public record IntervencaoOperacionalResponse(Long id, String tipo, String nomeIntervencao, Long trechoId,
        String descricaoTrecho, Long equipeId, String equipeNome) {

    public static IntervencaoOperacionalResponse de(IntervencaoOperacional intervencao) {
        TrechoRodovia trecho = intervencao.getTrecho();
        EquipeManutencao equipe = intervencao.getEquipeManutencao();
        return new IntervencaoOperacionalResponse(
                intervencao.getId(),
                intervencao.getTipo(),
                obterNomeIntervencao(intervencao.getTipo()),
                trecho != null ? trecho.getId() : null,
                trecho != null ? trecho.getDescricaoKm() : null,
                equipe != null ? equipe.getId() : null,
                equipe != null ? equipe.getNome() : null);
    }

    private static String obterNomeIntervencao(String tipo) {
        if (IntervencaoOperacional.TIPO_ROCADA_MANUAL.equals(tipo)) {
            return "Rocada manual";
        }
        if (IntervencaoOperacional.TIPO_ROCADA_MECANIZADA.equals(tipo)) {
            return "Rocada mecanizada";
        }
        if (IntervencaoOperacional.TIPO_PULVERIZACAO.equals(tipo)) {
            return "Pulverizacao";
        }
        return tipo;
    }
}
