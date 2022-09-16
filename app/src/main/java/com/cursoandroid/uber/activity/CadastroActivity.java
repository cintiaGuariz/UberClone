package com.cursoandroid.uber.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
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

public class CadastroActivity extends AppCompatActivity {

    private TextInputEditText textNomeCadastro;
    private TextInputEditText textEmailCadastro;
    private TextInputEditText textSenhaCadastro;
    private Switch tipoUsuario;
    private Button botaoCadastrar;
    private FirebaseAuth autenticacao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.cursoandroid.uber.R.layout.activity_cadastro);

        inicializarComponentes();

        botaoCadastrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //Recuperar textos dos campos
                String nomeCompleto = textNomeCadastro.getText().toString();
                String email = textEmailCadastro.getText().toString();
                String senha = textSenhaCadastro.getText().toString();

                if (!nomeCompleto.isEmpty()){
                    if (!email.isEmpty()){
                        if (!senha.isEmpty()){

                            Usuario usuario = new Usuario();
                            usuario.setNome(nomeCompleto);
                            usuario.setEmail(email);
                            usuario.setSenha(senha);
                            usuario.setTipo(verificaTipoUsuario());

                            cadastrarUsuario(usuario);

                        }else {
                            Toast.makeText(CadastroActivity.this,
                                    "Preencha a senha!",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }else {
                        Toast.makeText(CadastroActivity.this,
                                "Preencha o E-mail!",
                                Toast.LENGTH_SHORT).show();
                    }
                }else {
                    Toast.makeText(CadastroActivity.this,
                            "Preencha o nome!",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void cadastrarUsuario(Usuario usuario){

        autenticacao = ConfiguracaoFirebase.getAutenticacao();
        autenticacao.createUserWithEmailAndPassword(
                usuario.getEmail(),
                usuario.getSenha()
        ).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()){

                    try {
                        String idUsuario = task.getResult().getUser().getUid();
                        usuario.setId(idUsuario);
                        usuario.salvar();

                        //Atualiza nome no UserProfile
                        UsuarioFirebase.atualizarNomeUsuario(usuario.getNome());

                        //Redireciona o usuário de acordo com seu tipo
                        // (caso passageiro -> MapsAct., motorista ->RequisiçãoAct)
                        if (verificaTipoUsuario().equals("P")){
                            startActivity(new Intent(CadastroActivity.this, PassageiroActivity.class));
                            finish();

                            Toast.makeText(CadastroActivity.this,
                                    "Sucesso ao cadastrar passageiro!",
                                    Toast.LENGTH_SHORT).show();

                        }else {
                            startActivity(new Intent(CadastroActivity.this, RequisicaoActivity.class));
                            finish();

                            Toast.makeText(CadastroActivity.this,
                                    "Sucesso ao cadastrar motorista!",
                                    Toast.LENGTH_SHORT).show();
                        }

                    }catch (Exception e){
                        e.printStackTrace();
                    }

                }else {
                    String erroExcecao = "";
                    try {
                        throw task.getException();
                    }catch (FirebaseAuthWeakPasswordException e){
                        erroExcecao = "Digite uma senha forte!";
                    }catch (FirebaseAuthInvalidCredentialsException e){
                        erroExcecao = "Digite um Email válido!";
                    }catch (FirebaseAuthUserCollisionException e){
                        erroExcecao = "esta conta já foi cadastrada!";
                    } catch (Exception e) {
                        erroExcecao = e.getMessage();
                        e.printStackTrace();
                    }

                    Toast.makeText(CadastroActivity.this,
                            "Erro ao cadastrar: " + erroExcecao,
                            Toast.LENGTH_LONG).show();
                }
            }
        });

    }

    public String verificaTipoUsuario(){
        return tipoUsuario.isChecked() ? "M" : "P";
    }

    private void inicializarComponentes(){
        textNomeCadastro = findViewById(R.id.textNomeCadastro);
        textEmailCadastro = findViewById(R.id.textEmailCadastro);
        textSenhaCadastro = findViewById(R.id.textSenhaCadastro);
        botaoCadastrar = findViewById(R.id.buttonCadastrar);
        tipoUsuario = findViewById(R.id.switchTipoUsuario);
    }
}