package com.sensoresb1;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;
import java.util.List;

// cadastro e uso dos contatos de emergencia, guardados em SharedPreferences
public class ContatosEmergencia {

    private static final String PREFS = "contatos_emergencia";
    // indice do telefone marcado como contato principal
    private static final String CHAVE_PRINCIPAL = "principal";
    public static final int MAX_CONTATOS = 3;

    private ContatosEmergencia() {
        // classe so com metodos estaticos
    }

    // le os telefones salvos, ignorando os vazios
    public static List<String> obterTelefones(Context contexto) {
        List<String> telefones = new ArrayList<>();
        SharedPreferences prefs = contexto.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        for (int i = 0; i < MAX_CONTATOS; i++) {
            String telefone = prefs.getString("telefone" + i, "").trim();
            if (!telefone.isEmpty()) {
                telefones.add(telefone);
            }
        }
        return telefones;
    }

    // telefone marcado como principal; sem marcacao, usa o primeiro preenchido
    public static String obterTelefonePrincipal(Context contexto) {
        SharedPreferences prefs = contexto.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int indice = prefs.getInt(CHAVE_PRINCIPAL, -1);
        if (indice >= 0 && indice < MAX_CONTATOS) {
            String marcado = prefs.getString("telefone" + indice, "").trim();
            if (!marcado.isEmpty()) {
                return marcado;
            }
        }
        List<String> telefones = obterTelefones(contexto);
        if (telefones.isEmpty()) {
            return "";
        }
        return telefones.get(0);
    }

    // abre o dialogo para cadastrar de 1 a 3 telefones e marcar o principal
    public static void mostrarDialogo(final Activity tela) {
        try {
            final SharedPreferences prefs = tela.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

            LinearLayout layout = new LinearLayout(tela);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(40, 16, 40, 0);

            final EditText[] campos = new EditText[MAX_CONTATOS];
            final RadioButton[] marcadores = new RadioButton[MAX_CONTATOS];
            int principal = prefs.getInt(CHAVE_PRINCIPAL, -1);

            for (int i = 0; i < MAX_CONTATOS; i++) {
                // cada linha tem o telefone e o marcador de contato principal
                LinearLayout linha = new LinearLayout(tela);
                linha.setOrientation(LinearLayout.HORIZONTAL);
                linha.setGravity(Gravity.CENTER_VERTICAL);

                EditText campo = new EditText(tela);
                campo.setInputType(InputType.TYPE_CLASS_PHONE);
                campo.setHint(tela.getString(R.string.contatoHint, i + 1));
                campo.setText(prefs.getString("telefone" + i, ""));
                campo.setLayoutParams(new LinearLayout.LayoutParams(0,
                        ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                linha.addView(campo);
                campos[i] = campo;

                RadioButton marcador = new RadioButton(tela);
                marcador.setText(R.string.contatoPrincipal);
                marcador.setChecked(i == principal);
                linha.addView(marcador);
                marcadores[i] = marcador;
                // fora do RadioGroup a marcacao precisa ser controlada na mao
                marcarApenasUm(marcadores, marcador, i);

                layout.addView(linha);
            }

            new AlertDialog.Builder(tela)
                    .setTitle(R.string.contatosTitulo)
                    .setView(layout)
                    .setPositiveButton(R.string.salvar, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogo, int qual) {
                            SharedPreferences.Editor editor = prefs.edit();
                            int escolhido = -1;
                            for (int i = 0; i < MAX_CONTATOS; i++) {
                                String telefone = campos[i].getText().toString().trim();
                                editor.putString("telefone" + i, telefone);
                                // marcador so vale se a linha tiver numero
                                if (marcadores[i].isChecked() && !telefone.isEmpty()) {
                                    escolhido = i;
                                }
                            }
                            editor.putInt(CHAVE_PRINCIPAL, escolhido);
                            editor.apply();
                            Toast.makeText(tela, R.string.contatosSalvos, Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton(R.string.cancelar, null)
                    .show();
        } catch (Exception e) {
            Toast.makeText(tela, R.string.erroGenerico, Toast.LENGTH_SHORT).show();
        }
    }

    // deixa ligado so o marcador tocado, desligando os outros
    private static void marcarApenasUm(final RadioButton[] marcadores, RadioButton marcador,
                                       final int indice) {
        marcador.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (int j = 0; j < marcadores.length; j++) {
                    marcadores[j].setChecked(j == indice);
                }
            }
        });
    }

    // abre um app de SMS/WhatsApp com a mensagem e o link do mapa
    public static void enviarMensagem(Activity tela, String mensagem) {
        try {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, mensagem);

            List<String> telefones = obterTelefones(tela);
            if (!telefones.isEmpty()) {
                StringBuilder numeros = new StringBuilder();
                for (int i = 0; i < telefones.size(); i++) {
                    if (i > 0) {
                        numeros.append(";");
                    }
                    numeros.append(telefones.get(i));
                }
                // alguns apps usam este extra para preencher o destinatario
                intent.putExtra("address", numeros.toString());
            }

            tela.startActivity(Intent.createChooser(intent, tela.getString(R.string.enviarVia)));
        } catch (Exception e) {
            // sem app de mensagem instalado: nao quebra o fluxo
        }
    }
}
