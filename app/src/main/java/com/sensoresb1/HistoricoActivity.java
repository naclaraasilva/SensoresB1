package com.sensoresb1;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

// segunda tela: lista o historico local de eventos, com filtro por tipo
public class HistoricoActivity extends AppCompatActivity {

    private TextView txtVazio;
    private AdaptadorHistorico adaptador;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historico);

        ListView lista = findViewById(R.id.listaEventos);
        txtVazio = findViewById(R.id.txtHistoricoVazio);

        // botao de voltar para o painel
        findViewById(R.id.btnVoltar).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // le os eventos do banco; falha no banco vira lista vazia
        List<HistoricoDbHelper.Evento> eventos;
        try {
            eventos = new HistoricoDbHelper(this).listarEventos();
        } catch (Exception e) {
            eventos = new ArrayList<>();
        }
        adaptador = new AdaptadorHistorico(this, eventos);
        lista.setAdapter(adaptador);

        // filtros por tipo de evento
        configurarFiltro(R.id.filtroTodas, null);
        configurarFiltro(R.id.filtroQuedas, "queda");
        configurarFiltro(R.id.filtroFalsos, "falso positivo");
        configurarFiltro(R.id.filtroPanico, "panico");
        configurarFiltro(R.id.filtroCheckin, "checkin");

        // comeca mostrando todos os eventos
        aplicarFiltro(findViewById(R.id.filtroTodas), null);
    }

    // cada pill troca o filtro da lista quando e tocado
    private void configurarFiltro(int idBotao, final String tipo) {
        final TextView botao = findViewById(idBotao);
        botao.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                aplicarFiltro(botao, tipo);
            }
        });
    }

    // pinta o pill escolhido, filtra a lista e mostra o aviso de vazio
    private void aplicarFiltro(TextView escolhido, String tipo) {
        int[] ids = {R.id.filtroTodas, R.id.filtroQuedas, R.id.filtroFalsos,
                R.id.filtroPanico, R.id.filtroCheckin};
        for (int id : ids) {
            TextView pill = findViewById(id);
            boolean ativo = pill == escolhido;
            pill.setBackgroundResource(ativo ? R.drawable.fundo_filtro_ativo
                    : R.drawable.fundo_pill_escura);
            pill.setTextColor(ContextCompat.getColor(this,
                    ativo ? R.color.textoLime : R.color.sandCream70));
            pill.setTypeface(null, ativo ? Typeface.BOLD : Typeface.NORMAL);
        }

        adaptador.filtrar(tipo);
        if (adaptador.getCount() == 0) {
            txtVazio.setText(tipo == null ? R.string.historicoVazio
                    : R.string.historicoVazioFiltro);
            txtVazio.setVisibility(View.VISIBLE);
        } else {
            txtVazio.setVisibility(View.GONE);
        }
    }
}
