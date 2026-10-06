package com.sensoresb1;

import android.app.Activity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// envia os dados dos sensores para a API Socorro (POST /socorro/)
public class ApiSocorro {

    // servidor de producao + caminho do endpoint
    private static final String ENDERECO = "https://ideaberta.com.br/socorro/";

    private static final String CONTEUDO_JSON = "application/json; charset=utf-8";
    private static final String CONTEUDO_FORMULARIO = "application/x-www-form-urlencoded; charset=utf-8";

    // tempos de espera da conexao, em milissegundos
    private static final int TEMPO_CONEXAO = 10000;
    private static final int TEMPO_LEITURA = 15000;

    // valores aceitos pelo campo tipo_registro
    public static final int TIPO_NORMAL = 1;
    public static final int TIPO_QUEDA = 2;

    // a tela nao pode ser chamada de fora da main thread, entao o envio roda no executor
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    // retorno do resultado para a Activity
    public interface RespostaListener {
        void onSucesso(String mensagem, int id);

        void onErro(String mensagem);
    }

    private ApiSocorro() {
        // classe so com metodos estaticos
    }

    // envia o registro em JSON (Content-Type: application/json)
    public static void enviar(final Activity tela, final String usuario, final Integer tipoRegistro,
                              final String deviceId, final String modelo, final Double bateria,
                              final Double latitude, final Double longitude, final Double velocidade,
                              final Double temperatura, final Double pressao, final Double nivelQueda,
                              final RespostaListener ouvinte) {
        EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // campo null e omitido do corpo, todos os campos sao opcionais
                    JSONObject corpo = new JSONObject();
                    if (usuario != null) {
                        corpo.put("usuario", usuario);
                    }
                    if (tipoRegistro != null) {
                        corpo.put("tipo_registro", tipoRegistro.intValue());
                    }
                    if (deviceId != null) {
                        corpo.put("device_id", deviceId);
                    }
                    if (modelo != null) {
                        corpo.put("modelo", modelo);
                    }
                    if (bateria != null) {
                        corpo.put("bateria", bateria.doubleValue());
                    }
                    if (latitude != null) {
                        corpo.put("latitude", latitude.doubleValue());
                    }
                    if (longitude != null) {
                        corpo.put("longitude", longitude.doubleValue());
                    }
                    if (velocidade != null) {
                        corpo.put("velocidade", velocidade.doubleValue());
                    }
                    if (temperatura != null) {
                        corpo.put("temperatura", temperatura.doubleValue());
                    }
                    if (pressao != null) {
                        corpo.put("pressao", pressao.doubleValue());
                    }
                    if (nivelQueda != null) {
                        corpo.put("nivel_queda", nivelQueda.doubleValue());
                    }
                    executar(tela, corpo.toString().getBytes("UTF-8"), CONTEUDO_JSON, ouvinte);
                } catch (JSONException e) {
                    avisarErro(tela, "Corpo invalido: " + e.getMessage(), ouvinte);
                } catch (IOException e) {
                    avisarErro(tela, "Falha ao montar o corpo: " + e.getMessage(), ouvinte);
                }
            }
        });
    }

    // envia o registro como formulario (Content-Type: application/x-www-form-urlencoded)
    public static void enviarFormulario(final Activity tela, final String usuario, final Integer tipoRegistro,
                                        final String deviceId, final String modelo, final Double bateria,
                                        final Double latitude, final Double longitude, final Double velocidade,
                                        final Double temperatura, final Double pressao, final Double nivelQueda,
                                        final RespostaListener ouvinte) {
        EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // monta a query string ignorando os campos nulos
                    StringBuilder corpo = new StringBuilder();
                    anexar(corpo, "usuario", usuario);
                    if (tipoRegistro != null) {
                        anexar(corpo, "tipo_registro", String.valueOf(tipoRegistro));
                    }
                    anexar(corpo, "device_id", deviceId);
                    anexar(corpo, "modelo", modelo);
                    if (bateria != null) {
                        anexar(corpo, "bateria", String.valueOf(bateria));
                    }
                    if (latitude != null) {
                        anexar(corpo, "latitude", String.valueOf(latitude));
                    }
                    if (longitude != null) {
                        anexar(corpo, "longitude", String.valueOf(longitude));
                    }
                    if (velocidade != null) {
                        anexar(corpo, "velocidade", String.valueOf(velocidade));
                    }
                    if (temperatura != null) {
                        anexar(corpo, "temperatura", String.valueOf(temperatura));
                    }
                    if (pressao != null) {
                        anexar(corpo, "pressao", String.valueOf(pressao));
                    }
                    if (nivelQueda != null) {
                        anexar(corpo, "nivel_queda", String.valueOf(nivelQueda));
                    }
                    executar(tela, corpo.toString().getBytes("UTF-8"), CONTEUDO_FORMULARIO, ouvinte);
                } catch (IOException e) {
                    avisarErro(tela, "Falha ao montar o corpo: " + e.getMessage(), ouvinte);
                }
            }
        });
    }

    // monta e envia o POST, sempre fora da main thread
    private static void executar(Activity tela, byte[] corpo, String tipoConteudo, RespostaListener ouvinte) {
        HttpURLConnection conexao = null;
        try {
            conexao = (HttpURLConnection) new URL(ENDERECO).openConnection();
            conexao.setRequestMethod("POST");
            conexao.setConnectTimeout(TEMPO_CONEXAO);
            conexao.setReadTimeout(TEMPO_LEITURA);
            conexao.setUseCaches(false);
            conexao.setDoInput(true);
            conexao.setDoOutput(true);
            conexao.setRequestProperty("Content-Type", tipoConteudo);
            conexao.setRequestProperty("Accept", "application/json");

            OutputStream saida = conexao.getOutputStream();
            try {
                saida.write(corpo);
                saida.flush();
            } finally {
                saida.close();
            }

            int codigo = conexao.getResponseCode();
            boolean deuCerto = codigo >= 200 && codigo < 300;
            String resposta = lerConteudo(deuCerto ? conexao.getInputStream() : conexao.getErrorStream());

            if (deuCerto) {
                avisarSucesso(tela, resposta, ouvinte);
            } else {
                avisarErro(tela, "HTTP " + codigo + ": " + extrairMensagem(resposta), ouvinte);
            }
        } catch (IOException e) {
            avisarErro(tela, "Erro de conexao: " + e.getMessage(), ouvinte);
        } finally {
            if (conexao != null) {
                conexao.disconnect();
            }
        }
    }

    // le o corpo da resposta inteira
    private static String lerConteudo(InputStream entrada) throws IOException {
        if (entrada == null) {
            return "";
        }
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

    // separa a mensagem do corpo da resposta, ou devolve o texto cru
    private static String extrairMensagem(String resposta) {
        try {
            return new JSONObject(resposta).optString("mensagem", resposta);
        } catch (JSONException e) {
            return resposta;
        }
    }

    // joga o resultado na tela
    private static void avisarSucesso(Activity tela, String resposta, final RespostaListener ouvinte) {
        String mensagem = resposta;
        int id = 0;
        try {
            JSONObject objeto = new JSONObject(resposta);
            mensagem = objeto.optString("mensagem", resposta);
            id = objeto.optInt("id", 0);
        } catch (JSONException e) {
            mensagem = resposta;
        }
        final String texto = mensagem;
        final int numero = id;
        tela.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ouvinte.onSucesso(texto, numero);
            }
        });
    }

    private static void avisarErro(Activity tela, String mensagem, final RespostaListener ouvinte) {
        final String texto = mensagem;
        tela.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ouvinte.onErro(texto);
            }
        });
    }

    // separa os campos do formulario com & e escapa o valor
    private static void anexar(StringBuilder corpo, String nome, String valor) throws UnsupportedEncodingException {
        if (valor == null) {
            return;
        }
        if (corpo.length() > 0) {
            corpo.append("&");
        }
        corpo.append(nome).append("=").append(codificar(valor));
    }

    private static String codificar(String valor) throws UnsupportedEncodingException {
        return URLEncoder.encode(valor, "UTF-8");
    }
}