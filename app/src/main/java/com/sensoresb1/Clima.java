package com.sensoresb1;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// consulta o clima na OpenWeatherMap (HttpURLConnection + JSONObject)
public class Clima {

    // chave da OpenWeatherMap: troque pela sua (nao e uma lib nova, so um texto)
    public static final String CHAVE_OPENWEATHER = "COLOQUE_SUA_CHAVE_AQUI";

    // acima deste vento (m/s) consideramos vento forte
    private static final double LIMIAR_VENTO = 10.0;

    private static final String ENDERECO = "https://api.openweathermap.org/data/2.5/weather";
    private static final int TEMPO_CONEXAO = 10000;
    private static final int TEMPO_LEITURA = 15000;

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler PRINCIPAL = new Handler(Looper.getMainLooper());

    public interface Listener {
        void onClima(String mensagem);

        void onErro(String mensagem);
    }

    private Clima() {
        // classe so com metodos estaticos
    }

    // busca o clima da posicao e devolve um texto pronto para a tela
    public static void buscar(final Context contexto, final double latitude,
                              final double longitude, final Listener ouvinte) {
        if (CHAVE_OPENWEATHER.isEmpty() || "COLOQUE_SUA_CHAVE_AQUI".equals(CHAVE_OPENWEATHER)) {
            avisarErro(ouvinte, contexto.getString(R.string.climaSemChave));
            return;
        }

        EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conexao = null;
                try {
                    String url = ENDERECO + "?lat=" + latitude + "&lon=" + longitude
                            + "&units=metric&lang=pt_br&appid=" + CHAVE_OPENWEATHER;
                    conexao = (HttpURLConnection) new URL(url).openConnection();
                    conexao.setRequestMethod("GET");
                    conexao.setConnectTimeout(TEMPO_CONEXAO);
                    conexao.setReadTimeout(TEMPO_LEITURA);

                    int codigo = conexao.getResponseCode();
                    if (codigo < 200 || codigo >= 300) {
                        avisarErro(ouvinte, contexto.getString(R.string.climaErroHttp, codigo));
                        return;
                    }

                    JSONObject raiz = new JSONObject(lerConteudo(conexao.getInputStream()));
                    avisarClima(ouvinte, montarMensagem(contexto, raiz));
                } catch (Exception e) {
                    avisarErro(ouvinte, contexto.getString(R.string.climaErro, e.getMessage()));
                } finally {
                    if (conexao != null) {
                        conexao.disconnect();
                    }
                }
            }
        });
    }

    // monta o texto com temperatura e avisos de chuva/vento
    private static String montarMensagem(Context contexto, JSONObject raiz) throws Exception {
        String descricao = "";
        JSONArray clima = raiz.optJSONArray("weather");
        if (clima != null && clima.length() > 0) {
            descricao = clima.getJSONObject(0).optString("description", "");
        }

        double temperatura = raiz.optJSONObject("main") != null
                ? raiz.getJSONObject("main").optDouble("temp") : 0;

        double vento = 0;
        JSONObject objetoVento = raiz.optJSONObject("wind");
        if (objetoVento != null) {
            vento = objetoVento.optDouble("speed", 0);
        }

        boolean temChuva = raiz.optJSONObject("rain") != null
                || descricao.toLowerCase(Locale.getDefault()).contains("chuva")
                || descricao.toLowerCase(Locale.getDefault()).contains("rain");

        StringBuilder texto = new StringBuilder();
        texto.append(contexto.getString(R.string.climaFormato, temperatura, descricao));
        if (temChuva) {
            texto.append(contexto.getString(R.string.climaAvisoChuva));
        }
        if (vento > LIMIAR_VENTO) {
            texto.append(contexto.getString(R.string.climaAvisoVento, vento * 3.6));
        }
        return texto.toString();
    }

    private static String lerConteudo(InputStream entrada) throws IOException {
        StringBuilder texto = new StringBuilder();
        BufferedReader leitor = new BufferedReader(new InputStreamReader(entrada, "UTF-8"));
        try {
            String linha = leitor.readLine();
            while (linha != null) {
                texto.append(linha);
                linha = leitor.readLine();
            }
        } finally {
            leitor.close();
        }
        return texto.toString();
    }

    private static void avisarClima(final Listener ouvinte, final String mensagem) {
        PRINCIPAL.post(new Runnable() {
            @Override
            public void run() {
                ouvinte.onClima(mensagem);
            }
        });
    }

    private static void avisarErro(final Listener ouvinte, final String mensagem) {
        PRINCIPAL.post(new Runnable() {
            @Override
            public void run() {
                ouvinte.onErro(mensagem);
            }
        });
    }
}
