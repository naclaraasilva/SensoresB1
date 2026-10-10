package com.sensoresb1;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.InputType;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;
import java.util.List;

// cadastro e uso dos contatos de emergencia, guardados em SharedPreferences
public class ContatosEmergencia {

    private static final String PREFS = "contatos_emergencia";
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

    // abre o dialogo para cadastrar de 1 a 3 telefones
    public static void mostrarDialogo(final Activity tela) {
        try {
            final SharedPreferences prefs = tela.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

            LinearLayout layout = new LinearLayout(tela);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(40, 16, 40, 0);

            final EditText[] campos = new EditText[MAX_CONTATOS];
            for (int i = 0; i < MAX_CONTATOS; i++) {
                EditText campo = new EditText(tela);
                campo.setInputType(InputType.TYPE_CLASS_PHONE);
                campo.setHint(tela.getString(R.string.contatoHint, i + 1));
                campo.setText(prefs.getString("telefone" + i, ""));
                layout.addView(campo);
                campos[i] = campo;
            }

            new AlertDialog.Builder(tela)
                    .setTitle(R.string.contatosTitulo)
                    .setView(layout)
                    .setPositiveButton(R.string.salvar, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogo, int qual) {
                            SharedPreferences.Editor editor = prefs.edit();
                            for (int i = 0; i < MAX_CONTATOS; i++) {
                                editor.putString("telefone" + i, campos[i].getText().toString().trim());
                            }
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
