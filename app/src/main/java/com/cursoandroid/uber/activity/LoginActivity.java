package com.cursoandroid.uber.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.cursoandroid.uber.R;
import com.cursoandroid.uber.helper.ConfiguracaoFirebase;
import com.cursoandroid.uber.helper.UsuarioFirebase;
import com.cursoandroid.uber.model.Usuario;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText campoEmail, campoSenha;
    private FirebaseAuth autenticacao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        //Inicializar componentes
        campoEmail = findViewById(R.id.editEmailLogin);
        campoSenha = findViewById(R.id.editSenhaLogin);

    }

    public void validarLoginUsuario(View view){
        String textoEmail = campoEmail.getText().toString();
        String textoSenha = campoSenha.getText().toString();

        if (!textoEmail.isEmpty()){
            if (!textoSenha.isEmpty()){
                Usuario usuario = new Usuario();
                usuario.setEmail(textoEmail);
                usuario.setSenha(textoSenha);
                logarUsuario(usuario);
                finish();
            }else {
                Toast.makeText(this,
                        "Preencha a senha!",
                        Toast.LENGTH_SHORT).show();
            }
        }else {
            Toast.makeText(this,
                    "Preencha o E-mail!",
                    Toast.LENGTH_SHORT).show();
        }
    }

    public void logarUsuario(Usuario usuario){
        autenticacao = ConfiguracaoFirebase.getAutenticacao();
        autenticacao.signInWithEmailAndPassword(
                usuario.getEmail(),
                usuario.getSenha()
        ).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
               if (task.isSuccessful()){
                   //Verifica o tipo de usuário logado (M / P)
                   UsuarioFirebase.redirecionarUsuarioLogado(LoginActivity.this);

               }else {
                   String erroExcecao = "";
                   try {
                       throw task.getException();
                   }catch (FirebaseAuthInvalidCredentialsException e){
                       erroExcecao = "E-mail e senha não correspodem a um usuário cadastrado!";
                   }catch (FirebaseAuthUserCollisionException e){
                       erroExcecao = "Usuário não está cadastrado!";
                   } catch (Exception e) {
                       erroExcecao = e.getMessage();
                       e.printStackTrace();
                   }

                   Toast.makeText(LoginActivity.this,
                           "Erro ao logar: " + erroExcecao,
                           Toast.LENGTH_LONG).show();
               }
            }
        });
    }
}