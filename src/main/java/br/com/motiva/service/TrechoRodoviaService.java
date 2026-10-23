package br.com.motiva.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.motiva.dto.TrechoRodoviaRequest;
import br.com.motiva.exception.ConflitoException;
import br.com.motiva.exception.RecursoNaoEncontradoException;
import br.com.motiva.exception.RegraNegocioException;
import br.com.motiva.model.EquipeManutencao;
import br.com.motiva.model.TrechoRodovia;
import br.com.motiva.repository.EquipeManutencaoRepository;
import br.com.motiva.repository.IntervencaoOperacionalRepository;
import br.com.motiva.repository.TrechoRodoviaRepository;

@Service
public class TrechoRodoviaService {
    public static final double VALOR_MAXIMO_COLUNA = 99999999.99;
    public static final int CASAS_DECIMAIS_COLUNA = 2;

    private final TrechoRodoviaRepository trechoRepository;
    private final EquipeManutencaoRepository equipeRepository;
    private final IntervencaoOperacionalRepository intervencaoRepository;

    public TrechoRodoviaService(TrechoRodoviaRepository trechoRepository, EquipeManutencaoRepository equipeRepository,
            IntervencaoOperacionalRepository intervencaoRepository) {
        this.trechoRepository = trechoRepository;
        this.equipeRepository = equipeRepository;
        this.intervencaoRepository = intervencaoRepository;
    }

    @Transactional(readOnly = true)
    public List<TrechoRodovia> listarTodos() {
        return this.trechoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<TrechoRodovia> listarPorAlturaMinima(Double alturaMinima) {
        if (alturaMinima == null || !Double.isFinite(alturaMinima)) {
            throw new RegraNegocioException("A altura minima deve ser um numero valido.");
        }
        if (alturaMinima < 0) {
            throw new RegraNegocioException("A altura minima nao pode ser negativa.");
        }
        return this.trechoRepository.findByAlturaVegetacaoCmGreaterThanEqual(alturaMinima);
    }

    @Transactional(readOnly = true)
    public List<TrechoRodovia> listarPorRisco(String risco) {
        return this.trechoRepository.findByRiscoOperacional(this.normalizarRisco(risco));
    }

    @Transactional(readOnly = true)
    public List<TrechoRodovia> listarPorAmbiente(String ambiente) {
        return this.trechoRepository.findByTipoAmbiente(this.normalizarAmbiente(ambiente));
    }

    @Transactional(readOnly = true)
    public List<TrechoRodovia> listarPorEquipe(Long equipeId) {
        this.buscarEquipe(equipeId);
        return this.trechoRepository.findByEquipeManutencaoId(equipeId);
    }

    @Transactional(readOnly = true)
    public TrechoRodovia buscarPorId(Long id) {
        return this.trechoRepository.findById(id).orElseThrow(
                () -> new RecursoNaoEncontradoException("Trecho de rodovia com ID " + id + " nao encontrado."));
    }

    @Transactional
    public TrechoRodovia cadastrar(TrechoRodoviaRequest requisicao) {
        this.validarKms(requisicao.kmInicial(), requisicao.kmFinal());
        this.validarAltura(requisicao.alturaVegetacaoCm());
        String ambiente = this.normalizarAmbiente(requisicao.tipoAmbiente());
        String risco = this.normalizarRisco(requisicao.riscoOperacional());
        EquipeManutencao equipe = this.buscarEquipeOpcional(requisicao.equipeId());

        if (this.trechoRepository.existsByKmInicialAndKmFinal(requisicao.kmInicial(), requisicao.kmFinal())) {
            throw new ConflitoException(this.mensagemKmDuplicado(requisicao.kmInicial(), requisicao.kmFinal()));
        }

        TrechoRodovia trecho = new TrechoRodovia(requisicao.kmInicial(), requisicao.kmFinal(),
                requisicao.alturaVegetacaoCm(), ambiente, risco, equipe);
        return this.trechoRepository.save(trecho);
    }

    @Transactional
    public TrechoRodovia atualizar(Long id, TrechoRodoviaRequest requisicao) {
        TrechoRodovia trecho = this.buscarPorId(id);

        this.validarKms(requisicao.kmInicial(), requisicao.kmFinal());
        this.validarAltura(requisicao.alturaVegetacaoCm());
        String ambiente = this.normalizarAmbiente(requisicao.tipoAmbiente());
        String risco = this.normalizarRisco(requisicao.riscoOperacional());
        EquipeManutencao equipe = this.buscarEquipeOpcional(requisicao.equipeId());

        if (this.trechoRepository.existsByKmInicialAndKmFinalAndIdNot(requisicao.kmInicial(), requisicao.kmFinal(),
                id)) {
            throw new ConflitoException(this.mensagemKmDuplicado(requisicao.kmInicial(), requisicao.kmFinal()));
        }

        trecho.setKmInicial(requisicao.kmInicial());
        trecho.setKmFinal(requisicao.kmFinal());
        trecho.setAlturaVegetacaoCm(requisicao.alturaVegetacaoCm());
        trecho.setTipoAmbiente(ambiente);
        trecho.setRiscoOperacional(risco);
        trecho.setEquipeManutencao(equipe);
        return this.trechoRepository.save(trecho);
    }

    @Transactional
    public void remover(Long id) {
        TrechoRodovia trecho = this.buscarPorId(id);
        if (this.intervencaoRepository.existsByTrechoId(id)) {
            throw new ConflitoException("O trecho de rodovia com ID " + id
                    + " possui intervencoes vinculadas e nao pode ser removido.");
        }
        this.trechoRepository.delete(trecho);
    }

    private void validarKms(Double kmInicial, Double kmFinal) {
        if (kmInicial == null || kmInicial < 0) {
            throw new RegraNegocioException("O KM inicial nao pode ser negativo.");
        }
        this.validarNumeroColuna(kmInicial, "O KM inicial");
        if (kmFinal == null) {
            throw new RegraNegocioException("O KM final deve ser maior que o KM inicial.");
        }
        this.validarNumeroColuna(kmFinal, "O KM final");
        if (kmFinal <= kmInicial) {
            throw new RegraNegocioException("O KM final deve ser maior que o KM inicial.");
        }
    }

    private void validarAltura(Double alturaVegetacaoCm) {
        if (alturaVegetacaoCm == null || alturaVegetacaoCm < 0) {
            throw new RegraNegocioException("A altura da vegetacao nao pode ser negativa.");
        }
        this.validarNumeroColuna(alturaVegetacaoCm, "A altura da vegetacao");
    }

    private void validarNumeroColuna(Double valor, String campo) {
        if (!Double.isFinite(valor)) {
            throw new RegraNegocioException(campo + " deve ser um numero valido.");
        }
        if (valor > VALOR_MAXIMO_COLUNA) {
            throw new RegraNegocioException(campo + " deve ser no maximo 99999999.99.");
        }
        if (BigDecimal.valueOf(valor).stripTrailingZeros().scale() > CASAS_DECIMAIS_COLUNA) {
            throw new RegraNegocioException(campo + " deve ter no maximo duas casas decimais.");
        }
    }

    private String normalizarAmbiente(String ambiente) {
        String valor = this.normalizar(ambiente);
        if (!TrechoRodovia.AMBIENTE_SECO.equals(valor) && !TrechoRodovia.AMBIENTE_UMIDO.equals(valor)) {
            throw new RegraNegocioException("O ambiente deve ser seco ou umido.");
        }
        return valor;
    }

    private String normalizarRisco(String risco) {
        String valor = this.normalizar(risco);
        if (!TrechoRodovia.RISCO_BAIXO.equals(valor) && !TrechoRodovia.RISCO_ALTO.equals(valor)) {
            throw new RegraNegocioException("O risco deve ser baixo ou alto.");
        }
        return valor;
    }

    private String normalizar(String valor) {
        if (valor == null) {
            return null;
        }
        return valor.trim().toLowerCase(Locale.ROOT);
    }

    private EquipeManutencao buscarEquipeOpcional(Long equipeId) {
        if (equipeId == null) {
            return null;
        }
        return this.buscarEquipe(equipeId);
    }

    private EquipeManutencao buscarEquipe(Long equipeId) {
        return this.equipeRepository.findById(equipeId).orElseThrow(
                () -> new RecursoNaoEncontradoException("Equipe de manutencao com ID " + equipeId
                        + " nao encontrada."));
    }

    private String mensagemKmDuplicado(Double kmInicial, Double kmFinal) {
        return "Ja existe um trecho cadastrado do KM " + kmInicial + " ao KM " + kmFinal + ".";
    }
}
