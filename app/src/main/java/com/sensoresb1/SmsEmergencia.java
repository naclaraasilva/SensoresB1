package com.sensoresb1;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SmsManager;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// envia o SMS automatico de socorro para o contato principal
public class SmsEmergencia {

    // o envio roda fora da main thread, como no ApiSocorro
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler PRINCIPAL = new Handler(Looper.getMainLooper());

    public interface RespostaListener {
        void onSucesso(String mensagem);

        void onErro(String mensagem);
    }

    private SmsEmergencia() {
        // classe so com metodos estaticos
    }

    // envia o texto para o contato principal; avisa o resultado pelo ouvinte
    public static void enviar(final Context contexto, final String texto,
                              final RespostaListener ouvinte) {
        if (!temPermissao(contexto)) {
            avisarErro(ouvinte, contexto.getString(R.string.smsSemPermissao));
            return;
        }
        final String telefone = ContatosEmergencia.obterTelefonePrincipal(contexto);
        if (telefone.isEmpty()) {
            avisarErro(ouvinte, contexto.getString(R.string.smsSemContato));
            return;
        }

        // retorno para a Activity precisa acontecer na main thread
        EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    SmsManager gerenciador = obterGerenciador(contexto);
                    // a mensagem tem o link do mapa, entao pode passar de 160 caracteres
                    ArrayList<String> partes = gerenciador.divideMessage(texto);
                    gerenciador.sendMultipartTextMessage(telefone, null, partes, null, null);
                    avisarSucesso(ouvinte, contexto.getString(R.string.smsEnviado, telefone));
                } catch (Exception e) {
                    avisarErro(ouvinte, contexto.getString(R.string.smsErro, e.getMessage()));
                }
            }
        });
    }

    // a partir do Android 12 o SmsManager vem do proprio contexto
    private static SmsManager obterGerenciador(Context contexto) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return contexto.getSystemService(SmsManager.class);
        }
        return SmsManager.getDefault();
    }

    // o envio so funciona com a permissao de SMS concedida
    private static boolean temPermissao(Context contexto) {
        return ContextCompat.checkSelfPermission(contexto, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private static void avisarSucesso(final RespostaListener ouvinte, final String mensagem) {
        PRINCIPAL.post(new Runnable() {
            @Override
            public void run() {
                ouvinte.onSucesso(mensagem);
            }
        });
    }

    private static void avisarErro(final RespostaListener ouvinte, final String mensagem) {
        PRINCIPAL.post(new Runnable() {
            @Override
            public void run() {
                ouvinte.onErro(mensagem);
            }
        });
    }
}
