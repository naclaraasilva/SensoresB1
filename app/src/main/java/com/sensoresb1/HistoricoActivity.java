package com.sensoresb1;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

// segunda tela: lista o historico local de eventos
public class HistoricoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historico);

        ListView lista = findViewById(R.id.listaEventos);
        TextView txtVazio = findViewById(R.id.txtHistoricoVazio);

        try {
            List<String> itens = new HistoricoDbHelper(this).listar();
            if (itens.isEmpty()) {
                txtVazio.setText(R.string.historicoVazio);
            } else {
                lista.setAdapter(new ArrayAdapter<>(this,
                        android.R.layout.simple_list_item_1, itens));
            }
        } catch (Exception e) {
            // qualquer falha no banco mostra lista vazia
            txtVazio.setText(R.string.historicoVazio);
        }
    }
}
