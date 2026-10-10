package com.sensoresb1;

// detecta queda de celular a partir do acelerometro e do giroscopio
public class DetectorQueda {

    // limiar de impacto em m/s2 (2,5G equivale a cerca de 25 m/s2)
    private static final float LIMIAR_IMPACTO = 25.0f;

    // rotacao brusca considerada na queda, em rad/s
    private static final float LIMIAR_GIROSC_PICO = 4.0f;
    // janela apos o impacto para valer a rotacao brusca
    private static final long JANELA_GIROSC_PICO = 1000;

    // modulo da aceleracao em repouso (quase parado)
    private static final float GRAVIDADE = 9.81f;
    private static final float TOLERANCIA_PARADO = 1.5f;
    // tempo que o aparelho precisa ficar parado para confirmar
    private static final long JANELA_PARADO = 2000;

    // tempo maximo esperando a confirmacao depois do impacto
    private static final long JANELA_CONFIRMACAO = 4000;

    // anti falso-positivo: tempo minimo entre dois disparos
    private static final long INTERVALO_ENTRE_QUEDAS = 60000;

    // refinamento por luz: abaixo disso a tela esta no escuro (bolso)
    private static final float LIMIAR_ESCURO = 50.0f;

    public interface Listener {
        void onQuedaDetectada(float gMax);
    }

    private final Listener ouvinte;

    // estado da deteccao em andamento
    private boolean emAlerta;
    private long tempoImpacto;
    private float gMax;
    private boolean rotacaoBrusca;
    private boolean proximidadePerto;
    private boolean luzEscura;
    private long inicioParado;
    private long ultimoDisparo;

    public DetectorQueda(Listener ouvinte) {
        this.ouvinte = ouvinte;
    }

    // alimenta o acelerometro, em m/s2
    public void atualizarAceleracao(float x, float y, float z, long agora) {
        float modulo = (float) Math.sqrt(x * x + y * y + z * z);

        // impacto inicia a deteccao, respeitando o intervalo minimo entre quedas
        if (!emAlerta && modulo > LIMIAR_IMPACTO
                && (agora - ultimoDisparo) > INTERVALO_ENTRE_QUEDAS) {
            emAlerta = true;
            tempoImpacto = agora;
            gMax = modulo;
            rotacaoBrusca = false;
            inicioParado = 0;
            return;
        }

        if (!emAlerta) {
            return;
        }

        if (modulo > gMax) {
            gMax = modulo;
        }

        // aparelho voltou a ficar quase parado sob a gravidade por tempo suficiente
        if (Math.abs(modulo - GRAVIDADE) <= TOLERANCIA_PARADO) {
            if (inicioParado == 0) {
                inicioParado = agora;
            } else if ((agora - inicioParado) >= JANELA_PARADO) {
                confirmar(agora);
                return;
            }
        } else {
            inicioParado = 0;
        }

        // estourou a janela sem confirmar: cancela
        if ((agora - tempoImpacto) > JANELA_CONFIRMACAO) {
            emAlerta = false;
        }
    }

    // alimenta o giroscopio, em rad/s
    public void atualizarGiroscopio(float x, float y, float z, long agora) {
        if (!emAlerta) {
            return;
        }
        float modulo = (float) Math.sqrt(x * x + y * y + z * z);
        // rotacao brusca dentro da janela confirma a queda
        if (modulo > LIMIAR_GIROSC_PICO && (agora - tempoImpacto) <= JANELA_GIROSC_PICO) {
            rotacaoBrusca = true;
            confirmar(agora);
        }
    }

    // alimenta a proximidade: perto quando abaixo do alcance maximo
    public void atualizarProximidade(float valor, float alcanceMaximo) {
        proximidadePerto = valor < alcanceMaximo;
    }

    // alimenta a luminosidade
    public void atualizarLuminosidade(float lux) {
        luzEscura = lux < LIMIAR_ESCURO;
    }

    // fecha a deteccao e avisa o ouvinte
    private void confirmar(long agora) {
        // perto e escuro sem rotacao indica celular no bolso: ignora
        boolean noBolso = (proximidadePerto || luzEscura) && !rotacaoBrusca;
        emAlerta = false;
        if (noBolso) {
            return;
        }
        ultimoDisparo = agora;
        ouvinte.onQuedaDetectada(gMax / GRAVIDADE);
    }

    // dispara uma sequencia falsa de queda para testar a tela
    public void simularQueda() {
        ultimoDisparo = 0;
        emAlerta = false;
        proximidadePerto = false;
        luzEscura = false;
        long agora = System.currentTimeMillis();
        // impacto de 3G seguido de rotacao brusca
        atualizarAceleracao(0f, 0f, 30f, agora);
        atualizarGiroscopio(0f, 0f, 10f, agora + 50);
    }
}
