package com.sensoresb1;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class MainActivity extends AppCompatActivity implements SensorEventListener {


    private static final float LIMIAR_LUZ = 50;

    private static final long INTERVALO_GPS = 2000;
    private static final int CODIGO_PERMISSAO_LOCALIZACAO = 1;
    private static final int CODIGO_PERMISSAO_SMS = 2;


    private static final long DURACAO_ALERTA = 30000;

    private static final long INTERVALO_TELEMETRIA = 10000;

    private static final String USUARIO_PADRAO = "naclarinha";


    private static final int CHECKIN_INTERVALO_MINUTOS = 15;
    private static final long CHECKIN_RESPOSTA_MS = 30000;

    private static final long CLIMA_INTERVALO = 600000;

    private static final String PREFS_CONFIG = "config";
    private static final String CHAVE_CHECKIN = "checkin_ativo";

    private SensorManager gerenciador;
    private Sensor sensorAcelerometro;
    private Sensor sensorGiroscopio;
    private Sensor sensorProximidade;
    private Sensor sensorLuz;
    private Sensor sensorPressao;
    private Sensor sensorTemperatura;

    private TextView txtAceleracao;
    private TextView txtAceleracaoX;
    private TextView txtAceleracaoY;
    private TextView txtAceleracaoZ;
    private ProgressBar barraX;
    private ProgressBar barraY;
    private ProgressBar barraZ;
    private TextView txtGiroscopioX;
    private TextView txtGiroscopioY;
    private TextView txtGiroscopioZ;
    private TextView txtProximidade;
    private TextView txtLuminosidade;
    private TextView txtPressao;
    private TextView txtTemperatura;
    private TextView txtLatitude;
    private TextView txtLongitude;
    private TextView txtVelocidade;
    private TextView txtBateria;
    private TextView txtDeviceId;
    private TextView txtModelo;
    private TextView txtQueda;
    private TextView txtStatusApi;
    private TextView txtClima;
    private EditText edtNome;
    private View btnPanico;
    private View btnContatos;
    private View btnHistorico;
    private View btnEditarTitular;
    private View navPainel;
    private View navHistorico;
    private SwitchCompat swCheckin;

    // views do novo front da tela inicial
    private TextView txtIniciais;
    private TextView txtRelogio;
    private TextView txtOnline;
    private TextView txtBateriaStatus;
    private TextView txtSimTitulo;
    private TextView txtSimSub;

    // overlay de alerta de queda
    private View painelQueda;
    private TextView txtContagem;
    private TextView txtBateriaAlerta;
    private TextView txtGMaxAlerta;
    private TextView txtLocalAlerta;
    private AnelContagem anelContagem;
    private Button btnEstouBem;
    private Button btnSocorroImediato;
    private CountDownTimer contadorAlerta;
    private Vibrator vibrador;

    // telemetria periodica
    private Handler atualizador;
    private Runnable tarefaTelemetria;
    private Runnable tarefaCheckin;
    private Runnable tarefaClima;

    // relogio ao vivo do cabecalho e contagem do teste simulado
    private Runnable tarefaRelogio;
    private CountDownTimer contadorTeste;

    // check-in de seguranca
    private CountDownTimer contadorCheckin;
    private AlertDialog dialogoCheckin;

    // controle de quando o clima foi buscado pela ultima vez
    private long ultimoClima;

    // views usadas pelo tema, separadas por tipo
    private View telaPrincipal;
    private ScrollView rolagem;
    private View btnSimularQueda;
    private MaterialCardView[] cartoesTema;
    private View[] caixasTema;
    private View[] botaoSecundarios;

    // detector de queda, alimentado pelos sensores
    private DetectorQueda detectorQueda;

    // diz se o aparelho tem cada sensor
    private boolean existeAcelerometro;
    private boolean existeGiroscopio;
    private boolean existeProximidade;
    private boolean existeLuz;
    private boolean existePressao;
    private boolean existeTemperatura;

    // localizacao e identidade do aparelho
    private LocationManager gerenciadorLocalizacao;
    private boolean existeLocalizacao;
    private String idAparelho;
    private String modeloAparelho;

    // ultima leitura guardada de cada sensor
    private final float[] ultimaAceleracao = new float[3];
    private final float[] ultimoGiroscopio = new float[3];
    private float ultimaProximidade;
    private float ultimaLuminosidade;
    private float ultimaPressao;
    private float ultimaTemperatura;

    // diz se ja chegou alguma leitura valida (evita enviar valor zerado)
    private boolean recebeuLocalizacao;
    private boolean recebeuPressao;
    private boolean recebeuTemperatura;

    // ultima leitura guardada de localizacao
    private double ultimaLatitude;
    private double ultimaLongitude;
    private float ultimaVelocidade;

    // forca G do ultimo impacto detectado
    private float ultimoGMax;

    // recebe as atualizacoes do GPS
    private final LocationListener ouvinteLocalizacao = new LocationListener() {
        @Override
        public void onLocationChanged(Location local) {
            ultimaLatitude = local.getLatitude();
            ultimaLongitude = local.getLongitude();
            ultimaVelocidade = local.getSpeed() * 3.6f;
            recebeuLocalizacao = true;

            txtLatitude.setText(getString(R.string.formatoLatitude, ultimaLatitude));
            txtLongitude.setText(getString(R.string.formatoLongitude, ultimaLongitude));
            txtVelocidade.setText(getString(R.string.formatoVelocidade, ultimaVelocidade));

            // aproveita a posicao para buscar o clima, respeitando o intervalo
            buscarClima();
        }

        @Override
        public void onProviderEnabled(String provedor) {
            // vazio, obrigatorio pela interface
        }

        @Override
        public void onProviderDisabled(String provedor) {
            // vazio, obrigatorio pela interface
        }

        @Override
        public void onStatusChanged(String provedor, int estado, Bundle extras) {
            // vazio, obrigatorio pela interface
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // tela inteira atras das barras do sistema; o painel trata as margens
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);

        txtAceleracao = findViewById(R.id.txtAceleracao);
        txtAceleracaoX = findViewById(R.id.txtAceleracaoX);
        txtAceleracaoY = findViewById(R.id.txtAceleracaoY);
        txtAceleracaoZ = findViewById(R.id.txtAceleracaoZ);
        barraX = findViewById(R.id.barraX);
        barraY = findViewById(R.id.barraY);
        barraZ = findViewById(R.id.barraZ);
        txtGiroscopioX = findViewById(R.id.txtGiroscopioX);
        txtGiroscopioY = findViewById(R.id.txtGiroscopioY);
        txtGiroscopioZ = findViewById(R.id.txtGiroscopioZ);
        txtProximidade = findViewById(R.id.txtProximidade);
        txtLuminosidade = findViewById(R.id.txtLuminosidade);
        txtPressao = findViewById(R.id.txtPressao);
        txtTemperatura = findViewById(R.id.txtTemperatura);
        txtLatitude = findViewById(R.id.txtLatAtual);
        txtLongitude = findViewById(R.id.txtLongAtual);
        txtVelocidade = findViewById(R.id.txtVelocidade);
        txtBateria = findViewById(R.id.txtBateriaTopo);
        txtBateriaStatus = findViewById(R.id.txtBateriaStatus);
        txtOnline = findViewById(R.id.txtOnline);
        txtDeviceId = findViewById(R.id.txtIdAparelho);
        txtModelo = findViewById(R.id.txtModelo);
        txtQueda = findViewById(R.id.txtQueda);
        txtStatusApi = findViewById(R.id.txtStatusApi);
        edtNome = findViewById(R.id.edtNomeTitular);
        txtIniciais = findViewById(R.id.txtIniciais);
        txtRelogio = findViewById(R.id.txtRelogio);
        txtSimTitulo = findViewById(R.id.txtSimTitulo);
        txtSimSub = findViewById(R.id.txtSimSub);
        btnPanico = findViewById(R.id.btnPanicoPrincipal);
        txtClima = findViewById(R.id.txtClima);
        btnContatos = findViewById(R.id.btnContatos);
        btnHistorico = findViewById(R.id.btnHistorico);
        swCheckin = findViewById(R.id.swCheckin);
        btnEditarTitular = findViewById(R.id.btnEditarTitularBanner);
        navPainel = findViewById(R.id.navPainel);
        navHistorico = findViewById(R.id.navHistorico);

        telaPrincipal = findViewById(R.id.main);
        rolagem = (ScrollView) telaPrincipal;
        btnSimularQueda = findViewById(R.id.btnSimularTeste);
        painelQueda = findViewById(R.id.painelQueda);
        txtContagem = findViewById(R.id.txtContagem);
        txtBateriaAlerta = findViewById(R.id.txtBateriaAlerta);
        txtGMaxAlerta = findViewById(R.id.txtGMaxAlerta);
        txtLocalAlerta = findViewById(R.id.txtLocalAlerta);
        anelContagem = findViewById(R.id.anelContagem);
        btnEstouBem = findViewById(R.id.btnEstouBem);
        btnSocorroImediato = findViewById(R.id.btnSocorroImediato);

        // vibra enquanto o alerta esta na tela
        vibrador = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // junta as views que mudam de cor junto com o tema
        cartoesTema = new MaterialCardView[]{
                findViewById(R.id.cartaoPerfil),
                findViewById(R.id.cartaoAcelerometro),
                findViewById(R.id.cartaoGiroscopio),
                findViewById(R.id.cartaoProximidade),
                findViewById(R.id.cartaoLuminosidade),
                findViewById(R.id.cartaoPressao),
                findViewById(R.id.cartaoLocalizacao),
                findViewById(R.id.cartaoDispositivo),
                findViewById(R.id.cartaoQueda),
                findViewById(R.id.cartaoClima),
                findViewById(R.id.cartaoCheckin)
        };
        caixasTema = new View[]{
                findViewById(R.id.caixaAcelerometro),
                findViewById(R.id.caixaGiroscopio),
                findViewById(R.id.caixaLocalizacao),
                findViewById(R.id.caixaDispositivo),
                findViewById(R.id.caixaQueda),
                findViewById(R.id.caixaMapa)
        };
        botaoSecundarios = new View[]{
                btnContatos, btnHistorico
        };

        // detector de queda avisa a tela quando confirma uma queda
        detectorQueda = new DetectorQueda(new DetectorQueda.Listener() {
            @Override
            public void onQuedaDetectada(final float gMax) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ultimoGMax = gMax;
                        txtQueda.setText(getString(R.string.formatoQueda, gMax));
                        iniciarAlertaQueda();
                    }
                });
            }
        });

        // botao que testa a deteccao: espera 3 s e dispara a queda simulada
        btnSimularQueda.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                iniciarTesteSimulado();
            }
        });

        // abre o app de mapas com a posicao atual do GPS
        findViewById(R.id.btnMapa).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                abrirMapa();
            }
        });

        // botao que cancela o alerta: nao envia nada
        btnEstouBem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelarAlertaQueda();
            }
        });

        // pede ajuda na hora, sem esperar a contagem terminar
        btnSocorroImediato.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enviarSocorroImediato();
            }
        });

        // botao de panico: envia o alerta na hora, sem contagem
        btnPanico.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enviarPanico();
            }
        });

        // abre o cadastro de contatos de emergencia
        btnContatos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ContatosEmergencia.mostrarDialogo(MainActivity.this);
            }
        });

        // abre a tela de historico de eventos
        btnHistorico.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                abrirHistorico();
            }
        });

        // botao de editar o titular: so coloca o foco no nome
        btnEditarTitular.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                edtNome.requestFocus();
            }
        });

        // as iniciais do banner acompanham o nome digitado
        atualizarIniciais(edtNome.getText().toString());
        edtNome.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence texto, int inicio, int antes, int total) {
                // vazio, obrigatorio pela interface
            }

            @Override
            public void onTextChanged(CharSequence texto, int inicio, int antes, int total) {
                atualizarIniciais(texto.toString());
            }

            @Override
            public void afterTextChanged(Editable texto) {
                // vazio, obrigatorio pela interface
            }
        });

        // navegacao do rodape
        navPainel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rolagem.smoothScrollTo(0, 0);
            }
        });
        navHistorico.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                abrirHistorico();
            }
        });

        // liga/desliga o check-in de seguranca e guarda a escolha
        boolean checkinAtivo = getSharedPreferences(PREFS_CONFIG, Context.MODE_PRIVATE)
                .getBoolean(CHAVE_CHECKIN, false);
        swCheckin.setChecked(checkinAtivo);
        swCheckin.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(android.widget.CompoundButton botao, boolean ligado) {
                getSharedPreferences(PREFS_CONFIG, Context.MODE_PRIVATE)
                        .edit().putBoolean(CHAVE_CHECKIN, ligado).apply();
                if (ligado) {
                    agendarCheckin();
                } else {
                    atualizador.removeCallbacks(tarefaCheckin);
                }
            }
        });

        // telemetria periodica disparada pelo Handler a cada 10 s
        atualizador = new Handler(Looper.getMainLooper());
        tarefaTelemetria = new Runnable() {
            @Override
            public void run() {
                enviarTelemetria();
                atualizador.postDelayed(this, INTERVALO_TELEMETRIA);
            }
        };

        // check-in de seguranca periodico
        tarefaCheckin = new Runnable() {
            @Override
            public void run() {
                perguntarCheckin();
            }
        };

        // consulta de clima periodica
        tarefaClima = new Runnable() {
            @Override
            public void run() {
                buscarClima();
                atualizador.postDelayed(this, CLIMA_INTERVALO);
            }
        };

        // relogio ao vivo do cabecalho, atualizado por segundo
        tarefaRelogio = new Runnable() {
            @Override
            public void run() {
                atualizarRelogio();
                atualizador.postDelayed(this, 1000);
            }
        };

        gerenciador = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorAcelerometro = gerenciador.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorGiroscopio = gerenciador.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        sensorProximidade = gerenciador.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        sensorLuz = gerenciador.getDefaultSensor(Sensor.TYPE_LIGHT);
        sensorPressao = gerenciador.getDefaultSensor(Sensor.TYPE_PRESSURE);
        sensorTemperatura = gerenciador.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);

        existeAcelerometro = sensorAcelerometro != null;
        existeGiroscopio = sensorGiroscopio != null;
        existeProximidade = sensorProximidade != null;
        existeLuz = sensorLuz != null;
        existePressao = sensorPressao != null;
        existeTemperatura = sensorTemperatura != null;

        // avisa na tela quando o sensor nao existe
        if (!existeAcelerometro) {
            txtAceleracao.setText(getString(R.string.sensorInexistente));
        }
        if (!existeGiroscopio) {
            txtGiroscopioX.setText(getString(R.string.sensorInexistente));
        }
        if (!existeProximidade) {
            txtProximidade.setText(getString(R.string.sensorInexistente));
        }
        if (!existeLuz) {
            txtLuminosidade.setText(getString(R.string.sensorInexistente));
        }
        if (!existePressao) {
            txtPressao.setText(getString(R.string.sensorInexistente));
        }
        if (!existeTemperatura) {
            txtTemperatura.setText(getString(R.string.sensorInexistente));
        }

        // localizacao: depende do GPS existir e estar ligado
        gerenciadorLocalizacao = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        existeLocalizacao = gerenciadorLocalizacao != null
                && gerenciadorLocalizacao.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (existeLocalizacao) {
            pedirPermissaoLocalizacao();
        } else {
            txtLatitude.setText(getString(R.string.semGps));
        }

        // permissao do SMS automatico de emergencia
        pedirPermissaoSms();

        // nivel da bateria, exibido no cabecalho e no cartao de status
        BatteryManager gerenciadorBateria = (BatteryManager) getSystemService(Context.BATTERY_SERVICE);
        int nivelBateria = gerenciadorBateria.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
        txtBateria.setText(getString(R.string.formatoBateriaCurta, nivelBateria));
        txtBateriaStatus.setText(getString(R.string.formatoBateriaCurta, nivelBateria));
        txtOnline.setText(getString(R.string.formatoOnline, nivelBateria));
        txtBateriaAlerta.setText(getString(R.string.formatoBateriaCurta, nivelBateria));

        // identidade do aparelho, igual ao appMove
        idAparelho = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        modeloAparelho = Build.MANUFACTURER + " " + Build.MODEL;
        txtDeviceId.setText(getString(R.string.formatoDeviceId, idAparelho));
        txtModelo.setText(getString(R.string.formatoModelo, modeloAparelho));

        // comeca no tema claro ate o sensor de luz pedir o escuro
        aplicarTema(false);

        // ajusta o painel ao tamanho da tela e as barras do sistema
        configurarLayoutResponsivo();
    }

    // deixa o painel certo em qualquer aparelho, respeitando as barras do sistema
    private void configurarLayoutResponsivo() {
        // icones claros nas barras, que ficam sobre o fundo escuro do painel
        WindowInsetsControllerCompat controlador = WindowCompat.getInsetsController(
                getWindow(), getWindow().getDecorView());
        controlador.setAppearanceLightStatusBars(false);
        controlador.setAppearanceLightNavigationBars(false);

        final View rodape = findViewById(R.id.navRodape);
        final View cabecalhoTopo = findViewById(R.id.cabecalhoTopo);
        final ScrollView rolagemAlerta = findViewById(R.id.rolagemAlerta);
        final int alturaRodape = getResources().getDimensionPixelSize(R.dimen.alturaRodape);
        final int alturaTopo = getResources().getDimensionPixelSize(R.dimen.alturaTopo);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.raizPrincipal),
                new OnApplyWindowInsetsListener() {
                    @Override
                    public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                        Insets barras = insets.getInsets(WindowInsetsCompat.Type.systemBars());

                        // cabecalho fixo comeca abaixo da barra de status
                        ViewGroup.LayoutParams pCabecalho = cabecalhoTopo.getLayoutParams();
                        pCabecalho.height = alturaTopo + barras.top;
                        cabecalhoTopo.setLayoutParams(pCabecalho);
                        cabecalhoTopo.setPadding(cabecalhoTopo.getPaddingLeft(), barras.top,
                                cabecalhoTopo.getPaddingRight(), 0);

                        // rodape sobe acima da barra de navegacao
                        rodape.setPadding(rodape.getPaddingLeft(), 0,
                                rodape.getPaddingRight(), barras.bottom);
                        ViewGroup.LayoutParams pRodape = rodape.getLayoutParams();
                        pRodape.height = alturaRodape + barras.bottom;
                        rodape.setLayoutParams(pRodape);

                        // o conteudo comeca logo abaixo da barra de status e termina antes do rodape
                        rolagem.setPadding(rolagem.getPaddingLeft(), barras.top,
                                rolagem.getPaddingRight(), alturaRodape + barras.bottom);

                        // o alerta de queda tambem respeita as barras
                        rolagemAlerta.setPadding(rolagemAlerta.getPaddingLeft(), barras.top,
                                rolagemAlerta.getPaddingRight(), barras.bottom);
                        return insets;
                    }
                });
    }

    // pede a permissao de localizacao se ainda nao tiver
    private void pedirPermissaoLocalizacao() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            iniciarLocalizacao();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION},
                    CODIGO_PERMISSAO_LOCALIZACAO);
        }
    }

    // pede a permissao de SMS, necessaria para o envio automatico
    private void pedirPermissaoSms() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.SEND_SMS}, CODIGO_PERMISSAO_SMS);
    }

    // comeca a receber posicoes do GPS a cada dois segundos
    private void iniciarLocalizacao() {
        try {
            gerenciadorLocalizacao.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    INTERVALO_GPS, 0, ouvinteLocalizacao);
        } catch (SecurityException e) {
            txtLatitude.setText(getString(R.string.semGps));
        }
    }

    @Override
    public void onRequestPermissionsResult(int codigo, String[] permissoes, int[] resultados) {
        super.onRequestPermissionsResult(codigo, permissoes, resultados);
        if (codigo == CODIGO_PERMISSAO_LOCALIZACAO) {
            if (resultados.length > 0 && resultados[0] == PackageManager.PERMISSION_GRANTED) {
                iniciarLocalizacao();
            } else {
                txtLatitude.setText(getString(R.string.permissaoNegada));
            }
        }
        if (codigo == CODIGO_PERMISSAO_SMS) {
            // sem a permissao o SMS automatico nao sai, o resto do alerta continua igual
            if (resultados.length == 0
                    || resultados[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, R.string.smsSemPermissao, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (existeAcelerometro) {
            gerenciador.registerListener(this, sensorAcelerometro, SensorManager.SENSOR_DELAY_GAME);
        }
        if (existeGiroscopio) {
            gerenciador.registerListener(this, sensorGiroscopio, SensorManager.SENSOR_DELAY_GAME);
        }
        if (existeProximidade) {
            gerenciador.registerListener(this, sensorProximidade, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (existeLuz) {
            gerenciador.registerListener(this, sensorLuz, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (existePressao) {
            gerenciador.registerListener(this, sensorPressao, SensorManager.SENSOR_DELAY_NORMAL);
        }
        if (existeTemperatura) {
            gerenciador.registerListener(this, sensorTemperatura, SensorManager.SENSOR_DELAY_NORMAL);
        }

        // volta a acompanhar o GPS quando a tela reaparece
        if (existeLocalizacao
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            iniciarLocalizacao();
        }

        // comeca a telemetria periodica
        atualizador.postDelayed(tarefaTelemetria, INTERVALO_TELEMETRIA);

        // comeca o relogio ao vivo do cabecalho
        atualizador.post(tarefaRelogio);

        // retoma o check-in se estiver ligado e busca o clima
        agendarCheckin();
        atualizador.postDelayed(tarefaClima, 5000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        gerenciador.unregisterListener(this);

        // para a telemetria enquanto a tela nao esta visivel
        atualizador.removeCallbacks(tarefaTelemetria);

        // para o relogio do cabecalho
        atualizador.removeCallbacks(tarefaRelogio);

        // pausa o check-in e o clima
        atualizador.removeCallbacks(tarefaCheckin);
        atualizador.removeCallbacks(tarefaClima);
        if (contadorCheckin != null) {
            contadorCheckin.cancel();
            contadorCheckin = null;
        }
        if (dialogoCheckin != null && dialogoCheckin.isShowing()) {
            dialogoCheckin.dismiss();
        }

        if (gerenciadorLocalizacao != null && existeLocalizacao) {
            try {
                gerenciadorLocalizacao.removeUpdates(ouvinteLocalizacao);
            } catch (SecurityException e) {
                // sem permissao nao ha o que remover
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent evento) {
        int tipo = evento.sensor.getType();
        if (tipo == Sensor.TYPE_ACCELEROMETER) {
            ultimaAceleracao[0] = evento.values[0];
            ultimaAceleracao[1] = evento.values[1];
            ultimaAceleracao[2] = evento.values[2];
            txtAceleracao.setText(getString(R.string.formatoEixos,
                    ultimaAceleracao[0], ultimaAceleracao[1], ultimaAceleracao[2]));
            txtAceleracaoX.setText(getString(R.string.formatoValor, ultimaAceleracao[0]));
            txtAceleracaoY.setText(getString(R.string.formatoValor, ultimaAceleracao[1]));
            txtAceleracaoZ.setText(getString(R.string.formatoValor, ultimaAceleracao[2]));
            barraX.setProgress(proporcaoBarra(ultimaAceleracao[0]));
            barraY.setProgress(proporcaoBarra(ultimaAceleracao[1]));
            barraZ.setProgress(proporcaoBarra(ultimaAceleracao[2]));
            detectorQueda.atualizarAceleracao(ultimaAceleracao[0], ultimaAceleracao[1],
                    ultimaAceleracao[2], System.currentTimeMillis());
        } else if (tipo == Sensor.TYPE_GYROSCOPE) {
            ultimoGiroscopio[0] = evento.values[0];
            ultimoGiroscopio[1] = evento.values[1];
            ultimoGiroscopio[2] = evento.values[2];
            txtGiroscopioX.setText(getString(R.string.formatoValor, ultimoGiroscopio[0]));
            txtGiroscopioY.setText(getString(R.string.formatoValor, ultimoGiroscopio[1]));
            txtGiroscopioZ.setText(getString(R.string.formatoValor, ultimoGiroscopio[2]));
            detectorQueda.atualizarGiroscopio(ultimoGiroscopio[0], ultimoGiroscopio[1],
                    ultimoGiroscopio[2], System.currentTimeMillis());
        } else if (tipo == Sensor.TYPE_PROXIMITY) {
            ultimaProximidade = evento.values[0];
            detectorQueda.atualizarProximidade(ultimaProximidade,
                    sensorProximidade.getMaximumRange());
            // abaixo do alcance maximo significa que algo esta perto
            if (ultimaProximidade < sensorProximidade.getMaximumRange()) {
                txtProximidade.setText(getString(R.string.perto));
            } else {
                txtProximidade.setText(getString(R.string.longe));
            }
        } else if (tipo == Sensor.TYPE_LIGHT) {
            ultimaLuminosidade = evento.values[0];
            txtLuminosidade.setText(getString(R.string.formatoLux, ultimaLuminosidade));
            detectorQueda.atualizarLuminosidade(ultimaLuminosidade);
            // pouca luz deixa a tela escura
            aplicarTema(ultimaLuminosidade < LIMIAR_LUZ);
        } else if (tipo == Sensor.TYPE_PRESSURE) {
            ultimaPressao = evento.values[0];
            recebeuPressao = true;
            txtPressao.setText(getString(R.string.formatoPressao, ultimaPressao));
        } else if (tipo == Sensor.TYPE_AMBIENT_TEMPERATURE) {
            ultimaTemperatura = evento.values[0];
            recebeuTemperatura = true;
            txtTemperatura.setText(getString(R.string.formatoTemperatura, ultimaTemperatura));
        }
    }

    // mostra o overlay com a contagem regressiva e comeca a vibrar
    private void iniciarAlertaQueda() {
        // para a contagem anterior se ainda estiver rodando, senao o timer
        // antigo dispara o envio e grava queda repetida no historico
        if (contadorAlerta != null) {
            contadorAlerta.cancel();
        }
        painelQueda.setVisibility(View.VISIBLE);
        vibrar();

        // preenche os detalhes que aparecem na tela de alerta
        txtGMaxAlerta.setText(getString(R.string.formatoImpacto, ultimoGMax));
        if (recebeuLocalizacao) {
            txtLocalAlerta.setText(getString(R.string.formatoLocalAlerta,
                    ultimaLatitude, ultimaLongitude));
        } else {
            txtLocalAlerta.setText(getString(R.string.localIndisponivel));
        }
        if (anelContagem != null) {
            anelContagem.setFracao(1f);
        }

        contadorAlerta = new CountDownTimer(DURACAO_ALERTA, 1000) {
            @Override
            public void onTick(long millisRestantes) {
                int segundos = (int) (millisRestantes / 1000);
                txtContagem.setText(getString(R.string.contagemFormato, segundos));
                anelContagem.setFracao(millisRestantes / (float) DURACAO_ALERTA);
            }

            @Override
            public void onFinish() {
                anelContagem.setFracao(0f);
                painelQueda.setVisibility(View.GONE);
                pararVibracao();
                enviarAlertaQueda();
            }
        };
        contadorAlerta.start();
    }

    // dispara o alerta na hora, sem esperar a contagem terminar
    private void enviarSocorroImediato() {
        if (contadorAlerta != null) {
            contadorAlerta.cancel();
            contadorAlerta = null;
        }
        pararVibracao();
        painelQueda.setVisibility(View.GONE);
        enviarAlertaQueda();
    }

    // cancela o alerta: para tudo e nao envia nada
    private void cancelarAlertaQueda() {
        if (contadorAlerta != null) {
            contadorAlerta.cancel();
            contadorAlerta = null;
        }
        pararVibracao();
        painelQueda.setVisibility(View.GONE);
        // o usuario disse que esta bem: foi um falso positivo
        HistoricoDbHelper.registrar(this, "falso positivo",
                getString(R.string.motivoFalsoPositivo));
        // alerta encerrado: libera o detector para uma nova queda
        detectorQueda.rearmar();
    }

    // vibra em pulsos enquanto o alerta esta na tela
    private void vibrar() {
        try {
            long[] padrao = {0, 500, 500};
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrador.vibrate(VibrationEffect.createWaveform(padrao, 0));
            } else {
                vibrador.vibrate(padrao, 0);
            }
        } catch (Exception e) {
            // aparelho sem vibrador
        }
    }

    private void pararVibracao() {
        if (vibrador != null) {
            vibrador.cancel();
        }
    }

    // nome digitado pelo usuario, com valor padrao se estiver vazio
    private String lerUsuario() {
        String nome = edtNome.getText().toString().trim();
        if (nome.isEmpty()) {
            return USUARIO_PADRAO;
        }
        return nome;
    }

    // nivel da bateria em porcentagem
    private Double lerBateria() {
        BatteryManager gerenciadorBateria = (BatteryManager) getSystemService(Context.BATTERY_SERVICE);
        return Double.valueOf(
                gerenciadorBateria.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY));
    }

    // devolve valor so quando o sensor existe e ja mandou alguma leitura
    private Double lerLatitude() {
        return (existeLocalizacao && recebeuLocalizacao) ? Double.valueOf(ultimaLatitude) : null;
    }

    private Double lerLongitude() {
        return (existeLocalizacao && recebeuLocalizacao) ? Double.valueOf(ultimaLongitude) : null;
    }

    private Double lerVelocidade() {
        return (existeLocalizacao && recebeuLocalizacao) ? Double.valueOf(ultimaVelocidade) : null;
    }

    private Double lerTemperatura() {
        return (existeTemperatura && recebeuTemperatura) ? Double.valueOf(ultimaTemperatura) : null;
    }

    private Double lerPressao() {
        return (existePressao && recebeuPressao) ? Double.valueOf(ultimaPressao) : null;
    }

    // telemetria periodica com status NORMAL
    private void enviarTelemetria() {
        ApiSocorro.enviar(this, lerUsuario(), ApiSocorro.TIPO_NORMAL, idAparelho, modeloAparelho,
                lerBateria(), lerLatitude(), lerLongitude(), lerVelocidade(),
                lerTemperatura(), lerPressao(), null, new ApiSocorro.RespostaListener() {
                    @Override
                    public void onSucesso(String mensagem, int id) {
                        txtStatusApi.setText(getString(R.string.telemetriaEnviada));
                    }

                    @Override
                    public void onErro(String mensagem) {
                        txtStatusApi.setText(getString(R.string.alertaErro, mensagem));
                    }
                });
    }

    // alerta de panico: envia na hora, sem contagem regressiva
    private void enviarPanico() {
        Toast.makeText(this, getString(R.string.panicoEnviado), Toast.LENGTH_SHORT).show();
        txtStatusApi.setText(getString(R.string.panicoEnviado));
        HistoricoDbHelper.registrar(this, "panico", getString(R.string.motivoPanico));
        enviarAlertaEmergencia(getString(R.string.motivoPanico), null);
    }

    // monta e envia o alerta de queda para a API
    private void enviarAlertaQueda() {
        // registra no historico no mesmo momento em que a queda vai para a API
        HistoricoDbHelper.registrar(this, "queda", getString(R.string.motivoQueda));
        // SMS automatico para o contato principal marcado no cadastro
        enviarSmsSocorro(getString(R.string.motivoQueda));
        enviarAlertaEmergencia(getString(R.string.motivoQueda), Double.valueOf(ultimoGMax));
        // alerta encerrado: libera o detector para uma nova queda
        detectorQueda.rearmar();
    }

    // alerta disparado quando o check-in fica sem resposta
    private void enviarAlertaCheckin() {
        HistoricoDbHelper.registrar(this, "checkin", getString(R.string.motivoCheckin));
        enviarAlertaEmergencia(getString(R.string.motivoCheckin), null);
    }

    // envio comum de emergencia: API + mensagem para os contatos
    private void enviarAlertaEmergencia(String motivo, Double nivelQueda) {
        ApiSocorro.enviar(this, lerUsuario(), ApiSocorro.TIPO_QUEDA, idAparelho, modeloAparelho,
                lerBateria(), lerLatitude(), lerLongitude(), lerVelocidade(),
                lerTemperatura(), lerPressao(), nivelQueda, new ApiSocorro.RespostaListener() {
                    @Override
                    public void onSucesso(String mensagem, int id) {
                        txtStatusApi.setText(getString(R.string.alertaEnviado));
                    }

                    @Override
                    public void onErro(String mensagem) {
                        txtStatusApi.setText(getString(R.string.alertaErro, mensagem));
                    }
                });
        abrirMensagem(motivo);
    }

    // abre um app de SMS/WhatsApp com o texto de ajuda e o link do mapa
    private void abrirMensagem(String motivo) {
        try {
            ContatosEmergencia.enviarMensagem(this, montarMensagemAlerta(motivo));
        } catch (Exception e) {
            // sem contatos ou sem app de mensagem: segue sem quebrar
        }
    }

    // texto de socorro enviado ao contato de emergencia
    private String montarMensagemAlerta(String motivo) {
        String link = "https://maps.google.com/?q=" + ultimaLatitude + "," + ultimaLongitude;
        return getString(R.string.mensagemAlerta, lerUsuario(), motivo, link);
    }

    // SMS automatico para o contato principal, alem da mensagem manual
    private void enviarSmsSocorro(String motivo) {
        SmsEmergencia.enviar(this, montarMensagemAlerta(motivo),
                new SmsEmergencia.RespostaListener() {
                    @Override
                    public void onSucesso(String mensagem) {
                        Toast.makeText(MainActivity.this, mensagem, Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onErro(String mensagem) {
                        Toast.makeText(MainActivity.this, mensagem, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // agenda a proxima pergunta de check-in, se estiver ligado
    private void agendarCheckin() {
        atualizador.removeCallbacks(tarefaCheckin);
        if (swCheckin != null && swCheckin.isChecked()) {
            atualizador.postDelayed(tarefaCheckin, CHECKIN_INTERVALO_MINUTOS * 60000L);
        }
    }

    // pergunta se esta tudo bem e envia alerta se ninguem responder
    private void perguntarCheckin() {
        try {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            if (contadorCheckin != null) {
                contadorCheckin.cancel();
            }
            contadorCheckin = new CountDownTimer(CHECKIN_RESPOSTA_MS, 1000) {
                @Override
                public void onTick(long millisRestantes) {
                    // sem atualizacao visual: so conta o tempo
                }

                @Override
                public void onFinish() {
                    if (dialogoCheckin != null && dialogoCheckin.isShowing()) {
                        dialogoCheckin.dismiss();
                    }
                    enviarAlertaCheckin();
                    agendarCheckin();
                }
            };
            dialogoCheckin = new AlertDialog.Builder(this)
                    .setTitle(R.string.checkinTitulo)
                    .setMessage(R.string.checkinMensagem)
                    .setCancelable(false)
                    .setPositiveButton(R.string.estouBem, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogo, int qual) {
                            if (contadorCheckin != null) {
                                contadorCheckin.cancel();
                            }
                            agendarCheckin();
                        }
                    })
                    .show();
            contadorCheckin.start();
        } catch (Exception e) {
            // se o dialogo falhar, apenas reagenda
            agendarCheckin();
        }
    }

    // busca o clima da posicao atual, respeitando o intervalo minimo
    private void buscarClima() {
        if (!recebeuLocalizacao) {
            return;
        }
        long agora = System.currentTimeMillis();
        if (agora - ultimoClima < CLIMA_INTERVALO) {
            return;
        }
        ultimoClima = agora;
        Clima.buscar(this, ultimaLatitude, ultimaLongitude, new Clima.Listener() {
            @Override
            public void onClima(String mensagem) {
                txtClima.setText(mensagem);
            }

            @Override
            public void onErro(String mensagem) {
                txtClima.setText(mensagem);
            }
        });
    }

    // pinta fundo, cartoes, caixas e botoes conforme o tema
    private void aplicarTema(boolean escuro) {
        // o novo front e fixo no tema escuro
        escuro = true;
        int corFundo = ContextCompat.getColor(this,
                escuro ? R.color.surfaceDim : R.color.deepForest);
        telaPrincipal.setBackgroundColor(corFundo);

        int corCartao = ContextCompat.getColor(this,
                escuro ? R.color.deepForest : R.color.richTeal);
        for (MaterialCardView cartao : cartoesTema) {
            cartao.setCardBackgroundColor(corCartao);
        }

        int fundoCaixa = escuro ? R.drawable.fundo_caixa_escura : R.drawable.fundo_caixa;
        for (View caixa : caixasTema) {
            caixa.setBackgroundResource(fundoCaixa);
        }

        int fundoBotao = escuro ? R.drawable.fundo_botao_secundario_escura
                : R.drawable.fundo_botao_secundario;
        for (View botao : botaoSecundarios) {
            botao.setBackgroundResource(fundoBotao);
        }
    }

    // converte a leitura de um eixo em porcentagem da barra (0 a 100)
    private int proporcaoBarra(float valor) {
        float porcentagem = Math.abs(valor) / 20f * 100f;
        if (porcentagem > 100f) {
            porcentagem = 100f;
        }
        return Math.round(porcentagem);
    }

    // mostra as iniciais do titular no circulo do banner (ate duas palavras)
    private void atualizarIniciais(String nome) {
        StringBuilder iniciais = new StringBuilder();
        for (String palavra : nome.trim().split("\\s+")) {
            if (palavra.isEmpty() || iniciais.length() >= 2) {
                continue;
            }
            iniciais.append(palavra.charAt(0));
        }
        if (iniciais.length() == 0) {
            txtIniciais.setText(R.string.iniciaisPadrao);
        } else {
            txtIniciais.setText(iniciais.toString());
        }
    }

    // mostra a hora atual no cabecalho no formato HH:mm:ss
    private void atualizarRelogio() {
        SimpleDateFormat formato = new SimpleDateFormat("HH:mm:ss", new Locale("pt", "BR"));
        txtRelogio.setText(getString(R.string.formatoRelogio, formato.format(new Date())));
    }

    // contagem de 3 s no proprio botao antes de disparar o teste de queda
    private void iniciarTesteSimulado() {
        if (contadorTeste != null) {
            return;
        }
        txtSimTitulo.setText(getString(R.string.simulandoEm, 3));
        txtSimSub.setText(R.string.simulandoSub);
        btnSimularQueda.setEnabled(false);
        contadorTeste = new CountDownTimer(3000, 1000) {
            @Override
            public void onTick(long millisRestantes) {
                int segundos = (int) (millisRestantes / 1000);
                txtSimTitulo.setText(getString(R.string.simulandoEm, segundos));
            }

            @Override
            public void onFinish() {
                contadorTeste = null;
                btnSimularQueda.setEnabled(true);
                txtSimTitulo.setText(R.string.btnSimularTitulo);
                txtSimSub.setText(R.string.btnSimularSub);
                detectorQueda.simularQueda();
            }
        };
        contadorTeste.start();
    }

    // abre o app de mapas na posicao atual do GPS
    private void abrirMapa() {
        try {
            String link = "geo:" + ultimaLatitude + "," + ultimaLongitude;
            startActivity(new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(link)));
        } catch (Exception e) {
            Toast.makeText(this, R.string.erroGenerico, Toast.LENGTH_SHORT).show();
        }
    }

    // abre a tela de historico de eventos
    private void abrirHistorico() {
        try {
            startActivity(new Intent(MainActivity.this, HistoricoActivity.class));
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, R.string.erroGenerico, Toast.LENGTH_SHORT).show();
        }
    }



    @Override
    public void onAccuracyChanged(Sensor sensor, int precisao) {
        // vazio, obrigatorio pela interface
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (contadorAlerta != null) {
            contadorAlerta.cancel();
        }
        if (contadorTeste != null) {
            contadorTeste.cancel();
        }
        if (contadorCheckin != null) {
            contadorCheckin.cancel();
        }
        if (dialogoCheckin != null && dialogoCheckin.isShowing()) {
            dialogoCheckin.dismiss();
        }
        pararVibracao();
    }
}
