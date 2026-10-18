package com.sensoresb1;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

// guarda o historico local de eventos (queda, panico, cancelado)
public class HistoricoDbHelper extends SQLiteOpenHelper {

    private static final String BANCO = "historico.db";
    private static final int VERSAO = 1;
    private static final String TABELA = "eventos";

    public HistoricoDbHelper(Context contexto) {
        super(contexto, BANCO, null, VERSAO);
    }

    @Override
    public void onCreate(SQLiteDatabase banco) {
        banco.execSQL("CREATE TABLE " + TABELA + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "tipo TEXT, "
                + "descricao TEXT, "
                + "quando INTEGER)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase banco, int versaoAntiga, int versaoNova) {
        banco.execSQL("DROP TABLE IF EXISTS " + TABELA);
        onCreate(banco);
    }

    // grava um evento novo
    public void inserir(String tipo, String descricao) {
        ContentValues valores = new ContentValues();
        valores.put("tipo", tipo);
        valores.put("descricao", descricao);
        valores.put("quando", System.currentTimeMillis());
        getWritableDatabase().insert(TABELA, null, valores);
    }

    // um evento do historico, com os campos separados para a tela montar
    public static class Evento {
        public String tipo;
        public String descricao;
        public long quando;
    }

    // devolve os eventos do mais novo para o mais antigo
    public List<Evento> listarEventos() {
        List<Evento> eventos = new ArrayList<>();
        Cursor cursor = getReadableDatabase().query(TABELA,
                new String[]{"tipo", "descricao", "quando"},
                null, null, null, null, "quando DESC");
        try {
            while (cursor.moveToNext()) {
                Evento evento = new Evento();
                evento.tipo = cursor.getString(0);
                evento.descricao = cursor.getString(1);
                evento.quando = cursor.getLong(2);
                eventos.add(evento);
            }
        } finally {
            cursor.close();
        }
        return eventos;
    }

    // atalho seguro: falha no banco nao pode derrubar o app
    public static void registrar(Context contexto, String tipo, String descricao) {
        try {
            new HistoricoDbHelper(contexto).inserir(tipo, descricao);
        } catch (Exception e) {
            // historico e opcional
        }
    }
}
