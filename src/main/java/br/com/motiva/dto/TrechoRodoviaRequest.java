package br.com.motiva.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record TrechoRodoviaRequest(
        @NotNull(message = "O KM inicial deve ser informado.")
        @PositiveOrZero(message = "O KM inicial nao pode ser negativo.")
        Double kmInicial,

        @NotNull(message = "O KM final deve ser informado.")
        @Positive(message = "O KM final deve ser maior que zero.")
        Double kmFinal,

        @NotNull(message = "A altura da vegetacao deve ser informada.")
        Double alturaVegetacaoCm,

        @NotBlank(message = "O tipo de ambiente deve ser informado.")
        String tipoAmbiente,

        @NotBlank(message = "O risco operacional deve ser informado.")
        String riscoOperacional,

        Long equipeId) {
}
