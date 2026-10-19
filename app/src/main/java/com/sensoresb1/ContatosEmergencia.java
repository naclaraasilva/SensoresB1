package com.sensoresb1;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

// cadastro e uso do contato de emergencia, guardado em SharedPreferences
public class ContatosEmergencia {

    private static final String PREFS = "contatos_emergencia";
    private static final String CHAVE_TELEFONE = "telefone0";
    private static final String CHAVE_NOME = "nome0";

    private ContatosEmergencia() {
        // classe so com metodos estaticos
    }

    // le o telefone salvo, ignorando o vazio
    public static List<String> obterTelefones(Context contexto) {
        List<String> telefones = new ArrayList<>();
        String telefone = contexto.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(CHAVE_TELEFONE, "").trim();
        if (!telefone.isEmpty()) {
            telefones.add(telefone);
        }
        return telefones;
    }

    // nome do contato de emergencia; sem cadastro devolve vazio
    public static String obterNome(Context contexto) {
        return contexto.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(CHAVE_NOME, "").trim();
    }

    // telefone unico cadastrado; sem cadastro devolve vazio
    public static String obterTelefonePrincipal(Context contexto) {
        return contexto.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(CHAVE_TELEFONE, "").trim();
    }

    // abre o dialogo para cadastrar o contato de emergencia
    public static void mostrarDialogo(final Activity tela) {
        try {
            final SharedPreferences prefs = tela.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

            View conteudo = LayoutInflater.from(tela).inflate(
                    R.layout.dialog_contato_emergencia, null);
            final EditText campoNome = conteudo.findViewById(R.id.edtNome);
            final EditText campo = conteudo.findViewById(R.id.edtTelefone);
            campoNome.setText(prefs.getString(CHAVE_NOME, ""));
            campo.setText(prefs.getString(CHAVE_TELEFONE, ""));

            AlertDialog dialogo = new AlertDialog.Builder(tela, R.style.TemaDialogoEmergencia)
                    .setView(conteudo)
                    .setPositiveButton(R.string.salvar, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface d, int qual) {
                            String nome = campoNome.getText().toString().trim();
                            String telefone = campo.getText().toString().trim();
                            prefs.edit()
                                    .putString(CHAVE_NOME, nome)
                                    .putString(CHAVE_TELEFONE, telefone)
                                    .apply();
                            Toast.makeText(tela, R.string.contatosSalvos,
                                    Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton(R.string.cancelar, null)
                    .create();
            dialogo.show();

            // botoes no mesmo tom de destaque do restante do app
            int destaque = ContextCompat.getColor(tela, R.color.limeAccent);
            dialogo.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(destaque);
            dialogo.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(destaque);
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

            String telefone = obterTelefonePrincipal(tela);
            if (!telefone.isEmpty()) {
                // alguns apps usam este extra para preencher o destinatario
                intent.putExtra("address", telefone);
            }

            tela.startActivity(Intent.createChooser(intent, tela.getString(R.string.enviarVia)));
        } catch (Exception e) {
            // sem app de mensagem instalado: nao quebra o fluxo
        }
    }
}
