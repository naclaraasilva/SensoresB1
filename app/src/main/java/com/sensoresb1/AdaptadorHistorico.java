package com.sensoresb1;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

// mostra os eventos do historico em cartoes, com icone e cor por tipo
public class AdaptadorHistorico extends BaseAdapter {

    // guarda as views de cada linha, para nao inflar o layout de novo
    private static final class Porta {
        View quadro;
        ImageView icone;
        TextView titulo;
        TextView descricao;
        TextView data;
    }

    private final Activity contexto;
    private final List<HistoricoDbHelper.Evento> todos;
    private List<HistoricoDbHelper.Evento> visiveis;
    private final SimpleDateFormat formatoData =
            new SimpleDateFormat("dd MMM, HH:mm", new Locale("pt", "BR"));

    public AdaptadorHistorico(Activity contexto, List<HistoricoDbHelper.Evento> eventos) {
        this.contexto = contexto;
        this.todos = eventos;
        this.visiveis = eventos;
    }

    // deixa so os eventos do tipo escolhido; tipo nulo mostra todos
    public void filtrar(String tipo) {
        if (tipo == null) {
            visiveis = todos;
        } else {
            List<HistoricoDbHelper.Evento> escolhidos = new ArrayList<>();
            for (HistoricoDbHelper.Evento evento : todos) {
                if (tipo.equals(evento.tipo)) {
                    escolhidos.add(evento);
                }
            }
            visiveis = escolhidos;
        }
        // avisa a lista para redesenhar com o filtro novo
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return visiveis.size();
    }

    @Override
    public Object getItem(int posicao) {
        return visiveis.get(posicao);
    }

    @Override
    public long getItemId(int posicao) {
        return posicao;
    }

    @Override
    public View getView(int posicao, View convertView, ViewGroup pai) {
        Porta porta;
        if (convertView == null) {
            convertView = LayoutInflater.from(contexto)
                    .inflate(R.layout.item_historico, pai, false);
            porta = new Porta();
            porta.quadro = convertView.findViewById(R.id.quadroItem);
            porta.icone = convertView.findViewById(R.id.iconeItem);
            porta.titulo = convertView.findViewById(R.id.txtItemTitulo);
            porta.descricao = convertView.findViewById(R.id.txtItemDescricao);
            porta.data = convertView.findViewById(R.id.txtItemData);
            convertView.setTag(porta);
        } else {
            porta = (Porta) convertView.getTag();
        }

        HistoricoDbHelper.Evento evento = visiveis.get(posicao);
        porta.titulo.setText(tituloDe(evento.tipo));
        porta.descricao.setText(evento.descricao);
        porta.icone.setImageResource(iconeDe(evento.tipo));
        porta.quadro.setBackgroundTintList(
                ColorStateList.valueOf(ContextCompat.getColor(contexto, corDe(evento.tipo))));
        porta.data.setText(formatoData.format(new Date(evento.quando)));
        return convertView;
    }

    // titulo do evento conforme o tipo gravado
    private int tituloDe(String tipo) {
        if ("queda".equals(tipo)) {
            return R.string.tipoQueda;
        }
        if ("falso positivo".equals(tipo)) {
            return R.string.tipoFalso;
        }
        if ("panico".equals(tipo)) {
            return R.string.tipoPanico;
        }
        if ("checkin".equals(tipo)) {
            return R.string.tipoCheckin;
        }
        return R.string.tipoOutro;
    }

    // icone do evento conforme o tipo gravado
    private int iconeDe(String tipo) {
        if ("queda".equals(tipo)) {
            return R.drawable.ic_aviso;
        }
        if ("falso positivo".equals(tipo)) {
            return R.drawable.ic_check;
        }
        if ("panico".equals(tipo)) {
            return R.drawable.ic_emergencia;
        }
        if ("checkin".equals(tipo)) {
            return R.drawable.ic_pessoa;
        }
        return R.drawable.ic_history;
    }

    // cor do quadro do icone conforme o tipo gravado
    private int corDe(String tipo) {
        if ("queda".equals(tipo)) {
            return R.color.vermelhoStatus;
        }
        if ("falso positivo".equals(tipo)) {
            return R.color.limeAccent;
        }
        if ("panico".equals(tipo)) {
            return R.color.vermelhoPanico;
        }
        if ("checkin".equals(tipo)) {
            return R.color.primaryClaro;
        }
        return R.color.primaryTeal;
    }
}
