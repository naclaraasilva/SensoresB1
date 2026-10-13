package com.sensoresb1;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

// anel de progresso da contagem regressiva do alerta de queda
public class AnelContagem extends View {

    private final Paint pincelFundo = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pincelProgresso = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF area = new RectF();
    private final float espessura;
    private float fracao = 1f;

    public AnelContagem(Context contexto, AttributeSet atributos) {
        super(contexto, atributos);
        espessura = 8f * getResources().getDisplayMetrics().density;

        pincelFundo.setStyle(Paint.Style.STROKE);
        pincelFundo.setStrokeWidth(espessura);
        pincelFundo.setColor(Color.parseColor("#184848"));

        pincelProgresso.setStyle(Paint.Style.STROKE);
        pincelProgresso.setStrokeWidth(espessura);
        pincelProgresso.setStrokeCap(Paint.Cap.ROUND);
        pincelProgresso.setColor(Color.parseColor("#007878"));
    }

    // fracao de 0 a 1 (1 = anel cheio)
    public void setFracao(float valor) {
        if (valor < 0f) {
            valor = 0f;
        }
        if (valor > 1f) {
            valor = 1f;
        }
        this.fracao = valor;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float metade = espessura / 2f;
        area.set(metade, metade, getWidth() - metade, getHeight() - metade);
        canvas.drawArc(area, 0f, 360f, false, pincelFundo);
        if (fracao > 0f) {
            canvas.drawArc(area, -90f, 360f * fracao, false, pincelProgresso);
        }
    }
}
