package com.cursoandroid.uber.helper;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.cursoandroid.uber.activity.PassageiroActivity;
import com.cursoandroid.uber.activity.RequisicaoActivity;
import com.cursoandroid.uber.model.Usuario;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

public class UsuarioFirebase {

    public static void atualizarDadosLocalizacao(Double latitude, Double longitude){
        //Define nó de local usuário
        DatabaseReference localUsuario = ConfiguracaoFirebase.getFirebase()
                .child("local_usuario");

        GeoFire geoFire = new GeoFire(localUsuario);

        Usuario usuarioLogado = UsuarioFirebase.getDadosUsuarioLogado();
        geoFire.setLocation(usuarioLogado.getId(),
                new GeoLocation(latitude, longitude),
                new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {
                        if (error != null){
                            Log.d("Erro", "Erro ao salvar local!");
                        }
                    }
                });

    }

    public static FirebaseUser getUsuarioAtual(){
        FirebaseAuth usuario = ConfiguracaoFirebase.getAutenticacao();
        return usuario.getCurrentUser();
    }

    public static Usuario getDadosUsuarioLogado(){
        FirebaseUser firebaseUser = getUsuarioAtual();

        Usuario usuario = new Usuario();
        usuario.setId(firebaseUser.getUid());
        usuario.setEmail(firebaseUser.getEmail());
        usuario.setNome(firebaseUser.getDisplayName());

        return usuario;
    }

    public static boolean atualizarNomeUsuario(String nome){
        try {
            FirebaseUser user = getUsuarioAtual();
            UserProfileChangeRequest profile = new UserProfileChangeRequest.Builder()
                    .setDisplayName(nome)
                    .build();
            user.updateProfile(profile).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (!task.isSuccessful()){
                        Log.d("Perfil", "Erro ao atualizar nome de perfil");
                    }
                }
            });
            return true;

        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

    public static void redirecionarUsuarioLogado(Activity activity){

        FirebaseUser user = getUsuarioAtual();

        if (user != null){
            DatabaseReference usuarioRef = ConfiguracaoFirebase.getFirebase()
                    .child("usuarios")
                    .child(getIdentificadorUsuario());

            usuarioRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {

                    Usuario usuario = snapshot.getValue(Usuario.class);
                    String tipoUsuario = usuario.getTipo();
                    if (tipoUsuario.equals("M")){
                        activity.startActivity(new Intent(activity, RequisicaoActivity.class));
                    }else {
                        activity.startActivity(new Intent(activity, PassageiroActivity.class));
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }

    }

    public static String getIdentificadorUsuario(){
        return getUsuarioAtual().getUid();
    }
}
