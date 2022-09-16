package com.cursoandroid.uber.activity;

import static java.lang.Double.parseDouble;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;


import com.cursoandroid.uber.R;
import com.cursoandroid.uber.helper.ConfiguracaoFirebase;
import com.cursoandroid.uber.helper.Local;
import com.cursoandroid.uber.helper.UsuarioFirebase;
import com.cursoandroid.uber.model.Destino;
import com.cursoandroid.uber.model.Requisicao;
import com.cursoandroid.uber.model.Usuario;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;


public class PassageiroActivity extends AppCompatActivity implements OnMapReadyCallback {

    /*
    Destino: Av. Paulista, 2537
    Passageiro: Av. Paulista, 900
        lat/lon -23.5657, -46.6512
    Motorista:
    inicial - R. dos Belgas, 74
        lat/lon -23.5628, -46.6474
    intermediário - Alameda Joaquim Eugênio de Lima, 257
        lat/lon -23.5645, -46.6492
    próximo - Av. Paulista, 900
        lat/lon -23.5660, -46.6509
     */
    private EditText editDestino;
    private LinearLayout linearLayoutDestino;
    private Button buttonChamarUber;
    private GoogleMap mMap;
    private FirebaseAuth autenticacao;
    private DatabaseReference firebaseRef;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private LatLng localPassageiro;
    private LatLng localMotorista;
    private Requisicao requisicao;
    private Usuario motorista;
    private Usuario passageiro;
    private Marker marcadorMotorista;
    private Marker marcadorPassageiro;
    private Marker marcadorDestino;
    private String statusRequisicao;
    private Destino destino;

    private boolean uberCancelado = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passageiro);

        inicializarComponentes();

        //Adiciona listener para o status da requisição
        verificaStatusRequisicao();

    }

    private void verificaStatusRequisicao(){

        Usuario usuarioLogado = UsuarioFirebase.getDadosUsuarioLogado();

        DatabaseReference requisicoes = firebaseRef.child("requisicoes");
        Query requisicaoPesquisa = requisicoes.orderByChild("passageiro/id")
                .equalTo(usuarioLogado.getId());

        requisicaoPesquisa.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                List<Requisicao> lista = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()){

                    lista.add(ds.getValue(Requisicao.class));
                }

                Collections.reverse(lista);

                if (lista != null && lista.size() > 0){
                    requisicao = lista.get(0);

                    if(requisicao != null){
                        if (!requisicao.getStatus().equals(Requisicao.STATUS_ENCERRADA)){

                            passageiro = requisicao.getPassageiro();
                            localPassageiro = new LatLng(
                                    parseDouble(passageiro.getLatitude()),
                                    parseDouble(passageiro.getLongitude())
                            );

                            statusRequisicao = requisicao.getStatus();
                            destino = requisicao.getDestino();

                            if (requisicao.getMotorista() != null){
                                motorista = requisicao.getMotorista();
                                localMotorista = new LatLng(
                                        Double.parseDouble(motorista.getLatitude()),
                                        Double.parseDouble(motorista.getLongitude())
                                );
                            }

                            alteraInterfaceStatusRequisicao(statusRequisicao);
                        }

                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void alteraInterfaceStatusRequisicao(String status){

        if (status != null && !status.isEmpty()){
            uberCancelado = false;

            switch (status){
                case Requisicao.STATUS_AGUARDANDO:
                    requisicaoAguardando();
                    break;

                case Requisicao.STATUS_A_CAMINHO:
                    requisicaoACaminho();
                    break;

                case Requisicao.STATUS_VIAGEM:
                    requisicaoViagem();
                    break;

                case Requisicao.STATUS_FINALIZADA:
                    requisicaoFinalizada();
                    break;

                case Requisicao.STATUS_CANCELADA:
                    requisicaoCancelada();
                    break;
            }
        }else {
            //Adiciona marcador passageiro
            adicionaMarcadorPassageiro(localPassageiro, "Seu local");
            centralizarMarcador(localPassageiro);
        }
    }

    public void requisicaoAguardando(){
        linearLayoutDestino.setVisibility(View.GONE);
        buttonChamarUber.setText("Cancelar Uber");
        uberCancelado = true;

        //Adiciona marcador passageiro
        adicionaMarcadorPassageiro(localPassageiro, passageiro.getNome());
        centralizarMarcador(localPassageiro);
    }

    public void requisicaoACaminho(){
        linearLayoutDestino.setVisibility(View.GONE);
        buttonChamarUber.setText("Motorista a caminho");
        buttonChamarUber.setEnabled(false);

        //Adiciona marcador passageiro
        adicionaMarcadorPassageiro(localPassageiro, passageiro.getNome());

        //Adiciona marcador motorista
        adicionaMarcadorMotorista(localMotorista, motorista.getNome());

        //Centraliza passageiro/motorista
        centralizarDoisMarcadores(marcadorMotorista, marcadorPassageiro);

    }

    public void requisicaoViagem(){
        linearLayoutDestino.setVisibility(View.GONE);
        buttonChamarUber.setText("A caminho do destino");
        buttonChamarUber.setEnabled(false);

        //Adiciona marcador motorista
        adicionaMarcadorMotorista(localMotorista, motorista.getNome());

        //Adiciona marcador do destino
        LatLng localDestino = new LatLng(
                Double.parseDouble(destino.getLatitude()),
                Double.parseDouble(destino.getLongitude())
        );
        adicionaMarcadorDestino(localDestino, "Destino");

        //Centraliza motorista/destino
        centralizarDoisMarcadores(marcadorMotorista, marcadorDestino);

    }
    public void requisicaoFinalizada(){
        linearLayoutDestino.setVisibility(View.GONE);
        buttonChamarUber.setEnabled(false);

        if (marcadorMotorista != null)
            marcadorMotorista.remove();

        //Adiciona marcador do destino
        LatLng localDestino = new LatLng(
                Double.parseDouble(destino.getLatitude()),
                Double.parseDouble(destino.getLongitude())
        );
        adicionaMarcadorDestino(localDestino, "Destino");
        centralizarMarcador(localDestino);

        //Calcular distância
        float distancia = Local.calcularDistancia(localPassageiro, localDestino);
        float valor = distancia * 10;

        DecimalFormat decimal = new DecimalFormat("0.00");
        String resultado = decimal.format(valor);

        buttonChamarUber.setText("Corrida finalizada - R$" + resultado);

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Total viagem")
                .setMessage("Sua viagem ficou R$" + resultado)
                .setCancelable(false)
                .setNegativeButton("Encerrar viagem", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        requisicao.setStatus(Requisicao.STATUS_ENCERRADA);
                        requisicao.atualizarStatus();

                        finish();
                        startActivity(new Intent(getIntent()));
                    }
                });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void requisicaoCancelada(){
        linearLayoutDestino.setVisibility(View.VISIBLE);
        buttonChamarUber.setText("Chamar Uber");
        uberCancelado = false;
    }

    private void adicionaMarcadorDestino(LatLng localizacao, String titulo){

        if (marcadorPassageiro != null)
            marcadorPassageiro.remove();

        if (marcadorDestino != null)
            marcadorDestino.remove();

        marcadorDestino = mMap.addMarker(new MarkerOptions()
                .position(localizacao)
                .title(titulo)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.destino)));
    }

    private void centralizarMarcador(LatLng local){
        mMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(local, 18)
        );
    }

    private void adicionaMarcadorPassageiro(LatLng localizacao, String titulo){
        if (marcadorPassageiro != null)
            marcadorPassageiro.remove();

        marcadorPassageiro = mMap.addMarker(new MarkerOptions()
                .position(localizacao)
                .title(titulo)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.usuario)));
    }

    private void adicionaMarcadorMotorista(LatLng localizacao, String titulo){
        if (marcadorMotorista != null)
            marcadorMotorista.remove();

        marcadorMotorista = mMap.addMarker(new MarkerOptions()
                .position(localizacao)
                .title(titulo)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.carro)));
    }

    private void centralizarDoisMarcadores(Marker marcador1, Marker marcador2){

        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        builder.include(marcador1.getPosition());
        builder.include(marcador2.getPosition());

        LatLngBounds bounds = builder.build();

        int largura = getResources().getDisplayMetrics().widthPixels;
        int altura = getResources().getDisplayMetrics().heightPixels;
        int espacoInterno = (int) (largura * 0.20);

        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, largura, altura, espacoInterno));
    }

    public void chamarUber(View view){

        //false - uber não pode ser cancelado ainda
        //true - uber pode ser cancelado
        if (uberCancelado){//uber pode ser cancelado

            requisicao.setStatus(Requisicao.STATUS_CANCELADA);
            requisicao.atualizarStatus();

        }else {

            String enderecoDestino = editDestino.getText().toString();
            if (!enderecoDestino.equals("")){

                Address addressDestino = recuperarEndereco(enderecoDestino);

                if (addressDestino != null){

                    Destino destino = new Destino();
                    destino.setCidade(addressDestino.getSubAdminArea());
                    destino.setCep(addressDestino.getPostalCode());
                    destino.setBairro(addressDestino.getSubLocality());
                    destino.setRua(addressDestino.getThoroughfare());
                    destino.setNumero(addressDestino.getFeatureName());
                    destino.setLatitude(String.valueOf(addressDestino.getLatitude()));
                    destino.setLongitude(String.valueOf(addressDestino.getLongitude()));

                    StringBuilder mensagem = new StringBuilder();
                    mensagem.append("Cidade: " + destino.getCidade());
                    mensagem.append("\nRua: " + destino.getRua());
                    mensagem.append("\nBairro: " + destino.getBairro());
                    mensagem.append("\nNúmero: " + destino.getNumero());
                    mensagem.append("\nCEP: " + destino.getCep());

                    AlertDialog.Builder builder = new AlertDialog.Builder(PassageiroActivity.this)
                            .setTitle("Confirme o endereço")
                            .setMessage(mensagem)
                            .setPositiveButton("Confirmar", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    //salvar requisição
                                    salvarRequisicao(destino);

                                }
                            }).setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {

                                }
                            });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }

            }else {
                Toast.makeText(PassageiroActivity.this,
                        "Informe o endereço de destino!",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void salvarRequisicao(Destino destino){

        Requisicao requisicao = new Requisicao();
        requisicao.setDestino(destino);

        Usuario usuarioPassageiro = UsuarioFirebase.getDadosUsuarioLogado();
        usuarioPassageiro.setLatitude(String.valueOf(localPassageiro.latitude));
        usuarioPassageiro.setLongitude(String.valueOf(localPassageiro.longitude));

        requisicao.setPassageiro(usuarioPassageiro);
        requisicao.setStatus(Requisicao.STATUS_AGUARDANDO);
        requisicao.salvar();

        linearLayoutDestino.setVisibility(View.GONE);
        buttonChamarUber.setText("Cancelar Uber");
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        recuperarLocalizacaoUsuario();
    }


    private Address recuperarEndereco(String endereco){

        //StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        //StrictMode.setThreadPolicy(policy);

        Geocoder geocoder = new Geocoder(PassageiroActivity.this, Locale.getDefault());
        try {
            List<Address> listaEndereco = geocoder.getFromLocationName(endereco, 1);
            if (listaEndereco != null && listaEndereco.size() > 0){
                Address address = listaEndereco.get(0);

                return address;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void recuperarLocalizacaoUsuario() {

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                //Recuperar latitude e longitude
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                localPassageiro = new LatLng(latitude, longitude);

                //Atualiza geofire
                UsuarioFirebase.atualizarDadosLocalizacao(latitude, longitude);

                //Altera interface de acordo com o status
                alteraInterfaceStatusRequisicao(statusRequisicao);

                if (statusRequisicao != null && !statusRequisicao.isEmpty()){
                    if (statusRequisicao.equals(Requisicao.STATUS_VIAGEM)
                            || statusRequisicao.equals(Requisicao.STATUS_FINALIZADA)){
                        locationManager.removeUpdates(locationListener);
                    }else {
                        //Solicita atualização de localização
                        if (ActivityCompat.checkSelfPermission(PassageiroActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(PassageiroActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            locationManager.requestLocationUpdates(
                                    LocationManager.GPS_PROVIDER,
                                    10000,
                                    10,
                                    locationListener
                            );
                        }
                    }
                }
            }
        };

        //Solicitar atualizações de localização
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    10000,
                    10,
                    locationListener
            );
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.menuSair :
                autenticacao.signOut();
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void inicializarComponentes(){
        //Toolbar
        getSupportActionBar().setTitle("Iniciar uma viagem");

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(PassageiroActivity.this);

        //Configurações iniciais
        autenticacao = ConfiguracaoFirebase.getAutenticacao();
        firebaseRef = ConfiguracaoFirebase.getFirebase();
        editDestino = findViewById(R.id.editDestino);
        linearLayoutDestino = findViewById(R.id.linearLayoutDestino);
        buttonChamarUber = findViewById(R.id.buttonChamarUber);
    }
}